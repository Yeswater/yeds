import { reactive } from 'vue'

const HISTORY_STORAGE_KEY = 'iam_operation_history_v1'
const MAX_HISTORY_SIZE = 200

const state = reactive({
  latestResponse: null,
  history: loadHistory()
})

function loadHistory() {
  try {
    const raw = sessionStorage.getItem(HISTORY_STORAGE_KEY)
    if (!raw) {
      return []
    }
    const parsed = JSON.parse(raw)
    return Array.isArray(parsed) ? parsed : []
  } catch {
    return []
  }
}

function persistHistory() {
  sessionStorage.setItem(HISTORY_STORAGE_KEY, JSON.stringify(state.history))
}

export function recordApiResult(action, result) {
  state.latestResponse = {
    ...result,
    action
  }
  const item = {
    id: `${Date.now()}-${Math.random().toString(16).slice(2, 8)}`,
    action,
    ok: result.ok,
    status: result.status,
    elapsedMs: result.elapsedMs,
    requestId: result.requestId || '-',
    path: result.path,
    method: result.method,
    at: new Date().toISOString()
  }
  state.history = [item, ...state.history].slice(0, MAX_HISTORY_SIZE)
  persistHistory()
}

export function clearHistory() {
  state.history = []
  persistHistory()
}

export function exportDiagnosticsJson() {
  return JSON.stringify(
    {
      exportedAt: new Date().toISOString(),
      latestResponse: state.latestResponse,
      history: state.history
    },
    null,
    2
  )
}

export function useUiStore() {
  return state
}
