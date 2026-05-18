<template>
  <div v-if="total > 0" class="yeds-list-pager">
    <el-pagination
      layout="prev, pager, next, total"
      :total="total"
      :page-size="pageSize"
      :current-page="currentPage"
      @current-change="onCurrentChange"
    />
  </div>
</template>

<script setup>
const props = defineProps({
  total: {
    type: Number,
    default: 0
  },
  pageSize: {
    type: Number,
    default: 20
  },
  currentPage: {
    type: Number,
    default: 1
  }
})

const emit = defineEmits(['update:currentPage', 'change'])

function onCurrentChange(page) {
  emit('update:currentPage', page)
  emit('change', page)
}
</script>
