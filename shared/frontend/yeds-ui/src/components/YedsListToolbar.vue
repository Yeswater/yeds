<template>
  <div class="yeds-list-toolbar" :class="{ 'is-end': align === 'end' }">
    <slot />
  </div>
</template>

<script setup>
defineProps({
  align: {
    type: String,
    default: 'start',
    validator: (value) => value === 'start' || value === 'end'
  }
})
</script>
