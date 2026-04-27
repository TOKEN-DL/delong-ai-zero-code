<script setup lang="ts">
import BasicLayout from '@/layouts/BasicLayout.vue'
import { useLoginUserStore } from '@/stores/loginUser.ts'
import { useThemeStore } from '@/stores/themeStore'
import { theme } from 'ant-design-vue'
import { computed } from 'vue'

const loginUserStore = useLoginUserStore()
loginUserStore.fetchLoginUser()

const themeStore = useThemeStore()

const antTheme = computed(() => ({
  algorithm: themeStore.isDark ? theme.darkAlgorithm : theme.defaultAlgorithm,
  token: {
    colorPrimary: themeStore.isDark ? '#2997ff' : '#0071e3',
    borderRadius: 8,
    fontFamily: "-apple-system, BlinkMacSystemFont, 'SF Pro Display', 'SF Pro Text', 'Helvetica Neue', 'Segoe UI', Roboto, Arial, sans-serif",
  },
}))
</script>

<template>
  <a-config-provider :theme="antTheme">
    <BasicLayout />
  </a-config-provider>
</template>

<style>
/* Global style resets */
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

html,
body {
  height: 100%;
  font-family: -apple-system, BlinkMacSystemFont, 'SF Pro Display', 'SF Pro Text', 'Helvetica Neue',
    'Segoe UI', Roboto, Arial, sans-serif;
}

#app {
  height: 100%;
}
</style>
