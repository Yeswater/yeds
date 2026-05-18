import { computed, ref, watch } from 'vue'

/**
 * 前端分页：对列表 ref 做 slice。
 *
 * @param {import('vue').Ref<Array<unknown>>} rowsRef 全量数据
 * @param {number} defaultPageSize 每页条数
 */
export function useClientPager(rowsRef, defaultPageSize = 20) {
  const currentPage = ref(1)
  const pageSize = ref(defaultPageSize)

  const total = computed(() => (Array.isArray(rowsRef.value) ? rowsRef.value.length : 0))

  const pagedRows = computed(() => {
    const rows = Array.isArray(rowsRef.value) ? rowsRef.value : []
    const start = (currentPage.value - 1) * pageSize.value
    return rows.slice(start, start + pageSize.value)
  })

  watch(rowsRef, () => {
    currentPage.value = 1
  })

  function onPageChange(page) {
    currentPage.value = page
  }

  return {
    currentPage,
    pageSize,
    total,
    pagedRows,
    onPageChange
  }
}
