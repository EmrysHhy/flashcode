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
        <button v-else class="header__user" type="button" @click="goProfile">
          <span class="header__avatar">
            <img v-if="avatarUrl" :src="avatarUrl" alt="" />
            <span v-else>我</span>
          </span>
          <span v-if="displayName" class="header__name" :title="fullName">{{ displayName }}</span>
        </button>
      </div>
    </div>
  </header>
  <Login v-model:visible="showLoginModal" @success="handleLoginSuccess" />
</template>

<script setup>
import { computed, onMounted, ref, nextTick } from 'vue';
import { useRouter } from 'vue-router';
import { useUserStore } from '@/stores/user';
import { fetchProfile } from '@/apis';
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
const NAME_MAX_LENGTH = 8;
const avatarUrl = computed(() => userStore.profile?.avatar || '');
const fullName = computed(() => (userStore.profile?.nickName || '').trim());
const displayName = computed(() => {
  const name = fullName.value;
  if (name.length <= NAME_MAX_LENGTH) {
    return name;
  }
  return `${name.slice(0, NAME_MAX_LENGTH)}…`;
});

const loadAvatar = async () => {
  if (!userStore.isLoggedIn) {
    userStore.setProfile(null);
    return;
  }
  try {
    const profile = await fetchProfile();
    userStore.setProfile(profile);
  } catch (error) {
    console.error(error);
  }
};

const goProfile = () => {
  router.push('/profile');
};

// ========== 登录/登出 ==========
/**
 * 显示登录弹窗
 */
const handleLogin = () => {
  showLoginModal.value = true;
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
  loadAvatar();
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

.header__user {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 0;
  border: none;
  background: transparent;
  cursor: pointer;
}

.header__avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  overflow: hidden;
  background: #e6f4ff;
  color: $primary-color;
  font-size: 14px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.header__name {
  max-width: 8em;
  overflow: hidden;
  font-size: 14px;
  color: $text-color;
  white-space: nowrap;
}

.header__avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
</style>
