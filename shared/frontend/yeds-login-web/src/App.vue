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
import { onMounted, reactive, ref } from 'vue'
import { YedsLoginPage } from '@yeds/ui'

const formRef = ref(null)
const loading = ref(false)
const errorMessage = ref('')
const redirectUri = ref('')

const form = reactive({
  username: 'admin',
  password: 'admin123'
})

const rules = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

function readRedirectUri() {
  const params = new URLSearchParams(window.location.search)
  const value = params.get('redirect_uri') || ''
  if (!value) {
    return import.meta.env.VITE_DEFAULT_REDIRECT_URI || ''
  }
  return value
}

function appendTokensToRedirect(targetUri, payload, username) {
  const target = new URL(targetUri)
  target.searchParams.set('access_token', payload.accessToken || '')
  target.searchParams.set('refresh_token', payload.refreshToken || '')
  target.searchParams.set('expires_in', String(payload.expiresIn ?? 0))
  target.searchParams.set('username', username)
  return target.toString()
}

async function submit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) {
    return
  }
  if (!redirectUri.value) {
    errorMessage.value = '缺少 redirect_uri，无法完成登录回跳'
    return
  }
  loading.value = true
  errorMessage.value = ''
  try {
    const response = await fetch('/api/iam/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        username: form.username.trim(),
        password: form.password
      })
    })
    const payload = await response.json().catch(() => ({}))
    if (!response.ok) {
      throw new Error(payload.message || `登录失败：${response.status}`)
    }
    const nextUrl = appendTokensToRedirect(redirectUri.value, payload, form.username.trim())
    window.location.replace(nextUrl)
  } catch (error) {
    errorMessage.value = error?.message || '登录失败'
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  redirectUri.value = readRedirectUri()
  if (!redirectUri.value) {
    errorMessage.value = '缺少 redirect_uri 参数'
  }
})
</script>
