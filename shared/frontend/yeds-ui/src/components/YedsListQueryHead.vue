<template>
  <el-form :model="model" :label-width="labelWidth" class="yeds-query-head" @submit.prevent>
    <div class="yeds-query-grid">
      <slot />
      <el-form-item class="yeds-query-actions">
        <el-button type="primary" :loading="loading" @click="$emit('search')">查询</el-button>
        <el-button @click="$emit('reset')">重置</el-button>
      </el-form-item>
    </div>
  </el-form>
</template>

<script setup>
defineProps({
  model: {
    type: Object,
    required: true
  },
  loading: {
    type: Boolean,
    default: false
  },
  labelWidth: {
    type: String,
    default: '88px'
  }
})

defineEmits(['search', 'reset'])
</script>
