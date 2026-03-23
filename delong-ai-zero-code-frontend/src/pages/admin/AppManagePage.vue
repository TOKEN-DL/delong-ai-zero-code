<template>
  <div id="AppManagePage">
    <div class="container">
      <div class="search-section">
        <a-form layout="inline" :model="searchParams" @finish="doSearch">
          <a-form-item label="应用名称">
            <a-input v-model:value="searchParams.appName" placeholder="输入应用名称"/>
          </a-form-item>
          <a-form-item label="生成类型">
            <a-input v-model:value="searchParams.codeGenType" placeholder="输入生成类型"/>
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
            <a-tag color="blue">{{ record.codeGenType || '-' }}</a-tag>
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
    const res = await listAppByPageAdmin({
      ...searchParams,
    })
    if (res.data.data) {
      data.value = res.data.data.records ?? []
      total.value = res.data.data.totalRow ?? 0
    } else {
      message.error('获取数据失败: ' + res.data.message)
    }
  } catch (error) {
    message.error('获取数据失败')
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
  background: #f5f5f5;
}

.container {
  max-width: 1800px;
  margin: 0 auto;
  background: #fff;
  border-radius: 8px;
  padding: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.search-section {
  background: #fafafa;
  padding: 20px;
  border-radius: 6px;
  margin-bottom: 24px;
}

.app-name {
  font-weight: 600;
  color: #333;
}

.cover-preview {
  display: flex;
  align-items: center;
}

.no-cover {
  color: #999;
  font-size: 12px;
  padding: 8px 12px;
  background: #f5f5f5;
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
  background: #fafafa;
  font-weight: 600;
}

:deep(.ant-btn-primary) {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
}

:deep(.ant-btn-primary:hover) {
  background: linear-gradient(135deg, #764ba2 0%, #667eea 100%);
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
