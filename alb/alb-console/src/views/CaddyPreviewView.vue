<template>
  <YedsListPageCard title="Nginx 配置预览" tag="ALB">
    <YedsListToolbar>
      <el-button @click="load">刷新</el-button>
      <el-button @click="$router.push('/routes')">返回路由</el-button>
    </YedsListToolbar>
    <pre class="caddy-preview">{{ content }}</pre>
  </YedsListPageCard>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { YedsListPageCard, YedsListToolbar } from '@yeds/ui'
import { previewNginx } from '../api/albApi.js'

const content = ref('')

async function load() {
  try {
    const result = await previewNginx()
    content.value = result.content || ''
  } catch (error) {
    ElMessage.error(error.message)
  }
}

onMounted(load)
</script>

<style scoped>
.caddy-preview {
  margin: 0;
  padding: 16px;
  background: #1e1e1e;
  color: #d4d4d4;
  border-radius: 8px;
  font-size: 13px;
  line-height: 1.5;
  overflow: auto;
  max-height: 70vh;
}
</style>
