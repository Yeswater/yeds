import { reactive } from 'vue'

const STORAGE_KEY = 'iam_auth_session_v1'
const REFRESH_SKEW_MS = 5000

const state = reactive({
  accessToken: '',
  refreshToken: '',
  tokenType: 'Bearer',
  expiresAt: 0,
  username: ''
})

loadFromStorage()

function loadFromStorage() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (!raw) {
      return
    }
    const parsed = JSON.parse(raw)
    state.accessToken = parsed.accessToken || ''
    state.refreshToken = parsed.refreshToken || ''
    state.tokenType = parsed.tokenType || 'Bearer'
    state.expiresAt = Number(parsed.expiresAt || 0)
    state.username = parsed.username || ''
  } catch {
    clearSession()
  }
}

function persist() {
  localStorage.setItem(
    STORAGE_KEY,
    JSON.stringify({
      accessToken: state.accessToken,
      refreshToken: state.refreshToken,
      tokenType: state.tokenType,
      expiresAt: state.expiresAt,
      username: state.username
    })
  )
}

export function setSession(tokenResponse, username) {
  const expiresInSeconds = Number(tokenResponse?.expiresIn || 0)
  const ttlMs = Math.max(0, expiresInSeconds * 1000 - REFRESH_SKEW_MS)
  state.accessToken = tokenResponse?.accessToken || ''
  state.refreshToken = tokenResponse?.refreshToken || ''
  state.tokenType = tokenResponse?.tokenType || 'Bearer'
  state.expiresAt = Date.now() + ttlMs
  state.username = username || state.username
  persist()
}

export function clearSession() {
  state.accessToken = ''
  state.refreshToken = ''
  state.tokenType = 'Bearer'
  state.expiresAt = 0
  state.username = ''
  localStorage.removeItem(STORAGE_KEY)
}

export function isLoggedIn() {
  return Boolean(state.accessToken && Date.now() < state.expiresAt)
}

export function getAuthorizationHeader() {
  if (!state.accessToken) {
    return ''
  }
  return `${state.tokenType || 'Bearer'} ${state.accessToken}`
}

export function useAuthStore() {
  return state
}
