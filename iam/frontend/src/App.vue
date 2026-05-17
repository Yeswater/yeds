<template>
  <el-container class="admin-root">
    <el-header class="admin-header">
      <div class="brand">IAM</div>
      <div class="header-fill"></div>
      <div class="header-meta">
        <el-tag type="info" effect="plain">阶段三治理控制台</el-tag>
        <span class="time-text">{{ nowText }}</span>
      </div>
    </el-header>

    <el-container class="admin-body">
      <el-aside width="220px" class="admin-aside">
        <el-menu :default-active="activeMenu" class="side-menu" @select="onSelectMenu">
          <el-menu-item index="abac-policy">ABAC 策略管理</el-menu-item>
          <el-menu-item index="abac-check">ABAC 鉴权校验</el-menu-item>
          <el-menu-item index="federation">租户联邦映射</el-menu-item>
          <el-menu-item index="governance">风险与巡检</el-menu-item>
          <el-menu-item index="response">接口响应</el-menu-item>
          <el-menu-item index="timeline">最近操作时间线</el-menu-item>
        </el-menu>
      </el-aside>

      <el-main class="admin-main">
        <div class="content-grid">
          <el-card v-show="activeMenu === 'abac-policy' || activeMenu === 'all'" class="panel">
            <template #header>
              <div class="panel-title">
                <span>ABAC 策略管理</span>
                <el-tag type="success" effect="plain">POLICY</el-tag>
              </div>
            </template>
            <el-form label-width="96px" :model="abacPolicyForm">
              <el-form-item label="策略名称">
                <el-input v-model="abacPolicyForm.policyName" />
              </el-form-item>
              <el-form-item label="资源/动作">
                <div class="inline-two">
                  <el-input v-model="abacPolicyForm.resource" placeholder="bids:model" />
                  <el-input v-model="abacPolicyForm.action" placeholder="execute" />
                </div>
              </el-form-item>
              <el-form-item label="表达式">
                <el-input v-model="abacPolicyForm.expression" />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" :loading="loading.createAbac" @click="createAbacPolicy">
                  创建 ABAC 策略
                </el-button>
              </el-form-item>
            </el-form>
          </el-card>

          <el-card v-show="activeMenu === 'abac-check' || activeMenu === 'all'" class="panel">
            <template #header>
              <div class="panel-title">
                <span>ABAC 鉴权校验</span>
                <el-tag type="warning" effect="plain">VERIFY</el-tag>
              </div>
            </template>
            <el-form label-width="92px" :model="abacCheckForm">
              <el-form-item label="用户ID/租户">
                <div class="inline-two">
                  <el-input v-model="abacCheckForm.userId" />
                  <el-input v-model="abacCheckForm.tenantCode" />
                </div>
              </el-form-item>
              <el-form-item label="资源/动作">
                <div class="inline-two">
                  <el-input v-model="abacCheckForm.resource" />
                  <el-input v-model="abacCheckForm.action" />
                </div>
              </el-form-item>
              <el-form-item label="环境/属性">
                <div class="inline-two">
                  <el-input v-model="abacCheckForm.envTag" />
                  <el-input v-model="abacCheckForm.attributesJson" />
                </div>
              </el-form-item>
              <el-form-item>
                <el-button type="success" :loading="loading.checkAllow" @click="checkAbac(false)">
                  校验（预期允许）
                </el-button>
                <el-button type="danger" :loading="loading.checkDeny" @click="checkAbac(true)">
                  校验（预期拒绝）
                </el-button>
              </el-form-item>
            </el-form>
          </el-card>

          <el-card v-show="activeMenu === 'federation' || activeMenu === 'all'" class="panel">
            <template #header>
              <div class="panel-title">
                <span>租户联邦映射</span>
                <el-tag type="info" effect="plain">TENANT</el-tag>
              </div>
            </template>
            <el-form label-width="92px" :model="federationForm">
              <el-form-item label="租户/发行方">
                <div class="inline-two">
                  <el-input v-model="federationForm.tenantCode" />
                  <el-input v-model="federationForm.issuer" />
                </div>
              </el-form-item>
              <el-form-item label="外部租户">
                <el-input v-model="federationForm.externalTenant" />
              </el-form-item>
              <el-form-item label="启用状态">
                <el-switch v-model="federationForm.enabled" />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" :loading="loading.upsertFederation" @click="upsertTenantFederation">
                  保存联邦映射
                </el-button>
                <el-button :loading="loading.listFederation" @click="listTenantFederations">查询联邦映射</el-button>
              </el-form-item>
            </el-form>
            <el-table v-if="tenantFederations.length" :data="tenantFederations" border stripe size="small">
              <el-table-column prop="tenantCode" label="租户" min-width="120" />
              <el-table-column prop="issuer" label="发行方" min-width="120" />
              <el-table-column prop="externalTenant" label="外部租户" min-width="120" />
              <el-table-column prop="status" label="状态" width="100" />
            </el-table>
            <el-empty v-else description="暂无联邦映射数据" />
          </el-card>

          <el-card v-show="activeMenu === 'governance' || activeMenu === 'all'" class="panel">
            <template #header>
              <div class="panel-title">
                <span>风险与巡检</span>
                <el-tag type="danger" effect="plain">GOVERNANCE</el-tag>
              </div>
            </template>
            <div class="action-wrap">
              <el-button type="primary" :loading="loading.risk" @click="listRiskEvents">查询风险事件</el-button>
              <el-button :loading="loading.overPrivileged" @click="inspectOverPrivileged">巡检过权账户</el-button>
              <el-button :loading="loading.zombie" @click="inspectZombieAccounts">巡检僵尸账号</el-button>
              <el-button :loading="loading.staleClients" @click="inspectStaleClients">巡检长期未使用凭证</el-button>
            </div>
            <div class="risk-summary">
              <el-tag type="danger">高风险 {{ riskSummary.high }}</el-tag>
              <el-tag type="warning">中风险 {{ riskSummary.medium }}</el-tag>
              <el-tag type="success">低风险 {{ riskSummary.low }}</el-tag>
              <el-tag>其他 {{ riskSummary.other }}</el-tag>
            </div>
            <el-table v-if="riskEvents.length" :data="riskEvents" border stripe size="small">
              <el-table-column prop="eventType" label="事件类型" min-width="140" />
              <el-table-column prop="severity" label="风险等级" width="100" />
              <el-table-column prop="tenantCode" label="租户" width="120" />
              <el-table-column prop="detail" label="详情" min-width="220" show-overflow-tooltip />
            </el-table>
            <el-empty v-else description="风险事件为空" />
            <el-divider />
            <el-table v-if="inspectionRows.length" :data="inspectionRows" border stripe size="small">
              <el-table-column prop="type" label="巡检类型" min-width="140" />
              <el-table-column prop="name" label="对象" min-width="160" show-overflow-tooltip />
              <el-table-column prop="tenantCode" label="租户" width="120" />
              <el-table-column prop="detail" label="详情" min-width="220" show-overflow-tooltip />
            </el-table>
            <el-empty v-else description="巡检结果为空" />
          </el-card>

          <el-card v-show="activeMenu === 'response' || activeMenu === 'all'" class="panel">
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
            </div>
            <pre class="result-pre">{{ responseText }}</pre>
          </el-card>

          <el-card v-show="activeMenu === 'timeline' || activeMenu === 'all'" class="panel">
            <template #header>
              <div class="panel-title">
                <span>最近操作时间线</span>
                <el-tag type="info" effect="plain">TIMELINE</el-tag>
              </div>
            </template>
            <el-timeline v-if="history.length">
              <el-timeline-item
                v-for="item in history"
                :key="item.id"
                :timestamp="`${item.at} · 状态 ${item.status} · ${item.elapsedMs}ms`"
                :type="item.ok ? 'success' : 'danger'"
              >
                {{ item.action }}
              </el-timeline-item>
            </el-timeline>
            <el-empty v-else description="暂无操作记录" />
          </el-card>
        </div>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'

const activeMenu = ref('abac-policy')

const loading = reactive({
  createAbac: false,
  checkAllow: false,
  checkDeny: false,
  upsertFederation: false,
  listFederation: false,
  risk: false,
  overPrivileged: false,
  zombie: false,
  staleClients: false
})

const abacPolicyForm = reactive({
  policyName: '租户隔离执行策略',
  resource: 'bids:model',
  action: 'execute',
  expression: 'tenantCode=tenant-a;department=platform;envTag=prod'
})

const abacCheckForm = reactive({
  userId: '1',
  tenantCode: 'tenant-a',
  resource: 'bids:model',
  action: 'execute',
  envTag: 'prod',
  attributesJson: '{"department":"platform"}'
})

const federationForm = reactive({
  tenantCode: 'tenant-a',
  issuer: 'mock-oidc',
  externalTenant: 'external-tenant-a',
  enabled: true
})

const nowText = new Date().toLocaleString('zh-CN', { hour12: false })

const latestResponse = ref(null)
const history = ref([])
const riskEvents = ref([])
const tenantFederations = ref([])
const inspectionRows = ref([])

const responseText = computed(() => JSON.stringify(latestResponse.value || { message: '等待操作...' }, null, 2))

const responseTagText = computed(() => {
  const val = latestResponse.value
  if (!val) {
    return 'IDLE'
  }
  return val.ok ? 'SUCCESS' : 'FAIL'
})

const responseTagType = computed(() => {
  const val = latestResponse.value
  if (!val) {
    return 'info'
  }
  return val.ok ? 'success' : 'danger'
})

const metrics = computed(() => {
  const val = latestResponse.value
  if (!val) {
    return { statusText: '-', elapsedText: '-', itemCountText: '-', emptyText: '-' }
  }
  return {
    statusText: val.status || '-',
    elapsedText: val.elapsedMs ? `${val.elapsedMs} ms` : '-',
    itemCountText: String(inferItemCount(val.payload)),
    emptyText: isPayloadEmpty(val.payload) ? '空结果' : '有数据'
  }
})

const riskSummary = computed(() => {
  const summary = { high: 0, medium: 0, low: 0, other: 0 }
  for (const item of riskEvents.value) {
    const severity = String(item?.severity || '').toUpperCase()
    if (severity === 'HIGH') {
      summary.high += 1
    } else if (severity === 'MEDIUM') {
      summary.medium += 1
    } else if (severity === 'LOW') {
      summary.low += 1
    } else {
      summary.other += 1
    }
  }
  return summary
})

function onSelectMenu(menu) {
  activeMenu.value = menu
}

async function createAbacPolicy() {
  loading.createAbac = true
  await callApi('/api/iam/abac-policies', 'POST', {
    policyName: abacPolicyForm.policyName,
    resource: abacPolicyForm.resource,
    action: abacPolicyForm.action,
    expression: abacPolicyForm.expression
  }, '创建 ABAC 策略')
  loading.createAbac = false
}

async function checkAbac(forceDeny) {
  const key = forceDeny ? 'checkDeny' : 'checkAllow'
  loading[key] = true
  const attrs = parseAttributes(abacCheckForm.attributesJson)
  if (forceDeny) {
    attrs.department = 'other'
  }
  await callApi('/api/iam/authorize/check', 'POST', {
    userId: Number(abacCheckForm.userId),
    resource: abacCheckForm.resource,
    action: abacCheckForm.action,
    envTag: forceDeny ? 'test' : abacCheckForm.envTag,
    tenantCode: abacCheckForm.tenantCode,
    attributes: attrs
  }, forceDeny ? 'ABAC 校验（预期拒绝）' : 'ABAC 校验（预期允许）')
  loading[key] = false
}

async function upsertTenantFederation() {
  loading.upsertFederation = true
  await callApi('/api/iam/tenant-federations', 'POST', {
    tenantCode: federationForm.tenantCode,
    issuer: federationForm.issuer,
    externalTenant: federationForm.externalTenant,
    enabled: federationForm.enabled
  }, '保存联邦映射')
  loading.upsertFederation = false
}

async function listTenantFederations() {
  loading.listFederation = true
  const response = await callApi('/api/iam/tenant-federations', 'GET', null, '查询联邦映射')
  tenantFederations.value = Array.isArray(response?.payload) ? response.payload : []
  loading.listFederation = false
}

async function listRiskEvents() {
  loading.risk = true
  const response = await callApi('/api/iam/governance/risk-events?limit=50', 'GET', null, '查询风险事件')
  riskEvents.value = Array.isArray(response?.payload) ? response.payload : []
  loading.risk = false
}

async function inspectOverPrivileged() {
  loading.overPrivileged = true
  const response = await callApi(
    '/api/iam/governance/inspections/over-privileged?permissionThreshold=2',
    'GET',
    null,
    '巡检过权账户'
  )
  inspectionRows.value = mapInspectionRows(response?.payload, '过权账户')
  loading.overPrivileged = false
}

async function inspectZombieAccounts() {
  loading.zombie = true
  const response = await callApi(
    '/api/iam/governance/inspections/zombie-accounts?inactiveDays=30',
    'GET',
    null,
    '巡检僵尸账号'
  )
  inspectionRows.value = mapInspectionRows(response?.payload, '僵尸账号')
  loading.zombie = false
}

async function inspectStaleClients() {
  loading.staleClients = true
  const response = await callApi(
    '/api/iam/governance/inspections/stale-clients?staleDays=30',
    'GET',
    null,
    '巡检长期未使用凭证'
  )
  inspectionRows.value = mapInspectionRows(response?.payload, '长期未使用凭证')
  loading.staleClients = false
}

async function callApi(path, method, body, action) {
  const begin = performance.now()
  try {
    const response = await fetch(path, {
      method,
      headers: { 'Content-Type': 'application/json' },
      body: body ? JSON.stringify(body) : undefined
    })
    const payload = await response.json().catch(() => ({}))
    const elapsedMs = Math.round(performance.now() - begin)
    const result = {
      ok: response.ok,
      status: response.status,
      path,
      method,
      elapsedMs,
      requestBody: body,
      payload
    }
    latestResponse.value = result
    recordHistory(action, response.ok, response.status, elapsedMs)
    if (!response.ok) {
      ElMessage.error(`${action}失败：${payload?.message || response.status}`)
    } else {
      ElMessage.success(`${action}成功`)
    }
    return result
  } catch (error) {
    const elapsedMs = Math.round(performance.now() - begin)
    const result = {
      ok: false,
      status: 0,
      path,
      method,
      elapsedMs,
      requestBody: body,
      payload: { message: error?.message || String(error) }
    }
    latestResponse.value = result
    recordHistory(action, false, 0, elapsedMs)
    ElMessage.error(`${action}失败：${result.payload.message}`)
    return result
  }
}

function recordHistory(action, ok, status, elapsedMs) {
  const item = {
    id: `${Date.now()}-${Math.random().toString(16).slice(2, 8)}`,
    action,
    ok,
    status,
    elapsedMs,
    at: new Date().toLocaleTimeString('zh-CN', { hour12: false })
  }
  history.value = [item, ...history.value].slice(0, 10)
}

function parseAttributes(text) {
  if (!text) {
    return {}
  }
  try {
    const parsed = JSON.parse(text)
    if (parsed && typeof parsed === 'object') {
      return parsed
    }
  } catch {
    return {}
  }
  return {}
}

function inferItemCount(payload) {
  if (Array.isArray(payload)) {
    return payload.length
  }
  if (!payload || typeof payload !== 'object') {
    return 0
  }
  return Object.keys(payload).length
}

function isPayloadEmpty(payload) {
  return inferItemCount(payload) === 0
}

function mapInspectionRows(payload, type) {
  if (!Array.isArray(payload)) {
    return []
  }
  return payload.map((item) => ({
    type,
    name: item.username || item.client_id || item.clientId || String(item.user_id || item.userId || '-'),
    tenantCode: item.tenant_code || item.tenantCode || '-',
    detail: JSON.stringify(item)
  }))
}
</script>
