import { requestApi } from '../services/http'

export function saveTenantFederation(body) {
  return requestApi({
    action: '保存联邦映射',
    path: '/api/iam/tenant-federations',
    method: 'POST',
    body
  })
}

export function deleteTenantFederation(id, modifiedBy) {
  const query = new URLSearchParams()
  if (modifiedBy) {
    query.set('modifiedBy', modifiedBy)
  }
  const suffix = query.toString() ? `?${query.toString()}` : ''
  return requestApi({
    action: '删除联邦映射',
    path: `/api/iam/tenant-federations/${id}${suffix}`,
    method: 'DELETE'
  })
}

export function fetchTenantFederations() {
  return requestApi({
    action: '查询联邦映射',
    path: '/api/iam/tenant-federations',
    method: 'GET'
  })
}
