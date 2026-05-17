import { reactive } from 'vue'

const state = reactive({
  tenantCode: 'tenant-a',
  envTag: 'prod'
})

export function useTenantStore() {
  return state
}
