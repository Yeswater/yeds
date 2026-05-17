<template>
  <el-card class="panel">
      <template #header>
        <div class="panel-title">
          <span>租户联邦映射</span>
          <el-tag type="info" effect="plain">TENANT</el-tag>
        </div>
      </template>

    <el-form ref="formRef" label-width="92px" :model="form" :rules="rules">
      <el-form-item label="租户/发行方">
        <div class="inline-two">
          <el-form-item prop="tenantCode">
            <el-input v-model="form.tenantCode" />
          </el-form-item>
          <el-form-item prop="issuer">
            <el-input v-model="form.issuer" />
          </el-form-item>
        </div>
      </el-form-item>
      <el-form-item label="外部租户" prop="externalTenant">
        <el-input v-model="form.externalTenant" />
      </el-form-item>
      <el-form-item label="启用状态" prop="enabled">
        <el-switch v-model="form.enabled" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="saving" @click="save">保存联邦映射</el-button>
        <el-button :loading="listing" @click="list">查询联邦映射</el-button>
      </el-form-item>
    </el-form>

      <el-table v-if="rows.length" :data="rows" border stripe size="small">
        <el-table-column prop="tenantCode" label="租户" min-width="120" />
        <el-table-column prop="issuer" label="发行方" min-width="120" />
        <el-table-column prop="externalTenant" label="外部租户" min-width="120" />
        <el-table-column prop="status" label="状态" width="100" />
      </el-table>
      <el-empty v-else description="暂无联邦映射数据" />
    </el-card>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { fetchTenantFederations, saveTenantFederation } from '../../api/federationApi'

const formRef = ref(null)
const rows = ref([])
const saving = ref(false)
const listing = ref(false)

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

async function save() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) {
    return
  }
  saving.value = true
  await saveTenantFederation({
    tenantCode: form.tenantCode,
    issuer: form.issuer,
    externalTenant: form.externalTenant,
    enabled: form.enabled
  })
  saving.value = false
}

async function list() {
  listing.value = true
  const response = await fetchTenantFederations()
  rows.value = Array.isArray(response.payload) ? response.payload : []
  listing.value = false
}
</script>
