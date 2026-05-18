<template>
  <div class="redirect-page">
    <el-result icon="info" title="正在跳转登录" sub-title="即将跳转到 IAM 统一登录页" />
  </div>
</template>

<script setup>
import { onMounted } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()

function resolveLoginUrl() {
  const configured =
    import.meta.env.VITE_YEDS_LOGIN_URL || new URL('/iam/login/', window.location.origin).toString()
  const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/abac/policy'
  const callbackUrl = new URL('/auth/sso-callback', window.location.origin)
  callbackUrl.searchParams.set('redirect', redirect)
  const loginUrl = new URL(configured, window.location.origin)
  loginUrl.searchParams.set('redirect_uri', callbackUrl.toString())
  return loginUrl.toString()
}

onMounted(() => {
  window.location.replace(resolveLoginUrl())
})
</script>

<style scoped>
.redirect-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>
