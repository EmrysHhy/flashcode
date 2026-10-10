<template>
  <section class="about-article">
    <div class="layout">
      <nav class="toc" aria-label="项目说明目录">
        <button
          v-for="item in docs"
          :key="item.id"
          type="button"
          :class="['toc-item', { active: item.id === currentId }]"
          @click="currentId = item.id"
        >
          {{ item.title }}
        </button>
      </nav>
      <article class="doc" v-html="html"></article>
    </div>
  </section>
</template>

<script setup>
import { computed, ref } from 'vue';
import { marked } from 'marked';
import tech from '@/content/about/tech.md?raw';
import problems from '@/content/about/problems.md?raw';

const imageModules = import.meta.glob('@/content/about/images/*.{png,jpg,jpeg,webp,gif}', {
  eager: true,
  import: 'default',
});

const docs = [
  { id: 'tech', title: '技术选型', source: tech },
  { id: 'problems', title: '难点与解法', source: problems },
];

const currentId = ref(docs[0].id);

function resolveImages(html) {
  return html.replace(/src="([^"]+)"/g, (match, src) => {
    const name = src.split('/').pop();
    const entry = Object.entries(imageModules).find(([path]) => path.endsWith(`/${name}`));
    return entry ? `src="${entry[1]}"` : match;
  });
}

const html = computed(() => {
  const current = docs.find((item) => item.id === currentId.value) || docs[0];
  return resolveImages(marked.parse(current.source));
});
</script>

<style scoped lang="scss">
.about-article {
  padding: 48px 0 72px;
}

.layout {
  max-width: 1120px;
  margin: 0 auto;
  padding: 0 24px;
  display: grid;
  grid-template-columns: 200px 1fr;
  gap: 32px;
  align-items: start;
}

.toc {
  position: sticky;
  top: 88px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.toc-item {
  text-align: left;
  border: 0;
  background: transparent;
  border-radius: 8px;
  padding: 10px 12px;
  font-size: 15px;
  color: #4b5563;
  cursor: pointer;
}

.toc-item.active {
  background: #ffffff;
  color: #1d4ed8;
  font-weight: 600;
}

.doc {
  background: #ffffff;
  border-radius: 12px;
  padding: 36px 40px 48px;
  color: #1f2937;
  line-height: 1.75;
}

.doc :deep(h1) {
  margin: 0 0 20px;
  font-size: 28px;
  line-height: 1.3;
}

.doc :deep(h2) {
  margin: 32px 0 12px;
  font-size: 20px;
}

.doc :deep(p),
.doc :deep(li) {
  font-size: 16px;
}

.doc :deep(ul) {
  padding-left: 1.2em;
}

.doc :deep(code) {
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  font-size: 0.92em;
  background: #f3f4f6;
  padding: 0 4px;
  border-radius: 4px;
}

.doc :deep(img) {
  display: block;
  max-width: 100%;
  height: auto;
  margin: 12px 0;
  border-radius: 8px;
}

.doc :deep(a) {
  color: #1d4ed8;
}

@media (max-width: 800px) {
  .layout {
    grid-template-columns: 1fr;
  }

  .toc {
    position: static;
    flex-direction: row;
    flex-wrap: wrap;
  }

  .doc {
    padding: 24px 20px 32px;
  }
}
</style>
