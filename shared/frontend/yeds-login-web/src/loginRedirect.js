/**
 * 解析 SSO 回跳地址；未传 redirect_uri 时使用 IAM 前端默认回调。
 */
export function resolveRedirectUri() {
  const params = new URLSearchParams(window.location.search)
  const fromQuery = params.get('redirect_uri')
  if (fromQuery && fromQuery.trim()) {
    return fromQuery.trim()
  }
  const iamFrontend = (import.meta.env.VITE_IAM_FRONTEND_URL || 'http://127.0.0.1:5181').replace(
    /\/$/,
    ''
  )
  const fallback = new URL('/auth/sso-callback', iamFrontend)
  fallback.searchParams.set('redirect', '/abac/policy')
  return fallback.toString()
}

/**
 * 登录成功后携带令牌跳转到业务系统回调地址。
 */
export function redirectWithTokens(redirectUri, payload, username) {
  const target = new URL(redirectUri)
  target.searchParams.set('access_token', payload.accessToken || '')
  target.searchParams.set('refresh_token', payload.refreshToken || '')
  target.searchParams.set('expires_in', String(payload.expiresIn ?? 0))
  target.searchParams.set('username', username)
  window.location.replace(target.toString())
}
