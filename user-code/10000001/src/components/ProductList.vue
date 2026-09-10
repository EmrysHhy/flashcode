<template>
  <div class="product-list-container">
    <h2 class="section-title">🐱🐶 宠物商品列表</h2>
    
    <!-- 搜索和筛选 -->
    <div class="search-filter">
      <input 
        v-model="searchKeyword" 
        placeholder="搜索商品..." 
        class="search-input"
      />
      
      <select v-model="selectedCategory" class="category-select">
        <option value="">全部分类</option>
        <option value="cat">猫咪用品</option>
        <option value="dog">狗狗用品</option>
        <option value="food">宠物食品</option>
        <option value="health">健康护理</option>
      </select>
    </div>
    
    <!-- 商品列表 -->
    <div class="product-grid">
      <div 
        v-for="product in filteredProducts" 
        :key="product.id" 
        class="product-card"
      >
        <img :src="product.image" :alt="product.name" class="product-image" />
        <div class="product-info">
          <h3 class="product-name">{{ product.name }}</h3>
          <p class="product-category">{{ getCategoryText(product.category) }}</p>
          <p class="product-price">¥{{ product.price }}</p>
          <p class="product-spec">规格: {{ product.specification }}</p>
          <button class="buy-button">加入购物车</button>
        </div>
      </div>
    </div>
    
    <!-- 无结果提示 -->
    <div v-if="filteredProducts.length === 0" class="no-results">
      <p>暂无符合条件的商品</p>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'

// 模拟商品数据
const products = ref([
  {
    id: 1,
    name: '优质猫粮',
    category: 'cat',
    price: 89.9,
    specification: '1.5kg/袋',
    image: 'https://via.placeholder.com/200x200/FFB6C1/FFFFFF?text=猫粮'
  },
  {
    id: 2,
    name: '狗狗牵引绳',
    category: 'dog',
    price: 29.9,
    specification: '可伸缩3-5米',
    image: 'https://via.placeholder.com/200x200/87CEEB/FFFFFF?text=牵引绳'
  },
  {
    id: 3,
    name: '猫咪玩具球',
    category: 'cat',
    price: 15.8,
    specification: '直径8cm',
    image: 'https://via.placeholder.com/200x200/98FB98/FFFFFF?text=玩具球'
  },
  {
    id: 4,
    name: '狗狗磨牙棒',
    category: 'dog',
    price: 12.9,
    specification: '天然牛骨',
    image: 'https://via.placeholder.com/200x200/F0E68C/FFFFFF?text=磨牙棒'
  },
  {
    id: 5,
    name: '宠物洗护套装',
    category: 'health',
    price: 59.9,
    specification: '洗发+护毛+梳子',
    image: 'https://via.placeholder.com/200x200/DDA0DD/FFFFFF?text=洗护套装'
  },
  {
    id: 6,
    name: '幼犬专用粮',
    category: 'dog',
    price: 79.9,
    specification: '2kg/袋',
    image: 'https://via.placeholder.com/200x200/FFE4B5/FFFFFF?text=幼犬粮'
  },
  {
    id: 7,
    name: '猫咪爬架',
    category: 'cat',
    price: 199.9,
    specification: '三层设计',
    image: 'https://via.placeholder.com/200x200/E6E6FA/FFFFFF?text=猫爬架'
  },
  {
    id: 8,
    name: '宠物维生素',
    category: 'health',
    price: 39.9,
    specification: '60片/瓶',
    image: 'https://via.placeholder.com/200x200/FFA07A/FFFFFF?text=维生素'
  }
])

const searchKeyword = ref('')
const selectedCategory = ref('')

const getCategoryText = (category) => {
  const categories = {
    cat: '猫咪用品',
    dog: '狗狗用品',
    food: '宠物食品',
    health: '健康护理'
  }
  return categories[category] || '其他'
}

const filteredProducts = computed(() => {
  return products.value.filter(product => {
    const matchesSearch = product.name.toLowerCase().includes(searchKeyword.value.toLowerCase())
    const matchesCategory = !selectedCategory.value || product.category === selectedCategory.value
    return matchesSearch && matchesCategory
  })
})
</script>

<style scoped>
.product-list-container {
  width: 100%;
}

.section-title {
  text-align: center;
  margin-bottom: 2rem;
  color: #333;
  font-size: 1.8rem;
}

.search-filter {
  display: flex;
  gap: 1rem;
  margin-bottom: 2rem;
  justify-content: center;
  flex-wrap: wrap;
}

.search-input {
  padding: 0.5rem 1rem;
  border: 2px solid #ddd;
  border-radius: 4px;
  font-size: 1rem;
  min-width: 200px;
}

.category-select {
  padding: 0.5rem 1rem;
  border: 2px solid #ddd;
  border-radius: 4px;
  font-size: 1rem;
  background-color: white;
}

.product-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 2rem;
  margin-bottom: 2rem;
}

.product-card {
  background: white;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 4px 6px rgba(0,0,0,0.1);
  transition: transform 0.3s, box-shadow 0.3s;
}

.product-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 15px rgba(0,0,0,0.2);
}

.product-image {
  width: 100%;
  height: 200px;
  object-fit: cover;
}

.product-info {
  padding: 1rem;
}

.product-name {
  font-size: 1.2rem;
  margin-bottom: 0.5rem;
  color: #333;
}

.product-category {
  color: #666;
  font-size: 0.9rem;
  margin-bottom: 0.5rem;
}

.product-price {
  font-size: 1.3rem;
  color: #e74c3c;
  font-weight: bold;
  margin-bottom: 0.5rem;
}

.product-spec {
  color: #999;
  font-size: 0.9rem;
  margin-bottom: 1rem;
}

.buy-button {
  width: 100%;
  padding: 0.75rem;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 1rem;
  transition: opacity 0.3s;
}

.buy-button:hover {
  opacity: 0.9;
}

.no-results {
  text-align: center;
  padding: 3rem;
  color: #999;
  font-size: 1.1rem;
}

@media (max-width: 768px) {
  .search-filter {
    flex-direction: column;
    align-items: center;
  }
  
  .search-input,
  .category-select {
    min-width: 100%;
    max-width: 300px;
  }
  
  .product-grid {
    grid-template-columns: 1fr;
    gap: 1rem;
  }
}
</style>