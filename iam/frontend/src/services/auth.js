import { clearSession, setSession, useAuthStore } from '../stores/authStore'

const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL || '').replace(/\/$/, '')

function toUrl(path) {
  if (path.startsWith('http://') || path.startsWith('https://')) {
    return path
  }
  return `${API_BASE_URL}${path}`
}

async function post(path, body) {
  const response = await fetch(toUrl(path), {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body)
  })
  const payload = await response.json().catch(() => ({}))
  return { ok: response.ok, status: response.status, payload }
}

export async function loginByPassword(form) {
  const result = await post('/api/iam/auth/login', {
    username: form.username,
    password: form.password,
    otpCode: form.otpCode || undefined
  })
  if (result.ok) {
    setSession(result.payload, form.username)
  }
  return result
}

export async function refreshAccessToken() {
  const authStore = useAuthStore()
  if (!authStore.refreshToken) {
    return false
  }
  const result = await post('/api/iam/auth/refresh', {
    refreshToken: authStore.refreshToken
  })
  if (!result.ok) {
    clearSession()
    return false
  }
  setSession(result.payload, authStore.username)
  return true
}

export async function logoutCurrentUser() {
  const authStore = useAuthStore()
  if (!authStore.refreshToken) {
    clearSession()
    return
  }
  await post('/api/iam/auth/logout', {
    refreshToken: authStore.refreshToken
  })
  clearSession()
}
