<template>
  <div v-if="visible" class="login-overlay">
    <div class="login-modal">
      <button class="login-close" aria-label="关闭" @click="handleClose">×</button>
      <div class="login-header">
        <h1 class="login-title">Flash Code</h1>
        <p class="login-subtitle">让零代码开发变得更简单、更高效、更智能</p>
      </div>

      <el-form ref="loginFormRef" :model="loginForm" :rules="loginRules" class="login-form"
        @submit.prevent="handleLogin">
        <el-form-item prop="account">
          <el-input v-model="loginForm.account" placeholder="请输入邮箱" size="large" clearable>
            <template #prefix>
              <span class="login-input-icon" v-html="emailSvg" aria-hidden="true"></span>
            </template>
          </el-input>
        </el-form-item>

        <el-form-item prop="code">
          <el-input v-model="loginForm.code" placeholder="请输入验证码" size="large" maxlength="6">
            <template #prefix>
              <span class="login-input-icon" v-html="lockSvg" aria-hidden="true"></span>
            </template>
            <template #append>
              <el-button :disabled="countdown > 0" :loading="sendingCode" text @click="handleSendCode">
                {{ countdown > 0 ? `${countdown}s后重新获取` : '获取验证码' }}
              </el-button>
            </template>
          </el-input>
        </el-form-item>

        <el-form-item class="login-submit">
          <el-button type="primary" size="large" class="login-button" :loading="loggingIn"
            native-type="submit" @click="handleLogin">
            登录/注册
          </el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onUnmounted } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { sendVerificationCode, loginWithCode } from '@/apis';
import { useUserStore } from '@/stores/user';
import emailSvg from '@/imges/email.svg?raw';
import lockSvg from '@/imges/lock.svg?raw';

const props = defineProps({
  visible: {
    type: Boolean,
    default: true,
  },
});

const emit = defineEmits(['update:visible', 'close', 'success']);

const router = useRouter();
const userStore = useUserStore();

// 表单引用
const loginFormRef = ref(null);

// 表单数据
const loginForm = reactive({
  account: '',
  code: '',
});

// 自定义验证器：验证邮箱
const validateAccount = (rule, value, callback) => {
  if (!value) {
    callback(new Error('请输入邮箱'));
    return;
  }

  const emailRegex = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/;

  if (emailRegex.test(value)) {
    callback();
  } else {
    callback(new Error('请输入正确的邮箱格式'));
  }
};

// 表单验证规则
const loginRules = {
  account: [
    { required: true, validator: validateAccount, trigger: 'blur' },
  ],
  code: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    {
      pattern: /^\d{6}$/,
      message: '请输入6位数字验证码',
      trigger: 'blur',
    },
  ],
};

// 状态管理
const sendingCode = ref(false);
const loggingIn = ref(false);
const countdown = ref(0);
let countdownTimer = null;

// 关闭弹层
const handleClose = () => {
  emit('update:visible', false);
  emit('close');
};

// 启动倒计时
const startCountdown = () => {
  countdown.value = 60;
  countdownTimer = setInterval(() => {
    countdown.value--;
    if (countdown.value <= 0) {
      clearInterval(countdownTimer);
      countdownTimer = null;
    }
  }, 1000);
};

// 发送验证码
const handleSendCode = async () => {
  try {
    await loginFormRef.value.validateField('account');
  } catch (error) {
    return;
  }

  sendingCode.value = true;
  try {
    await sendVerificationCode({ account: loginForm.account });
    ElMessage.success('验证码已发送，请注意查收');
    startCountdown();
  } catch (error) {
    ElMessage.error(error.message || '发送验证码失败');
  } finally {
    sendingCode.value = false;
  }
};

// 登录
const handleLogin = async () => {
  try {
    await loginFormRef.value.validate();
  } catch (error) {
    return;
  }

  loggingIn.value = true;
  try {
    const result = await loginWithCode({ email: loginForm.account, code: loginForm.code });
    userStore.setUserInfo(result);
    ElMessage.success('登录成功');
    emit('success');
    emit('update:visible', false);
    router.push('/');
  } catch (error) {
    ElMessage.error(error.message || '登录失败');
  } finally {
    loggingIn.value = false;
  }
};

// 组件卸载时清理定时器
onUnmounted(() => {
  if (countdownTimer) {
    clearInterval(countdownTimer);
  }
});
</script>

<style scoped lang="scss">
$primary-color: #4d90fe;
$text-color: #333;
$text-light: #999999;
$border-color: #e5e5e5;
$overlay-bg: rgba(0, 0, 0, 0.45);

.login-overlay {
  position: fixed;
  inset: 0;
  background: $overlay-bg;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2000;
  backdrop-filter: blur(2px);
  padding: 16px;
}

.login-modal {
  width: 520px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.18);
  padding: 32px 40px 40px;
  position: relative;
}

.login-close {
  position: absolute;
  right: 16px;
  top: 12px;
  border: none;
  background: transparent;
  font-size: 24px;
  color: #999;
  cursor: pointer;
  line-height: 1;
}

.login-header {
  text-align: center;
  margin-bottom: 32px;
}

.login-title {
  margin: 0 0 12px 0;
  font-size: 36px;
  font-weight: 700;
  color: $text-color;
}

.login-subtitle {
  margin: 0;
  font-size: 16px;
  color: $text-light;
}

.login-form {
  .el-form-item {
    margin-bottom: 24px;
  }

  :deep(.el-input) {
    position: relative;
  }

  :deep(.el-input__wrapper) {
    box-shadow: none;
    border: 1px solid $border-color;
    border-radius: 8px;
    height: 58px;
    padding: 0 12px;
  }

  // 聚焦时高亮，前提：所在表单项未处于错误态
  :deep(.el-form-item:not(.is-error) .el-input__wrapper.is-focus) {
    border-color: #3b82f6;
  }

  // 错误态保持红色，防止被聚焦样式覆盖
  :deep(.el-form-item.is-error .el-input__wrapper) {
    border-color: var(--el-color-danger);
  }

  :deep(.el-input__inner) {
    font-size: 16px;
    color: $text-color;
    padding-right: 8px;
  }

  :deep(.el-input__prefix) {
    /* 图标颜色来自 currentColor（ElementPlus 默认会给 prefix 区域一个灰色） */
    color: var(--el-text-color-placeholder);
  }

  :deep(.el-input-group__append) {
    width: 100px;
    position: absolute;
    right: 0;
    top: 50%;
    transform: translateY(-50%);
    border: none;
    padding: 0;
    background: transparent;
    box-shadow: none;
    margin: 0;
    display: flex;
    align-items: center;

    .el-button {
      color: $primary-color;
      font-size: 14px;
      font-weight: 500;
      border: none;
      padding: 0;
      height: auto;
      background: transparent;
      box-shadow: none;
    }

    .el-button:hover {
      background: transparent;
      color: $primary-color;
    }

    .el-button:disabled {
      color: $text-light;
    }
  }
}

.login-input-icon {
  width: 20px;
  height: 20px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

/* v-html 注入的 svg 不带 scoped 属性，需要 deep 才能生效 */
:deep(.login-input-icon svg) {
  width: 20px;
  height: 20px;
}

.login-submit {
  margin-top: 16px;
}

.login-button {
  width: 100%;
  height: 54px;
  font-size: 18px;
  font-weight: 600;
  background: $primary-color;
  border: none;
}
</style>
