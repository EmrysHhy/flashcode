<template>
    <div class="cases-page">
        <Header />
        <CaseBanner />
        <CaseList />
        <Footer />
    </div>
</template>

<script setup>
import CaseBanner from '@/components/caseComp/CaseBanner.vue';
import CaseList from '@/components/caseComp/CaseList.vue';
</script>

<style scoped lang="scss">
.cases-page {
    background-color: #f6f9fc;
    min-height: 100vh;
}
</style>
