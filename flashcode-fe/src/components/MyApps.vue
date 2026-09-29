<template>
  <section v-if="shouldShowSection" class="my-apps" id="my-apps">
    <div class="container">
      <div class="section-header fade-in">
        <h2 class="section-title">我的应用</h2>
      </div>

      <div class="filter-bar fade-in">
        <div class="filter-left">
          <div class="category-filters">
            <button v-for="category in categories" :key="category"
              :class="['filter-btn', { active: activeCategory === category }]" @click="activeCategory = category">
              {{ category }}
            </button>
          </div>
        </div>
        <!-- <div class="filter-right">
          <a href="#my-apps" class="view-more-link" @click.prevent="handleViewMore">查看更多 »</a>
        </div> -->
      </div>

      <div v-if="cases.length > 0" class="my-apps-grid">
        <div v-for="(caseItem, index) in cases" :key="caseItem.id || index" class="my-app-card card fade-in"
          :style="{ animationDelay: `${index * 0.1}s` }">
          <div class="app-image" :style="getImageStyle(caseItem)">
            <div class="app-overlay">
              <button class="view-detail-btn" @click.stop="previewApp(caseItem)">查看详情 »</button>
            </div>
          </div>
          <div class="app-info">
            <h3 class="app-name">{{ caseItem.appName }}</h3>
            <p class="app-description">{{ caseItem.appDesc || caseItem.appDescription || getAppDescription(caseItem.appType) }}</p>
            <div class="app-footer">
              <span class="app-type-badge" :style="{
                background: getAppTypeColor(caseItem.appType).background,
                color: getAppTypeColor(caseItem.appType).color
              }">
                {{ getAppTypeLabel(caseItem.appType) }}
              </span>
              <span class="app-time">{{ formatDate(caseItem) }}</span>
            </div>
          </div>
        </div>
      </div>

      <div v-if="hasMore" class="load-more fade-in">
        <button class="load-more-link" @click="loadMore" :disabled="isLoading">
          {{ isLoading ? '加载中...' : '加载更多' }}
        </button>
      </div>
      <div v-else-if="cases.length > 0" class="load-more fade-in">
        <p class="no-more-text">
          <span v-if="activeCategory === '全部'">已加载全部应用</span>
          <span v-else>已显示全部筛选结果</span>
          <span class="total-count">(共 {{ totals }} 个)</span>
        </p>
      </div>

      <div v-if="cases.length === 0 && !isLoading" class="empty-state fade-in">
        <p class="empty-text">
          <span v-if="activeCategory === '全部'">您还没有创建任何应用</span>
          <span v-else>「{{ activeCategory }}」分类下暂无应用</span>
        </p>
      </div>

      <div v-if="isLoading && cases.length === 0" class="loading-state fade-in">
        <p class="loading-text">加载中...</p>
      </div>

      <div v-if="loadError && cases.length === 0" class="error-state fade-in">
        <p class="error-text">加载失败，请稍后重试</p>
        <button class="btn btn-primary retry-btn" @click="loadAppList(true)">
          重新加载
        </button>
      </div>
    </div>
  </section>
</template>

<script setup>
import { ref, computed, onMounted, inject, watch } from 'vue';
import { useRouter } from 'vue-router';
import { fetchMyAppList } from '@/apis';
import { useUserStore } from '@/stores/user';
import { uniqueBy } from '@/utils/arrayUtils';

const router = useRouter();
const userStore = useUserStore();

// 获取父组件提供的状态（如果存在）
const isGeneratingRequirements = inject('isGeneratingRequirements', null);
const hasLoadedOnce = ref(false);

const isLoading = ref(false);
const cases = ref([]);
const pageNo = ref(1);
const pageSize = ref(4); // 每次加载3条
const totals = ref(0);
const totalPages = ref(0);
const loadError = ref(false); // 加载错误状态

// 筛选相关
const activeCategory = ref('全部');
const categories = ref([
  '全部',
  'HTML',
  'VUE3',
  'VUE3-Spring'
]);

// 获取图片样式
const getImageStyle = (item) => {
  if (item.appScreenshot) {
    return {
      backgroundImage: `url(${item.appScreenshot})`,
      backgroundSize: 'cover',
      backgroundPosition: 'center',
      backgroundRepeat: 'no-repeat'
    };
  }
  // 如果没有截图，使用默认渐变背景
  const gradients = [
    'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
    'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)',
    'linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)',
    'linear-gradient(135deg, #43e97b 0%, #38f9d7 100%)',
    'linear-gradient(135deg, #fa709a 0%, #fee140 100%)',
    'linear-gradient(135deg, #30cfd0 0%, #330867 100%)'
  ];
  const index = (item.appName || '').length % gradients.length;
  return { background: gradients[index] };
};

// 应用类型颜色映射（后端：0=HTML, 1=VUE, 2=VUE_SPRING）
// 格式：{ background: '背景色', color: '文字颜色' }
const appTypeColors = {
  0: { background: '#D8E6FD', color: '#3B82F6' }, // HTML - 蓝色
  1: { background: '#F3E8FF', color: '#9333EA' }, // VUE3 - 紫色
  2: { background: '#E3FFEC', color: '#2EB85C' }  // VUE3-Spring - 绿色
};

// 应用类型标签映射（后端：0=HTML, 1=VUE, 2=VUE_SPRING）
const appTypeLabels = {
  0: 'HTML',
  1: 'VUE3',
  2: 'VUE3-Spring',
};

// 应用类型描述映射（后端：0=HTML, 1=VUE, 2=VUE_SPRING）
const appTypeDescriptions = {
  0: '基于 HTML 技术栈开发的轻量级应用',
  1: '使用 Vue3 框架构建的现代化前端应用',
  2: 'Vue3 前端 + Spring 后端的全栈应用',
};

// 获取应用类型颜色
const getAppTypeColor = (appType) => {
  return appTypeColors[appType] || { background: '#3B82F6', color: '#3B82F6' };
};

// 获取应用类型标签
const getAppTypeLabel = (appType) => {
  return appTypeLabels[appType] || '未知';
};

// 获取应用描述
const getAppDescription = (appType) => {
  return appTypeDescriptions[appType] || '使用 Flash Code 生成的应用';
};

// 将分类转换为应用类型（后端期望数字：0=HTML, 1=VUE, 2=VUE_SPRING）
const categoryToAppType = (category) => {
  const categoryToTypeMap = {
    '全部': null,
    'HTML': 0,
    'VUE3': 1,
    'VUE3-Spring': 2,
  };
  return categoryToTypeMap[category];
};

// 格式化时间（显示为"创建于X小时前"等）
const formatTimeAgo = (item) => {
  // 如果应用数据中有创建时间字段，使用它
  // 否则返回默认文本
  if (item.createTime || item.deployTime) {
    const time = item.createTime || item.deployTime;
    return formatRelativeTime(time);
  }
  return '创建时间未知';
};

// 格式化相对时间
const formatRelativeTime = (timeStr) => {
  if (!timeStr) return '创建时间未知';

  try {
    const time = new Date(timeStr);
    const now = new Date();
    const diff = now - time;

    const seconds = Math.floor(diff / 1000);
    const minutes = Math.floor(seconds / 60);
    const hours = Math.floor(minutes / 60);
    const days = Math.floor(hours / 24);
    const weeks = Math.floor(days / 7);
    const months = Math.floor(days / 30);

    if (months > 0) {
      return `创建于${months}个月前`;
    } else if (weeks > 0) {
      return `创建于${weeks}周前`;
    } else if (days > 0) {
      return `创建于${days}天前`;
    } else if (hours > 0) {
      return `创建于${hours}小时前`;
    } else if (minutes > 0) {
      return `创建于${minutes}分钟前`;
    } else {
      return '刚刚创建';
    }
  } catch (error) {
    return '创建时间未知';
  }
};

// 预览应用（查看对话）
const previewApp = (item) => {
  // 跳转到应用详情页
  router.push({
    name: 'app',
    params: { id: item.id }
  });
};

// 格式化日期
const formatDate = (item) => {
  if (item.deployTime || item.createTime) {
    const time = item.deployTime || item.createTime;
    try {
      const date = new Date(time);
      const year = date.getFullYear();
      const month = String(date.getMonth() + 1).padStart(2, '0');
      const day = String(date.getDate()).padStart(2, '0');
      return `${year}-${month}-${day}`;
    } catch (error) {
      return '';
    }
  }
  return '';
};

// 查看更多（暂时不跳转）
const handleViewMore = () => {
  // 暂时不跳转，后续可以添加我的应用页面
  console.log('查看更多我的应用');
};

// 加载应用列表
const loadAppList = async (reset = false) => {
  if (isLoading.value) return;

  // 如果没有登录，则直接返回
  if (!userStore.accessToken) return
  try {
    isLoading.value = true;

    // 如果是重置，从第一页开始
    if (reset) {
      pageNo.value = 1;
      cases.value = [];
    }

    // 获取当前筛选的应用类型
    const appType = categoryToAppType(activeCategory.value);


    const result = await fetchMyAppList({
      pageNo: pageNo.value,
      pageSize: pageSize.value,
      ...(appType !== null ? { appType } : {}),
    });

    if (result && result.list) {
      // 根据是否重置决定数据的处理方式
      if (reset) {
        // 重置时直接替换
        cases.value = result.list;
      } else {
        // 合并并去重，防止重复数据
        const merged = [...cases.value, ...result.list];
        cases.value = uniqueBy(merged, 'id');
      }

      // 更新总数和总页数
      totals.value = result.totals || 0;
      totalPages.value = result.totalPages || 0;

      // 不要用服务器返回的 pageNo，保持客户端控制
      // pageNo 在 loadMore 中已经自增了

      loadError.value = false; // 加载成功，清除错误状态
    }
  } catch (error) {
    console.error('加载我的应用列表失败:', error);
    loadError.value = true; // 设置错误状态
  } finally {
    isLoading.value = false;
  }
};

// 加载更多
const loadMore = () => {
  if (hasMore.value && !isLoading.value) {
    pageNo.value++; // 页码自增
    loadAppList(false); // 加载下一页数据
  }
};

// 是否有更多数据
const hasMore = computed(() => {
  return pageNo.value < totalPages.value;
});

// 是否应该显示整个区块
const shouldShowSection = computed(() => userStore.isLoggedIn);

// 组件挂载时加载数据
onMounted(() => {
  if (isGeneratingRequirements && isGeneratingRequirements.value) {
    return;
  }
  loadAppList(true);
  hasLoadedOnce.value = true;
});

// 监听生成状态
watch(() => isGeneratingRequirements?.value, (newVal, oldVal) => {
  if (oldVal === true && newVal === false && !hasLoadedOnce.value) {
    loadAppList(true);
    hasLoadedOnce.value = true;
  }
});

// 监听筛选类型变化，重新加载数据
watch(activeCategory, () => {
  loadAppList(true);
});

// 监听登录状态变化，当从未登录变为已登录时，刷新数据
watch(() => userStore.isLoggedIn, (newVal, oldVal) => {
  // 如果从未登录变为已登录，且当前在首页，则刷新数据
  if (oldVal === false && newVal === true) {
    const currentPath = router.currentRoute.value.path;
    // 检查是否在首页（路径为 '/' 或 '/home'）
    if (currentPath === '/' || currentPath === '/home') {
      loadAppList(true);
    }
  }
});
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';

.my-apps {
  padding: $spacing-xxl 0 $spacing-lg 0;
  background-color: #ffffff;
  position: relative;

  // 添加顶部阴影，与上方对话框区分
  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    height: 1px;
    background: rgba(0, 0, 0, 0.05);
    box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
  }
}

.section-header {
  margin-bottom: $spacing-xl;

  .section-title {
    font-size: 24px;
    font-weight: 700;
    color: $text-primary;
    margin: 0 0 $spacing-xs 0;
  }

  .section-subtitle {
    font-size: $font-size-sm;
    color: $text-secondary;
    margin: 0;
    opacity: 0.8;
  }
}

.filter-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30px;
  gap: $spacing-lg;
  flex-wrap: wrap;
}

.filter-left {
  display: flex;
  align-items: center;
  gap: $spacing-lg;
  flex-wrap: wrap;
}

.filter-right {
  display: flex;
  align-items: center;
}

.view-more-link {
  color: #1890ff;
  font-size: 14px;
  text-decoration: none;
  cursor: pointer;
  transition: color 0.3s ease;

  &:hover {
    color: #0d7cd9;
    text-decoration: underline;
  }
}

.category-filters {
  display: flex;
  gap: $spacing-lg;
  flex-wrap: wrap;
}

.filter-btn {
  padding: 7px 15px;
  background-color: $bg-primary;
  border: 1px solid $border-color;
  line-height: 22px;
  border-radius: 19px;
  font-size: $font-size-sm;
  color: $text-secondary;
  cursor: pointer;
  transition: all $transition-base;
  white-space: nowrap;

  &:hover {
    border-color: #3887F9;
    color: #3887F9;
    background-color: rgba(#3887F9, 0.05);
  }

  &.active {
    background: #3887F9;
    color: $text-white;
    border-color: $primary-color;
  }
}

.my-apps-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}

.my-app-card {
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s ease;
  border-radius: 12px;
  background: white;
  border: 1px solid #E5E7EB;
  box-shadow: none;
  padding: 0;

  &:hover {
    transform: translateY(-4px);
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.12);

    .app-overlay {
      opacity: 1;
    }
  }
}

.app-image {
  position: relative;
  width: 100%;
  height: 200px;
  overflow: hidden;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.app-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.6);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.3s ease;

  .view-detail-btn {
    padding: 10px 24px;
    background-color: #3887F9;
    color: #fff;
    border: none;
    border-radius: 20px;
    font-size: 14px;
    font-weight: 500;
    cursor: pointer;
    transition: all 0.3s ease;
  }
}

.app-info {
  padding: 16px;
  background: #fff;
}

.app-name {
  font-size: 16px;
  font-weight: 600;
  color: #333;
  margin: 0 0 8px 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.app-description {
  font-size: 14px;
  color: #666;
  line-height: 1.5;
  margin-bottom: 12px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  min-height: 42px;
}

.app-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.app-type-badge {
  padding: 0 6px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
  white-space: nowrap;
}

.app-time {
  font-size: 12px;
  color: #999;
  white-space: nowrap;
}

.load-more {
  text-align: center;
  margin-top: $spacing-xl;

  .load-more-link {
    background: transparent;
    border: none;
    color: #3b82f6;
    font-size: 14px;
    padding: 6px 8px;
    cursor: pointer;
    transition: color $transition-base;
  }

  .load-more-link:hover:not(:disabled) {
    color: darken(#3b82f6, 8%);
  }

  .load-more-link:disabled {
    color: #9ca3af;
    cursor: not-allowed;
  }

  .no-more-text {
    color: $text-light;
    font-size: $font-size-sm;
    padding: $spacing-md 0;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: $spacing-xs;

    &::before,
    &::after {
      content: '';
      width: 40px;
      height: 1px;
      background: linear-gradient(to right, transparent, $border-color, transparent);
    }

    .total-count {
      opacity: 0.7;
    }
  }
}

.empty-state,
.loading-state,
.error-state {
  text-align: center;
  padding: $spacing-xxl * 2 0;

  .empty-text,
  .loading-text,
  .error-text {
    color: $text-secondary;
    font-size: $font-size-md;
    padding: $spacing-xl;
    background: $bg-secondary;
    border-radius: $border-radius-lg;
    display: inline-block;
  }
}

.error-state {
  .error-text {
    background: rgba(255, 82, 82, 0.1);
    color: #ff5252;
    margin-bottom: $spacing-md;
  }

  .retry-btn {
    margin-top: $spacing-md;
    padding: $spacing-sm $spacing-xl;
  }
}

.loading-state {
  .loading-text {
    position: relative;
    padding-left: $spacing-xxl;

    &::before {
      content: '';
      position: absolute;
      left: $spacing-lg;
      top: 50%;
      transform: translateY(-50%);
      width: 16px;
      height: 16px;
      border: 2px solid $text-light;
      border-top-color: $primary-color;
      border-radius: 50%;
      animation: spin 0.8s linear infinite;
    }
  }
}

@keyframes spin {
  to {
    transform: translateY(-50%) rotate(360deg);
  }
}

@media (max-width: 1200px) {
  .my-apps-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (max-width: 768px) {
  .my-apps-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 16px;
  }

  .filter-bar {
    flex-direction: column;
    align-items: flex-start;
  }

  .filter-right {
    width: 100%;
    justify-content: flex-end;
  }
}

@media (max-width: $breakpoint-mobile) {
  .my-apps-grid {
    grid-template-columns: 1fr;
  }

  .app-image {
    height: 160px;
  }
}
</style>
