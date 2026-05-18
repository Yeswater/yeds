<template>
  <div class="yeds-login-page">
    <div class="yeds-login-card">
      <h1 class="yeds-login-title">{{ title }}</h1>
      <p v-if="subtitle" class="yeds-login-subtitle">{{ subtitle }}</p>
      <slot />
    </div>
  </div>
</template>

<script setup>
defineProps({
  title: {
    type: String,
    default: 'IAM 统一登录'
  },
  subtitle: {
    type: String,
    default: ''
  }
})
</script>
