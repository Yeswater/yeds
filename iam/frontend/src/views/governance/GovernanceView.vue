<template>
  <el-card class="panel yeds-panel">
      <template #header>
        <div class="panel-title">
          <span>风险与巡检</span>
          <el-tag type="danger" effect="plain">GOVERNANCE</el-tag>
        </div>
      </template>

      <el-form ref="formRef" :model="params" :rules="rules" label-width="132px">
        <el-form-item label="风险事件数量" prop="riskLimit">
          <el-input-number v-model="params.riskLimit" :min="1" :max="500" />
        </el-form-item>
        <el-form-item label="过权权限阈值" prop="permissionThreshold">
          <el-input-number v-model="params.permissionThreshold" :min="1" :max="1000" />
        </el-form-item>
        <el-form-item label="僵尸账号天数" prop="inactiveDays">
          <el-input-number v-model="params.inactiveDays" :min="1" :max="3650" />
        </el-form-item>
        <el-form-item label="长期未用凭证天数" prop="staleDays">
          <el-input-number v-model="params.staleDays" :min="1" :max="3650" />
        </el-form-item>
      </el-form>

      <div class="action-wrap">
        <el-button type="primary" :loading="loading.risk" @click="queryRiskEvents">查询风险事件</el-button>
        <el-button :loading="loading.overPrivileged" @click="queryOverPrivileged">巡检过权账户</el-button>
        <el-button :loading="loading.zombie" @click="queryZombie">巡检僵尸账号</el-button>
        <el-button :loading="loading.staleClients" @click="queryStaleClients">巡检长期未使用凭证</el-button>
      </div>

      <div class="risk-summary">
        <el-tag type="danger">高风险 {{ riskSummary.high }}</el-tag>
        <el-tag type="warning">中风险 {{ riskSummary.medium }}</el-tag>
        <el-tag type="success">低风险 {{ riskSummary.low }}</el-tag>
        <el-tag>其他 {{ riskSummary.other }}</el-tag>
      </div>

      <YedsListTableWrap>
        <el-table :data="pagedRiskEvents" border stripe size="small">
          <YedsTableOperationColumn :show-edit="false" :show-delete="false" @copy="onCopyRisk" />
          <el-table-column prop="eventType" label="事件类型" min-width="140" />
          <el-table-column prop="severity" label="风险等级" width="100" />
          <el-table-column prop="tenantCode" label="租户" width="120" />
          <el-table-column prop="detail" label="详情" min-width="220" show-overflow-tooltip />
          <YedsTableAuditColumns />
          <template #empty>
            <el-empty description="风险事件为空" />
          </template>
        </el-table>
      </YedsListTableWrap>
      <YedsListPagination
        :total="riskTotal"
        :page-size="riskPageSize"
        :current-page="riskCurrentPage"
        @update:current-page="onRiskPageChange"
      />

      <el-divider />

      <YedsListTableWrap>
        <el-table :data="pagedInspectionRows" border stripe size="small">
          <YedsTableOperationColumn :show-edit="false" :show-delete="false" @copy="onCopyInspection" />
          <el-table-column prop="type" label="巡检类型" min-width="140" />
          <el-table-column prop="name" label="对象" min-width="160" show-overflow-tooltip />
          <el-table-column prop="tenantCode" label="租户" width="120" />
          <el-table-column prop="detail" label="详情" min-width="220" show-overflow-tooltip />
          <YedsTableAuditColumns />
          <template #empty>
            <el-empty description="巡检结果为空" />
          </template>
        </el-table>
      </YedsListTableWrap>
      <YedsListPagination
        :total="inspectionTotal"
        :page-size="inspectionPageSize"
        :current-page="inspectionCurrentPage"
        @update:current-page="onInspectionPageChange"
      />
    </el-card>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  YedsListTableWrap,
  YedsListPagination,
  YedsTableOperationColumn,
  YedsTableAuditColumns,
  useClientPager,
  mapAuditRows,
  copyText
} from '@yeds/ui'
import {
  fetchRiskEvents,
  inspectOverPrivileged,
  inspectStaleClients,
  inspectZombieAccounts
} from '../../api/governanceApi'

const formRef = ref(null)
const riskEvents = ref([])
const inspectionRows = ref([])

const {
  currentPage: riskCurrentPage,
  pageSize: riskPageSize,
  total: riskTotal,
  pagedRows: pagedRiskEvents,
  onPageChange: onRiskPageChange
} = useClientPager(riskEvents, 20)

const {
  currentPage: inspectionCurrentPage,
  pageSize: inspectionPageSize,
  total: inspectionTotal,
  pagedRows: pagedInspectionRows,
  onPageChange: onInspectionPageChange
} = useClientPager(inspectionRows, 20)
const loading = reactive({
  risk: false,
  overPrivileged: false,
  zombie: false,
  staleClients: false
})

const params = reactive({
  riskLimit: 50,
  permissionThreshold: 2,
  inactiveDays: 30,
  staleDays: 30
})

const buildRangeRule = (label, min, max) => ({
  validator: (_rule, value, callback) => {
    if (typeof value !== 'number' || Number.isNaN(value)) {
      callback(new Error(`${label}必须为数字`))
      return
    }
    if (value < min || value > max) {
      callback(new Error(`${label}范围应在 ${min}-${max}`))
      return
    }
    callback()
  },
  trigger: 'change'
})

const rules = {
  riskLimit: [buildRangeRule('风险事件数量', 1, 500)],
  permissionThreshold: [buildRangeRule('过权权限阈值', 1, 1000)],
  inactiveDays: [buildRangeRule('僵尸账号天数', 1, 3650)],
  staleDays: [buildRangeRule('长期未用凭证天数', 1, 3650)]
}

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

async function queryRiskEvents() {
  const valid = await formRef.value.validateField('riskLimit').then(() => true).catch(() => false)
  if (!valid) {
    return
  }
  loading.risk = true
  const response = await fetchRiskEvents(params.riskLimit)
  const rows = Array.isArray(response.payload) ? response.payload : []
  riskEvents.value = mapAuditRows(
    rows.map((item) => ({
      ...item,
      lastOperator: '-',
      lastOperateTime: item.createdAt || item.created_at || ''
    })),
    'IAM'
  )
  loading.risk = false
}

async function queryOverPrivileged() {
  const valid = await formRef.value.validateField('permissionThreshold').then(() => true).catch(() => false)
  if (!valid) {
    return
  }
  loading.overPrivileged = true
  const response = await inspectOverPrivileged(params.permissionThreshold)
  inspectionRows.value = mapAuditRows(mapInspectionRows(response.payload, '过权账户'), 'IAM')
  loading.overPrivileged = false
}

async function queryZombie() {
  const valid = await formRef.value.validateField('inactiveDays').then(() => true).catch(() => false)
  if (!valid) {
    return
  }
  loading.zombie = true
  const response = await inspectZombieAccounts(params.inactiveDays)
  inspectionRows.value = mapAuditRows(mapInspectionRows(response.payload, '僵尸账号'), 'IAM')
  loading.zombie = false
}

async function onCopyRisk(row) {
  const ok = await copyText(JSON.stringify(row, null, 2))
  if (ok) {
    ElMessage.success('已复制风险事件')
  }
}

async function onCopyInspection(row) {
  const ok = await copyText(row.detail || JSON.stringify(row))
  if (ok) {
    ElMessage.success('已复制巡检详情')
  }
}

async function queryStaleClients() {
  const valid = await formRef.value.validateField('staleDays').then(() => true).catch(() => false)
  if (!valid) {
    return
  }
  loading.staleClients = true
  const response = await inspectStaleClients(params.staleDays)
  inspectionRows.value = mapAuditRows(mapInspectionRows(response.payload, '长期未使用凭证'), 'IAM')
  loading.staleClients = false
}
</script>
