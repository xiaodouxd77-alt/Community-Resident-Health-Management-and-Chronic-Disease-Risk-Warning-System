<template>
  <div class="page-container">
    <el-card class="section">
      <template #header>
        <div class="card-header">
          <span>健康统计概览（仅统计管理范围内居民）</span>
          <el-button :icon="Refresh" size="small" :loading="loading" @click="loadAll">刷新</el-button>
        </div>
      </template>
      <div v-loading="loading" class="stats">
        <div class="stat">
          <div class="stat-value primary">{{ overview.residentCount ?? 0 }}</div>
          <div class="stat-label">管理居民数</div>
        </div>
        <div class="stat">
          <div class="stat-value success">{{ overview.verifiedProfileCount ?? 0 }}</div>
          <div class="stat-label">已核实档案</div>
        </div>
        <div class="stat" @click="goHighRisk({ hasChronic: true })">
          <div class="stat-value warning">{{ overview.chronicConfirmedResidentCount ?? 0 }}</div>
          <div class="stat-label">已核实慢病居民</div>
          <div class="stat-sub">档案 {{ overview.chronicConfirmedRecordCount ?? 0 }} 份</div>
        </div>
        <div class="stat clickable" @click="goHighRisk({ riskLevel: 'HIGH' })">
          <div class="stat-value danger">{{ overview.highRiskResidentCount ?? 0 }}</div>
          <div class="stat-label">高风险居民</div>
        </div>
        <div class="stat clickable" @click="goHighRisk({ hasUnhandledAlert: true })">
          <div class="stat-value danger">{{ overview.unhandledAlertResidentCount ?? 0 }}</div>
          <div class="stat-label">有未处理预警居民</div>
          <div class="stat-sub">未处理预警 {{ overview.unhandledAlertCount ?? 0 }} 条</div>
        </div>
        <div class="stat clickable" @click="goHighRisk({})">
          <div class="stat-value info">{{ overview.resolvedAlertCount ?? 0 }}</div>
          <div class="stat-label">已完成预警</div>
          <div class="stat-sub">共 {{ overview.totalAlertCount ?? 0 }} 条</div>
        </div>
      </div>
    </el-card>

    <el-row :gutter="16" class="section">
      <el-col :span="12">
        <el-card v-loading="loading">
          <template #header><span>慢病类型分布（已核实档案）</span></template>
          <el-empty v-if="!chronicItems.length" description="暂无已核实慢病档案" />
          <v-chart v-else :option="chronicOption" height="320px" />
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card v-loading="loading">
          <template #header><span>风险评估等级分布</span></template>
          <el-empty v-if="!hasRiskData" description="暂无风险评估数据" />
          <v-chart v-else :option="riskOption" height="320px" />
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="section">
      <el-col :span="12">
        <el-card v-loading="loading">
          <template #header>
            <div class="card-header">
              <span>预警处理状态</span>
              <span class="rate">
                处理率 {{ formatPercent(alertStats.handlingRate) }}
                ({{ alertStats.resolvedCount ?? 0 }}/{{ alertStats.totalCount ?? 0 }})
              </span>
            </div>
          </template>
          <el-empty v-if="!alertStats.totalCount" description="暂无预警" />
          <v-chart v-else :option="alertOption" height="320px" />
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card v-loading="loading">
          <template #header>
            <div class="card-header">
              <span>预警趋势</span>
              <div class="trend-controls">
                <el-radio-group v-model="trendGranularity" size="small" @change="loadTrend">
                  <el-radio-button value="day">按天</el-radio-button>
                  <el-radio-button value="month">按月</el-radio-button>
                </el-radio-group>
              </div>
            </div>
          </template>
          <el-date-picker
            v-model="trendRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            size="small"
            class="trend-range"
            @change="loadTrend"
          />
          <el-empty v-if="!trendPoints.length" description="所选区间暂无预警" />
          <v-chart v-else :option="trendOption" height="300px" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Refresh } from '@element-plus/icons-vue'
import VChart from '@/components/VChart.vue'
import {
  getHealthOverview,
  getChronicStatistics,
  getRiskDistribution,
  getAlertStatistics,
  getAlertTrend
} from '@/api/doctor'
import { RISK_COLORS, ALERT_STATUS_COLORS, DISEASE_COLORS, formatPercent } from '@/utils/labels'

const router = useRouter()
const loading = ref(false)
const overview = reactive({})
const chronicItems = ref([])
const riskItems = ref([])
const alertStats = reactive({})
const trendPoints = ref([])
const trendGranularity = ref('day')
const trendRange = ref(defaultRange())

function defaultRange() {
  const end = new Date()
  const start = new Date()
  start.setDate(start.getDate() - 90)
  const fmt = (d) => `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
  return [fmt(start), fmt(end)]
}

const hasRiskData = computed(() => riskItems.value.some((i) => (i.count || 0) > 0))

const chronicOption = computed(() => ({
  tooltip: { trigger: 'item', formatter: '{b}: {c} 份 ({d}%)' },
  legend: { bottom: 0 },
  color: DISEASE_COLORS,
  series: [
    {
      name: '慢病档案',
      type: 'pie',
      radius: ['40%', '65%'],
      avoidLabelOverlap: true,
      label: { formatter: '{b}\n{c} 份' },
      data: chronicItems.value.map((i) => ({ name: i.diseaseTypeLabel, value: i.recordCount || 0 }))
    }
  ]
}))

const riskOption = computed(() => ({
  tooltip: { trigger: 'item', formatter: '{b}: {c} 次 ({d}%)' },
  legend: { bottom: 0 },
  series: [
    {
      name: '评估次数',
      type: 'pie',
      radius: ['40%', '65%'],
      data: riskItems.value.map((i) => ({
        name: i.riskLevelLabel,
        value: i.count || 0,
        itemStyle: { color: RISK_COLORS[i.riskLevel] }
      }))
    }
  ]
}))

const alertOption = computed(() => ({
  tooltip: { trigger: 'item', formatter: '{b}: {c} 条 ({d}%)' },
  legend: { bottom: 0 },
  series: [
    {
      name: '预警状态',
      type: 'pie',
      radius: ['40%', '65%'],
      data: [
        { name: '待处理', value: alertStats.pendingCount || 0, itemStyle: { color: ALERT_STATUS_COLORS.PENDING } },
        { name: '处理中', value: alertStats.processingCount || 0, itemStyle: { color: ALERT_STATUS_COLORS.PROCESSING } },
        { name: '已完成', value: alertStats.resolvedCount || 0, itemStyle: { color: ALERT_STATUS_COLORS.RESOLVED } }
      ]
    }
  ]
}))

const trendOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  legend: { bottom: 0 },
  grid: { left: 40, right: 20, top: 20, bottom: 40 },
  xAxis: { type: 'category', data: trendPoints.value.map((p) => p.bucket) },
  yAxis: { type: 'value', minInterval: 1 },
  series: [
    { name: '预警总数', type: 'line', smooth: true, data: trendPoints.value.map((p) => p.totalCount || 0) },
    { name: '待处理', type: 'line', smooth: true, data: trendPoints.value.map((p) => p.pendingCount || 0) },
    { name: '处理中', type: 'line', smooth: true, data: trendPoints.value.map((p) => p.processingCount || 0) },
    { name: '已完成', type: 'line', smooth: true, data: trendPoints.value.map((p) => p.resolvedCount || 0) }
  ]
}))

const loadOverview = async () => {
  const res = await getHealthOverview()
  Object.assign(overview, res.data || {})
}
const loadChronic = async () => {
  const res = await getChronicStatistics()
  chronicItems.value = res.data?.items || []
}
const loadRisk = async () => {
  const res = await getRiskDistribution()
  riskItems.value = res.data || []
}
const loadAlerts = async () => {
  const res = await getAlertStatistics()
  Object.assign(alertStats, res.data || {})
}
const loadTrend = async () => {
  const params = {
    granularity: trendGranularity.value,
    from: trendRange.value?.[0] ? `${trendRange.value[0]} 00:00:00` : undefined,
    to: trendRange.value?.[1] ? `${trendRange.value[1]} 23:59:59` : undefined
  }
  const res = await getAlertTrend(params)
  trendPoints.value = res.data || []
}

const loadAll = async () => {
  loading.value = true
  try {
    await Promise.all([loadOverview(), loadChronic(), loadRisk(), loadAlerts(), loadTrend()])
  } finally {
    loading.value = false
  }
}

const goHighRisk = (query) => router.push({ path: '/doctor/high-risk-residents', query })

onMounted(loadAll)
</script>

<style scoped>
.page-container {
  max-width: 1320px;
  margin: 0 auto;
}

.section {
  margin-bottom: 16px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.stats {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 14px;
}

.stat {
  border-radius: 8px;
  padding: 16px 12px;
  text-align: center;
  background: #f7f9fc;
  border: 1px solid #eef1f5;
}

.stat.clickable {
  cursor: pointer;
  transition: box-shadow 0.2s;
}

.stat.clickable:hover {
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.08);
}

.stat-value {
  font-size: 26px;
  font-weight: 700;
}

.stat-value.primary {
  color: #1f6fd6;
}

.stat-value.success {
  color: #67c23a;
}

.stat-value.warning {
  color: #e6a23c;
}

.stat-value.danger {
  color: #f56c6c;
}

.stat-value.info {
  color: #909399;
}

.stat-label {
  margin-top: 6px;
  color: #606266;
  font-size: 13px;
}

.stat-sub {
  margin-top: 2px;
  color: #a8abb2;
  font-size: 12px;
}

.rate {
  color: #606266;
  font-size: 13px;
}

.trend-range {
  margin-bottom: 10px;
}
</style>
