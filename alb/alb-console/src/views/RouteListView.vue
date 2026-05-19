<template>
  <YedsListPageCard title="路由管理" tag="ALB">
    <YedsListToolbar>
      <el-button type="primary" @click="openCreate">新建路由</el-button>
      <el-button @click="$router.push('/upstreams')">上游管理</el-button>
        <el-button @click="$router.push('/caddy')">Nginx 预览</el-button>
    </YedsListToolbar>

    <YedsListTableWrap>
      <el-table v-loading="loading" :data="rows" row-key="id">
        <el-table-column label="操作" width="200" fixed="left">
          <template #default="{ row }">
            <el-button link type="primary" @click="$router.push(`/routes/${row.id}`)">Header</el-button>
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button v-if="!row.systemLocked" link type="danger" @click="onDelete(row)">删除</el-button>
          </template>
        </el-table-column>
        <el-table-column prop="host" label="Host" min-width="140" />
        <el-table-column prop="pathPattern" label="Path" min-width="100" />
        <el-table-column prop="upstreamName" label="上游" width="120" />
        <el-table-column prop="priority" label="优先级" width="80" />
        <el-table-column label="启用" width="70">
          <template #default="{ row }">
            <el-tag :type="row.enabled ? 'success' : 'info'" size="small">{{ row.enabled ? '是' : '否' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="redirectUrl" label="重定向" min-width="160" show-overflow-tooltip />
        <el-table-column label="系统" width="70">
          <template #default="{ row }">
            <el-tag v-if="row.systemLocked" size="small">内置</el-tag>
          </template>
        </el-table-column>
      </el-table>
    </YedsListTableWrap>
  </YedsListPageCard>

  <el-drawer v-model="drawerVisible" :title="editingId ? '编辑路由' : '新建路由'" size="480px">
    <el-form label-position="top">
      <el-form-item label="Host" required>
        <el-input v-model="form.host" placeholder="例如 bids.yeds.com" />
      </el-form-item>
      <el-form-item label="Path">
        <el-input v-model="form.pathPattern" placeholder="/**" />
      </el-form-item>
      <el-form-item label="上游" required>
        <el-select v-model="form.upstreamId" style="width: 100%">
          <el-option v-for="u in upstreams" :key="u.id" :label="`${u.name} (${u.targetUrl})`" :value="u.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="优先级">
        <el-input-number v-model="form.priority" :min="0" />
      </el-form-item>
      <el-form-item label="重定向 URL（可选）">
        <el-input v-model="form.redirectUrl" placeholder="http://bids.yeds.com:9080" />
      </el-form-item>
      <el-form-item label="启用">
        <el-switch v-model="form.enabled" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="drawerVisible = false">取消</el-button>
      <el-button type="primary" :loading="saving" @click="save">保存</el-button>
    </template>
  </el-drawer>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { YedsListPageCard, YedsListTableWrap, YedsListToolbar } from '@yeds/ui'
import { createRoute, deleteRoute, listRoutes, listUpstreams, updateRoute } from '../api/albApi.js'

const loading = ref(false)
const saving = ref(false)
const rows = ref([])
const upstreams = ref([])
const drawerVisible = ref(false)
const editingId = ref(null)
const form = reactive({
  host: '',
  pathPattern: '/**',
  upstreamId: null,
  priority: 0,
  redirectUrl: '',
  enabled: true
})

async function load() {
  loading.value = true
  try {
    rows.value = await listRoutes()
    upstreams.value = await listUpstreams()
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editingId.value = null
  Object.assign(form, { host: '', pathPattern: '/**', upstreamId: upstreams.value[0]?.id ?? null, priority: 0, redirectUrl: '', enabled: true })
  drawerVisible.value = true
}

function openEdit(row) {
  editingId.value = row.id
  Object.assign(form, {
    host: row.host,
    pathPattern: row.pathPattern,
    upstreamId: row.upstreamId,
    priority: row.priority,
    redirectUrl: row.redirectUrl || '',
    enabled: row.enabled
  })
  drawerVisible.value = true
}

async function save() {
  saving.value = true
  try {
    const body = { ...form, env: 'dev' }
    if (editingId.value) {
      await updateRoute(editingId.value, body)
    } else {
      await createRoute(body)
    }
    ElMessage.success('已保存，请点击顶部「发布配置」生效')
    drawerVisible.value = false
    await load()
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    saving.value = false
  }
}

async function onDelete(row) {
  await ElMessageBox.confirm(`确定删除路由 ${row.host}？`, '确认')
  try {
    await deleteRoute(row.id)
    ElMessage.success('已删除')
    await load()
  } catch (error) {
    ElMessage.error(error.message)
  }
}

onMounted(load)
window.addEventListener('alb-published', load)
</script>
