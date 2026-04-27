<template>
  <div id="ChatHistoryManagePage">
    <div class="container">
      <div class="search-section">
        <a-form layout="inline" :model="searchParams" @finish="doSearch">
          <a-form-item label="应用ID">
            <a-input-number v-model:value="searchParams.appId" placeholder="输入应用ID" :min="1"/>
          </a-form-item>
          <a-form-item label="用户ID">
            <a-input-number v-model:value="searchParams.userId" placeholder="输入用户ID" :min="1"/>
          </a-form-item>
          <a-form-item label="消息类型">
            <a-select v-model:value="searchParams.messageType" placeholder="选择消息类型" allow-clear style="width: 120px">
              <a-select-option value="user">用户消息</a-select-option>
              <a-select-option value="ai">AI消息</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="消息内容">
            <a-input v-model:value="searchParams.message" placeholder="输入消息内容关键词" style="width: 200px"/>
          </a-form-item>
          <a-form-item>
            <a-button type="primary" html-type="submit">搜索</a-button>
          </a-form-item>
        </a-form>
      </div>
      <a-divider/>

      <a-table
          :columns="columns"
          :data-source="data"
          :pagination="pagination"
          @change="doTableChange"
          :row-key="(record: TableRecord) => record.id"
          :loading="loading"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'id'">
            {{ record.id }}
          </template>
          <template v-else-if="column.dataIndex === 'message'">
            <div class="message-text" :title="record.message">
              {{ truncateMessage(record.message) }}
            </div>
          </template>
          <template v-else-if="column.dataIndex === 'messageType'">
            <a-tag :color="record.messageType === 'user' ? 'blue' : 'green'">
              {{ record.messageType === 'user' ? '用户消息' : 'AI消息' }}
            </a-tag>
          </template>
          <template v-else-if="column.dataIndex === 'appId'">
            {{ record.appId }}
          </template>
          <template v-else-if="column.dataIndex === 'userId'">
            {{ record.userId || '-' }}
          </template>
          <template v-else-if="column.dataIndex === 'createTime'">
            {{ dayjs(record.createTime).format('YYYY-MM-DD HH:mm:ss') }}
          </template>
        </template>
      </a-table>
    </div>
  </div>

</template>

<script lang="ts" setup>
import { computed, onMounted, reactive, ref } from "vue";
import { listAppChatHistoryByPageForAdmin } from "@/api/chatHistoryController";
import { message } from "ant-design-vue";
import dayjs from "dayjs";

interface TableRecord {
  id?: number
  message?: string
  messageType?: string
  appId?: number
  userId?: number
  createTime?: string
  updateTime?: string
}

const columns = [
  {
    title: 'ID',
    dataIndex: 'id',
    width: 80,
    fixed: 'left',
  },
  {
    title: '消息内容',
    dataIndex: 'message',
    width: 300,
    ellipsis: true,
  },
  {
    title: '消息类型',
    dataIndex: 'messageType',
    width: 120,
  },
  {
    title: '应用ID',
    dataIndex: 'appId',
    width: 100,
  },
  {
    title: '用户ID',
    dataIndex: 'userId',
    width: 100,
  },
  {
    title: '创建时间',
    dataIndex: 'createTime',
    width: 180,
  },
];

const data = ref<API.ChatHistory[]>([])
const loading = ref(false)
const total = ref(0)

// 搜索条件
const searchParams = reactive<API.ChatHistoryQueryRequest>({
  pageNum: 1,
  pageSize: 10,
  appId: undefined,
  userId: undefined,
  messageType: undefined,
  message: '',
})

// 截断消息内容
const truncateMessage = (msg: string | undefined): string => {
  if (!msg) return '-'
  return msg.length > 100 ? msg.substring(0, 100) + '...' : msg
}

// 调用后端API获取数据
const fetchData = async () => {
  loading.value = true
  try {
    // 过滤掉 undefined 和空字符串的参数
    const params: Record<string, any> = {}
    Object.keys(searchParams).forEach(key => {
      const value = (searchParams as Record<string, any>)[key]
      if (value !== undefined && value !== '') {
        params[key] = value
      }
    })

    const res = await listAppChatHistoryByPageForAdmin(params)

    // 检查响应状态码
    if (res.data.code != 0) {
      message.error('获取数据失败: ' + (res.data.message || '未知错误'))
      data.value = []
      total.value = 0
      return
    }
    // 检查数据是否存在
    if (res.data.data) {
      data.value = res.data.data.records ?? []
      total.value = res.data.data.totalRow ?? 0
    } else {
      data.value = []
      total.value = 0
    }
  } catch (error: any) {
    console.error('获取对话历史列表失败:', error)
    message.error('获取数据失败: ' + (error.message || '网络错误'))
    data.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

// 分页参数
const pagination = computed(() => {
  return {
    current: searchParams.pageNum,
    pageSize: searchParams.pageSize,
    total: total.value,
    showSizeChanger: true,
    showTotal: (total: number) => `共${total}条`,
    pageSizeOptions: ['10', '20', '50', '100'],
  }
})

// 表格变化时的操作
const doTableChange = (page: any) => {
  searchParams.pageNum = page.current
  searchParams.pageSize = page.pageSize
  fetchData()
}

// 搜索
const doSearch = () => {
  // 重置页码
  searchParams.pageNum = 1
  fetchData()
}

// 页面加载时请求
onMounted(() => {
  fetchData()
})
</script>

<style scoped>
#ChatHistoryManagePage {
  min-height: 100vh;
  padding: 24px;
  background: var(--bg-surface);
}

.container {
  max-width: 1800px;
  margin: 0 auto;
  background: var(--color-card-bg);
  border-radius: 12px;
  padding: 24px;
  box-shadow: var(--shadow-sm);
  border: 1px solid var(--color-border-light);
}

.search-section {
  background: var(--bg-surface);
  padding: 20px;
  border-radius: 8px;
  margin-bottom: 24px;
  border: 1px solid var(--color-border-light);
}

.message-text {
  max-width: 280px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: var(--color-text-primary);
}

:deep(.ant-table) {
  border-radius: 6px;
  overflow: hidden;
}

:deep(.ant-table-thead > tr > th) {
  background: var(--bg-surface) !important;
  font-weight: 600;
}

@media (max-width: 768px) {
  #ChatHistoryManagePage {
    padding: 16px;
  }

  .container {
    padding: 16px;
  }

  .search-section {
    padding: 16px;
  }

  :deep(.ant-table) {
    font-size: 12px;
  }
}
</style>
