<template>
  <div class="chat-panel">
    <div class="chat-messages" ref="messagesContainer">
      <!-- 加载更多提示 -->
      <div v-if="isLoadingHistory" class="loading-more">
        <div class="loading-spinner-small"></div>
        <span>加载中...</span>
      </div>

      <!-- 无更多数据提示 -->
      <div v-else-if="!hasMoreHistory && chatHistory.length > 0 && !isInitialLoad" class="no-more-history">
        <span>没有更多对话历史了</span>
      </div>

      <div v-for="(message, index) in filteredChatHistory" :key="index"
        :class="['message', message.msgRole === 0 ? 'user' : 'bot', { 'has-document': isRequirementDocument(message, index) }, { 'loading-message': message.isLoading }]">
        <div class="message-avatar">
          <img v-if="message.msgRole === 0" src="@/imges/self.webp" alt="用户头像" class="avatar-img">
          <img v-else src="@/imges/ai.webp" alt="AI头像" class="avatar-img">
        </div>
        <div class="message-content"
          :class="{ 'document-content-wrapper': isRequirementDocument(message, index), 'loading-message': message.isLoading, 'progress-content': message.isProgress }">
          <!-- Loading 状态 -->
          <div v-if="message.isLoading" class="message-text">
            正在思考和处理中
            <span class="loading-dots">
              <span class="dot"></span>
              <span class="dot"></span>
              <span class="dot"></span>
            </span>
          </div>
          <!-- 进度消息 -->
          <div v-else-if="message.isProgress" class="progress-message">
            <div class="progress-steps">
              <template v-for="(step, stepIndex) in message.progress.steps" :key="stepIndex">
                <div class="progress-step" :class="{
                  'completed': step.status === 'completed',
                  'processing': step.status === 'processing',
                  'failed': step.status === 'failed'
                }">
                  <div class="step-icon">
                    <div v-if="step.status === 'completed'" class="icon-circle completed-icon">
                      <svg viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                        <path d="M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z" fill="white" />
                      </svg>
                    </div>
                    <div v-else-if="step.status === 'failed'" class="icon-circle failed-icon">
                      <svg viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                        <path
                          d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-2h2v2zm0-4h-2V7h2v6z"
                          fill="#ff4d4f" />
                      </svg>
                    </div>
                    <div v-else class="icon-circle pending-icon">
                      <div class="step-dot"></div>
                    </div>
                  </div>
                  <div class="step-content">
                    <div class="step-name">{{ step.name }}</div>
                  </div>
                </div>
              </template>
            </div>
            <div v-if="message.progress.isLoading" class="progress-loading">
              正在思考和处理中
              <span class="loading-dots">
                <span class="dot"></span>
                <span class="dot"></span>
                <span class="dot"></span>
              </span>
            </div>
            <div v-if="message.progress.isFailed" class="progress-actions">
              <button class="btn-regenerate-app" @click="handleGenerateFromDocument" :disabled="isSending">
                重新生成应用
              </button>
            </div>
          </div>
          <!-- 需求文档卡片样式 -->
          <div v-else-if="isRequirementDocument(message, index)" class="document-card">
            <div class="document-card-title">已根据您的需求生成产品文档</div>
            <div class="document-card-header">
              <img src="@/imges/fileIcon.webp" alt="文档图标" class="document-icon">
              <div class="document-info">
                <div class="document-name">需求文档.md</div>
              </div>
              <button class="download-btn" @click="handleDownloadDocument(message.content)" title="下载">
                <svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg" class="download-icon">
                  <path
                    d="M12 15V3M12 15L8 11M12 15L16 11M2 17L2 19C2 20.1046 2.89543 21 4 21L20 21C21.1046 21 22 20.1046 22 19V17"
                    stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" />
                </svg>
              </button>
            </div>
            <!-- 文档操作按钮 -->
            <div class="document-actions" v-if="!isGeneratingApp">
              <button class="btn-generate-app" @click="handleGenerateFromDocument" :disabled="isSending">
                立即生成应用
              </button>
            </div>
          </div>
          <!-- 普通消息 -->
          <div v-else>
            <div class="message-text" v-html="formatMessageText(displayContent(message))"></div>
          </div>
        </div>
      </div>

      <div v-if="filteredChatHistory.length === 0" class="empty-chat">
        <p>暂无对话历史</p>
      </div>
    </div>

    <!-- 消息输入区域 -->
    <div class="chat-input-container">
      <div v-if="selectedElementSelector" class="selected-element-info">
        <span class="label">已选元素：</span>
        <code class="selector">{{ selectedElementSelector }}</code>
        <button class="btn-clear-selector" @click="handleClearSelection" title="清除选择">×</button>
      </div>
      <div class="input-wrapper">
        <textarea v-model="messageInput" @keydown="handleKeyDown"
          :placeholder="isEditingMode && selectedElementSelector ? '输入要修改的内容...' : '请输入要修改的内容...'" class="chat-input"
          rows="1" :disabled="isSending"></textarea>
        <button class="btn-send" @click="handleSendMessage"
          :disabled="!messageInput.trim() || isSending || (isEditingMode && !selectedElementSelector)">
          <svg viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg" class="send-icon">
            <path d="M2.01 21L23 12 2.01 3 2 10l15 2-15 2z" />
          </svg>
          <span v-if="isSending">发送中...</span>
          <span v-else>{{ isEditingMode ? '修改' : '发送' }}</span>
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick, watch } from 'vue';
import { fetchChatHistory, generateApp, modifyApp, fetchAppDetail } from '@/apis';
import { formatMessageText } from '@/utils/messageFormatter.js';

const props = defineProps({
  appId: {
    type: String,
    required: true
  },
  appDetail: {
    type: Object,
    default: null
  },
  requirementDocument: {
    type: String,
    default: ''
  },
  isEditingMode: {
    type: Boolean,
    default: false
  },
  selectedElementSelector: {
    type: String,
    default: ''
  },
});

const emit = defineEmits([
  'update:appDetail',
  'update:selectedElementSelector',
  'refresh-preview',
  'generate-app',
  'load-app-detail',
  'update:activeTab'
]);

// 聊天历史
const chatHistory = ref([]);
const messagesContainer = ref(null);

// 聊天历史分页状态
const chatPageNo = ref(1);
const chatPageSize = ref(10);
const chatTotalPages = ref(0);
const chatTotals = ref(0);
const isLoadingHistory = ref(false);
const hasMoreHistory = ref(true);
const isInitialLoad = ref(true);

// 消息输入
const messageInput = ref('');
const isSending = ref(false);

// 应用生成进度状态
const generatingProgress = ref(null);
const isGeneratingApp = ref(false);

/**
 * 构建生成应用接口所需的 FormData
 * 后端参数为 requirement、appId，参考图字段为 reference
 * @param {string} requirement - 需求文档
 * @param {string|number|null|undefined} appId - 应用ID
 * @returns {FormData} 构建好的 FormData 对象
 */
const buildGenerateAppFormData = (requirement, appId) => {
  const formData = new FormData();
  formData.append('requirement', requirement ?? '');
  // 只有当 appId 存在且不为空字符串时才添加
  // 这样可以区分新建应用和更新应用
  if (appId !== undefined && appId !== null && String(appId).trim() !== '') {
    formData.append('appId', String(appId));
  }
  return formData;
};

const isSourceCode = (content) => {
  if (!content) return false;
  const trimmed = content.trimStart();
  return trimmed.startsWith('APP_TYPE=') || trimmed.startsWith('FILE:') || content.includes('\nFILE:');
};

const displayContent = (message) => {
  const content = message?.content || '';
  if (!isSourceCode(content)) {
    return content;
  }
  return message.msgRole === 0 ? '已提交代码修改' : '应用已更新';
};

/**
 * 判断是否是系统内部消息
 * 系统消息包含特定的标记，这些消息不应该显示给用户
 * @param {Object} message - 消息对象
 * @returns {boolean} 如果是系统消息返回true，否则返回false
 */
const isSystemMessage = (message) => {
  if (!message || !message.content) return false;
  const content = message.content;

  // 系统消息的标记列表，包含这些标记的消息会被过滤掉
  const systemMarkers = [
    '【用户需求文档】',
    '【输出要求】',
    '请调用 commitFile 工具',
    '<function=',
    '</function>',
    '<parameter=',
    '</parameter>',
    '<tool_call>',
    '</tool_call>'
  ];

  return systemMarkers.some(marker => content.includes(marker));
};

/**
 * 过滤后的聊天历史
 * 过滤掉系统消息和空消息，但保留加载中和进度消息
 */
const filteredChatHistory = computed(() => {
  if (!chatHistory.value || chatHistory.value.length === 0) {
    return [];
  }

  return chatHistory.value.filter((msg) => {
    // 过滤掉系统内部消息
    if (isSystemMessage(msg)) {
      return false;
    }

    // 保留加载中的消息（显示加载动画）
    if (msg.isLoading) {
      return true;
    }

    // 保留进度消息（显示生成进度）
    if (msg.isProgress) {
      return true;
    }

    // 过滤掉空消息
    if (!msg.content || !msg.content.trim()) {
      return false;
    }

    return true;
  });
});

/**
 * 判断是否是需求文档消息
 * 需求文档需要满足以下条件：
 * 1. 必须是AI回复的消息（msgRole === 1）
 * 2. 必须是AI的第一条消息
 * 3. 内容长度超过100字符
 * 4. 包含Markdown格式或需求相关关键词
 * @param {Object} message - 消息对象
 * @param {number} index - 消息在过滤后列表中的索引
 * @returns {boolean} 如果是需求文档返回true，否则返回false
 */
const isRequirementDocument = (message, index) => {
  // 只处理AI的消息
  if (message.msgRole !== 1 || !message.content) {
    return false;
  }

  // 查找AI的第一条消息索引
  const firstBotIndex = filteredChatHistory.value.findIndex(msg => msg.msgRole === 1);
  const isFirstBotMessage = index === firstBotIndex;

  // 只有第一条AI消息才可能是需求文档
  if (!isFirstBotMessage) {
    return false;
  }

  const content = message.content;
  if (isSourceCode(content)) {
    return false;
  }
  const contentLength = content.length;

  // 检查是否包含Markdown格式（标题、列表等）
  const hasMarkdownFormat = /^#+\s|^[-*]\s|^\d+\.\s/m.test(content);
  // 检查是否包含需求文档相关的关键词
  const hasRequirementKeywords = /应用需求文档|需求文档|应用名称|应用描述|应用核心功能/.test(content);

  // 内容长度超过100字符，且包含Markdown格式或需求关键词
  return contentLength > 100 && (hasMarkdownFormat || hasRequirementKeywords);
};

/**
 * 加载聊天历史
 * @param {boolean} reset - 是否重置（重置时从第一页开始加载，清空现有历史）
 */
const loadChatHistory = async (reset = false) => {
  if (isLoadingHistory.value || !props.appId) return;

  // 如果没有更多历史且不是重置操作，则直接返回
  if (!hasMoreHistory.value && !reset) {
    return;
  }

  try {
    isLoadingHistory.value = true;

    if (reset) {
      // 重置时，重置分页状态和聊天历史
      chatPageNo.value = 1;
      chatHistory.value = [];
      hasMoreHistory.value = true;
    }

    const result = await fetchChatHistory({
      appId: props.appId,
      pageNo: chatPageNo.value,
      pageSize: chatPageSize.value,
    });

    if (result && result.list) {
      const newMessages = result.list;

      if (reset) {
        // 重置时直接替换
        chatHistory.value = newMessages;
        isInitialLoad.value = false;
      } else {
        // 加载更多时，将新消息插入到数组前面（因为历史消息是倒序的）
        chatHistory.value = [...newMessages, ...chatHistory.value];
      }

      chatTotals.value = result.totals || 0;
      chatTotalPages.value = result.totalPages || 0;

      // 判断是否还有更多历史消息
      // 如果返回的消息数少于每页大小，或者当前页已经是最后一页，则没有更多了
      if (newMessages.length < chatPageSize.value || chatPageNo.value >= chatTotalPages.value) {
        hasMoreHistory.value = false;
      } else {
        hasMoreHistory.value = true;
      }
    }
  } catch (err) {
    console.error('加载聊天历史失败:', err);
  } finally {
    isLoadingHistory.value = false;
  }
};

/**
 * 加载更多聊天历史
 * 当用户滚动到顶部时触发，加载历史消息
 * 加载后需要保持滚动位置，避免页面跳动
 */
const loadMoreHistory = async () => {
  if (isLoadingHistory.value || !hasMoreHistory.value) {
    return;
  }

  const container = messagesContainer.value;
  if (!container) return;

  // 记录加载前的滚动位置和容器高度
  const oldScrollHeight = container.scrollHeight;
  const oldScrollTop = container.scrollTop;

  // 加载下一页数据
  chatPageNo.value++;
  await loadChatHistory(false);

  // 等待DOM更新
  await nextTick();
  // 计算新内容的高度差，调整滚动位置，保持用户看到的视觉位置不变
  const newScrollHeight = container.scrollHeight;
  container.scrollTop = oldScrollTop + (newScrollHeight - oldScrollHeight);
};

// 滚动到底部。长消息渲染后高度还会变，下一帧再对齐一次
const scrollToBottom = () => {
  nextTick(() => {
    const container = messagesContainer.value;
    if (!container) return;
    container.scrollTop = container.scrollHeight;
    requestAnimationFrame(() => {
      if (messagesContainer.value) {
        messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight;
      }
    });
  });
};

/**
 * 处理滚动事件
 * 当用户滚动到顶部附近（距离顶部50px内）时，自动加载更多历史消息
 */
const handleScroll = () => {
  const container = messagesContainer.value;
  if (!container) return;

  // 滚动到顶部附近，且有更多历史，且当前没有正在加载时，触发加载更多
  if (container.scrollTop < 50 && hasMoreHistory.value && !isLoadingHistory.value) {
    loadMoreHistory();
  }
};

/**
 * 发送消息
 * 处理用户发送消息的逻辑，包括编辑模式和普通聊天模式
 * 编辑模式：修改应用中的特定元素
 * 普通模式：生成新的应用内容
 */
const handleSendMessage = async () => {
  const message = messageInput.value.trim();
  if (!message || isSending.value || !props.appDetail) {
    return;
  }

  // 编辑模式下必须选中元素
  if (props.isEditingMode && !props.selectedElementSelector) {
    alert('请先在预览页面中选择要修改的元素');
    return;
  }

  try {
    isSending.value = true;

    // 添加用户消息到聊天历史
    const userMessage = {
      msgRole: 0,
      // 编辑模式下，消息内容包含元素选择器信息
      content: props.isEditingMode ? `修改元素 ${props.selectedElementSelector} 将其: ${message}` : message,
      chatTime: new Date().toISOString()
    };
    chatHistory.value.push(userMessage);

    // 添加加载中的AI消息（显示加载动画）
    const loadingMessage = {
      msgRole: 1,
      content: '',
      chatTime: new Date().toISOString(),
      isLoading: true
    };
    chatHistory.value.push(loadingMessage);

    const contentToSend = message;
    messageInput.value = '';

    await nextTick();
    scrollToBottom();

    let result;
    // 记录加载消息的索引，用于后续更新
    let loadingMessageIndex = chatHistory.value.length - 1;

    if (props.isEditingMode) {
      // 编辑模式：调用修改应用接口
      result = await modifyApp({
        appId: props.appDetail.id,
        elementSelector: props.selectedElementSelector,
        newContent: contentToSend,
      });

      // 将加载消息替换为成功消息
      chatHistory.value[loadingMessageIndex] = {
        msgRole: 1,
        content: '应用修改成功！',
        chatTime: new Date().toISOString(),
        isLoading: false
      };

      if (result && result.url) {
        emit('update:appDetail', { ...props.appDetail, previewUrl: result.url });
      }
      // 不论是否返回 previewUrl，都刷新预览并清空选中元素
      emit('refresh-preview', true);
      emit('update:selectedElementSelector', '');
    } else {
      // 普通模式：调用生成应用接口
			{
				const formData = new FormData();
				formData.append('requirement', contentToSend);
				formData.append('appId', props.appDetail.id);
				result = await generateApp(formData);
			}

      if (result && (result.url || result.appId)) {
        chatHistory.value[loadingMessageIndex] = {
          msgRole: 1,
          content: '应用已生成',
          chatTime: new Date().toISOString(),
          isLoading: false
        };

        if (result.url) {
          emit('update:appDetail', { ...props.appDetail, previewUrl: result.url });
          emit('refresh-preview', true);
        }
      } else {
        // 如果没有返回内容，删除加载消息
        chatHistory.value.splice(loadingMessageIndex, 1);
      }
    }

    await nextTick();
    scrollToBottom();
  } catch (err) {
    console.error(props.isEditingMode ? '修改应用失败:' : '发送消息失败:', err);

    // 查找加载中的消息，替换为错误消息
    const loadingMessageIndex = chatHistory.value.findIndex(msg => msg.isLoading === true);
    if (loadingMessageIndex !== -1) {
      chatHistory.value[loadingMessageIndex] = {
        msgRole: 1,
        content: `抱歉，${props.isEditingMode ? '修改应用失败' : '发送消息失败'}：${err.message || '请稍后重试'}`,
        chatTime: new Date().toISOString(),
        isLoading: false
      };
    } else {
      // 如果找不到加载消息，添加新的错误消息
      const errorMessage = {
        msgRole: 1,
        content: `抱歉，${props.isEditingMode ? '修改应用失败' : '发送消息失败'}：${err.message || '请稍后重试'}`,
        chatTime: new Date().toISOString()
      };
      chatHistory.value.push(errorMessage);
    }

    await nextTick();
    scrollToBottom();
  } finally {
    isSending.value = false;
  }
};

// 处理键盘事件
const handleKeyDown = (event) => {
  if ((event.ctrlKey || event.metaKey) && event.key === 'Enter') {
    event.preventDefault();
    handleSendMessage();
  }
};

// 清除选择
const handleClearSelection = () => {
  emit('update:selectedElementSelector', '');
};

// 下载文档
const handleDownloadDocument = (content) => {
  if (!content) return;

  const blob = new Blob([content], { type: 'text/markdown;charset=utf-8' });
  const url = URL.createObjectURL(blob);

  const link = document.createElement('a');
  link.href = url;
  link.download = '需求文档.md';
  document.body.appendChild(link);
  link.click();

  document.body.removeChild(link);
  URL.revokeObjectURL(url);
};

/**
 * 从需求文档生成应用
 * 这是一个复杂的流程，包括：
 * 1. 显示进度步骤（设计应用、开发应用、准备数据）
 * 2. 调用生成接口
 * 3. 加载应用详情
 * 4. 切换到预览tab并刷新预览
 */
const handleGenerateFromDocument = async () => {
  if (!props.requirementDocument) {
    console.warn('无法生成应用：需求文档为空');
    return;
  }
  if (isSending.value) {
    console.warn('无法生成应用：正在发送中');
    return;
  }
  if (isGeneratingApp.value) {
    console.warn('无法生成应用：正在生成中');
    return;
  }

  try {
    isSending.value = true;
    isGeneratingApp.value = true;

    // 添加用户消息
    const userMessage = {
      msgRole: 0,
      content: '立即生成应用',
      chatTime: new Date().toISOString()
    };
    chatHistory.value.push(userMessage);

    // 创建进度消息，显示生成步骤
    const progressMessage = {
      msgRole: 1,
      chatTime: new Date().toISOString(),
      isProgress: true,
      progress: {
        steps: [
          { name: '设计应用', status: 'pending' },
          { name: '开发应用', status: 'pending' },
          { name: '准备数据', status: 'pending' }
        ],
        isLoading: true,
        isFailed: false
      }
    };
    chatHistory.value.push(progressMessage);
    generatingProgress.value = progressMessage;

    await nextTick();
    scrollToBottom();

    let result;
    try {
      // 调用生成应用接口
      {
        const formData = new FormData();
        formData.append('requirement', props.requirementDocument);
        if (props.appDetail?.id) {
          formData.append('appId', props.appDetail.id);
        }
        result = await generateApp(formData);
      }

      // 接口调用成功，将所有步骤标记为已完成
      progressMessage.progress.steps.forEach(step => {
        step.status = 'completed';
      });
      progressMessage.progress.isLoading = false;

      if (result && result.appId) {
        // 有 appId 表示生成成功，添加"完成！"步骤
        progressMessage.progress.steps.push(
          { name: '完成！', status: 'completed' }
        );

        // 先加载应用详情，确保有 previewUrl 后再切换 tab
        try {
          const appDetail = await fetchAppDetail({ appId: result.appId });
          if (appDetail) {
            // 更新应用详情
            emit('update:appDetail', appDetail);
            // 等待 Vue 更新完成
            await nextTick();
            // 延迟一下确保 DOM 更新完成（给 PreviewPanel 时间渲染）
            await new Promise(resolve => setTimeout(resolve, 100));
          }
        } catch (err) {
          console.error('加载应用详情失败:', err);
          // 即使加载失败，也继续执行后续操作
        }

        // 切换到预览 tab
        emit('update:activeTab', 'preview');

        // 等待 tab 切换动画完成
        await nextTick();
        await new Promise(resolve => setTimeout(resolve, 100));

        // 触发预览刷新（让 PreviewPanel 加载最新的应用内容）
        emit('refresh-preview', true);

        // 清理状态
        generatingProgress.value = null;
        isGeneratingApp.value = false;

        await nextTick();
        scrollToBottom();
      } else {
        // 没有 appId，标记为失败
        updateProgressFailed(progressMessage);
      }
    } catch (err) {
      console.error('生成应用接口调用失败:', err);
      // 接口调用失败，更新所有待处理的步骤为失败状态
      progressMessage.progress.steps.forEach((step) => {
        if (step.status === 'pending' || step.status === 'processing') {
          step.status = 'failed';
        }
      });
      progressMessage.progress.isLoading = false;
      progressMessage.progress.isFailed = true;
      // 调用 updateProgressFailed 添加失败消息
      updateProgressFailed(progressMessage);
      throw err;
    }
  } catch (err) {
    console.error('生成应用失败:', err);
    if (generatingProgress.value) {
      generatingProgress.value.progress.isLoading = false;
      updateProgressFailed(generatingProgress.value);
    }
  } finally {
    isSending.value = false;
    isGeneratingApp.value = false;
  }
};


/**
 * 更新进度为失败状态
 * 当应用生成失败时调用，更新进度步骤的显示状态
 * @param {Object} progressMessage - 进度消息对象
 */
const updateProgressFailed = (progressMessage) => {
  // 检查是否已经完成（有"完成！"步骤），如果已完成则不处理失败
  // 避免在成功后又显示失败信息
  const hasCompletedMessage = progressMessage.progress.steps.some(
    step => step.name === '完成！'
  );

  if (hasCompletedMessage) {
    // 已经完成，不处理失败
    return;
  }

  // 更新步骤状态：将待处理的步骤标记为已完成（如果还没有被标记为失败）
  // 如果步骤已经被标记为 failed（比如接口调用失败时），则保持 failed 状态
  // 最后一个步骤不标记为已完成，因为后面要添加失败步骤
  progressMessage.progress.steps.forEach((step, index) => {
    if (step.status === 'pending' || step.status === 'processing') {
      // 不是最后一个步骤才标记为已完成
      if (index < progressMessage.progress.steps.length - 1) {
        step.status = 'completed';
      }
    }
  });

  // 检查是否已经有失败消息，避免重复添加
  const hasFailedMessage = progressMessage.progress.steps.some(
    step => step.name === '应用生成失败！'
  );

  if (!hasFailedMessage) {
    // 添加"编辑应用"（已完成）和"应用生成失败！"（失败）两个步骤
    progressMessage.progress.steps.push(
      { name: '编辑应用', status: 'completed' },
      { name: '应用生成失败！', status: 'failed' }
    );
  }

  progressMessage.progress.isFailed = true;
  progressMessage.progress.isLoading = false;

  nextTick(() => {
    scrollToBottom();
  });
  generatingProgress.value = null;
  isGeneratingApp.value = false;
};

// 初始化聊天历史（从 sessionStorage 或 API）
const initChatHistory = async (initialMessages = []) => {
  if (initialMessages && initialMessages.length > 0) {
    chatHistory.value = initialMessages;
    await nextTick();
    scrollToBottom();
  } else if (props.appId) {
    await loadChatHistory(true);
    await nextTick();
    scrollToBottom();
  }
};

// 添加消息到聊天历史
const addMessage = (message) => {
  chatHistory.value.push(message);
  nextTick(() => {
    scrollToBottom();
  });
};

// 暴露方法供父组件调用
defineExpose({
  loadChatHistory,
  initChatHistory,
  addMessage,
  scrollToBottom
});

/**
 * 监听 messagesContainer 的变化，动态添加/移除滚动监听
 * 当 ref 元素变化时（如组件重新渲染），需要重新绑定事件监听器
 */
watch(messagesContainer, (newVal, oldVal) => {
  // 移除旧元素的监听器（避免内存泄漏）
  if (oldVal) {
    oldVal.removeEventListener('scroll', handleScroll);
  }
  // 为新元素添加监听器
  if (newVal) {
    newVal.addEventListener('scroll', handleScroll);
  }
});

// 组件挂载时初始化
onMounted(() => {
  // 检查是否有从 sessionStorage 传递过来的数据
  // 这通常是从其他页面（如 ChatBox）跳转过来时保存的聊天数据
  const appViewDataStr = sessionStorage.getItem('appViewData');
  if (appViewDataStr) {
    try {
      const appViewData = JSON.parse(appViewDataStr);
      if (appViewData.document) {
        // 过滤并转换消息格式
        // 过滤掉空的bot消息
        const validMessages = (appViewData.messages || [])
          .filter(msg => {
            if (msg.type === 'bot' && (!msg.text || !msg.text.trim())) {
              return false;
            }
            return true;
          })
          // 转换为组件需要的消息格式
          .map(msg => ({
            msgRole: msg.type === 'user' ? 0 : 1, // 0=用户，1=AI
            content: msg.text || '',
            chatTime: msg.time || new Date().toISOString()
          }));

        // 分离用户消息和AI消息
        const userMessages = validMessages.filter(msg => msg.msgRole === 0);
        const botMessages = validMessages.filter(msg => msg.msgRole === 1);

        // 重新组织消息顺序：
        // 1. 先显示用户消息
        // 2. 然后显示需求文档（作为AI的第一条消息）
        // 3. 最后显示其他AI消息
        const initialMessages = [
          ...userMessages,
          {
            msgRole: 1,
            content: appViewData.document,
            chatTime: new Date().toISOString()
          },
          ...botMessages
        ];

        initChatHistory(initialMessages);
        // 使用完后清除 sessionStorage
        sessionStorage.removeItem('appViewData');
        return;
      }
    } catch (err) {
      console.error('解析appViewData失败:', err);
    }
  }

  // 如果没有 sessionStorage 数据，则从API加载聊天历史，并停在最新一条
  if (props.appId) {
    initChatHistory();
  }
});

// 组件卸载时清理
onUnmounted(() => {
  if (messagesContainer.value) {
    messagesContainer.value.removeEventListener('scroll', handleScroll);
  }

});
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';

.chat-panel {
  background: $bg-primary;
  border-radius: 12px;
  border: 1px solid rgba(0, 0, 0, 0.1);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: $spacing-lg;
  display: flex;
  flex-direction: column;
  gap: $spacing-md;
  background: $bg-primary;
}

.loading-more {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: $spacing-sm;
  padding: $spacing-md;
  color: $text-secondary;
  font-size: $font-size-sm;

  .loading-spinner-small {
    width: 16px;
    height: 16px;
    border: 2px solid $border-color;
    border-top-color: $primary-color;
    border-radius: 50%;
    animation: spin 0.6s linear infinite;
  }
}

.no-more-history {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: $spacing-md;
  color: $text-light;
  font-size: $font-size-sm;

  span {
    position: relative;
    padding: 0 $spacing-md;

    &::before,
    &::after {
      content: '';
      position: absolute;
      top: 50%;
      width: 40px;
      height: 1px;
      background: linear-gradient(to right, transparent, $border-color, transparent);
    }

    &::before {
      right: 100%;
    }

    &::after {
      left: 100%;
    }
  }
}

.chat-input-container {
  padding: 16px 20px;
  border-top: none;
  background: $bg-primary;
  display: flex;
  flex-direction: column;
  gap: $spacing-sm;

  .selected-element-info {
    display: flex;
    align-items: center;
    gap: $spacing-sm;
    padding: $spacing-xs $spacing-sm;
    background: rgba(0, 164, 255, 0.1);
    border: 1px solid rgba(0, 164, 255, 0.3);
    border-radius: $border-radius;
    font-size: $font-size-sm;

    .label {
      color: $text-secondary;
      font-weight: 500;
    }

    .selector {
      flex: 1;
      background: $bg-card;
      padding: 2px $spacing-xs;
      border-radius: 3px;
      font-family: 'Courier New', monospace;
      font-size: $font-size-xs;
      color: $primary-dark;
      font-weight: 600;
    }

    .btn-clear-selector {
      padding: 2px 6px;
      background: transparent;
      border: none;
      border-radius: $border-radius;
      cursor: pointer;
      color: $text-secondary;
      font-size: $font-size-lg;
      line-height: 1;
      transition: all $transition-fast;

      &:hover {
        background: rgba(0, 0, 0, 0.1);
        color: $text-primary;
      }
    }
  }

  .input-wrapper {
    display: flex;
    gap: 8px;
    align-items: flex-start;
    padding: 12px 16px;
    background: #F9FAFB;
    border-radius: 8px;
    height: 158px;
  }

  .chat-input {
    flex: 1;
    padding: 0;
    border: none;
    font-size: 14px;
    font-family: inherit;
    resize: none;
    transition: all $transition-base;
    background: transparent;
    color: $text-primary;
    line-height: 1.5;
    min-height: 24px;
    height: 100%;

    &:focus {
      outline: none;
    }

    &:disabled {
      cursor: not-allowed;
      opacity: 0.6;
    }

    &::placeholder {
      color: #CCCCCC;
    }
  }

  .btn-send {
    align-self: flex-end;
    padding: 8px 15px;
    background: #3B82F6;
    color: $text-white;
    border: none;
    border-radius: 6px;
    cursor: pointer;
    transition: all 0.2s ease;
    display: flex;
    align-items: center;
    justify-content: center;
    width: fit-content;
    flex-shrink: 0;
    gap: 8px;

    .send-icon {
      width: 18px;
      height: 18px;
      fill: currentColor;
    }

    &:hover:not(:disabled) {
      background: #2563EB;
      box-shadow: $shadow-lg;
    }

    &:active:not(:disabled) {
      background: #1D4ED8;
    }

    &:disabled {
      opacity: 0.5;
      cursor: not-allowed;
      background: #93C5FD;
    }
  }
}

.empty-chat {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: $text-light;
  font-size: $font-size-lg;
}

.message {
  display: flex;
  gap: 12px;
  animation: slideIn 0.3s ease;

  &.user {
    flex-direction: row-reverse;

    .message-content {
      background: rgba(59, 130, 246, 0.06);
      color: #3B82F6;
      align-items: flex-end;
      border: none;
    }
  }

  &.bot {
    .message-content {
      background-color: #F5F5F5;
      color: $text-primary;
      border: none;

      &.document-content-wrapper {
        background-color: transparent;
        padding: 0;
        border: none;
        box-shadow: none;
        max-width: 100%;
      }

      &.loading-message {
        background-color: transparent;
        box-shadow: none;
        color: #9CA3AF;
        display: flex;
        align-items: center;
      }

      &.progress-content {
        background-color: transparent;
        box-shadow: none;
        padding: 0;
        max-width: 100%;
      }
    }
  }
}

.message-avatar {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  overflow: hidden;

  .avatar-img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
}

.message-content {
  display: flex;
  flex-direction: column;
  gap: $spacing-xs;
  max-width: 80%;
  padding: $spacing-sm $spacing-md;
  border-radius: $border-radius;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);

  :deep(code) {
    white-space: normal;
  }

  :deep(ul) {
    padding-left: 16px;
  }
}

.message-text {
  font-size: $font-size-md;
  line-height: 1.6;
  white-space: pre-wrap;
  word-wrap: break-word;

  :deep(code) {
    background-color: rgba(0, 0, 0, 0.05);
    padding: 2px 6px;
    border-radius: 3px;
    font-family: 'Courier New', monospace;
    font-size: 0.9em;
  }

  /* Markdown 代码块（```xxx） */
  :deep(pre.message-code-block) {
    background: rgba(0, 0, 0, 0.06);
    border: 1px solid rgba(0, 0, 0, 0.08);
    border-radius: 8px;
    padding: 12px 14px;
    margin: 10px 0;
    overflow-x: auto;
    white-space: pre;
    /* 保留换行/缩进 */
  }

  :deep(pre.message-code-block code) {
    background: transparent;
    padding: 0;
    border-radius: 0;
    font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", "Courier New", monospace;
    font-size: 0.9em;
    line-height: 1.55;
    display: block;
    white-space: pre;
    /* 防止被父级 pre-wrap 影响 */
  }

  :deep(strong) {
    font-weight: 600;
  }

  :deep(.message-link) {
    color: $primary-color;
    text-decoration: underline;
    transition: all $transition-fast;

    &:hover {
      color: $primary-dark;
      text-decoration: none;
    }
  }

  :deep(.function-call),
  :deep(.tool-call) {
    background: rgba(59, 130, 246, 0.1);
    border-left: 3px solid #3B82F6;
    padding: 8px 12px;
    margin: 8px 0;
    border-radius: 4px;
    font-family: 'Courier New', monospace;
    font-size: 0.9em;
  }

  :deep(.function-param) {
    background: rgba(147, 51, 234, 0.1);
    border-left: 3px solid #9333EA;
    padding: 6px 12px;
    margin: 4px 0;
    margin-left: 20px;
    border-radius: 4px;
    font-family: 'Courier New', monospace;
    font-size: 0.85em;
  }
}

.loading-dots {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  margin-right: 8px;

  .dot {
    width: 6px;
    height: 6px;
    border-radius: 50%;
    background-color: #999999;
    animation: dotPulse 1.4s infinite ease-in-out both;

    &:nth-child(1) {
      animation-delay: -0.32s;
    }

    &:nth-child(2) {
      animation-delay: -0.16s;
    }
  }
}

.document-card {
  width: 100%;
  max-width: 100%;
  background: #FFFFFF;
  overflow: hidden;
}

.document-card-title {
  font-size: 14px;
  line-height: 21px;
  color: #222222;
  margin-bottom: 10px;
}

.document-card-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 15px 10px;
  background: #F5F5F780;
  border-bottom: 1px solid #F3F4F6;
  box-shadow: 0px 1px 4px 0px rgba(0, 0, 0, 0.1);
  border-radius: 12px;
  border: 1px solid #DCDEE0;
}

.document-actions {
  display: flex;
  flex-direction: column;
  gap: 0;

  button {
    width: 100%;
    padding: 12px 16px;
    border-radius: 6px;
    font-size: 14px;
    font-weight: 500;
    cursor: pointer;
    transition: all 0.2s ease;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 8px;
    border: none;
  }
}

.btn-generate-app {
  background: linear-gradient(-90deg, #2468F2 2.14%, #28A4FC 100%);
  border-radius: 6px;
  color: #FFFFFF;
  margin-top: 12px;
  font-size: 14px;
  line-height: 20px;
  padding: 8px 16px !important;

  &:hover:not(:disabled) {
    box-shadow: 0 2px 8px rgba(59, 130, 246, 0.3);
  }

  &:active:not(:disabled) {
    transform: scale(0.98);
  }

  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
    background: #93C5FD;
  }
}

.document-icon {
  width: 34px;
  height: 34px;
  flex-shrink: 0;
  object-fit: contain;
}

.document-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.document-name {
  font-size: 16px;
  color: #222222;
  margin: 0;
  line-height: 1.5;
}

.download-btn {
  flex-shrink: 0;
  padding: 0;
  background: transparent;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.2s ease;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  color: #6B7280;

  .download-icon {
    width: 18px;
    height: 18px;
  }

  &:hover {
    background: #F3F4F6;
    color: #3B82F6;
  }

  &:active {
    transform: scale(0.95);
  }
}

.progress-message {
  width: 100%;
}

.progress-steps {
  display: flex;
  flex-direction: column;
  gap: 0;
}

.progress-step {
  display: flex;
  align-items: center;
  gap: 12px;
  position: relative;
  padding-bottom: 28px;

  &:last-child {
    padding-bottom: 0;
  }

  &:not(:last-child)::before {
    content: '';
    position: absolute;
    left: 9px;
    top: 22px;
    width: 0;
    height: calc(100% - 8px);
    border-left: 2px dashed #2196F3;
  }

  .step-icon {
    width: 19px;
    height: 19px;
    flex-shrink: 0;
    display: flex;
    align-items: center;
    justify-content: center;

    .icon-circle {
      width: 19px;
      height: 19px;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;

      svg {
        width: 19px;
        height: 19px;
      }

      .step-dot {
        width: 8px;
        height: 8px;
        border-radius: 50%;
        background: #FFFFFF;
      }
    }

    .completed-icon {
      background: #2196F3;

      svg {
        width: 14px;
        height: 14px;
      }
    }

    .failed-icon {
      background: transparent;

      svg {
        width: 20px;
        height: 20px;
      }
    }

    .pending-icon {
      background: #D1D5DB;
    }
  }

  .step-content {
    flex: 1;
    display: flex;
    flex-direction: column;
    gap: 0;
  }

  .step-name {
    font-size: 16px;
    line-height: 24px;
    color: #333;
  }

  &.completed {
    .step-name {
      color: #333;
    }
  }

  &.processing {
    .step-icon {
      .pending-icon {
        background: #2196F3;

        .step-dot {
          animation: pulse 1.5s ease-in-out infinite;
        }
      }
    }

    .step-name {
      color: #2196F3;
    }
  }

  &.failed {
    .step-name {
      color: #333;
    }
  }
}

.progress-loading {
  padding-left: 32px;
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: #9CA3AF;

  .loading-dots {
    display: inline-flex;
    align-items: center;
    gap: 4px;

    .dot {
      width: 6px;
      height: 6px;
      border-radius: 50%;
      background-color: #9CA3AF;
      animation: dotPulse 1.4s infinite ease-in-out both;

      &:nth-child(1) {
        animation-delay: -0.32s;
      }

      &:nth-child(2) {
        animation-delay: -0.16s;
      }
    }
  }
}

.progress-actions {
  margin-top: 16px;
  display: flex;
  justify-content: center;
}

.btn-regenerate-app {
  background: linear-gradient(-90deg, #2468F2 2.14%, #28A4FC 100%);
  border-radius: 6px;
  color: #FFFFFF;
  font-size: 14px;
  line-height: 20px;
  padding: 8px 16px;
  border: none;
  cursor: pointer;
  transition: all 0.2s ease;

  &:hover:not(:disabled) {
    box-shadow: 0 2px 8px rgba(59, 130, 246, 0.3);
  }

  &:active:not(:disabled) {
    transform: scale(0.98);
  }

  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
    background: #93C5FD;
  }
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

@keyframes slideIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }

  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes dotPulse {

  0%,
  80%,
  100% {
    transform: scale(0.6);
    opacity: 0.4;
  }

  40% {
    transform: scale(1);
    opacity: 1;
  }
}

@keyframes pulse {

  0%,
  100% {
    opacity: 1;
    transform: scale(1);
  }

  50% {
    opacity: 0.5;
    transform: scale(0.8);
  }
}
</style>
