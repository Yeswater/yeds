<template>
  <el-table-column label="操作" :width="width" fixed="left">
    <template #default="{ row }">
      <el-button v-if="showCopy" link type="primary" @click="emit('copy', row)">复制</el-button>
      <el-button v-if="showEdit" link type="primary" @click="emit('edit', row)">编辑</el-button>
      <el-button v-if="showDelete" link type="danger" @click="handleDelete(row)">删除</el-button>
    </template>
  </el-table-column>
</template>

<script setup>
import { ElMessageBox } from 'element-plus'

const props = defineProps({
  width: {
    type: [Number, String],
    default: 180
  },
  showCopy: {
    type: Boolean,
    default: true
  },
  showEdit: {
    type: Boolean,
    default: true
  },
  showDelete: {
    type: Boolean,
    default: true
  },
  deleteTitle: {
    type: String,
    default: '确认删除'
  },
  deleteMessage: {
    type: Function,
    default: null
  }
})

const emit = defineEmits(['copy', 'edit', 'delete'])

async function handleDelete(row) {
  const message = props.deleteMessage ? props.deleteMessage(row) : '删除后不可恢复，是否继续？'
  try {
    await ElMessageBox.confirm(message, props.deleteTitle, {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      confirmButtonClass: 'el-button--danger'
    })
    emit('delete', row)
  } catch {
    // 用户取消
  }
}
</script>
