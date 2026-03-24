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
const siteTitle = ref('AI 应用生成平台')
const siteDescription = ref('一句话轻松创建网站应用')

// 用户提示词输入
const userInput = ref('')
const creating = ref(false)

// 快捷提示词示例
const promptExamples = ref([
  {
    title: '个人博客',
    prompt: '帮我创建一个个人博客网站，包含首页展示最新文章列表、文章详情页、关于我页面、分类归档页面。首页需要有轮播图展示精选文章，文章列表支持分页加载，右侧侧边栏显示热门文章和标签云。整体风格简约清新，使用暖色调配色，支持响应式布局，在手机端也能良好展示。',
  },
  {
    title: '企业官网',
    prompt: '帮我创建一个科技公司企业官网，包含首页、产品介绍、解决方案、关于我们、联系我们等页面。首页需要有动态的hero区域展示公司核心价值，产品介绍页需要卡片式布局展示产品特点，支持PC和移动端自适应。整体风格现代科技感，使用蓝色渐变为主色调，配合流畅的动画效果。',
  },
  {
    title: '在线简历',
    prompt: '帮我创建一个在线简历网站，包含个人简介、工作经历、项目展示、技能标签、教育背景、联系方式等模块。使用时间轴展示工作经历，卡片式展示项目作品，技能用进度条或标签云展示。整体风格专业简洁，深色主题，支持打印为PDF格式。',
  },
  {
    title: '作品集',
    prompt: '帮我创建一个设计师作品集网站，包含首页瀑布流作品展示、作品详情弹窗、关于设计师、联系方式等页面。首页需要大面积展示设计作品，支持分类筛选（如UI设计、品牌设计、插画等），作品hover时显示项目信息，点击打开详情弹窗查看大图和项目介绍。整体风格简约高级，黑白灰为主色调。',
  },
])

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
          placeholder="帮我创建个人博客网站"
          :auto-size="{ minRows: 2, maxRows: 4 }"
          :maxlength="500"
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

      <!-- 快捷提示词示例 -->
      <div class="prompt-examples">
        <div
          v-for="example in promptExamples"
          :key="example.title"
          class="prompt-example-card"
          @click="userInput = example.prompt"
        >
          <div class="example-title">{{ example.title }}</div>
          <div class="example-desc">{{ example.prompt.slice(0, 50) }}...</div>
        </div>
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
  background: linear-gradient(180deg, #e0f2fe 0%, #f0f9ff 30%, #ffffff 100%);
  padding-bottom: 40px;
}

/* 顶部英雄区域 - 清凉海滩风格 */
.hero-section {
  width: 100%;
  min-height: 50vh;
  padding: 80px 20px 60px;
  text-align: center;
  background:
    radial-gradient(ellipse at 30% 0%, rgba(14, 165, 233, 0.1) 0%, transparent 50%),
    radial-gradient(ellipse at 70% 30%, rgba(6, 182, 212, 0.08) 0%, transparent 40%),
    radial-gradient(ellipse at 50% 100%, rgba(56, 189, 248, 0.1) 0%, transparent 50%);
  position: relative;
}

.hero-section::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-image:
    linear-gradient(rgba(14, 165, 233, 0.03) 1px, transparent 1px),
    linear-gradient(90deg, rgba(14, 165, 233, 0.03) 1px, transparent 1px);
  background-size: 40px 40px;
  pointer-events: none;
}

.site-title {
  font-size: 48px;
  font-weight: 700;
  background: linear-gradient(135deg, #0284c7 0%, #06b6d4 50%, #0ea5e9 100%);
  background-size: 200% 200%;
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  margin-bottom: 16px;
  animation: gradientShift 4s ease infinite;
}

@keyframes gradientShift {
  0%, 100% { background-position: 0% 50%; }
  50% { background-position: 100% 50%; }
}

.site-description {
  font-size: 18px;
  color: #0369a1;
  margin-bottom: 40px;
}

/* 输入区域 */
.input-section {
  max-width: 700px;
  margin: 0 auto 30px;
  display: flex;
  gap: 12px;
  align-items: flex-end;
}

.prompt-input {
  flex: 1;
  border-radius: 12px;
  background: #ffffff;
  border: 1px solid #bae6fd;
  box-shadow: 0 2px 8px rgba(14, 165, 233, 0.08);
}

.prompt-input :deep(textarea) {
  font-size: 15px;
  color: #0c4a6e;
  background: transparent !important;
}

.prompt-input :deep(textarea::placeholder) {
  color: #7dd3fc;
}

.prompt-input:focus,
.prompt-input:hover {
  border-color: #38bdf8;
  box-shadow: 0 0 0 3px rgba(56, 189, 248, 0.15);
}

.create-btn {
  height: 48px;
  padding: 0 32px;
  font-size: 15px;
  background: linear-gradient(135deg, #0ea5e9 0%, #06b6d4 100%);
  border: none;
  border-radius: 12px;
  flex-shrink: 0;
}

.create-btn:hover {
  background: linear-gradient(135deg, #0284c7 0%, #0891b2 100%);
  transform: translateY(-2px);
  box-shadow: 0 4px 20px rgba(14, 165, 233, 0.35);
}

.create-btn:disabled {
  background: #e0f2fe;
  transform: none;
  box-shadow: none;
}

/* 快捷提示词示例 */
.prompt-examples {
  max-width: 900px;
  margin: 0 auto;
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.prompt-example-card {
  background: rgba(255, 255, 255, 0.8);
  border: 1px solid #e0f2fe;
  border-radius: 12px;
  padding: 16px;
  cursor: pointer;
  transition: all 0.3s ease;
  text-align: left;
  backdrop-filter: blur(4px);
}

.prompt-example-card:hover {
  background: #ffffff;
  border-color: #7dd3fc;
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(14, 165, 233, 0.15);
}

.example-title {
  font-size: 14px;
  font-weight: 600;
  color: #0369a1;
  margin-bottom: 6px;
}

.example-desc {
  font-size: 12px;
  color: #0ea5e9;
  line-height: 1.4;
  opacity: 0.8;
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
  color: #0c4a6e;
  margin: 0;
  display: flex;
  align-items: center;
  gap: 12px;
}

.section-icon {
  font-size: 24px;
  color: #0ea5e9;
}

.section-icon.featured {
  color: #f59e0b;
}

/* 应用网格 */
.apps-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 20px;
}

/* 应用卡片 - 清凉白色主题 */
.app-card {
  background: #ffffff;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 12px rgba(14, 165, 233, 0.08);
  cursor: pointer;
  transition: all 0.3s ease;
  border: 1px solid #e0f2fe;
}

.app-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 32px rgba(14, 165, 233, 0.18);
  border-color: #7dd3fc;
}

.app-card.featured {
  border-color: #fcd34d;
}

.app-card.featured:hover {
  border-color: #fbbf24;
}

/* 应用封面 */
.app-cover {
  width: 100%;
  height: 160px;
  position: relative;
  overflow: hidden;
  background: linear-gradient(135deg, #e0f2fe 0%, #f0f9ff 100%);
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
  color: #7dd3fc;
}

.featured-badge {
  position: absolute;
  top: 10px;
  right: 10px;
  background: linear-gradient(135deg, #fbbf24 0%, #f59e0b 100%);
  color: #fff;
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
  box-shadow: 0 2px 8px rgba(251, 191, 36, 0.4);
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
  border: 2px solid #e0f2fe;
}

.app-header-info {
  flex: 1;
  min-width: 0;
}

.app-name {
  font-size: 15px;
  font-weight: 600;
  color: #0c4a6e;
  margin: 0 0 2px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.app-author {
  font-size: 12px;
  color: #0ea5e9;
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.app-actions {
  padding: 12px 16px;
  border-top: 1px solid #f0f9ff;
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.app-actions :deep(.ant-btn-link) {
  padding: 0 8px;
  height: auto;
  font-size: 13px;
  color: #0ea5e9;
}

.app-actions :deep(.ant-btn-link:hover) {
  color: #0369a1;
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
  color: #0369a1;
}

.login-tip a {
  color: #0ea5e9;
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

  .prompt-examples {
    grid-template-columns: 1fr;
  }
}
</style>
