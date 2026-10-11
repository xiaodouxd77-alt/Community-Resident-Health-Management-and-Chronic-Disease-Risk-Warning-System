<template>
  <div class="page-container">
    <PageHeader title="健康指标历史" description="查看您记录的各项健康指标、风险评估结果与关联预警，支持按类型和时间筛选。">
      <el-button type="primary" :icon="Plus" @click="$router.push('/resident/indicators/new')">录入指标</el-button>
    </PageHeader>

    <div class="stat-row">
      <StatCard label="记录总数" :value="summary.total" hint="全部健康指标记录" />
      <StatCard label="预警记录" :value="summary.alertCount" tone="danger" hint="由指标触发的预警" />
      <StatCard label="最近测量" :value="summary.lastMeasuredAt || '—'" hint="最近一次测量时间" />
    </div>

    <el-card>
      <FilterBar>
        <el-select v-model="query.indicatorType" placeholder="全部指标类型" clearable style="width: 170px" @change="handleSearch">
          <el-option v-for="type in INDICATOR_TYPES" :key="type.value" :label="type.label" :value="type.value" />
        </el-select>
        <el-date-picker
          v-model="dateRange"
          type="datetimerange"
          value-format="YYYY-MM-DD HH:mm:ss"
          range-separator="至"
          start-placeholder="开始时间"
          end-placeholder="结束时间"
          :clearable="true"
        />
        <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
        <el-button :icon="Refresh" @click="handleReset">重置</el-button>
      </FilterBar>

      <div class="quick-types">
        <span class="quick-types__label">快捷筛选</span>
        <el-radio-group v-model="query.indicatorType" @change="handleSearch">
          <el-radio-button value="">全部</el-radio-button>
          <el-radio-button v-for="type in QUICK_TYPES" :key="type.value" :value="type.value">
            {{ type.label }}
          </el-radio-button>
        </el-radio-group>
      </div>

      <el-table v-loading="loading" :data="tableData" style="width: 100%" empty-text="暂无健康指标记录">
        <el-table-column prop="indicatorTypeLabel" label="指标" width="100" />
        <el-table-column label="测量值" width="180">
          <template #default="{ row }">{{ formatIndicatorValue(row) }}</template>
        </el-table-column>
        <el-table-column label="场景" width="120">
          <template #default="{ row }">{{ row.measurementContextLabel || '-' }}</template>
        </el-table-column>
        <el-table-column prop="measuredAt" label="测量时间" width="180" />
        <el-table-column label="风险评估" width="120">
          <template #default="{ row }">
            <StatusTag v-if="row.assessmentRiskLevel" :text="row.assessmentRiskLevelLabel" :type="riskTag(row.assessmentRiskLevel)" />
            <span v-else class="text-muted">-</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" fixed="right" width="100">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="query.page"
        v-model:page-size="query.size"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        class="pager"
        @size-change="load"
        @current-change="load"
      />
    </el-card>

    <DetailDrawer v-model="detailVisible" title="指标详情">
      <template v-if="current">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="指标类型">{{ current.indicatorTypeLabel }}</el-descriptions-item>
          <el-descriptions-item label="测量值">{{ formatIndicatorValue(current) }}</el-descriptions-item>
          <el-descriptions-item label="测量场景">{{ current.measurementContextLabel || '-' }}</el-descriptions-item>
          <el-descriptions-item label="测量时间">{{ current.measuredAt }}</el-descriptions-item>
          <el-descriptions-item label="数据来源">{{ current.source || '-' }}</el-descriptions-item>
          <el-descriptions-item label="备注">{{ current.remark || '-' }}</el-descriptions-item>
          <el-descriptions-item label="风险评估">
            <StatusTag v-if="current.assessmentRiskLevel" :text="current.assessmentRiskLevelLabel" :type="riskTag(current.assessmentRiskLevel)" />
            <span v-else class="text-muted">未评估</span>
          </el-descriptions-item>
        </el-descriptions>

        <el-alert
          v-if="current.alertId"
          class="alert-tip"
          type="warning"
          :closable="false"
          show-icon
          title="该次测量触发了预警，可在“我的预警”中查看处理进展。"
        />
      </template>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-button v-if="current?.alertId" type="primary" @click="goAlert(current.alertId)">查看关联预警</el-button>
      </template>
    </DetailDrawer>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Plus, Search, Refresh } from '@element-plus/icons-vue'
import { getMyIndicators, getMyAlerts } from '@/api/health'
import { INDICATOR_TYPES, formatIndicatorValue, riskTag } from '@/utils/labels'
import PageHeader from '@/components/PageHeader.vue'
import FilterBar from '@/components/FilterBar.vue'
import StatCard from '@/components/StatCard.vue'
import StatusTag from '@/components/StatusTag.vue'
import DetailDrawer from '@/components/DetailDrawer.vue'

// 快捷筛选只保留系统已有指标类型
const QUICK_TYPES = [
  { value: 'BLOOD_PRESSURE', label: '血压' },
  { value: 'BLOOD_GLUCOSE', label: '血糖' },
  { value: 'HEART_RATE', label: '心率' },
  { value: 'WEIGHT', label: '体重' }
]

const router = useRouter()
const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const dateRange = ref([])

const detailVisible = ref(false)
const current = ref(null)

const summary = reactive({ total: 0, alertCount: 0, lastMeasuredAt: '' })

const query = reactive({ page: 1, size: 10, indicatorType: '' })

const load = async () => {
  loading.value = true
  try {
    const params = {
      page: query.page,
      size: query.size,
      indicatorType: query.indicatorType || undefined,
      startTime: dateRange.value?.[0] || undefined,
      endTime: dateRange.value?.[1] || undefined
    }
    const res = await getMyIndicators(params)
    tableData.value = res.data.records || []
    total.value = res.data.total || 0
  } finally {
    loading.value = false
  }
}

// 摘要数据全部来自真实接口：记录总数与最近测量取指标接口，预警数量取预警接口
const loadSummary = async () => {
  try {
    const [indRes, alertRes] = await Promise.all([
      getMyIndicators({ page: 1, size: 1 }),
      getMyAlerts({ page: 1, size: 1 })
    ])
    summary.total = indRes.data?.total || 0
    summary.lastMeasuredAt = indRes.data?.records?.[0]?.measuredAt || ''
    summary.alertCount = alertRes.data?.total || 0
  } catch (e) {
    // 摘要为辅助信息，获取失败不影响主列表
  }
}

const handleSearch = () => {
  query.page = 1
  load()
}

const handleReset = () => {
  query.indicatorType = ''
  dateRange.value = []
  query.page = 1
  load()
}

const openDetail = (row) => {
  current.value = row
  detailVisible.value = true
}

const goAlert = (id) => router.push(`/resident/alerts/${id}`)

onMounted(() => {
  load()
  loadSummary()
})
</script>

<style scoped>
.page-container {
  max-width: 1240px;
  margin: 0 auto;
}

.stat-row {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  margin-bottom: 18px;
}

.quick-types {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.quick-types__label {
  font-size: 13px;
  color: var(--text-secondary);
  flex: none;
}

.pager {
  margin-top: 16px;
  justify-content: flex-end;
}

.alert-tip {
  margin-top: 16px;
}
</style>
