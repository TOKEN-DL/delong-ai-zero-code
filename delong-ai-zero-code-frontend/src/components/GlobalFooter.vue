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
  background: var(--bg-surface);
  color: var(--color-text-muted);
  text-align: center;
  padding: 16px 50px;
  position: sticky;
  bottom: 0;
  z-index: 999;
  border-top: 1px solid var(--color-border-light);
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
  color: var(--color-text-muted);
}

.author-link {
  color: var(--color-primary);
  text-decoration: none;
  transition: color 0.2s;
}

.author-link:hover {
  text-decoration: underline;
}

@media (max-width: 576px) {
  .footer {
    padding: 12px 24px;
  }

  .footer-content {
    font-size: 12px;
  }
}
</style>
