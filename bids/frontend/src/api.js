import { buildSyncExportFileName, parseContentDispositionFileName } from './dateFormat.js'
import { refreshSessionIfNeeded } from './iamSession.js'

const API_ROOT = import.meta.env.VITE_API_BASE ?? ''

function bearerAuth(accessToken) {
  return `Bearer ${accessToken}`
}

async function http(path, { method = 'GET', auth, body } = {}) {
  let activeAuth = await refreshSessionIfNeeded(auth, false)
  let response = await fetch(`${API_ROOT}${path}`, {
    method,
    headers: {
      'Content-Type': 'application/json',
      Authorization: bearerAuth(activeAuth.accessToken)
    },
    body
  })
  if (response.status === 401) {
    activeAuth = await refreshSessionIfNeeded(activeAuth, true)
    response = await fetch(`${API_ROOT}${path}`, {
      method,
      headers: {
        'Content-Type': 'application/json',
        Authorization: bearerAuth(activeAuth.accessToken)
      },
      body
    })
  }
  const data = await response.json().catch(() => ({}))
  if (!response.ok) {
    throw new Error(data.message || `请求失败 ${response.status}`)
  }
  return data
}

export async function loadForm(modelCode, auth) {
  return http(`/api/runtime/models/${encodeURIComponent(modelCode)}/form`, {
    method: 'GET',
    auth
  })
}

export async function executeModel(modelCode, parameters, auth, paging = {}) {
  const currentPage = paging.currentPage ?? 1
  const pageSize = paging.pageSize ?? 200
  return http(`/api/runtime/models/${encodeURIComponent(modelCode)}/execute`, {
    method: 'POST',
    auth,
    body: JSON.stringify({ parameters, currentPage, pageSize })
  })
}

/** 按执行编号拉取审计日志（与 execute 返回的 executeId 对应）。 */
export async function fetchExecuteLog(executeId, auth) {
  return http(`/api/runtime/logs/${encodeURIComponent(executeId)}`, {
    method: 'GET',
    auth
  })
}

export async function listDataSources(auth) {
  return http('/api/config/datasources', { method: 'GET', auth })
}

export async function createDataSource(payload, auth) {
  return http('/api/config/datasources', {
    method: 'POST',
    auth,
    body: JSON.stringify(payload)
  })
}

export async function listSqlModels(auth) {
  return http('/api/config/models', { method: 'GET', auth })
}

export async function getSqlModel(id, auth) {
  return http(`/api/config/models/${encodeURIComponent(id)}`, { method: 'GET', auth })
}

export async function createSqlModel(payload, auth) {
  return http('/api/config/models', {
    method: 'POST',
    auth,
    body: JSON.stringify(payload)
  })
}

export async function updateSqlModel(id, payload, auth) {
  return http(`/api/config/models/${encodeURIComponent(id)}`, {
    method: 'PUT',
    auth,
    body: JSON.stringify(payload)
  })
}

export async function validateSqlModel(id, auth) {
  return http(`/api/config/models/${encodeURIComponent(id)}/validate`, {
    method: 'POST',
    auth
  })
}

export async function publishSqlModel(id, auth) {
  return http(`/api/config/models/${encodeURIComponent(id)}/publish`, {
    method: 'POST',
    auth
  })
}

export async function offlineSqlModel(id, auth) {
  return http(`/api/config/models/${encodeURIComponent(id)}/offline`, {
    method: 'POST',
    auth
  })
}

export async function estimateExport(modelCode, parameters, auth) {
  return http(`/api/runtime/models/${encodeURIComponent(modelCode)}/export/estimate`, {
    method: 'POST',
    auth,
    body: JSON.stringify({ parameters })
  })
}

export async function syncExportModel(modelCode, parameters, auth) {
  let activeAuth = await refreshSessionIfNeeded(auth, false)
  let response = await fetch(`${API_ROOT}/api/runtime/models/${encodeURIComponent(modelCode)}/export`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Authorization: bearerAuth(activeAuth.accessToken)
    },
    body: JSON.stringify({ parameters })
  })
  if (response.status === 401) {
    activeAuth = await refreshSessionIfNeeded(activeAuth, true)
    response = await fetch(`${API_ROOT}/api/runtime/models/${encodeURIComponent(modelCode)}/export`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: bearerAuth(activeAuth.accessToken)
      },
      body: JSON.stringify({ parameters })
    })
  }
  if (!response.ok) {
    const data = await response.json().catch(() => ({}))
    throw new Error(data.message || `导出失败 ${response.status}`)
  }
  const blob = await response.blob()
  const disposition = response.headers.get('Content-Disposition') || ''
  const fileName = parseContentDispositionFileName(disposition, buildSyncExportFileName(modelCode))
  return { blob, fileName }
}

export async function createExportTask(modelCode, parameters, auth) {
  return http(`/api/runtime/models/${encodeURIComponent(modelCode)}/export/tasks`, {
    method: 'POST',
    auth,
    body: JSON.stringify({ parameters })
  })
}

export async function getExportTask(taskId, auth) {
  return http(`/api/runtime/export/tasks/${encodeURIComponent(taskId)}`, {
    method: 'GET',
    auth
  })
}

export async function listExportTasks(limit, auth) {
  return http(`/api/runtime/export/tasks?limit=${limit}`, {
    method: 'GET',
    auth
  })
}

export function exportTaskDownloadUrl(taskId) {
  return `${API_ROOT}/api/runtime/export/tasks/${encodeURIComponent(taskId)}/download`
}
