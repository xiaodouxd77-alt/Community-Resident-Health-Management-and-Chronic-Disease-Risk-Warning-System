<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>健康指标历史</span>
          <el-button type="primary" :icon="Plus" @click="$router.push('/resident/indicators/new')">
            录入指标
          </el-button>
        </div>
      </template>

      <div class="filters">
        <el-select v-model="query.indicatorType" placeholder="全部指标类型" clearable style="width: 180px">
          <el-option
            v-for="type in INDICATOR_TYPES"
            :key="type.value"
            :label="type.label"
            :value="type.value"
          />
        </el-select>
        <el-date-picker
          v-model="dateRange"
          type="datetimerange"
          value-format="YYYY-MM-DD HH:mm:ss"
          range-separator="至"
          start-placeholder="开始时间"
          end-placeholder="结束时间"
        />
        <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
        <el-button :icon="Refresh" @click="handleReset">重置</el-button>
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
            <el-tag v-if="row.assessmentRiskLevel" :type="riskTag(row.assessmentRiskLevel)" size="small">
              {{ row.assessmentRiskLevelLabel }}
            </el-tag>
            <span v-else class="muted">-</span>
          </template>
        </el-table-column>
        <el-table-column label="预警" width="120">
          <template #default="{ row }">
            <el-button v-if="row.alertId" link type="primary" @click="goAlert(row.alertId)">
              查看预警
            </el-button>
            <span v-else class="muted">无</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" fixed="right" width="100">
          <template #default="{ row }">
            <el-button link type="primary" @click="showDetail(row)">详情</el-button>
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

    <el-dialog v-model="detailVisible" title="指标详情" width="560px">
      <el-descriptions v-if="current" :column="1" border>
        <el-descriptions-item label="指标类型">{{ current.indicatorTypeLabel }}</el-descriptions-item>
        <el-descriptions-item label="测量值">{{ formatIndicatorValue(current) }}</el-descriptions-item>
        <el-descriptions-item label="测量场景">{{ current.measurementContextLabel || '-' }}</el-descriptions-item>
        <el-descriptions-item label="测量时间">{{ current.measuredAt }}</el-descriptions-item>
        <el-descriptions-item label="备注">{{ current.remark || '-' }}</el-descriptions-item>
        <el-descriptions-item label="风险评估">
          <el-tag v-if="current.assessmentRiskLevel" :type="riskTag(current.assessmentRiskLevel)">
            {{ current.assessmentRiskLevelLabel }}
          </el-tag>
          <span v-else class="muted">未评估</span>
        </el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-button v-if="current?.alertId" type="primary" @click="goAlert(current.alertId)">
          查看关联预警
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Plus, Search, Refresh } from '@element-plus/icons-vue'
import { getMyIndicators } from '@/api/health'
import { INDICATOR_TYPES, formatIndicatorValue, riskTag } from '@/utils/labels'

const router = useRouter()
const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const dateRange = ref([])
const detailVisible = ref(false)
const current = ref(null)

const query = reactive({ page: 1, size: 10, indicatorType: '', startTime: null, endTime: null })

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

const showDetail = (row) => {
  current.value = row
  detailVisible.value = true
}

const goAlert = (id) => router.push(`/resident/alerts/${id}`)

onMounted(load)
</script>

<style scoped>
.page-container {
  max-width: 1100px;
  margin: 0 auto;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.filters {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 16px;
}

.pager {
  margin-top: 16px;
  justify-content: flex-end;
}

.muted {
  color: #909399;
}
</style>
