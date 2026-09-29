<template>
  <section class="chat-section" id="chat" :style="{ backgroundImage: `url(${bgImage})` }">
    <div class="container">
      <div class="section-header fade-in">
        <h2 class="section-title">说一句话，即刻生成你的专属应用</h2>
        <p class="section-description">告诉AI你的想法，让它帮你生成应用</p>
      </div>

      <div class="chat-container card fade-in">
        <div class="chat-messages" ref="messagesContainer">
          <div v-for="(message, index) in messages" :key="index" :class="['message', message.type]">
            <div class="message-avatar">
              <img v-if="message.type === 'user'" src="@/imges/self.webp" alt="用户头像" class="avatar-img">
              <img v-else src="@/imges/ai.webp" alt="AI头像" class="avatar-img">
            </div>
            <div class="message-content">
              <!-- 如果消息包含文件附件 -->
              <div v-if="message.file" class="message-file">
                <img v-if="message.file.type?.startsWith('image/')" :src="message.file.previewUrl" alt="附件图片"
                  class="message-file-image" />
                <div v-else class="message-file-doc">
                  <span class="file-doc-icon">📄</span>
                  <span class="file-doc-name">{{ message.file.name }}</span>
                </div>
              </div>
              <!-- 如果是 bot 消息且没有内容，显示加载状态 -->
              <div v-if="message.type === 'bot' && !message.text" class="typing-dots">
                <span></span>
                <span></span>
                <span></span>
              </div>
              <div v-else class="message-text" v-html="formatMessageText(message.text)"></div>
              <div class="message-time">{{ message.time }}</div>
            </div>
          </div>
        </div>

        <div class="chat-input-area">
          <!-- 文件预览区域 -->
          <div v-if="uploadedFile" class="file-preview">
            <div class="file-preview-item">
              <img v-if="isImageFile(uploadedFile)" :src="filePreviewUrl" alt="预览图" class="file-preview-image" />
              <div v-else class="file-preview-doc">
                <span class="file-icon">📄</span>
                <span class="file-name">{{ uploadedFile.name }}</span>
              </div>
              <button type="button" class="file-remove-btn" @click="removeFile" title="移除文件">
                ✕
              </button>
            </div>
          </div>

          <!-- 主输入框区域 -->
          <div class="main-input-container">
            <button type="button" class="upload-btn" @click="triggerFileUpload" title="上传文件">
              📎
            </button>
            <input ref="fileInput" type="file" @change="handleFileSelect" accept="image/*,.pdf,.doc,.docx,.txt"
              style="display: none" />
            <input v-model="inputMessage" @keyup.enter.prevent="sendMessage()" type="text" class="main-chat-input"
              placeholder="请为我生成一个" />
            <button type="button" @click="sendMessage()"
              :disabled="(!inputMessage.trim() && !uploadedFile) || isSending" class="main-send-btn">
              <span v-if="isSending" class="btn-loading">
                <span class="spinner"></span>
                生成中...
              </span>
              <span v-else>
                <svg width="16" height="16" viewBox="0 0 16 16" fill="none" xmlns="http://www.w3.org/2000/svg"
                  style="margin-right: 4px;transform: rotate(-180deg);">
                  <path d="M1 8L15 1L11 8L15 15L1 8Z" fill="currentColor" />
                </svg>
                发送
              </span>
            </button>
          </div>
        </div>
      </div>

      <div class="suggestions-section fade-in">
        <div class="suggestions-grid">
          <button v-for="(suggestion, index) in suggestions" :key="index" type="button" class="suggestion-card"
            @click="sendMessage(suggestion)">
            <span class="suggestion-text">{{ suggestion }}</span>
          </button>
        </div>
      </div>
    </div>
    <Login v-model:visible="showLoginModal" @success="handleLoginSuccess" />
  </section>
</template>

<script setup>
import { ref, nextTick, inject } from 'vue';
import { useRouter } from 'vue-router';
import { marked } from 'marked';
import { useUserStore } from '@/stores/user';
import Login from '@/components/Login.vue';
import { generateRequirements } from '@/apis';

const router = useRouter();
const userStore = useUserStore();
const showLoginModal = ref(false);

// 背景图片
const bgImage = new URL('@/imges/homeChatBoardBg.png', import.meta.url).href;

// 获取父组件提供的状态（如果存在）
const isGeneratingRequirements = inject('isGeneratingRequirements', null);

// 欢迎消息（始终是第一条）
const welcomeMessage = {
  type: 'bot',
  text: '你好！我是 Flash Code AI 助手。告诉我你想创建什么应用，我会帮你快速生成需求文档。',
  time: '刚刚'
};

const messages = ref([welcomeMessage]);

const inputMessage = ref('');
const messagesContainer = ref(null);
const isSending = ref(false); // 防止重复发送
const uploadedFile = ref(null); // 上传的文件
const filePreviewUrl = ref(''); // 文件预览URL
const fileInput = ref(null); // 文件输入元素引用

const suggestions = ref([
  '创建一个待办事项应用',
  '生成一个博客网站',
  '制作一个电商平台',
  '开发一个CRM系统'
]);

// 触发文件上传选择
const triggerFileUpload = () => {
  fileInput.value?.click();
};

// 处理文件选择
const handleFileSelect = (event) => {
  const file = event.target.files?.[0];
  if (!file) return;

  // 验证文件大小（限制为5MB，与后端配置一致）
  const maxSize = 5 * 1024 * 1024; // 5MB
  if (file.size > maxSize) {
    alert('文件大小不能超过 5MB');
    return;
  }

  uploadedFile.value = file;

  // 如果是图片，生成预览
  if (isImageFile(file)) {
    const reader = new FileReader();
    reader.onload = (e) => {
      filePreviewUrl.value = e.target.result;
    };
    reader.readAsDataURL(file);
  }

  // 清空文件输入，允许重复选择同一文件
  event.target.value = '';
};

// 判断是否为图片文件
const isImageFile = (file) => {
  return file && file.type.startsWith('image/');
};

// 移除已选择的文件
const removeFile = () => {
  uploadedFile.value = null;
  filePreviewUrl.value = '';
};

// 登录成功处理
const handleLoginSuccess = () => {
  showLoginModal.value = false;
};

const sendMessage = async (text = null) => {
  const messageText = text || inputMessage.value.trim();
  if ((!messageText && !uploadedFile.value) || isSending.value) return;

  // 检查登录状态
  if (!userStore.isLoggedIn) {
    showLoginModal.value = true;
    return;
  }

  // 设置正在生成状态
  if (isGeneratingRequirements) {
    isGeneratingRequirements.value = true;
  }

  // 添加用户消息
  const userMessage = {
    type: 'user',
    text: messageText || '上传了文件',
    time: getCurrentTime()
  };

  // 如果有文件，添加文件信息
  if (uploadedFile.value) {
    userMessage.file = {
      name: uploadedFile.value.name,
      type: uploadedFile.value.type,
      size: uploadedFile.value.size,
      previewUrl: filePreviewUrl.value
    };
  }

  // 确保欢迎消息始终在第一位，然后添加用户消息
  if (messages.value[0]?.text !== welcomeMessage.text) {
    messages.value.unshift(welcomeMessage);
  }
  messages.value.push(userMessage);

  // 保存文件引用用于API调用
  const fileToUpload = uploadedFile.value;

  inputMessage.value = '';
  uploadedFile.value = null;
  filePreviewUrl.value = '';
  isSending.value = true;

  // 滚动到底部
  await nextTick();
  scrollToBottom();

  // 添加一个空的 bot 消息，用于显示流式输出
  const botMessage = {
    type: 'bot',
    text: '',
    time: getCurrentTime()
  };
  messages.value.push(botMessage);
  const botMessageIndex = messages.value.length - 1;

  await nextTick();
  scrollToBottom();

  try {
    const result = await generateRequirements(messageText, fileToUpload);

    // 生成完成后，跳转到 AppView 页面
    if (result && result.document && result.document.trim()) {
      // 确保欢迎消息始终在第一位（在 ChatBox 中）
      if (messages.value[0]?.text !== welcomeMessage.text) {
        messages.value.unshift(welcomeMessage);
      }

      // 传递给 AppView 的消息：排除欢迎消息，只保留用户输入和后续的 bot 消息
      const messagesForAppView = messages.value
        .filter(msg => msg.text !== welcomeMessage.text) // 排除欢迎消息
        .filter(msg => msg.text && msg.text.trim()); // 排除空消息

      const stateData = {
        document: result.document,
        messages: JSON.parse(JSON.stringify(messagesForAppView))
      };

      sessionStorage.setItem('appViewData', JSON.stringify(stateData));

      if (result.appId) {
        router.push({ name: 'app', params: { id: String(result.appId) } });
      } else {
        router.push({ name: 'app' });
      }
    } else {
      throw new Error('生成的需求文档为空');
    }
  } catch (error) {
    console.error('调用接口失败:', error);
    // 确保欢迎消息始终在第一位
    if (messages.value[0]?.text !== welcomeMessage.text) {
      messages.value.unshift(welcomeMessage);
    }

    // 如果 bot 消息为空，则添加错误消息
    if (messages.value[botMessageIndex] && !messages.value[botMessageIndex].text) {
      messages.value[botMessageIndex].text = '抱歉，生成需求文档时出错了，请稍后再试。错误信息：' + (error.message || '未知错误');
    } else {
      // 否则添加一条新的错误消息
      messages.value.push({
        type: 'bot',
        text: '抱歉，生成需求文档时出错了，请稍后再试。错误信息：' + (error.message || '未知错误'),
        time: getCurrentTime()
      });
    }
    await nextTick();
    scrollToBottom();
  } finally {
    // 重置正在生成状态
    if (isGeneratingRequirements) {
      isGeneratingRequirements.value = false;
    }
    isSending.value = false;
  }
};

const getCurrentTime = () => {
  const now = new Date();
  return `${now.getHours()}:${now.getMinutes().toString().padStart(2, '0')}`;
};

const scrollToBottom = () => {
  if (messagesContainer.value) {
    // 使用平滑滚动，提升用户体验
    const container = messagesContainer.value;
    const isNearBottom = container.scrollHeight - container.scrollTop - container.clientHeight < 100;

    // 如果用户已经滚动到底部附近，自动跟随滚动
    // 如果用户向上滚动查看历史消息，则不强制滚动
    if (isNearBottom) {
      container.scrollTop = container.scrollHeight;
    }
  }
};

/**
 * 使用 marked 库将 Markdown 格式转换为 HTML
 * 配置 marked 以支持更好的渲染效果
 */
// 配置 marked 选项
marked.setOptions({
  breaks: true, // 支持 GitHub 风格的换行
  gfm: true, // 启用 GitHub 风格的 Markdown
});

const formatMessageText = (text) => {
  if (!text) return '';

  try {
    // 使用 marked 将 Markdown 转换为 HTML
    return marked.parse(text);
  } catch (error) {
    console.error('Markdown 解析错误:', error);
    // 如果解析失败，返回转义后的原始文本
    return text
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/\n/g, '<br>');
  }
};
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';

.chat-section {
  padding: $spacing-xxl $spacing-lg;
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
  position: relative;
  min-height: 600px;

  // 添加底部阴影，与下方内容区分
  &::after {
    content: '';
    position: absolute;
    bottom: 0;
    left: 0;
    right: 0;
    height: 1px;
    background: rgba(0, 0, 0, 0.05);
    box-shadow: 0 -4px 20px rgba(0, 0, 0, 0.05);
  }
}

.section-header {
  text-align: center;
  margin-bottom: $spacing-xl;

  .section-title {
    font-size: 48px;
    font-weight: 700;
    color: #09144A;
    margin-bottom: $spacing-md;
    line-height: 1.3;
  }

  .section-description {
    font-size: 18px;
    color: #3A456C;
    font-weight: 400;
  }
}

.chat-container {
  max-width: 900px;
  margin: 0 auto;
  min-height: 320px;
  max-height: 420px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background-color: #ffffff;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
  border-radius: 16px;
  backdrop-filter: blur(10px);
  transition: box-shadow $transition-base;

  &:hover {
    box-shadow: 0 8px 30px rgba(0, 0, 0, 0.12);
  }
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: $spacing-md;
  display: flex;
  flex-direction: column;
  gap: $spacing-sm;
  scroll-behavior: smooth;

  // 自定义滚动条样式
  &::-webkit-scrollbar {
    width: 6px;
  }

  &::-webkit-scrollbar-track {
    background: rgba(0, 0, 0, 0.03);
    border-radius: 3px;
  }

  &::-webkit-scrollbar-thumb {
    background: rgba(0, 0, 0, 0.15);
    border-radius: 3px;
    transition: background $transition-base;

    &:hover {
      background: rgba(0, 0, 0, 0.25);
    }
  }
}

.message {
  display: flex;
  gap: $spacing-sm;
  animation: slideIn 0.3s ease;
  opacity: 0;
  animation-fill-mode: forwards;

  &.user {
    flex-direction: row-reverse;

    .message-content {
      background: $gradient-primary;
      color: $text-white;
      align-items: flex-end;
      box-shadow: 0 2px 8px rgba(0, 123, 255, 0.15);
    }

    .message-time {
      color: rgba(255, 255, 255, 0.8);
    }
  }

  &.bot {
    .message-content {
      background-color: $bg-secondary;
      color: $text-primary;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
    }
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
  max-width: 70%;
  padding: $spacing-sm $spacing-md;
  border-radius: $border-radius;
  transition: transform $transition-base;

  // Bot 消息内容区域，确保文档格式正确显示
  .message.bot & {
    max-width: 85%;
    padding: $spacing-md $spacing-lg;
  }
}

.message-file {
  margin-bottom: $spacing-xs;
}

.message-file-image {
  max-width: 200px;
  max-height: 200px;
  border-radius: $border-radius;
  object-fit: cover;
  cursor: pointer;
  transition: transform $transition-base;

  &:hover {
    transform: scale(1.02);
  }
}

.message-file-doc {
  display: flex;
  align-items: center;
  gap: $spacing-xs;
  padding: $spacing-xs $spacing-sm;
  background: rgba(0, 0, 0, 0.05);
  border-radius: $border-radius;
  font-size: $font-size-xs;
}

.file-doc-icon {
  font-size: $font-size-md;
}

.file-doc-name {
  color: $text-secondary;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 150px;
}

.message-text {
  font-size: $font-size-sm;
  line-height: 1.6;
  word-wrap: break-word;
  overflow-wrap: break-word;
  display: block;
  text-align: left;

  // 确保 Markdown 渲染后的 HTML 元素正确显示
  >* {
    display: block;
    text-align: left;
  }

  // 内联元素保持内联
  >strong,
  >em,
  >code,
  >a {
    display: inline;
  }

  // Markdown 样式 - 文档格式
  :deep(h1) {
    font-size: 1.6em;
    font-weight: 700;
    margin: 0 0 $spacing-md 0;
    padding: 0;
    color: $text-primary;
    line-height: 1.3;
    text-align: left;
  }

  :deep(h2) {
    font-size: 1.3em;
    font-weight: 600;
    margin: $spacing-lg 0 $spacing-sm 0;
    color: $text-primary;
    line-height: 1.4;
    text-align: left;
  }

  :deep(h3) {
    font-size: 1.15em;
    font-weight: 600;
    margin: $spacing-md 0 $spacing-xs 0;
    color: $text-primary;
    line-height: 1.4;
  }

  :deep(h4) {
    font-size: 1.05em;
    font-weight: 600;
    margin: $spacing-sm 0 $spacing-xs 0;
    color: $text-primary;
    line-height: 1.4;
  }

  :deep(ul),
  :deep(ol) {
    margin: $spacing-sm 0 $spacing-sm 0;
    padding-left: $spacing-lg;
    line-height: 1.6;
  }

  :deep(ul) {
    list-style-type: disc;
    list-style-position: outside;
  }

  :deep(ol) {
    list-style-type: decimal;
    list-style-position: outside;
  }

  :deep(li) {
    margin: $spacing-sm 0;
    line-height: 1.6;
    padding-left: $spacing-xs;

    // 列表项中的加粗文本（如 "3.1 页面头部导航栏"）
    strong {
      font-weight: 600;
      color: $text-primary;
    }

    // 嵌套列表
    ul,
    ol {
      margin-top: $spacing-sm;
      margin-bottom: $spacing-sm;
      margin-left: $spacing-md;
    }

    // 列表项中的段落
    p {
      margin: 0;
      display: inline;
    }

    p+p {
      margin-top: $spacing-xs;
      display: block;
    }
  }

  :deep(p) {
    margin: $spacing-sm 0;
    line-height: 1.6;
    color: $text-primary;
    text-align: left;

    // 段落中的加粗文本
    strong {
      font-weight: 600;
      color: $text-primary;
    }
  }

  :deep(blockquote) {
    border-left: 3px solid $primary-color;
    padding-left: $spacing-sm;
    margin: $spacing-sm 0;
    color: $text-secondary;
    font-style: italic;
  }

  :deep(table) {
    width: 100%;
    border-collapse: collapse;
    margin: $spacing-sm 0;

    th,
    td {
      border: 1px solid $border-color;
      padding: $spacing-sm $spacing-md;
      text-align: left;
    }

    th {
      background-color: $bg-secondary;
      font-weight: 600;
    }
  }

  :deep(a) {
    color: $primary-color;
    text-decoration: none;

    &:hover {
      text-decoration: underline;
    }
  }

  :deep(hr) {
    border: none;
    border-top: 1px solid $border-color;
    margin: $spacing-md 0;
  }

  :deep(code) {
    background-color: rgba(0, 0, 0, 0.05);
    padding: 2px 5px;
    border-radius: 3px;
    font-family: 'Courier New', monospace;
    font-size: 0.85em;
  }

  :deep(pre) {
    background-color: rgba(0, 0, 0, 0.05);
    padding: $spacing-sm;
    border-radius: $border-radius;
    overflow-x: auto;
    margin: $spacing-sm 0;
    border-left: 3px solid $primary-color;

    code {
      background-color: transparent;
      padding: 0;
      font-size: 0.85em;
    }
  }

  // 确保文档结构清晰
  :deep(*:first-child) {
    margin-top: 0;
  }

  :deep(*:last-child) {
    margin-bottom: 0;
  }

  // 确保文档第一行和最后一行没有多余间距
  :deep(> h1:first-child) {
    margin-top: 0;
  }

  :deep(> *:last-child) {
    margin-bottom: 0;
  }
}

.message-time {
  font-size: $font-size-xs;
  color: $text-light;
}

.typing-indicator {
  .typing-dots {
    display: flex;
    gap: $spacing-xs;
    padding: $spacing-sm 0;

    span {
      width: 8px;
      height: 8px;
      background-color: $text-light;
      border-radius: 50%;
      animation: typing 1.4s infinite;

      &:nth-child(2) {
        animation-delay: 0.2s;
      }

      &:nth-child(3) {
        animation-delay: 0.4s;
      }
    }
  }
}

@keyframes typing {

  0%,
  60%,
  100% {
    transform: translateY(0);
    opacity: 0.7;
  }

  30% {
    transform: translateY(-10px);
    opacity: 1;
  }
}

.chat-input-area {
  border-top: 1px solid $border-color;
  padding: $spacing-md 0 0;
  backdrop-filter: blur(10px);
}

.file-preview {
  margin-bottom: $spacing-sm;
}

.file-preview-item {
  position: relative;
  display: inline-block;
  border-radius: $border-radius;
  background: rgba(0, 0, 0, 0.03);
  padding: $spacing-xs;
  animation: fadeIn 0.3s ease;
}

.file-preview-image {
  max-width: 120px;
  max-height: 120px;
  border-radius: $border-radius;
  object-fit: cover;
  display: block;
}

.file-preview-doc {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
  padding: $spacing-sm $spacing-md;
}

.file-icon {
  font-size: $font-size-lg;
}

.file-name {
  font-size: $font-size-xs;
  color: $text-secondary;
  max-width: 200px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.file-remove-btn {
  position: absolute;
  top: -6px;
  right: -6px;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: $primary-color;
  color: white;
  border: 2px solid white;
  cursor: pointer;
  font-size: 12px;
  line-height: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all $transition-base;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.2);

  &:hover {
    background: darken($primary-color, 10%);
    transform: scale(1.1);
  }
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: scale(0.9);
  }

  to {
    opacity: 1;
    transform: scale(1);
  }
}

.suggestions-section {
  max-width: 900px;
  margin: $spacing-lg auto 0;
  padding: 0 $spacing-md;
}

.suggestions-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: $spacing-sm;
}

.suggestion-card {
  padding: 12px 20px;
  background: rgba(255, 255, 255, 0.8);
  border: 1px solid rgba(229, 231, 235, 0.8);
  border-radius: 8px;
  font-size: 14px;
  color: #4B5563;
  cursor: pointer;
  transition: all $transition-base;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.04);
  text-align: center;
  font-weight: 400;
  position: relative;
  overflow: hidden;
  backdrop-filter: blur(10px);

  &:hover {
    background: rgba(255, 255, 255, 1);
    border-color: rgba(77, 144, 254, 0.3);
    transform: translateY(-2px);
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);

    .suggestion-text {
      color: $primary-color;
    }
  }

  &:active {
    transform: translateY(0);
  }
}

.suggestion-text {
  display: block;
  transition: color $transition-base;
}

.main-input-container {
  display: flex;
  gap: $spacing-sm;
  align-items: center;
}

.main-chat-input {
  flex: 1;
  padding: 14px 0;
  border: none;
  border-radius: 8px;
  font-size: 16px;
  transition: all $transition-base;
  background: transparent;
  height: auto;

  &:focus {
    outline: none;
  }

  &::placeholder {
    color: #9ca3af;
  }

}

.main-send-btn {
  padding: 14px 24px;
  white-space: nowrap;
  font-size: 16px;
  font-weight: 500;
  height: auto;
  border-radius: 8px;
  background: rgba($color: #3887F9, $alpha: 0.5);
  color: white;
  border: none;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all $transition-base;

  &:not(:disabled):hover {
    background: darken($primary-color, 5%);
    transform: translateY(-1px);
    box-shadow: 0 4px 12px rgba(0, 123, 255, 0.3);
  }

  &:not(:disabled):active {
    transform: translateY(0);
  }

  &:disabled {
    opacity: 0.6;
    cursor: not-allowed;
  }
}

.upload-btn {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  background: transparent;
  border: none;
  cursor: pointer;
  font-size: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all $transition-base;
  flex-shrink: 0;
  color: #64748b;

  &:hover {
    color: $primary-color;
    background: rgba(0, 123, 255, 0.05);
  }

  &:active {
    transform: scale(0.95);
  }
}

.main-input-wrapper .upload-btn {
  width: 44px;
  height: 44px;
  font-size: 22px;
}

.btn-loading {
  display: flex;
  align-items: center;
  gap: $spacing-xs;
}

.spinner {
  width: 12px;
  height: 12px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top-color: white;
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

@media (max-width: $breakpoint-mobile) {
  .chat-section {
    padding: $spacing-lg $spacing-md;
  }

  .chat-container {
    height: 280px;
  }

  .message-content {
    max-width: 85%;
  }

  .message-file-image {
    max-width: 150px;
    max-height: 150px;
  }

  .file-preview-image {
    max-width: 100px;
    max-height: 100px;
  }

  .suggestions-grid {
    grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
    gap: $spacing-xs;
  }

  .suggestion-card {
    padding: $spacing-sm $spacing-md;
    font-size: $font-size-xs;
  }

  .upload-btn {
    width: 32px;
    height: 32px;
    font-size: $font-size-md;
  }
}
</style>
