<template>
    <section class="cases-section">
        <div class="container">
            <div class="filter-bar fade-in">
                <div class="category-filters">
                    <button v-for="category in categories" :key="category"
                        :class="['filter-btn', { active: activeCategory === category }]"
                        @click="activeCategory = category">
                        {{ category }}
                    </button>
                </div>
            </div>

            <div class="cases-grid">
                <div v-for="(caseItem, index) in cases" :key="caseItem.id || index" class="case-card card fade-in"
                    :style="{ animationDelay: `${index * 0.05}s` }">
                    <div class="case-image" :style="getImageStyle(caseItem)">
                        <div class="case-overlay">
                            <button class="view-detail-btn" @click.stop="previewApp(caseItem)">查看详情 »</button>
                        </div>
                    </div>
                    <div class="case-content">
                        <h3 class="case-title">{{ caseItem.appName }}</h3>
                        <p class="case-description">
                            {{ caseItem.appDescription || getAppDescription(caseItem.appType) }}
                        </p>
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

            <div v-if="cases.length === 0 && !isLoading" class="empty-state fade-in">
                <p class="empty-text">暂无应用案例</p>
            </div>

            <div class="load-more fade-in" v-if="hasMore">
                <button class="btn btn-secondary" @click="loadMore" :disabled="isLoading">
                    {{ isLoading ? '加载中...' : hasMore ? '加载更多' : '已加载全部' }}
                </button>
            </div>
        </div>
        <Login v-model:visible="showLoginModal" @success="handleLoginSuccess" />
    </section>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue';
import { ElMessage } from 'element-plus';
import { fetchAppList } from '@/apis';
import { useUserStore } from '@/stores/user';
import { uniqueBy } from '@/utils/arrayUtils';
import Login from '@/components/Login.vue';

const userStore = useUserStore();

// 登录相关
const showLoginModal = ref(false);
const pendingApp = ref(null); // 待打开的已发布应用

const categories = ref(['全部', 'HTML', 'VUE3', 'VUE3+Spring']);
const activeCategory = ref('全部');

const isLoading = ref(false);
const cases = ref([]);
const pageNo = ref(1);
const pageSize = ref(4); // 每页加载12条，参考设计展示
const totalPages = ref(0);
const total = ref(0);

const appTypeColors = {
    0: { background: '#D8E6FD', color: '#3B82F6' }, // HTML - 蓝色
    1: { background: '#F3E8FF', color: '#9333EA' }, // VUE3 - 紫色
    2: { background: '#E3FFEC', color: '#2EB85C' }  // VUE3-Spring - 绿色
};

const appTypeLabels = {
    0: 'HTML',
    1: 'VUE3',
    2: 'VUE3+Spring'
};

const appTypeDescriptions = {
    0: '基于 HTML 技术栈开发的轻量级应用',
    1: '使用 Vue3 框架构建的现代化前端应用',
    2: 'Vue3 前端 + Spring 后端的全栈应用'
};

const categoryToAppType = (category) => {
    const map = {
        '全部': null,
        'HTML': 0,
        'VUE3': 1,
        'VUE3+Spring': 2
    };
    return map[category];
};

const getImageStyle = (item) => {
    if (item.appScreenshot) {
        return {
            backgroundImage: `url(${item.appScreenshot})`,
            backgroundSize: 'cover',
            backgroundPosition: 'center',
            backgroundRepeat: 'no-repeat'
        };
    }
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

const getAppTypeColor = (appType) => {
    return appTypeColors[appType] || { background: '#3B82F6', color: '#3B82F6' };
};

const getAppTypeLabel = (appType) => {
    return appTypeLabels[appType] || '未知';
};

const getAppDescription = (appType) => {
    return appTypeDescriptions[appType] || '使用 Flash Code 生成的应用';
};

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

// 广场里的应用都已发布，直接在新标签页打开可用地址，不进编辑页
const openApp = (item) => {
    if (!item?.appUrl) {
        ElMessage.warning('该应用暂时无法访问');
        return;
    }
    window.open(item.appUrl, '_blank');
};

const previewApp = (item) => {
    // 检查是否已登录
    if (!userStore.isLoggedIn) {
        // 未登录，记住这条案例并显示登录窗口
        pendingApp.value = item;
        showLoginModal.value = true;
        return;
    }

    openApp(item);
};

// 登录成功后的处理
const handleLoginSuccess = () => {
    // 如果有待打开的应用，登录成功后打开
    if (pendingApp.value) {
        openApp(pendingApp.value);
        pendingApp.value = null;
    }
};

const hasMore = computed(() => {
    return pageNo.value < totalPages.value;
});

const loadAppList = async (reset = false) => {
    if (isLoading.value) return;
    try {
        isLoading.value = true;
        if (reset) {
            pageNo.value = 1;
            cases.value = [];
        }
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
        }
    } catch (error) {
        console.error('加载应用列表失败:', error);
    } finally {
        isLoading.value = false;
    }
};

const loadMore = () => {
    if (hasMore.value && !isLoading.value) {
        pageNo.value += 1;
        loadAppList(false);
    }
};

onMounted(() => {
    loadAppList(true);
});

watch(activeCategory, () => {
    loadAppList(true);
});
</script>

<style scoped lang="scss">
.cases-section {
    padding: 36px 0 64px 0;
    background-color: #fff;
}

.container {
    max-width: 1200px;
    margin: 0 auto;
    padding: 0 24px;
}

.filter-bar {
    display: flex;
    justify-content: flex-start;
    align-items: center;
    margin-bottom: 24px;
}

.category-filters {
    display: flex;
    gap: 12px;
    flex-wrap: wrap;
}

.filter-btn {
    padding: 8px 18px;
    background-color: #f5f6fa;
    border: 1px solid #e5e7eb;
    border-radius: 16px;
    font-size: 14px;
    color: #3a456c;
    cursor: pointer;
    transition: all 0.2s ease;
    min-width: 68px;
}

.filter-btn:hover {
    color: #3b82f6;
    border-color: #3b82f6;
}

.filter-btn.active {
    background: #3b82f6;
    color: #fff;
    border-color: #3b82f6;
}

.cases-grid {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 20px;
    margin-bottom: 32px;
}

.case-card {
    overflow: hidden;
    cursor: pointer;
    transition: all 0.3s ease;
    background: #fff;
    border-radius: 12px;
    border: 1px solid #e5e7eb;
    padding: 0;
}

.case-card:hover {
    transform: translateY(-4px);
    box-shadow: 0 8px 20px rgba(0, 0, 0, 0.12);
}

.case-card:hover .case-overlay {
    opacity: 1;
}

.case-image {
    position: relative;
    height: 200px;
    overflow: hidden;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.case-overlay {
    position: absolute;
    inset: 0;
    background-color: rgba(0, 0, 0, 0.6);
    display: flex;
    align-items: center;
    justify-content: center;
    opacity: 0;
    transition: opacity 0.25s ease;
}

.view-detail-btn {
    padding: 10px 24px;
    background-color: #3887f9;
    color: #fff;
    border: none;
    border-radius: 20px;
    font-size: 14px;
    font-weight: 500;
    cursor: pointer;
    transition: all 0.3s ease;
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

.empty-state {
    text-align: center;
    padding: 40px 0;
}

.empty-text {
    color: #6b7280;
    font-size: 15px;
}

.load-more {
    text-align: center;
}

.btn {
    padding: 10px 28px;
    border-radius: 20px;
    border: 1px solid #3b82f6;
    background: #fff;
    color: #3b82f6;
    font-size: 14px;
    cursor: pointer;
    transition: all 0.2s ease;
}

.btn:hover {
    background: #3b82f6;
    color: #fff;
}

.btn:disabled {
    opacity: 0.6;
    cursor: not-allowed;
}

@media (max-width: 1200px) {
    .cases-grid {
        grid-template-columns: repeat(3, 1fr);
    }
}

@media (max-width: 900px) {
    .cases-grid {
        grid-template-columns: repeat(2, 1fr);
        gap: 16px;
    }
}

@media (max-width: 600px) {
    .cases-grid {
        grid-template-columns: 1fr;
    }
}
</style>
