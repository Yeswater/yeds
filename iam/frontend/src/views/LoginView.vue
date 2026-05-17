<template>
  <div class="login-page">
    <el-card class="login-card">
      <template #header>
        <div class="panel-title">
          <span>IAM 登录</span>
          <el-tag type="primary" effect="plain">AUTH</el-tag>
        </div>
      </template>

      <el-form ref="formRef" :model="form" :rules="rules" label-width="84px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" autocomplete="username" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" show-password autocomplete="current-password" />
        </el-form-item>
        <el-form-item label="OTP">
          <el-input v-model="form.otpCode" placeholder="可选：MFA 验证码" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="submit">登录</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { loginByPassword } from '../services/auth'

const route = useRoute()
const router = useRouter()
const formRef = ref(null)
const loading = ref(false)

const form = reactive({
  username: 'admin',
  password: 'admin123',
  otpCode: ''
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function submit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) {
    return
  }
  loading.value = true
  const result = await loginByPassword(form)
  loading.value = false
  if (!result.ok) {
    ElMessage.error(`登录失败：${result.payload?.message || result.status}`)
    return
  }
  const redirectPath = typeof route.query.redirect === 'string' ? route.query.redirect : '/abac/policy'
  router.replace(redirectPath)
}
</script>
