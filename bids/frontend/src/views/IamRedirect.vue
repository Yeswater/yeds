<template>
  <div class="redirect-page">
    <el-result icon="info" title="正在跳转登录" sub-title="即将跳转到 IAM 统一登录页面" />
  </div>
</template>

<script setup>
import { onMounted } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()

function resolveIamLoginUrl() {
  const configured =
    import.meta.env.VITE_YEDS_LOGIN_URL ||
    import.meta.env.VITE_IAM_LOGIN_URL ||
    new URL('/iam/login/', window.location.origin).toString()
  const callbackUrl = new URL('/auth/iam/callback', window.location.origin)
  const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/run/svc'
  callbackUrl.searchParams.set('redirect', redirect)
  const loginUrl = new URL(configured, window.location.origin)
  loginUrl.searchParams.set('redirect_uri', callbackUrl.toString())
  return loginUrl.toString()
}

onMounted(() => {
  window.location.replace(resolveIamLoginUrl())
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
