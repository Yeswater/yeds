<template>
  <el-card class="panel yeds-panel">
    <template #header>
      <div class="panel-title">
        <span>最近操作时间线</span>
        <el-tag type="info" effect="plain">TIMELINE</el-tag>
      </div>
    </template>

    <div class="action-wrap">
      <el-button size="small" @click="exportDiagnostics">导出诊断信息</el-button>
      <el-button size="small" type="danger" plain @click="resetHistory">清空历史</el-button>
    </div>

    <el-timeline v-if="pagedItems.length">
      <el-timeline-item
        v-for="item in pagedItems"
        :key="item.id"
        :timestamp="`${formatDateTime(item.at)} · 状态 ${item.status} · ${item.elapsedMs}ms · ${item.requestId}`"
        :type="item.ok ? 'success' : 'danger'"
      >
        {{ item.action }}（{{ item.method }} {{ item.path }}）
      </el-timeline-item>
    </el-timeline>
    <el-empty v-else description="暂无操作记录" />

    <YedsListPagination
      :total="uiStore.history.length"
      :page-size="pageSize"
      :current-page="currentPage"
      @update:current-page="onPageChange"
    />
  </el-card>
</template>

<script setup>
import { computed, ref } from 'vue'
import { ElMessageBox } from 'element-plus'
import { YedsListPagination, formatDateTime } from '@yeds/ui'
import { clearHistory, exportDiagnosticsJson, useUiStore } from '../../stores/uiStore'

const uiStore = useUiStore()
const currentPage = ref(1)
const pageSize = 20

const pagedItems = computed(() => {
  const start = (currentPage.value - 1) * pageSize
  return uiStore.history.slice(start, start + pageSize)
})

function onPageChange(page) {
  currentPage.value = page
}

function exportDiagnostics() {
  const blob = new Blob([exportDiagnosticsJson()], { type: 'application/json;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `iam-diagnostics-${Date.now()}.json`
  link.click()
  URL.revokeObjectURL(url)
}

async function resetHistory() {
  await ElMessageBox.confirm('确认清空所有操作历史吗？', '提示', { type: 'warning' })
  clearHistory()
  currentPage.value = 1
}
</script>
