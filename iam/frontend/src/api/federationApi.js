import { requestApi } from '../services/http'

export function saveTenantFederation(body) {
  return requestApi({
    action: '保存联邦映射',
    path: '/api/iam/tenant-federations',
    method: 'POST',
    body
  })
}

export function fetchTenantFederations() {
  return requestApi({
    action: '查询联邦映射',
    path: '/api/iam/tenant-federations',
    method: 'GET'
  })
}
