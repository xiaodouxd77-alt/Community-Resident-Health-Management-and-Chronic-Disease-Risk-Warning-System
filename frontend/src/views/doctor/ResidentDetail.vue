<template>
  <div class="page-container" v-loading="loading">
    <el-page-header @back="$router.push('/doctor/residents')" class="page-header">
      <template #content>
        <span>{{ detail?.realName || '居民详情' }}</span>
        <span class="muted ml8">{{ detail?.residentNo }}</span>
      </template>
    </el-page-header>

    <el-card v-if="detail" class="section">
      <el-tabs v-model="activeTab" @tab-change="onTabChange">
        <el-tab-pane label="基本资料" name="basic">
          <el-descriptions :column="3" border>
            <el-descriptions-item label="姓名">{{ detail.realName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="性别">{{ detail.genderLabel || '-' }}</el-descriptions-item>
            <el-descriptions-item label="出生日期">{{ detail.birthDate || '-' }}</el-descriptions-item>
            <el-descriptions-item label="居民编号">{{ detail.residentNo }}</el-descriptions-item>
            <el-descriptions-item label="所属社区">{{ detail.communityName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="身高">{{ detail.height ? detail.height + ' cm' : '-' }}</el-descriptions-item>
            <el-descriptions-item label="联系电话">{{ detail.phone || '-' }}</el-descriptions-item>
            <el-descriptions-item label="档案核实">
              <el-tag :type="detail.verified ? 'success' : 'info'" size="small">
                {{ detail.verified ? '已核实' : '待核实（居民自述）' }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="建档时间">{{ detail.createTime || '-' }}</el-descriptions-item>
            <el-descriptions-item label="既往病史" :span="3">{{ detail.pastHistory || '未填写' }}</el-descriptions-item>
            <el-descriptions-item label="家族病史" :span="3">{{ detail.familyHistory || '未填写' }}</el-descriptions-item>
            <el-descriptions-item label="过敏史" :span="3">{{ detail.allergyHistory || '未填写' }}</el-descriptions-item>
            <el-descriptions-item label="健康备注" :span="3">{{ detail.healthRemark || '未填写' }}</el-descriptions-item>
          </el-descriptions>
        </el-tab-pane>

        <el-tab-pane label="健康指标" name="indicators">
          <el-table v-loading="tabLoading.indicators" :data="indicators" size="small" empty-text="暂无指标记录">
            <el-table-column prop="indicatorTypeLabel" label="指标" width="100" />
            <el-table-column label="测量值" width="170">
              <template #default="{ row }">{{ formatIndicatorValue(row) }}</template>
            </el-table-column>
            <el-table-column label="场景" width="110">
              <template #default="{ row }">{{ contextLabel(row.measurementContext) }}</template>
            </el-table-column>
            <el-table-column prop="measuredAt" label="测量时间" width="180" />
            <el-table-column label="风险评估" width="120">
              <template #default="{ row }">
                <el-tag v-if="row.assessmentRiskLevel" :type="riskTag(row.assessmentRiskLevel)" size="small">
                  {{ row.assessmentRiskLevelLabel }}
                </el-tag>
                <span v-else>-</span>
              </template>
            </el-table-column>
          </el-table>
          <el-pagination
            v-model:current-page="indQuery.page"
            :page-size="indQuery.size"
            :total="indTotal"
            layout="total, prev, pager, next"
            class="pager"
            @current-change="loadIndicators"
          />
        </el-tab-pane>

        <el-tab-pane label="风险评估" name="assessments">
          <el-table v-loading="tabLoading.assessments" :data="assessments" size="small" empty-text="暂无评估记录">
            <el-table-column prop="assessmentCode" label="评估编号" width="200" />
            <el-table-column prop="indicatorTypeLabel" label="指标" width="90" />
            <el-table-column label="风险等级" width="110">
              <template #default="{ row }">
                <el-tag :type="riskTag(row.riskLevel)" effect="dark" size="small">{{ row.riskLevelLabel }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="assessmentSummary" label="评估摘要" min-width="280" show-overflow-tooltip />
            <el-table-column prop="assessedAt" label="评估时间" width="180" />
          </el-table>
          <el-pagination
            v-model:current-page="assessQuery.page"
            :page-size="assessQuery.size"
            :total="assessTotal"
            layout="total, prev, pager, next"
            class="pager"
            @current-change="loadAssessments"
          />
        </el-tab-pane>

        <el-tab-pane label="异常预警" name="alerts">
          <el-table v-loading="tabLoading.alerts" :data="alerts" size="small" empty-text="暂无预警记录">
            <el-table-column prop="alertTitle" label="预警" width="200" />
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
            <el-table-column prop="createTime" label="预警时间" width="180" />
            <el-table-column label="操作" width="90">
              <template #default="{ row }">
                <el-button link type="primary" @click="$router.push(`/doctor/alerts/${row.id}`)">处理</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-pagination
            v-model:current-page="alertQuery.page"
            :page-size="alertQuery.size"
            :total="alertTotal"
            layout="total, prev, pager, next"
            class="pager"
            @current-change="loadAlerts"
          />
        </el-tab-pane>

        <el-tab-pane label="慢病档案" name="chronic">
          <el-table v-loading="tabLoading.chronic" :data="chronicList" size="small" empty-text="暂无慢病档案">
            <el-table-column prop="diseaseTypeLabel" label="慢病类型" width="120" />
            <el-table-column label="资料状态" width="140">
              <template #default="{ row }">
                <el-tag :type="diagnosisStatusTag(row.diagnosisStatus)" size="small">{{ row.diagnosisStatusLabel }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="管理状态" width="120">
              <template #default="{ row }">
                <el-tag :type="managementStatusTag(row.managementStatus)" size="small">{{ row.managementStatusLabel }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="diagnosisDate" label="诊断日期" width="130" />
            <el-table-column prop="createTime" label="建档时间" width="180" />
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="所属分组" name="groups">
          <el-empty v-if="tabLoading.groups && !groups.length" description="加载中" />
          <el-table v-else :data="groups" size="small" empty-text="尚未加入任何分组">
            <el-table-column prop="groupName" label="分组名称" width="180" />
            <el-table-column prop="groupCode" label="分组编码" width="180" />
            <el-table-column prop="communityName" label="所属社区" width="140">
              <template #default="{ row }">{{ row.communityName || '-' }}</template>
            </el-table-column>
            <el-table-column prop="managerDoctorName" label="负责医生" width="120" />
            <el-table-column label="状态" width="90">
              <template #default="{ row }">
                <el-tag :type="groupStatusTag(row.status)" size="small">{{ row.statusLabel }}</el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import {
  getResidentDetail,
  getResidentIndicators,
  getResidentAssessments,
  getResidentAlerts,
  getResidentChronic,
  getResidentGroups
} from '@/api/doctor'
import {
  formatIndicatorValue,
  contextLabel,
  riskTag,
  statusTag,
  diagnosisStatusTag,
  managementStatusTag,
  groupStatusTag
} from '@/utils/labels'

const route = useRoute()
const residentId = ref(route.params.id)

const loading = ref(false)
const detail = ref(null)
const activeTab = ref('basic')

const indicators = ref([])
const indTotal = ref(0)
const indQuery = reactive({ page: 1, size: 10 })

const assessments = ref([])
const assessTotal = ref(0)
const assessQuery = reactive({ page: 1, size: 10 })

const alerts = ref([])
const alertTotal = ref(0)
const alertQuery = reactive({ page: 1, size: 10 })

const chronicList = ref([])
const groups = ref([])

const tabLoading = reactive({ indicators: false, assessments: false, alerts: false, chronic: false, groups: false })
const loaded = reactive({ indicators: false, assessments: false, alerts: false, chronic: false, groups: false })

const loadDetail = async () => {
  loading.value = true
  try {
    const res = await getResidentDetail(residentId.value)
    detail.value = res.data
  } finally {
    loading.value = false
  }
}

const loadIndicators = async () => {
  tabLoading.indicators = true
  try {
    const res = await getResidentIndicators(residentId.value, { page: indQuery.page, size: indQuery.size })
    indicators.value = res.data.records || []
    indTotal.value = res.data.total || 0
    loaded.indicators = true
  } finally {
    tabLoading.indicators = false
  }
}

const loadAssessments = async () => {
  tabLoading.assessments = true
  try {
    const res = await getResidentAssessments(residentId.value, { page: assessQuery.page, size: assessQuery.size })
    assessments.value = res.data.records || []
    assessTotal.value = res.data.total || 0
    loaded.assessments = true
  } finally {
    tabLoading.assessments = false
  }
}

const loadAlerts = async () => {
  tabLoading.alerts = true
  try {
    const res = await getResidentAlerts(residentId.value, { page: alertQuery.page, size: alertQuery.size })
    alerts.value = res.data.records || []
    alertTotal.value = res.data.total || 0
    loaded.alerts = true
  } finally {
    tabLoading.alerts = false
  }
}

const loadChronic = async () => {
  tabLoading.chronic = true
  try {
    const res = await getResidentChronic(residentId.value)
    chronicList.value = res.data || []
    loaded.chronic = true
  } finally {
    tabLoading.chronic = false
  }
}

const loadGroups = async () => {
  tabLoading.groups = true
  try {
    const res = await getResidentGroups(residentId.value)
    groups.value = res.data || []
    loaded.groups = true
  } finally {
    tabLoading.groups = false
  }
}

const onTabChange = (name) => {
  if (name === 'indicators' && !loaded.indicators) loadIndicators()
  if (name === 'assessments' && !loaded.assessments) loadAssessments()
  if (name === 'alerts' && !loaded.alerts) loadAlerts()
  if (name === 'chronic' && !loaded.chronic) loadChronic()
  if (name === 'groups' && !loaded.groups) loadGroups()
}

onMounted(async () => {
  await loadDetail()
  loadIndicators()
})
</script>

<style scoped>
.page-container {
  max-width: 1200px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 16px;
}

.section {
  margin-bottom: 16px;
}

.pager {
  margin-top: 12px;
  justify-content: flex-end;
}

.muted {
  color: #909399;
  font-size: 13px;
}

.ml8 {
  margin-left: 8px;
}
</style>
