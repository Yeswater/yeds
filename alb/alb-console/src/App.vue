<template>
  <div class="alb-console-shell">
    <header class="alb-console-header">
      <h1>YEDS ALB 路由控制台</h1>
      <el-button type="primary" :loading="publishing" @click="onPublish">发布配置</el-button>
    </header>
    <router-view />
  </div>
</template>

<script setup>
import { ref, provide } from 'vue'
import { ElMessage } from 'element-plus'
import { publishRoutes } from './api/albApi.js'

const publishing = ref(false)

async function onPublish() {
  publishing.value = true
  try {
    const result = await publishRoutes()
    ElMessage.success(`发布成功：版本 ${result.version}，路由 ${result.routeCount} 条`)
    window.dispatchEvent(new CustomEvent('alb-published'))
  } catch (error) {
    ElMessage.error(error.message || '发布失败')
  } finally {
    publishing.value = false
  }
}

provide('albPublish', onPublish)
</script>
