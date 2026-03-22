<script setup lang="ts">
import { reactive, ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { addApp, listAppByPage, listFeaturedAppByPage } from '@/api/appController.ts'
import { useLoginUserStore } from '@/stores/loginUser.ts'
import { SearchOutlined, AppstoreOutlined } from '@ant-design/icons-vue'

const router = useRouter()
const loginUserStore = useLoginUserStore()

// 网站标题
const siteTitle = ref('德龙AI 零代码平台')
const siteDescription = ref('不写一行代码，生成完整应用')

// 提示词输入
const promptInput = ref('')

// 我的应用搜索条件
const myAppSearchParams = reactive<API.AppQueryRequest>({
  pageNum: 1,
  pageSize: 20,
  appName: ''
})

// 精选应用搜索条件
const featuredAppSearchParams = reactive<API.AppQueryRequest>({
  pageNum: 1,
  pageSize: 20,
  appName: ''
})

// 我的应用数据
const myApps = ref<API.AppVO[]>([])
const myAppsTotal = ref(0)

// 精选应用数据
const featuredApps = ref<API.AppVO[]>([])
const featuredAppsTotal = ref(0)

// 获取我的应用列表
const fetchMyApps = async () => {
  try {
    const res = await listAppByPage(myAppSearchParams)
    if (res.data.code === 0 && res.data.data) {
      myApps.value = res.data.data.records ?? []
      myAppsTotal.value = res.data.data.totalRow ?? 0
    } else {
      message.error('获取我的应用失败: ' + res.data.message)
    }
  } catch (error) {
    message.error('获取我的应用失败')
  }
}

// 获取精选应用列表
const fetchFeaturedApps = async () => {
  try {
    const res = await listFeaturedAppByPage(featuredAppSearchParams)
    if (res.data.code === 0 && res.data.data) {
      featuredApps.value = res.data.data.records ?? []
      featuredAppsTotal.value = res.data.data.totalRow ?? 0
    } else {
      message.error('获取精选应用失败: ' + res.data.message)
    }
  } catch (error) {
    message.error('获取精选应用失败')
  }
}

// 我的应用分页
const myAppsPagination = computed(() => ({
  current: myAppSearchParams.pageNum,
  pageSize: myAppSearchParams.pageSize,
  total: myAppsTotal.value,
  showSizeChanger: true,
  showTotal: (total: number) => `共${total}条`,
  onChange: (page: number, pageSize: number) => {
    myAppSearchParams.pageNum = page
    myAppSearchParams.pageSize = pageSize
    fetchMyApps()
  }
}))

// 精选应用分页
const featuredAppsPagination = computed(() => ({
  current: featuredAppSearchParams.pageNum,
  pageSize: featuredAppSearchParams.pageSize,
  total: featuredAppsTotal.value,
  showSizeChanger: true,
  showTotal: (total: number) => `共${total}条`,
  onChange: (page: number, pageSize: number) => {
    featuredAppSearchParams.pageNum = page
    featuredAppSearchParams.pageSize = pageSize
    fetchFeaturedApps()
  }
}))

// 创建应用
const handleCreateApp = async () => {
  if (!promptInput.value.trim()) {
    message.warning('请输入提示词')
    return
  }

  if (!loginUserStore.loginUser.id) {
    message.warning('请先登录')
    router.push('/user/login')
    return
  }

  try {
    const res = await addApp({
      appName: promptInput.value.substring(0, 50),
      initPrompt: promptInput.value,
      codeGenType: 'web'
    })

    if (res.data.code === 0 && res.data.data) {
      message.success('应用创建成功')
      // 跳转到对话页面
      router.push({
        path: '/app/chat',
        query: {
          appId: res.data.data.toString()
        }
      })
    } else {
      message.error('创建应用失败: ' + res.data.message)
    }
  } catch (error) {
    message.error('创建应用失败')
  }
}

// 搜索我的应用
const handleSearchMyApps = () => {
  myAppSearchParams.pageNum = 1
  fetchMyApps()
}

// 搜索精选应用
const handleSearchFeaturedApps = () => {
  featuredAppSearchParams.pageNum = 1
  fetchFeaturedApps()
}

// 点击应用卡片
const handleAppClick = (app: API.AppVO) => {
  // 跳转到对话页面
  router.push({
    path: '/app/chat',
    query: {
      appId: app.id?.toString()
    }
  })
}

// 页面加载时获取数据
onMounted(() => {
  fetchMyApps()
  fetchFeaturedApps()
})
</script>

<template>
  <div class="home-page">
    <!-- 网站标题和介绍 -->
    <div class="hero-section">
      <h1 class="site-title">{{ siteTitle }}</h1>
      <p class="site-description">{{ siteDescription }}</p>
    </div>

    <!-- 提示词输入框 -->
    <div class="prompt-section">
      <a-input
        v-model:value="promptInput"
        placeholder="描述你想创建的应用，例如：创建一个在线商城网站，包含商品展示、购物车和订单功能"
        size="large"
        class="prompt-input"
        @pressEnter="handleCreateApp"
      >
        <template #suffix>
          <a-button type="primary" size="large" @click="handleCreateApp">
            生成应用
          </a-button>
        </template>
      </a-input>
    </div>

    <!-- 我的应用区域 -->
    <div class="apps-section">
      <div class="section-header">
        <h2>我的应用</h2>
        <div class="search-box">
          <a-input
            v-model:value="myAppSearchParams.appName"
            placeholder="搜索应用名称"
            style="width: 200px"
            @pressEnter="handleSearchMyApps"
          >
            <template #suffix>
              <SearchOutlined @click="handleSearchMyApps" style="cursor: pointer" />
            </template>
          </a-input>
        </div>
      </div>

      <div class="apps-grid">
        <div
          v-for="app in myApps"
          :key="app.id"
          class="app-card"
          @click="handleAppClick(app)"
        >
          <div class="app-cover">
            <img v-if="app.cover" :src="app.cover" :alt="app.appName" />
            <div v-else class="default-cover">
              <AppstoreOutlined style="font-size: 48px" />
            </div>
          </div>
          <div class="app-info">
            <h3 class="app-name">{{ app.appName }}</h3>
            <p class="app-prompt">{{ app.initPrompt }}</p>
            <div class="app-meta">
              <span class="create-time">
                {{ app.createTime }}
              </span>
            </div>
          </div>
        </div>
      </div>

      <div class="pagination-wrapper">
        <a-pagination v-bind="myAppsPagination" />
      </div>
    </div>

    <!-- 精选应用区域 -->
    <div class="apps-section">
      <div class="section-header">
        <h2>精选应用</h2>
        <div class="search-box">
          <a-input
            v-model:value="featuredAppSearchParams.appName"
            placeholder="搜索应用名称"
            style="width: 200px"
            @pressEnter="handleSearchFeaturedApps"
          >
            <template #suffix>
              <search-outlined @click="handleSearchFeaturedApps" style="cursor: pointer" />
            </template>
          </a-input>
        </div>
      </div>

      <div class="apps-grid">
        <div
          v-for="app in featuredApps"
          :key="app.id"
          class="app-card"
          @click="handleAppClick(app)"
        >
          <div class="app-cover">
            <img v-if="app.cover" :src="app.cover" :alt="app.appName" />
            <div v-else class="default-cover">
              <AppstoreOutlined style="font-size: 48px" />
            </div>
          </div>
          <div class="app-info">
            <h3 class="app-name">{{ app.appName }}</h3>
            <p class="app-prompt">{{ app.initPrompt }}</p>
            <div class="app-meta">
              <span class="create-time">
                {{ app.createTime }}
              </span>
            </div>
          </div>
        </div>
      </div>

      <div class="pagination-wrapper">
        <a-pagination v-bind="featuredAppsPagination" />
      </div>
    </div>
  </div>
</template>

<style scoped>
.home-page {
  max-width: 1400px;
  margin: 0 auto;
  padding: 24px;
}

.hero-section {
  text-align: center;
  padding: 40px 0;
}

.site-title {
  font-size: 42px;
  font-weight: 700;
  color: #1890ff;
  margin-bottom: 16px;
}

.site-description {
  font-size: 18px;
  color: #666;
  margin-bottom: 24px;
}

.prompt-section {
  margin: 40px 0;
  padding: 20px;
  background: #f5f5f5;
  border-radius: 8px;
}

.prompt-input {
  max-width: 800px;
  margin: 0 auto;
  display: block;
}

.apps-section {
  margin: 40px 0;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.section-header h2 {
  font-size: 24px;
  font-weight: 600;
  margin: 0;
}

.search-box {
  display: flex;
  gap: 12px;
}

.apps-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 20px;
}

.app-card {
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s;
  background: #fff;
}

.app-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  transform: translateY(-4px);
}

.app-cover {
  width: 100%;
  height: 160px;
  overflow: hidden;
  background: #f5f5f5;
  display: flex;
  align-items: center;
  justify-content: center;
}

.app-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.default-cover {
  color: #999;
}

.app-info {
  padding: 16px;
}

.app-name {
  font-size: 18px;
  font-weight: 600;
  margin: 0 0 8px 0;
  color: #333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.app-prompt {
  font-size: 14px;
  color: #666;
  margin: 0 0 12px 0;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  min-height: 40px;
}

.app-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
  color: #999;
}

.create-time {
  color: #999;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}

@media (max-width: 768px) {
  .home-page {
    padding: 16px;
  }

  .site-title {
    font-size: 32px;
  }

  .site-description {
    font-size: 16px;
  }

  .apps-grid {
    grid-template-columns: 1fr;
  }

  .section-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 16px;
  }

  .search-box {
    width: 100%;
  }

  .search-box :deep(.ant-input) {
    width: 100% !important;
  }
}
</style>