<template>
  <el-drawer
    v-model="visible"
    :title="title"
    :size="size"
    :direction="direction"
    :destroy-on-close="destroyOnClose"
    class="detail-drawer"
  >
    <div v-loading="loading" class="detail-drawer__body">
      <slot />
    </div>
    <template v-if="$slots.footer" #footer>
      <div class="detail-drawer__footer">
        <slot name="footer" />
      </div>
    </template>
  </el-drawer>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  title: { type: String, default: '详情' },
  size: { type: [String, Number], default: '560px' },
  direction: { type: String, default: 'rtl' },
  loading: { type: Boolean, default: false },
  destroyOnClose: { type: Boolean, default: true }
})

const emit = defineEmits(['update:modelValue'])

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})
</script>

<style scoped>
.detail-drawer__body {
  min-height: 120px;
}

.detail-drawer__footer {
  text-align: right;
}
</style>
