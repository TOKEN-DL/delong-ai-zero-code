<template>
  <div id="UserManagePage">
    <div class="container">
      <div class="search-section">
        <a-form layout="inline" :model="searchParams" @finish="doSearch">
          <a-form-item label="账号">
            <a-input v-model:value="searchParams.userAccount" placeholder="输入账号"/>
          </a-form-item>
          <a-form-item label="用户名">
            <a-input v-model:value="searchParams.userName" placeholder="输入用户名"/>
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
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'userAvatar'">
            <a-image :src="record.userAvatar" :width="60" :height="60" style="border-radius: 50%;"/>
          </template>
          <template v-else-if="column.dataIndex === 'userRole'">
            <div v-if="record.userRole === 'admin'">
              <a-tag color="green">管理员</a-tag>
            </div>
            <div v-else>
              <a-tag color="blue">普通用户</a-tag>
            </div>
          </template>
          <template v-else-if="column.dataIndex === 'createTime'">
            {{dayjs(record.createTime).format('YYYY-MM-DD HH:mm:ss')}}
          </template>
          <template v-else-if="column.key === 'action'">
            <a-button danger @click="doDelete(record.id)">删除</a-button>
          </template>
        </template>
      </a-table>
    </div>
  </div>

</template>

<script lang="ts" setup>
import { SmileOutlined, DownOutlined } from '@ant-design/icons-vue';
import {computed, onMounted, reactive, ref} from "vue";
import { deleteUser, listUserByPage } from "@/api/userController";
import {message} from "ant-design-vue";
import dayjs from "dayjs";

interface TableRecord {
  id?: string | number
  userAccount?: string
  userName?: string
  userAvatar?: string
  userProfile?: string
  userRole?: string
  createTime?: string
}

const columns = [
  {
    title: 'id',
    dataIndex: 'id',
    width: 80,
  },
  {
    title: '账号',
    dataIndex: 'userAccount',
    width: 150,
  },
  {
    title: '用户名',
    dataIndex: 'userName',
    width: 150,
  },
  {
    title: '头像',
    dataIndex: 'userAvatar',
    width: 100,
  },
  {
    title: '用户简介',
    dataIndex: 'userProfile',
    ellipsis: true,
  },
  {
    title: '用户角色',
    dataIndex: 'userRole',
    width: 120,
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
    width: 100,
  },
];

const data = ref<API.UserVO[]>([])

const total =ref(0)

// 搜索条件
const searchParams = reactive<API.UserQueryRequest>({
  pageNum: 1,
  pageSize: 10,

})


//调用后端API获取数据
const fetchData = async () => {
  const res = await listUserByPage({
    ...searchParams,
  })
  if (res.data.data){
    data.value = res.data.data.records ?? []
    total.value = res.data.data.totalRow ?? 0
  }else {
    message.error('获取数据失败' + res.data.message)
  }
}


// 分页参数
const pagination = computed( () => {
  return {
    current: searchParams.pageNum,
    pageSize: searchParams.pageSize,
    total: total.value,
    showSizeChanger: true,
    showTotal: (total :number) => `共${total}条`,
  }
})

//表格变化时的操作
const doTableChange = (page: any) => {
  searchParams.pageNum = page.current
  searchParams.pageSize = page.pageSize
  fetchData()
}


const doSearch = () => {
  //重置页码
  searchParams.pageNum = 1
  fetchData()
}

// 删除数据
const doDelete = async (id: string | number) => {
  if (!id) {
    return
  }
  // @ts-ignore 避免大数字精度丢失
  const res = await deleteUser({ id })
  if (res.data.code === 0) {
    message.success('删除成功')
    //刷新数据
    fetchData()

  } else {
    message.error('删除失败')
  }
}

//页面加载时请求
onMounted( () => {
  fetchData()
} )
</script>

<style scoped>
#UserManagePage {
  min-height: 100vh;
  padding: 24px;
  background: var(--bg-surface);
}

.container {
  max-width: 1400px;
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

:deep(.ant-table) {
  border-radius: 6px;
  overflow: hidden;
}

:deep(.ant-table-thead > tr > th) {
  background: var(--bg-surface) !important;
  font-weight: 600;
}

@media (max-width: 768px) {
  #UserManagePage {
    padding: 16px;
  }

  .container {
    padding: 16px;
  }

  .search-section {
    padding: 16px;
  }
}
</style>
