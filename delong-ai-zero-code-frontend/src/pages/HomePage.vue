<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

// 网站标题
const siteTitle = ref('零代码平台')
const siteDescription = ref('不写一行代码，生成完整应用')
</script>

<template>
  <div class="home-page">
    <!-- 网站标题和介绍 -->
    <div class="hero-section">
      <h1 class="site-title">{{ siteTitle }}</h1>
      <p class="site-description">{{ siteDescription }}</p>
    </div>
  </div>
</template>

<style scoped>
.home-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 20px;
}

.hero-section {
  background: #fff;
  border-radius: 12px;
  padding: 60px 40px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
  text-align: center;
  max-width: 600px;
}

.site-title {
  font-size: 36px;
  font-weight: 700;
  color: #333;
  margin-bottom: 16px;
}

.site-description {
  font-size: 18px;
  color: #666;
  margin-bottom: 24px;
}

@media (max-width: 768px) {
  .hero-section {
    padding: 40px 24px;
  }

  .site-title {
    font-size: 28px;
  }

  .site-description {
    font-size: 16px;
  }
}
</style>