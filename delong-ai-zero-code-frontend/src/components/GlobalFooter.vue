<template>
  <a-layout-footer class="footer">
    <div class="footer-content">
      <span class="copyright">零代码生成项目 by </span>
      <a
        href="https://github.com/TOKEN-DL"
        target="_blank"
        rel="noopener noreferrer"
        class="author-link"
      >
        时雨夏树
      </a>
    </div>
  </a-layout-footer>
</template>

<script setup lang="ts">
</script>

<style scoped>
.footer {
  background: #001529;
  color: rgba(255, 255, 255, 0.85);
  text-align: center;
  padding: 16px 50px;
  position: sticky;
  bottom: 0;
  z-index: 999;
}

.footer-content {
  max-width: 1400px;
  margin: 0 auto;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  font-size: 14px;
}

.copyright {
  color: rgba(255, 255, 255, 0.85);
}

.author-link {
  color: #1890ff;
  text-decoration: none;
  transition: color 0.3s;
}

.author-link:hover {
  color: #40a9ff;
  text-decoration: underline;
}

/* 响应式设计 */
@media (max-width: 576px) {
  .footer {
    padding: 12px 24px;
  }

  .footer-content {
    font-size: 12px;
  }
}
</style>
