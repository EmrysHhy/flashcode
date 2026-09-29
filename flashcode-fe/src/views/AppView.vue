<template>
  <div class="app-detail-view">
    <!-- Header -->
    <Header v-if="!isVscodeMode" />

    <!-- Main Content -->
    <main class="main-content" :class="{ 'vscode-fullscreen': isVscodeMode }">
      <div class="content-wrapper" :class="{ 'vscode-fullscreen': isVscodeMode }">
        <div class="container-fluid" :class="{ 'vscode-fullscreen': isVscodeMode }">
          <!-- 加载状态 -->
          <div v-if="isLoading" class="loading-container">
            <div class="loading-spinner"></div>
            <p class="loading-text">加载中...</p>
          </div>

          <!-- 错误状态 -->
          <div v-else-if="error" class="error-container">
            <div class="error-icon">⚠️</div>
            <p class="error-text">{{ error }}</p>
            <button class="btn btn-primary" @click="goBack">返回</button>
          </div>

          <!-- 应用详情 -->
          <div v-else-if="appDetail" class="detail-container">
            <div class="detail-grid"
              :class="{ 'vscode-fullscreen': isVscodeMode, 'code-edit-mode': activeTab === 'code' }">
              <!-- 左侧：应用预览或需求文档 -->
              <PreviewPanel ref="previewPanelRef" :app-detail="appDetail" :requirement-document="requirementDocument"
                :selected-element-selector="selectedElementSelector" :active-tab="activeTab"
                @update:app-detail="handleUpdateAppDetail"
                @update:selected-element-selector="handleUpdateSelectedElementSelector"
                @update:is-editing-mode="handleUpdateIsEditingMode" @update:is-vscode-mode="handleUpdateIsVscodeMode"
                @update:active-tab="handleUpdateActiveTab" @refresh-preview="handleRefreshPreview" />

              <!-- 右侧：对话框 -->
              <ChatPanel :app-id="appId" :app-detail="appDetail" :requirement-document="requirementDocument"
                :is-editing-mode="isEditingMode" :selected-element-selector="selectedElementSelector"
                :is-vscode-mode="isVscodeMode" @update:app-detail="handleUpdateAppDetail"
                @update:selected-element-selector="handleUpdateSelectedElementSelector"
                @refresh-preview="handleRefreshPreview" @generate-app="handleGenerateApp"
                @load-app-detail="handleLoadAppDetail" @update:active-tab="handleUpdateActiveTab" />
            </div>
          </div>
        </div>
      </div>
    </main>

  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import Header from '@/components/Header.vue';
import PreviewPanel from '@/components/appViewComp/PreviewPanel.vue';
import ChatPanel from '@/components/appViewComp/ChatPanel.vue';
import { fetchAppDetail } from '@/apis';

const route = useRoute();
const router = useRouter();

// 状态
const isLoading = ref(true);
const error = ref(null);
const appDetail = ref(null);
const isVscodeMode = ref(false);

// PreviewPanel 引用
const previewPanelRef = ref(null);

// 编辑状态（用于组件间通信）
const isEditingMode = ref(false);
const selectedElementSelector = ref('');
const activeTab = ref('preview'); // 'preview' | 'document' | 'code'

// 从路由参数获取 appId
const appId = computed(() => route.params.id);

// 需求文档内容（从 ChatPanel 获取，这里先设为空，实际由 ChatPanel 管理）
const requirementDocument = ref('');

// 加载应用详情
const loadAppDetail = async () => {
  // 首先检查是否有从ChatBox传递过来的数据
  const appViewDataStr = sessionStorage.getItem('appViewData');
  if (appViewDataStr) {
    try {
      const appViewData = JSON.parse(appViewDataStr);

      if (appViewData.document) {
        requirementDocument.value = appViewData.document;
      }

      sessionStorage.removeItem('appViewData');

      if (appId.value) {
        await loadAppDetailFromAPI();
      } else {
        isLoading.value = false;
      }
      return;
    } catch (err) {
      console.error('解析appViewData失败:', err);
    }
  }

  if (!appId.value) {
    error.value = '应用ID不存在';
    isLoading.value = false;
    return;
  }

  await loadAppDetailFromAPI();
};

// 从API加载应用详情
const loadAppDetailFromAPI = async () => {
  try {
    isLoading.value = true;
    error.value = null;

    const data = await fetchAppDetail({ appId: appId.value });
    appDetail.value = data;

    // 从详情接口的 appDoc 字段获取需求文档
    if (data && data.appDoc) {
      requirementDocument.value = data.appDoc;
    }
  } catch (err) {
    console.error('加载应用详情失败:', err);
    error.value = err.message || '加载应用详情失败，请稍后重试';
  } finally {
    isLoading.value = false;
  }
};

// 处理更新应用详情
const handleUpdateAppDetail = (newAppDetail) => {
  appDetail.value = newAppDetail;
};

// 处理更新选中的元素选择器
const handleUpdateSelectedElementSelector = (selector) => {
  selectedElementSelector.value = selector;
};

// 处理更新编辑模式
const handleUpdateIsEditingMode = (mode) => {
  isEditingMode.value = mode;
};

// 处理更新 VSCode 模式
const handleUpdateIsVscodeMode = (mode) => {
  isVscodeMode.value = mode;
};

// 处理更新激活的 Tab
const handleUpdateActiveTab = (tab) => {
  activeTab.value = tab;
};

// 处理刷新预览
const handleRefreshPreview = (forceReload = false) => {
  // 调用 PreviewPanel 的刷新方法
  if (previewPanelRef.value && previewPanelRef.value.refreshPreview) {
    previewPanelRef.value.refreshPreview(forceReload);
  }
};

// 处理生成应用
const handleGenerateApp = (newAppId) => {
  if (newAppId) {
    // 更新路由或重新加载应用详情
    router.push(`/app/${newAppId}`);
  }
};

// 处理加载应用详情
const handleLoadAppDetail = () => {
  loadAppDetail();
};

// 返回
const goBack = () => {
  router.push('/');
};

// 组件挂载时加载数据
onMounted(() => {
  loadAppDetail();
});
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';

.app-detail-view {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background-color: $bg-secondary;
  overflow: hidden;
}

.main-content {
  // 当有 Header 时，高度应该是 100vh - 64px (header 高度)
  height: calc(100vh - 64px);
  overflow: hidden;
  display: flex;
  flex-direction: column;

  // VSCode 模式下，没有 Header，高度应该是 100vh
  &.vscode-fullscreen {
    height: 100vh;
  }
}

.content-wrapper {
  flex: 1;
  padding: 20px 0;
  overflow: hidden;
  display: flex;
  flex-direction: column;

  &.vscode-fullscreen {
    padding: 0;
  }
}

.container-fluid {
  padding: 0 20px 0 16px;
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;

  &.vscode-fullscreen {
    max-width: 100%;
    margin: 0;
    padding: 0;
  }
}

.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  gap: $spacing-lg;
}

.loading-spinner {
  width: 48px;
  height: 48px;
  border: 4px solid $border-color;
  border-top-color: $primary-color;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

.loading-text {
  font-size: $font-size-lg;
  color: $text-secondary;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.error-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  gap: $spacing-lg;
  text-align: center;
}

.error-icon {
  font-size: 64px;
}

.error-text {
  font-size: $font-size-lg;
  color: #ff5252;
  max-width: 500px;
}

.detail-container {
  animation: fadeIn 0.4s ease;
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.detail-grid {
  display: grid;
  grid-template-columns: 1fr minmax(0, 496px);
  gap: 12px;
  height: 100%;
  overflow: hidden;

  &.vscode-fullscreen {
    grid-template-columns: 1fr;
    gap: 0;
  }

  // 代码编辑模式下，右侧宽度为 396px
  &.code-edit-mode {
    grid-template-columns: 1fr minmax(0, 396px);
  }
}

.btn {
  padding: $spacing-sm $spacing-md;
  border-radius: $border-radius;
  font-size: $font-size-sm;
  font-weight: 500;
  cursor: pointer;
  transition: all $transition-base;
  border: none;
  display: inline-flex;
  align-items: center;
  gap: $spacing-xs;

  &.btn-primary {
    background: $gradient-primary;
    color: $text-white;

    &:hover:not(:disabled) {
      transform: translateY(-2px);
      box-shadow: $shadow-md;
    }

    &:disabled {
      opacity: 0.6;
      cursor: not-allowed;
    }
  }
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(20px);
  }

  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@media (max-width: $breakpoint-tablet) {
  .detail-grid {
    grid-template-columns: 1fr;
    gap: $spacing-lg;

    .preview-panel,
    .chat-panel {
      min-height: 0;
      height: 50%;
    }
  }
}
</style>
