<template>
  <a-layout-header class="header">


    <a-row :wrap="false">
      <a-col flex="200px">
        <RouterLink to="/">
          <div class="left-section">
            <img src="@/assets/logo.png" alt="Logo" class="logo" />
            <h1 class="site-title">{{ siteTitle }}</h1>
          </div>
        </RouterLink>
      </a-col>

      <a-col flex="auto">
        <a-menu
            v-model:selectedKeys="selectedKeys"
            mode="horizontal"
            :items="menuItems"
            @select="handleMenuClick"
        >
        </a-menu>
      </a-col>


      <a-col>
        <div class="right-section">
          <div v-if="loginUserStore.loginUser.id">
            <a-dropdown>
              <a-space>
                <a-avatar :src="loginUserStore.loginUser.userAvatar"/>
                {{loginUserStore.loginUser.userName ?? '无名'}}
              </a-space>
              <template #overlay>
                <a-menu>
                  <a-menu-item @click="doLogout">
                    <logout-outlined/>
                  </a-menu-item>
                </a-menu>

              </template>
            </a-dropdown>

          </div>
          <div v-else>
            <a-button type="primary" href="/user/login">
              登录
            </a-button>

          </div>


        </div>
      </a-col>
    </a-row>

  </a-layout-header>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import {type MenuProps, message} from 'ant-design-vue'
import {useLoginUserStore} from "@/stores/loginUser.ts";
import {LogoutOutlined} from '@ant-design/icons-vue'
import {userLogout} from "@/api/userController.ts";


const loginUserStore = useLoginUserStore()

loginUserStore.fetchLoginUser()


const router = useRouter()

const siteTitle = ref('零代码平台')

const selectedKeys = ref<string[]>(['/'])
//监听路由变化
router.afterEach((to, from, next) => {
  selectedKeys.value = [to.path]
})


const originItems = [
  {
    key: '/',
    label: '首页',
    title: '首页',
  },
  {
    key: '/user/register',
    label: '注册',
    title: '注册'
  },
  {
    key: '/admin/userManage',
    label: '用户管理',
    title: '用户管理',
  },
  {
    key: '/admin/AppManage',
    label: '应用管理',
    title: '应用管理',
  },
  {
    key: '/test',
    label: '测试页面',
    title: '测试页面',
  },


]


//过滤菜单项
const filterMenus = (menus = [] as MenuProps['items']) => {
  return menus?.filter((menu) => {
    const menuKey = menu?.key as string
    if(menuKey?.startsWith('/admin')) {
      const loginUser = loginUserStore.loginUser
      if (!loginUser || loginUser.userRole !== 'admin') {
        return false
      }
    }
      return true

  })
}

const menuItems = computed<MenuProps['items']>( () => filterMenus(originItems))


// 处理菜单点击
const handleMenuClick: MenuProps['onClick'] = (e) => {
  const key = e.key as string
  selectedKeys.value = [key]
  // 跳转到对应页面
  if (key.startsWith('/')) {
    router.push(key)
  }

}


// 退出登录

const doLogout = async () => {
  const res = await userLogout()
  if (res.data.code === 0) {
    loginUserStore.setLoginUser({
      userName: '未登录',
    })
    message.success('退出登录成功')
    await router.push('/user/login')
  }else {
    message.error('退出登录失败: ' + res.data.message)
  }
}



</script>

<style scoped>
.header {
  background: #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  padding: 0;
  height: 64px;
  line-height: 64px;
  position: sticky;
  top: 0;
  z-index: 1000;
}

.header-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  max-width: 1400px;
  margin: 0 auto;
  padding: 0 24px;
  height: 100%;
}

.left-section {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 200px;
}

.logo {
  width: 32px;
  height: 32px;
  object-fit: contain;
}

.site-title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #1890ff;
  white-space: nowrap;
}

.nav-menu {
  flex: 1;
  display: flex;
  justify-content: center;
  border-bottom: none;
  background: transparent;
}

.nav-menu :deep(.ant-menu-item) {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0 20px;
}

.right-section {
  display: flex;
  align-items: center;
  gap: 16px;
  min-width: 200px;
  justify-content: flex-end;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .header-content {
    padding: 0 16px;
  }

  .left-section {
    min-width: auto;
  }

  .site-title {
    display: none;
  }

  .nav-menu {
    display: none;
  }

  .right-section {
    min-width: auto;
  }
}

@media (max-width: 576px) {
  .logo {
    width: 28px;
    height: 28px;
  }
}
</style>
