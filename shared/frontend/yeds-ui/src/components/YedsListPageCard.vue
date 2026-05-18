<template>
  <el-card class="panel yeds-panel yeds-list-page-card">
    <template #header>
      <div class="panel-title">
        <span>{{ title }}</span>
        <el-tag v-if="tag" :type="tagType" effect="plain">{{ tag }}</el-tag>
      </div>
    </template>
    <div class="yeds-list-page-body">
      <slot />
    </div>
  </el-card>
</template>

<script setup>
defineProps({
  title: {
    type: String,
    required: true
  },
  tag: {
    type: String,
    default: ''
  },
  tagType: {
    type: String,
    default: 'success'
  }
})
</script>
