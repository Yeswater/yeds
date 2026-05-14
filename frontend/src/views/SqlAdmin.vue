<template>
  <div class="page">
    <el-card class="card">
      <el-form :inline="true">
        <el-form-item label="管理员账号">
          <el-input v-model="auth.username" style="width: 140px" />
        </el-form-item>
        <el-form-item label="管理员密码">
          <el-input v-model="auth.password" type="password" show-password style="width: 160px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="refreshAll">刷新</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-tabs v-model="activeTab">
      <el-tab-pane label="数据源" name="ds">
        <el-button type="primary" class="mb" @click="openDsDialog()">新建数据源</el-button>
        <el-table :data="datasources" border stripe>
          <el-table-column prop="code" label="编码" width="120" />
          <el-table-column prop="name" label="名称" min-width="140" />
          <el-table-column prop="jdbcUrl" label="JDBC URL" min-width="260" show-overflow-tooltip />
          <el-table-column prop="username" label="用户" width="100" />
          <el-table-column prop="sqlDialect" label="方言" width="110" />
          <el-table-column prop="driverClassName" label="驱动" min-width="180" show-overflow-tooltip />
          <el-table-column prop="maxPoolSize" label="池大小" width="90" />
          <el-table-column prop="active" label="启用" width="80" />
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="SQL 服务（模型）" name="models">
        <el-button type="primary" class="mb" @click="openModelDrawer()">新建 SQL 服务</el-button>
        <el-table :data="models" border stripe>
          <el-table-column prop="code" label="服务编码" width="140" />
          <el-table-column prop="name" label="显示名称" min-width="140" />
          <el-table-column prop="datasourceCode" label="数据源" width="120" />
          <el-table-column prop="status" label="状态" width="100" />
          <el-table-column prop="updatedAt" label="更新时间" min-width="180" />
          <el-table-column label="操作" width="280" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="openModelDrawer(row.id)">编辑</el-button>
              <el-button link type="primary" @click="handleValidate(row.id)">校验</el-button>
              <el-button link type="success" @click="handlePublish(row.id)">发布</el-button>
              <el-button link type="warning" @click="handleOffline(row.id)">下线</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="dsDialog" title="数据源" width="640px" @close="resetDsForm">
      <el-form :model="dsForm" label-width="120px">
        <el-form-item label="编码" required>
          <el-input v-model="dsForm.code" placeholder="唯一标识，如 local / prod-mysql" />
        </el-form-item>
        <el-form-item label="名称" required>
          <el-input v-model="dsForm.name" />
        </el-form-item>
        <el-form-item label="JDBC URL" required>
          <el-input v-model="dsForm.jdbcUrl" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="用户名" required>
          <el-input v-model="dsForm.username" />
        </el-form-item>
        <el-form-item label="密码" required>
          <el-input v-model="dsForm.password" type="password" show-password />
        </el-form-item>
        <el-form-item label="驱动类" required>
          <el-input v-model="dsForm.driverClassName" />
        </el-form-item>
        <el-form-item label="SQL 方言" required>
          <el-select v-model="dsForm.sqlDialect" style="width: 100%">
            <el-option v-for="d in dialects" :key="d" :label="d" :value="d" />
          </el-select>
        </el-form-item>
        <el-form-item label="最大连接数">
          <el-input-number v-model="dsForm.maxPoolSize" :min="1" :max="50" />
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="dsForm.active" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dsDialog = false">取消</el-button>
        <el-button type="primary" :loading="dsSaving" @click="saveDatasource">保存</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="modelDrawer" :title="modelId ? '编辑 SQL 服务' : '新建 SQL 服务'" size="70%">
      <el-form label-width="120px">
        <el-divider content-position="left">基本信息</el-divider>
        <el-form-item label="服务编码">
          <el-input v-model="modelForm.code" placeholder="对外 API 路径中的 modelCode" />
        </el-form-item>
        <el-form-item label="显示名称">
          <el-input v-model="modelForm.name" />
        </el-form-item>
        <el-form-item label="数据源">
          <el-select v-model="modelForm.datasourceCode" filterable style="width: 100%">
            <el-option v-for="d in datasources" :key="d.code" :label="`${d.code} — ${d.name}`" :value="d.code" />
          </el-select>
        </el-form-item>
        <el-form-item label="最大行数">
          <el-input-number v-model="modelForm.maxRows" :min="1" :max="10000" />
        </el-form-item>
        <el-form-item label="SQL 模板">
          <el-input v-model="modelForm.sqlTemplate" type="textarea" :rows="10" placeholder="Freemarker 模板，命名参数 :param" />
        </el-form-item>

        <el-divider content-position="left">入参（表单字段）</el-divider>
        <el-button class="mb" @click="addField">添加入参</el-button>
        <el-table :data="modelForm.fields" border size="small">
          <el-table-column label="参数名" width="140">
            <template #default="{ row }"><el-input v-model="row.fieldName" /></template>
          </el-table-column>
          <el-table-column label="标签" width="120">
            <template #default="{ row }"><el-input v-model="row.label" /></template>
          </el-table-column>
          <el-table-column label="类型" width="130">
            <template #default="{ row }">
              <el-select v-model="row.fieldType" style="width: 100%">
                <el-option v-for="t in fieldTypes" :key="t" :label="t" :value="t" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="必填" width="80">
            <template #default="{ row }"><el-switch v-model="row.required" /></template>
          </el-table-column>
          <el-table-column label="默认值" width="120">
            <template #default="{ row }"><el-input v-model="row.defaultValue" /></template>
          </el-table-column>
          <el-table-column label="下拉 JSON" min-width="160">
            <template #default="{ row }"><el-input v-model="row.optionsJson" placeholder='[{"label":"","value":""}]' /></template>
          </el-table-column>
          <el-table-column label="" width="70">
            <template #default="{ $index }">
              <el-button link type="danger" @click="modelForm.fields.splice($index, 1)">删</el-button>
            </template>
          </el-table-column>
        </el-table>

        <el-divider content-position="left">出参（结果列）</el-divider>
        <el-button class="mb" @click="addColumn">添加出参</el-button>
        <el-table :data="modelForm.columns" border size="small">
          <el-table-column label="列名" width="140">
            <template #default="{ row }"><el-input v-model="row.columnName" /></template>
          </el-table-column>
          <el-table-column label="标签" width="120">
            <template #default="{ row }"><el-input v-model="row.label" /></template>
          </el-table-column>
          <el-table-column label="类型" width="130">
            <template #default="{ row }">
              <el-select v-model="row.valueType" style="width: 100%">
                <el-option v-for="t in fieldTypes" :key="t" :label="t" :value="t" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="展示" width="80">
            <template #default="{ row }"><el-switch v-model="row.visible" /></template>
          </el-table-column>
          <el-table-column label="脱敏" width="100">
            <template #default="{ row }"><el-input v-model="row.maskType" placeholder="PHONE/EMAIL" /></template>
          </el-table-column>
          <el-table-column label="" width="70">
            <template #default="{ $index }">
              <el-button link type="danger" @click="modelForm.columns.splice($index, 1)">删</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-form>
      <template #footer>
        <div style="text-align: right">
          <el-button @click="modelDrawer = false">关闭</el-button>
          <el-button type="primary" :loading="modelSaving" @click="saveModel">保存草稿</el-button>
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  listDataSources,
  createDataSource,
  listSqlModels,
  getSqlModel,
  createSqlModel,
  updateSqlModel,
  validateSqlModel,
  publishSqlModel,
  offlineSqlModel
} from '../api'

const auth = reactive({ username: 'admin', password: 'admin' })
const activeTab = ref('ds')
const datasources = ref([])
const models = ref([])
const dsDialog = ref(false)
const dsSaving = ref(false)
const dialects = ['MYSQL', 'POSTGRESQL', 'OPENGAUSS']
const fieldTypes = ['TEXT', 'NUMBER', 'DATE', 'DATETIME', 'BOOLEAN', 'SELECT']

const dsForm = reactive({
  code: '',
  name: '',
  jdbcUrl: 'jdbc:mysql://127.0.0.1:3306/bids?useUnicode=true&characterEncoding=utf8&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC',
  username: 'bids',
  password: 'bids',
  driverClassName: 'com.mysql.cj.jdbc.Driver',
  sqlDialect: 'MYSQL',
  maxPoolSize: 10,
  active: true
})

const modelDrawer = ref(false)
const modelId = ref('')
const modelSaving = ref(false)
const modelForm = reactive({
  code: '',
  name: '',
  datasourceCode: '',
  sqlTemplate: 'select 1',
  maxRows: 500,
  fields: [],
  columns: []
})

function authOpts() {
  return { username: auth.username, password: auth.password }
}

async function refreshAll() {
  try {
    datasources.value = await listDataSources(authOpts())
    models.value = await listSqlModels(authOpts())
    ElMessage.success('已刷新')
  } catch (e) {
    ElMessage.error(e.message)
  }
}

function resetDsForm() {
  Object.assign(dsForm, {
    code: '',
    name: '',
    jdbcUrl: 'jdbc:mysql://127.0.0.1:3306/bids?useUnicode=true&characterEncoding=utf8&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC',
    username: 'bids',
    password: 'bids',
    driverClassName: 'com.mysql.cj.jdbc.Driver',
    sqlDialect: 'MYSQL',
    maxPoolSize: 10,
    active: true
  })
}

function openDsDialog() {
  resetDsForm()
  dsDialog.value = true
}

async function saveDatasource() {
  dsSaving.value = true
  try {
    await createDataSource({ ...dsForm }, authOpts())
    ElMessage.success('数据源已创建')
    dsDialog.value = false
    await refreshAll()
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    dsSaving.value = false
  }
}

function emptyField(i) {
  return { fieldName: '', label: '', fieldType: 'TEXT', required: false, defaultValue: '', optionsJson: '', sortOrder: i }
}
function emptyColumn(i) {
  return { columnName: '', label: '', valueType: 'TEXT', visible: true, maskType: '', sortOrder: i }
}

async function openModelDrawer(id) {
  modelId.value = id || ''
  modelForm.code = ''
  modelForm.name = ''
  modelForm.datasourceCode = datasources.value[0]?.code || ''
  modelForm.sqlTemplate = 'select 1'
  modelForm.maxRows = 500
  modelForm.fields = [emptyField(0)]
  modelForm.columns = [emptyColumn(0)]
  modelDrawer.value = true
  if (id) {
    await loadModel(id)
  }
}

async function loadModel(id) {
  try {
    const c = await getSqlModel(id, authOpts())
    const m = c.model
    modelForm.code = m.code
    modelForm.name = m.name
    modelForm.datasourceCode = m.datasourceCode
    modelForm.sqlTemplate = m.sqlTemplate
    modelForm.maxRows = m.maxRows
    modelForm.fields = (c.fields || []).map((f, i) => ({
      fieldName: f.fieldName,
      label: f.label,
      fieldType: f.fieldType,
      required: f.required,
      defaultValue: f.defaultValue || '',
      optionsJson: f.optionsJson || '',
      sortOrder: f.sortOrder ?? i
    }))
    modelForm.columns = (c.columns || []).map((col, i) => ({
      columnName: col.columnName,
      label: col.label,
      valueType: col.valueType || 'TEXT',
      visible: col.visible !== false,
      maskType: col.maskType || '',
      sortOrder: col.sortOrder ?? i
    }))
    if (!modelForm.fields.length) modelForm.fields = [emptyField(0)]
    if (!modelForm.columns.length) modelForm.columns = [emptyColumn(0)]
  } catch (e) {
    ElMessage.error(e.message)
  }
}

function addField() {
  modelForm.fields.push(emptyField(modelForm.fields.length))
}
function addColumn() {
  modelForm.columns.push(emptyColumn(modelForm.columns.length))
}

function buildPayload() {
  modelForm.fields.forEach((f, i) => {
    f.sortOrder = i
  })
  modelForm.columns.forEach((c, i) => {
    c.sortOrder = i
  })
  return {
    code: modelForm.code,
    name: modelForm.name,
    datasourceCode: modelForm.datasourceCode,
    sqlTemplate: modelForm.sqlTemplate,
    maxRows: modelForm.maxRows,
    fields: modelForm.fields.filter((f) => f.fieldName && f.label),
    columns: modelForm.columns.filter((c) => c.columnName && c.label),
    permissions: []
  }
}

async function saveModel() {
  modelSaving.value = true
  try {
    const body = buildPayload()
    if (modelId.value) {
      await updateSqlModel(modelId.value, body, authOpts())
      ElMessage.success('已保存')
    } else {
      const created = await createSqlModel(body, authOpts())
      modelId.value = created.model.id
      ElMessage.success('已创建，可继续编辑后发布')
    }
    await refreshAll()
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    modelSaving.value = false
  }
}

async function handleValidate(id) {
  try {
    const r = await validateSqlModel(id, authOpts())
    ElMessage.success(r.message || '校验通过')
  } catch (e) {
    ElMessage.error(e.message)
  }
}

async function handlePublish(id) {
  try {
    await publishSqlModel(id, authOpts())
    ElMessage.success('已发布')
    await refreshAll()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

async function handleOffline(id) {
  try {
    await offlineSqlModel(id, authOpts())
    ElMessage.success('已下线')
    await refreshAll()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

onMounted(refreshAll)
</script>

<style scoped>
.mb {
  margin-bottom: 12px;
}
</style>
