<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>慢病患者管理</span>
          <el-button type="primary" :icon="Plus" @click="openCreate">新建建档</el-button>
        </div>
      </template>

      <div class="filters">
        <el-input v-model="query.keyword" placeholder="居民姓名 / 编号" clearable style="width: 200px" @keyup.enter="handleSearch" />
        <el-select v-model="query.diseaseType" placeholder="全部慢病类型" clearable style="width: 150px">
          <el-option v-for="t in DISEASE_TYPES" :key="t.value" :label="t.label" :value="t.value" />
        </el-select>
        <el-select v-model="query.diagnosisStatus" placeholder="全部资料状态" clearable style="width: 150px">
          <el-option v-for="s in DIAGNOSIS_STATUSES" :key="s.value" :label="s.label" :value="s.value" />
        </el-select>
        <el-select v-model="query.managementStatus" placeholder="全部管理状态" clearable style="width: 150px">
          <el-option v-for="s in MANAGEMENT_STATUSES" :key="s.value" :label="s.label" :value="s.value" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
        <el-button :icon="Refresh" @click="handleReset">重置</el-button>
      </div>

      <el-table v-loading="loading" :data="tableData" style="width: 100%" empty-text="暂无慢病档案">
        <el-table-column label="居民" width="170">
          <template #default="{ row }">
            <div>{{ row.residentName || '-' }}</div>
            <div class="muted">{{ row.residentNo }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="diseaseTypeLabel" label="慢病类型" width="110" />
        <el-table-column label="资料状态" width="130">
          <template #default="{ row }">
            <el-tag :type="diagnosisStatusTag(row.diagnosisStatus)" size="small">{{ row.diagnosisStatusLabel }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="diagnosisDate" label="诊断日期" width="120" />
        <el-table-column prop="managingDoctorName" label="负责医生" width="110" />
        <el-table-column label="管理状态" width="110">
          <template #default="{ row }">
            <el-tag :type="managementStatusTag(row.managementStatus)" size="small">{{ row.managementStatusLabel }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="updateTime" label="最近更新" width="180" />
        <el-table-column label="操作" width="190" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">详情</el-button>
            <el-button
              v-if="row.diagnosisStatus === 'PENDING_REVIEW'"
              link
              type="warning"
              @click="openReview(row)"
            >审核</el-button>
            <el-button
              v-if="row.managementStatus !== 'CLOSED'"
              link
              type="danger"
              @click="openStatus(row)"
            >状态</el-button>
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

    <!-- 新建建档 -->
    <el-dialog v-model="createVisible" title="慢病建档" width="560px">
      <el-alert
        type="info"
        :closable="false"
        show-icon
        title="建档应以已有诊断资料为依据；建档仅登记纳入慢病管理，不代表系统给出诊断结论。"
        class="mb12"
      />
      <el-form ref="createRef" :model="createForm" :rules="createRules" label-width="100px">
        <el-form-item label="居民" prop="residentId">
          <el-select v-model="createForm.residentId" filterable placeholder="请选择居民" style="width: 100%">
            <el-option
              v-for="r in residents"
              :key="r.id"
              :label="`${r.realName || r.residentNo}（${r.residentNo}）`"
              :value="r.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="慢病类型" prop="diseaseType">
          <el-select v-model="createForm.diseaseType" placeholder="请选择" style="width: 100%">
            <el-option v-for="t in DISEASE_TYPES" :key="t.value" :label="t.label" :value="t.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="诊断日期" prop="diagnosisDate">
          <el-date-picker
            v-model="createForm.diagnosisDate"
            type="date"
            value-format="YYYY-MM-DD"
            :disabled-date="disableFuture"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="诊断依据" prop="diagnosisBasis">
          <el-input v-model="createForm.diagnosisBasis" type="textarea" :rows="3" maxlength="500" show-word-limit
                    placeholder="如：上级医院诊断证明、既往病历等" />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="createForm.remark" type="textarea" :rows="2" maxlength="255" show-word-limit placeholder="选填" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitCreate">提交建档</el-button>
      </template>
    </el-dialog>

    <!-- 详情 -->
    <el-dialog v-model="detailVisible" title="慢病档案详情" width="620px">
      <template v-if="current">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="居民">{{ current.residentName }}（{{ current.residentNo }}）</el-descriptions-item>
          <el-descriptions-item label="慢病类型">{{ current.diseaseTypeLabel }}</el-descriptions-item>
          <el-descriptions-item label="资料状态">
            <el-tag :type="diagnosisStatusTag(current.diagnosisStatus)" size="small">{{ current.diagnosisStatusLabel }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="管理状态">
            <el-tag :type="managementStatusTag(current.managementStatus)" size="small">{{ current.managementStatusLabel }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="诊断日期">{{ current.diagnosisDate || '-' }}</el-descriptions-item>
          <el-descriptions-item label="负责医生">{{ current.managingDoctorName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="审核医生">{{ current.reviewedByName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="审核时间">{{ current.reviewedAt || '-' }}</el-descriptions-item>
          <el-descriptions-item label="诊断依据" :span="2">{{ current.diagnosisBasis || '-' }}</el-descriptions-item>
          <el-descriptions-item label="备注" :span="2">{{ current.remark || '-' }}</el-descriptions-item>
        </el-descriptions>

        <el-divider content-position="left">变更记录</el-divider>
        <el-empty v-if="!history.length" description="暂无记录" :image-size="60" />
        <el-timeline v-else>
          <el-timeline-item
            v-for="h in history"
            :key="h.id"
            :timestamp="h.createTime"
            placement="top"
          >
            <div class="hist-head">
              <el-tag size="small">{{ h.actionLabel }}</el-tag>
              <span class="muted">{{ h.operatorName }}</span>
            </div>
            <p class="hist-detail">{{ h.detail || '-' }}</p>
          </el-timeline-item>
        </el-timeline>
      </template>
    </el-dialog>

    <!-- 审核 -->
    <el-dialog v-model="reviewVisible" title="诊断资料审核" width="460px">
      <el-form label-width="90px">
        <el-form-item label="审核结果">
          <el-radio-group v-model="reviewForm.diagnosisStatus">
            <el-radio value="CONFIRMED">资料已核实</el-radio>
            <el-radio value="REJECTED">未通过核实</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="审核意见">
          <el-input v-model="reviewForm.comment" type="textarea" :rows="3" maxlength="500" show-word-limit placeholder="选填" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reviewVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitReview">确认审核</el-button>
      </template>
    </el-dialog>

    <!-- 状态变更 -->
    <el-dialog v-model="statusVisible" title="管理状态变更" width="460px">
      <el-form label-width="90px">
        <el-form-item label="目标状态">
          <el-radio-group v-model="statusForm.managementStatus">
            <el-radio value="ACTIVE">管理中</el-radio>
            <el-radio value="SUSPENDED">暂停管理</el-radio>
            <el-radio value="CLOSED">终止管理</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="变更说明">
          <el-input v-model="statusForm.comment" type="textarea" :rows="3" maxlength="500" show-word-limit placeholder="选填" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="statusVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitStatus">确认变更</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import {
  getChronicList,
  createChronic,
  reviewChronic,
  changeChronicStatus,
  getChronicHistory,
  getMyResidents
} from '@/api/doctor'
import {
  DISEASE_TYPES,
  DIAGNOSIS_STATUSES,
  MANAGEMENT_STATUSES,
  diagnosisStatusTag,
  managementStatusTag
} from '@/utils/labels'
import { ElMessage } from 'element-plus'
import { Plus, Search, Refresh } from '@element-plus/icons-vue'

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const residents = ref([])
const query = reactive({ page: 1, size: 10, keyword: '', diseaseType: '', diagnosisStatus: '', managementStatus: '' })

const submitting = ref(false)
const createVisible = ref(false)
const createRef = ref(null)
const createForm = reactive({ residentId: null, diseaseType: '', diagnosisDate: null, diagnosisBasis: '', remark: '' })
const createRules = {
  residentId: [{ required: true, message: '请选择居民', trigger: 'change' }],
  diseaseType: [{ required: true, message: '请选择慢病类型', trigger: 'change' }]
}

const detailVisible = ref(false)
const current = ref(null)
const history = ref([])

const reviewVisible = ref(false)
const reviewTarget = ref(null)
const reviewForm = reactive({ diagnosisStatus: 'CONFIRMED', comment: '' })

const statusVisible = ref(false)
const statusTarget = ref(null)
const statusForm = reactive({ managementStatus: 'SUSPENDED', comment: '' })

const disableFuture = (date) => date.getTime() > Date.now()

const load = async () => {
  loading.value = true
  try {
    const res = await getChronicList({
      page: query.page,
      size: query.size,
      keyword: query.keyword || undefined,
      diseaseType: query.diseaseType || undefined,
      diagnosisStatus: query.diagnosisStatus || undefined,
      managementStatus: query.managementStatus || undefined
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
  Object.assign(query, { keyword: '', diseaseType: '', diagnosisStatus: '', managementStatus: '', page: 1 })
  load()
}

const openCreate = () => {
  Object.assign(createForm, { residentId: null, diseaseType: '', diagnosisDate: null, diagnosisBasis: '', remark: '' })
  createVisible.value = true
  createRef.value?.clearValidate()
}

const submitCreate = async () => {
  const valid = await createRef.value.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    await createChronic({ ...createForm })
    ElMessage.success('建档成功')
    createVisible.value = false
    load()
  } finally {
    submitting.value = false
  }
}

const openDetail = async (row) => {
  current.value = row
  detailVisible.value = true
  history.value = []
  const res = await getChronicHistory(row.id)
  history.value = res.data || []
}

const openReview = (row) => {
  reviewTarget.value = row
  Object.assign(reviewForm, { diagnosisStatus: 'CONFIRMED', comment: '' })
  reviewVisible.value = true
}

const submitReview = async () => {
  submitting.value = true
  try {
    await reviewChronic(reviewTarget.value.id, { ...reviewForm })
    ElMessage.success('审核完成')
    reviewVisible.value = false
    load()
  } finally {
    submitting.value = false
  }
}

const openStatus = (row) => {
  statusTarget.value = row
  const next = row.managementStatus === 'ACTIVE' ? 'SUSPENDED' : 'ACTIVE'
  Object.assign(statusForm, { managementStatus: next, comment: '' })
  statusVisible.value = true
}

const submitStatus = async () => {
  submitting.value = true
  try {
    await changeChronicStatus(statusTarget.value.id, { ...statusForm })
    ElMessage.success('状态已更新')
    statusVisible.value = false
    load()
  } finally {
    submitting.value = false
  }
}

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

.mb12 {
  margin-bottom: 12px;
}

.muted {
  color: #909399;
  font-size: 12px;
}

.hist-head {
  display: flex;
  align-items: center;
  gap: 8px;
}

.hist-detail {
  margin: 6px 0 0;
  color: #606266;
}
</style>
