<template>
  <div id="userLoginPage">
    <div class="login-container">
      <div class="header">
        <h2 class="title">时零AI 应用生成</h2>
        <div class="desc">不写一行代码，生成完整应用</div>
      </div>

      <a-form
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

        <div class="tips">
          还没有账号？
          <RouterLink to="/user/register" class="link">立即注册</RouterLink>
        </div>

        <a-form-item>
          <a-button type="primary" html-type="submit" size="large" style="width: 100%">登录</a-button>
        </a-form-item>
      </a-form>
    </div>
  </div>
</template>


<script lang="ts" setup>
import { reactive } from 'vue';
import {useLoginUserStore} from "@/stores/loginUser.ts";
import {useRouter} from "vue-router";
import {userLogin} from "@/api/userController.ts";
import {message} from "ant-design-vue";


const loginUserStore = useLoginUserStore();
const router = useRouter();


interface FormState {
  username: string;
  password: string;
  remember: boolean;
}

const formState = reactive<API.UserLoginRequest>({
  userAccount: '',
  userPassword: ''
});


/**
 * 提交表单
 * @param values
 */
const handleSubmit = async (values: any) => {
  const res = await userLogin(values);
  if (res.data.code === 0 && res.data.data) {
    await loginUserStore.fetchLoginUser()
    message.success('登录成功')
    router.push({
      path: '/',
      replace: true,
    });
  } else {
    message.error(res.data.message);
  }
};


</script>

<style scoped>
#userLoginPage {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--auth-bg);
  padding: 20px;
}

.login-container {
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
  .login-container {
    padding: 30px 20px;
  }

  .title {
    font-size: 20px;
  }
}
</style>