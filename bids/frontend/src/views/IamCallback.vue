<template>
  <div class="callback-page">
    <el-result :icon="state.icon" :title="state.title" :sub-title="state.subTitle" />
  </div>
</template>

<script setup>
import { onMounted, reactive } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { writeSession } from '../authStorage.js'

const route = useRoute()
const router = useRouter()
const state = reactive({
  icon: 'info',
  title: '正在处理登录结果',
  subTitle: '请稍候...'
})

function cleanSensitiveQuery() {
  const cleanUrl = `${window.location.origin}/auth/iam/callback`
  window.history.replaceState({}, document.title, cleanUrl)
}

onMounted(() => {
  const accessToken = typeof route.query.access_token === 'string' ? route.query.access_token : ''
  const refreshToken = typeof route.query.refresh_token === 'string' ? route.query.refresh_token : ''
  const expiresInText = typeof route.query.expires_in === 'string' ? route.query.expires_in : '0'
  const username = typeof route.query.username === 'string' ? route.query.username : 'unknown'
  const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/run/svc'
  const expiresIn = Number.parseInt(expiresInText, 10)

  if (!accessToken || !refreshToken || !Number.isFinite(expiresIn) || expiresIn <= 0) {
    state.icon = 'error'
    state.title = '登录失败'
    state.subTitle = 'IAM 回调参数无效，请重新登录'
    setTimeout(() => router.replace('/login'), 1200)
    return
  }

  writeSession({
    accessToken,
    refreshToken,
    expiresAt: Date.now() + expiresIn * 1000,
    username
  })
  cleanSensitiveQuery()
  state.icon = 'success'
  state.title = '登录成功'
  state.subTitle = '正在返回业务系统...'
  setTimeout(() => {
    const target = redirect.startsWith('/') && !redirect.startsWith('//') ? redirect : '/run/svc'
    router.replace(target)
  }, 300)
})
</script>

<style scoped>
.callback-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>
