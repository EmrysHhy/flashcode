<template>
  <div class="preview-panel-wrapper">
    <!-- 左侧 Tab 栏 -->
    <div class="sidebar-tabs">
      <!-- 应用预览按钮 -->
      <button v-if="hasPreview" class="tab-item" @click="switchTab('preview')" title="应用预览">
        <img :src="activeTab === 'preview' ? appIconActive : appIcon" alt="应用预览" class="tab-icon" />
      </button>
      <!-- 需求文档按钮 -->
      <button class="tab-item" @click="switchTab('document')" title="需求文档">
        <img :src="activeTab === 'document' ? docIconActive : docIcon" alt="需求文档" class="tab-icon" />
      </button>
      <!-- 代码编辑按钮 -->
      <button v-if="hasPreview" class="tab-item" @click="switchTab('code')" title="代码编辑">
        <img :src="activeTab === 'code' ? codeIconActive : codeIcon" alt="代码编辑" class="tab-icon" />
      </button>
    </div>

    <div class="preview-panel">
      <div class="panel-content">
        <div v-if="activeTab === 'preview' || activeTab === 'code'" class="panel-header">
          <div class="panel-title-wrapper">
            <h3 class="panel-title">
              {{ activeTab === 'preview' ? '应用预览' : '代码编辑' }}
            </h3>
            <span v-if="appDetail?.appType !== null && appDetail?.appType !== undefined" class="app-type-badge" :style="{
              background: getAppTypeColor(appDetail.appType).background,
              color: getAppTypeColor(appDetail.appType).color
            }">
              {{ getAppTypeLabel(appDetail.appType) }}
            </span>
          </div>
          <div class="preview-controls">
            <button v-if="activeTab === 'preview'" class="btn btn-outline btn-sm" @click="handleRefreshPreview"
              title="刷新">
              刷新
            </button>
            <button v-if="activeTab === 'preview'" class="btn btn-outline btn-sm" @click="handleOpenInNewWindow"
              title="在新窗口打开">
              新窗口打开
            </button>
            <button v-if="activeTab === 'preview'" class="btn btn-outline btn-sm" @click="handleOpenVscode"
              :disabled="isOpeningVscode || isDeploying" title="高级编辑">
              {{ isOpeningVscode ? '打开中...' : '高级编辑' }}
            </button>
            <button v-if="activeTab === 'preview'" class="btn btn-outline btn-sm"
              :class="{ 'btn-active': isEditingMode }" @click="handleToggleEditMode" :disabled="isDeploying"
              title="可视化编辑">
              {{ isEditingMode ? '退出编辑' : '编辑' }}
            </button>
            <button v-if="activeTab === 'preview'" class="btn btn-primary btn-sm" @click="handleDeploy"
              :disabled="isDeploying" title="发布应用">
              {{ isDeploying ? '发布中...' : '发布' }}
            </button>
            <button v-if="activeTab === 'code'" class="btn btn-outline btn-sm" @click="handleExitAdvancedEdit"
              :disabled="isCompletingEdit" title="退出编辑">
              退出编辑
            </button>
            <button v-if="activeTab === 'code'" class="btn btn-primary btn-sm" @click="handleCompleteEdit"
              :disabled="isCompletingEdit || isDeploying" title="完成编辑">
              {{ isCompletingEdit ? '处理中...' : '完成编辑' }}
            </button>
          </div>
        </div>

        <!-- 应用预览 -->
        <div v-if="activeTab === 'preview' && hasPreview" class="preview-container">
          <div class="preview-iframe-wrapper" :class="{ 'editing-mode': isEditingMode }">
            <!-- Loading 效果 -->
            <div v-if="isPreviewLoading" class="preview-loading-container">
              <div class="loading-spinner"></div>
              <p class="loading-text">预览加载中...</p>
            </div>
            <iframe ref="previewIframe" :src="iframeUrl" :key="iframeKey" class="preview-iframe"
              :class="{ 'preview-loading': isPreviewLoading }" frameborder="0" allowfullscreen
              @load="handleIframeLoad"></iframe>
            <div v-if="isEditingMode" class="edit-mode-tip">
              <p>编辑模式：点击预览页面中的元素进行选择</p>
            </div>
          </div>
        </div>

        <!-- 需求文档 -->
        <div v-else-if="activeTab === 'document' && requirementDocument" class="document-content">
          <div v-html="formattedDocument"></div>
        </div>

        <!-- 代码编辑 -->
        <div v-else-if="activeTab === 'code' && hasPreview" class="code-editor-container">
          <div class="preview-container">
            <div class="preview-iframe-wrapper">
              <!-- VSCode 加载动画 -->
              <div v-if="isVscodeLoading" class="vscode-loading-container">
                <div class="loading-spinner"></div>
                <p class="loading-text">VSCode 加载中...</p>
              </div>
              <iframe v-if="vscodeUrl" ref="vscodeIframe" :src="vscodeUrl" class="preview-iframe"
                :class="{ 'vscode-loading': isVscodeLoading }" frameborder="0" allowfullscreen></iframe>
              <div v-else class="vscode-loading-container">
                <p class="loading-text">正在打开代码编辑器...</p>
              </div>
            </div>
          </div>
        </div>

        <!-- 什么都没有 -->
        <div v-else-if="activeTab === 'document' && !requirementDocument" class="no-document">
          <p>暂无内容</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onUnmounted, nextTick } from 'vue';
import { deployApp, createVscode, completeEdit } from '@/apis';
import { createElementSelectionHandler } from '@/utils/elementSelector.js';
import { formatMessageText } from '@/utils/messageFormatter.js';

// 图标资源
const appIcon = new URL('@/imges/appIcon.svg', import.meta.url).href;
const appIconActive = new URL('@/imges/appIcon_active.svg', import.meta.url).href;
const docIcon = new URL('@/imges/docIcon.svg', import.meta.url).href;
const docIconActive = new URL('@/imges/docIcon_active.svg', import.meta.url).href;
const codeIcon = new URL('@/imges/codeIcon.svg', import.meta.url).href;
const codeIconActive = new URL('@/imges/codeIcon_active.svg', import.meta.url).href;

const props = defineProps({
  appDetail: {
    type: Object,
    default: null
  },
  requirementDocument: {
    type: String,
    default: ''
  },
  selectedElementSelector: {
    type: String,
    default: ''
  },
  activeTab: {
    type: String,
    default: 'preview'
  }
});

const emit = defineEmits([
  'update:appDetail',
  'update:selectedElementSelector',
  'update:isEditingMode',
  'update:activeTab',
  'refresh-preview'
]);

// 状态
// iframeKey: 用于强制刷新 iframe，每次递增时会触发 Vue 重新渲染 iframe 元素
// 通过改变 key 值，Vue 会销毁旧 iframe 并创建新的，从而绕过浏览器缓存
const iframeKey = ref(0);
const previewIframe = ref(null);
const isPreviewLoading = ref(false);
let loadingTimeout = null; // 用于超时清除 loading

// 发布状态
const isDeploying = ref(false);

// VSCode 编辑状态
const isOpeningVscode = ref(false);
const vscodeUrl = ref(null);
const isCompletingEdit = ref(false);
const isVscodeLoading = ref(false);
const vscodeIframe = ref(null);

// 编辑状态
const isEditingMode = ref(false);
const elementSelectionHandler = ref(null);

// Tab 状态管理
// 使用 computed 实现双向绑定：优先使用父组件传入的 prop，如果没有则使用本地状态
// 这样既支持受控模式（父组件控制），也支持非受控模式（组件自己管理状态）
const localActiveTab = ref(props.activeTab !== undefined ? props.activeTab : 'preview');
const activeTab = computed({
  get: () => props.activeTab !== undefined ? props.activeTab : localActiveTab.value,
  set: (value) => {
    localActiveTab.value = value;
    emit('update:activeTab', value);
  }
});

// 应用类型颜色映射
// 用于在预览面板头部显示应用类型徽章的颜色
const appTypeColors = {
  0: { background: '#D8E6FD', color: '#3B82F6' }, // HTML - 蓝色
  1: { background: '#F3E8FF', color: '#9333EA' }, // VUE3 - 紫色
  2: { background: '#E3FFEC', color: '#2EB85C' }  // VUE3-Spring - 绿色
};

// 应用类型标签映射
// 应用类型的显示文本
const appTypeLabels = {
  0: 'HTML',
  1: 'VUE3',
  2: 'VUE3-Spring'
};

// 获取应用类型颜色
// 根据应用类型返回对应的背景色和文字颜色，如果类型未知则返回默认蓝色
const getAppTypeColor = (appType) => {
  return appTypeColors[appType] || { background: '#3B82F6', color: '#3B82F6' };
};

// 获取应用类型标签
// 根据应用类型返回对应的显示文本，如果类型未知则返回"未知"
const getAppTypeLabel = (appType) => {
  return appTypeLabels[appType] || '未知';
};

// 判断是否有预览
// 检查应用详情中是否存在预览 URL（appUrl 或 previewUrl）
const hasPreview = computed(() => {
  if (!props.appDetail) return false;
  return !!(props.appDetail.appUrl || props.appDetail.previewUrl);
});

// 切换 Tab
// 处理不同 tab 切换时的逻辑：
// - 切换到代码编辑：退出编辑模式（如果开启），自动打开 VSCode（如果未打开）
// - 切换到预览：保留 VSCode URL 但不显示（方便下次切换回来时快速加载）
const switchTab = (tab) => {
  activeTab.value = tab;
  // 如果切换到代码编辑，退出编辑模式
  if (tab === 'code') {
    if (isEditingMode.value) {
      handleToggleEditMode();
    }
    // 如果还没有打开 VSCode，自动打开
    if (!vscodeUrl.value && !isOpeningVscode.value && props.appDetail?.id) {
      handleOpenVscode();
    }
  } else if (tab === 'preview') {
    // 切换到预览时，可以保留 VSCode URL，但不显示
    // 不再需要退出 VSCode 模式，因为代码编辑模式不再全屏
  }
};

// 监听 activeTab prop 变化，同步到本地状态并触发刷新
// 当父组件改变 activeTab 时，同步本地状态
// 如果切换到预览 tab，自动刷新预览内容
watch(() => props.activeTab, (newVal, oldVal) => {
  if (newVal !== undefined && newVal !== localActiveTab.value) {
    localActiveTab.value = newVal;
    // 如果切换到预览 tab，触发刷新
    if (newVal === 'preview' && oldVal !== 'preview') {
      nextTick(() => {
        handleRefreshPreview(true);
      });
    }
  }
});

// 监听需求文档变化，如果没有预览则默认显示需求文档
// 当需求文档加载完成但应用预览不可用时，自动切换到文档 tab
watch(() => props.requirementDocument, (newVal) => {
  if (newVal && !hasPreview.value && activeTab.value === 'preview') {
    activeTab.value = 'document';
  }
}, { immediate: true });

// 监听预览状态变化
// 智能切换 tab：预览不可用时切换到文档，预览可用且当前在文档但文档为空时切换到预览
watch(hasPreview, (newVal) => {
  if (!newVal && activeTab.value === 'preview') {
    activeTab.value = 'document';
  } else if (newVal && activeTab.value === 'document' && !props.requirementDocument) {
    activeTab.value = 'preview';
  }
}, { immediate: true });

// 获取预览 URL
// 在开发环境中，如果预览 URL 指向代理目标服务器，将其转换为相对路径
// 这样可以通过 Vite 的代理配置访问，避免跨域问题
const previewUrl = computed(() => {
  if (!props.appDetail) return null;
  let baseUrl = props.appDetail.appUrl || props.appDetail.previewUrl;
  if (!baseUrl) return null;

  // 预览文件在 80 端口。改成当前页面下的 /preview，由 19080 的 nginx 再转发过去，编辑才能读到 iframe
  try {
    const url = new URL(baseUrl, window.location.origin);
    if (url.pathname.startsWith('/preview')) {
      baseUrl = url.pathname + url.search + url.hash;
    }
  } catch (err) {
    console.warn('预览 URL 格式异常:', err);
  }

  return baseUrl;
});

// 获取带时间戳的 iframe URL（每次访问都使用当前时间戳避免缓存）
// 通过添加 _t 查询参数并依赖 iframeKey，确保每次刷新时都生成新的 URL
// 这样可以绕过浏览器缓存，强制重新加载预览内容
const iframeUrl = computed(() => {
  if (!previewUrl.value) return null;
  
  // 移除已有的 _t 参数（如果有），避免重复添加
  let url = previewUrl.value.replace(/[?&]_t=\d+/g, '');
  const separator = url.includes('?') ? '&' : '?';
  
  // 依赖 iframeKey 确保每次 key 变化时都重新计算并生成新的时间戳
  // 这样每次刷新或 URL 变化时都会生成新的时间戳
  const _ = iframeKey.value; // 建立依赖关系，当 iframeKey 变化时重新计算
  return `${url}${separator}_t=${Date.now()}`;
});

// 格式化需求文档
// 使用统一的格式化工具函数，支持完整的 Markdown 语法（标题、列表、代码块、链接等）
const formattedDocument = computed(() => {
  return formatMessageText(props.requirementDocument);
});

// 处理 iframe 加载完成
const handleIframeLoad = () => {
  // 清除超时定时器
  if (loadingTimeout) {
    clearTimeout(loadingTimeout);
    loadingTimeout = null;
  }

  isPreviewLoading.value = false;

  // 如果处于编辑模式，启用元素选择
  if (isEditingMode.value) {
    nextTick(() => {
      enableElementSelection();
    });
  }
};

// 检查 iframe 是否已经加载完成（用于处理缓存情况）
// 当浏览器缓存了 iframe 内容时，可能不会触发 load 事件
// 此函数用于检测这种情况并手动调用 handleIframeLoad
const checkIframeLoaded = () => {
  if (!previewIframe.value || !previewUrl.value) return;

  nextTick(() => {
    try {
      // 尝试检查 iframe 是否已经加载完成（仅在同源情况下有效）
      if (previewIframe.value.contentDocument && previewIframe.value.contentDocument.readyState === 'complete') {
        handleIframeLoad();
      }
    } catch (err) {
      // 跨域情况下无法访问 contentDocument，忽略错误，等待 load 事件
    }
  });
};

// 设置 loading 超时保护
// 防止 iframe 加载失败或超时时 loading 状态一直显示
// 30秒后自动隐藏 loading，避免用户等待过久
const setLoadingTimeout = () => {
  // 清除之前的超时，避免多个超时定时器同时存在
  if (loadingTimeout) {
    clearTimeout(loadingTimeout);
  }

  // 设置新的超时（30秒）
  loadingTimeout = setTimeout(() => {
    if (isPreviewLoading.value) {
      console.warn('iframe 加载超时，隐藏 loading');
      isPreviewLoading.value = false;
      loadingTimeout = null;
    }
  }, 30000);
};

// 刷新预览
// forceReload: 是否强制重新加载（尝试直接调用 iframe 的 reload 方法）
// 如果跨域无法直接 reload，则通过改变 iframeKey 强制 Vue 重新创建 iframe
const handleRefreshPreview = (forceReload = false) => {
  if (isEditingMode.value) {
    disableElementSelection();
  }

  // 显示 loading
  isPreviewLoading.value = true;
  setLoadingTimeout();

  if (forceReload && previewIframe.value) {
    try {
      // 尝试直接刷新 iframe（仅在同源情况下有效）
      previewIframe.value.contentWindow?.location.reload(true);
    } catch (err) {
      // 跨域情况下无法直接刷新，使用 key 刷新方式
      console.log('无法直接刷新 iframe（跨域），使用 key 刷新方式');
      iframeKey.value++;
    }
  } else {
    // 通过改变 key 强制 Vue 重新创建 iframe，绕过浏览器缓存
    iframeKey.value++;
  }

  // 注意：不要在这里 emit('refresh-preview')，否则会形成循环调用
  // AppView 监听 refresh-preview 事件后会调用 previewPanelRef.value.refreshPreview
  // 如果这里再 emit，就会形成循环
};

// 在新窗口打开预览
// 在新标签页中打开预览 URL，方便用户独立查看
const handleOpenInNewWindow = () => {
  if (previewUrl.value) {
    window.open(previewUrl.value, '_blank');
  }
};

// 发布应用
// 调用发布 API，获取部署 URL 并在新窗口打开
const handleDeploy = async () => {
  if (!props.appDetail?.id || isDeploying.value) {
    return;
  }

  try {
    isDeploying.value = true;
    const data = await deployApp(props.appDetail.id);
    const deployUrl = typeof data === 'string' ? data : data?.url;
    if (deployUrl) {
      window.open(deployUrl, '_blank');
    }
  } catch (err) {
    console.error('发布应用失败:', err);
    alert(`发布失败：${err.message || '请稍后重试'}`);
  } finally {
    isDeploying.value = false;
  }
};

// 打开高级编辑（VSCode）
// 创建 VSCode 编辑环境并加载到 iframe 中
// 如果当前处于编辑模式，先退出编辑模式
const handleOpenVscode = async () => {
  if (!props.appDetail?.id || isOpeningVscode.value) {
    return;
  }

  // 先设置标志，防止 switchTab 中再次调用 handleOpenVscode（避免重复请求）
  isOpeningVscode.value = true;

  // 切换到代码编辑 tab
  switchTab('code');

  try {
    const data = await createVscode(props.appDetail.id);
    const rawUrl = typeof data === 'string' ? data : data?.url;
    const url = rawUrl && !/^https?:\/\//i.test(rawUrl) ? `http://${rawUrl}` : rawUrl;

    if (url) {
      if (isEditingMode.value) {
        handleToggleEditMode();
      }
      vscodeUrl.value = url;
      isVscodeLoading.value = true;
      // 确保切换到代码编辑 tab
      if (activeTab.value !== 'code') {
        activeTab.value = 'code';
        emit('update:activeTab', 'code');
      }

      await nextTick();
      // 设置 VSCode iframe 加载完成监听
      // 处理两种情况：同源（可直接检查 readyState）和跨域（只能监听 load 事件）
      if (vscodeIframe.value) {
        const iframe = vscodeIframe.value;
        const handleVscodeLoad = () => {
          isVscodeLoading.value = false;
        };
        
        try {
          // 尝试检查 iframe 是否已经加载完成（同源情况）
          if (iframe.contentDocument && iframe.contentDocument.readyState === 'complete') {
            isVscodeLoading.value = false;
          } else {
            // 监听 load 事件（跨域或未加载完成的情况）
            iframe.addEventListener('load', handleVscodeLoad, { once: true });
            // 设置超时保护，30秒后强制隐藏 loading
            setTimeout(() => {
              if (isVscodeLoading.value) {
                isVscodeLoading.value = false;
              }
            }, 30000);
          }
        } catch (err) {
          // 跨域情况下无法访问 contentDocument，只能监听 load 事件
          iframe.addEventListener('load', handleVscodeLoad, { once: true });
          setTimeout(() => {
            if (isVscodeLoading.value) {
              isVscodeLoading.value = false;
            }
          }, 30000);
        }
      }
    } else {
      throw new Error('未获取到 VSCode 编辑环境 URL');
    }
  } catch (err) {
    console.error('打开高级编辑失败:', err);
    alert(`打开高级编辑失败：${err.message || '请稍后重试'}`);
    vscodeUrl.value = null;
    isVscodeLoading.value = false;
    isOpeningVscode.value = false; // 重置标志，允许重试
  } finally {
    isOpeningVscode.value = false;
  }
};

// 退出高级编辑，只回到预览，不提交 VSCode 里的修改
const handleExitAdvancedEdit = () => {
  if (isCompletingEdit.value) {
    return;
  }
  switchTab('preview');
};

// 完成编辑
// 调用完成编辑 API，将 VSCode 中的修改同步回应用
// 完成后切换到预览 tab 并刷新预览内容
const handleCompleteEdit = async () => {
  if (!props.appDetail?.id || isCompletingEdit.value) {
    return;
  }

  try {
    isCompletingEdit.value = true;
    const result = await completeEdit(props.appDetail.id);

    if (result && result.url) {
      emit('update:appDetail', { ...props.appDetail, previewUrl: result.url });
      alert('完成编辑成功！');
      // 完成编辑后，切换到预览 tab
      switchTab('preview');
      vscodeUrl.value = null;
      isVscodeLoading.value = false;
      vscodeIframe.value = null;
      handleRefreshPreview(true);
    } else {
      throw new Error('完成编辑失败');
    }
  } catch (err) {
    console.error('完成编辑失败:', err);
    alert(`完成编辑失败：${err.message || '请稍后重试'}`);
  } finally {
    isCompletingEdit.value = false;
  }
};

// 切换编辑模式
// 开启/关闭可视化编辑模式，允许用户在预览页面中选择元素
const handleToggleEditMode = () => {
  isEditingMode.value = !isEditingMode.value;
  emit('update:isEditingMode', isEditingMode.value);

  if (isEditingMode.value) {
    enableElementSelection();
  } else {
    disableElementSelection();
    clearSelection();
  }
};

// 清除选择
// 清除当前选中的元素选择器，并通知父组件和元素选择处理器
const clearSelection = () => {
  emit('update:selectedElementSelector', '');
  if (elementSelectionHandler.value) {
    elementSelectionHandler.value.clearSelection();
  }
};

// 检查 iframe 是否同源
// 通过尝试访问 iframe 的 location.origin 来判断是否同源
// 如果跨域，访问会抛出异常，此时返回 false
// 同源检查是必要的，因为元素选择功能需要访问 iframe 内部的 DOM
const isIframeSameOrigin = (iframe) => {
  try {
    // 尝试访问 iframe 的 location，如果跨域会抛出异常
    const iframeOrigin = iframe.contentWindow.location.origin;
    const currentOrigin = window.location.origin;
    return iframeOrigin === currentOrigin;
  } catch (err) {
    // 如果抛出异常，说明跨域
    return false;
  }
};

// 启用元素选择
// 在预览页面上启用可视化编辑功能，允许用户点击选择页面元素
// 需要 iframe 同源才能访问其内部 DOM，跨域情况下会提示用户
const enableElementSelection = () => {
  if (!previewIframe.value) {
    console.warn('⚠️ enableElementSelection: previewIframe 不存在');
    return;
  }

  // 先检查是否同源（元素选择功能需要访问 iframe 内部的 DOM）
  if (!isIframeSameOrigin(previewIframe.value)) {
    console.warn('⚠️ 预览页面跨域，无法启用元素选择');
    alert('无法进入编辑模式：预览页面与当前页面不同源（跨域），无法访问其内容。\n\n解决方案：\n1. 确保预览页面与当前页面在同一域名下\n2. 或者在开发环境中配置代理，使预览 URL 通过代理访问\n3. 或者使用 postMessage 通信（需要预览页面支持）');
    isEditingMode.value = false;
    emit('update:isEditingMode', false);
    return;
  }

  try {
    // 如果已有选择处理器，先禁用旧的
    if (elementSelectionHandler.value) {
      disableElementSelection();
    }

    const iframeDoc = previewIframe.value.contentDocument || previewIframe.value.contentWindow.document;

    // 再次检查 iframe 是否已加载完成（DOM 必须存在才能初始化选择器）
    if (!iframeDoc || !iframeDoc.body) {
      console.warn('⚠️ iframe 文档未加载完成，等待加载...');
      // 等待 iframe 加载完成后再初始化
      previewIframe.value.addEventListener('load', () => {
        nextTick(() => {
          try {
            const doc = previewIframe.value.contentDocument || previewIframe.value.contentWindow.document;
            if (doc && doc.body) {
              elementSelectionHandler.value = createElementSelectionHandlerWrapper(doc);
              elementSelectionHandler.value.init();
            }
          } catch (err) {
            console.error('启用元素选择失败', err);
            alert('无法进入编辑模式：' + err.message);
            isEditingMode.value = false;
            emit('update:isEditingMode', false);
          }
        });
      }, { once: true });
      return;
    }

    // 创建并初始化元素选择处理器
    elementSelectionHandler.value = createElementSelectionHandlerWrapper(iframeDoc);
    elementSelectionHandler.value.init();
  } catch (err) {
    console.error('启用元素选择失败（跨域问题）', err);
    alert('无法进入编辑模式：预览页面可能是跨域的，无法访问其内容。\n\n详细信息：' + err.message);
    isEditingMode.value = false;
    emit('update:isEditingMode', false);
  }
};

// 禁用元素选择
const disableElementSelection = () => {
  if (elementSelectionHandler.value) {
    elementSelectionHandler.value.destroy();
    elementSelectionHandler.value = null;
  }
};

// 创建元素选择处理器的包装函数，用于处理组件特定的逻辑
// 当用户选择元素时，将选择器通过 emit 传递给父组件
const createElementSelectionHandlerWrapper = (doc) => {
  return createElementSelectionHandler(doc, (selector) => {
    emit('update:selectedElementSelector', selector);
  });
};

// 监听编辑模式变化
// 当编辑模式开启时，如果 iframe 已加载完成，立即启用元素选择
// 如果 iframe 尚未加载完成，等待 load 事件后再启用
watch(isEditingMode, (newVal) => {
  if (newVal && previewIframe.value) {
    const iframe = previewIframe.value;
    const tryEnable = () => {
      try {
        if (iframe.contentDocument && iframe.contentDocument.readyState === 'complete') {
          enableElementSelection();
        }
      } catch (err) {
        console.error('无法访问iframe内容:', err);
      }
    };

    // 检查 iframe 是否已加载完成
    if (iframe.contentDocument && iframe.contentDocument.readyState === 'complete') {
      tryEnable();
    } else {
      // 等待 iframe 加载完成
      iframe.addEventListener('load', tryEnable, { once: true });
    }
  }
});

// 监听 iframe URL 变化
// 当预览 URL 变化时，显示 loading 并检查是否已加载完成（处理缓存情况）
// 如果处于编辑模式，需要先禁用元素选择（因为 iframe 即将重新加载）
watch(() => previewUrl.value, (newVal, oldVal) => {
  // 当 URL 变化时，显示 loading
  if (newVal && newVal !== oldVal) {
    isPreviewLoading.value = true;
    setLoadingTimeout();
    // 延迟检查，确保 iframe 已经创建（处理浏览器缓存的情况）
    setTimeout(() => {
      checkIframeLoaded();
    }, 100);
  }

  // URL 变化时，如果处于编辑模式，需要先禁用元素选择
  if (isEditingMode.value && previewIframe.value) {
    disableElementSelection();
  }
}, { immediate: true });

// 监听 iframeKey 变化，重置 loading 状态
// iframeKey 变化意味着 iframe 被强制刷新（通过 key 变化重新创建）
// 此时需要显示 loading 并检查加载状态
watch(() => iframeKey.value, () => {
  if (previewUrl.value) {
    isPreviewLoading.value = true;
    setLoadingTimeout();
    // 延迟检查，确保新的 iframe 已经创建
    setTimeout(() => {
      checkIframeLoaded();
    }, 100);
  }
});

// 组件挂载时，如果有 previewUrl，显示 loading
// 初始化时检查 iframe 是否已加载完成（处理浏览器缓存的情况）
onMounted(() => {
  if (previewUrl.value) {
    isPreviewLoading.value = true;
    setLoadingTimeout();
    // 检查 iframe 是否已经加载完成（处理缓存情况）
    checkIframeLoaded();
  }
});

// 组件卸载时清理定时器
// 防止内存泄漏，确保所有定时器都被清除
onUnmounted(() => {
  if (loadingTimeout) {
    clearTimeout(loadingTimeout);
    loadingTimeout = null;
  }
  // 如果处于编辑模式，清理元素选择处理器
  if (elementSelectionHandler.value) {
    disableElementSelection();
  }
});

// 监听 selectedElementSelector 变化，当被清空时清除选中状态
// 当父组件清空选择器时（例如用户取消选择），同步清除元素选择处理器的选中状态
watch(() => props.selectedElementSelector, (newVal) => {
  if (!newVal && elementSelectionHandler.value) {
    // 当选择器被清空时，清除元素选择处理器的选中状态
    elementSelectionHandler.value.clearSelection();
  }
});

// 暴露方法供父组件调用
defineExpose({
  refreshPreview: handleRefreshPreview,
  clearSelection
});
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';

.preview-panel-wrapper {
  display: flex;
  flex-direction: row;
  overflow: hidden;
  gap: 10px;
  height: 100%;

  &.vscode-fullscreen {
    .preview-panel {
      border-radius: 0;
      border: none;
      box-shadow: none;
    }
  }
}

.preview-panel {
  flex: 1;
  background: $bg-card;
  border-radius: 12px;
  border: 1px solid rgba(0, 0, 0, 0.1);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-width: 0;

  &.vscode-fullscreen {
    border-radius: 0;
    border: none;
    box-shadow: none;
    height: 100%;
  }
}

.sidebar-tabs {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 11px 0 0 0;
  gap: 26px;
  flex-shrink: 0;

  .tab-item {
    width: fit-content;
    height: 40px;
    display: flex;
    align-items: center;
    justify-content: center;
    background: transparent;
    border: none;
    border-radius: $border-radius;
    cursor: pointer;
    transition: all $transition-base;
    padding: 0;

    &:hover {
      background: $bg-secondary;
    }

    &.active {
      background: rgba(59, 130, 246, 0.1);
    }

    .tab-icon {
      width: 24px;
      height: 24px;
      object-fit: contain;
    }
  }
}

.panel-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-width: 0;
}

.panel-header {
  padding: 12px 20px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.1);
  background: #FFFFFF;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;

  .panel-title-wrapper {
    display: flex;
    align-items: center;
    gap: 12px;
    flex-shrink: 0;
  }

  .panel-title {
    font-size: 16px;
    font-weight: 600;
    color: #333333;
    margin: 0;
  }
}

.preview-controls {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.app-type-badge {
  padding: 0 6px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
  white-space: nowrap;
}

.code-editor-container {
  height: 100%;
}

.preview-container {
  height: 100%;
  flex: 1;
  display: flex;
  flex-direction: column;
  position: relative;
  background: $bg-secondary;
}


.preview-iframe-wrapper {
  flex: 1;
  position: relative;
  display: flex;
  flex-direction: column;

  &.editing-mode {
    .preview-iframe {
      pointer-events: auto;
    }
  }
}

.vscode-loading-container {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: $spacing-lg;
  background: $bg-card;
  z-index: 100;
  pointer-events: none;
}

.preview-iframe {
  flex: 1;
  width: 100%;
  border: none;
  background: white;
  transition: opacity 0.3s ease;

  &.vscode-loading {
    opacity: 0;
    pointer-events: none;
  }

  &.preview-loading {
    opacity: 0;
    pointer-events: none;
  }
}

.preview-loading-container {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: $spacing-lg;
  background: $bg-card;
  z-index: 100;
  pointer-events: none;
}

.edit-mode-tip {
  position: absolute;
  top: $spacing-sm;
  left: 50%;
  transform: translateX(-50%);
  background: rgba(0, 164, 255, 0.9);
  color: white;
  padding: $spacing-sm $spacing-md;
  border-radius: $border-radius;
  font-size: $font-size-sm;
  z-index: 10;
  pointer-events: none;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);

  p {
    margin: 0;
  }
}

.document-content {
  flex: 1;
  padding: $spacing-lg $spacing-xl;
  overflow-y: auto;
  line-height: 1;
  color: $text-primary;
  font-size: $font-size-md;

  :deep(h1) {
    font-size: $font-size-xl;
    font-weight: 700;
    margin: $spacing-sm 0 2px 0;
    color: $text-primary;
    border-bottom: 2px solid $primary-color;
    padding-bottom: 2px;
  }

  :deep(h2) {
    font-size: $font-size-lg;
    font-weight: 600;
    margin: $spacing-sm 0 2px 0;
    color: $text-primary;
  }

  :deep(h3) {
    font-size: $font-size-md;
    font-weight: 600;
    margin: $spacing-xs 0 2px 0;
    color: $text-secondary;
  }

  :deep(ul) {
    padding-left: $spacing-lg;
    list-style-type: disc;
  }

  :deep(li) {
    margin: 1px 0;
    line-height: 1;
  }

  :deep(code) {
    background-color: rgba(0, 164, 255, 0.1);
    padding: 2px 6px;
    border-radius: 3px;
    font-family: 'Courier New', monospace;
    font-size: 0.9em;
    color: $primary-dark;
  }

  :deep(pre) {
    background-color: $bg-secondary;
    padding: $spacing-md;
    border-radius: $border-radius;
    overflow-x: auto;
    margin: $spacing-xs 0;
    border-left: 3px solid $primary-color;

    code {
      background-color: transparent;
      padding: 0;
      color: $text-primary;
    }
  }

  :deep(strong) {
    font-weight: 600;
    color: $text-primary;
  }
}

.no-document {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: $text-light;
  font-size: $font-size-lg;
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

.btn {
  padding: 5px 12px;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 400;
  cursor: pointer;
  transition: all 0.2s ease;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 1px solid;
  white-space: nowrap;

  &.btn-primary {
    background: #409EFF;
    color: #FFFFFF;
    border-color: #409EFF;

    &:hover:not(:disabled) {
      background: #66b1ff;
      border-color: #66b1ff;
    }

    &:active:not(:disabled) {
      background: #3a8ee6;
      border-color: #3a8ee6;
    }

    &:disabled {
      opacity: 0.6;
      cursor: not-allowed;
      background: #a0cfff;
      border-color: #a0cfff;
    }
  }

  &.btn-outline {
    background: #FFFFFF;
    border-color: #DCDFE6;
    color: #606266;

    &:hover:not(:disabled) {
      color: #409EFF;
      border-color: #C6E2FF;
      background-color: #ECF5FF;
    }

    &:active:not(:disabled) {
      color: #3a8ee6;
      border-color: #3a8ee6;
    }

    &.btn-active {
      color: #409EFF;
      border-color: #409EFF;
      background-color: #ECF5FF;
    }

    &:disabled {
      border-color: #E4E7ED;
      color: #C0C4CC;
      cursor: not-allowed;
      background-color: #FFFFFF;
    }
  }

  &.btn-sm {
    padding: 5px 12px;
    font-size: 14px;
  }
}
</style>
