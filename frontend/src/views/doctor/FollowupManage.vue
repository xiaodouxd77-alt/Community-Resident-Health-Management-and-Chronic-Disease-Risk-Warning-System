<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>随访计划管理</span>
          <el-button type="primary" :icon="Plus" @click="openCreate">新建随访计划</el-button>
        </div>
      </template>

      <div class="filters">
        <el-input v-model="query.keyword" placeholder="居民姓名 / 编号" clearable style="width: 180px" @keyup.enter="handleSearch" />
        <el-select v-model="query.residentId" placeholder="全部居民" clearable filterable style="width: 170px">
          <el-option v-for="r in residents" :key="r.id" :label="`${r.realName || r.residentNo}（${r.residentNo}）`" :value="r.id" />
        </el-select>
        <el-select v-model="query.planStatus" placeholder="全部状态" clearable style="width: 130px">
          <el-option v-for="s in PLAN_STATUSES" :key="s.value" :label="s.label" :value="s.value" />
        </el-select>
        <el-select v-model="query.followupMethod" placeholder="全部方式" clearable style="width: 120px">
          <el-option v-for="m in FOLLOWUP_METHODS" :key="m.value" :label="m.label" :value="m.value" />
        </el-select>
        <el-date-picker
          v-model="dateRange"
          type="datetimerange"
          range-separator="至"
          start-placeholder="计划开始"
          end-placeholder="计划结束"
          value-format="YYYY-MM-DD HH:mm:ss"
          style="width: 360px"
        />
        <el-checkbox v-model="query.overdueOnly" label="仅看逾期" />
        <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
        <el-button :icon="Refresh" @click="handleReset">重置</el-button>
      </div>

      <el-table v-loading="loading" :data="tableData" style="width: 100%" empty-text="暂无随访计划">
        <el-table-column label="居民" width="160">
          <template #default="{ row }">
            <div>{{ row.residentName || '-' }}</div>
            <div class="muted">{{ row.residentNo }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="planTitle" label="计划标题" min-width="180" show-overflow-tooltip />
        <el-table-column prop="scheduledAt" label="计划随访时间" width="170" />
        <el-table-column prop="followupMethodLabel" label="随访方式" width="100" />
        <el-table-column label="状态" width="130">
          <template #default="{ row }">
            <el-tag :type="planStatusTag(row.planStatus)" size="small">{{ row.planStatusLabel }}</el-tag>
            <el-tag v-if="row.overdue" type="danger" size="small" effect="plain" class="ml6">已逾期</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="关联" width="150">
          <template #default="{ row }">
            <el-tag v-if="row.chronicDiseaseRecordId" size="small" type="info" class="mr4">
              慢病·{{ row.chronicDiseaseTypeLabel }}
            </el-tag>
            <el-tag v-if="row.alertId" size="small" type="warning">预警</el-tag>
            <span v-if="!row.chronicDiseaseRecordId && !row.alertId" class="muted">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="recordCount" label="随访记录" width="90" />
        <el-table-column label="操作" width="190" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="goDetail(row.id)">详情</el-button>
            <el-button v-if="!isTerminal(row.planStatus)" link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button v-if="!isTerminal(row.planStatus)" link type="danger" @click="cancelPlan(row)">取消</el-button>
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
    <el-dialog v-model="formVisible" :title="formMode === 'create' ? '新建随访计划' : '编辑随访计划'" width="560px">
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="100px">
        <el-form-item v-if="formMode === 'create'" label="居民" prop="residentId">
          <el-select v-model="form.residentId" filterable placeholder="请选择居民" style="width: 100%" @change="onResidentChange">
            <el-option
              v-for="r in residents"
              :key="r.id"
              :label="`${r.realName || r.residentNo}（${r.residentNo}）`"
              :value="r.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="计划标题" prop="planTitle">
          <el-input v-model="form.planTitle" maxlength="100" placeholder="如：高血压随访复诊" />
        </el-form-item>
        <el-form-item label="计划随访时间" prop="scheduledAt">
          <el-date-picker
            v-model="form.scheduledAt"
            type="datetime"
            value-format="YYYY-MM-DD HH:mm:ss"
            placeholder="选择日期时间"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="随访方式" prop="followupMethod">
          <el-select v-model="form.followupMethod" placeholder="请选择" style="width: 100%">
            <el-option v-for="m in FOLLOWUP_METHODS" :key="m.value" :label="m.label" :value="m.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="随访目的" prop="followupPurpose">
          <el-input v-model="form.followupPurpose" type="textarea" :rows="2" maxlength="500" show-word-limit placeholder="选填" />
        </el-form-item>
        <el-form-item v-if="formMode === 'create'" label="关联慢病档案">
          <el-select v-model="form.chronicDiseaseRecordId" clearable placeholder="选填" style="width: 100%" :disabled="!form.residentId">
            <el-option
              v-for="c in chronicOptions"
              :key="c.id"
              :label="`${c.diseaseTypeLabel}（${c.managementStatusLabel}）`"
              :value="c.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item v-if="formMode === 'create'" label="关联预警">
          <el-select v-model="form.alertId" clearable placeholder="选填" style="width: 100%" :disabled="!form.residentId">
            <el-option
              v-for="a in alertOptions"
              :key="a.id"
              :label="`${a.alertTitle}（${a.riskLevelLabel}）`"
              :value="a.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="允许居民查看">
          <el-switch v-model="form.residentVisible" />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="2" maxlength="255" show-word-limit placeholder="选填" />
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
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search, Refresh } from '@element-plus/icons-vue'
import {
  getFollowupPlans,
  createFollowupPlan,
  updateFollowupPlan,
  cancelFollowupPlan,
  getMyResidents,
  getResidentChronic,
  getResidentAlerts
} from '@/api/doctor'
import { FOLLOWUP_METHODS, PLAN_STATUSES, planStatusTag } from '@/utils/labels'

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
  planStatus: '',
  followupMethod: '',
  overdueOnly: false
})

const submitting = ref(false)
const formVisible = ref(false)
const formMode = ref('create')
const editId = ref(null)
const formRef = ref(null)
const chronicOptions = ref([])
const alertOptions = ref([])
const form = reactive({
  residentId: null,
  chronicDiseaseRecordId: null,
  alertId: null,
  planTitle: '',
  followupPurpose: '',
  scheduledAt: null,
  followupMethod: 'PHONE',
  residentVisible: true,
  remark: ''
})
const formRules = {
  residentId: [{ required: true, message: '请选择居民', trigger: 'change' }],
  planTitle: [{ required: true, message: '请输入计划标题', trigger: 'blur' }],
  scheduledAt: [{ required: true, message: '请选择计划随访时间', trigger: 'change' }],
  followupMethod: [{ required: true, message: '请选择随访方式', trigger: 'change' }]
}

const isTerminal = (status) => status === 'COMPLETED' || status === 'CANCELLED'

const load = async () => {
  loading.value = true
  try {
    const res = await getFollowupPlans({
      page: query.page,
      size: query.size,
      keyword: query.keyword || undefined,
      residentId: query.residentId || undefined,
      planStatus: query.planStatus || undefined,
      followupMethod: query.followupMethod || undefined,
      overdueOnly: query.overdueOnly || undefined,
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
  Object.assign(query, { keyword: '', residentId: '', planStatus: '', followupMethod: '', overdueOnly: false, page: 1 })
  dateRange.value = []
  load()
}

const resetForm = () => {
  Object.assign(form, {
    residentId: null,
    chronicDiseaseRecordId: null,
    alertId: null,
    planTitle: '',
    followupPurpose: '',
    scheduledAt: null,
    followupMethod: 'PHONE',
    residentVisible: true,
    remark: ''
  })
  chronicOptions.value = []
  alertOptions.value = []
}

const onResidentChange = async (residentId) => {
  form.chronicDiseaseRecordId = null
  form.alertId = null
  chronicOptions.value = []
  alertOptions.value = []
  if (!residentId) return
  const [chronicRes, alertRes] = await Promise.all([
    getResidentChronic(residentId),
    getResidentAlerts(residentId, { page: 1, size: 50 })
  ])
  chronicOptions.value = chronicRes.data || []
  alertOptions.value = alertRes.data.records || []
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
    planTitle: row.planTitle,
    followupPurpose: row.followupPurpose || '',
    scheduledAt: row.scheduledAt,
    followupMethod: row.followupMethod,
    residentVisible: row.residentVisible !== false,
    remark: row.remark || ''
  })
  formVisible.value = true
  formRef.value?.clearValidate()
}

const submitForm = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    if (formMode.value === 'create') {
      await createFollowupPlan({
        residentId: form.residentId,
        chronicDiseaseRecordId: form.chronicDiseaseRecordId || undefined,
        alertId: form.alertId || undefined,
        planTitle: form.planTitle,
        followupPurpose: form.followupPurpose || undefined,
        scheduledAt: form.scheduledAt,
        followupMethod: form.followupMethod,
        residentVisible: form.residentVisible,
        remark: form.remark || undefined
      })
      ElMessage.success('随访计划已创建')
    } else {
      await updateFollowupPlan(editId.value, {
        planTitle: form.planTitle,
        followupPurpose: form.followupPurpose || undefined,
        scheduledAt: form.scheduledAt,
        followupMethod: form.followupMethod,
        residentVisible: form.residentVisible,
        remark: form.remark || undefined
      })
      ElMessage.success('随访计划已更新')
    }
    formVisible.value = false
    load()
  } finally {
    submitting.value = false
  }
}

const cancelPlan = async (row) => {
  await ElMessageBox.confirm(`确认取消计划「${row.planTitle}」？`, '提示', { type: 'warning' })
  await cancelFollowupPlan(row.id)
  ElMessage.success('计划已取消')
  load()
}

const goDetail = (id) => router.push(`/doctor/followups/plan/${id}`)

const applyRouteQuery = () => {
  if (route.query.planStatus) query.planStatus = route.query.planStatus
  if (route.query.overdueOnly === 'true' || route.query.overdueOnly === true) query.overdueOnly = true
  if (route.query.residentId) query.residentId = Number(route.query.residentId)
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

.mr4 {
  margin-right: 4px;
}
</style>
