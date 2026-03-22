import { createRouter, createWebHistory } from 'vue-router'
import HomePage from '../pages/HomePage.vue'
import UserLoginPage from "@/pages/user/UserLoginPage.vue";
import UserRegisterPage from "@/pages/user/UserRegisterPage.vue";
import UserManagePage from "@/pages/admin/UserManagePage.vue";
import AppChatPage from "@/pages/app/AppChatPage.vue";
import AppManagePage from "@/pages/admin/AppManagePage.vue";
import AppUpdatePage from "@/pages/app/AppUpdatePage.vue";
import TestPage from "@/pages/TestPage.vue";

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: '主页',
      component: HomePage,
    },
    {
      path: '/user/login',
      name: '用户登录',
      component: UserLoginPage,
    },
    {
      path: '/user/register',
      name: '用户注册',
      component: UserRegisterPage,
    },
    {
      path: '/admin/UserManage',
      name: '用户管理',
      component: UserManagePage,
    },
    {
      path: '/app/chat',
      name: '应用生成对话页',
      component: AppChatPage,
    },
    {
      path: '/admin/AppManage',
      name: '应用管理',
      component: AppManagePage,
    },
    {
      path: '/app/update',
      name: '应用信息修改页',
      component: AppUpdatePage,
    },
    {
      path: '/test',
      name: '测试页面',
      component: TestPage,
    },

  ],
})

export default router
