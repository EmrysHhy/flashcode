<template>
    <section class="about-intro">
        <div class="container">
            <div class="intro-left">
                <div class="intro-title">项目介绍</div>
                <p class="intro-text">
                    本项目以新兴 AI
                    技术的实践应用为核心亮点，整合了传统开发必备的核心框架与技术栈。
                </p>
                <p class="intro-text">
                    内容涵盖通用项目架构的设计思路与落地技巧，快速开发迭代流程中的代码管控、编写设计及工程化实践；同时精炼实战指南高频考点与核心技术痛点，助力同学们在全面掌握技能后，征战项目或中软岗位面试，所向披靡。
                </p>
            </div>
            <div class="intro-right">
                <img :src="highlightImg" alt="项目亮点" />
            </div>
        </div>
    </section>
</template>

<script setup>
const highlightImg = new URL('@/imges/highlightPoint.png', import.meta.url).href;
</script>

<style scoped lang="scss">
.about-intro {
    padding: 64px 0 72px;
    background-color: #ffffff;
}

.container {
    max-width: 1200px;
    margin: 0 auto;
    padding: 0 24px;
    display: grid;
    grid-template-columns: 1fr 1fr;
    align-items: center;
    gap: 40px;
}

.intro-left {
    display: flex;
    flex-direction: column;
    gap: 24px;
}

.intro-title {
    position: relative;
    font-size: 24px;
    font-weight: 600;
    color: #1f2937;
}

.intro-title::before {
    content: "“";
    position: absolute;
    left: 3px;
    top: -6px;
    font-size: 82px;
    line-height: 52px;
    color: rgba(0, 0, 0, 0.08);
    pointer-events: none;
}

.intro-text {
    font-size: 16px;
    line-height: 27px;
    color: #333333;
    margin: 0;
}

.intro-right img {
    width: 100%;
    max-width: 612px;
    aspect-ratio: 612 / 374;
    object-fit: cover;
    border-radius: 4px;
    display: block;
    margin-left: auto;
}

@media (max-width: 900px) {
    .container {
        grid-template-columns: 1fr;
    }

    .intro-right img {
        margin: 0 auto;
    }
}
</style>
