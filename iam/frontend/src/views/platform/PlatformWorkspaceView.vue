<template>
  <el-card class="panel">
    <template #header>
      <div class="panel-title">
        <span>{{ title }}</span>
        <el-tag type="info" effect="plain">{{ platform }}</el-tag>
      </div>
    </template>
    <el-empty :description="description" />
  </el-card>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()

const title = computed(() => route.meta.title || '平台工作台')
const description = computed(() => route.meta.description || '平台能力开发中')
const platform = computed(() => route.meta.platform || 'PLATFORM')
</script>
