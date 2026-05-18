import { requestApi } from '../services/http'

export function createAbacPolicy(body) {
  return requestApi({
    action: '创建 ABAC 策略',
    path: '/api/iam/abac-policies',
    method: 'POST',
    body
  })
}

export function updateAbacPolicy(id, body) {
  return requestApi({
    action: '更新 ABAC 策略',
    path: `/api/iam/abac-policies/${id}`,
    method: 'PUT',
    body
  })
}

export function deleteAbacPolicy(id, modifiedBy) {
  const query = new URLSearchParams()
  if (modifiedBy) {
    query.set('modifiedBy', modifiedBy)
  }
  const suffix = query.toString() ? `?${query.toString()}` : ''
  return requestApi({
    action: '删除 ABAC 策略',
    path: `/api/iam/abac-policies/${id}${suffix}`,
    method: 'DELETE'
  })
}

export function listAbacPolicies(params = {}) {
  const query = new URLSearchParams()
  if (params.policyName) {
    query.set('policyName', params.policyName)
  }
  if (params.resourceCode) {
    query.set('resourceCode', params.resourceCode)
  }
  if (params.actionCode) {
    query.set('actionCode', params.actionCode)
  }
  query.set('limit', String(params.limit || 100))
  return requestApi({
    action: '查询 ABAC 策略',
    path: `/api/iam/abac-policies?${query.toString()}`,
    method: 'GET'
  })
}

export function checkAuthorize(body, forceDeny) {
  return requestApi({
    action: forceDeny ? 'ABAC 校验（预期拒绝）' : 'ABAC 校验（预期允许）',
    path: '/api/iam/authorize/check',
    method: 'POST',
    body
  })
}
