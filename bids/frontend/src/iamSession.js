import { clearSession, isSessionExpired, readSession, writeSession } from './authStorage.js'
import { isSessionAlmostExpired } from './sessionAuth.js'

const API_ROOT = import.meta.env.VITE_API_BASE ?? ''

let refreshPromise = null

async function requestRefresh(refreshToken) {
  const response = await fetch(`${API_ROOT}/api/iam/auth/refresh`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({ refreshToken })
  })
  const payload = await response.json().catch(() => ({}))
  if (!response.ok) {
    throw new Error(payload.message || `令牌刷新失败 ${response.status}`)
  }
  return payload
}

/**
 * 确保会话有效：即将过期或已过期时自动刷新。
 */
export async function refreshSessionIfNeeded(sourceSession = null, force = false) {
  const session = sourceSession || readSession()
  if (!session) {
    throw new Error('未登录')
  }
  const shouldRefresh = force || isSessionExpired(session) || isSessionAlmostExpired(session)
  if (!shouldRefresh) {
    return session
  }
  if (refreshPromise) {
    return refreshPromise
  }
  refreshPromise = (async () => {
    try {
      const refreshed = await requestRefresh(session.refreshToken)
      const nextSession = {
        accessToken: refreshed.accessToken,
        refreshToken: refreshed.refreshToken || session.refreshToken,
        expiresAt: Date.now() + Number(refreshed.expiresIn || 0) * 1000,
        username: session.username
      }
      if (!nextSession.accessToken || !Number.isFinite(nextSession.expiresAt)) {
        throw new Error('刷新响应缺少有效 accessToken')
      }
      writeSession(nextSession)
      return nextSession
    } catch (error) {
      clearSession()
      throw error
    } finally {
      refreshPromise = null
    }
  })()
  return refreshPromise
}

export async function logoutSession() {
  const session = readSession()
  if (!session) {
    clearSession()
    return
  }
  try {
    await fetch(`${API_ROOT}/api/iam/auth/logout`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ refreshToken: session.refreshToken })
    })
  } finally {
    clearSession()
  }
}
