<template>
  <div class="page-stack">
    <el-card class="panel">
      <template #header>
        <div class="panel-title">
          <span>ABAC 策略管理</span>
          <el-tag type="success" effect="plain">POLICY</el-tag>
        </div>
      </template>

      <el-form label-width="88px" :model="queryForm">
        <el-form-item label="策略名称">
          <el-input v-model="queryForm.policyName" clearable />
        </el-form-item>
        <el-form-item label="资源标识">
          <el-input v-model="queryForm.resourceCode" clearable />
        </el-form-item>
        <el-form-item label="动作">
          <el-select v-model="queryForm.actionCode" clearable filterable placeholder="请选择动作">
            <el-option v-for="action in actionOptions" :key="action" :label="action" :value="action" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button :loading="querying" @click="queryPolicies">查询</el-button>
          <el-button type="primary" @click="openCreateDrawer">新建策略</el-button>
        </el-form-item>
      </el-form>

      <el-table v-if="policyRows.length" :data="policyRows" border stripe size="small">
        <el-table-column prop="policyName" label="策略名" min-width="150" />
        <el-table-column prop="resourceCode" label="资源" min-width="120" />
        <el-table-column prop="actionCode" label="动作" width="110" />
        <el-table-column prop="expression" label="表达式" min-width="280" show-overflow-tooltip />
        <el-table-column prop="creator" label="创建人" width="100" />
        <el-table-column prop="owner" label="责任人" width="100" />
        <el-table-column prop="gmtModified" label="最后修改时间" min-width="170" />
        <el-table-column prop="modifier" label="最后修改人" width="110" />
      </el-table>
      <el-empty v-else description="暂无策略数据，请设置筛选条件后查询" />
    </el-card>

    <el-drawer v-model="createVisible" title="创建 ABAC 策略" size="560px" destroy-on-close>
      <el-form ref="formRef" label-width="96px" :model="form" :rules="rules">
        <el-form-item label="策略名称" prop="policyName">
          <el-input v-model="form.policyName" />
        </el-form-item>
        <el-form-item label="资源" prop="resource">
          <el-input v-model="form.resource" placeholder="bids:model" />
        </el-form-item>
        <el-form-item label="动作" prop="action">
          <el-select v-model="form.action" filterable allow-create default-first-option>
            <el-option v-for="action in actionOptions" :key="action" :label="action" :value="action" />
          </el-select>
        </el-form-item>
        <el-form-item label="表达式" prop="expression">
          <el-input v-model="form.expression" type="textarea" :rows="4" />
        </el-form-item>
        <el-form-item label="责任人" prop="owner">
          <el-input v-model="form.owner" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="submit">创建</el-button>
          <el-button @click="createVisible = false">取消</el-button>
        </el-form-item>
      </el-form>
    </el-drawer>
  </div>
</template>

<script setup>
import { onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { createAbacPolicy, listAbacPolicies } from '../../api/abacApi'
import { useAuthStore } from '../../stores/authStore'

const formRef = ref(null)
const loading = ref(false)
const querying = ref(false)
const createVisible = ref(false)
const policyRows = ref([])
const authStore = useAuthStore()
const queryTimer = ref(0)

const actionOptions = ['execute', 'read', 'write', 'delete']

const queryForm = reactive({
  policyName: '',
  resourceCode: '',
  actionCode: ''
})

const form = reactive({
  policyName: '租户隔离执行策略',
  resource: 'bids:model',
  action: 'execute',
  expression: 'tenantCode=tenant-a;department=platform;envTag=prod',
  owner: authStore.username || 'admin'
})

const rules = {
  policyName: [{ required: true, message: '请输入策略名称', trigger: 'blur' }],
  resource: [{ required: true, message: '请输入资源标识', trigger: 'blur' }],
  action: [{ required: true, message: '请输入动作', trigger: 'blur' }],
  expression: [{ required: true, message: '请输入表达式', trigger: 'blur' }],
  owner: [{ required: true, message: '请输入责任人', trigger: 'blur' }]
}

function openCreateDrawer() {
  createVisible.value = true
}

async function queryPolicies() {
  querying.value = true
  const response = await listAbacPolicies({
    policyName: queryForm.policyName,
    resourceCode: queryForm.resourceCode,
    actionCode: queryForm.actionCode,
    limit: 100
  })
  const rows = Array.isArray(response.payload) ? response.payload : []
  policyRows.value = rows.map((row) => ({
    ...row,
    creator: row.createdBy || '-',
    owner: row.owner || '-',
    modifier: row.modifiedBy || '-',
    gmtModified: row.gmtModified || '-',
    expression: row.expression || '-'
  }))
  querying.value = false
}

async function submit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) {
    return
  }
  loading.value = true
  await createAbacPolicy({
    policyName: form.policyName,
    resource: form.resource,
    action: form.action,
    expression: form.expression,
    createdBy: authStore.username || 'admin',
    owner: form.owner,
    modifiedBy: authStore.username || 'admin'
  })
  loading.value = false
  createVisible.value = false
  await queryPolicies()
}

function scheduleQuery() {
  window.clearTimeout(queryTimer.value)
  queryTimer.value = window.setTimeout(() => {
    queryPolicies()
  }, 300)
}

watch(
  () => [queryForm.policyName, queryForm.resourceCode, queryForm.actionCode],
  () => {
    scheduleQuery()
  }
)

onMounted(queryPolicies)
onBeforeUnmount(() => {
  window.clearTimeout(queryTimer.value)
})
</script>
