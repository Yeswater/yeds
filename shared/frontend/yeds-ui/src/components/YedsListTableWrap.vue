<template>
  <div class="yeds-list-table-wrap" :style="wrapStyle">
    <slot />
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  maxHeight: {
    type: String,
    default: 'calc(100vh - 320px)'
  }
})

const wrapStyle = computed(() => {
  if (!props.maxHeight) {
    return {}
  }
  return { maxHeight: props.maxHeight }
})
</script>
