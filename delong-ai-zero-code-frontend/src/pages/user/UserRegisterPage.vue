<template>
  <div id="userRegisterPage">
    <div class="register-container">
      <div class="header">
        <h2 class="title">德龙AI 应用生成</h2>
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
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 20px;
}

.register-container {
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
  .register-container {
    padding: 30px 20px;
  }

  .title {
    font-size: 20px;
  }
}
</style>