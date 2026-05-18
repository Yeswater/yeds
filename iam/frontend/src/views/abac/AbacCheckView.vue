<template>
  <div class="page-stack">
    <el-card class="panel yeds-panel">
      <template #header>
        <div class="panel-title">
          <span>ABAC 鉴权校验</span>
          <el-tag type="warning" effect="plain">VERIFY</el-tag>
        </div>
      </template>
      <el-form ref="formRef" label-width="92px" :model="form" :rules="rules">
        <el-form-item label="用户">
          <div class="inline-two">
            <el-form-item>
              <el-input :model-value="authStore.username || 'unknown'" disabled />
            </el-form-item>
            <el-form-item prop="userId">
              <el-input v-model="form.userId" placeholder="用户ID" />
            </el-form-item>
          </div>
        </el-form-item>
        <el-form-item label="租户/环境">
          <div class="inline-two">
            <el-form-item prop="tenantCode">
              <el-select v-model="form.tenantCode" filterable allow-create default-first-option>
                <el-option v-for="tenant in tenantOptions" :key="tenant" :label="tenant" :value="tenant" />
              </el-select>
            </el-form-item>
            <el-form-item prop="envTag">
              <el-select v-model="form.envTag" filterable>
                <el-option v-for="env in envOptions" :key="env" :label="env" :value="env" />
              </el-select>
            </el-form-item>
          </div>
        </el-form-item>
        <el-form-item label="资源" prop="resource">
          <el-select v-model="form.resource" filterable allow-create default-first-option>
            <el-option v-for="resource in resourceOptions" :key="resource" :label="resource" :value="resource" />
          </el-select>
        </el-form-item>
        <el-form-item label="动作" prop="action">
          <el-select v-model="form.action" filterable allow-create default-first-option>
            <el-option v-for="action in actionOptions" :key="action" :label="action" :value="action" />
          </el-select>
        </el-form-item>
        <el-form-item label="属性JSON" prop="attributesJson">
          <el-input v-model="form.attributesJson" type="textarea" :rows="4" />
        </el-form-item>
        <el-form-item>
          <el-button type="success" :loading="loadingAllow" @click="submit(false)">校验（预期允许）</el-button>
          <el-button type="danger" :loading="loadingDeny" @click="submit(true)">校验（预期拒绝）</el-button>
        </el-form-item>
      </el-form>
    </el-card>
    <api-response-panel />
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { checkAuthorize } from '../../api/abacApi'
import { fetchTenantCatalog } from '../../api/tenantApi'
import ApiResponsePanel from '../../components/ApiResponsePanel.vue'
import { useAuthStore } from '../../stores/authStore'
import { useTenantStore } from '../../stores/tenantStore'

const formRef = ref(null)
const loadingAllow = ref(false)
const loadingDeny = ref(false)
const authStore = useAuthStore()
const tenantStore = useTenantStore()

const tenantOptions = ref([])
const envOptions = ['prod', 'staging', 'test', 'dev']
const resourceOptions = ['bids:model', 'bids:query', 'iam:user']
const actionOptions = ['execute', 'read', 'write', 'delete']

const form = reactive({
  userId: '1',
  tenantCode: 'tenant-a',
  resource: 'bids:model',
  action: 'execute',
  envTag: 'prod',
  attributesJson: '{"department":"platform"}'
})

const jsonValidator = (_rule, value, callback) => {
  try {
    const parsed = JSON.parse(value || '{}')
    if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed)) {
      callback(new Error('属性必须是 JSON 对象'))
      return
    }
    callback()
  } catch {
    callback(new Error('属性 JSON 格式不正确'))
  }
}

const rules = {
  userId: [{ required: true, message: '请输入用户ID', trigger: 'blur' }],
  tenantCode: [{ required: true, message: '请输入租户编码', trigger: 'blur' }],
  envTag: [{ required: true, message: '请输入环境标签', trigger: 'blur' }],
  resource: [{ required: true, message: '请输入资源标识', trigger: 'blur' }],
  action: [{ required: true, message: '请输入动作', trigger: 'blur' }],
  attributesJson: [{ validator: jsonValidator, trigger: 'blur' }]
}

async function submit(forceDeny) {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) {
    return
  }
  const attrs = JSON.parse(form.attributesJson || '{}')
  if (forceDeny) {
    attrs.department = 'other'
  }
  if (forceDeny) {
    loadingDeny.value = true
  } else {
    loadingAllow.value = true
  }
  await checkAuthorize(
    {
      userId: Number(form.userId),
      resource: form.resource,
      action: form.action,
      envTag: forceDeny ? 'test' : form.envTag,
      tenantCode: form.tenantCode,
      attributes: attrs
    },
    forceDeny
  )
  if (forceDeny) {
    loadingDeny.value = false
  } else {
    loadingAllow.value = false
  }
}

async function loadTenantOptions() {
  if (authStore.username !== 'admin') {
    tenantOptions.value = [tenantStore.tenantCode]
    form.tenantCode = tenantStore.tenantCode
    return
  }
  const response = await fetchTenantCatalog()
  const rows = Array.isArray(response.payload) ? response.payload : []
  const dynamicTenants = rows.map((row) => row.tenantCode).filter((item) => Boolean(item))
  const uniqueTenants = Array.from(new Set(dynamicTenants))
  tenantOptions.value = uniqueTenants
  if (!uniqueTenants.includes(form.tenantCode)) {
    form.tenantCode = uniqueTenants[0] || 'tenant-a'
  }
}

onMounted(loadTenantOptions)
</script>
