<template>
  <div class="page">
    <el-card class="card page-card">
      <div class="content-toolbar">
        <span class="content-title">{{ sectionTitle }}</span>
        <el-button type="primary" :loading="refreshing" @click="refreshAll">刷新</el-button>
      </div>

      <div v-show="activeMenu === 'ds'" class="content-block">
        <div class="block-toolbar">
          <el-button type="primary" @click="openDsDialog()">新建数据源</el-button>
        </div>
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
      </div>

      <div v-show="activeMenu === 'svc'" class="content-block">
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
      </div>

      <div v-show="activeMenu === 'run'" class="content-block">
        <el-table :data="publishedModels" border stripe class="mb-table">
          <el-table-column prop="code" label="服务编码" width="140" />
          <el-table-column prop="name" label="显示名称" min-width="140" />
          <el-table-column prop="datasourceCode" label="数据源" width="120" />
          <el-table-column prop="status" label="状态" width="100" />
          <el-table-column prop="updatedAt" label="更新时间" min-width="180" />
          <el-table-column label="操作" width="120" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="handleRunService(row)">运行</el-button>
              <el-button link type="primary" @click="openLogForModel(row)">日志</el-button>
            </template>
          </el-table-column>
        </el-table>

        <el-card v-if="formConfig" class="inner-card">
              <template #header>{{ formConfig.modelName }}</template>
              <el-form label-width="120px" :model="formValues">
                <el-divider content-position="left">分页参数</el-divider>
                <el-form-item label="当前页">
                  <el-input-number v-model="runPageNo" :min="1" :max="999999" controls-position="right" style="width: 200px" />
                </el-form-item>
                <el-form-item label="页大小">
                  <el-input-number
                    v-model="runPageSize"
                    :min="1"
                    controls-position="right"
                    style="width: 200px"
                    @change="onRunPageSizeFormChange"
                  />
                </el-form-item>
                <el-divider content-position="left">查询条件</el-divider>
                <el-form-item
                  v-for="field in formConfig.fields"
                  :key="field.fieldName"
                  :label="field.label"
                  :required="field.required"
                >
                  <el-input-number
                    v-if="field.fieldType === 'NUMBER'"
                    v-model="formValues[field.fieldName]"
                    controls-position="right"
                  />
                  <el-date-picker
                    v-else-if="field.fieldType === 'DATE'"
                    v-model="formValues[field.fieldName]"
                    type="date"
                    value-format="YYYY-MM-DD"
                  />
                  <el-date-picker
                    v-else-if="field.fieldType === 'DATETIME'"
                    v-model="formValues[field.fieldName]"
                    type="datetime"
                    value-format="YYYY-MM-DDTHH:mm:ss"
                  />
                  <el-select
                    v-else-if="field.fieldType === 'BOOLEAN' && field.required"
                    v-model="formValues[field.fieldName]"
                    placeholder="请选择"
                    style="width: 220px"
                  >
                    <el-option label="是" :value="true" />
                    <el-option label="否" :value="false" />
                  </el-select>
                  <el-switch v-else-if="field.fieldType === 'BOOLEAN'" v-model="formValues[field.fieldName]" />
                  <el-select
                    v-else-if="field.fieldType === 'SELECT'"
                    v-model="formValues[field.fieldName]"
                    clearable
                    filterable
                  >
                    <el-option
                      v-for="option in fieldSelectOptions(field)"
                      :key="String(option.value)"
                      :label="option.label"
                      :value="option.value"
                    />
                  </el-select>
                  <el-select
                    v-else-if="field.fieldType === 'TEXT' && (field.required || fieldSelectOptions(field).length > 0)"
                    v-model="formValues[field.fieldName]"
                    :multiple="isMultiProductCsv(field)"
                    filterable
                    allow-create
                    default-first-option
                    clearable
                    :placeholder="field.required ? '请选择或输入' : '可选：请选择或输入'"
                    style="width: 100%"
                    @change="(v) => onTextOptionsSelectChange(field, v)"
                  >
                    <el-option
                      v-if="isMultiProductCsv(field)"
                      :label="'全选'"
                      :value="MULTI_SELECT_ALL"
                    />
                    <el-option
                      v-for="option in fieldSelectOptions(field)"
                      :key="String(option.value)"
                      :label="option.label"
                      :value="option.value"
                    />
                  </el-select>
                  <el-input v-else v-model="formValues[field.fieldName]" clearable />
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" :loading="executing" @click="handleExecute">执行查询</el-button>
                </el-form-item>
              </el-form>
        </el-card>

        <el-card v-if="result" class="inner-card">
          <el-tabs v-model="resultTab">
            <el-tab-pane label="查询结果" name="result">
              <vxe-table border stripe height="420" :data="result.rows">
                <vxe-column
                  v-for="column in result.columns"
                  :key="column.columnName"
                  :field="column.columnName"
                  :title="column.label"
                  min-width="140"
                />
              </vxe-table>
              <el-pagination
                v-if="tablePagerTotal > 0"
                class="result-pager"
                v-model:current-page="runPageNo"
                v-model:page-size="runPageSize"
                :total="tablePagerTotal"
                :page-sizes="[50, 100, 200]"
                layout="total, sizes, prev, pager, next, jumper"
                background
                @size-change="onPagerSizeChange"
                @current-change="onPagerCurrentChange"
              />
            </el-tab-pane>
            <el-tab-pane label="原始响应" name="raw">
              <div class="json-code-block">
                <div class="json-toolbar">
                  <el-button size="small" @click="rawJsonExpanded = !rawJsonExpanded">
                    {{ rawJsonExpanded ? '收起' : '展开' }}
                  </el-button>
                  <el-button size="small" type="primary" @click="copyRawJson">复制 JSON</el-button>
                </div>
                <pre class="json-pre" :class="{ 'is-collapsed': !rawJsonExpanded }">{{ rawJsonFormatted }}</pre>
              </div>
            </el-tab-pane>
            <el-tab-pane label="运行脚本" name="script">
              <div class="sql-code-block">
                <el-button type="primary" size="small" class="sql-copy-inside" @click="copyFinalSql">复制</el-button>
                <pre class="sql-pre-block">{{ result.finalSql }}</pre>
              </div>
            </el-tab-pane>
          </el-tabs>
        </el-card>
      </div>
    </el-card>

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

    <el-dialog v-model="logDialogVisible" title="执行日志" width="720px" destroy-on-close>
      <el-descriptions v-if="logDetail" :column="1" border size="small">
        <el-descriptions-item label="执行编号">{{ logDetail.executeId }}</el-descriptions-item>
        <el-descriptions-item label="服务编码">{{ logDetail.modelCode }}</el-descriptions-item>
        <el-descriptions-item label="用户">{{ logDetail.username }}</el-descriptions-item>
        <el-descriptions-item label="成功">{{ logDetail.success ? '是' : '否' }}</el-descriptions-item>
        <el-descriptions-item label="错误信息">{{ logDetail.errorMessage || '—' }}</el-descriptions-item>
        <el-descriptions-item label="耗时 ms">{{ logDetail.durationMs }}</el-descriptions-item>
        <el-descriptions-item label="行数">{{ logDetail.rowCount }}</el-descriptions-item>
        <el-descriptions-item label="时间">{{ logDetail.createdAt }}</el-descriptions-item>
        <el-descriptions-item label="参数 JSON">
          <pre class="log-pre">{{ logDetail.parametersJson }}</pre>
        </el-descriptions-item>
        <el-descriptions-item label="最终 SQL">
          <pre class="log-pre">{{ logDetail.finalSql }}</pre>
        </el-descriptions-item>
      </el-descriptions>
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
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  createDataSource,
  createSqlModel,
  executeModel,
  fetchExecuteLog,
  getSqlModel,
  listDataSources,
  listSqlModels,
  loadForm,
  offlineSqlModel,
  publishSqlModel,
  updateSqlModel,
  validateSqlModel
} from '../api'
import { sessionAuthOpts } from '../sessionAuth.js'

/** 多选下拉中「全选」使用的哨兵值，不会作为真实参数提交 */
const MULTI_SELECT_ALL = '__BIDS_SELECT_ALL__'

const route = useRoute()

/** 当前分区：与路由 meta.menu 一致 */
const activeMenu = computed(() => route.meta.menu || 'svc')

const sectionTitle = computed(() => {
  const m = activeMenu.value
  if (m === 'ds') return '数据源'
  if (m === 'svc') return '服务配置'
  return '服务运行'
})

const dialects = ['MYSQL', 'POSTGRESQL', 'OPENGAUSS']
const dsDialog = ref(false)
const dsSaving = ref(false)
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

const queryState = reactive({
  modelCode: ''
})

const formConfig = ref(null)
const formValues = reactive({})
const result = ref(null)
const loadingForm = ref(false)
const executing = ref(false)
const refreshing = ref(false)
const datasources = ref([])
const models = ref([])
const lastExecuteIdByCode = reactive({})
const fieldTypes = ['TEXT', 'NUMBER', 'DATE', 'DATETIME', 'BOOLEAN', 'SELECT']
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
const logDialogVisible = ref(false)
const logDetail = ref(null)
/** 查询结果区 Tab：result=表格，raw=原始响应，script=运行脚本 */
const resultTab = ref('result')

/** 分页：默认每页条数；服务运行页大小前端上限 */
const DEFAULT_PAGE_SIZE = 200
const MAX_PAGE_SIZE = 200

const runPageNo = ref(1)
const runPageSize = ref(DEFAULT_PAGE_SIZE)
/** 最近一次接口完整 JSON（未截断行），供原始响应 Tab */
const apiResultSnapshot = ref(null)
/** 原始 JSON 是否展开（false 为预览折叠） */
const rawJsonExpanded = ref(false)

const publishedModels = computed(() => models.value.filter((m) => m.status === 'PUBLISHED'))

const tablePagerTotal = computed(() => Number(result.value?.total ?? 0))

const rawDisplayObject = computed(() => {
  const snap = apiResultSnapshot.value
  if (!snap) return null
  const rest = { ...snap }
  delete rest.executeId
  delete rest.finalSql
  return rest
})

const rawJsonFormatted = computed(() => {
  if (!rawDisplayObject.value) return ''
  try {
    return JSON.stringify(rawDisplayObject.value, null, 2)
  } catch {
    return ''
  }
})

function authOpts() {
  return sessionAuthOpts()
}

async function refreshAll(options = {}) {
  const { toast = true } = options
  refreshing.value = true
  try {
    datasources.value = await listDataSources(authOpts())
    models.value = await listSqlModels(authOpts())
    if (toast) ElMessage.success('已刷新')
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    refreshing.value = false
  }
}

onMounted(refreshAll)

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
    await refreshAll({ toast: false })
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    dsSaving.value = false
  }
}

async function handleRunService(row) {
  queryState.modelCode = row.code
  await handleLoadForm()
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

function buildModelPayload() {
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
    const body = buildModelPayload()
    if (modelId.value) {
      await updateSqlModel(modelId.value, body, authOpts())
      ElMessage.success('已保存')
    } else {
      const created = await createSqlModel(body, authOpts())
      modelId.value = created.model.id
      ElMessage.success('已创建，可继续编辑后发布')
    }
    await refreshAll({ toast: false })
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
    await refreshAll({ toast: false })
  } catch (e) {
    ElMessage.error(e.message)
  }
}

async function handleOffline(id) {
  try {
    await offlineSqlModel(id, authOpts())
    ElMessage.success('已下线')
    await refreshAll({ toast: false })
  } catch (e) {
    ElMessage.error(e.message)
  }
}

async function openLogForModel(row) {
  const executeId = lastExecuteIdByCode[row.code]
  if (!executeId) {
    ElMessage.warning('该服务在本页尚未成功执行过，暂无日志')
    return
  }
  try {
    const auth = authOpts()
    logDetail.value = await fetchExecuteLog(executeId, auth.username, auth.password)
    logDialogVisible.value = true
  } catch (e) {
    ElMessage.error(e.message)
  }
}

async function handleLoadForm() {
  if (!queryState.modelCode) {
    ElMessage.warning('请选择或输入服务编码')
    return
  }
  loadingForm.value = true
  try {
    const auth = authOpts()
    const data = await loadForm(queryState.modelCode, auth.username, auth.password)
    formConfig.value = data
    result.value = null
    apiResultSnapshot.value = null
    rawJsonExpanded.value = false
    runPageNo.value = 1
    runPageSize.value = DEFAULT_PAGE_SIZE
    Object.keys(formValues).forEach((key) => delete formValues[key])
    data.fields.forEach((field) => {
      const k = field.fieldName
      const dv = field.defaultValue
      const hasDef = dv !== null && dv !== undefined && dv !== ''

      if (field.fieldType === 'BOOLEAN') {
        if (hasDef) {
          formValues[k] = dv === true || dv === 'true'
        } else {
          formValues[k] = false
        }
      } else if (isMultiProductCsv(field)) {
        if (hasDef) {
          formValues[k] = String(dv)
            .split(',')
            .map((s) => s.trim())
            .filter(Boolean)
        } else {
          formValues[k] = fieldSelectOptions(field).map((o) => o.value)
        }
      } else if (field.fieldType === 'NUMBER') {
        if (hasDef) {
          const n = Number(dv)
          formValues[k] = Number.isFinite(n) ? n : null
        } else {
          formValues[k] = field.required ? null : 0
        }
      } else if (field.fieldType === 'DATE') {
        formValues[k] = hasDef ? String(dv) : todayStr()
      } else if (field.fieldType === 'DATETIME') {
        formValues[k] = hasDef ? String(dv) : null
      } else if (field.fieldType === 'SELECT') {
        const opts = fieldSelectOptions(field)
        if (hasDef) {
          formValues[k] = dv
        } else {
          formValues[k] = opts.length ? opts[0].value : null
        }
      } else if (field.fieldType === 'TEXT' && (field.required || fieldSelectOptions(field).length > 0)) {
        formValues[k] = hasDef ? dv : field.required ? null : ''
      } else {
        formValues[k] = hasDef ? dv : field.required ? null : ''
      }
    })
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    loadingForm.value = false
  }
}

async function handleExecute() {
  executing.value = true
  try {
    if (runPageSize.value > MAX_PAGE_SIZE) {
      ElMessage.warning(`页大小不能超过 ${MAX_PAGE_SIZE}`)
      runPageSize.value = MAX_PAGE_SIZE
    }
    const payload = { ...formValues }
    for (const k of Object.keys(payload)) {
      const v = payload[k]
      if (Array.isArray(v)) {
        payload[k] = v.filter((x) => x !== MULTI_SELECT_ALL).join(',')
      }
    }
    const auth = authOpts()
    const data = await executeModel(queryState.modelCode, payload, auth.username, auth.password, {
      currentPage: runPageNo.value,
      pageSize: Math.min(runPageSize.value, MAX_PAGE_SIZE)
    })
    try {
      apiResultSnapshot.value =
        typeof structuredClone === 'function' ? structuredClone(data) : JSON.parse(JSON.stringify(data))
    } catch {
      apiResultSnapshot.value = { ...data }
    }
    result.value = data
    if (data.currentPage != null) runPageNo.value = data.currentPage
    if (data.pageSize != null) runPageSize.value = Math.min(data.pageSize, MAX_PAGE_SIZE)
    rawJsonExpanded.value = false
    resultTab.value = 'result'
    if (data.executeId) {
      lastExecuteIdByCode[queryState.modelCode] = data.executeId
    }
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    executing.value = false
  }
}

function fieldSelectOptions(field) {
  if (field.optionItems && field.optionItems.length > 0) {
    return field.optionItems.map((o) => ({ label: o.label, value: o.value }))
  }
  return parseOptions(field.optionsJson)
}

function isMultiProductCsv(field) {
  try {
    const j = JSON.parse(field.optionsJson || '{}')
    return j.distinctFrom?.multiple === true
  } catch {
    return false
  }
}

/**
 * 多选 TEXT 下拉：通过「全选」项一次全选或全不选（与选项列表一致时再次选全选则清空）。
 * @param {*} field 表单字段配置
 * @param {unknown} val
 */
function onTextOptionsSelectChange(field, val) {
  if (!isMultiProductCsv(field)) return
  const opts = fieldSelectOptions(field).map((o) => o.value)
  const key = field.fieldName
  if (!Array.isArray(val)) return
  if (!val.includes(MULTI_SELECT_ALL)) return
  const rest = val.filter((v) => v !== MULTI_SELECT_ALL)
  const allPicked = opts.length > 0 && rest.length === opts.length
  formValues[key] = allPicked ? [] : [...opts]
}

function parseOptions(optionsJson) {
  if (!optionsJson) {
    return []
  }
  try {
    return JSON.parse(optionsJson)
  } catch {
    return []
  }
}

/** 本地日期 YYYY-MM-DD（DATE 默认值） */
function todayStr() {
  const d = new Date()
  const z = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${z(d.getMonth() + 1)}-${z(d.getDate())}`
}

function onTablePageSizeChange(size) {
  const n = Number(size)
  const raw = Number.isFinite(n) && n > 0 ? n : DEFAULT_PAGE_SIZE
  if (raw > MAX_PAGE_SIZE) {
    ElMessage.warning(`页大小不能超过 ${MAX_PAGE_SIZE}`)
  }
  runPageSize.value = Math.min(raw, MAX_PAGE_SIZE)
  runPageNo.value = 1
}

function onRunPageSizeFormChange() {
  const v = runPageSize.value
  if (typeof v !== 'number' || !Number.isFinite(v)) return
  if (v > MAX_PAGE_SIZE) {
    ElMessage.warning(`页大小不能超过 ${MAX_PAGE_SIZE}`)
    runPageSize.value = MAX_PAGE_SIZE
  }
}

async function onPagerSizeChange(size) {
  if (executing.value) return
  onTablePageSizeChange(size)
  await handleExecute()
}

async function onPagerCurrentChange() {
  if (executing.value) return
  await handleExecute()
}

async function copyRawJson() {
  const text = rawJsonFormatted.value
  if (!text) {
    ElMessage.warning('无原始响应数据')
    return
  }
  try {
    await navigator.clipboard.writeText(text)
    ElMessage.success('已复制到剪贴板')
  } catch {
    try {
      const ta = document.createElement('textarea')
      ta.value = text
      ta.style.position = 'fixed'
      ta.style.left = '-9999px'
      document.body.appendChild(ta)
      ta.select()
      document.execCommand('copy')
      document.body.removeChild(ta)
      ElMessage.success('已复制到剪贴板')
    } catch {
      ElMessage.error('复制失败')
    }
  }
}

async function copyFinalSql() {
  const text = result.value?.finalSql ?? ''
  if (!text) {
    ElMessage.warning('无可复制内容')
    return
  }
  try {
    await navigator.clipboard.writeText(text)
    ElMessage.success('已复制到剪贴板')
  } catch {
    try {
      const ta = document.createElement('textarea')
      ta.value = text
      ta.style.position = 'fixed'
      ta.style.left = '-9999px'
      document.body.appendChild(ta)
      ta.select()
      document.execCommand('copy')
      document.body.removeChild(ta)
      ElMessage.success('已复制到剪贴板')
    } catch {
      ElMessage.error('复制失败')
    }
  }
}
</script>

<style scoped>
.page-card :deep(.el-card__body) {
  padding-top: 12px;
}
.content-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--el-border-color-lighter);
}
.content-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}
.content-block {
  min-height: 200px;
}
.block-toolbar {
  margin-bottom: 12px;
}
.mb {
  margin-bottom: 12px;
}
.mb-table {
  margin-bottom: 16px;
}
.inner-card {
  margin-bottom: 16px;
}
.result-pager {
  margin-top: 12px;
  justify-content: flex-end;
}
.json-code-block {
  border: 1px solid var(--el-border-color);
  border-radius: 6px;
  background: var(--el-fill-color-blank);
}
.json-toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding: 8px 12px;
  border-bottom: 1px solid var(--el-border-color-lighter);
}
.json-pre {
  margin: 0;
  padding: 12px;
  font-size: 12px;
  line-height: 1.5;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  white-space: pre-wrap;
  word-break: break-all;
  overflow: auto;
  max-height: 560px;
  transition: max-height 0.2s ease;
}
.json-pre.is-collapsed {
  max-height: 120px;
  overflow: hidden;
}
.sql-code-block {
  position: relative;
  background: #111827;
  color: #d1d5db;
  border-radius: 6px;
  padding: 12px;
  min-height: 100px;
}
.sql-copy-inside {
  position: absolute;
  top: 10px;
  right: 10px;
  z-index: 1;
}
.sql-pre-block {
  margin: 0;
  padding: 40px 8px 8px;
  white-space: pre-wrap;
  word-break: break-all;
  overflow: auto;
  max-height: 480px;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 13px;
  line-height: 1.45;
}
.log-pre {
  margin: 0;
  max-height: 200px;
  overflow: auto;
  font-size: 12px;
  white-space: pre-wrap;
  word-break: break-all;
}
</style>
