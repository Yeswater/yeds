import { isSessionExpired, readSession } from './authStorage.js'

/** @returns {{ accessToken: string, refreshToken: string, expiresAt: number, username: string }} */
export function sessionAuthOpts() {
  const s = readSession()
  if (!s) {
    throw new Error('未登录')
  }
  return s
}

export function isSessionAlmostExpired(session, advanceMs = 30_000) {
  if (!session) {
    return true
  }
  return Date.now() + advanceMs >= session.expiresAt
}
