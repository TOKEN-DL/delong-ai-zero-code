<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { message } from 'ant-design-vue'
import { getAppById, getAppByIdAdmin, updateApp, updateAppAdmin } from '@/api/appController'
import { useLoginUserStore } from '@/stores/loginUser'

const router = useRouter()
const route = useRoute()
const loginUserStore = useLoginUserStore()

const appId = ref<string>(route.params.appId as string)
const loading = ref(false)
const submitLoading = ref(false)

// 表单数据
const formData = ref<API.AppUpdateRequest | API.AppUpdateByAdminRequest>({
  // @ts-ignore 避免大数字精度丢失
  id: appId.value,
  appName: '',
})

// 是否为管理员
const isAdmin = computed(() => loginUserStore.loginUser.userRole === 'admin')

// 获取应用信息
const fetchAppInfo = async () => {
  loading.value = true
  try {
    // 管理员使用管理员接口，普通用户使用普通接口
    let res
    if (isAdmin.value) {
      // @ts-ignore 避免大数字精度丢失
      res = await getAppByIdAdmin({ id: appId.value })
    } else {
      // @ts-ignore 避免大数字精度丢失
      res = await getAppById({ id: appId.value })
    }

    if (res.data.code === 0 && res.data.data) {
      const app = res.data.data
      // 检查权限：普通用户只能编辑自己的应用
      if (!isAdmin.value && app.userId !== loginUserStore.loginUser.id) {
        message.error('无权编辑此应用')
        router.back()
        return
      }

      formData.value = {
        id: app.id,
        appName: app.appName || '',
      }

      // 如果是管理员，添加额外字段
      if (isAdmin.value) {
        const adminForm = formData.value as API.AppUpdateByAdminRequest
        adminForm.cover = app.cover || ''
        adminForm.priority = app.priority || 0
      }
    } else {
      message.error('获取应用信息失败')
      router.back()
    }
  } catch (error) {
    message.error('获取应用信息失败')
    router.back()
  } finally {
    loading.value = false
  }
}

// 提交表单
const handleSubmit = async () => {
  if (!formData.value.appName?.trim()) {
    message.warning('请输入应用名称')
    return
  }

  submitLoading.value = true
  try {
    let res
    if (isAdmin.value) {
      // 管理员调用管理员接口
      const adminForm = formData.value as API.AppUpdateByAdminRequest
      res = await updateAppAdmin(adminForm)
    } else {
      // 普通用户调用普通接口
      res = await updateApp(formData.value as API.AppUpdateRequest)
    }

    if (res.data.code === 0) {
      message.success('修改成功')
      // 返回上一页
      router.back()
    } else {
      message.error(res.data.message || '修改失败')
    }
  } catch (error) {
    message.error('修改失败')
  } finally {
    submitLoading.value = false
  }
}

// 取消编辑
const handleCancel = () => {
  router.back()
}

// 页面加载时获取应用信息
onMounted(() => {
  fetchAppInfo()
})
</script>

<template>
  <div class="app-edit-page">
    <div class="container">
      <div class="header">
        <h2>编辑应用</h2>
        <a-button @click="handleCancel">返回</a-button>
      </div>

      <a-spin :spinning="loading">
        <a-form
          :model="formData"
          layout="vertical"
          @finish="handleSubmit"
          class="edit-form"
        >
          <a-form-item
            label="应用名称"
            name="appName"
            :rules="[{ required: true, message: '请输入应用名称' }]"
          >
            <a-input
              v-model:value="formData.appName"
              placeholder="请输入应用名称"
              :maxlength="50"
              show-count
            />
          </a-form-item>

          <!-- 管理员专属字段 -->
          <template v-if="isAdmin">
            <a-form-item label="应用封面" name="cover">
              <a-input
                v-model:value="(formData as API.AppUpdateByAdminRequest).cover"
                placeholder="请输入封面URL"
                :maxlength="500"
              />
            </a-form-item>

            <a-form-item label="优先级" name="priority">
              <a-input-number
                v-model:value="(formData as API.AppUpdateByAdminRequest).priority"
                placeholder="请输入优先级"
                :min="0"
                :max="99"
                style="width: 100%"
              />
              <template #extra>
                <span>设置为 99 将在首页精选应用中展示</span>
              </template>
            </a-form-item>
          </template>

          <a-form-item>
            <a-space>
              <a-button type="primary" html-type="submit" :loading="submitLoading">
                保存
              </a-button>
              <a-button @click="handleCancel">取消</a-button>
            </a-space>
          </a-form-item>
        </a-form>
      </a-spin>
    </div>
  </div>
</template>

<style scoped>
.app-edit-page {
  min-height: 100vh;
  background: #f5f5f5;
  padding: 24px;
}

.container {
  max-width: 800px;
  margin: 0 auto;
  background: #fff;
  border-radius: 8px;
  padding: 32px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 32px;
  padding-bottom: 16px;
  border-bottom: 1px solid #e8e8e8;
}

.header h2 {
  margin: 0;
  font-size: 24px;
  font-weight: 600;
  color: #333;
}

.edit-form {
  max-width: 500px;
}

.edit-form :deep(.ant-form-item-label > label) {
  font-weight: 500;
  color: #333;
}

:deep(.ant-btn-primary) {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
}

:deep(.ant-btn-primary:hover) {
  background: linear-gradient(135deg, #764ba2 0%, #667eea 100%);
}

@media (max-width: 768px) {
  .app-edit-page {
    padding: 16px;
  }

  .container {
    padding: 20px;
  }

  .header {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }

  .header h2 {
    font-size: 20px;
  }

  .edit-form {
    max-width: 100%;
  }
}
</style>
