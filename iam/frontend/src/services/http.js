import { ElMessage } from 'element-plus'
import { getAuthorizationHeader } from '../stores/authStore'
import { recordApiResult } from '../stores/uiStore'
import { refreshAccessToken } from './auth'

const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL || '').replace(/\/$/, '')
let refreshingPromise = null

function toUrl(path) {
  if (path.startsWith('http://') || path.startsWith('https://')) {
    return path
  }
  return `${API_BASE_URL}${path}`
}

async function tryRefresh() {
  if (!refreshingPromise) {
    refreshingPromise = refreshAccessToken().finally(() => {
      refreshingPromise = null
    })
  }
  return refreshingPromise
}

export async function requestApi(options) {
  const {
    action,
    path,
    method = 'GET',
    body,
    auth = true,
    retryOnUnauthorized = true
  } = options
  const begin = performance.now()
  const headers = {}
  if (body !== undefined) {
    headers['Content-Type'] = 'application/json'
  }
  if (auth) {
    const token = getAuthorizationHeader()
    if (token) {
      headers.Authorization = token
    }
  }

  try {
    const response = await fetch(toUrl(path), {
      method,
      headers,
      body: body !== undefined ? JSON.stringify(body) : undefined
    })
    if (response.status === 401 && auth && retryOnUnauthorized) {
      const refreshed = await tryRefresh()
      if (refreshed) {
        return requestApi({
          ...options,
          retryOnUnauthorized: false
        })
      }
    }

    const payload = await response.json().catch(() => ({}))
    const result = {
      ok: response.ok,
      status: response.status,
      path,
      method,
      elapsedMs: Math.round(performance.now() - begin),
      requestId: response.headers.get('x-request-id') || payload?.requestId || '-',
      requestBody: body,
      payload
    }
    recordApiResult(action, result)
    if (!response.ok) {
      ElMessage.error(`${action}失败：${payload?.message || result.status}`)
    } else {
      ElMessage.success(`${action}成功`)
    }
    return result
  } catch (error) {
    const result = {
      ok: false,
      status: 0,
      path,
      method,
      elapsedMs: Math.round(performance.now() - begin),
      requestId: '-',
      requestBody: body,
      payload: { message: error?.message || String(error) }
    }
    recordApiResult(action, result)
    ElMessage.error(`${action}失败：${result.payload.message}`)
    return result
  }
}
