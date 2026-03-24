<script setup>
import { init } from 'echarts/core'
import { nextTick, onMounted, onUnmounted, ref, watch } from 'vue'

import '@/lib/echarts'

const props = defineProps({
  option: {
    type: Object,
    required: true,
  },
  height: {
    type: String,
    default: '320px',
  },
})

const chartRef = ref(null)

let chartInstance = null

function renderChart() {
  if (!chartRef.value) {
    return
  }

  if (!chartInstance) {
    chartInstance = init(chartRef.value)
  }

  chartInstance.setOption(props.option, true)
}

function resizeChart() {
  chartInstance?.resize()
}

watch(
  () => props.option,
  async () => {
    await nextTick()
    renderChart()
  },
  { deep: true },
)

onMounted(async () => {
  await nextTick()
  renderChart()
  window.addEventListener('resize', resizeChart)
})

onUnmounted(() => {
  window.removeEventListener('resize', resizeChart)
  chartInstance?.dispose()
  chartInstance = null
})
</script>

<template>
  <div ref="chartRef" class="insight-chart" :style="{ height }"></div>
</template>

<style scoped>
.insight-chart {
  width: 100%;
}
</style>
