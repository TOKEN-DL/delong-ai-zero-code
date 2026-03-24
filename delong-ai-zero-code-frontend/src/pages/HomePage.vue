<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import { addApp, listAppByPage, listFeaturedAppByPage, deleteApp } from '@/api/appController'
import { useLoginUserStore } from '@/stores/loginUser'
import { getDeployedAppUrl } from '@/config'
import { PlusOutlined, AppstoreOutlined, DeleteOutlined, EditOutlined, MessageOutlined, EyeOutlined, UserOutlined } from '@ant-design/icons-vue'
import dayjs from 'dayjs'

const router = useRouter()
const loginUserStore = useLoginUserStore()

// 网站标题
const siteTitle = ref('零代码平台')
const siteDescription = ref('不写一行代码，生成完整应用')

// 用户提示词输入
const userInput = ref('')
const creating = ref(false)

// 我的应用列表
const myApps = ref<API.AppVO[]>([])
const myAppsLoading = ref(false)
const myAppsTotal = ref(0)
const myAppsSearchParams = ref<API.AppQueryRequest>({
  pageNum: 1,
  pageSize: 20,
  appName: '',
})

// 精选应用列表
const featuredApps = ref<API.AppVO[]>([])
const featuredAppsLoading = ref(false)
const featuredAppsTotal = ref(0)
const featuredAppsSearchParams = ref<API.AppQueryRequest>({
  pageNum: 1,
  pageSize: 20,
  appName: '',
})

// 是否已登录
const isLoggedIn = computed(() => !!loginUserStore.loginUser.id)

// 创建应用
const handleCreateApp = async () => {
  if (!isLoggedIn.value) {
    message.warning('请先登录')
    router.push('/user/login')
    return
  }

  if (!userInput.value.trim()) {
    message.warning('请输入应用描述')
    return
  }

  creating.value = true
  try {
    const res = await addApp({
      initPrompt: userInput.value.trim(),
    })
    if (res.data.code === 0 && res.data.data) {
      message.success('创建成功，正在跳转...')
      // 跳转到对话页面
      router.push(`/app/chat/${res.data.data}`)
    } else {
      message.error(res.data.message || '创建失败')
    }
  } catch (error) {
    message.error('创建失败')
  } finally {
    creating.value = false
  }
}

// 获取我的应用列表
const fetchMyApps = async () => {
  if (!isLoggedIn.value) {
    myApps.value = []
    return
  }

  myAppsLoading.value = true
  try {
    const res = await listAppByPage({
      ...myAppsSearchParams.value,
    })
    if (res.data.code === 0 && res.data.data) {
      myApps.value = res.data.data.records ?? []
      myAppsTotal.value = res.data.data.totalRow ?? 0
    } else {
      message.error('获取我的应用失败: ' + res.data.message)
    }
  } catch (error) {
    message.error('获取我的应用失败')
  } finally {
    myAppsLoading.value = false
  }
}

// 获取精选应用列表
const fetchFeaturedApps = async () => {
  featuredAppsLoading.value = true
  try {
    const res = await listFeaturedAppByPage({
      ...featuredAppsSearchParams.value,
    })
    if (res.data.code === 0 && res.data.data) {
      featuredApps.value = res.data.data.records ?? []
      featuredAppsTotal.value = res.data.data.totalRow ?? 0
    } else {
      message.error('获取精选应用失败: ' + res.data.message)
    }
  } catch (error) {
    message.error('获取精选应用失败')
  } finally {
    featuredAppsLoading.value = false
  }
}

// 搜索我的应用
const handleSearchMyApps = () => {
  myAppsSearchParams.value.pageNum = 1
  fetchMyApps()
}

// 搜索精选应用
const handleSearchFeaturedApps = () => {
  featuredAppsSearchParams.value.pageNum = 1
  fetchFeaturedApps()
}

// 我的应用分页变化
const handleMyAppsPageChange = (page: number, pageSize: number) => {
  myAppsSearchParams.value.pageNum = page
  myAppsSearchParams.value.pageSize = pageSize
  fetchMyApps()
}

// 精选应用分页变化
const handleFeaturedAppsPageChange = (page: number, pageSize: number) => {
  featuredAppsSearchParams.value.pageNum = page
  featuredAppsSearchParams.value.pageSize = pageSize
  fetchFeaturedApps()
}

// 进入应用对话页面
const enterApp = (app: API.AppVO, event?: Event) => {
  if (event) {
    event.stopPropagation()
  }
  if (app.id) {
    router.push(`/app/chat/${app.id}?view=1`)
  }
}

// 编辑应用
const editApp = (app: API.AppVO, event: Event) => {
  event.stopPropagation()
  if (app.id) {
    router.push(`/app/edit/${app.id}`)
  }
}

// 删除应用
const deleteMyApp = (app: API.AppVO, event: Event) => {
  event.stopPropagation()
  if (!app.id) return

  Modal.confirm({
    title: '确认删除',
    content: `确定要删除应用"${app.appName}"吗？删除后无法恢复。`,
    okText: '确定',
    cancelText: '取消',
    okType: 'danger',
    onOk: async () => {
      try {
        const res = await deleteApp({ id: app.id })
        if (res.data.code === 0) {
          message.success('删除成功')
          fetchMyApps()
        } else {
          message.error('删除失败: ' + res.data.message)
        }
      } catch (error) {
        message.error('删除失败')
      }
    },
  })
}

// 打开部署的网站（查看作品）
const openDeployedApp = (app: API.AppVO, event: Event) => {
  event.stopPropagation()
  if (app.deployKey) {
    window.open(getDeployedAppUrl(app.deployKey), '_blank')
  }
}

// 页面加载时获取数据
onMounted(() => {
  loginUserStore.fetchLoginUser().then(() => {
    fetchMyApps()
  })
  fetchFeaturedApps()
})
</script>

<template>
  <div class="home-page">
    <!-- 网站标题和介绍 -->
    <div class="hero-section">
      <h1 class="site-title">{{ siteTitle }}</h1>
      <p class="site-description">{{ siteDescription }}</p>

      <!-- 用户提示词输入框 -->
      <div class="input-section">
        <a-textarea
          v-model:value="userInput"
          :placeholder="'描述你想要的应用，例如：帮我生成一个个人博客网站，包含首页、文章列表、关于我等页面...'"
          :auto-size="{ minRows: 3, maxRows: 6 }"
          :maxlength="500"
          show-count
          class="prompt-input"
        />
        <a-button
          type="primary"
          size="large"
          :loading="creating"
          :disabled="!userInput.trim()"
          class="create-btn"
          @click="handleCreateApp"
        >
          <template #icon>
            <PlusOutlined />
          </template>
          创建应用
        </a-button>
      </div>
    </div>

    <!-- 我的应用分页列表 -->
    <div class="section my-apps-section" v-if="isLoggedIn">
      <div class="section-header">
        <h2 class="section-title">
          <AppstoreOutlined class="section-icon" />
          我的应用
        </h2>
        <div class="search-box">
          <a-input-search
            v-model:value="myAppsSearchParams.appName"
            placeholder="搜索我的应用"
            style="width: 200px"
            @search="handleSearchMyApps"
            allow-clear
          />
        </div>
      </div>

      <a-spin :spinning="myAppsLoading">
        <div class="apps-grid" v-if="myApps.length > 0">
          <div
            v-for="app in myApps"
            :key="app.id"
            class="app-card"
            @click="enterApp(app)"
          >
            <!-- 应用封面 -->
            <div class="app-cover">
              <img v-if="app.cover" :src="app.cover" alt="应用封面" />
              <div v-else class="app-cover-placeholder">
                <AppstoreOutlined class="cover-icon" />
              </div>
            </div>
            <!-- 应用信息 -->
            <div class="app-header">
              <a-avatar :size="40" :src="app.user?.userAvatar" class="app-avatar">
                <template #icon><UserOutlined /></template>
              </a-avatar>
              <div class="app-header-info">
                <h3 class="app-name">{{ app.appName || '未命名应用' }}</h3>
                <span class="app-author">{{ app.user?.userName || '匿名用户' }}</span>
              </div>
            </div>
            <!-- 操作按钮 -->
            <div class="app-actions">
              <a-button type="link" size="small" @click="(e: Event) => enterApp(app, e)">
                <MessageOutlined /> 查看对话
              </a-button>
              <a-button
                v-if="app.deployKey"
                type="link"
                size="small"
                @click="(e: Event) => openDeployedApp(app, e)"
              >
                <EyeOutlined /> 查看作品
              </a-button>
              <a-button type="link" size="small" @click="(e: Event) => editApp(app, e)">
                <EditOutlined /> 编辑
              </a-button>
              <a-button type="link" size="small" danger @click="(e: Event) => deleteMyApp(app, e)">
                <DeleteOutlined /> 删除
              </a-button>
            </div>
          </div>
        </div>
        <a-empty v-else description="暂无应用，快来创建一个吧！" />
      </a-spin>

      <div class="pagination-wrapper" v-if="myAppsTotal > (myAppsSearchParams.pageSize || 20)">
        <a-pagination
          v-model:current="myAppsSearchParams.pageNum"
          v-model:pageSize="myAppsSearchParams.pageSize"
          :total="myAppsTotal"
          :show-size-changer="true"
          :show-total="(total: number) => `共 ${total} 个应用`"
          @change="handleMyAppsPageChange"
        />
      </div>
    </div>

    <!-- 精选应用分页列表 -->
    <div class="section featured-apps-section">
      <div class="section-header">
        <h2 class="section-title">
          <AppstoreOutlined class="section-icon featured" />
          精选应用
        </h2>
        <div class="search-box">
          <a-input-search
            v-model:value="featuredAppsSearchParams.appName"
            placeholder="搜索精选应用"
            style="width: 200px"
            @search="handleSearchFeaturedApps"
            allow-clear
          />
        </div>
      </div>

      <a-spin :spinning="featuredAppsLoading">
        <div class="apps-grid" v-if="featuredApps.length > 0">
          <div
            v-for="app in featuredApps"
            :key="app.id"
            class="app-card featured"
            @click="enterApp(app)"
          >
            <!-- 应用封面 -->
            <div class="app-cover">
              <img v-if="app.cover" :src="app.cover" alt="应用封面" />
              <div v-else class="app-cover-placeholder">
                <AppstoreOutlined class="cover-icon" />
              </div>
              <span class="featured-badge">精选</span>
            </div>
            <!-- 应用信息 -->
            <div class="app-header">
              <a-avatar :size="40" :src="app.user?.userAvatar" class="app-avatar">
                <template #icon><UserOutlined /></template>
              </a-avatar>
              <div class="app-header-info">
                <h3 class="app-name">{{ app.appName || '未命名应用' }}</h3>
                <span class="app-author">{{ app.user?.userName || '匿名用户' }}</span>
              </div>
            </div>
            <div class="app-actions">
              <a-button type="link" size="small" @click="(e: Event) => enterApp(app, e)">
                <MessageOutlined /> 查看对话
              </a-button>
              <a-button
                v-if="app.deployKey"
                type="link"
                size="small"
                @click="(e: Event) => openDeployedApp(app, e)"
              >
                <EyeOutlined /> 查看作品
              </a-button>
            </div>
          </div>
        </div>
        <a-empty v-else description="暂无精选应用" />
      </a-spin>

      <div class="pagination-wrapper" v-if="featuredAppsTotal > (featuredAppsSearchParams.pageSize || 20)">
        <a-pagination
          v-model:current="featuredAppsSearchParams.pageNum"
          v-model:pageSize="featuredAppsSearchParams.pageSize"
          :total="featuredAppsTotal"
          :show-size-changer="true"
          :show-total="(total: number) => `共 ${total} 个应用`"
          @change="handleFeaturedAppsPageChange"
        />
      </div>
    </div>
  </div>
</template>

<style scoped>
.home-page {
  min-height: 100vh;
  background: #f5f5f5;
  padding-bottom: 40px;
}

/* 顶部英雄区域 */
.hero-section {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 60px 20px 40px;
  text-align: center;
}

.site-title {
  font-size: 42px;
  font-weight: 700;
  color: #fff;
  margin-bottom: 12px;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.site-description {
  font-size: 18px;
  color: rgba(255, 255, 255, 0.9);
  margin-bottom: 32px;
}

/* 输入区域 */
.input-section {
  max-width: 800px;
  margin: 0 auto;
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.15);
}

.prompt-input {
  border: 2px solid #667eea;
  border-radius: 8px;
}

.prompt-input:focus,
.prompt-input:hover {
  border-color: #764ba2;
  box-shadow: 0 0 0 2px rgba(102, 126, 234, 0.1);
}

.prompt-input :deep(textarea) {
  font-size: 15px;
}

.create-btn {
  margin-top: 16px;
  height: 44px;
  font-size: 16px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  border-radius: 8px;
}

.create-btn:hover {
  background: linear-gradient(135deg, #764ba2 0%, #667eea 100%);
}

.create-btn:disabled {
  background: #ccc;
}

/* 通用区块样式 */
.section {
  max-width: 1400px;
  margin: 0 auto;
  padding: 32px 20px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.section-title {
  font-size: 24px;
  font-weight: 600;
  color: #333;
  margin: 0;
  display: flex;
  align-items: center;
  gap: 12px;
}

.section-icon {
  font-size: 24px;
  color: #667eea;
}

.section-icon.featured {
  color: #faad14;
}

/* 应用网格 */
.apps-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 20px;
}

/* 应用卡片 */
.app-card {
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  cursor: pointer;
  transition: all 0.3s ease;
  border: 2px solid transparent;
}

.app-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
  border-color: #667eea;
}

.app-card.featured {
  border-color: #faad14;
}

.app-card.featured:hover {
  border-color: #faad14;
}

/* 应用封面 */
.app-cover {
  width: 100%;
  height: 160px;
  position: relative;
  overflow: hidden;
  background: linear-gradient(135deg, #f5f7fa 0%, #e4e8ec 100%);
}

.app-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s ease;
}

.app-card:hover .app-cover img {
  transform: scale(1.05);
}

.app-cover-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.app-cover-placeholder .cover-icon {
  font-size: 48px;
  color: #ccc;
}

.featured-badge {
  position: absolute;
  top: 10px;
  right: 10px;
  background: linear-gradient(135deg, #faad14 0%, #fa8c16 100%);
  color: #fff;
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
}

/* 卡片头部 - 左右结构 */
.app-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
}

.app-avatar {
  flex-shrink: 0;
  border: 2px solid #f0f0f0;
}

.app-header-info {
  flex: 1;
  min-width: 0;
}

.app-name {
  font-size: 15px;
  font-weight: 600;
  color: #333;
  margin: 0 0 2px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.app-author {
  font-size: 12px;
  color: #999;
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.app-actions {
  padding: 12px 16px;
  border-top: 1px solid #f0f0f0;
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.app-actions :deep(.ant-btn-link) {
  padding: 0 8px;
  height: auto;
  font-size: 13px;
}

/* 分页 */
.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}

/* 未登录提示 */
.login-tip {
  text-align: center;
  padding: 40px;
  color: #666;
}

.login-tip a {
  color: #667eea;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .hero-section {
    padding: 40px 16px 30px;
  }

  .site-title {
    font-size: 28px;
  }

  .site-description {
    font-size: 15px;
    margin-bottom: 24px;
  }

  .input-section {
    padding: 16px;
  }

  .section {
    padding: 24px 16px;
  }

  .section-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }

  .section-title {
    font-size: 20px;
  }

  .search-box {
    width: 100%;
  }

  .search-box :deep(.ant-input-search) {
    width: 100% !important;
  }

  .apps-grid {
    grid-template-columns: 1fr;
  }
}
</style>
