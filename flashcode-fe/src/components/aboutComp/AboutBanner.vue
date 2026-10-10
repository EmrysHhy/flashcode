<template>
  <section class="about-banner">
    <div class="banner-bg" :style="{ backgroundImage: `url(${bannerBg})` }">
      <h1 class="banner-title">Flashcode</h1>
    </div>
  </section>
</template>

<script setup>
const bannerBg = new URL('@/imges/aboutBg.webp', import.meta.url).href;
</script>

<style scoped lang="scss">
.about-banner {
  width: 100%;
  background-color: #eaf2ff;
}

.banner-bg {
  margin: 0 auto;
  height: 260px;
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
  display: flex;
  align-items: center;
  justify-content: center;
}

.banner-title {
  font-size: 42px;
  color: #ffffff;
  margin: 0;
  font-weight: 700;
}

@media (max-width: 600px) {
  .banner-title {
    font-size: 32px;
  }
}
</style>
