<template>
  <YedsLoginPage title="IAM 统一登录">
    <el-form
      ref="formRef"
      class="yeds-login-form"
      :model="form"
      :rules="rules"
      label-position="top"
      @submit.prevent="submit"
    >
      <el-form-item label="账号" prop="username">
        <el-input v-model="form.username" autocomplete="username" />
      </el-form-item>
      <el-form-item label="密码" prop="password">
        <el-input
          v-model="form.password"
          type="password"
          show-password
          autocomplete="current-password"
        />
      </el-form-item>
      <el-form-item v-if="showOtp" label="OTP">
        <el-input v-model="form.otpCode" placeholder="可选：MFA 验证码" />
      </el-form-item>
      <el-form-item>
        <el-button class="yeds-login-submit" type="primary" native-type="submit" :loading="loading">
          登录
        </el-button>
      </el-form-item>
      <div v-if="errorMessage" class="yeds-login-error">{{ errorMessage }}</div>
    </el-form>
  </YedsLoginPage>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { YedsLoginPage } from '@yeds/ui'
import { loginByPassword } from '../services/auth'

const route = useRoute()
const router = useRouter()
const formRef = ref(null)
const loading = ref(false)
const errorMessage = ref('')
const showOtp = false

const form = reactive({
  username: 'admin',
  password: 'admin123',
  otpCode: ''
})

const rules = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function submit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) {
    return
  }
  loading.value = true
  errorMessage.value = ''
  const result = await loginByPassword(form)
  loading.value = false
  if (!result.ok) {
    errorMessage.value = result.payload?.message || `登录失败：${result.status}`
    return
  }
  const redirectPath = typeof route.query.redirect === 'string' ? route.query.redirect : '/abac/policy'
  router.replace(redirectPath)
}
</script>
