<template>
  <el-card class="panel response-embed">
    <template #header>
      <div class="panel-title">
        <span>最近接口响应</span>
        <el-tag :type="responseTagType">{{ responseTagText }}</el-tag>
      </div>
    </template>

    <div class="metric-list">
      <div class="metric-item">
        <span class="metric-label">状态码</span>
        <span class="metric-value">{{ metrics.statusText }}</span>
      </div>
      <div class="metric-item">
        <span class="metric-label">耗时</span>
        <span class="metric-value">{{ metrics.elapsedText }}</span>
      </div>
      <div class="metric-item">
        <span class="metric-label">请求</span>
        <span class="metric-value">{{ metrics.pathText }}</span>
      </div>
      <div class="metric-item">
        <span class="metric-label">追踪ID</span>
        <span class="metric-value">{{ metrics.requestIdText }}</span>
      </div>
    </div>
    <pre class="result-pre">{{ responseText }}</pre>
  </el-card>
</template>

<script setup>
import { computed } from 'vue'
import { useUiStore } from '../stores/uiStore'

const uiStore = useUiStore()

const responseText = computed(() => JSON.stringify(uiStore.latestResponse || { message: '等待操作...' }, null, 2))

const responseTagText = computed(() => {
  if (!uiStore.latestResponse) {
    return 'IDLE'
  }
  return uiStore.latestResponse.ok ? 'SUCCESS' : 'FAIL'
})

const responseTagType = computed(() => {
  if (!uiStore.latestResponse) {
    return 'info'
  }
  return uiStore.latestResponse.ok ? 'success' : 'danger'
})

const metrics = computed(() => {
  const value = uiStore.latestResponse
  if (!value) {
    return {
      statusText: '-',
      elapsedText: '-',
      pathText: '-',
      requestIdText: '-'
    }
  }
  return {
    statusText: String(value.status || '-'),
    elapsedText: value.elapsedMs ? `${value.elapsedMs} ms` : '-',
    pathText: `${value.method || '-'} ${value.path || '-'}`,
    requestIdText: value.requestId || '-'
  }
})
</script>
