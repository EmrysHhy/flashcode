import { request } from '@/utils/request';

/**
 * 发送验证码（仅支持邮箱）
 * @param {{ account: string }} params
 * @returns {Promise<Object>}
 */
export const sendVerificationCode = async (params) => {
  return await request({
    url: '/portal/user/login/send_code',
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
    url: '/portal/user/login/logout',
    method: 'DELETE',
  });
};

/**
 * 当前登录用户资料
 */
export const fetchProfile = async () => {
  return await request({
    url: '/portal/user/query/login_info/get',
    method: 'GET',
  });
};

/**
 * 修改昵称，或补绑手机号/邮箱
 * @param {{ nickName?: string, phone?: string, email?: string, code?: string }} data
 */
export const updateProfile = async (data) => {
  return await request({
    url: '/portal/user/login/edit',
    method: 'POST',
    data,
  });
};

/**
 * 上传头像
 * @param {File} file
 * @returns {Promise<string>}
 */
export const updateAvatar = async (file) => {
  const data = new FormData();
  data.append('file', file);
  return await request({
    url: '/portal/user/login/avatar',
    method: 'POST',
    data,
  });
};
