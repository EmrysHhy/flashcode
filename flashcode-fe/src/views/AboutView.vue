<template>
  <div class="about-page">
    <Header />
    <AboutBanner />
    <AboutArticle />
    <Footer />
  </div>
</template>

<script setup>
import Header from '@/components/Header.vue';
import Footer from '@/components/Footer.vue';
import AboutBanner from '@/components/aboutComp/AboutBanner.vue';
import AboutArticle from '@/components/aboutComp/AboutArticle.vue';
</script>

<style scoped lang="scss">
.about-page {
  background-color: #f6f9fc;
  min-height: 100vh;
}
</style>
