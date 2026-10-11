<template>
  <div class="stat-card" :class="`stat-card--${tone}`">
    <div class="stat-card__label">{{ label }}</div>
    <div class="stat-card__value">{{ value }}</div>
    <div v-if="hint" class="stat-card__hint">{{ hint }}</div>
  </div>
</template>

<script setup>
defineProps({
  label: { type: String, required: true },
  value: { type: [String, Number], default: '-' },
  hint: { type: String, default: '' },
  tone: { type: String, default: 'default' } // default | danger | warning | success
})
</script>

<style scoped>
.stat-card {
  flex: 1;
  min-width: 180px;
  background: var(--surface);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-card);
  padding: 16px 20px;
  position: relative;
  overflow: hidden;
}

.stat-card::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 3px;
  background: var(--brand-primary);
}

.stat-card--danger::before {
  background: var(--tone-danger);
}

.stat-card--warning::before {
  background: var(--tone-warning);
}

.stat-card--success::before {
  background: var(--tone-success);
}

.stat-card__label {
  font-size: 13px;
  color: var(--text-secondary);
}

.stat-card__value {
  margin-top: 8px;
  font-size: 24px;
  font-weight: 600;
  line-height: 1.2;
  color: var(--text-primary);
  word-break: break-all;
}

.stat-card__hint {
  margin-top: 6px;
  font-size: 12px;
  color: var(--text-secondary);
}
</style>
