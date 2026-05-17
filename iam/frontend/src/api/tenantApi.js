import { requestApi } from '../services/http'

export function fetchTenantCatalog(keyword = '') {
  const query = new URLSearchParams()
  if (keyword) {
    query.set('keyword', keyword)
  }
  const suffix = query.toString()
  return requestApi({
    action: '查询租户主数据',
    path: suffix ? `/api/iam/tenants?${suffix}` : '/api/iam/tenants',
    method: 'GET'
  })
}
