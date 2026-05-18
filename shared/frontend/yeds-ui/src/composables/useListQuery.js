import { reactive, ref } from 'vue'

/**
 * 列表查询表单：支持重置为初始快照。
 *
 * @param {Record<string, unknown>} initialValues 初始筛选值
 */
export function useListQuery(initialValues) {
  const initialSnapshot = ref(structuredClone(initialValues))
  const model = reactive(structuredClone(initialValues))

  function resetModel() {
    const snapshot = initialSnapshot.value
    Object.keys(model).forEach((key) => {
      model[key] = structuredClone(snapshot[key])
    })
  }

  function setInitialSnapshot(nextValues) {
    initialSnapshot.value = structuredClone(nextValues)
    resetModel()
  }

  return {
    model,
    resetModel,
    setInitialSnapshot
  }
}
