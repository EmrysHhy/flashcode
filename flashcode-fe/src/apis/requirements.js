import { request } from '@/utils/request.js';

/**
 * 流式生成应用需求文档
 * @param {string} input - 用户输入的需求描述
 * @param {File|null} sketch - 可选的 UI 草图文件
 * @param {Function} onData - 数据回调函数，接收流式数据片段
 * @returns {Promise<void>}
 */
export const streamRequirements = async (input, sketch = null, onData) => {
  // 构建 FormData
  const formData = new FormData();
  if (input) {
    formData.append('input', input);
  }
  if (sketch) {
    formData.append('sketch', sketch);
  }

  // 发送请求（流式响应需要跳过响应拦截器）
  const response = await request({
    url: '/portal/flashcode/requirement/generate',
    method: 'POST',
    data: formData,
    skipResponseInterceptor: true,
  });

  if (!response.ok) {
    throw new Error(`HTTP error! status: ${response.status}`);
  }

  // 读取流式响应
  const reader = response.body.getReader();
  const decoder = new TextDecoder();
  let buffer = '';

  while (true) {
    const { done, value } = await reader.read();

    if (done) {
      break;
    }

    // 解码数据块
    buffer += decoder.decode(value, { stream: true });

    // 处理 SSE 格式的数据
    const lines = buffer.split('\n');
    buffer = lines.pop() || '';

    for (const line of lines) {
      if (line.startsWith('data: ')) {
        const content = line.substring(6);
        if (content.trim() && onData) {
          onData(content);
        }
      }
    }
  }

  // 处理最后剩余的数据
  if (buffer.trim()) {
    if (buffer.startsWith('data: ')) {
      const content = buffer.substring(6);
      if (onData) {
        onData(content);
      }
    } else if (onData) {
      onData(buffer);
    }
  }
};

/**
 * 非流式生成需求文档，返回 appId 与 document
 * @param {string} input
 * @param {File|null} file
 * @param {Object} extraHeaders - 额外请求头
 * @returns {Promise<{ appId: string|null, document: string }>}
 */
export const generateRequirements = async (input, file = null, extraHeaders = {}) => {
  // 校验文件大小（5MB）
  if (file && file.size > 5 * 1024 * 1024) {
    throw new Error('文件大小不能超过 5MB');
  }

  const formData = new FormData();
  formData.append('input', input || '请根据上传的文件生成需求文档');
  if (file) {
    formData.append('sketch', file);
  }

  try {
    const data = await request({
      url: '/portal/flashcode/requirement/generate',
      method: 'POST',
      data: formData,
      headers: {
        ...extraHeaders,
      },
      skipErrorHandler: true, // 跳过统一错误处理，自行处理特殊错误
    });

    const document = data.requirement || '';
    const appId = data.appId ? String(data.appId) : null;

    return { appId, document };
  } catch (error) {
    // 文件大小相关错误特殊处理
    if (error.message && (error.message.includes('文件大小') || error.message.includes('buffer overflow'))) {
      throw new Error('文件太大，请上传小于 5MB 的文件');
    }
    // 其他错误重新抛出（响应拦截器已处理）
    throw error;
  }
};
