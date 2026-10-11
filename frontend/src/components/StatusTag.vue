<template>
  <el-tag :type="type" :size="size" :effect="effect" :round="round">{{ text }}</el-tag>
</template>

<script setup>
defineProps({
  text: { type: String, default: '' },
  type: { type: String, default: 'info' },
  size: { type: String, default: 'small' },
  effect: { type: String, default: 'light' },
  round: { type: Boolean, default: false }
})
</script>
