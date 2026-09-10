<template>
  <div class="app-container">
    <header class="header">
      <h1 class="title">🐱🐶 猫狗商城</h1>
      <nav class="nav">
        <router-link to="/products" class="nav-item">商品列表</router-link>
        <router-link to="/knowledge" class="nav-item">知识文章</router-link>
      </nav>
    </header>
    
    <main class="main-content">
      <router-view />
    </main>
    
    <footer class="footer">
      <p>&copy; 2023 猫狗商城 - 为您的爱宠提供优质服务</p>
    </footer>
  </div>
</template>

<script setup>
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: 'Arial', sans-serif;
  background-color: #f5f5f5;
  color: #333;
}

.app-container {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.header {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  padding: 1rem 2rem;
  box-shadow: 0 2px 10px rgba(0,0,0,0.1);
}

.title {
  text-align: center;
  margin-bottom: 1rem;
  font-size: 2rem;
}

.nav {
  display: flex;
  justify-content: center;
  gap: 2rem;
}

.nav-item {
  color: white;
  text-decoration: none;
  padding: 0.5rem 1rem;
  border-radius: 4px;
  transition: background-color 0.3s;
}

.nav-item.router-link-active,
.nav-item:hover {
  background-color: rgba(255,255,255,0.2);
}

.main-content {
  flex: 1;
  padding: 2rem;
  max-width: 1200px;
  margin: 0 auto;
  width: 100%;
}

.footer {
  background-color: #333;
  color: white;
  text-align: center;
  padding: 1rem;
  margin-top: auto;
}

@media (max-width: 768px) {
  .nav {
    flex-direction: column;
    align-items: center;
    gap: 0.5rem;
  }
  
  .title {
    font-size: 1.5rem;
  }
  
  .main-content {
    padding: 1rem;
  }
}
</style>