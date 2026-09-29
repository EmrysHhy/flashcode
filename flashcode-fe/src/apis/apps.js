import { request } from '@/utils/request.js';

/**
 * 获取案例广场应用列表（已部署的应用）
 * @param {Object} data - 请求体（由调用方组装）
 * @returns {Promise<{ list: Array, totals: number, totalPages: number }>}
 */
export const fetchAppList = async (data = {}) => {
  return await request({
    url: '/portal/list/square',
    method: 'POST',
    data,
  });
};

/**
 * 获取我的应用列表
 * @param {Object} data - 请求体（由调用方组装）
 * @returns {Promise<{ list: Array, totals: number, totalPages: number }>}
 */
export const fetchMyAppList = async (data = {}) => {
  return await request({
    url: '/portal/list/personal',
    method: 'POST',
    data,
  });
};

/**
 * 生成应用
 * @param {any} data - 请求体（通常为 FormData，由调用方组装）
 * @returns {Promise<Object>} 返回生成结果
 */
export const generateApp = async (data) => {
  return await request({
    url: '/portal/flashcode/app/generate',
    method: 'POST',
    data,
  });
};


/**
 * 获取应用详情（包含应用信息和对话历史）
 * @param {{ appId: number|string }} params
 * @returns {Promise<Object>}
 */
export const fetchAppDetail = async (params) => {
  return await request({
    url: '/portal/flashcode/detail',
    method: 'GET',
    params,
  });
};

/**
 * 获取聊天历史列表（分页）
 * @param {Object} data - 请求体（由调用方组装）
 * @returns {Promise<{ list: Array, totals: number, totalPages: number }>}
 */
export const fetchChatHistory = async (data = {}) => {
  return await request({
    url: '/portal/list/chat_history',
    method: 'POST',
    data,
  });
};

/**
 * 发布应用，返回已部署的访问地址
 * @param {number|string} appId
 * @returns {Promise<string>}
 */
export const deployApp = async (appId) => {
  return await request({
    url: '/portal/flashcode/app_deploy',
    method: 'POST',
    params: { appId },
  });
};

/**
 * 修改应用中已选中的元素
 * @param {{ appId: number|string, elementSelector: string, newContent: string }} data
 * @returns {Promise<{ appId: string, appType: string, url: string }>}
 */
export const modifyApp = async (data) => {
  return await request({
    url: '/portal/flashcode/edit',
    method: 'POST',
    data,
  });
};

/**
 * 打开高级编辑，返回 VS Code 地址
 * @param {number|string} appId
 * @returns {Promise<string>}
 */
export const createVscode = async (appId) => {
  return await request({
    url: '/portal/flashcode/getsrc',
    method: 'POST',
    params: { appId },
  });
};

/**
 * 完成高级编辑并重新打包预览
 * @param {number|string} appId
 * @returns {Promise<{ appId: string, appType: string, url: string }>}
 */
export const completeEdit = async (appId) => {
  return await request({
    url: '/portal/flashcode/advanced_edit',
    method: 'POST',
    params: { appId },
  });
};

