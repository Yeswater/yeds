<template>
  <div class="login-page">
    <div class="login-card">
      <h1 class="title">BIDS</h1>
      <p class="subtitle">管理系统</p>
      <el-form :model="form" label-position="top" @submit.prevent="onSubmit">
        <el-form-item label="账号">
          <el-input v-model="form.username" autocomplete="username" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" show-password autocomplete="current-password" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" class="submit" native-type="submit" :loading="loading" style="width: 100%">
            登 录
          </el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { listDataSources } from '../api'
import { writeSession } from '../authStorage.js'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const form = reactive({
  username: 'admin',
  password: 'admin'
})

async function onSubmit() {
  if (!form.username || !form.password) {
    ElMessage.warning('请输入账号和密码')
    return
  }
  loading.value = true
  try {
    await listDataSources({ username: form.username, password: form.password })
    writeSession({ username: form.username, password: form.password })
    const raw = typeof route.query.redirect === 'string' ? route.query.redirect : '/run/svc'
    const redirect = raw.startsWith('/') && !raw.startsWith('//') ? raw : '/run/svc'
    router.replace(redirect)
  } catch (e) {
    ElMessage.error(e.message || '登录失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(160deg, var(--el-fill-color-darker) 0%, var(--el-bg-color-page) 45%);
}
.login-card {
  width: 400px;
  max-width: 92vw;
  padding: 40px 36px 32px;
  border-radius: 12px;
  background: var(--el-bg-color);
  box-shadow: var(--el-box-shadow);
}
.title {
  margin: 0;
  text-align: center;
  font-size: 28px;
  font-weight: 700;
  letter-spacing: 0.12em;
  color: var(--el-color-primary);
}
.subtitle {
  margin: 8px 0 28px;
  text-align: center;
  font-size: 14px;
  color: var(--el-text-color-secondary);
}
</style>
