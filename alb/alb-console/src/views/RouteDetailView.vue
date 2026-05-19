<template>
  <YedsListPageCard title="Header 策略" tag="ALB">
    <YedsListToolbar>
      <el-button @click="$router.push('/routes')">返回路由列表</el-button>
      <el-button type="primary" @click="openCreate">添加 Header 策略</el-button>
    </YedsListToolbar>

    <p v-if="route" class="route-meta">路由：{{ route.host }}{{ route.pathPattern }}</p>

    <YedsListTableWrap>
      <el-table v-loading="loading" :data="headers" row-key="id">
        <el-table-column label="操作" width="140" fixed="left">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="onDelete(row)">删除</el-button>
          </template>
        </el-table-column>
        <el-table-column prop="direction" label="方向" width="100" />
        <el-table-column prop="op" label="操作" width="80" />
        <el-table-column prop="headerKey" label="Header Key" min-width="140" />
        <el-table-column prop="headerValue" label="Header Value" min-width="160" show-overflow-tooltip />
        <el-table-column prop="sortOrder" label="排序" width="70" />
        <el-table-column label="启用" width="70">
          <template #default="{ row }">
            <el-tag :type="row.enabled ? 'success' : 'info'" size="small">{{ row.enabled ? '是' : '否' }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
    </YedsListTableWrap>
  </YedsListPageCard>

  <el-drawer v-model="drawerVisible" :title="editingId ? '编辑 Header' : '添加 Header'" size="420px">
    <el-form label-position="top">
      <el-form-item label="方向">
        <el-select v-model="form.direction" style="width: 100%">
          <el-option label="请求" value="request" />
          <el-option label="响应" value="response" />
        </el-select>
      </el-form-item>
      <el-form-item label="操作">
        <el-select v-model="form.op" style="width: 100%">
          <el-option label="add" value="add" />
          <el-option label="set" value="set" />
          <el-option label="remove" value="remove" />
        </el-select>
      </el-form-item>
      <el-form-item label="Header Key">
        <el-input v-model="form.headerKey" />
      </el-form-item>
      <el-form-item label="Header Value">
        <el-input v-model="form.headerValue" />
      </el-form-item>
      <el-form-item label="排序">
        <el-input-number v-model="form.sortOrder" :min="0" />
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
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { YedsListPageCard, YedsListTableWrap, YedsListToolbar } from '@yeds/ui'
import { createHeader, deleteHeader, listHeaders, listRoutes, updateHeader } from '../api/albApi.js'

const vueRoute = useRoute()
const routeId = computed(() => Number(vueRoute.params.id))
const loading = ref(false)
const saving = ref(false)
const headers = ref([])
const allRoutes = ref([])
const drawerVisible = ref(false)
const editingId = ref(null)
const form = reactive({
  direction: 'request',
  op: 'set',
  headerKey: '',
  headerValue: '',
  sortOrder: 0,
  enabled: true
})

const route = computed(() => allRoutes.value.find((item) => item.id === routeId.value))

async function load() {
  loading.value = true
  try {
    allRoutes.value = await listRoutes()
    headers.value = await listHeaders(routeId.value)
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editingId.value = null
  Object.assign(form, { direction: 'request', op: 'set', headerKey: '', headerValue: '', sortOrder: 0, enabled: true })
  drawerVisible.value = true
}

function openEdit(row) {
  editingId.value = row.id
  Object.assign(form, {
    direction: row.direction,
    op: row.op,
    headerKey: row.headerKey,
    headerValue: row.headerValue || '',
    sortOrder: row.sortOrder,
    enabled: row.enabled
  })
  drawerVisible.value = true
}

async function save() {
  saving.value = true
  try {
    if (editingId.value) {
      await updateHeader(editingId.value, form)
    } else {
      await createHeader(routeId.value, form)
    }
    ElMessage.success('已保存，请发布配置')
    drawerVisible.value = false
    await load()
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    saving.value = false
  }
}

async function onDelete(row) {
  await ElMessageBox.confirm('确定删除该 Header 策略？', '确认')
  try {
    await deleteHeader(row.id)
    await load()
  } catch (error) {
    ElMessage.error(error.message)
  }
}

onMounted(load)
</script>

<style scoped>
.route-meta {
  margin: 0 0 12px;
  color: #606266;
  font-size: 14px;
}
</style>
