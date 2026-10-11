<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>健康干预管理</span>
          <el-button type="primary" :icon="Plus" @click="openCreate">新建干预方案</el-button>
        </div>
      </template>

      <div class="filters">
        <el-input v-model="query.keyword" placeholder="居民姓名 / 编号" clearable style="width: 170px" @keyup.enter="handleSearch" />
        <el-select v-model="query.residentId" placeholder="全部居民" clearable filterable style="width: 170px">
          <el-option v-for="r in residents" :key="r.id" :label="`${r.realName || r.residentNo}（${r.residentNo}）`" :value="r.id" />
        </el-select>
        <el-select v-model="query.interventionType" placeholder="全部类型" clearable style="width: 150px">
          <el-option v-for="t in INTERVENTION_TYPES" :key="t.value" :label="t.label" :value="t.value" />
        </el-select>
        <el-select v-model="query.planStatus" placeholder="全部状态" clearable style="width: 130px">
          <el-option v-for="s in INTERVENTION_STATUSES" :key="s.value" :label="s.label" :value="s.value" />
        </el-select>
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期起"
          end-placeholder="开始日期止"
          value-format="YYYY-MM-DD"
          style="width: 300px"
        />
        <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
        <el-button :icon="Refresh" @click="handleReset">重置</el-button>
      </div>

      <el-table v-loading="loading" :data="tableData" style="width: 100%" empty-text="暂无干预方案">
        <el-table-column label="居民" width="150">
          <template #default="{ row }">
            <div>{{ row.residentName || '-' }}</div>
            <div class="muted">{{ row.residentNo }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="planName" label="方案名称" min-width="170" show-overflow-tooltip />
        <el-table-column prop="interventionTypeLabel" label="干预类型" width="130" />
        <el-table-column prop="doctorName" label="负责人" width="100" />
        <el-table-column label="起止日期" width="200">
          <template #default="{ row }">
            {{ row.startDate || '-' }} ~ {{ row.endDate || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="150">
          <template #default="{ row }">
            <el-tag :type="interventionStatusTag(row.planStatus)" size="small">{{ row.planStatusLabel }}</el-tag>
            <el-tag
              v-if="row.latestExecutionStatus"
              :type="executionStatusTag(row.latestExecutionStatus)"
              size="small"
              effect="plain"
              class="ml6"
            >
              {{ row.latestExecutionStatusLabel }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="recordCount" label="执行记录" width="90" />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="goDetail(row.id)">详情</el-button>
            <el-button v-if="!isTerminal(row.planStatus)" link type="primary" @click="openEdit(row)">编辑</el-button>
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

    <!-- 新建 / 编辑 -->
    <el-dialog v-model="formVisible" :title="formMode === 'create' ? '新建干预方案' : '编辑干预方案'" width="640px">
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="110px">
        <el-form-item v-if="formMode === 'create'" label="居民" prop="residentId">
          <el-select v-model="form.residentId" filterable placeholder="请选择居民" style="width: 100%" @change="onResidentChange">
            <el-option v-for="r in residents" :key="r.id" :label="`${r.realName || r.residentNo}（${r.residentNo}）`" :value="r.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="方案名称" prop="planName">
          <el-input v-model="form.planName" maxlength="100" placeholder="如：高血压生活方式干预" />
        </el-form-item>
        <el-form-item label="干预类型" prop="interventionType">
          <el-select v-model="form.interventionType" placeholder="请选择" style="width: 100%">
            <el-option v-for="t in INTERVENTION_TYPES" :key="t.value" :label="t.label" :value="t.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="开始日期" prop="startDate">
          <el-date-picker v-model="form.startDate" type="date" value-format="YYYY-MM-DD" placeholder="选填" style="width: 100%" />
        </el-form-item>
        <el-form-item label="结束日期" prop="endDate">
          <el-date-picker v-model="form.endDate" type="date" value-format="YYYY-MM-DD" placeholder="选填" style="width: 100%" />
        </el-form-item>
        <el-form-item label="干预目标" prop="interventionGoal">
          <el-input v-model="form.interventionGoal" type="textarea" :rows="2" maxlength="500" show-word-limit placeholder="选填" />
        </el-form-item>
        <el-form-item label="干预措施" prop="interventionContent">
          <el-input v-model="form.interventionContent" type="textarea" :rows="3" maxlength="1000" show-word-limit placeholder="选填" />
        </el-form-item>
        <el-form-item label="关联慢病档案">
          <el-select v-model="form.chronicDiseaseRecordId" clearable placeholder="选填" style="width: 100%" :disabled="!form.residentId">
            <el-option v-for="c in chronicOptions" :key="c.id" :label="`${c.diseaseTypeLabel}（${c.managementStatusLabel}）`" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="关联预警">
          <el-select v-model="form.alertId" clearable placeholder="选填" style="width: 100%" :disabled="!form.residentId">
            <el-option v-for="a in alertOptions" :key="a.id" :label="`${a.alertTitle}（${a.riskLevelLabel}）`" :value="a.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="关联随访计划">
          <el-select v-model="form.followupPlanId" clearable placeholder="选填" style="width: 100%" :disabled="!form.residentId">
            <el-option v-for="f in followupOptions" :key="f.id" :label="`${f.planTitle}（${f.planStatusLabel}）`" :value="f.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="允许居民查看">
          <el-switch v-model="form.residentVisible" />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="2" maxlength="255" show-word-limit placeholder="选填，居民不可见" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Plus, Search, Refresh } from '@element-plus/icons-vue'
import {
  getInterventionPlans,
  createInterventionPlan,
  updateInterventionPlan,
  getMyResidents,
  getResidentChronic,
  getResidentAlerts,
  getResidentFollowupPlans
} from '@/api/doctor'
import {
  INTERVENTION_TYPES,
  INTERVENTION_STATUSES,
  interventionStatusTag,
  executionStatusTag
} from '@/utils/labels'

const router = useRouter()
const route = useRoute()
const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const residents = ref([])
const dateRange = ref([])
const query = reactive({
  page: 1,
  size: 10,
  keyword: '',
  residentId: '',
  interventionType: '',
  planStatus: ''
})

const submitting = ref(false)
const formVisible = ref(false)
const formMode = ref('create')
const editId = ref(null)
const formRef = ref(null)
const chronicOptions = ref([])
const alertOptions = ref([])
const followupOptions = ref([])
const form = reactive({
  residentId: null,
  chronicDiseaseRecordId: null,
  alertId: null,
  followupPlanId: null,
  planName: '',
  interventionType: 'LIFESTYLE',
  interventionGoal: '',
  interventionContent: '',
  startDate: null,
  endDate: null,
  residentVisible: true,
  remark: ''
})
const formRules = {
  residentId: [{ required: true, message: '请选择居民', trigger: 'change' }],
  planName: [{ required: true, message: '请输入方案名称', trigger: 'blur' }],
  interventionType: [{ required: true, message: '请选择干预类型', trigger: 'change' }]
}

const isTerminal = (status) => status === 'COMPLETED' || status === 'CANCELLED'

const load = async () => {
  loading.value = true
  try {
    const res = await getInterventionPlans({
      page: query.page,
      size: query.size,
      keyword: query.keyword || undefined,
      residentId: query.residentId || undefined,
      interventionType: query.interventionType || undefined,
      planStatus: query.planStatus || undefined,
      startDateFrom: dateRange.value?.[0] || undefined,
      startDateTo: dateRange.value?.[1] || undefined
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
  Object.assign(query, { keyword: '', residentId: '', interventionType: '', planStatus: '', page: 1 })
  dateRange.value = []
  load()
}

const resetForm = () => {
  Object.assign(form, {
    residentId: null,
    chronicDiseaseRecordId: null,
    alertId: null,
    followupPlanId: null,
    planName: '',
    interventionType: 'LIFESTYLE',
    interventionGoal: '',
    interventionContent: '',
    startDate: null,
    endDate: null,
    residentVisible: true,
    remark: ''
  })
  chronicOptions.value = []
  alertOptions.value = []
  followupOptions.value = []
}

const loadLinkageOptions = async (residentId) => {
  chronicOptions.value = []
  alertOptions.value = []
  followupOptions.value = []
  if (!residentId) return
  const [chronicRes, alertRes, followupRes] = await Promise.all([
    getResidentChronic(residentId),
    getResidentAlerts(residentId, { page: 1, size: 50 }),
    getResidentFollowupPlans(residentId)
  ])
  chronicOptions.value = chronicRes.data || []
  alertOptions.value = alertRes.data.records || []
  followupOptions.value = followupRes.data || []
}

const onResidentChange = (residentId) => {
  form.chronicDiseaseRecordId = null
  form.alertId = null
  form.followupPlanId = null
  loadLinkageOptions(residentId)
}

const openCreate = () => {
  formMode.value = 'create'
  resetForm()
  formVisible.value = true
  formRef.value?.clearValidate()
}

const openEdit = (row) => {
  formMode.value = 'edit'
  editId.value = row.id
  resetForm()
  Object.assign(form, {
    residentId: row.residentId,
    chronicDiseaseRecordId: row.chronicDiseaseRecordId || null,
    alertId: row.alertId || null,
    followupPlanId: row.followupPlanId || null,
    planName: row.planName,
    interventionType: row.interventionType,
    interventionGoal: row.interventionGoal || '',
    interventionContent: row.interventionContent || '',
    startDate: row.startDate || null,
    endDate: row.endDate || null,
    residentVisible: row.residentVisible !== false,
    remark: row.remark || ''
  })
  loadLinkageOptions(row.residentId)
  formVisible.value = true
  formRef.value?.clearValidate()
}

const submitForm = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  if (form.startDate && form.endDate && form.startDate > form.endDate) {
    ElMessage.warning('开始日期不能晚于结束日期')
    return
  }
  submitting.value = true
  try {
    const payload = {
      chronicDiseaseRecordId: form.chronicDiseaseRecordId || undefined,
      alertId: form.alertId || undefined,
      followupPlanId: form.followupPlanId || undefined,
      planName: form.planName,
      interventionType: form.interventionType,
      interventionGoal: form.interventionGoal || undefined,
      interventionContent: form.interventionContent || undefined,
      startDate: form.startDate || undefined,
      endDate: form.endDate || undefined,
      residentVisible: form.residentVisible,
      remark: form.remark || undefined
    }
    if (formMode.value === 'create') {
      await createInterventionPlan({ ...payload, residentId: form.residentId })
      ElMessage.success('干预方案已创建')
    } else {
      await updateInterventionPlan(editId.value, payload)
      ElMessage.success('干预方案已更新')
    }
    formVisible.value = false
    load()
  } finally {
    submitting.value = false
  }
}

const goDetail = (id) => router.push(`/doctor/interventions/${id}`)

const applyRouteQuery = () => {
  if (route.query.planStatus) query.planStatus = route.query.planStatus
  if (route.query.interventionType) query.interventionType = route.query.interventionType
}

onMounted(() => {
  applyRouteQuery()
  loadResidents()
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

.filters {
  display: flex;
  gap: 12px;
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
</style>
