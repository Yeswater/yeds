<template>
  <div class="page-stack">
    <YedsListPageCard title="ABAC 策略管理" tag="POLICY">
      <YedsListQueryHead :model="queryForm" :loading="querying" @search="queryPolicies" @reset="onReset">
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
      </YedsListQueryHead>

      <YedsListToolbar>
        <el-button type="primary" @click="openCreateDrawer">新建策略</el-button>
      </YedsListToolbar>

      <YedsListTableWrap>
        <el-table :data="pagedRows" border stripe size="small">
          <YedsTableOperationColumn
            :delete-message="(row) => `确认删除策略「${row.policyName}」吗？`"
            @copy="onCopy"
            @edit="openEditDrawer"
            @delete="onDelete"
          />
          <el-table-column prop="policyName" label="策略名" min-width="150" />
          <el-table-column prop="resourceCode" label="资源" min-width="120" />
          <el-table-column prop="actionCode" label="动作" width="110" />
          <el-table-column prop="expression" label="表达式" min-width="280" show-overflow-tooltip />
          <el-table-column prop="owner" label="责任人" width="100" />
          <YedsTableAuditColumns />
          <template #empty>
            <el-empty description="暂无策略数据，请设置筛选条件后查询" />
          </template>
        </el-table>
      </YedsListTableWrap>

      <YedsListPagination
        :total="total"
        :page-size="pageSize"
        :current-page="currentPage"
        @update:current-page="onPageChange"
      />
    </YedsListPageCard>

    <el-drawer
      v-model="drawerVisible"
      :title="editingId ? '编辑 ABAC 策略' : '创建 ABAC 策略'"
      size="560px"
      destroy-on-close
    >
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
          <el-button type="primary" :loading="loading" @click="submit">
            {{ editingId ? '保存' : '创建' }}
          </el-button>
          <el-button @click="drawerVisible = false">取消</el-button>
        </el-form-item>
      </el-form>
    </el-drawer>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  YedsListPageCard,
  YedsListQueryHead,
  YedsListToolbar,
  YedsListTableWrap,
  YedsListPagination,
  YedsTableOperationColumn,
  YedsTableAuditColumns,
  useListQuery,
  useClientPager,
  mapAuditRows
} from '@yeds/ui'
import { createAbacPolicy, deleteAbacPolicy, listAbacPolicies, updateAbacPolicy } from '../../api/abacApi'
import { useAuthStore } from '../../stores/authStore'

const formRef = ref(null)
const loading = ref(false)
const querying = ref(false)
const drawerVisible = ref(false)
const editingId = ref(null)
const policyRows = ref([])
const authStore = useAuthStore()

const actionOptions = ['execute', 'read', 'write', 'delete']

const { model: queryForm, resetModel } = useListQuery({
  policyName: '',
  resourceCode: '',
  actionCode: ''
})

const { currentPage, pageSize, total, pagedRows, onPageChange } = useClientPager(policyRows, 20)

const form = reactive({
  policyName: '',
  resource: 'bids:model',
  action: 'execute',
  expression: '',
  owner: authStore.username || 'admin'
})

const rules = {
  policyName: [{ required: true, message: '请输入策略名称', trigger: 'blur' }],
  resource: [{ required: true, message: '请输入资源标识', trigger: 'blur' }],
  action: [{ required: true, message: '请输入动作', trigger: 'blur' }],
  expression: [{ required: true, message: '请输入表达式', trigger: 'blur' }],
  owner: [{ required: true, message: '请输入责任人', trigger: 'blur' }]
}

function resetFormDefaults() {
  form.policyName = '租户隔离执行策略'
  form.resource = 'bids:model'
  form.action = 'execute'
  form.expression = 'tenantCode=tenant-a;department=platform;envTag=prod'
  form.owner = authStore.username || 'admin'
}

function openCreateDrawer() {
  editingId.value = null
  resetFormDefaults()
  drawerVisible.value = true
}

function openEditDrawer(row) {
  editingId.value = row.id
  form.policyName = row.policyName || ''
  form.resource = row.resourceCode || ''
  form.action = row.actionCode || ''
  form.expression = row.expression === '-' ? '' : row.expression || ''
  form.owner = row.owner === '-' ? authStore.username || 'admin' : row.owner
  drawerVisible.value = true
}

function onCopy(row) {
  editingId.value = null
  form.policyName = `${row.policyName || ''}-副本`
  form.resource = row.resourceCode || ''
  form.action = row.actionCode || ''
  form.expression = row.expression === '-' ? '' : row.expression || ''
  form.owner = row.owner === '-' ? authStore.username || 'admin' : row.owner
  drawerVisible.value = true
}

async function onDelete(row) {
  const operator = authStore.username || 'admin'
  await deleteAbacPolicy(row.id, operator)
  ElMessage.success('策略已删除')
  await queryPolicies()
}

function onReset() {
  resetModel()
  queryPolicies()
}

async function queryPolicies() {
  querying.value = true
  try {
    const response = await listAbacPolicies({
      policyName: queryForm.policyName,
      resourceCode: queryForm.resourceCode,
      actionCode: queryForm.actionCode,
      limit: 200
    })
    const rows = Array.isArray(response.payload) ? response.payload : []
    policyRows.value = mapAuditRows(
      rows.map((row) => ({
        ...row,
        owner: row.owner || '-',
        expression: row.expression || '-'
      })),
      'IAM'
    )
  } finally {
    querying.value = false
  }
}

async function submit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) {
    return
  }
  loading.value = true
  try {
    const operator = authStore.username || 'admin'
    const payload = {
      policyName: form.policyName,
      resource: form.resource,
      action: form.action,
      expression: form.expression,
      owner: form.owner,
      modifiedBy: operator
    }
    if (editingId.value) {
      await updateAbacPolicy(editingId.value, payload)
    } else {
      await createAbacPolicy({
        ...payload,
        createdBy: operator
      })
    }
    drawerVisible.value = false
    await queryPolicies()
  } finally {
    loading.value = false
  }
}

onMounted(queryPolicies)
</script>
