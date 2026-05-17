<template>
  <div class="callback-page">
    <el-result :icon="state.icon" :title="state.title" :sub-title="state.subTitle" />
  </div>
</template>

<script setup>
import { onMounted, reactive } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { setSession } from '../stores/authStore'

const route = useRoute()
const router = useRouter()
const state = reactive({
  icon: 'info',
  title: '正在接入平台会话',
  subTitle: '请稍候...'
})

function cleanSensitiveQuery() {
  window.history.replaceState({}, document.title, `${window.location.origin}/auth/sso-callback`)
}

onMounted(() => {
  const accessToken = typeof route.query.access_token === 'string' ? route.query.access_token : ''
  const refreshToken = typeof route.query.refresh_token === 'string' ? route.query.refresh_token : ''
  const expiresInText = typeof route.query.expires_in === 'string' ? route.query.expires_in : '0'
  const username = typeof route.query.username === 'string' ? route.query.username : 'unknown'
  const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/abac/policy'
  const expiresIn = Number.parseInt(expiresInText, 10)

  if (!accessToken || !refreshToken || !Number.isFinite(expiresIn) || expiresIn <= 0) {
    state.icon = 'error'
    state.title = '会话接入失败'
    state.subTitle = '缺少有效令牌，请重新登录'
    setTimeout(() => router.replace({ path: '/login', query: { redirect } }), 1200)
    return
  }

  setSession(
    {
      accessToken,
      refreshToken,
      expiresIn,
      tokenType: 'Bearer'
    },
    username
  )
  cleanSensitiveQuery()
  state.icon = 'success'
  state.title = '会话接入成功'
  state.subTitle = '正在进入 IAM 控制台...'
  setTimeout(() => {
    const target = redirect.startsWith('/') && !redirect.startsWith('//') ? redirect : '/abac/policy'
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
