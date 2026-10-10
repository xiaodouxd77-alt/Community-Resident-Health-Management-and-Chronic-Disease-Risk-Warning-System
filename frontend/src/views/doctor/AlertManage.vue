<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>预警管理</span>
          <span class="muted">仅展示您所负责居民的预警</span>
        </div>
      </template>

      <div class="filters">
        <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 140px">
          <el-option v-for="s in ALERT_STATUSES" :key="s.value" :label="s.label" :value="s.value" />
        </el-select>
        <el-select v-model="query.riskLevel" placeholder="全部风险等级" clearable style="width: 150px">
          <el-option v-for="r in RISK_LEVELS" :key="r.value" :label="r.label" :value="r.value" />
        </el-select>
        <el-select v-model="query.residentId" placeholder="全部居民" clearable filterable style="width: 200px">
          <el-option
            v-for="r in residents"
            :key="r.id"
            :label="residentLabel(r)"
            :value="r.id"
          />
        </el-select>
        <el-input
          v-model="query.keyword"
          placeholder="居民编号 / 姓名"
          clearable
          style="width: 200px"
          @keyup.enter="handleSearch"
        />
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

      <el-table
        v-loading="loading"
        :data="tableData"
        style="width: 100%"
        empty-text="暂无预警记录"
      >
        <el-table-column label="居民" width="180">
          <template #default="{ row }">
            <div>{{ row.residentName || '-' }}</div>
            <div class="muted">{{ row.residentNo }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="alertTitle" label="预警" width="200" />
        <el-table-column label="测量值" width="150">
          <template #default="{ row }">{{ formatIndicatorValue(row) }}</template>
        </el-table-column>
        <el-table-column label="风险等级" width="110">
          <template #default="{ row }">
            <el-tag :type="riskTag(row.riskLevel)" effect="dark" size="small">{{ row.riskLevelLabel }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.status)" size="small">{{ row.statusLabel }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="最近处理意见" min-width="220" show-overflow-tooltip>
          <template #default="{ row }">{{ row.latestHandlingComment || '尚未处理' }}</template>
        </el-table-column>
        <el-table-column prop="createTime" label="预警时间" width="180" />
        <el-table-column label="操作" width="90" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="goDetail(row.id)">处理</el-button>
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
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Search, Refresh } from '@element-plus/icons-vue'
import { getDoctorAlerts, getMyResidents } from '@/api/doctor'
import { ALERT_STATUSES, RISK_LEVELS, riskTag, statusTag, formatIndicatorValue } from '@/utils/labels'

const router = useRouter()
const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const residents = ref([])
const dateRange = ref([])
const query = reactive({ page: 1, size: 10, status: '', riskLevel: '', residentId: '', keyword: '' })

const residentLabel = (r) => `${r.realName || r.residentNo}（${r.residentNo}）`

const load = async () => {
  loading.value = true
  try {
    const res = await getDoctorAlerts({
      page: query.page,
      size: query.size,
      status: query.status || undefined,
      riskLevel: query.riskLevel || undefined,
      residentId: query.residentId || undefined,
      keyword: query.keyword || undefined,
      startTime: dateRange.value?.[0] || undefined,
      endTime: dateRange.value?.[1] || undefined
    })
    tableData.value = res.data.records || []
    total.value = res.data.total || 0
  } finally {
    loading.value = false
  }
}

const loadResidents = async () => {
  const res = await getMyResidents()
  residents.value = res.data || []
}

const handleSearch = () => {
  query.page = 1
  load()
}

const handleReset = () => {
  query.status = ''
  query.riskLevel = ''
  query.residentId = ''
  query.keyword = ''
  dateRange.value = []
  query.page = 1
  load()
}

const goDetail = (id) => router.push(`/doctor/alerts/${id}`)

onMounted(() => {
  loadResidents()
  load()
})
</script>

<style scoped>
.page-container {
  max-width: 1280px;
  margin: 0 auto;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
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
  font-size: 12px;
}
</style>
