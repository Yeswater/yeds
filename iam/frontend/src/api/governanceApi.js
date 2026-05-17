import { requestApi } from '../services/http'

export function fetchRiskEvents(limit) {
  return requestApi({
    action: '查询风险事件',
    path: `/api/iam/governance/risk-events?limit=${limit}`,
    method: 'GET'
  })
}

export function inspectOverPrivileged(permissionThreshold) {
  return requestApi({
    action: '巡检过权账户',
    path: `/api/iam/governance/inspections/over-privileged?permissionThreshold=${permissionThreshold}`,
    method: 'GET'
  })
}

export function inspectZombieAccounts(inactiveDays) {
  return requestApi({
    action: '巡检僵尸账号',
    path: `/api/iam/governance/inspections/zombie-accounts?inactiveDays=${inactiveDays}`,
    method: 'GET'
  })
}

export function inspectStaleClients(staleDays) {
  return requestApi({
    action: '巡检长期未使用凭证',
    path: `/api/iam/governance/inspections/stale-clients?staleDays=${staleDays}`,
    method: 'GET'
  })
}
