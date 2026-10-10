<template>
  <div class="page-container">
    <el-card>
      <template #header><span>我的异常预警</span></template>

      <div class="filters">
        <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 150px">
          <el-option v-for="s in ALERT_STATUSES" :key="s.value" :label="s.label" :value="s.value" />
        </el-select>
        <el-select v-model="query.riskLevel" placeholder="全部风险等级" clearable style="width: 150px">
          <el-option v-for="r in RISK_LEVELS" :key="r.value" :label="r.label" :value="r.value" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
        <el-button :icon="Refresh" @click="handleReset">重置</el-button>
      </div>

      <el-table
        v-loading="loading"
        :data="tableData"
        style="width: 100%"
        empty-text="暂无预警记录"
      >
        <el-table-column prop="alertTitle" label="预警" width="200" />
        <el-table-column label="测量值" width="150">
          <template #default="{ row }">{{ formatIndicatorValue(row) }}</template>
        </el-table-column>
        <el-table-column label="风险等级" width="110">
          <template #default="{ row }">
            <el-tag :type="riskTag(row.riskLevel)" effect="dark" size="small">{{ row.riskLevelLabel }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="alertReason" label="触发原因" min-width="260" show-overflow-tooltip />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.status)" size="small">{{ row.statusLabel }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="医生处理意见" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">{{ row.latestHandlingComment || '尚未处理' }}</template>
        </el-table-column>
        <el-table-column prop="createTime" label="预警时间" width="180" />
        <el-table-column label="操作" width="90" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="goDetail(row.id)">查看</el-button>
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
import { getMyAlerts } from '@/api/health'
import { ALERT_STATUSES, RISK_LEVELS, riskTag, statusTag, formatIndicatorValue } from '@/utils/labels'

const router = useRouter()
const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const query = reactive({ page: 1, size: 10, status: '', riskLevel: '' })

const load = async () => {
  loading.value = true
  try {
    const res = await getMyAlerts({
      page: query.page,
      size: query.size,
      status: query.status || undefined,
      riskLevel: query.riskLevel || undefined
    })
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
  query.status = ''
  query.riskLevel = ''
  query.page = 1
  load()
}

const goDetail = (id) => router.push(`/resident/alerts/${id}`)

onMounted(load)
</script>

<style scoped>
.page-container {
  max-width: 1200px;
  margin: 0 auto;
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
</style>
