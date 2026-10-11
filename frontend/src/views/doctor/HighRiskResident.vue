<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>高风险居民筛查</span>
          <el-button :icon="Refresh" size="small" @click="load">刷新</el-button>
        </div>
      </template>

      <el-alert
        type="info"
        :closable="false"
        show-icon
        class="notice"
        title="风险等级来自最近一次指标风险评估；未处理预警与已核实慢病为独立维度，无需相互推导。"
      />

      <div class="filters">
        <el-input v-model="query.keyword" placeholder="居民姓名 / 编号" clearable style="width: 170px" @keyup.enter="handleSearch" />
        <el-select v-model="query.riskLevel" placeholder="风险等级" clearable style="width: 130px">
          <el-option v-for="r in RISK_LEVELS" :key="r.value" :label="r.label" :value="r.value" />
        </el-select>
        <el-select v-model="query.hasUnhandledAlert" placeholder="有未处理预警" clearable style="width: 150px">
          <el-option v-for="b in BOOL_OPTIONS" :key="String(b.value)" :label="b.label" :value="b.value" />
        </el-select>
        <el-select v-model="query.diseaseType" placeholder="慢病类型" clearable style="width: 130px">
          <el-option v-for="d in DISEASE_TYPES" :key="d.value" :label="d.label" :value="d.value" />
        </el-select>
        <el-select v-model="query.hasChronic" placeholder="已核实慢病" clearable style="width: 140px">
          <el-option v-for="b in BOOL_OPTIONS" :key="String(b.value)" :label="b.label" :value="b.value" />
        </el-select>
        <el-select v-model="query.alertStatus" placeholder="预警状态" clearable style="width: 130px">
          <el-option v-for="s in ALERT_STATUSES" :key="s.value" :label="s.label" :value="s.value" />
        </el-select>
        <el-select v-model="query.groupId" placeholder="居民分组" clearable filterable style="width: 160px">
          <el-option v-for="g in groups" :key="g.id" :label="g.groupName" :value="g.id" />
        </el-select>
        <el-date-picker
          v-model="measuredRange"
          type="datetimerange"
          range-separator="至"
          start-placeholder="最近测量起"
          end-placeholder="最近测量止"
          value-format="YYYY-MM-DD HH:mm:ss"
          style="width: 360px"
        />
        <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
        <el-button :icon="RefreshLeft" @click="handleReset">重置</el-button>
      </div>

      <el-table v-loading="loading" :data="tableData" style="width: 100%" empty-text="暂无符合条件的居民">
        <el-table-column label="居民" width="140" fixed="left">
          <template #default="{ row }">
            <div>{{ row.realName || '-' }}</div>
            <div class="muted">{{ row.residentNo }}</div>
          </template>
        </el-table-column>
        <el-table-column label="性别/年龄" width="100">
          <template #default="{ row }">{{ row.genderLabel || '-' }}{{ row.age != null ? ` / ${row.age}` : '' }}</template>
        </el-table-column>
        <el-table-column prop="communityName" label="所属社区" width="120">
          <template #default="{ row }">{{ row.communityName || '-' }}</template>
        </el-table-column>
        <el-table-column label="负责医生" width="100">
          <template #default="{ row }">{{ row.doctorName || '待分配' }}</template>
        </el-table-column>
        <el-table-column label="最近风险" width="170">
          <template #default="{ row }">
            <template v-if="row.latestRiskLevel">
              <el-tag :type="riskTag(row.latestRiskLevel)" size="small">{{ row.latestRiskLevelLabel }}</el-tag>
              <div class="muted">{{ row.latestRiskAssessedAt || '-' }}</div>
            </template>
            <span v-else class="muted">暂无评估</span>
          </template>
        </el-table-column>
        <el-table-column label="未处理预警" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="row.unhandledAlertCount > 0 ? 'danger' : 'info'" size="small" effect="plain">
              {{ row.unhandledAlertCount ?? 0 }} / {{ row.totalAlertCount ?? 0 }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="已核实慢病" min-width="200">
          <template #default="{ row }">
            <template v-if="row.chronicSummaries && row.chronicSummaries.length">
              <el-tag
                v-for="c in row.chronicSummaries"
                :key="c.recordId"
                size="small"
                class="ml6"
                :type="c.managementStatus === 'ACTIVE' ? 'success' : 'info'"
              >
                {{ c.diseaseTypeLabel }}·{{ c.managementStatusLabel }}
              </el-tag>
            </template>
            <span v-else class="muted">无已核实慢病</span>
          </template>
        </el-table-column>
        <el-table-column label="最近测量" width="160">
          <template #default="{ row }">{{ row.lastMeasuredAt || '-' }}</template>
        </el-table-column>
        <el-table-column label="最近随访" width="160">
          <template #default="{ row }">{{ row.lastFollowupAt || '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">查看详情</el-button>
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

    <el-dialog v-model="detailVisible" title="居民风险详情" width="860px" v-loading="detailLoading">
      <template v-if="detail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="姓名">{{ detail.resident?.realName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="居民编号">{{ detail.resident?.residentNo || '-' }}</el-descriptions-item>
          <el-descriptions-item label="性别">{{ detail.resident?.genderLabel || '-' }}</el-descriptions-item>
          <el-descriptions-item label="出生日期">{{ detail.resident?.birthDate || '-' }}</el-descriptions-item>
          <el-descriptions-item label="所属社区">{{ detail.resident?.communityName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="联系电话">{{ detail.resident?.phone || '-' }}</el-descriptions-item>
          <el-descriptions-item label="档案核实">
            <el-tag :type="detail.resident?.verified ? 'success' : 'info'" size="small">
              {{ detail.resident?.verified ? '已核实' : '未核实' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="最近风险">
            <el-tag v-if="detail.latestRiskLevel" :type="riskTag(detail.latestRiskLevel)" size="small">
              {{ detail.latestRiskLevelLabel }}
            </el-tag>
            <span v-else class="muted">暂无评估</span>
            <span v-if="detail.latestRiskAssessedAt" class="muted">　{{ detail.latestRiskAssessedAt }}</span>
          </el-descriptions-item>
        </el-descriptions>

        <el-divider content-position="left">最近一次风险评估</el-divider>
        <template v-if="detail.latestAssessment">
          <p class="sum">{{ detail.latestAssessment.assessmentSummary || '-' }}</p>
          <el-table :data="detail.latestAssessment.matchedRules || []" size="small" empty-text="无命中规则">
            <el-table-column prop="ruleName" label="规则" min-width="140" />
            <el-table-column label="数值" width="140">
              <template #default="{ row }">{{ row.actualValue }} {{ row.valueFieldLabel }}</template>
            </el-table-column>
            <el-table-column prop="operator" label="条件" width="70" />
            <el-table-column prop="thresholdValue" label="阈值" width="90" />
            <el-table-column prop="riskLevelLabel" label="等级" width="100" />
          </el-table>
        </template>
        <el-empty v-else description="暂无评估记录" :image-size="60" />

        <el-divider content-position="left">慢病档案</el-divider>
        <el-table :data="detail.chronicDiseases || []" size="small" empty-text="暂无慢病档案">
          <el-table-column prop="diseaseTypeLabel" label="类型" width="110" />
          <el-table-column prop="diagnosisStatusLabel" label="资料状态" width="120" />
          <el-table-column prop="managementStatusLabel" label="管理状态" width="110" />
          <el-table-column prop="diagnosisDate" label="诊断日期" width="120" />
          <el-table-column prop="managingDoctorName" label="负责医生" width="110" />
        </el-table>

        <el-divider content-position="left">
          预警记录（未处理 {{ detail.unhandledAlertCount ?? 0 }} / 共 {{ detail.totalAlertCount ?? 0 }}）
        </el-divider>
        <el-table :data="detail.alerts || []" size="small" empty-text="暂无预警">
          <el-table-column prop="alertTitle" label="预警" min-width="160" show-overflow-tooltip />
          <el-table-column label="等级" width="100">
            <template #default="{ row }">
              <el-tag :type="riskTag(row.riskLevel)" size="small">{{ row.riskLevelLabel }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="statusTag(row.status)" size="small">{{ row.statusLabel }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="产生时间" width="160" />
        </el-table>

        <el-divider content-position="left">最近指标</el-divider>
        <el-table :data="detail.recentIndicators || []" size="small" empty-text="暂无指标记录">
          <el-table-column prop="indicatorTypeLabel" label="指标" width="90" />
          <el-table-column label="数值" width="140">
            <template #default="{ row }">{{ formatIndicatorValue(row) }}</template>
          </el-table-column>
          <el-table-column label="评估" width="110">
            <template #default="{ row }">
              <el-tag v-if="row.assessmentRiskLevel" :type="riskTag(row.assessmentRiskLevel)" size="small">
                {{ row.assessmentRiskLevelLabel }}
              </el-tag>
              <span v-else class="muted">-</span>
            </template>
          </el-table-column>
          <el-table-column prop="measuredAt" label="测量时间" width="160" />
        </el-table>
      </template>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { Refresh, RefreshLeft, Search } from '@element-plus/icons-vue'
import { getHighRiskResidents, getHighRiskResidentDetail, getGroupList } from '@/api/doctor'
import {
  RISK_LEVELS,
  ALERT_STATUSES,
  DISEASE_TYPES,
  BOOL_OPTIONS,
  riskTag,
  statusTag,
  formatIndicatorValue
} from '@/utils/labels'

const route = useRoute()
const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const groups = ref([])
const measuredRange = ref([])

const query = reactive({
  page: 1,
  size: 10,
  keyword: '',
  riskLevel: '',
  hasUnhandledAlert: '',
  diseaseType: '',
  hasChronic: '',
  alertStatus: '',
  groupId: ''
})

const detailVisible = ref(false)
const detailLoading = ref(false)
const detail = ref(null)

const toBool = (v) => {
  if (v === true || v === false) return v
  if (v === 'true') return true
  if (v === 'false') return false
  return ''
}

/** 布尔筛选：仅 true/false 参与查询，空值一律不传 */
const boolParam = (v) => (v === true || v === false ? v : undefined)

const load = async () => {
  loading.value = true
  try {
    const res = await getHighRiskResidents({
      page: query.page,
      size: query.size,
      keyword: query.keyword || undefined,
      riskLevel: query.riskLevel || undefined,
      hasUnhandledAlert: boolParam(query.hasUnhandledAlert),
      diseaseType: query.diseaseType || undefined,
      hasChronic: boolParam(query.hasChronic),
      alertStatus: query.alertStatus || undefined,
      groupId: query.groupId || undefined,
      lastMeasuredFrom: measuredRange.value?.[0] || undefined,
      lastMeasuredTo: measuredRange.value?.[1] || undefined
    })
    tableData.value = res.data.records || []
    total.value = res.data.total || 0
  } finally {
    loading.value = false
  }
}

const loadGroups = async () => {
  const res = await getGroupList({ page: 1, size: 100 })
  groups.value = res.data?.records || []
}

const handleSearch = () => {
  query.page = 1
  load()
}

const handleReset = () => {
  Object.assign(query, {
    keyword: '',
    riskLevel: '',
    hasUnhandledAlert: '',
    diseaseType: '',
    hasChronic: '',
    alertStatus: '',
    groupId: '',
    page: 1
  })
  measuredRange.value = []
  load()
}

const openDetail = async (row) => {
  detail.value = null
  detailVisible.value = true
  detailLoading.value = true
  try {
    const res = await getHighRiskResidentDetail(row.residentId)
    detail.value = res.data
  } finally {
    detailLoading.value = false
  }
}

const applyRouteQuery = () => {
  if (route.query.riskLevel) query.riskLevel = route.query.riskLevel
  if (route.query.hasUnhandledAlert !== undefined) query.hasUnhandledAlert = toBool(route.query.hasUnhandledAlert)
  if (route.query.hasChronic !== undefined) query.hasChronic = toBool(route.query.hasChronic)
  if (route.query.groupId) query.groupId = Number(route.query.groupId)
}

onMounted(() => {
  applyRouteQuery()
  loadGroups()
  load()
})
</script>

<style scoped>
.page-container {
  max-width: 1320px;
  margin: 0 auto;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.notice {
  margin-bottom: 14px;
}

.filters {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  align-items: center;
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

.ml6 {
  margin-left: 6px;
}

.sum {
  margin: 0 0 10px;
  color: #606266;
}
</style>
