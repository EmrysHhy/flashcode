<template>
    <section class="cases-banner">
        <div class="banner-bg" :style="{ backgroundImage: `url(${bannerBg})` }">
            <div class="banner-content">
                <h1 class="banner-title">案例广场</h1>
                <p class="banner-subtitle">丰富的应用资源，帮助快速搭建应用</p>
            </div>
        </div>
    </section>
</template>

<script setup>
const bannerBg = new URL('@/imges/casePageBg.webp', import.meta.url).href;
</script>

<style scoped lang="scss">
.cases-banner {
    width: 100%;
    background-color: #eaf2ff;
}

.banner-bg {
    margin: 0 auto;
    height: 240px;
    background-size: cover;
    background-position: center;
    background-repeat: no-repeat;
    display: flex;
    align-items: center;
    justify-content: center;
}

.banner-content {
    text-align: center;
}

.banner-title {
    font-size: 42px;
    color: #09144a;
    margin: 0 0 8px 0;
    font-weight: 700;
}

.banner-subtitle {
    font-size: 18px;
    color: #3a456c;
    margin: 0;
}

@media (max-width: 600px) {
    .banner-title {
        font-size: 32px;
    }

    .banner-subtitle {
        font-size: 16px;
    }
}
</style>
