<template>
  <YedsLoginPage title="IAM 统一登录" subtitle="BIDS 数据服务">
    <el-form class="yeds-login-form" :model="form" label-position="top" @submit.prevent="onSubmit">
      <el-form-item label="账号">
        <el-input v-model="form.username" autocomplete="username" />
      </el-form-item>
      <el-form-item label="密码">
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
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { YedsLoginPage } from '@yeds/ui'
import { listDataSources } from '../api'
import { writeSession } from '../authStorage.js'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const errorMessage = ref('')
const form = reactive({
  username: 'admin',
  password: 'admin'
})

async function onSubmit() {
  if (!form.username || !form.password) {
    errorMessage.value = '请输入账号和密码'
    return
  }
  loading.value = true
  errorMessage.value = ''
  try {
    await listDataSources({ username: form.username, password: form.password })
    writeSession({ username: form.username, password: form.password })
    const raw = typeof route.query.redirect === 'string' ? route.query.redirect : '/run/svc'
    const redirect = raw.startsWith('/') && !raw.startsWith('//') ? raw : '/run/svc'
    router.replace(redirect)
  } catch (e) {
    errorMessage.value = e.message || '登录失败'
  } finally {
    loading.value = false
  }
}
</script>
