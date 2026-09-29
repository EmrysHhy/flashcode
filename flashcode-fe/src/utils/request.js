import { useUserStore } from '@/stores/user';

/**
 * 请求拦截器数组
 * 每个拦截器接收 config，返回修改后的 config
 */
const requestInterceptors = [];

/**
 * 响应拦截器数组
 * 每个拦截器接收 response，返回处理后的数据或抛出错误
 */
const responseInterceptors = [];

/**
 * 添加请求拦截器
 * @param {Function} interceptor - 拦截器函数，接收 config，返回 config
 */
export const addRequestInterceptor = (interceptor) => {
  requestInterceptors.push(interceptor);
};

/**
 * 添加响应拦截器
 * @param {Function} interceptor - 拦截器函数，接收 response，返回处理后的数据
 */
export const addResponseInterceptor = (interceptor) => {
  responseInterceptors.push(interceptor);
};

/**
 * 默认请求拦截器：处理 URL 参数、headers、body
 */
const defaultRequestInterceptor = (config) => {
  const userStore = useUserStore();
  const {
    url,
    method = 'GET',
    params,
    data,
    headers: customHeaders = {},
    skipResponseInterceptor = false,
    ...rest
  } = config;

  // 处理 query params
  let finalUrl = url || '';
  if (params && typeof params === 'object') {
    const searchParams = new URLSearchParams();
    Object.entries(params).forEach(([key, value]) => {
      if (value !== undefined && value !== null) {
        searchParams.append(key, String(value));
      }
    });
    const queryString = searchParams.toString();
    if (queryString) {
      const hasQuery = finalUrl.includes('?');
      finalUrl += `${hasQuery ? '&' : '?'}${queryString}`;
    }
  }

  // 处理 headers
  const headers = { ...customHeaders };
  if (userStore.accessToken) {
    headers['Authorization'] = `Bearer ${userStore.accessToken}`;
  }

  // 处理 body
  let body = rest.body;
  if (data !== undefined && data !== null) {
    const isFormData = data instanceof FormData;
    if (!isFormData && method.toUpperCase() !== 'GET') {
      headers['Content-Type'] = headers['Content-Type'] || 'application/json';
      body = JSON.stringify(data);
    } else {
      body = data;
    }
  }

  return {
    ...rest,
    url: finalUrl,
    method,
    headers,
    body,
    skipResponseInterceptor,
  };
};

/**
 * 自定义错误类，包含 code 和 msg
 */
class ApiError extends Error {
  constructor(message, code, msg) {
    super(message);
    this.name = 'ApiError';
    this.code = code;
    this.msg = msg || message;
  }
}

/**
 * 解析响应中的错误信息
 */
const parseErrorResponse = async (response) => {
  let code = null;
  let msg = null;

  try {
    const contentType = response.headers.get('content-type');
    if (contentType && contentType.includes('application/json')) {
      // 克隆 response 以避免消耗原始响应体
      const clonedResponse = response.clone();
      const errorJson = await clonedResponse.json();
      code = errorJson.code !== undefined ? errorJson.code : null;
      msg = errorJson.msg || errorJson.message || null;
    } else if (response.status === 504) {
      msg = '生成时间超过网关等待上限，请稍后在应用详情里查看是否已经生成';
    } else {
      // 非 JSON 响应，尝试读取文本
      const clonedResponse = response.clone();
      const errorText = await clonedResponse.text();
      msg = errorText && !errorText.trim().startsWith('<')
        ? errorText
        : `请求失败 (HTTP ${response.status})`;
    }
  } catch (e) {
    // 解析失败，使用默认错误信息
    console.warn('Failed to parse error response:', e);
  }

  // 如果没有获取到错误信息，使用默认值
  if (!msg) {
    msg = `请求失败 (HTTP ${response.status})`;
  }

  return { code, msg };
};

/**
 * 默认响应拦截器：统一处理成功和失败
 */
const defaultResponseInterceptor = async (response) => {
  // 检查 HTTP 状态
  if (!response.ok) {
    const { code, msg } = await parseErrorResponse(response);
    const errorMessage = msg || `请求失败 (HTTP ${response.status})`;
    throw new ApiError(errorMessage, code, msg);
  }

  // 尝试解析 JSON
  let result;
  try {
    const contentType = response.headers.get('content-type');
    if (contentType && contentType.includes('application/json')) {
      result = await response.json();
    } else {
      // 非 JSON 响应，直接返回 response
      return response;
    }
  } catch (e) {
    // 解析失败，返回原始 response
    return response;
  }

  // 检查业务状态码
  if (result.code !== undefined && result.code !== 200000) {
    const errorMessage = result.msg || result.message || '请求失败';
    throw new ApiError(errorMessage, result.code, result.msg || result.message);
  }

  // 返回数据
  return result.data !== undefined ? result.data : result;
};

// 注册默认拦截器
addRequestInterceptor(defaultRequestInterceptor);
addResponseInterceptor(defaultResponseInterceptor);

/**
 * 统一错误处理
 */
const handleError = (error, config) => {
  const userStore = useUserStore();
  let errorMessage = '请求失败，请稍后重试';

  // 如果是 ApiError，使用其中的 msg 和 code
  if (error instanceof ApiError) {
    // 如果 code 是 401003，表示登录已过期，清除登录状态
    if (error.code === 401003) {
      userStore.clearUserInfo();
      errorMessage = error.msg || '登录已过期，请重新登录';
    } else {
      // 优先使用接口返回的 msg
      errorMessage = error.msg || error.message;
    }

    // 在控制台输出详细的错误信息（包含 code）
    if (error.code !== null && error.code !== undefined) {
      console.error(`API Error [Code: ${error.code}]: ${error.msg || error.message}`);
      // ElMessage.error(errorMessage);
    } else {
      console.error(`API Error: ${error.msg || error.message}`);
      // ElMessage.error(errorMessage);
    }
  } else {
    errorMessage = error.message || errorMessage;
    console.error('Request Error:', error);
    // ElMessage.error(errorMessage);
  }

  // 如果配置了 skipErrorHandler，则不显示错误消息
  if (config.skipErrorHandler) {
    throw error;
  }

  throw error;
};

/**
 * 通用请求封装
 * 支持两种调用方式：
 * - request('/api/path', { method, params, data, headers })
 * - request({ url, method, params, data, headers })
 * 
 * 配置选项：
 * - skipResponseInterceptor: 跳过响应拦截器（用于流式响应等特殊场景）
 * - skipErrorHandler: 跳过统一错误处理（不显示错误消息）
 */
export const request = async (input, options = {}) => {
  // 兼容旧签名：request(url, options)
  const config = typeof input === 'string' ? { url: input, ...options } : input || {};

  // 默认超时：10 分钟（600000ms）
  // 可通过单次请求传入 timeoutMs 覆盖；传 0 / null / undefined 表示不启用超时。
  const timeoutMs = config.timeoutMs ?? 600000;

  try {
    // 执行请求拦截器
    let finalConfig = config;
    for (const interceptor of requestInterceptors) {
      finalConfig = await interceptor(finalConfig);
    }

    // 超时控制（fetch 原生无 timeout，使用 AbortController）
    let timeoutId = null;
    let fetchSignal = finalConfig.signal;
    let controller = null;

    if (timeoutMs !== null && timeoutMs !== undefined && timeoutMs > 0) {
      controller = new AbortController();
      fetchSignal = controller.signal;

      // 如果外部也传入了 signal，则联动中止
      if (finalConfig.signal) {
        if (finalConfig.signal.aborted) {
          controller.abort(finalConfig.signal.reason);
        } else {
          finalConfig.signal.addEventListener(
            'abort',
            () => controller?.abort(finalConfig.signal.reason),
            { once: true }
          );
        }
      }

      timeoutId = setTimeout(() => {
        controller?.abort(new Error(`请求超时（${timeoutMs}ms）`));
      }, timeoutMs);
    }

    // 发送请求
    try {
      const response = await fetch(finalConfig.url, {
        method: finalConfig.method,
        headers: finalConfig.headers,
        body: finalConfig.body,
        ...finalConfig,
        // 用我们合并后的 signal 覆盖
        signal: fetchSignal,
      });

      // 如果跳过响应拦截器，直接返回 response（用于流式响应等）
      if (finalConfig.skipResponseInterceptor) {
        return response;
      }

      // 执行响应拦截器
      let result = response;
      for (const interceptor of responseInterceptors) {
        result = await interceptor(result);
      }

      return result;
    } finally {
      if (timeoutId) {
        clearTimeout(timeoutId);
      }
    }
  } catch (error) {
    return handleError(error, config);
  }
};


