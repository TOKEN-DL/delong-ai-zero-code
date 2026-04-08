<script setup lang="ts">
import { ref, onMounted, nextTick, onUnmounted, computed, h } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import { getAppById, deployApp, deleteApp } from '@/api/appController'
import { listAppChatHistory } from '@/api/chatHistoryController'
import { useLoginUserStore } from '@/stores/loginUser'
import { getSSEUrl, getAppPreviewUrl } from '@/config'
import { getCodeGenTypeLabel } from '@/constants/app'
import { SendOutlined, RocketOutlined, AppstoreOutlined, ArrowLeftOutlined, InfoCircleOutlined, UserOutlined, EditOutlined, DeleteOutlined, DownloadOutlined, HighlightOutlined, CloseOutlined, ExportOutlined, ReloadOutlined } from '@ant-design/icons-vue'
import { marked } from 'marked'
import hljs from 'highlight.js'
import {
  visualEditor,
  type SelectedElement,
  generateElementPrompt
} from '@/utils/visualEditor'

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
const isInputDisabled = computed(() => !isOwner.value)

// 对话消息
interface ChatMessage {
  id?: number
  role: 'user' | 'assistant'
  content: string
  timestamp: Date
  createTime?: string
  files?: string[] // 生成的文件列表
}
const messages = ref<ChatMessage[]>([])

// 当前输入
const currentInput = ref('')
const sending = ref(false)

// 对话历史加载状态
const loadingHistory = ref(false)
const hasMoreHistory = ref(false)
const lastCreateTime = ref<string | undefined>(undefined)

// 对话历史总数
const chatHistoryTotal = ref(0)

// 生成的代码是否完成
const codeGenerated = ref(false)
const generatedFiles = ref<string[]>([])

// 预览 iframe 刷新 key
const previewKey = ref(Date.now())

// 是否显示网站预览（代码已生成 或 至少有2条对话记录）
const showPreview = computed(() => {
  return codeGenerated.value || chatHistoryTotal.value >= 2
})

// 部署状态
const deploying = ref(false)
const deployedUrl = ref('')

// 下载状态
const downloading = ref(false)

// 消息容器引用
const messagesContainer = ref<HTMLElement | null>(null)

// 预览 iframe 引用
const previewIframe = ref<HTMLIFrameElement | null>(null)

// 可视化编辑相关状态
const isEditMode = ref(false)
const selectedElements = ref<SelectedElement[]>([])

// 处理 iframe 加载完成
const handleIframeLoad = () => {
  const iframe = previewIframe.value

  if (iframe) {
    // 设置元素选中回调
    visualEditor.setOnElementSelected((elements: SelectedElement[]) => {
      selectedElements.value = [...elements]
    })
    // 设置 iframe 引用
    visualEditor.setIframe(iframe)
  }
}

// 切换编辑模式
const toggleEditMode = () => {
  isEditMode.value = visualEditor.toggleEditMode()
}

// 刷新预览
const refreshPreview = () => {
  previewKey.value = Date.now()
  message.success('预览已刷新')
}

// 自动刷新预览（代码生成完成后调用）
const autoRefreshPreview = () => {
  console.log('开始自动刷新预览...')

  // 第一次尝试：1.5秒后刷新
  setTimeout(() => {
    previewKey.value = Date.now()
    console.log('预览区域第一次刷新 (key changed)')

    // 第二次尝试：3秒后再次刷新，确保后端文件完全写入
    setTimeout(() => {
      previewKey.value = Date.now()
      console.log('预览区域第二次刷新 (key changed)')

      // 尝试直接刷新 iframe 内容
      nextTick(() => {
        if (previewIframe.value?.contentWindow) {
          try {
            previewIframe.value.contentWindow.location.reload()
            console.log('预览 iframe 已通过 reload 刷新')
          } catch (e) {
            console.log('预览区域已通过 key 刷新')
          }
        }
      })
    }, 1500)
  }, 1500)
}

// 在新窗口打开
const openInNewTab = () => {
  if (appInfo.value && showPreview.value) {
    const url = getAppPreviewUrl(appInfo.value.codeGenType || '', appInfo.value.id || '')
    window.open(url, '_blank')
  }
}

// 移除选中的元素
const removeSelectedElement = (xpath: string) => {
  visualEditor.removeSelectedElement(xpath)
}

// 清除所有选中的元素
const clearSelectedElements = () => {
  visualEditor.clearSelectedElements()
  selectedElements.value = []
}

// SSE 连接控制器
let abortController: AbortController | null = null

// 获取应用信息
const fetchAppInfo = async () => {
  try {
    // @ts-ignore 避免大数字精度丢失
    const res = await getAppById({ id: appId.value })
    if (res.data.code === 0 && res.data.data) {
      appInfo.value = res.data.data
      // 加载对话历史
      await loadChatHistory()
      console.log('对话历史加载完成，消息数量:', messages.value.length)
      // 如果是自己的作品，且没有对话历史，且有初始提示词，才自动发送
      if (isOwner.value && messages.value.length === 0 && appInfo.value.initPrompt) {
        console.log('没有历史记录，自动发送初始提示词')
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

// 加载对话历史
const loadChatHistory = async (loadMore = false) => {
  if (loadingHistory.value) return

  loadingHistory.value = true
  try {
    // @ts-ignore 避免大数字精度丢失
    const params: any = {
      appId: appId.value,
      pageSize: 10,
    }

    // 如果是加载更多，传入上一页最后一条消息的创建时间
    if (loadMore && lastCreateTime.value) {
      params.lastCreateTime = lastCreateTime.value
    }

    const res = await listAppChatHistory(params)
    console.log('加载对话历史响应:', res.data)

    if (res.data.code === 0 && res.data.data) {
      const pageData = res.data.data
      const records = pageData.records || []

      // 更新总数
      chatHistoryTotal.value = pageData.totalRow || 0
      console.log('对话历史记录:', records)

      if (records.length > 0) {
        // 转换为 ChatMessage 格式
        const historyMessages: ChatMessage[] = records.map(record => ({
          id: record.id,
          role: record.messageType === 'user' ? 'user' : 'assistant',
          content: record.message || '',
          timestamp: new Date(record.createTime || ''),
          createTime: record.createTime,
        }))

        // 后端返回的是按创建时间降序（最新的在前），需要反转为升序展示
        historyMessages.reverse()

        if (loadMore) {
          // 加载更多：插入到消息列表头部（更早的消息）
          messages.value = [...historyMessages, ...messages.value]
        } else {
          // 初次加载：直接设置消息列表
          messages.value = historyMessages
        }

        // 更新游标（取最早一条消息的创建时间）
        // 反转后第一条是最早的
        const firstRecord = records[0]
        if (firstRecord) {
          lastCreateTime.value = firstRecord.createTime
        }

        // 判断是否还有更多历史
        hasMoreHistory.value = records.length >= 10
      } else {
        hasMoreHistory.value = false
      }

      // 滚动到底部（仅初次加载时）
      if (!loadMore) {
        scrollToBottom()
      }
    } else {
      console.error('加载对话历史失败:', res.data.message)
      hasMoreHistory.value = false
    }
  } catch (error) {
    console.error('加载对话历史异常:', error)
    hasMoreHistory.value = false
  } finally {
    loadingHistory.value = false
  }
}

// 加载更多历史消息
const loadMoreHistory = () => {
  loadChatHistory(true)
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

    // 自动刷新预览区域，等待后端文件写入完成
    autoRefreshPreview()

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

  let prompt = currentInput.value.trim()
  currentInput.value = ''

  // 合并获取选中的元素：优先使用 Vue 响应式数据，备选从 visualEditor 直接获取
  const elements = selectedElements.value.length > 0
    ? selectedElements.value
    : visualEditor.getSelectedElements()

  // 如果有选中的元素，将元素信息添加到提示词中
  if (elements.length > 0) {
    const elementPrompt = generateElementPrompt(elements)
    prompt = prompt + elementPrompt
  }

  // 添加用户消息（显示原始提示词，不包含元素信息）
  messages.value.push({
    role: 'user',
    content: prompt.split('\n--- 用户选中的页面元素 ---')[0] || prompt,
    timestamp: new Date(),
  })

  // 清除选中元素并退出编辑模式
  if (isEditMode.value) {
    clearSelectedElements()
    isEditMode.value = false
    visualEditor.exitEditMode()
  }

  // 滚动到底部
  scrollToBottom()

  // 调用生成代码接口
  startCodeGen(prompt)
}

// 处理回车键发送
const handlePressEnter = (e: KeyboardEvent) => {
  // Shift+Enter 换行，单独 Enter 发送
  if (!e.shiftKey) {
    e.preventDefault()
    sendMessage()
  }
}

// 部署应用
const handleDeploy = async () => {
  if (!appInfo.value) {
    return
  }

  deploying.value = true
  try {
    // @ts-ignore 避免大数字精度丢失
    const res = await deployApp({ appId: appId.value })
    if (res.data.code === 0 && res.data.data) {
      const url = res.data.data
      deployedUrl.value = url
      Modal.confirm({
        icon: h('span', { style: 'font-size: 22px' }, '🎉'),
        title: '部署成功',
        content: h('div', { style: 'margin-top: 8px' }, [
          h('p', { style: 'margin-bottom: 12px; color: #666' }, '您的应用已成功部署，可以通过以下链接访问：'),
          h('div', {
            style: 'background: #f5f5f5; padding: 12px; border-radius: 8px; display: flex; justify-content: space-between; align-items: center; gap: 12px;'
          }, [
            h('span', { style: 'color: #1890ff; word-break: break-all; flex: 1' }, url),
            h('button', {
              style: 'background: #1890ff; color: white; border: none; padding: 6px 12px; border-radius: 4px; cursor: pointer; white-space: nowrap; font-size: 13px',
              onClick: () => {
                navigator.clipboard.writeText(url)
                message.success('链接已复制')
              }
            }, '复制链接')
          ])
        ]),
        okText: '访问应用',
        cancelText: '关闭',
        width: 520,
        onOk: () => window.open(url, '_blank')
      })
    } else {
      message.error(res.data.message || '部署失败')
    }
  } catch (error) {
    message.error('部署失败')
  } finally {
    deploying.value = false
  }
}

// 下载应用代码
const handleDownload = async () => {
  if (!appInfo.value) {
    return
  }

  downloading.value = true
  try {
    const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api'
    const url = `${API_BASE_URL}/app/download/${appId.value}?appId=${appId.value}`

    const response = await fetch(url, {
      method: 'GET',
      credentials: 'include',
    })

    if (!response.ok) {
      // 尝试解析错误信息
      let errorMsg = `下载失败: ${response.status}`
      try {
        const errorData = await response.json()
        errorMsg = errorData.message || errorData.msg || errorMsg
      } catch (e) {
        // 忽略解析错误
      }
      message.error(errorMsg)
      return
    }

    // 从 Content-Disposition 头获取文件名
    const contentDisposition = response.headers.get('Content-Disposition')
    let filename = `${appInfo.value.appName || 'app'}.zip`
    if (contentDisposition) {
      const match = contentDisposition.match(/filename="?(.+?)"?$/)
      if (match && match[1]) {
        filename = match[1]
      }
    }

    // 获取 blob 数据
    const blob = await response.blob()

    // 创建下载链接
    const downloadUrl = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = downloadUrl
    link.download = filename
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(downloadUrl)

    message.success('下载成功')
  } catch (error) {
    console.error('下载失败:', error)
    message.error('下载失败')
  } finally {
    downloading.value = false
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
  // 销毁可视化编辑器
  visualEditor.destroy()
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
          <a-tag v-if="appInfo?.codeGenType" color="processing" class="code-gen-type-tag">
            {{ getCodeGenTypeLabel(appInfo.codeGenType) }}
          </a-tag>
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
              <!-- 生成类型 -->
              <div class="detail-item">
                <div class="detail-label">生成类型</div>
                <div class="detail-value">
                  <a-tag v-if="appInfo?.codeGenType" color="processing">
                    {{ getCodeGenTypeLabel(appInfo.codeGenType) }}
                  </a-tag>
                  <span v-else>-</span>
                </div>
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
        <!-- 下载代码按钮 -->
        <a-button
          size="large"
          :loading="downloading"
          :disabled="!showPreview"
          @click="handleDownload"
        >
          <template #icon>
            <DownloadOutlined />
          </template>
          下载代码
        </a-button>
        <!-- 部署按钮 -->
        <a-button
          type="primary"
          size="large"
          :loading="deploying"
          :disabled="!showPreview"
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
          <!-- 加载更多历史消息按钮 -->
          <div v-if="hasMoreHistory" class="load-more-wrapper">
            <a-button
              type="link"
              :loading="loadingHistory"
              @click="loadMoreHistory"
              class="load-more-btn"
            >
              <template #icon v-if="!loadingHistory">
                <ArrowLeftOutlined />
              </template>
              {{ loadingHistory ? '加载中...' : '加载更多历史消息' }}
            </a-button>
          </div>
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
          <!-- 选中的元素信息展示 -->
          <div v-if="selectedElements.length > 0" class="selected-elements-info">
            <a-alert type="info" class="selected-elements-alert">
              <template #message>
                <div class="selected-elements-header">
                  <span>已选中 {{ selectedElements.length }} 个元素</span>
                  <a-button type="link" size="small" @click="clearSelectedElements">
                    <template #icon><CloseOutlined /></template>
                    清除全部
                  </a-button>
                </div>
              </template>
              <template #description>
                <div class="selected-elements-list">
                  <div v-for="(element, index) in selectedElements" :key="element.xpath" class="selected-element-item">
                    <div class="element-info">
                      <span class="element-tag">{{ element.tagName }}</span>
                      <span v-if="element.id" class="element-id">#{{ element.id }}</span>
                      <span v-if="element.className" class="element-class">.{{ element.className.split(' ')[0] }}</span>
                      <span v-if="element.text" class="element-text">"{{ element.text.slice(0, 30) }}{{ element.text.length > 30 ? '...' : '' }}"</span>
                    </div>
                    <a-button type="text" size="small" class="remove-element-btn" @click="removeSelectedElement(element.xpath)">
                      <template #icon><CloseOutlined /></template>
                    </a-button>
                  </div>
                </div>
              </template>
            </a-alert>
          </div>

          <!-- 编辑模式提示 -->
          <div v-if="isEditMode" class="edit-mode-tip">
            <a-alert type="warning" message="编辑模式已开启" description="在右侧预览区域点击元素即可选中，选中后发送消息时会自动附加元素信息。" closable />
          </div>

          <div class="input-row">
            <a-tooltip :title="!isOwner ? '无法在别人的作品下对话哦~' : ''" placement="top">
              <a-textarea
                v-model:value="currentInput"
                :placeholder="!isOwner ? '无法在别人的作品下对话哦~' : (isEditMode ? '描述你想对选中元素进行的修改...' : '请描述你想生成的网站，越详细效果越好哦')"
                :auto-size="{ minRows: 2, maxRows: 4 }"
                :disabled="sending || isInputDisabled"
                @pressEnter="handlePressEnter"
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
      </div>

      <!-- 右侧网页展示区域 -->
      <div class="preview-section">
        <div class="preview-header">
          <div class="preview-header-left">
            <h2>应用预览</h2>
            <a-tag v-if="showPreview" color="success">已生成</a-tag>
            <a-tag v-else color="processing">生成中...</a-tag>
          </div>
          <div class="preview-header-right">
            <!-- 选择元素按钮 -->
            <a-tooltip :title="!showPreview ? '请先生成代码' : (isEditMode ? '退出编辑模式' : '点击选择要修改的页面元素')" placement="bottom">
              <a-button
                :type="isEditMode ? 'primary' : 'default'"
                size="small"
                :disabled="!showPreview || isInputDisabled"
                @click="toggleEditMode"
              >
                <template #icon>
                  <HighlightOutlined />
                </template>
                {{ isEditMode ? '退出编辑' : '选择元素' }}
              </a-button>
            </a-tooltip>
            <!-- 刷新预览按钮 -->
            <a-tooltip title="刷新预览" placement="bottom">
              <a-button
                size="small"
                :disabled="!showPreview"
                @click="refreshPreview"
              >
                <template #icon>
                  <ReloadOutlined />
                </template>
              </a-button>
            </a-tooltip>
            <!-- 新窗口打开按钮 -->
            <a-tooltip title="在新窗口中打开" placement="bottom">
              <a-button
                size="small"
                :disabled="!showPreview"
                @click="openInNewTab"
              >
                <template #icon>
                  <ExportOutlined />
                </template>
              </a-button>
            </a-tooltip>
          </div>
        </div>
        <div class="preview-container">
          <div v-if="showPreview && appInfo" class="preview-iframe-wrapper">
            <iframe
              ref="previewIframe"
              :src="getAppPreviewUrl(appInfo.codeGenType || '', appInfo.id || '') + '?t=' + previewKey"
              :key="previewKey"
              class="preview-iframe"
              frameborder="0"
              @load="handleIframeLoad"
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
  background: linear-gradient(180deg, #e0f2fe 0%, #f0f9ff 50%, #ffffff 100%);
}

.top-bar {
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(8px);
  padding: 8px 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  box-shadow: 0 1px 4px rgba(14, 165, 233, 0.1);
  position: sticky;
  top: 0;
  z-index: 100;
  border-bottom: 1px solid #e0f2fe;
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
  color: #0ea5e9;
}

.app-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.app-icon {
  font-size: 28px;
  color: #0ea5e9;
}

.app-info h1 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: #0c4a6e;
}

.code-gen-type-tag {
  margin-left: 4px;
  font-size: 12px;
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
  color: #0ea5e9;
  margin-bottom: 6px;
}

.detail-value {
  font-size: 14px;
  color: #0c4a6e;
}

.creator-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.creator-avatar {
  flex-shrink: 0;
  border: 2px solid #e0f2fe;
}

.creator-name {
  font-size: 14px;
  color: #0c4a6e;
  font-weight: 500;
}

.detail-actions {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #f0f9ff;
  display: flex;
  gap: 10px;
}

.detail-actions .ant-btn {
  flex: 1;
}

.content-area {
  display: flex;
  flex: 1;
  gap: 12px;
  padding: 12px;
  max-width: 1800px;
  margin: 0 auto;
  width: 100%;
  box-sizing: border-box;
  height: calc(100vh - 64px);
  overflow: hidden;
}

/* 左侧对话区域 - 2:3 比例 */
.chat-section {
  flex: 2;
  display: flex;
  flex-direction: column;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 4px 16px rgba(14, 165, 233, 0.1);
  min-width: 0;
  border: 1px solid #e0f2fe;
  height: 600px;
  max-height: 600px;
}

.messages-container {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  background: linear-gradient(180deg, #f0f9ff 0%, #ffffff 100%);
  min-height: 0;
}

/* 加载更多按钮 */
.load-more-wrapper {
  text-align: center;
  padding: 8px 0 16px;
  margin-bottom: 8px;
}

.load-more-btn {
  color: #0ea5e9;
  font-size: 13px;
}

.load-more-btn:hover {
  color: #0284c7;
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
  color: #0ea5e9;
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
  background: linear-gradient(135deg, #0ea5e9 0%, #06b6d4 100%);
  color: #fff;
  border-bottom-right-radius: 4px;
  box-shadow: 0 2px 8px rgba(14, 165, 233, 0.25);
}

.message-item.assistant .message-text {
  background: #ffffff;
  color: #0c4a6e;
  border: 1px solid #e0f2fe;
  border-bottom-left-radius: 4px;
}

.message-item.assistant .message-text.loading {
  color: #0ea5e9;
}

.files-list {
  background: #f0f9ff;
  border-radius: 8px;
  padding: 12px;
  margin-top: 8px;
  width: 100%;
  border: 1px solid #e0f2fe;
}

.files-title {
  font-size: 12px;
  color: #0369a1;
  margin-bottom: 8px;
  font-weight: 500;
}

.file-item {
  font-size: 13px;
  color: #0c4a6e;
  padding: 4px 0;
}

.input-area {
  padding: 16px;
  background: #ffffff;
  border-top: 1px solid #e0f2fe;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.input-row {
  display: flex;
  gap: 12px;
  align-items: flex-end;
}

.edit-mode-btn {
  flex-shrink: 0;
  height: auto;
  min-height: 40px;
}

.edit-mode-btn:disabled {
  opacity: 0.5;
}

.selected-elements-info {
  margin-bottom: 8px;
}

.selected-elements-alert {
  border-radius: 8px;
}

.selected-elements-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.selected-elements-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 8px;
}

.selected-element-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  background: #f0f9ff;
  border-radius: 6px;
  border: 1px solid #bae6fd;
}

.element-info {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
  overflow: hidden;
}

.element-tag {
  background: #0ea5e9;
  color: white;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
}

.element-id {
  color: #f59e0b;
  font-size: 12px;
}

.element-class {
  color: #8b5cf6;
  font-size: 12px;
}

.element-text {
  color: #64748b;
  font-size: 12px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.remove-element-btn {
  flex-shrink: 0;
  color: #94a3b8;
}

.remove-element-btn:hover {
  color: #ef4444;
}

.edit-mode-tip {
  margin-bottom: 8px;
}

.edit-mode-tip :deep(.ant-alert) {
  border-radius: 8px;
}

.message-input {
  flex: 1;
  border-radius: 8px;
  border: 1px solid #bae6fd;
}

.message-input:focus,
.message-input:hover {
  border-color: #38bdf8;
  box-shadow: 0 0 0 2px rgba(56, 189, 248, 0.15);
}

.send-btn {
  flex-shrink: 0;
  height: auto;
  min-height: 40px;
  background: linear-gradient(135deg, #0ea5e9 0%, #06b6d4 100%);
  border: none;
}

.send-btn:hover {
  background: linear-gradient(135deg, #0284c7 0%, #0891b2 100%);
}

/* 右侧预览区域 - 2:3 比例 */
.preview-section {
  flex: 3;
  display: flex;
  flex-direction: column;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 4px 16px rgba(14, 165, 233, 0.1);
  min-width: 0;
  border: 1px solid #e0f2fe;
  height: 600px;
  max-height: 600px;
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
  padding: 12px 16px;
  border-bottom: 1px solid #e0f2fe;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: linear-gradient(180deg, #ffffff 0%, #f0f9ff 100%);
}

.preview-header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.preview-header-left h2 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #0c4a6e;
}

.preview-header-right {
  display: flex;
  align-items: center;
  gap: 8px;
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
  color: #0ea5e9;
  padding: 40px;
}

.placeholder-icon {
  font-size: 64px;
  margin-bottom: 16px;
  opacity: 0.4;
}

.preview-placeholder p {
  font-size: 16px;
  margin-bottom: 24px;
  color: #0369a1;
}

/* 响应式设计 */
@media (max-width: 1200px) {
  .content-area {
    flex-direction: column;
    height: auto;
    min-height: calc(100vh - 88px);
  }

  .chat-section {
    flex: none;
    height: 400px;
  }

  .preview-section {
    flex: none;
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
    padding: 8px;
    gap: 8px;
  }

  .chat-section {
    height: 350px;
  }

  .preview-section {
    height: 400px;
  }

  .message-content {
    max-width: 95%;
  }
}

/* Markdown 内容样式 - 使用 :deep() 穿透 scoped 样式 */
.markdown-content {
  font-size: 14px;
  line-height: 1.7;
}

.markdown-content :deep(h1),
.markdown-content :deep(h2),
.markdown-content :deep(h3),
.markdown-content :deep(h4),
.markdown-content :deep(h5),
.markdown-content :deep(h6) {
  margin-top: 16px;
  margin-bottom: 8px;
  font-weight: 600;
  line-height: 1.3;
  color: #0c4a6e;
}

.markdown-content :deep(h1) { font-size: 1.5em; }
.markdown-content :deep(h2) { font-size: 1.3em; }
.markdown-content :deep(h3) { font-size: 1.15em; }
.markdown-content :deep(h4) { font-size: 1em; }

.markdown-content :deep(p) {
  margin: 8px 0;
}

.markdown-content :deep(ul),
.markdown-content :deep(ol) {
  padding-left: 20px;
  margin: 8px 0;
}

.markdown-content :deep(li) {
  margin: 4px 0;
}

.markdown-content :deep(a) {
  color: #0ea5e9;
  text-decoration: none;
}

.markdown-content :deep(a:hover) {
  text-decoration: underline;
  color: #0284c7;
}

.markdown-content :deep(blockquote) {
  border-left: 4px solid #0ea5e9;
  padding-left: 12px;
  margin: 8px 0;
  color: #0369a1;
  background: #f0f9ff;
  padding: 8px 12px;
  border-radius: 4px;
}

.markdown-content :deep(table) {
  border-collapse: collapse;
  margin: 8px 0;
  width: 100%;
}

.markdown-content :deep(th),
.markdown-content :deep(td) {
  border: 1px solid #bae6fd;
  padding: 8px 12px;
  text-align: left;
}

.markdown-content :deep(th) {
  background: #e0f2fe;
  font-weight: 600;
  color: #0c4a6e;
}

.markdown-content :deep(hr) {
  border: none;
  border-top: 1px solid #e0f2fe;
  margin: 16px 0;
}

/* 行内代码样式 */
.markdown-content :deep(code) {
  background: rgba(14, 165, 233, 0.1);
  color: #0284c7;
  padding: 2px 6px;
  border-radius: 4px;
  font-family: 'JetBrains Mono', 'Fira Code', 'Consolas', 'Monaco', monospace;
  font-size: 13px;
}

/* 代码块容器 */
.markdown-content :deep(.code-block-wrapper) {
  background: #1e293b;
  border-radius: 10px;
  margin: 12px 0;
  overflow: hidden;
  box-shadow: 0 4px 12px rgba(14, 165, 233, 0.15);
}

/* 代码块头部 */
.markdown-content :deep(.code-block-header) {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 16px;
  background: #334155;
  border-bottom: 1px solid #475569;
}

.markdown-content :deep(.code-lang) {
  font-size: 12px;
  color: #7dd3fc;
  font-weight: 500;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.markdown-content :deep(.code-copy-btn) {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  background: transparent;
  border: 1px solid #64748b;
  border-radius: 6px;
  color: #94a3b8;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.markdown-content :deep(.code-copy-btn:hover) {
  background: #475569;
  border-color: #38bdf8;
  color: #38bdf8;
}

.markdown-content :deep(.code-copy-btn.copied) {
  background: rgba(74, 222, 128, 0.15);
  border-color: #4ade80;
  color: #4ade80;
}

.markdown-content :deep(.copy-icon) {
  font-size: 12px;
}

/* 代码块主体 */
.markdown-content :deep(.code-block) {
  background: #1e293b;
  padding: 16px;
  margin: 0;
  overflow-x: auto;
  border-radius: 0;
}

.markdown-content :deep(.code-block code) {
  background: transparent;
  padding: 0;
  color: #e2e8f0;
  font-size: 13px;
  line-height: 1.6;
  font-family: 'JetBrains Mono', 'Fira Code', 'Consolas', 'Monaco', monospace;
}

/* Highlight.js Ocean Blue 主题 */
.markdown-content :deep(.hljs) {
  background: transparent;
  padding: 0;
}

/* 关键字 - 青色 */
.markdown-content :deep(.hljs-keyword),
.markdown-content :deep(.hljs-selector-tag),
.markdown-content :deep(.hljs-built_in),
.markdown-content :deep(.hljs-class) {
  color: #22d3ee;
}

/* 字符串 - 绿色 */
.markdown-content :deep(.hljs-string),
.markdown-content :deep(.hljs-attr),
.markdown-content :deep(.hljs-template-variable),
.markdown-content :deep(.hljs-doctag) {
  color: #4ade80;
}

/* 数字 - 橙色 */
.markdown-content :deep(.hljs-number),
.markdown-content :deep(.hljs-literal) {
  color: #fb923c;
}

/* 函数 - 蓝色 */
.markdown-content :deep(.hljs-function),
.markdown-content :deep(.hljs-title) {
  color: #60a5fa;
}

/* 注释 - 灰色 */
.markdown-content :deep(.hljs-comment),
.markdown-content :deep(.hljs-quote) {
  color: #64748b;
  font-style: italic;
}

/* 变量 - 粉色 */
.markdown-content :deep(.hljs-variable),
.markdown-content :deep(.hljs-params) {
  color: #f472b6;
}

/* 标签名 */
.markdown-content :deep(.hljs-tag) {
  color: #f472b6;
}

.markdown-content :deep(.hljs-name) {
  color: #f472b6;
}

/* 属性 - 黄色 */
.markdown-content :deep(.hljs-attribute) {
  color: #fbbf24;
}

/* HTML 特定 */
.markdown-content :deep(.hljs-tag .hljs-attr) {
  color: #fbbf24;
}

.markdown-content :deep(.hljs-tag .hljs-string) {
  color: #4ade80;
}

/* CSS 特定 */
.markdown-content :deep(.hljs-selector-id),
.markdown-content :deep(.hljs-selector-class) {
  color: #f472b6;
}

.markdown-content :deep(.hljs-property),
.markdown-content :deep(.hljs-rule .hljs-keyword) {
  color: #60a5fa;
}

/* JavaScript/TypeScript 特定 */
.markdown-content :deep(.hljs-meta) {
  color: #60a5fa;
}

.markdown-content :deep(.hljs-symbol),
.markdown-content :deep(.hljs-bullet) {
  color: #60a5fa;
}

/* JSON 特定 */
.markdown-content :deep(.hljs-addition) {
  color: #4ade80;
}

.markdown-content :deep(.hljs-deletion) {
  color: #f472b6;
}

/* 正则表达式 */
.markdown-content :deep(.hljs-regexp) {
  color: #4ade80;
}

/* 强调 */
.markdown-content :deep(.hljs-emphasis) {
  font-style: italic;
}

.markdown-content :deep(.hljs-strong) {
  font-weight: bold;
}

/* 滚动条美化 */
.markdown-content :deep(.code-block::-webkit-scrollbar) {
  height: 8px;
}

.markdown-content :deep(.code-block::-webkit-scrollbar-track) {
  background: #1e293b;
}

.markdown-content :deep(.code-block::-webkit-scrollbar-thumb) {
  background: #475569;
  border-radius: 4px;
}

.markdown-content :deep(.code-block::-webkit-scrollbar-thumb:hover) {
  background: #0ea5e9;
}
</style>
