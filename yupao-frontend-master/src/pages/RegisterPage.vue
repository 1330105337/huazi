<template>
  <van-form @submit="onSubmit">
    <van-cell-group inset>
      <van-field
          v-model="username"
          name="用户名"
          label="用户名"
          placeholder="用户名"
          :rules="[{ required: true, message: '请填写用户名' }]"
      />
      <van-field
          v-model="password"
          type="password"
          name="密码"
          label="密码"
          placeholder="密码"
          :rules="[{ required: true, message: '请填写密码' }]"
      />
      <van-field
          v-model="email"
          type="邮箱"
          name="邮箱"
          label="邮箱"
          placeholder="邮箱"
          :rules="[{ required: true, message: '请填写邮箱' }]"

      />
      <van-field
          v-model="code"
          type="验证码"
          name="验证码"
          label="验证码"
          placeholder="验证码"
          :rules="[{ required: true, message: '请填写验证码' }]"

      />

      <van-field
          v-model="checkPassword"
          type="password"
          name="确认密码"
          label="确认密码"
          placeholder="确认密码"
          :rules="[{ required: true, message: '请确认密码' }]"
      />

      <van-field
          v-model="planetCode"
          name="用户编号"
          label="用户编号"
          placeholder="用户编号"
          :rules="[{ required: true, message: '请输入编号' }]"
      />

    </van-cell-group>
    <div style="margin: 16px;">
      <van-button round block type="primary" native-type="submit">
        提交
      </van-button>
    </div>
  </van-form>
</template>

<script setup  lang="ts">
import {ref} from "vue";
import {useRoute, useRouter} from "vue-router";
import myAxios from "../plugins/myAxios";
const router = useRouter()
const username = ref('');
const password = ref('');
import {Toast} from "vant";
const  checkPassword= ref('');
const userCode= ref('');
const  planetCode= ref('');
const onSubmit = async () => {
  const res = await myAxios.post('/user/register', {
    userAccount: username.value,
    userPassword: password.value,
    checkPassword: checkPassword.value,
    planetCode: planetCode.value,
    code: userCode.value
  })
  if (res.code === 0 && res.data) {
    Toast.success('登录成功');
    // 跳转到之前的页面
    window.location.href = `/user/login`;
  } else {
    Toast.fail('注册失败');
  }
}
</script>
<style scoped>

</style>
