const SESSION_KEY = 'bids_session'

/** @returns {{ username: string, password: string } | null} */
export function readSession() {
  try {
    const raw = sessionStorage.getItem(SESSION_KEY)
    if (!raw) return null
    const o = JSON.parse(raw)
    if (o && typeof o.username === 'string' && typeof o.password === 'string') {
      return { username: o.username, password: o.password }
    }
  } catch {
    /* ignore */
  }
  return null
}

/** @param {{ username: string, password: string }} s */
export function writeSession(s) {
  sessionStorage.setItem(SESSION_KEY, JSON.stringify(s))
}

export function clearSession() {
  sessionStorage.removeItem(SESSION_KEY)
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
