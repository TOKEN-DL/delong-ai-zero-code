<script setup lang="ts">
import BasicLayout from '@/layouts/BasicLayout.vue'


import {useLoginUserStore} from "@/stores/loginUser.ts";

const loginUserStore = useLoginUserStore();

loginUserStore.fetchLoginUser()



</script>

<template>
  <BasicLayout />
</template>



<style>
/* 全局样式重置 */
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

html,
body {
  height: 100%;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial,
    'Noto Sans', sans-serif, 'Apple Color Emoji', 'Segoe UI Emoji', 'Segoe UI Symbol',
    'Noto Color Emoji';
}

#app {
  height: 100%;
}
</style>
