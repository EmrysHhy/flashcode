import { ref } from 'vue';
import { defineStore } from 'pinia';
import { decodeJWT } from '@/utils/jwt';

export const useUserStore = defineStore('user', () => {
  // 用户状态
  const accessToken = ref(localStorage.getItem('accessToken') || '');
  const refreshToken = ref(localStorage.getItem('refreshToken') || '');

  // 初始化时，如果 accessToken 是 JWT，解码并作为 userInfo
  // 否则使用 localStorage 中的 userInfo（向后兼容）
  let initialUserInfo = null;
  if (accessToken.value) {
    const decodedToken = decodeJWT(accessToken.value);
    if (decodedToken) {
      initialUserInfo = decodedToken;
    } else {
      // 如果 JWT 解码失败，尝试从 localStorage 读取（向后兼容）
      try {
        initialUserInfo = JSON.parse(localStorage.getItem('userInfo') || 'null');
      } catch (e) {
        initialUserInfo = null;
      }
    }
  }

  const userInfo = ref(initialUserInfo);
  const profile = ref(null);
  const isLoggedIn = ref(!!accessToken.value);

  /**
   * 设置用户登录信息
   * @param {Object} loginData - 登录返回的数据
   */
  function setUserInfo(loginData) {
    accessToken.value = loginData.accessToken || '';
    refreshToken.value = loginData.refreshToken || '';

    // 如果 accessToken 是 JWT，解码并作为 userInfo
    if (accessToken.value) {
      const decodedToken = decodeJWT(accessToken.value);
      if (decodedToken) {
        // 使用 JWT 解码后的信息作为 userInfo
        userInfo.value = decodedToken;
      } else if (loginData.userInfo) {
        // 如果 JWT 解码失败，使用传入的 userInfo（向后兼容）
        userInfo.value = loginData.userInfo;
      } else {
        userInfo.value = null;
      }
    } else {
      userInfo.value = null;
    }
    isLoggedIn.value = !!accessToken.value;

    // 持久化到 localStorage
    if (accessToken.value) {
      localStorage.setItem('accessToken', accessToken.value);
    }
    if (refreshToken.value) {
      localStorage.setItem('refreshToken', refreshToken.value);
    }
    if (userInfo.value) {
      localStorage.setItem('userInfo', JSON.stringify(userInfo.value));
    }
  }

  /**
   * 清除用户信息（退出登录）
   */
  function setProfile(info) {
    profile.value = info;
  }

  function clearUserInfo() {
    accessToken.value = '';
    refreshToken.value = '';
    userInfo.value = null;
    profile.value = null;
    isLoggedIn.value = false;

    // 清除 localStorage
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    localStorage.removeItem('userInfo');
  }

  return {
    accessToken,
    refreshToken,
    userInfo,
    profile,
    isLoggedIn,
    setUserInfo,
    setProfile,
    clearUserInfo,
  };
});

