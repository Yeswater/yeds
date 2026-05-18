<template>
  <el-card class="panel yeds-panel">
    <template #header>
      <div class="panel-title">
        <span>租户联邦映射</span>
        <el-tag type="info" effect="plain">TENANT</el-tag>
      </div>
    </template>

    <YedsListToolbar>
      <el-button type="primary" @click="openCreate">新建映射</el-button>
      <el-button :loading="listing" @click="list">刷新列表</el-button>
    </YedsListToolbar>

    <YedsListTableWrap>
      <el-table :data="pagedRows" border stripe size="small">
        <YedsTableOperationColumn
          :delete-message="(row) => `确认删除联邦映射「${row.tenantCode} / ${row.issuer} / ${row.externalTenant}」吗？`"
          @copy="onCopy"
          @edit="onEdit"
          @delete="onDelete"
        />
        <el-table-column prop="tenantCode" label="租户" min-width="120" />
        <el-table-column prop="issuer" label="发行方" min-width="120" />
        <el-table-column prop="externalTenant" label="外部租户" min-width="120" />
        <el-table-column prop="status" label="状态" width="100" />
        <YedsTableAuditColumns />
        <template #empty>
          <el-empty description="暂无联邦映射数据" />
        </template>
      </el-table>
    </YedsListTableWrap>
    <YedsListPagination
      :total="total"
      :page-size="pageSize"
      :current-page="currentPage"
      @update:current-page="onPageChange"
    />

    <el-drawer v-model="drawerVisible" :title="editingId ? '编辑联邦映射' : '新建联邦映射'" size="480px" destroy-on-close>
      <el-form ref="formRef" label-width="92px" :model="form" :rules="rules">
        <el-form-item label="租户" prop="tenantCode">
          <el-input v-model="form.tenantCode" :disabled="!!editingId" />
        </el-form-item>
        <el-form-item label="发行方" prop="issuer">
          <el-input v-model="form.issuer" :disabled="!!editingId" />
        </el-form-item>
        <el-form-item label="外部租户" prop="externalTenant">
          <el-input v-model="form.externalTenant" :disabled="!!editingId" />
        </el-form-item>
        <el-form-item label="启用状态" prop="enabled">
          <el-switch v-model="form.enabled" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="saving" @click="save">保存</el-button>
          <el-button @click="drawerVisible = false">取消</el-button>
        </el-form-item>
      </el-form>
    </el-drawer>
  </el-card>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  YedsListTableWrap,
  YedsListPagination,
  YedsListToolbar,
  YedsTableOperationColumn,
  YedsTableAuditColumns,
  useClientPager,
  mapAuditRows
} from '@yeds/ui'
import { deleteTenantFederation, fetchTenantFederations, saveTenantFederation } from '../../api/federationApi'
import { useAuthStore } from '../../stores/authStore'

const formRef = ref(null)
const rows = ref([])
const drawerVisible = ref(false)
const editingId = ref(null)
const saving = ref(false)
const listing = ref(false)
const authStore = useAuthStore()

const { currentPage, pageSize, total, pagedRows, onPageChange } = useClientPager(rows, 20)

const form = reactive({
  tenantCode: 'tenant-a',
  issuer: 'mock-oidc',
  externalTenant: 'external-tenant-a',
  enabled: true
})

const rules = {
  tenantCode: [{ required: true, message: '请输入租户编码', trigger: 'blur' }],
  issuer: [{ required: true, message: '请输入发行方', trigger: 'blur' }],
  externalTenant: [{ required: true, message: '请输入外部租户', trigger: 'blur' }]
}

function openCreate() {
  editingId.value = null
  form.tenantCode = 'tenant-a'
  form.issuer = 'mock-oidc'
  form.externalTenant = 'external-tenant-a'
  form.enabled = true
  drawerVisible.value = true
}

function onEdit(row) {
  editingId.value = row.id
  form.tenantCode = row.tenantCode
  form.issuer = row.issuer
  form.externalTenant = row.externalTenant
  form.enabled = row.status === 1
  drawerVisible.value = true
}

function onCopy(row) {
  editingId.value = null
  form.tenantCode = row.tenantCode
  form.issuer = row.issuer
  form.externalTenant = `${row.externalTenant}-copy`
  form.enabled = row.status === 1
  drawerVisible.value = true
}

async function onDelete(row) {
  const operator = authStore.username || 'admin'
  await deleteTenantFederation(row.id, operator)
  ElMessage.success('联邦映射已删除')
  await list()
}

async function save() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) {
    return
  }
  saving.value = true
  try {
    const operator = authStore.username || 'admin'
    await saveTenantFederation({
      tenantCode: form.tenantCode,
      issuer: form.issuer,
      externalTenant: form.externalTenant,
      enabled: form.enabled,
      modifiedBy: operator,
      appCode: 'IAM'
    })
    drawerVisible.value = false
    ElMessage.success('联邦映射已保存')
    await list()
  } finally {
    saving.value = false
  }
}

async function list() {
  listing.value = true
  try {
    const response = await fetchTenantFederations()
    const data = Array.isArray(response.payload) ? response.payload : []
    rows.value = mapAuditRows(data, 'IAM')
  } finally {
    listing.value = false
  }
}

onMounted(list)
</script>
