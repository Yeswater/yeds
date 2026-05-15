<template>
  <div class="page">
    <el-card class="card">
      <el-form :inline="true" :model="queryState">
        <el-form-item label="模型编码">
          <el-select
            v-model="queryState.modelCode"
            filterable
            allow-create
            default-first-option
            clearable
            placeholder="请选择或输入模型编码"
            style="width: 280px"
          >
            <el-option
              v-for="opt in publishedModelOptions"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="用户">
          <el-input v-model="queryState.username" placeholder="Basic 用户名" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="queryState.password" type="password" placeholder="Basic 密码" show-password />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loadingForm" @click="handleLoadForm">加载表单</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card v-if="formConfig" class="card">
      <template #header>{{ formConfig.modelName }}</template>
      <el-form label-width="120px" :model="formValues">
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
          >
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

    <el-card v-if="result" class="card">
      <template #header>查询结果</template>
      <vxe-table border stripe height="420" :data="result.rows">
        <vxe-column
          v-for="column in result.columns"
          :key="column.columnName"
          :field="column.columnName"
          :title="column.label"
          min-width="140"
        />
      </vxe-table>
    </el-card>

    <el-card v-if="result" class="card">
      <template #header>最终 SQL</template>
      <pre class="sql-box">{{ result.finalSql }}</pre>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { executeModel, listSqlModels, loadForm } from '../api'

const queryState = reactive({
  modelCode: '',
  username: 'admin',
  password: 'admin'
})
const formConfig = ref(null)
const formValues = reactive({})
const result = ref(null)
const loadingForm = ref(false)
const executing = ref(false)
/** 已发布模型，供模型编码下拉 */
const publishedModelOptions = ref([])

async function refreshPublishedModels() {
  try {
    const list = await listSqlModels({
      username: queryState.username,
      password: queryState.password
    })
    publishedModelOptions.value = list
      .filter((m) => m.status === 'PUBLISHED')
      .map((m) => ({ label: `${m.code}（${m.name}）`, value: m.code }))
  } catch {
    publishedModelOptions.value = []
  }
}

onMounted(refreshPublishedModels)
watch(
  () => [queryState.username, queryState.password],
  () => {
    refreshPublishedModels()
  }
)

async function handleLoadForm() {
  if (!queryState.modelCode) {
    ElMessage.warning('请选择或输入模型编码')
    return
  }
  loadingForm.value = true
  try {
    const data = await loadForm(queryState.modelCode, queryState.username, queryState.password)
    formConfig.value = data
    result.value = null
    Object.keys(formValues).forEach((key) => delete formValues[key])
    data.fields.forEach((field) => {
      if (field.fieldType === 'BOOLEAN') {
        const d = field.defaultValue
        if (d === true || d === 'true') {
          formValues[field.fieldName] = true
        } else if (d === false || d === 'false') {
          formValues[field.fieldName] = false
        } else {
          formValues[field.fieldName] = field.required ? false : null
        }
      } else if (isMultiProductCsv(field)) {
        const d = field.defaultValue
        formValues[field.fieldName] = d
          ? String(d)
              .split(',')
              .map((s) => s.trim())
              .filter(Boolean)
          : []
      } else {
        formValues[field.fieldName] = field.defaultValue || null
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
    const payload = { ...formValues }
    for (const k of Object.keys(payload)) {
      const v = payload[k]
      if (Array.isArray(v)) {
        payload[k] = v.join(',')
      }
    }
    result.value = await executeModel(queryState.modelCode, payload, queryState.username, queryState.password)
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
</script>
