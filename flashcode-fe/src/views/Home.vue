<template>
  <div class="home-page">
    <Header />
    <main>
      <ChatBox />
      <MyApps />
      <CaseGallery />
    </main>
    <Footer />
  </div>
</template>

<script setup>
import { ref, provide } from 'vue';
import Header from '@/components/Header.vue';
import ChatBox from '@/components/ChatBox.vue';
import MyApps from '@/components/MyApps.vue';
import CaseGallery from '@/components/CaseGallery.vue';
import Footer from '@/components/Footer.vue';

// 创建一个共享状态，表示是否正在生成需求文档
const isGeneratingRequirements = ref(false);

// 提供给子组件使用
provide('isGeneratingRequirements', isGeneratingRequirements);
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';

.home-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background-color: #ffffff;
}

main {
  flex: 1;
  display: flex;
  flex-direction: column;
}
</style>
