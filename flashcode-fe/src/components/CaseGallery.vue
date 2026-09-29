<template>
  <section v-if="shouldShowSection" class="case-gallery" id="cases">
    <div class="container">
      <div class="section-header fade-in">
        <h2 class="section-title">案例广场</h2>
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
        <div class="filter-right">
          <a href="#cases" class="view-more-link" @click.prevent="handleViewMore">查看更多 »</a>
        </div>
      </div>

      <div class="cases-grid">
        <div v-for="(caseItem, index) in cases" :key="caseItem.id" class="case-card card fade-in"
          :style="{ animationDelay: `${index * 0.1}s` }">
          <div class="case-image" :style="getImageStyle(caseItem)">
            <div class="case-overlay">
              <button class="view-detail-btn" @click.stop="previewApp(caseItem)">查看详情 »</button>
            </div>
          </div>
          <div class="case-content">
            <h3 class="case-title">{{ caseItem.appName }}</h3>
            <p class="case-description">{{ caseItem.appDescription || getAppDescription(caseItem.appType) }}</p>
            <div class="case-footer">
              <span class="case-badge" :style="{
                background: getAppTypeColor(caseItem.appType).background,
                color: getAppTypeColor(caseItem.appType).color
              }">
                {{ getAppTypeLabel(caseItem.appType) }}
              </span>
              <span class="case-date">{{ formatDate(caseItem) }}</span>
            </div>
          </div>
        </div>
      </div>

      <div v-if="hasMore && isLoading" class="load-more fade-in">
        <button class="btn btn-secondary" @click="loadMore" :disabled="isLoading">
          {{ isLoading ? '加载中...' : '加载更多' }}
        </button>
      </div>
      <!-- <div v-else-if="cases.length > 0" class="load-more fade-in">
        <p class="no-more-text">已加载全部应用</p>
      </div> -->
      <div v-if="cases.length === 0 && !isLoading" class="empty-state fade-in">
        <p class="empty-text">{{ activeCategory === '全部' ? '暂无应用案例' : `「${activeCategory}」分类下暂无应用案例` }}</p>
      </div>
    </div>
    <Login v-model:visible="showLoginModal" @success="handleLoginSuccess" />
  </section>
</template>

<script setup>
import { ref, computed, onMounted, inject, watch } from 'vue';
import { useRouter } from 'vue-router';
import { fetchAppList } from '@/apis';
import { useUserStore } from '@/stores/user';
import { uniqueBy } from '@/utils/arrayUtils';
import Login from '@/components/Login.vue';

const router = useRouter();
const userStore = useUserStore();

// 获取父组件提供的状态（如果存在）
const isGeneratingRequirements = inject('isGeneratingRequirements', null);
const hasLoadedOnce = ref(false); // 标记是否已经加载过一次

// 登录相关
const showLoginModal = ref(false);
const pendingAppId = ref(null); // 待跳转的应用ID

const activeCategory = ref('全部');
const sortType = ref('default');
const isLoading = ref(false);
const cases = ref([]);
const pageNo = ref(1);
const pageSize = ref(4); // 每次加载3条
const total = ref(0);
const totalPages = ref(0);
const loadError = ref(false); // 加载错误状态

const categories = ref([
  '全部',
  'HTML',
  'VUE3',
  'VUE3-Spring'
]);

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
  2: 'VUE3+Spring'
};

// 应用类型描述映射（后端：0=HTML, 1=VUE, 2=VUE_SPRING）
const appTypeDescriptions = {
  0: '基于 HTML 技术栈开发的轻量级应用',
  1: '使用 Vue3 框架构建的现代化前端应用',
  2: 'Vue3 前端 + Spring 后端的全栈应用'
};

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
    'VUE3-Spring': 2
  };
  return categoryToTypeMap[category];
};

// 获取作者首字母
const getAuthorInitial = (userName) => {
  if (!userName) return '?';
  return userName.charAt(0).toUpperCase();
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

// 预览应用
const previewApp = (item) => {
  // 检查是否已登录
  if (!userStore.isLoggedIn) {
    // 未登录，保存应用ID并显示登录窗口
    pendingAppId.value = item.id;
    showLoginModal.value = true;
    return;
  }

  // 已登录，直接跳转到应用详情页
  router.push({
    name: 'app',
    params: { id: item.id }
  });
};

// 登录成功后的处理
const handleLoginSuccess = () => {
  // 如果有待跳转的应用ID，登录成功后跳转
  if (pendingAppId.value) {
    router.push({
      name: 'app',
      params: { id: pendingAppId.value }
    });
    pendingAppId.value = null;
  }
};

// 查看更多
const handleViewMore = () => {
  // 跳转到案例广场页面（可以通过路由或滚动到页面顶部）
  router.push({ name: 'cases' }).catch(() => {
    // 如果路由不存在，滚动到当前页面的案例广场区域
    const element = document.getElementById('cases');
    if (element) {
      element.scrollIntoView({ behavior: 'smooth' });
    }
  });
};

// 加载应用列表
const loadAppList = async (reset = false) => {
  if (isLoading.value) return;

  try {
    isLoading.value = true;

    if (reset) {
      pageNo.value = 1;
      cases.value = [];
    }

    // 获取当前筛选的应用类型
    const appType = categoryToAppType(activeCategory.value);


    const result = await fetchAppList({
      pageNo: pageNo.value,
      pageSize: pageSize.value,
      ...(appType !== null ? { appType } : {}),
    });

    if (result && result.list) {
      if (reset) {
        cases.value = result.list;
      } else {
        // 合并并去重，防止重复数据
        const merged = [...cases.value, ...result.list];
        cases.value = uniqueBy(merged, 'id');
      }

      total.value = result.totals || 0;
      totalPages.value = result.totalPages || 0;
      pageNo.value = result.pageNo || pageNo.value;
      loadError.value = false; // 加载成功，清除错误状态
    }
  } catch (error) {
    console.error('加载应用列表失败:', error);
    loadError.value = true; // 设置错误状态
  } finally {
    isLoading.value = false;
  }
};

// 加载更多
const loadMore = () => {
  if (hasMore.value && !isLoading.value) {
    pageNo.value++;
    loadAppList(false);
  }
};

// 是否有更多数据
const hasMore = computed(() => {
  return pageNo.value < totalPages.value;
});

// 是否应该显示整个区块
const shouldShowSection = computed(() => {
  // 如果正在加载第一页数据，显示区块
  if (isLoading.value && cases.value.length === 0) {
    return true;
  }
  // 仅在有加载错误时隐藏区块；当前分类下没数据时仍显示区块，由空状态展示
  return !loadError.value;
});

// 组件挂载时加载数据
onMounted(() => {
  // 如果正在生成需求文档，则不自动加载列表
  if (isGeneratingRequirements && isGeneratingRequirements.value) {
    return;
  }
  loadAppList(true);
  hasLoadedOnce.value = true;
});

// 监听生成状态，如果生成完成且还没有加载过列表，则加载列表
watch(() => isGeneratingRequirements?.value, (newVal, oldVal) => {
  // 如果从生成中变为未生成，且还没有加载过列表，则加载列表
  if (oldVal === true && newVal === false && !hasLoadedOnce.value) {
    loadAppList(true);
    hasLoadedOnce.value = true;
  }
});

// 监听筛选类型变化，重新加载数据
watch(activeCategory, () => {
  loadAppList(true);
});
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';

.case-gallery {
  padding: $spacing-lg 0 $spacing-xxl 0;
  background-color: #ffffff;
  border-top: 1px solid rgba(0, 0, 0, 0.05);
}

.section-header {
  margin-bottom: $spacing-xl;

  .section-title {
    font-size: 24px;
    font-weight: 700;
    color: $text-primary;
    margin: 0;
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

.sort-select {
  .sort-dropdown {
    padding: $spacing-sm $spacing-md;
    background-color: $bg-primary;
    border: 1px solid $border-color;
    border-radius: $border-radius;
    font-size: $font-size-md;
    color: $text-primary;
    cursor: pointer;
    outline: none;
    transition: all $transition-base;

    &:hover {
      border-color: $primary-color;
    }

    &:focus {
      border-color: $primary-color;
    }
  }
}

.category-filters {
  display: flex;
  gap: $spacing-lg;
  flex-wrap: wrap;
  justify-content: center;
  width: 100%;
}

.filter-btn {
  padding: 7px 15px;
  background-color: $bg-primary;
  border: 1px solid $border-color;
  border-radius: 19px;
  font-size: $font-size-sm;
  color: $text-secondary;
  cursor: pointer;
  transition: all $transition-base;
  white-space: nowrap;

  &:hover {
    border-color: $primary-color;
    color: $primary-color;
    background-color: rgba($primary-color, 0.05);
  }

  &.active {
    background: #3887F9;
    color: $text-white;
    border-color: $primary-color;
  }
}

.view-all-btn {
  display: flex;
  align-items: center;
  gap: $spacing-xs;
  padding: $spacing-xs $spacing-md;
  background-color: transparent;
  border: 1px solid $border-color;
  border-radius: $border-radius;
  font-size: $font-size-sm;
  color: $text-secondary;
  cursor: pointer;
  transition: all $transition-base;
  white-space: nowrap;

  .arrow {
    font-size: $font-size-xs;
  }

  &:hover {
    border-color: $primary-color;
    color: $primary-color;
    background-color: rgba($primary-color, 0.05);
  }
}

.cases-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  margin-bottom: $spacing-xxl;
}

.case-card {
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s ease;
  background: #fff;
  border-radius: 12px;
  border: 1px solid #E5E7EB;
  box-shadow: none;
  padding: 0;

  &:hover {
    transform: translateY(-4px);
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.12);

    .case-overlay {
      opacity: 1;
    }
  }
}

.case-image {
  position: relative;
  height: 200px;
  overflow: hidden;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
}

.case-overlay {
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
  transition: opacity $transition-base;

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

.case-content {
  padding: 16px;
  background: #fff;
}

.case-title {
  font-size: 16px;
  font-weight: 600;
  color: #333;
  margin: 0 0 8px 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.case-description {
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

.case-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.case-badge {
  padding: 0 6px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
  white-space: nowrap;
}

.case-date {
  font-size: 12px;
  color: #999;
  white-space: nowrap;
}

.case-stats {
  display: flex;
  gap: $spacing-md;

  .stat {
    display: flex;
    align-items: center;
    gap: $spacing-xs;
    font-size: $font-size-sm;
    color: $text-light;

    svg {
      opacity: 0.7;
    }
  }
}

.load-more {
  text-align: center;
  margin-top: $spacing-xl;

  .btn {
    padding: $spacing-md $spacing-xxl;

    &:disabled {
      opacity: 0.6;
      cursor: not-allowed;
    }
  }

  .no-more-text {
    color: $text-light;
    font-size: $font-size-sm;
  }
}

.empty-state {
  text-align: center;
  padding: $spacing-xxl * 2 0;

  .empty-text {
    color: $text-secondary;
    font-size: $font-size-lg;
  }
}

@media (max-width: 1200px) {
  .cases-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (max-width: 768px) {
  .cases-grid {
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
  .cases-grid {
    grid-template-columns: 1fr;
  }
}
</style>
