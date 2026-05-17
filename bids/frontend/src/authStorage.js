const SESSION_KEY = 'bids_session'

/**
 * @returns {{ accessToken: string, refreshToken: string, expiresAt: number, username: string } | null}
 */
export function readSession() {
  try {
    const raw = sessionStorage.getItem(SESSION_KEY)
    if (!raw) {
      return null
    }
    const o = JSON.parse(raw)
    if (
      o &&
      typeof o.accessToken === 'string' &&
      typeof o.refreshToken === 'string' &&
      typeof o.expiresAt === 'number' &&
      typeof o.username === 'string'
    ) {
      return {
        accessToken: o.accessToken,
        refreshToken: o.refreshToken,
        expiresAt: o.expiresAt,
        username: o.username
      }
    }
  } catch {
    /* ignore */
  }
  return null
}

/**
 * @param {{ accessToken: string, refreshToken: string, expiresAt: number, username: string }} s
 */
export function writeSession(s) {
  sessionStorage.setItem(SESSION_KEY, JSON.stringify(s))
}

export function clearSession() {
  sessionStorage.removeItem(SESSION_KEY)
}

export function isSessionExpired(session) {
  if (!session) {
    return true
  }
  return Date.now() >= session.expiresAt
}

const THEME_KEY = 'bids_theme'

/** @returns {'light' | 'dark'} */
export function readThemePreference() {
  const v = localStorage.getItem(THEME_KEY)
  return v === 'dark' ? 'dark' : 'light'
}

/** @param {'light' | 'dark'} mode */
export function writeThemePreference(mode) {
  localStorage.setItem(THEME_KEY, mode)
}
