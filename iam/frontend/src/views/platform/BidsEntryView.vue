<template>
  <div class="page-stack">
    <el-card class="panel">
      <template #header>
        <div class="panel-title">
          <span>BIDS 平台入口</span>
          <el-tag type="success" effect="plain">BIDS</el-tag>
        </div>
      </template>

      <el-form label-width="96px" inline>
        <el-form-item label="环境">
          <el-select v-model="activeEnv" style="width: 180px">
            <el-option
              v-for="env in environments"
              :key="env.key"
              :label="env.label"
              :value="env.key"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="模块过滤">
          <el-input v-model="keyword" clearable placeholder="输入模块名称/描述" style="width: 260px" />
        </el-form-item>
      </el-form>

      <div class="module-grid">
        <el-card
          v-for="module in filteredModules"
          :key="module.key"
          class="module-card"
          shadow="hover"
        >
          <template #header>
            <div class="module-head">
              <span>{{ module.name }}</span>
              <el-tag size="small" effect="plain">{{ module.type }}</el-tag>
            </div>
          </template>

          <div class="module-desc">{{ module.description }}</div>
          <div class="module-url">{{ getModuleUrl(module) }}</div>

          <div class="action-wrap">
            <el-button type="primary" size="small" @click="openModule(module)">打开入口</el-button>
            <el-button size="small" @click="copyModuleUrl(module)">复制地址</el-button>
          </div>
        </el-card>
      </div>

      <el-empty v-if="filteredModules.length === 0" description="没有匹配的 BIDS 模块" />
    </el-card>

    <el-card class="panel">
      <template #header>
        <div class="panel-title">
          <span>最近访问</span>
          <el-tag type="info" effect="plain">HISTORY</el-tag>
        </div>
      </template>

      <el-table v-if="historyRows.length" :data="historyRows" border stripe size="small">
        <el-table-column prop="moduleName" label="模块" min-width="160" />
        <el-table-column prop="envLabel" label="环境" width="120" />
        <el-table-column prop="url" label="入口地址" min-width="300" show-overflow-tooltip />
        <el-table-column prop="visitedAt" label="访问时间" min-width="170" />
      </el-table>
      <el-empty v-else description="暂无访问记录" />
    </el-card>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { buildBidsSsoCallbackUrl, buildBidsLoginEntryUrl } from '../../platformSwitch'

const HISTORY_KEY = 'bids_entry_history_v1'
const keyword = ref('')
const activeEnv = ref('local')

const environments = [
  { key: 'local', label: '本地开发' },
  { key: 'test', label: '测试环境' },
  { key: 'prod', label: '生产环境' }
]

const moduleCatalog = [
  {
    key: 'bids-web',
    name: 'BIDS 前端',
    type: 'frontend',
    description: 'BIDS 统一前端入口（模型配置、执行与导出管理）',
    urls: {
      local: 'http://127.0.0.1:5173',
      test: 'https://bids-test.yeswater.com',
      prod: 'https://bids.yeswater.com'
    }
  },
  {
    key: 'bids-config',
    name: '配置中心',
    type: 'service',
    description: '数据源、模型与查询模板配置接口',
    urls: {
      local: 'http://127.0.0.1:8081',
      test: 'https://bids-config-test.yeswater.com',
      prod: 'https://bids-config.yeswater.com'
    }
  },
  {
    key: 'bids-exec',
    name: '执行中心',
    type: 'service',
    description: 'SQL 执行与鉴权检查服务',
    urls: {
      local: 'http://127.0.0.1:8082',
      test: 'https://bids-exec-test.yeswater.com',
      prod: 'https://bids-exec.yeswater.com'
    }
  },
  {
    key: 'bids-export',
    name: '导出中心',
    type: 'service',
    description: '报表导出、任务管理与文件分发服务',
    urls: {
      local: 'http://127.0.0.1:8083',
      test: 'https://bids-export-test.yeswater.com',
      prod: 'https://bids-export.yeswater.com'
    }
  }
]

const historyRows = ref(loadHistory())

const filteredModules = computed(() => {
  const text = keyword.value.trim().toLowerCase()
  if (!text) {
    return moduleCatalog
  }
  return moduleCatalog.filter((module) => {
    return (
      module.name.toLowerCase().includes(text) ||
      module.description.toLowerCase().includes(text) ||
      module.key.toLowerCase().includes(text)
    )
  })
})

function getModuleUrl(module) {
  return module.urls[activeEnv.value]
}

function getEnvLabel(envKey) {
  const target = environments.find((item) => item.key === envKey)
  return target ? target.label : envKey
}

function loadHistory() {
  try {
    const raw = localStorage.getItem(HISTORY_KEY)
    if (!raw) {
      return []
    }
    const parsed = JSON.parse(raw)
    return Array.isArray(parsed) ? parsed : []
  } catch {
    return []
  }
}

function saveHistory(rows) {
  localStorage.setItem(HISTORY_KEY, JSON.stringify(rows))
}

function recordVisit(module, url) {
  const record = {
    id: `${Date.now()}-${Math.random().toString(16).slice(2, 6)}`,
    moduleName: module.name,
    envLabel: getEnvLabel(activeEnv.value),
    url,
    visitedAt: new Date().toLocaleString('zh-CN', { hour12: false })
  }
  historyRows.value = [record, ...historyRows.value].slice(0, 20)
  saveHistory(historyRows.value)
}

function openModule(module) {
  const url = getModuleUrl(module)
  recordVisit(module, url)
  if (module.key === 'bids-web') {
    const target = buildBidsSsoCallbackUrl('/run/svc') || buildBidsLoginEntryUrl('/run/svc')
    window.location.assign(target)
    return
  }
  window.open(url, '_blank', 'noopener,noreferrer')
}

async function copyModuleUrl(module) {
  const url = getModuleUrl(module)
  try {
    await navigator.clipboard.writeText(url)
    ElMessage.success('入口地址已复制')
  } catch {
    ElMessage.error('复制失败，请手动复制')
  }
}
</script>
