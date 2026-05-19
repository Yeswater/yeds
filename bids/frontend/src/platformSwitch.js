import { isSessionExpired, readSession } from './authStorage.js'

const IAM_FRONTEND_BASE = (import.meta.env.VITE_IAM_FRONTEND_URL || `${window.location.origin}/iam`).replace(/\/$/, '')
const IAM_DEFAULT_PATH = '/abac/policy'

function safeRedirectPath(path, fallback) {
  if (typeof path === 'string' && path.startsWith('/') && !path.startsWith('//')) {
    return path
  }
  return fallback
}

function sessionExpiresInSeconds(session) {
  return Math.max(1, Math.floor((session.expiresAt - Date.now()) / 1000))
}

/**
 * 构建 IAM 前端 SSO 回调地址（已登录时免密跳转）。
 */
export function buildIamSsoCallbackUrl(redirectPath = IAM_DEFAULT_PATH) {
  const session = readSession()
  if (!session || isSessionExpired(session)) {
    return null
  }
  const target = safeRedirectPath(redirectPath, IAM_DEFAULT_PATH)
  const callback = new URL(`${IAM_FRONTEND_BASE}/auth/sso-callback`)
  callback.searchParams.set('access_token', session.accessToken)
  callback.searchParams.set('refresh_token', session.refreshToken)
  callback.searchParams.set('expires_in', String(sessionExpiresInSeconds(session)))
  callback.searchParams.set('username', session.username)
  callback.searchParams.set('redirect', target)
  return callback.toString()
}

/**
 * 构建 IAM 前端登录页地址（未登录时）。
 */
export function buildIamLoginUrl(redirectPath = IAM_DEFAULT_PATH) {
  const target = safeRedirectPath(redirectPath, IAM_DEFAULT_PATH)
  const loginUrl = new URL(`${IAM_FRONTEND_BASE}/login`)
  loginUrl.searchParams.set('redirect', target)
  return loginUrl.toString()
}

/**
 * 切换到 IAM 平台。
 */
export function switchToIam(redirectPath = IAM_DEFAULT_PATH) {
  const ssoUrl = buildIamSsoCallbackUrl(redirectPath)
  window.location.assign(ssoUrl || buildIamLoginUrl(redirectPath))
}
