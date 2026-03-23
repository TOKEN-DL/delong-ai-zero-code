<template>
  <div id="userLoginPage">
    <div class="login-container">
      <div class="header">
        <h2 class="title">德龙AI 应用生成</h2>
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
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 20px;
}

.login-container {
  width: 100%;
  max-width: 400px;
  background: #fff;
  border-radius: 12px;
  padding: 40px 30px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
}

.header {
  text-align: center;
  margin-bottom: 32px;
}

.title {
  font-size: 24px;
  font-weight: 600;
  color: #333;
  margin-bottom: 12px;
}

.desc {
  font-size: 14px;
  color: #666;
  margin-bottom: 0;
}

.tips {
  text-align: center;
  color: #666;
  font-size: 14px;
  margin-bottom: 24px;
}

.link {
  color: #1890ff;
  text-decoration: none;
  font-weight: 500;
  cursor: pointer;
}

.link:hover {
  text-decoration: underline;
  color: #40a9ff;
}

.tips a {
  margin-left: 8px;
}

:deep(.ant-form-item) {
  margin-bottom: 20px;
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