const BASE = '/alb-api/v1'

async function request(path, options = {}) {
  const response = await fetch(`${BASE}${path}`, {
    headers: { 'Content-Type': 'application/json', ...(options.headers || {}) },
    ...options
  })
  const text = await response.text()
  const data = text ? JSON.parse(text) : null
  if (!response.ok) {
    throw new Error(data?.message || `请求失败 (${response.status})`)
  }
  return data
}

export function listRoutes() {
  return request('/routes')
}

export function createRoute(body) {
  return request('/routes', { method: 'POST', body: JSON.stringify(body) })
}

export function updateRoute(id, body) {
  return request(`/routes/${id}`, { method: 'PUT', body: JSON.stringify(body) })
}

export function deleteRoute(id) {
  return request(`/routes/${id}`, { method: 'DELETE' })
}

export function listUpstreams() {
  return request('/upstreams')
}

export function createUpstream(body) {
  return request('/upstreams', { method: 'POST', body: JSON.stringify(body) })
}

export function updateUpstream(id, body) {
  return request(`/upstreams/${id}`, { method: 'PUT', body: JSON.stringify(body) })
}

export function deleteUpstream(id) {
  return request(`/upstreams/${id}`, { method: 'DELETE' })
}

export function listHeaders(routeId) {
  return request(`/routes/${routeId}/headers`)
}

export function createHeader(routeId, body) {
  return request(`/routes/${routeId}/headers`, { method: 'POST', body: JSON.stringify(body) })
}

export function updateHeader(id, body) {
  return request(`/headers/${id}`, { method: 'PUT', body: JSON.stringify(body) })
}

export function deleteHeader(id) {
  return request(`/headers/${id}`, { method: 'DELETE' })
}

export function publishRoutes() {
  return request('/publish', { method: 'POST' })
}

export function previewNginx() {
  return request('/publish/nginx-preview')
}

export function previewCaddy() {
  return request('/publish/caddy-preview')
}
