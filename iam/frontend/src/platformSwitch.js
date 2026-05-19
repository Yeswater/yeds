import { isLoggedIn, useAuthStore } from './stores/authStore'

const BIDS_FRONTEND_BASE = (import.meta.env.VITE_BIDS_FRONTEND_URL || `${window.location.origin}/bids`).replace(/\/$/, '')
const BIDS_DEFAULT_PATH = '/run/svc'

function safeRedirectPath(path, fallback) {
  if (typeof path === 'string' && path.startsWith('/') && !path.startsWith('//')) {
    return path
  }
  return fallback
}

function sessionExpiresInSeconds(authStore) {
  return Math.max(1, Math.floor((authStore.expiresAt - Date.now()) / 1000))
}

/**
 * 构建 BIDS 前端 SSO 回调地址（已登录时免密跳转）。
 */
export function buildBidsSsoCallbackUrl(redirectPath = BIDS_DEFAULT_PATH) {
  if (!isLoggedIn()) {
    return null
  }
  const authStore = useAuthStore()
  const target = safeRedirectPath(redirectPath, BIDS_DEFAULT_PATH)
  const callback = new URL(`${BIDS_FRONTEND_BASE}/auth/iam/callback`)
  callback.searchParams.set('access_token', authStore.accessToken)
  callback.searchParams.set('refresh_token', authStore.refreshToken)
  callback.searchParams.set('expires_in', String(sessionExpiresInSeconds(authStore)))
  callback.searchParams.set('username', authStore.username || 'unknown')
  callback.searchParams.set('redirect', target)
  return callback.toString()
}

/**
 * 构建 BIDS 登录入口（未登录时走 BIDS 统一登录链）。
 */
export function buildBidsLoginEntryUrl(redirectPath = BIDS_DEFAULT_PATH) {
  const target = safeRedirectPath(redirectPath, BIDS_DEFAULT_PATH)
  const loginUrl = new URL(`${BIDS_FRONTEND_BASE}/login`)
  loginUrl.searchParams.set('redirect', target)
  return loginUrl.toString()
}

/**
 * 切换到 BIDS 平台。
 */
export function switchToBids(redirectPath = BIDS_DEFAULT_PATH) {
  const ssoUrl = buildBidsSsoCallbackUrl(redirectPath)
  window.location.assign(ssoUrl || buildBidsLoginEntryUrl(redirectPath))
}
