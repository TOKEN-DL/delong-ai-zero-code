<script setup lang="ts">
import { ref, onMounted, nextTick, onUnmounted, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import { getAppById, deployApp, deleteApp } from '@/api/appController'
import { useLoginUserStore } from '@/stores/loginUser'
import { getSSEUrl, getAppPreviewUrl } from '@/config'
import { SendOutlined, RocketOutlined, AppstoreOutlined, ArrowLeftOutlined, InfoCircleOutlined, UserOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons-vue'
import { marked } from 'marked'
import hljs from 'highlight.js'

// 代码语言别名映射
const langAliases: Record<string, string> = {
  js: 'javascript',
  ts: 'typescript',
  py: 'python',
  rb: 'ruby',
  sh: 'bash',
  shell: 'bash',
  yml: 'yaml',
  md: 'markdown',
  htm: 'html',
}

// 获取规范化的语言名称
const getNormalizedLang = (lang: string): string => {
  const normalized = langAliases[lang.toLowerCase()] || lang.toLowerCase()
  return normalized
}

// 自定义渲染器，添加代码高亮和语言标签
const renderer = new marked.Renderer()
renderer.code = function({ text, lang }: { text: string; lang?: string }) {
  const language = lang ? getNormalizedLang(lang) : ''
  let highlighted: string

  if (language && hljs.getLanguage(language)) {
    try {
      highlighted = hljs.highlight(text, { language }).value
    } catch (e) {
      highlighted = hljs.highlightAuto(text).value
    }
  } else {
    // 自动检测语言
    const result = hljs.highlightAuto(text, ['html', 'css', 'javascript', 'typescript', 'python', 'java', 'json', 'bash', 'sql'])
    highlighted = result.value
  }

  // 显示的语言标签
  const displayLang = language || 'code'

  // 生成唯一的代码块ID
  const codeId = `code-${Date.now()}-${Math.random().toString(36).slice(2, 11)}`

  return `
    <div class="code-block-wrapper">
      <div class="code-block-header">
        <span class="code-lang">${displayLang}</span>
        <button class="code-copy-btn" onclick="copyCode('${codeId}', this)" title="复制代码">
          <span class="copy-icon">📋</span>
          <span class="copy-text">复制</span>
        </button>
      </div>
      <pre class="code-block" id="${codeId}"><code class="hljs ${language}">${highlighted}</code></pre>
    </div>
  `
}

// 配置 marked
marked.setOptions({
  renderer,
  breaks: true,
  gfm: true,
})

// 渲染 Markdown
const renderMarkdown = (content: string): string => {
  if (!content) return ''
  try {
    return marked.parse(content) as string
  } catch (e) {
    return content
  }
}

const router = useRouter()
const route = useRoute()
const loginUserStore = useLoginUserStore()

const appId = ref<string>(route.params.appId as string)

// 应用信息
const appInfo = ref<API.AppVO | null>(null)

// 是否为只读查看模式（从卡片点击进入时带 view=1 参数）
const isViewMode = computed(() => route.query.view === '1')

// 是否为自己的作品
const isOwner = computed(() => {
  const loginUser = loginUserStore.loginUser
  return loginUser.id && appInfo.value?.userId === loginUser.id
})

// 是否为管理员
const isAdmin = computed(() => {
  return loginUserStore.loginUser.userRole === 'admin'
})

// 是否可以管理（本人或管理员）
const canManage = computed(() => isOwner.value || isAdmin.value)

// 输入框是否禁用
const isInputDisabled = computed(() => isViewMode.value || !isOwner.value)

// 对话消息
interface ChatMessage {
  role: 'user' | 'assistant'
  content: string
  timestamp: Date
  files?: string[] // 生成的文件列表
}
const messages = ref<ChatMessage[]>([])

// 当前输入
const currentInput = ref('')
const sending = ref(false)

// 生成的代码是否完成
const codeGenerated = ref(false)
const generatedFiles = ref<string[]>([])

// 部署状态
const deploying = ref(false)
const deployedUrl = ref('')

// 消息容器引用
const messagesContainer = ref<HTMLElement | null>(null)

// SSE 连接控制器
let abortController: AbortController | null = null

// 获取应用信息
const fetchAppInfo = async () => {
  try {
    const res = await getAppById({ id: appId.value })
    if (res.data.code === 0 && res.data.data) {
      appInfo.value = res.data.data
      // 如果有初始提示词，且不是查看模式，且是自己的作品，才自动发送
      if (appInfo.value.initPrompt && messages.value.length === 0 && !isViewMode.value && isOwner.value) {
        // 添加初始提示词消息
        messages.value.push({
          role: 'user',
          content: appInfo.value.initPrompt,
          timestamp: new Date(),
        })
        // 调用生成代码接口
        startCodeGen(appInfo.value.initPrompt)
      }
    } else {
      message.error('获取应用信息失败')
      router.push('/')
    }
  } catch (error) {
    message.error('获取应用信息失败')
    router.push('/')
  }
}

// 开始生成代码（SSE）- 使用 fetch 方式以获取错误信息
const startCodeGen = async (prompt: string) => {
  sending.value = true
  generatedFiles.value = []
  codeGenerated.value = false

  // 添加AI消息占位
  const aiMessageIndex = messages.value.length
  messages.value.push({
    role: 'assistant',
    content: '',
    timestamp: new Date(),
    files: [],
  })

  const url = getSSEUrl(appId.value, prompt)
  console.log('SSE连接URL:', url)

  abortController = new AbortController()

  try {
    const response = await fetch(url, {
      signal: abortController.signal,
      credentials: 'include', // 携带 cookie
    })

    console.log('SSE响应状态:', response.status)

    if (!response.ok) {
      // 获取错误信息
      let errorMsg = `请求失败: ${response.status}`
      try {
        const errorData = await response.json()
        errorMsg = errorData.message || errorData.msg || errorMsg
        console.error('后端错误响应:', errorData)
      } catch (e) {
        const errorText = await response.text()
        console.error('后端错误响应(文本):', errorText)
        if (errorText) {
          errorMsg = errorText
        }
      }

      const msg = messages.value[aiMessageIndex]
      if (msg) {
        msg.content = `生成失败: ${errorMsg}`
      }
      sending.value = false
      return
    }

    const reader = response.body?.getReader()
    if (!reader) {
      const msg = messages.value[aiMessageIndex]
      if (msg) {
        msg.content = '无法读取响应流'
      }
      sending.value = false
      return
    }

    const decoder = new TextDecoder()
    let fullContent = ''

    while (true) {
      const { done, value } = await reader.read()

      if (done) {
        console.log('SSE流读取完成, 内容长度:', fullContent.length)
        break
      }

      const chunk = decoder.decode(value, { stream: true })
      console.log('收到数据块:', chunk)

      // 解析 SSE 数据
      // 格式: data:{"d":"内容"}\n 或 data:{"d":"内容"}data:{"d":"内容"}
      // 查找所有 data: 后面的 JSON 对象
      let searchPos = 0
      while (true) {
        const dataIdx = chunk.indexOf('data:', searchPos)
        if (dataIdx === -1) break

        const jsonStart = dataIdx + 5 // 'data:'.length
        const jsonStartChar = chunk[jsonStart]

        if (jsonStartChar !== '{') {
          searchPos = jsonStart
          continue
        }

        // 找到匹配的 }
        let braceCount = 0
        let inString = false
        let escape = false
        let jsonEnd = -1

        for (let i = jsonStart; i < chunk.length; i++) {
          const c = chunk[i]

          if (escape) {
            escape = false
            continue
          }

          if (c === '\\' && inString) {
            escape = true
            continue
          }

          if (c === '"') {
            inString = !inString
            continue
          }

          if (!inString) {
            if (c === '{') braceCount++
            else if (c === '}') {
              braceCount--
              if (braceCount === 0) {
                jsonEnd = i
                break
              }
            }
          }
        }

        if (jsonEnd !== -1) {
          const jsonStr = chunk.substring(jsonStart, jsonEnd + 1)
          try {
            const json = JSON.parse(jsonStr)
            if (json.d !== undefined) {
              fullContent += json.d
              console.log('SSE收到数据:', jsonStr)
            }
          } catch (e) {
            console.log('JSON解析失败:', jsonStr)
          }
          searchPos = jsonEnd + 1
        } else {
          break
        }
      }

      // 实时更新消息内容
      if (fullContent) {
        const msg = messages.value[aiMessageIndex]
        if (msg) {
          msg.content = fullContent
        }
        scrollToBottom()
      }
    }

    // 完成
    sending.value = false
    codeGenerated.value = true
    console.log('生成完成, 最终内容长度:', fullContent.length)

    const msg = messages.value[aiMessageIndex]
    if (msg) {
      if (fullContent) {
        msg.content = fullContent
      } else {
        msg.content = '代码生成完成！您可以在右侧查看效果。'
      }
    }

  } catch (error: any) {
    console.error('SSE连接错误:', error)

    const msg = messages.value[aiMessageIndex]
    if (msg) {
      if (error.name === 'AbortError') {
        msg.content = '请求已取消'
      } else {
        msg.content = `生成过程中出现错误: ${error.message || '请检查后端服务是否正常运行'}`
      }
    }
    sending.value = false
  }
}

// 发送消息
const sendMessage = async () => {
  if (!currentInput.value.trim() || sending.value) {
    return
  }

  const prompt = currentInput.value.trim()
  currentInput.value = ''

  // 添加用户消息
  messages.value.push({
    role: 'user',
    content: prompt,
    timestamp: new Date(),
  })

  // 滚动到底部
  scrollToBottom()

  // 调用生成代码接口
  startCodeGen(prompt)
}

// 部署应用
const handleDeploy = async () => {
  if (!appInfo.value) {
    return
  }

  deploying.value = true
  try {
    const res = await deployApp({ appId: appId.value })
    if (res.data.code === 0 && res.data.data) {
      deployedUrl.value = res.data.data
      message.success('部署成功！')
      // 在新窗口打开部署的网站
      window.open(deployedUrl.value, '_blank')
    } else {
      message.error(res.data.message || '部署失败')
    }
  } catch (error) {
    message.error('部署失败')
  } finally {
    deploying.value = false
  }
}

// 返回首页
const goBack = () => {
  router.push('/')
}

// 编辑应用
const editApp = () => {
  if (appInfo.value?.id) {
    router.push(`/app/edit/${appInfo.value.id}`)
  }
}

// 删除应用
const deleteAppHandler = () => {
  if (!appInfo.value?.id) return

  Modal.confirm({
    title: '确认删除',
    content: `确定要删除应用"${appInfo.value.appName || '未命名应用'}"吗？删除后无法恢复。`,
    okText: '确定',
    cancelText: '取消',
    okType: 'danger',
    onOk: async () => {
      try {
        const res = await deleteApp({ id: appInfo.value!.id! })
        if (res.data.code === 0) {
          message.success('删除成功')
          router.push('/')
        } else {
          message.error('删除失败: ' + res.data.message)
        }
      } catch (error) {
        message.error('删除失败')
      }
    },
  })
}

// 格式化日期时间
const formatDateTime = (datetime: string | undefined) => {
  if (!datetime) return '-'
  const date = new Date(datetime)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  })
}

// 滚动到消息底部
const scrollToBottom = () => {
  nextTick(() => {
    if (messagesContainer.value) {
      messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
    }
  })
}

// 格式化时间
const formatTime = (timestamp: Date) => {
  const date = new Date(timestamp)
  return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

// 全局复制代码函数
const setupCopyCode = () => {
  (window as any).copyCode = async (codeId: string, btn: HTMLElement) => {
    const codeBlock = document.getElementById(codeId)
    if (codeBlock) {
      const code = codeBlock.textContent || ''
      try {
        await navigator.clipboard.writeText(code)
        // 更新按钮状态
        const copyText = btn.querySelector('.copy-text')
        const copyIcon = btn.querySelector('.copy-icon')
        if (copyText) copyText.textContent = '已复制'
        if (copyIcon) copyIcon.textContent = '✅'
        btn.classList.add('copied')

        // 2秒后恢复
        setTimeout(() => {
          if (copyText) copyText.textContent = '复制'
          if (copyIcon) copyIcon.textContent = '📋'
          btn.classList.remove('copied')
        }, 2000)
      } catch (err) {
        message.error('复制失败')
      }
    }
  }
}

// 组件卸载时取消请求
onUnmounted(() => {
  if (abortController) {
    abortController.abort()
    abortController = null
  }
  // 移除全局复制函数
  if ((window as any).copyCode) {
    delete (window as any).copyCode
  }
})

// 页面加载时获取应用信息
onMounted(async () => {
  // 设置全局复制函数
  setupCopyCode()
  // 先获取登录用户信息
  await loginUserStore.fetchLoginUser()
  // 再获取应用信息
  fetchAppInfo()
})
</script>

<template>
  <div class="app-chat-page">
    <!-- 顶部栏 -->
    <div class="top-bar">
      <div class="left-section">
        <a-button type="text" @click="goBack" class="back-btn">
          <template #icon>
            <ArrowLeftOutlined />
          </template>
        </a-button>
        <div class="app-info">
          <AppstoreOutlined class="app-icon" />
          <h1>{{ appInfo?.appName || '未命名应用' }}</h1>
        </div>
      </div>
      <div class="right-section">
        <!-- 应用详情按钮 -->
        <a-popover trigger="click" placement="bottomRight" :overlay-style="{ width: '280px' }">
          <template #content>
            <div class="app-detail-popover">
              <!-- 创建者信息 -->
              <div class="detail-item">
                <div class="detail-label">创建者</div>
                <div class="creator-info">
                  <a-avatar :size="32" :src="appInfo?.user?.userAvatar" class="creator-avatar">
                    <template #icon><UserOutlined /></template>
                  </a-avatar>
                  <span class="creator-name">{{ appInfo?.user?.userName || '匿名用户' }}</span>
                </div>
              </div>
              <!-- 创建时间 -->
              <div class="detail-item">
                <div class="detail-label">创建时间</div>
                <div class="detail-value">{{ formatDateTime(appInfo?.createTime) }}</div>
              </div>
              <!-- 操作栏（仅本人或管理员可见） -->
              <div v-if="canManage" class="detail-actions">
                <a-button type="default" size="small" @click="editApp">
                  <EditOutlined /> 修改
                </a-button>
                <a-button type="default" size="small" danger @click="deleteAppHandler">
                  <DeleteOutlined /> 删除
                </a-button>
              </div>
            </div>
          </template>
          <a-button size="large">
            <template #icon>
              <InfoCircleOutlined />
            </template>
            应用详情
          </a-button>
        </a-popover>
        <!-- 部署按钮 -->
        <a-button
          type="primary"
          size="large"
          :loading="deploying"
          :disabled="!codeGenerated"
          @click="handleDeploy"
        >
          <template #icon>
            <RocketOutlined />
          </template>
          部署应用
        </a-button>
      </div>
    </div>

    <!-- 核心内容区域 -->
    <div class="content-area">
      <!-- 左侧对话区域 -->
      <div class="chat-section">
        <div class="messages-container" ref="messagesContainer">
          <div
            v-for="(message, index) in messages"
            :key="index"
            :class="['message-item', message.role]"
          >
            <div class="message-content">
              <div class="message-role">
                {{ message.role === 'user' ? '我' : 'AI助手' }}
                <span class="message-time">{{ formatTime(message.timestamp) }}</span>
              </div>
              <div class="message-text">
                <div v-if="message.role === 'assistant'" class="markdown-content" v-html="renderMarkdown(message.content)"></div>
                <span v-else>{{ message.content }}</span>
              </div>
              <!-- 显示生成的文件列表 -->
              <div v-if="message.files && message.files.length > 0" class="files-list">
                <div class="files-title">生成的文件：</div>
                <div v-for="(file, fIndex) in message.files" :key="fIndex" class="file-item">
                  📄 {{ file }}
                </div>
              </div>
            </div>
          </div>
          <div v-if="sending && !messages.some(m => m.role === 'assistant' && !m.content)" class="message-item assistant">
            <div class="message-content">
              <div class="message-role">AI助手</div>
              <div class="message-text loading">
                <a-spin size="small" /> 正在生成中...
              </div>
            </div>
          </div>
        </div>
        <div class="input-area">
          <a-tooltip :title="!isOwner ? '无法在别人的作品下对话哦~' : ''" placement="top">
            <a-textarea
              v-model:value="currentInput"
              :placeholder="!isOwner ? '无法在别人的作品下对话哦~' : '继续完善你的应用描述，可以一步一步完善生成效果...'"
              :auto-size="{ minRows: 2, maxRows: 4 }"
              :disabled="sending || isInputDisabled"
              @pressEnter="sendMessage"
              class="message-input"
            />
          </a-tooltip>
          <a-button
            type="primary"
            size="large"
            :disabled="!currentInput.trim() || sending || isInputDisabled"
            @click="sendMessage"
            class="send-btn"
          >
            <template #icon>
              <SendOutlined />
            </template>
            发送
          </a-button>
        </div>
      </div>

      <!-- 右侧网页展示区域 -->
      <div class="preview-section">
        <div class="preview-header">
          <h2>应用预览</h2>
          <a-tag v-if="codeGenerated" color="success">已生成</a-tag>
          <a-tag v-else color="processing">生成中...</a-tag>
        </div>
        <div class="preview-container">
          <div v-if="codeGenerated && appInfo" class="preview-iframe-wrapper">
            <iframe
              :src="getAppPreviewUrl(appInfo.codeGenType || '', appInfo.id || '')"
              class="preview-iframe"
              frameborder="0"
            />
          </div>
          <div v-else class="preview-placeholder">
            <AppstoreOutlined class="placeholder-icon" />
            <p>代码生成完成后，这里将显示生成的应用效果</p>
            <a-spin v-if="sending" tip="正在生成代码..." />
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.app-chat-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f5f5f5;
}

.top-bar {
  background: #fff;
  padding: 8px 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.1);
  position: sticky;
  top: 0;
  z-index: 100;
}

.left-section {
  display: flex;
  align-items: center;
  gap: 8px;
}

.right-section {
  display: flex;
  align-items: center;
  gap: 12px;
}

.back-btn {
  font-size: 18px;
}

.app-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.app-icon {
  font-size: 28px;
  color: #667eea;
}

.app-info h1 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: #333;
}

/* 应用详情悬浮窗 */
.app-detail-popover {
  padding: 4px 0;
}

.detail-item {
  margin-bottom: 16px;
}

.detail-item:last-of-type {
  margin-bottom: 0;
}

.detail-label {
  font-size: 12px;
  color: #999;
  margin-bottom: 6px;
}

.detail-value {
  font-size: 14px;
  color: #333;
}

.creator-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.creator-avatar {
  flex-shrink: 0;
}

.creator-name {
  font-size: 14px;
  color: #333;
  font-weight: 500;
}

.detail-actions {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
  display: flex;
  gap: 10px;
}

.detail-actions .ant-btn {
  flex: 1;
}

.content-area {
  display: flex;
  flex: 1;
  gap: 16px;
  padding: 16px;
  max-width: 1800px;
  margin: 0 auto;
  width: 100%;
  box-sizing: border-box;
  height: calc(100vh - 64px);
  overflow: hidden;
}

/* 左侧对话区域 */
.chat-section {
  flex: 0 0 380px;
  display: flex;
  flex-direction: column;
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  height: 600px;
  max-height: 600px;
}

.messages-container {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  background: #fafafa;
  min-height: 0;
}

.message-item {
  display: flex;
  margin-bottom: 16px;
}

.message-item.user {
  justify-content: flex-end;
}

.message-item.assistant {
  justify-content: flex-start;
}

.message-content {
  max-width: 90%;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.message-item.user .message-content {
  align-items: flex-end;
}

.message-item.assistant .message-content {
  align-items: flex-start;
}

.message-role {
  font-size: 12px;
  color: #999;
  display: flex;
  align-items: center;
  gap: 8px;
}

.message-time {
  font-size: 11px;
  opacity: 0.7;
}

.message-text {
  padding: 12px 16px;
  border-radius: 12px;
  font-size: 14px;
  line-height: 1.6;
  word-break: break-word;
}

.message-text pre {
  margin: 0;
  white-space: pre-wrap;
  font-family: inherit;
}

.message-item.user .message-text {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  border-bottom-right-radius: 4px;
}

.message-item.assistant .message-text {
  background: #fff;
  color: #333;
  border: 1px solid #e8e8e8;
  border-bottom-left-radius: 4px;
}

.message-item.assistant .message-text.loading {
  color: #666;
}

.files-list {
  background: #f5f5f5;
  border-radius: 8px;
  padding: 12px;
  margin-top: 8px;
  width: 100%;
}

.files-title {
  font-size: 12px;
  color: #666;
  margin-bottom: 8px;
  font-weight: 500;
}

.file-item {
  font-size: 13px;
  color: #333;
  padding: 4px 0;
}

.input-area {
  padding: 16px;
  background: #fff;
  border-top: 1px solid #e8e8e8;
  display: flex;
  gap: 12px;
  align-items: flex-end;
}

.message-input {
  flex: 1;
  border-radius: 8px;
  border: 1px solid #e8e8e8;
}

.message-input:focus,
.message-input:hover {
  border-color: #667eea;
}

.send-btn {
  flex-shrink: 0;
  height: auto;
  min-height: 40px;
}

/* 右侧预览区域 */
.preview-section {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  height: 600px;
  min-width: 0;
}

.preview-container {
  flex: 1;
  background: #fafafa;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  min-height: 0;
}

.preview-header {
  padding: 16px 20px;
  border-bottom: 1px solid #e8e8e8;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.preview-header h2 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #333;
}

.preview-iframe-wrapper {
  width: 100%;
  height: 100%;
}

.preview-iframe {
  width: 100%;
  height: 100%;
  border: none;
}

.preview-placeholder {
  text-align: center;
  color: #999;
  padding: 40px;
}

.placeholder-icon {
  font-size: 64px;
  margin-bottom: 16px;
  opacity: 0.3;
}

.preview-placeholder p {
  font-size: 16px;
  margin-bottom: 24px;
}

/* 响应式设计 */
@media (max-width: 1200px) {
  .content-area {
    flex-direction: column;
    height: auto;
    min-height: calc(100vh - 88px);
  }

  .chat-section {
    max-width: 100%;
    min-width: auto;
    height: 500px;
  }

  .preview-section {
    min-width: auto;
    height: 500px;
  }
}

@media (max-width: 768px) {
  .top-bar {
    flex-direction: column;
    gap: 12px;
    padding: 12px 16px;
  }

  .top-bar :deep(.ant-btn-primary) {
    width: 100%;
  }

  .app-info h1 {
    font-size: 18px;
  }

  .content-area {
    padding: 16px;
    gap: 16px;
  }

  .chat-section,
  .preview-section {
    height: 400px;
  }

  .message-content {
    max-width: 95%;
  }
}

/* Markdown 内容样式 */
.markdown-content {
  font-size: 14px;
  line-height: 1.7;
}

.markdown-content h1,
.markdown-content h2,
.markdown-content h3,
.markdown-content h4,
.markdown-content h5,
.markdown-content h6 {
  margin-top: 16px;
  margin-bottom: 8px;
  font-weight: 600;
  line-height: 1.3;
}

.markdown-content h1 { font-size: 1.5em; }
.markdown-content h2 { font-size: 1.3em; }
.markdown-content h3 { font-size: 1.15em; }
.markdown-content h4 { font-size: 1em; }

.markdown-content p {
  margin: 8px 0;
}

.markdown-content ul,
.markdown-content ol {
  padding-left: 20px;
  margin: 8px 0;
}

.markdown-content li {
  margin: 4px 0;
}

.markdown-content a {
  color: #667eea;
  text-decoration: none;
}

.markdown-content a:hover {
  text-decoration: underline;
}

.markdown-content blockquote {
  border-left: 4px solid #667eea;
  padding-left: 12px;
  margin: 8px 0;
  color: #666;
  background: #f9f9f9;
  padding: 8px 12px;
  border-radius: 4px;
}

.markdown-content table {
  border-collapse: collapse;
  margin: 8px 0;
  width: 100%;
}

.markdown-content th,
.markdown-content td {
  border: 1px solid #e8e8e8;
  padding: 8px 12px;
  text-align: left;
}

.markdown-content th {
  background: #f5f5f5;
  font-weight: 600;
}

.markdown-content hr {
  border: none;
  border-top: 1px solid #e8e8e8;
  margin: 16px 0;
}

/* 行内代码样式 */
.markdown-content code {
  background: rgba(102, 126, 234, 0.1);
  color: #667eea;
  padding: 2px 6px;
  border-radius: 4px;
  font-family: 'JetBrains Mono', 'Fira Code', 'Consolas', 'Monaco', monospace;
  font-size: 13px;
}

/* 代码块容器 */
.markdown-content .code-block-wrapper {
  background: #1e1e1e;
  border-radius: 10px;
  margin: 12px 0;
  overflow: hidden;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

/* 代码块头部 */
.code-block-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 16px;
  background: #2d2d2d;
  border-bottom: 1px solid #3d3d3d;
}

.code-lang {
  font-size: 12px;
  color: #888;
  font-weight: 500;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.code-copy-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  background: transparent;
  border: 1px solid #555;
  border-radius: 6px;
  color: #aaa;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.code-copy-btn:hover {
  background: #3d3d3d;
  border-color: #667eea;
  color: #667eea;
}

.code-copy-btn.copied {
  background: rgba(102, 234, 126, 0.1);
  border-color: #66ea7e;
  color: #66ea7e;
}

.code-copy-btn .copy-icon {
  font-size: 12px;
}

/* 代码块主体 */
.markdown-content .code-block {
  background: #1e1e1e;
  padding: 16px;
  margin: 0;
  overflow-x: auto;
  border-radius: 0;
}

.markdown-content .code-block code {
  background: transparent;
  padding: 0;
  color: #d4d4d4;
  font-size: 13px;
  line-height: 1.6;
  font-family: 'JetBrains Mono', 'Fira Code', 'Consolas', 'Monaco', monospace;
}

/* Highlight.js One Dark 主题 */
.markdown-content .hljs {
  background: transparent;
  padding: 0;
}

/* 关键字 - 紫色 */
.markdown-content .hljs-keyword,
.markdown-content .hljs-selector-tag,
.markdown-content .hljs-built_in,
.markdown-content .hljs-class {
  color: #c678dd;
}

/* 字符串 - 绿色 */
.markdown-content .hljs-string,
.markdown-content .hljs-attr,
.markdown-content .hljs-template-variable,
.markdown-content .hljs-doctag {
  color: #98c379;
}

/* 数字 - 橙色 */
.markdown-content .hljs-number,
.markdown-content .hljs-literal {
  color: #d19a66;
}

/* 函数 - 蓝色 */
.markdown-content .hljs-function,
.markdown-content .hljs-title {
  color: #61afef;
}

/* 注释 - 灰色 */
.markdown-content .hljs-comment,
.markdown-content .hljs-quote {
  color: #5c6370;
  font-style: italic;
}

/* 变量 - 红色 */
.markdown-content .hljs-variable,
.markdown-content .hljs-params {
  color: #e06c75;
}

/* 标签名 - 红色/橙色 */
.markdown-content .hljs-tag {
  color: #e06c75;
}

.markdown-content .hljs-name {
  color: #e06c75;
}

/* 属性 - 黄色 */
.markdown-content .hljs-attribute {
  color: #d19a66;
}

/* HTML 特定 */
.markdown-content .hljs-tag .hljs-attr {
  color: #d19a66;
}

.markdown-content .hljs-tag .hljs-string {
  color: #98c379;
}

/* CSS 特定 */
.markdown-content .hljs-selector-id,
.markdown-content .hljs-selector-class {
  color: #e06c75;
}

.markdown-content .hljs-property,
.markdown-content .hljs-rule .hljs-keyword {
  color: #61afef;
}

/* JavaScript/TypeScript 特定 */
.markdown-content .hljs-meta {
  color: #61afef;
}

.markdown-content .hljs-symbol,
.markdown-content .hljs-bullet {
  color: #61afef;
}

/* JSON 特定 */
.markdown-content .hljs-addition {
  color: #98c379;
}

.markdown-content .hljs-deletion {
  color: #e06c75;
}

/* 正则表达式 */
.markdown-content .hljs-regexp {
  color: #98c379;
}

/* 强调 */
.markdown-content .hljs-emphasis {
  font-style: italic;
}

.markdown-content .hljs-strong {
  font-weight: bold;
}

/* 滚动条美化 */
.markdown-content .code-block::-webkit-scrollbar {
  height: 8px;
}

.markdown-content .code-block::-webkit-scrollbar-track {
  background: #1e1e1e;
}

.markdown-content .code-block::-webkit-scrollbar-thumb {
  background: #555;
  border-radius: 4px;
}

.markdown-content .code-block::-webkit-scrollbar-thumb:hover {
  background: #667eea;
}
</style>
