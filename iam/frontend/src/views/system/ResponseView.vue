<template>
  <el-card class="panel yeds-panel">
    <template #header>
      <div class="panel-title">
        <span>接口响应</span>
        <el-tag :type="responseTagType">{{ responseTagText }}</el-tag>
      </div>
    </template>

    <div class="metric-list">
      <div class="metric-item">
        <span class="metric-label">状态码</span>
        <span class="metric-value">{{ metrics.statusText }}</span>
      </div>
      <div class="metric-item">
        <span class="metric-label">接口耗时</span>
        <span class="metric-value">{{ metrics.elapsedText }}</span>
      </div>
      <div class="metric-item">
        <span class="metric-label">数据项数量</span>
        <span class="metric-value">{{ metrics.itemCountText }}</span>
      </div>
      <div class="metric-item">
        <span class="metric-label">结果状态</span>
        <span class="metric-value">{{ metrics.emptyText }}</span>
      </div>
      <div class="metric-item">
        <span class="metric-label">请求地址</span>
        <span class="metric-value">{{ metrics.pathText }}</span>
      </div>
      <div class="metric-item">
        <span class="metric-label">请求追踪ID</span>
        <span class="metric-value">{{ metrics.requestIdText }}</span>
      </div>
    </div>

    <pre class="result-pre">{{ responseText }}</pre>
  </el-card>
</template>

<script setup>
import { computed } from 'vue'
import { useUiStore } from '../../stores/uiStore'

const uiStore = useUiStore()

const responseText = computed(() =>
  JSON.stringify(uiStore.latestResponse || { message: '等待操作...' }, null, 2)
)

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

function inferItemCount(payload) {
  if (Array.isArray(payload)) {
    return payload.length
  }
  if (!payload || typeof payload !== 'object') {
    return 0
  }
  return Object.keys(payload).length
}

const metrics = computed(() => {
  const val = uiStore.latestResponse
  if (!val) {
    return {
      statusText: '-',
      elapsedText: '-',
      itemCountText: '-',
      emptyText: '-',
      pathText: '-',
      requestIdText: '-'
    }
  }
  const itemCount = inferItemCount(val.payload)
  return {
    statusText: val.status || '-',
    elapsedText: val.elapsedMs ? `${val.elapsedMs} ms` : '-',
    itemCountText: String(itemCount),
    emptyText: itemCount === 0 ? '空结果' : '有数据',
    pathText: `${val.method || '-'} ${val.path || '-'}`,
    requestIdText: val.requestId || '-'
  }
})
</script>
