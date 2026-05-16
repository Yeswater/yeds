import { buildSyncExportFileName, parseContentDispositionFileName } from './dateFormat.js'

const API_ROOT = import.meta.env.VITE_API_BASE ?? ''

function basicAuth(username, password) {
  return `Basic ${btoa(`${username}:${password}`)}`
}

async function http(path, { method = 'GET', username, password, body } = {}) {
  const response = await fetch(`${API_ROOT}${path}`, {
    method,
    headers: {
      'Content-Type': 'application/json',
      Authorization: basicAuth(username || 'admin', password || 'admin')
    },
    body
  })
  const data = await response.json().catch(() => ({}))
  if (!response.ok) {
    throw new Error(data.message || `请求失败 ${response.status}`)
  }
  return data
}

export async function loadForm(modelCode, username, password) {
  return http(`/api/runtime/models/${encodeURIComponent(modelCode)}/form`, {
    method: 'GET',
    username,
    password
  })
}

export async function executeModel(modelCode, parameters, username, password, paging = {}) {
  const currentPage = paging.currentPage ?? 1
  const pageSize = paging.pageSize ?? 200
  return http(`/api/runtime/models/${encodeURIComponent(modelCode)}/execute`, {
    method: 'POST',
    username,
    password,
    body: JSON.stringify({ parameters, currentPage, pageSize })
  })
}

/** 按执行编号拉取审计日志（与 execute 返回的 executeId 对应）。 */
export async function fetchExecuteLog(executeId, username, password) {
  return http(`/api/runtime/logs/${encodeURIComponent(executeId)}`, {
    method: 'GET',
    username,
    password
  })
}

export async function listDataSources(auth) {
  return http('/api/config/datasources', { method: 'GET', ...auth })
}

export async function createDataSource(payload, auth) {
  return http('/api/config/datasources', {
    method: 'POST',
    ...auth,
    body: JSON.stringify(payload)
  })
}

export async function listSqlModels(auth) {
  return http('/api/config/models', { method: 'GET', ...auth })
}

export async function getSqlModel(id, auth) {
  return http(`/api/config/models/${encodeURIComponent(id)}`, { method: 'GET', ...auth })
}

export async function createSqlModel(payload, auth) {
  return http('/api/config/models', {
    method: 'POST',
    ...auth,
    body: JSON.stringify(payload)
  })
}

export async function updateSqlModel(id, payload, auth) {
  return http(`/api/config/models/${encodeURIComponent(id)}`, {
    method: 'PUT',
    ...auth,
    body: JSON.stringify(payload)
  })
}

export async function validateSqlModel(id, auth) {
  return http(`/api/config/models/${encodeURIComponent(id)}/validate`, {
    method: 'POST',
    ...auth
  })
}

export async function publishSqlModel(id, auth) {
  return http(`/api/config/models/${encodeURIComponent(id)}/publish`, {
    method: 'POST',
    ...auth
  })
}

export async function offlineSqlModel(id, auth) {
  return http(`/api/config/models/${encodeURIComponent(id)}/offline`, {
    method: 'POST',
    ...auth
  })
}

export async function estimateExport(modelCode, parameters, username, password) {
  return http(`/api/runtime/models/${encodeURIComponent(modelCode)}/export/estimate`, {
    method: 'POST',
    username,
    password,
    body: JSON.stringify({ parameters })
  })
}

export async function syncExportModel(modelCode, parameters, username, password) {
  const response = await fetch(`${API_ROOT}/api/runtime/models/${encodeURIComponent(modelCode)}/export`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Authorization: basicAuth(username || 'admin', password || 'admin')
    },
    body: JSON.stringify({ parameters })
  })
  if (!response.ok) {
    const data = await response.json().catch(() => ({}))
    throw new Error(data.message || `导出失败 ${response.status}`)
  }
  const blob = await response.blob()
  const disposition = response.headers.get('Content-Disposition') || ''
  const fileName = parseContentDispositionFileName(disposition, buildSyncExportFileName(modelCode))
  return { blob, fileName }
}

export async function createExportTask(modelCode, parameters, username, password) {
  return http(`/api/runtime/models/${encodeURIComponent(modelCode)}/export/tasks`, {
    method: 'POST',
    username,
    password,
    body: JSON.stringify({ parameters })
  })
}

export async function getExportTask(taskId, username, password) {
  return http(`/api/runtime/export/tasks/${encodeURIComponent(taskId)}`, {
    method: 'GET',
    username,
    password
  })
}

export async function listExportTasks(limit, username, password) {
  return http(`/api/runtime/export/tasks?limit=${limit}`, {
    method: 'GET',
    username,
    password
  })
}

export function exportTaskDownloadUrl(taskId) {
  return `${API_ROOT}/api/runtime/export/tasks/${encodeURIComponent(taskId)}/download`
}
