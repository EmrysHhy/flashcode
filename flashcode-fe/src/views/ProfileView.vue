<template>
  <div class="profile-page">
    <Header />
    <main class="profile">
      <section class="profile__card" v-if="profile">
        <div class="profile__avatar">
          <img v-if="profile.avatar" :src="profile.avatar" alt="头像" />
          <span v-else>我</span>
          <label class="profile__avatar-action">
            更换头像
            <input type="file" accept="image/*" @change="handleAvatar" />
          </label>
        </div>

        <div class="profile__fields">
          <label>
            <span>昵称</span>
            <input v-model="form.nickName" type="text" maxlength="20" />
          </label>
          <label>
            <span>邮箱</span>
            <input :value="profile.email || '未绑定'" type="text" disabled />
          </label>
          <label>
            <span>手机号</span>
            <input :value="profile.phoneNumber || '未绑定'" type="text" disabled />
          </label>
          <button class="profile__save" type="button" :disabled="savingName" @click="saveName">
            保存昵称
          </button>
        </div>

        <div v-if="bindTarget" class="profile__bind">
          <h3>{{ bindTarget === 'phone' ? '绑定手机号' : '绑定邮箱' }}</h3>
          <p class="profile__hint">
            {{ bindTarget === 'phone' ? '验证码会发到已绑定的邮箱' : '验证码会发到已绑定的手机号' }}
          </p>
          <label>
            <span>{{ bindTarget === 'phone' ? '手机号' : '邮箱' }}</span>
            <input v-model="bindValue" type="text" />
          </label>
          <label v-if="codeSent">
            <span>验证码</span>
            <input v-model="code" type="text" maxlength="6" />
          </label>
          <button class="profile__save" type="button" :disabled="binding" @click="submitBind">
            {{ codeSent ? '确认绑定' : '发送验证码' }}
          </button>
        </div>
        <p v-else-if="!profile.email && !profile.phoneNumber" class="profile__hint">
          当前没有邮箱和手机号，只能修改昵称。
        </p>

        <button class="profile__logout" type="button" @click="handleLogout">退出登录</button>
      </section>
    </main>
    <Footer />
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import Header from '@/components/Header.vue';
import Footer from '@/components/Footer.vue';
import { fetchProfile, logout, updateAvatar, updateProfile } from '@/apis';
import { useUserStore } from '@/stores/user';

const router = useRouter();
const userStore = useUserStore();
const profile = ref(null);
const form = reactive({ nickName: '' });
const bindValue = ref('');
const code = ref('');
const codeSent = ref(false);
const savingName = ref(false);
const binding = ref(false);

const bindTarget = computed(() => {
  if (!profile.value) return '';
  if (profile.value.email && !profile.value.phoneNumber) return 'phone';
  if (profile.value.phoneNumber && !profile.value.email) return 'email';
  return '';
});

const loadProfile = async () => {
  const data = await fetchProfile();
  profile.value = data;
  form.nickName = data?.nickName || '';
  userStore.setProfile(data);
};

const saveName = async () => {
  const nickName = form.nickName.trim();
  if (!nickName) {
    ElMessage.warning('请输入昵称');
    return;
  }
  savingName.value = true;
  try {
    await updateProfile({ nickName });
    await loadProfile();
    ElMessage.success('昵称已更新');
  } catch (error) {
    ElMessage.error(error.message || '保存失败');
  } finally {
    savingName.value = false;
  }
};

const handleAvatar = async (event) => {
  const file = event.target.files?.[0];
  event.target.value = '';
  if (!file) return;
  try {
    const url = await updateAvatar(file);
    profile.value = { ...profile.value, avatar: url };
    userStore.setProfile(profile.value);
    ElMessage.success('头像已更新');
  } catch (error) {
    ElMessage.error(error.message || '上传头像失败');
  }
};

const submitBind = async () => {
  const value = bindValue.value.trim();
  if (!value) {
    ElMessage.warning(bindTarget.value === 'phone' ? '请输入手机号' : '请输入邮箱');
    return;
  }
  if (codeSent.value && !code.value.trim()) {
    ElMessage.warning('请输入验证码');
    return;
  }
  binding.value = true;
  try {
    const payload = bindTarget.value === 'phone'
      ? { phone: value, code: codeSent.value ? code.value.trim() : undefined }
      : { email: value, code: codeSent.value ? code.value.trim() : undefined };
    await updateProfile(payload);
    if (!codeSent.value) {
      codeSent.value = true;
      ElMessage.success('验证码已发送');
      return;
    }
    codeSent.value = false;
    code.value = '';
    bindValue.value = '';
    await loadProfile();
    ElMessage.success('绑定成功');
  } catch (error) {
    ElMessage.error(error.message || '绑定失败');
  } finally {
    binding.value = false;
  }
};

const handleLogout = async () => {
  if (!window.confirm('确定要退出登录吗？')) return;
  try {
    await logout();
  } catch (error) {
    console.error(error);
  }
  userStore.clearUserInfo();
  ElMessage.success('已退出登录');
  router.push('/home');
};

onMounted(async () => {
  if (!userStore.isLoggedIn) {
    router.replace('/home');
    return;
  }
  try {
    await loadProfile();
  } catch (error) {
    ElMessage.error(error.message || '获取用户信息失败');
  }
});
</script>

<style scoped lang="scss">
.profile-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f6f9fc;
}

.profile {
  flex: 1;
  width: min(720px, calc(100% - 48px));
  margin: 40px auto;
}

.profile__card {
  background: #fff;
  border-radius: 16px;
  padding: 32px;
  box-shadow: 0 8px 24px rgba(26, 31, 54, 0.06);
}

.profile__avatar {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 28px;
}

.profile__avatar img,
.profile__avatar span {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  object-fit: cover;
}

.profile__avatar span {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: #e6f4ff;
  color: #1890ff;
  font-size: 24px;
}

.profile__avatar-action {
  color: #1890ff;
  cursor: pointer;
  font-size: 14px;
}

.profile__avatar-action input {
  display: none;
}

.profile__fields,
.profile__bind {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.profile__bind {
  margin-top: 28px;
  padding-top: 24px;
  border-top: 1px solid #eef2f6;
}

.profile__bind h3 {
  margin: 0;
  font-size: 16px;
  color: #1a1f36;
}

label {
  display: flex;
  flex-direction: column;
  gap: 8px;
  color: #6b7280;
  font-size: 13px;
}

input {
  height: 40px;
  padding: 0 12px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  color: #1a1f36;
  font-size: 14px;
}

input:disabled {
  background: #f8fafc;
}

.profile__save,
.profile__logout {
  height: 40px;
  border: none;
  border-radius: 8px;
  cursor: pointer;
}

.profile__save {
  background: #1890ff;
  color: #fff;
}

.profile__save:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.profile__hint {
  margin: 16px 0 0;
  color: #6b7280;
  font-size: 13px;
}

.profile__logout {
  margin-top: 28px;
  background: transparent;
  color: #1890ff;
  padding: 0 16px;
}
</style>
