<template>
  <YedsListPageCard title="上游管理" tag="ALB">
    <YedsListToolbar>
      <el-button type="primary" @click="openCreate">新建上游</el-button>
      <el-button @click="$router.push('/routes')">返回路由</el-button>
    </YedsListToolbar>

    <YedsListTableWrap>
      <el-table v-loading="loading" :data="rows" row-key="id">
        <el-table-column label="操作" width="140" fixed="left">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="onDelete(row)">删除</el-button>
          </template>
        </el-table-column>
        <el-table-column prop="name" label="名称" width="120" />
        <el-table-column prop="targetUrl" label="目标 URL" min-width="220" show-overflow-tooltip />
        <el-table-column label="WebSocket" width="100">
          <template #default="{ row }">
            {{ row.websocketEnabled ? '是' : '否' }}
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="120" show-overflow-tooltip />
      </el-table>
    </YedsListTableWrap>
  </YedsListPageCard>

  <el-drawer v-model="drawerVisible" :title="editingId ? '编辑上游' : '新建上游'" size="420px">
    <el-form label-position="top">
      <el-form-item label="名称">
        <el-input v-model="form.name" />
      </el-form-item>
      <el-form-item label="目标 URL">
        <el-input v-model="form.targetUrl" placeholder="http://127.0.0.1:5173" />
      </el-form-item>
      <el-form-item label="WebSocket">
        <el-switch v-model="form.websocketEnabled" />
      </el-form-item>
      <el-form-item label="备注">
        <el-input v-model="form.remark" />
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
import { createUpstream, deleteUpstream, listUpstreams, updateUpstream } from '../api/albApi.js'

const loading = ref(false)
const saving = ref(false)
const rows = ref([])
const drawerVisible = ref(false)
const editingId = ref(null)
const form = reactive({ name: '', targetUrl: '', websocketEnabled: true, remark: '' })

async function load() {
  loading.value = true
  try {
    rows.value = await listUpstreams()
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editingId.value = null
  Object.assign(form, { name: '', targetUrl: '', websocketEnabled: true, remark: '' })
  drawerVisible.value = true
}

function openEdit(row) {
  editingId.value = row.id
  Object.assign(form, {
    name: row.name,
    targetUrl: row.targetUrl,
    websocketEnabled: row.websocketEnabled,
    remark: row.remark || ''
  })
  drawerVisible.value = true
}

async function save() {
  saving.value = true
  try {
    if (editingId.value) {
      await updateUpstream(editingId.value, form)
    } else {
      await createUpstream(form)
    }
    ElMessage.success('已保存')
    drawerVisible.value = false
    await load()
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    saving.value = false
  }
}

async function onDelete(row) {
  await ElMessageBox.confirm(`确定删除上游 ${row.name}？`, '确认')
  try {
    await deleteUpstream(row.id)
    await load()
  } catch (error) {
    ElMessage.error(error.message)
  }
}

onMounted(load)
</script>
