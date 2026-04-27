<template>
  <div id="AppManagePage">
    <div class="container">
      <div class="search-section">
        <a-form layout="inline" :model="searchParams" @finish="doSearch">
          <a-form-item label="应用名称">
            <a-input v-model:value="searchParams.appName" placeholder="输入应用名称"/>
          </a-form-item>
          <a-form-item label="生成类型">
            <a-select v-model:value="searchParams.codeGenType" placeholder="选择生成类型" allow-clear style="width: 160px">
              <a-select-option v-for="item in CODE_GEN_TYPE_OPTIONS" :key="item.value" :value="item.value">
                {{ item.label }}
              </a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="用户ID">
            <a-input-number v-model:value="searchParams.userId" placeholder="输入用户ID" :min="1"/>
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
          <template v-else-if="column.dataIndex === 'appName'">
            <div class="app-name">{{ record.appName || '未命名应用' }}</div>
          </template>
          <template v-else-if="column.dataIndex === 'cover'">
            <div v-if="record.cover" class="cover-preview">
              <a-image :src="record.cover" :width="50" :height="50" style="border-radius: 4px;"/>
            </div>
            <div v-else class="no-cover">无封面</div>
          </template>
          <template v-else-if="column.dataIndex === 'initPrompt'">
            <div class="prompt-text">{{ record.initPrompt || '-' }}</div>
          </template>
          <template v-else-if="column.dataIndex === 'codeGenType'">
            <a-tag color="blue">{{ getCodeGenTypeLabel(record.codeGenType) }}</a-tag>
          </template>
          <template v-else-if="column.dataIndex === 'priority'">
            <a-tag v-if="record.priority === 99" color="gold">精选</a-tag>
            <span v-else>{{ record.priority || 0 }}</span>
          </template>
          <template v-else-if="column.dataIndex === 'userId'">
            {{ record.userId || '-' }}
          </template>
          <template v-else-if="column.dataIndex === 'user'">
            <div v-if="record.user">
              <div>账号：{{ record.user.userAccount }}</div>
              <div>姓名：{{ record.user.userName }}</div>
            </div>
            <div v-else>-</div>
          </template>
          <template v-else-if="column.dataIndex === 'deployedTime'">
            {{ record.deployedTime || '-' }}
          </template>
          <template v-else-if="column.dataIndex === 'createTime'">
            {{ dayjs(record.createTime).format('YYYY-MM-DD HH:mm:ss') }}
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space>
              <a-button type="link" size="small" @click="doEdit(record.id!)">编辑</a-button>
              <a-button type="link" size="small" @click="doFeatured(record.id!)" :disabled="record.priority === 99">
                精选
              </a-button>
              <a-button type="link" size="small" danger @click="doDelete(record.id!)">删除</a-button>
            </a-space>
          </template>
        </template>
      </a-table>
    </div>
  </div>

</template>

<script lang="ts" setup>
import { computed, onMounted, reactive, ref, h } from "vue";
import { useRouter } from "vue-router";
import { listAppByPageAdmin, deleteAppAdmin, updateAppAdmin } from "@/api/appController";
import { message, Modal } from "ant-design-vue";
import dayjs from "dayjs";
import { ExclamationCircleOutlined } from '@ant-design/icons-vue';
import { CODE_GEN_TYPE_OPTIONS, getCodeGenTypeLabel } from "@/constants/app";

const router = useRouter()

interface TableRecord {
  id?: string | number
  appName?: string
  cover?: string
  initPrompt?: string
  codeGenType?: string
  deployKey?: string
  deployedTime?: string
  priority?: number
  userId?: string | number
  editTime?: string
  createTime?: string
  updateTime?: string
  user?: API.UserVO
}

const columns = [
  {
    title: 'ID',
    dataIndex: 'id',
    width: 80,
    fixed: 'left',
  },
  {
    title: '应用名称',
    dataIndex: 'appName',
    width: 150,
  },
  {
    title: '封面',
    dataIndex: 'cover',
    width: 100,
  },
  {
    title: '初始提示词',
    dataIndex: 'initPrompt',
    width: 200,
    ellipsis: true,
  },
  {
    title: '生成类型',
    dataIndex: 'codeGenType',
    width: 120,
  },
  {
    title: '优先级',
    dataIndex: 'priority',
    width: 100,
  },
  {
    title: '用户ID',
    dataIndex: 'userId',
    width: 100,
  },
  {
    title: '用户信息',
    dataIndex: 'user',
    width: 150,
  },
  {
    title: '部署时间',
    dataIndex: 'deployedTime',
    width: 160,
  },
  {
    title: '创建时间',
    dataIndex: 'createTime',
    width: 180,
  },
  {
    title: '操作',
    dataIndex: 'action',
    key: 'action',
    width: 180,
    fixed: 'right',
  },
];

const data = ref<API.AppVO[]>([])
const loading = ref(false)
const total = ref(0)

// 搜索条件
const searchParams = reactive<API.AppQueryRequest>({
  pageNum: 1,
  pageSize: 10,
  appName: '',
  codeGenType: '',
  userId: undefined,
})

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

    const res = await listAppByPageAdmin(params)

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
    console.error('获取应用列表失败:', error)
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

// 编辑应用
const doEdit = (id: string | number) => {
  if (!id) {
    return
  }
  router.push(`/app/edit/${id}`)
}

// 设置为精选
const doFeatured = (id: string | number) => {
  if (!id) {
    return
  }
  Modal.confirm({
    title: '确认精选',
    icon: h(ExclamationCircleOutlined),
    content: '确定要将此应用设置为精选应用吗？精选应用会显示在首页。',
    onOk: async () => {
      try {
        const res = await updateAppAdmin({
          // @ts-ignore 避免大数字精度丢失
          id: id,
          priority: 99,
        })
        if (res.data.code === 0) {
          message.success('设置精选成功')
          // 刷新数据
          fetchData()
        } else {
          message.error('设置精选失败: ' + res.data.message)
        }
      } catch (error) {
        message.error('设置精选失败')
      }
    },
  })
}

// 删除应用
const doDelete = (id: string | number) => {
  if (!id) {
    return
  }
  Modal.confirm({
    title: '确认删除',
    icon: h(ExclamationCircleOutlined),
    content: '确定要删除此应用吗？删除后无法恢复。',
    okText: '确定',
    cancelText: '取消',
    okType: 'danger',
    onOk: async () => {
      try {
        // @ts-ignore 避免大数字精度丢失
        const res = await deleteAppAdmin({ id })
        if (res.data.code === 0) {
          message.success('删除成功')
          // 刷新数据
          fetchData()
        } else {
          message.error('删除失败: ' + res.data.message)
        }
      } catch (error) {
        message.error('删除失败')
      }
    },
  })
}

// 页面加载时请求
onMounted(() => {
  fetchData()
})
</script>

<style scoped>
#AppManagePage {
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

.app-name {
  font-weight: 600;
  color: var(--color-text-primary);
}

.cover-preview {
  display: flex;
  align-items: center;
}

.no-cover {
  color: var(--color-text-muted);
  font-size: 12px;
  padding: 8px 12px;
  background: var(--bg-surface);
  border-radius: 4px;
}

.prompt-text {
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
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
  #AppManagePage {
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
