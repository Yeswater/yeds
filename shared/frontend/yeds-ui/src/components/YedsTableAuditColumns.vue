<template>
  <el-table-column prop="appCode" label="所属应用" :width="appWidth" />
  <el-table-column prop="lastOperator" label="最后操作人" :width="operatorWidth" show-overflow-tooltip />
  <el-table-column prop="lastOperateTimeText" label="最后操作时间" :min-width="timeMinWidth" />
</template>

<script setup>
defineProps({
  appWidth: {
    type: [Number, String],
    default: 110
  },
  operatorWidth: {
    type: [Number, String],
    default: 110
  },
  timeMinWidth: {
    type: [Number, String],
    default: 170
  }
})
</script>
