<template>
  <div class="knowledge-list-container">
    <h2 class="section-title">📚 宠物知识文章列表</h2>
    
    <!-- 分类筛选 -->
    <div class="filter-section">
      <select v-model="selectedCategory" class="category-select">
        <option value="">全部主题</option>
        <option value="care">日常护理</option>
        <option value="health">健康保健</option>
        <option value="training">训练技巧</option>
        <option value="nutrition">营养饮食</option>
      </select>
    </div>
    
    <!-- 文章列表 -->
    <div class="article-list">
      <div 
        v-for="article in filteredArticles" 
        :key="article.id" 
        class="article-card"
      >
        <div class="article-header">
          <h3 class="article-title">{{ article.title }}</h3>
          <span class="article-category">{{ getCategoryText(article.category) }}</span>
        </div>
        
        <p class="article-summary">{{ article.summary }}</p>
        
        <div class="article-meta">
          <span class="article-date">{{ formatDate(article.publishDate) }}</span>
          <span class="article-author">作者: {{ article.author }}</span>
        </div>
      </div>
    </div>
    
    <!-- 无结果提示 -->
    <div v-if="filteredArticles.length === 0" class="no-results">
      <p>暂无符合条件的文章</p>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'

// 模拟知识文章数据
const articles = ref([
  {
    id: 1,
    title: '猫咪日常护理全攻略',
    category: 'care',
    summary: '详细介绍猫咪的日常护理要点，包括梳理毛发、清洁耳朵、修剪指甲等实用技巧。',
    publishDate: '2023-10-15',
    author: '宠物专家小王'
  },
  {
    id: 2,
    title: '狗狗健康检查要点',
    category: 'health',
    summary: '教你如何在家为狗狗进行基础健康检查，及时发现潜在健康问题。',
    publishDate: '2023-10-10',
    author: '兽医师李医生'
  },
  {
    id: 3,
    title: '幼犬社会化训练指南',
    category: 'training',
    summary: '幼犬社会化训练的重要性及具体方法，帮助狗狗更好地适应社会环境。',
    publishDate: '2023-10-05',
    author: '训犬师张教练'
  },
  {
    id: 4,
    title: '猫咪营养搭配秘籍',
    category: 'nutrition',
    summary: '科学分析猫咪的营养需求，推荐合理的饮食搭配方案。',
    publishDate: '2023-09-28',
    author: '营养师陈老师'
  },
  {
    id: 5,
    title: '狗狗运动锻炼计划',
    category: 'care',
    summary: '根据不同品种和年龄的狗狗制定合适的运动计划，保持狗狗身体健康。',
    publishDate: '2023-09-20',
    author: '宠物健身师刘教练'
  },
  {
    id: 6,
    title: '常见宠物疾病预防',
    category: 'health',
    summary: '介绍常见宠物疾病的症状、预防措施和早期干预方法。',
    publishDate: '2023-09-15',
    author: '兽医师赵医生'
  },
  {
    id: 7,
    title: '猫咪行为解读',
    category: 'training',
    summary: '解读猫咪的各种行为语言，增进与猫咪的情感交流。',
    publishDate: '2023-09-10',
    author: '动物行为学家孙博士'
  },
  {
    id: 8,
    title: '狗狗换季护理注意事项',
    category: 'care',
    summary: '季节变换时狗狗的特殊护理需求，确保狗狗健康度过换季期。',
    publishDate: '2023-09-05',
    author: '宠物护理师周老师'
  }
])

const selectedCategory = ref('')

const getCategoryText = (category) => {
  const categories = {
    care: '日常护理',
    health: '健康保健',
    training: '训练技巧',
    nutrition: '营养饮食'
  }
  return categories[category] || '其他'
}

const formatDate = (dateString) => {
  const date = new Date(dateString)
  return date.toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: 'long',
    day: 'numeric'
  })
}

const filteredArticles = computed(() => {
  if (!selectedCategory.value) {
    return articles.value
  }
  return articles.value.filter(article => article.category === selectedCategory.value)
})
</script>

<style scoped>
.knowledge-list-container {
  width: 100%;
}

.section-title {
  text-align: center;
  margin-bottom: 2rem;
  color: #333;
  font-size: 1.8rem;
}

.filter-section {
  text-align: center;
  margin-bottom: 2rem;
}

.category-select {
  padding: 0.5rem 1rem;
  border: 2px solid #ddd;
  border-radius: 4px;
  font-size: 1rem;
  background-color: white;
}

.article-list {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.article-card {
  background: white;
  border-radius: 8px;
  padding: 1.5rem;
  box-shadow: 0 4px 6px rgba(0,0,0,0.1);
  transition: transform 0.3s, box-shadow 0.3s;
}

.article-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 12px rgba(0,0,0,0.15);
}

.article-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 1rem;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.article-title {
  font-size: 1.3rem;
  color: #333;
  margin: 0;
  flex: 1;
  min-width: 200px;
}

.article-category {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  padding: 0.25rem 0.75rem;
  border-radius: 12px;
  font-size: 0.8rem;
  white-space: nowrap;
}

.article-summary {
  color: #666;
  line-height: 1.6;
  margin-bottom: 1rem;
  font-size: 1rem;
}

.article-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  color: #999;
  font-size: 0.9rem;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.article-date {
  font-weight: 500;
}

.article-author {
  font-style: italic;
}

.no-results {
  text-align: center;
  padding: 3rem;
  color: #999;
  font-size: 1.1rem;
}

@media (max-width: 768px) {
  .article-header {
    flex-direction: column;
    align-items: stretch;
  }
  
  .article-title {
    margin-bottom: 0.5rem;
  }
  
  .article-meta {
    flex-direction: column;
    align-items: flex-start;
  }
  
  .category-select {
    min-width: 100%;
    max-width: 300px;
  }
}
</style>