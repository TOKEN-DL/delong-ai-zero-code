<template>
  <div id="userRegisterPage">
    <div class="register-container">
      <div class="header">
        <h2 class="title">时零AI 应用生成</h2>
        <div class="desc">不写一行代码，生成完整应用</div>
      </div>

      <a-form class="form"
          :model="formState"
          name="basic"
          layout="vertical"
          autocomplete="off"
          @finish="handleSubmit"
      >
        <a-form-item
              label="账号"
              name="userAccount"
              :rules="[{ required: true, message: '请输入账号' }]"
        >
          <a-input v-model:value="formState.userAccount" placeholder="请输入账号" size="large"/>
        </a-form-item>

        <a-form-item
              label="密码"
              name="userPassword"
              :rules="[
                { required: true, message: '请输入密码' },
                {min: 8, message: '密码长度不能小于8位'}
              ]"
        >
          <a-input-password v-model:value="formState.userPassword" placeholder="请输入密码" size="large"/>
        </a-form-item>

        <a-form-item
              label="确认密码"
              name="checkPassword"
              :rules="[
                { required: true, message: '请确认密码' },
                {min: 8, message: '密码长度不能小于8位'}
              ]"
        >
          <a-input-password v-model:value="formState.checkPassword" placeholder="请确认密码" size="large"/>
        </a-form-item>

        <div class="tips">
          已有账号？
          <RouterLink to="/user/login" class="link">立即登录</RouterLink>
        </div>

        <a-form-item>
          <a-button type="primary" html-type="submit" size="large" style="width: 100%">注册</a-button>
        </a-form-item>
      </a-form>
    </div>
  </div>
</template>


<script lang="ts" setup>
import { reactive } from 'vue';
import {useRouter} from "vue-router";
import {userRegister} from "@/api/userController.ts";
import {message} from "ant-design-vue";


const router = useRouter();


const formState = reactive<API.UserRegisterRequest>({
  userAccount: '',
  userPassword: '',
  checkPassword: ''
});


/**
 * 提交表单
 * @param values
 */
const handleSubmit = async (values: any) => {
  // 校验密码和确认密码是否一致
  if (values.userPassword !== values.checkPassword) {
    message.error('两次输入的密码不一致');
    return;
  }

  const res = await userRegister(values);
  if (res.data.code === 0 && res.data.data) {
    message.success('注册成功')
    // 注册成功后跳转到登录页面
    router.push({
      path: '/user/login',
      replace: true,
    });
  } else {
    message.error(res.data.message);
  }
};


</script>

<style scoped>
#userRegisterPage {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--auth-bg);
  padding: 20px;
}

.register-container {
  width: 100%;
  max-width: 400px;
  background: var(--auth-card-bg);
  border-radius: 16px;
  padding: 40px 30px;
  box-shadow: var(--shadow-lg);
  border: 1px solid var(--color-border-light);
}

.header {
  text-align: center;
  margin-bottom: 32px;
}

.title {
  font-size: 24px;
  font-weight: 600;
  color: var(--color-text-primary);
  margin-bottom: 12px;
  letter-spacing: -0.01em;
}

.desc {
  font-size: 14px;
  color: var(--color-text-muted);
  margin-bottom: 0;
}

.tips {
  text-align: center;
  color: var(--color-text-muted);
  font-size: 14px;
  margin-bottom: 24px;
}

.link {
  color: var(--color-primary);
  text-decoration: none;
  font-weight: 500;
  cursor: pointer;
}

.link:hover {
  text-decoration: underline;
  color: var(--color-primary-hover);
}

.tips a {
  margin-left: 8px;
}

:deep(.ant-form-item) {
  margin-bottom: 20px;
}

:deep(.ant-input),
:deep(.ant-input-password .ant-input) {
  display: flex;
  align-items: center;
}

:deep(.ant-input-affix-wrapper) {
  display: flex;
  align-items: center;
}

:deep(.ant-btn-primary) {
  height: 44px;
  font-size: 16px;
  font-weight: 500;
}

@media (max-width: 576px) {
  .register-container {
    padding: 30px 20px;
  }

  .title {
    font-size: 20px;
  }
}
</style>