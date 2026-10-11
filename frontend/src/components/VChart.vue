<template>
  <div ref="el" class="v-chart" :style="{ height }"></div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, watch, nextTick } from 'vue'
import * as echarts from 'echarts'

const props = defineProps({
  option: { type: Object, default: () => ({}) },
  height: { type: String, default: '320px' }
})

const el = ref(null)
let chart = null

const render = () => {
  if (chart && props.option) {
    chart.setOption(props.option, true)
  }
}
const resize = () => chart && chart.resize()

onMounted(() => {
  chart = echarts.init(el.value)
  render()
  window.addEventListener('resize', resize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resize)
  if (chart) {
    chart.dispose()
    chart = null
  }
})

watch(() => props.option, () => nextTick(render), { deep: true })
</script>

<style scoped>
.v-chart {
  width: 100%;
}
</style>
