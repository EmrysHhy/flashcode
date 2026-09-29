import { request } from '@/utils/request';

/**
 * 发送验证码（仅支持邮箱）
 * @param {{ account: string }} params
 * @returns {Promise<Object>}
 */
export const sendVerificationCode = async (params) => {
  return await request({
    url: '/portal/user/send_code',
    method: 'GET',
    params,
  });
};

/**
 * 验证码登录（仅支持邮箱）
 * @param {{ email: string, code: string }} data
 * @returns {Promise<{ accessToken: string, expires?: number }>}
 */
export const loginWithCode = async (data) => {
  return await request({
    url: '/portal/user/login/code',
    method: 'POST',
    data,
  });
};

/**
 * 退出登录
 * @returns {Promise<void>}
 */
export const logout = async () => {
  return await request({
    url: '/portal/user/logout',
    method: 'DELETE',
  });
};
