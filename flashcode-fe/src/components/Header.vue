<template>
  <header class="header">
    <div class="header__container">
      <div class="header__logo">
        <img :src="logoSrc" alt="Logo" />
      </div>
      <nav class="header__nav">
        <div class="header__nav-list">
          <a v-for="item in navItems" :key="item.id" :href="item.href"
            :class="['header__nav-item', { 'header__nav-item--active': activeNav === item.id }]"
            @click.prevent="setActiveNav(item)" ref="navItemRefs">
            {{ item.label }}
          </a>
          <div class="header__nav-slider" :style="sliderStyle"></div>
        </div>
      </nav>
      <div class="header__actions">
        <button v-if="!userStore.isLoggedIn" class="header__login-btn" @click="handleLogin">
          登录
        </button>
        <div v-else class="user-section">
          <span class="user-name">{{ getUserDisplayName() }}</span>
          <span class="btn btn-outline" @click="handleLogout">退出</span>
        </div>
      </div>
    </div>
  </header>
  <Login v-model:visible="showLoginModal" @success="handleLoginSuccess" />
</template>

<script setup>
import { onMounted, ref, nextTick } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { useUserStore } from '@/stores/user';
import { logout } from '@/apis';
import Login from '@/components/Login.vue';

// ========== 初始化 ==========
const router = useRouter();
const userStore = useUserStore();
const logoSrc = new URL('@/imges/logo.webp', import.meta.url).href;

// 导航配置
const navItems = [
  { id: 'home', label: '首页', href: '/home' },
  { id: 'cases', label: '案例广场', href: '/cases' },
  { id: 'about', label: '关于我们', href: '/about' },
];

// 导航状态
const activeNav = ref('');
const navItemRefs = ref([]);
const sliderStyle = ref({});
const showLoginModal = ref(false);

// ========== 用户信息显示 ==========
/**
 * 获取用户显示名称，优先级：username > 脱敏手机号 > 脱敏邮箱 > "用户"
 */
const getUserDisplayName = () => {
  // 优先显示用户名
  if (userStore.userInfo?.username) {
    return userStore.userInfo.username;
  }
  
  // 脱敏手机号：139****1234
  if (userStore.userInfo?.phone) {
    const phone = userStore.userInfo.phone;
    return phone.replace(/(\d{3})\d{4}(\d{4})/, '$1****$2');
  }
  
  // 脱敏邮箱：abc***@example.com
  if (userStore.userInfo?.email) {
    const email = userStore.userInfo.email;
    const [name, domain] = email.split('@');
    if (name.length > 3) {
      return `${name.substring(0, 3)}***@${domain}`;
    }
    return email;
  }
  
  return '用户';
};

// ========== 登录/登出 ==========
/**
 * 显示登录弹窗
 */
const handleLogin = () => {
  showLoginModal.value = true;
};

/**
 * 处理退出登录
 */
const handleLogout = async () => {
  const confirmed = window.confirm('确定要退出登录吗？');
  if (!confirmed) {
    return;
  }

  // 调用退出登录接口（允许失败）
  try {
    await logout();
  } catch (error) {
    console.error('退出登录接口调用失败:', error);
  }

  // 清除本地用户信息
  userStore.clearUserInfo();
  ElMessage.success('已退出登录');

  // 跳转到首页
  if (router.currentRoute.value.path !== '/') {
    router.push('/');
  }
};

/**
 * 登录成功回调
 * 如果在首页，刷新页面以加载用户的应用列表
 */
const handleLoginSuccess = () => {
  showLoginModal.value = false;

  nextTick(() => {
    const currentPath = router.currentRoute.value.path;
    // Login 组件会自动跳转到首页，这里检查是否需要刷新
    if (currentPath === '/' || currentPath === '/home') {
      window.location.reload();
    }
  });
};

// ========== 导航高亮滑块 ==========
/**
 * 更新导航下方的高亮滑块位置和宽度
 */
const updateSlider = () => {
  const idx = navItems.findIndex((item) => item.id === activeNav.value);
  const el = navItemRefs.value?.[idx];
  if (!el) return;
  
  const { offsetWidth, offsetLeft } = el;
  sliderStyle.value = {
    width: `${offsetWidth}px`,
    transform: `translateX(${offsetLeft}px)`,
  };
};

/**
 * 设置激活的导航项
 * @param {Object} item - 导航项配置
 */
const setActiveNav = (item) => {
  // 更新激活状态
  activeNav.value = item.id;
  
  // 路由跳转
  router.replace(item.href).catch(() => {});
  
  // 更新滑块位置
  nextTick(updateSlider);
};

/**
 * 根据当前路由同步导航激活状态
 */
const syncActiveByRoute = () => {
  const currentPath = router.currentRoute.value.path;
  const found = navItems.find((item) => currentPath.startsWith(item.href));
  
  if (found) {
    activeNav.value = found.id;
    nextTick(updateSlider);
  }
};

// ========== 生命周期 ==========
/**
 * 组件挂载时同步导航状态
 * 注意：由于每个页面都独立引入 Header 组件，路由切换时组件会重新创建
 */
onMounted(() => {
  syncActiveByRoute();
});
</script>

<style lang="scss" scoped>
$primary-color: #1890ff;
$text-color: #333;
$border-color: #e8e8e8;
$white: #fff;

.header {
  width: 100%;
  height: 70px;
  background-color: $white;
  border-bottom: 1px solid $border-color;
}

.header__container {
  height: 100%;
  margin: 0 auto;
  padding: 0 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
}

.header__logo {
  display: flex;
  align-items: center;
  gap: 8px;
}

.header__logo img {
  height: 72px;
  width: auto;
}

.header__nav {
  flex: 0.95;
}

.header__nav-list {
  display: flex;
  align-items: center;
  gap: 40px;
  position: relative;
  padding-bottom: 5px;
}

.header__nav-item {
  font-size: 14px;
  color: $text-color;
  text-decoration: none;
  cursor: pointer;
  transition: color 0.3s ease;
}

.header__nav-item:hover,
.header__nav-item--active {
  color: $primary-color;
}

.header__nav-slider {
  position: absolute;
  bottom: 0;
  left: 0;
  height: 2px;
  background-color: $primary-color;
  transition: transform 0.3s ease, width 0.3s ease;
}

.header__actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header__login-btn {
  padding: 8px 24px;
  font-size: 14px;
  color: $white;
  background-color: $primary-color;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  transition: background-color 0.3s ease;
}

.header__login-btn:hover {
  background-color: darken($primary-color, 10%);
}

.user-section {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-name {
  font-size: 14px;
  color: $text-color;
}

.btn-outline {
  padding: 6px 16px;
  font-size: 14px;
  background: transparent;
  color: $primary-color;
  border-radius: 4px;
  cursor: pointer;
  transition: background-color 0.3s ease, color 0.3s ease;
}
</style>
