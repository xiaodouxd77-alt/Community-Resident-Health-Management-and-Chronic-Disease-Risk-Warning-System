<template>
  <div class="page-container" v-loading="loading">
    <el-page-header @back="$router.push('/doctor/followups')" content="随访计划详情" class="page-header" />

    <template v-if="plan">
      <!-- 计划概况 -->
      <el-card class="section">
        <template #header>
          <div class="card-header">
            <span>{{ plan.planTitle }}</span>
            <div class="header-tags">
              <el-tag :type="planStatusTag(plan.planStatus)" effect="dark">{{ plan.planStatusLabel }}</el-tag>
              <el-tag v-if="plan.overdue" type="danger" effect="plain">已逾期</el-tag>
            </div>
          </div>
        </template>

        <el-descriptions :column="2" border>
          <el-descriptions-item label="居民">
            {{ plan.residentName || '-' }}（{{ plan.residentNo }}）
          </el-descriptions-item>
          <el-descriptions-item label="联系电话">{{ plan.phone || '-' }}</el-descriptions-item>
          <el-descriptions-item label="性别">{{ plan.genderLabel || '-' }}</el-descriptions-item>
          <el-descriptions-item label="出生日期">{{ plan.birthDate || '-' }}</el-descriptions-item>
          <el-descriptions-item label="所属社区">{{ plan.communityName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="负责医生">{{ plan.doctorName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="计划随访时间">{{ plan.scheduledAt }}</el-descriptions-item>
          <el-descriptions-item label="随访方式">{{ plan.followupMethodLabel }}</el-descriptions-item>
          <el-descriptions-item label="随访目的" :span="2">
            {{ plan.followupPurpose || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="居民可见">{{ plan.residentVisible ? '是' : '否' }}</el-descriptions-item>
          <el-descriptions-item label="随访记录数">{{ plan.recordCount ?? 0 }}</el-descriptions-item>
          <el-descriptions-item label="最近随访时间">{{ plan.lastFollowedUpAt || '-' }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ plan.createTime }}</el-descriptions-item>
          <el-descriptions-item label="备注" :span="2">{{ plan.remark || '-' }}</el-descriptions-item>
        </el-descriptions>

        <div class="actions">
          <el-button
            v-if="!isTerminal"
            type="primary"
            :icon="EditPen"
            @click="openRecordDialog"
          >
            填写随访记录
          </el-button>
          <el-button v-if="!isTerminal" @click="openEdit">编辑计划</el-button>
          <el-button v-if="!isTerminal" type="success" plain @click="doComplete">完成计划</el-button>
          <el-button v-if="!isTerminal" type="danger" plain @click="doCancel">取消计划</el-button>
          <span v-if="isTerminal" class="muted">计划已{{ plan.planStatusLabel }}，不可再编辑或填写记录</span>
        </div>
      </el-card>

      <!-- 关联慢病档案 / 预警 -->
      <el-card v-if="plan.chronicDiseaseRecordId || plan.alertId" class="section">
        <template #header><span>关联信息</span></template>
        <el-descriptions :column="1" border>
          <el-descriptions-item v-if="plan.chronicDiseaseRecordId" label="关联慢病档案">
            {{ plan.chronicDiseaseTypeLabel || '慢病档案' }}（档案编号 {{ plan.chronicDiseaseRecordId }}）
            <el-button link type="primary" @click="$router.push('/doctor/chronic')">前往慢病管理</el-button>
          </el-descriptions-item>
          <el-descriptions-item v-if="plan.alertId" label="关联预警">
            {{ plan.alertTitle || '预警' }}
            <el-tag :type="riskTag(plan.alertRiskLevel)" size="small" class="ml6">
              {{ plan.alertRiskLevelLabel }}
            </el-tag>
            <el-button link type="primary" @click="$router.push(`/doctor/alerts/${plan.alertId}`)">查看</el-button>
          </el-descriptions-item>
        </el-descriptions>
      </el-card>

      <!-- 随访记录 -->
      <el-card class="section">
        <template #header>
          <div class="card-header">
            <span>随访记录（按时间倒序）</span>
            <el-button v-if="!isTerminal" type="primary" size="small" :icon="Plus" @click="openRecordDialog">
              新增记录
            </el-button>
          </div>
        </template>

        <el-empty v-if="!records.length" description="暂无随访记录" />
        <el-timeline v-else>
          <el-timeline-item
            v-for="r in records"
            :key="r.id"
            :timestamp="r.followedUpAt"
            placement="top"
            type="primary"
          >
            <div class="record">
              <div class="record-head">
                <el-tag size="small">{{ r.followupMethodLabel }}</el-tag>
                <span class="muted">随访医生：{{ r.doctorName || '-' }}</span>
                <el-tag v-if="r.residentVisible" size="small" type="success" effect="plain">居民可见</el-tag>
                <el-tag v-else size="small" type="info" effect="plain">仅医生可见</el-tag>
              </div>
              <el-descriptions :column="1" size="small" class="record-body">
                <el-descriptions-item v-if="r.residentFeedback" label="居民反馈">
                  {{ r.residentFeedback }}
                </el-descriptions-item>
                <el-descriptions-item v-if="r.healthSummary" label="健康情况摘要">
                  {{ r.healthSummary }}
                </el-descriptions-item>
                <el-descriptions-item v-if="r.indicatorSummary" label="指标变化摘要">
                  {{ r.indicatorSummary }}
                </el-descriptions-item>
                <el-descriptions-item v-if="r.interventionAdvice" label="干预建议">
                  {{ r.interventionAdvice }}
                </el-descriptions-item>
                <el-descriptions-item v-if="r.followupResult" label="随访结果">
                  {{ r.followupResult }}
                </el-descriptions-item>
                <el-descriptions-item v-if="r.nextFollowupAt" label="建议下次随访">
                  {{ r.nextFollowupAt }}
                </el-descriptions-item>
                <el-descriptions-item v-if="r.remark" label="备注">{{ r.remark }}</el-descriptions-item>
              </el-descriptions>
            </div>
          </el-timeline-item>
        </el-timeline>

        <el-pagination
          v-if="recordTotal > recordQuery.size"
          v-model:current-page="recordQuery.page"
          v-model:page-size="recordQuery.size"
          :total="recordTotal"
          layout="total, prev, pager, next"
          class="pager"
          @current-change="loadRecords"
        />
      </el-card>
    </template>

    <!-- 填写随访记录 -->
    <el-dialog v-model="recordVisible" title="填写随访记录" width="640px">
      <el-form ref="recordFormRef" :model="recordForm" :rules="recordRules" label-width="120px">
        <el-form-item label="实际随访时间" prop="followedUpAt">
          <el-date-picker
            v-model="recordForm.followedUpAt"
            type="datetime"
            value-format="YYYY-MM-DD HH:mm:ss"
            placeholder="选择实际随访时间"
            :disabled-date="disableFuture"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="随访方式" prop="followupMethod">
          <el-select v-model="recordForm.followupMethod" placeholder="请选择" style="width: 100%">
            <el-option v-for="m in FOLLOWUP_METHODS" :key="m.value" :label="m.label" :value="m.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="居民反馈" prop="residentFeedback">
          <el-input v-model="recordForm.residentFeedback" type="textarea" :rows="2" maxlength="500" show-word-limit placeholder="选填" />
        </el-form-item>
        <el-form-item label="健康情况摘要" prop="healthSummary">
          <el-input v-model="recordForm.healthSummary" type="textarea" :rows="2" maxlength="500" show-word-limit placeholder="选填" />
        </el-form-item>
        <el-form-item label="指标变化摘要" prop="indicatorSummary">
          <el-input v-model="recordForm.indicatorSummary" type="textarea" :rows="2" maxlength="500" show-word-limit placeholder="选填" />
        </el-form-item>
        <el-form-item label="干预建议" prop="interventionAdvice">
          <el-input v-model="recordForm.interventionAdvice" type="textarea" :rows="2" maxlength="500" show-word-limit placeholder="选填" />
        </el-form-item>
        <el-form-item label="随访结果" prop="followupResult">
          <el-input v-model="recordForm.followupResult" type="textarea" :rows="2" maxlength="500" show-word-limit placeholder="本次随访的结论（非诊断结论）" />
        </el-form-item>
        <el-form-item label="建议下次随访">
          <el-date-picker
            v-model="recordForm.nextFollowupAt"
            type="datetime"
            value-format="YYYY-MM-DD HH:mm:ss"
            placeholder="选填"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="居民可见">
          <el-switch v-model="recordForm.residentVisible" />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="recordForm.remark" type="textarea" :rows="2" maxlength="255" show-word-limit placeholder="选填，居民不可见" />
        </el-form-item>
        <el-form-item label="提交后处理">
          <el-checkbox v-model="recordForm.complete">同时将计划标记为已完成</el-checkbox>
          <el-checkbox v-model="recordForm.createNextPlan">按建议时间生成下一次计划</el-checkbox>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="recordVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitRecord">提交记录</el-button>
      </template>
    </el-dialog>

    <!-- 编辑计划 -->
    <el-dialog v-model="editVisible" title="编辑随访计划" width="560px">
      <el-form ref="editFormRef" :model="editForm" :rules="editRules" label-width="100px">
        <el-form-item label="计划标题" prop="planTitle">
          <el-input v-model="editForm.planTitle" maxlength="100" />
        </el-form-item>
        <el-form-item label="计划随访时间" prop="scheduledAt">
          <el-date-picker
            v-model="editForm.scheduledAt"
            type="datetime"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="随访方式" prop="followupMethod">
          <el-select v-model="editForm.followupMethod" style="width: 100%">
            <el-option v-for="m in FOLLOWUP_METHODS" :key="m.value" :label="m.label" :value="m.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="随访目的" prop="followupPurpose">
          <el-input v-model="editForm.followupPurpose" type="textarea" :rows="2" maxlength="500" show-word-limit />
        </el-form-item>
        <el-form-item label="居民可见">
          <el-switch v-model="editForm.residentVisible" />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="editForm.remark" type="textarea" :rows="2" maxlength="255" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="editSubmitting" @click="submitEdit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, EditPen } from '@element-plus/icons-vue'
import {
  getFollowupPlanDetail,
  updateFollowupPlan,
  cancelFollowupPlan,
  completeFollowupPlan,
  getFollowupPlanRecords,
  createFollowupRecord
} from '@/api/doctor'
import { FOLLOWUP_METHODS, planStatusTag, riskTag } from '@/utils/labels'

const route = useRoute()
const loading = ref(false)
const plan = ref(null)
const records = ref([])
const recordTotal = ref(0)
const recordQuery = reactive({ page: 1, size: 10 })

const isTerminal = computed(() => {
  const s = plan.value?.planStatus
  return s === 'COMPLETED' || s === 'CANCELLED'
})

const nowStr = () => {
  const d = new Date()
  const p = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`
}

// ---------------- 加载 ----------------
const load = async () => {
  loading.value = true
  try {
    const res = await getFollowupPlanDetail(route.params.id)
    plan.value = res.data
    await loadRecords()
  } finally {
    loading.value = false
  }
}

const loadRecords = async () => {
  const res = await getFollowupPlanRecords(route.params.id, { page: recordQuery.page, size: recordQuery.size })
  records.value = res.data.records || []
  recordTotal.value = res.data.total || 0
}

// ---------------- 填写记录 ----------------
const recordVisible = ref(false)
const submitting = ref(false)
const recordFormRef = ref(null)
const recordForm = reactive({
  followedUpAt: '',
  followupMethod: 'PHONE',
  residentFeedback: '',
  healthSummary: '',
  indicatorSummary: '',
  interventionAdvice: '',
  followupResult: '',
  nextFollowupAt: null,
  residentVisible: true,
  remark: '',
  complete: false,
  createNextPlan: false
})
const recordRules = {
  followedUpAt: [{ required: true, message: '请选择实际随访时间', trigger: 'change' }],
  followupMethod: [{ required: true, message: '请选择随访方式', trigger: 'change' }]
}

const openRecordDialog = () => {
  Object.assign(recordForm, {
    followedUpAt: nowStr(),
    followupMethod: plan.value?.followupMethod || 'PHONE',
    residentFeedback: '',
    healthSummary: '',
    indicatorSummary: '',
    interventionAdvice: '',
    followupResult: '',
    nextFollowupAt: null,
    residentVisible: true,
    remark: '',
    complete: false,
    createNextPlan: false
  })
  recordVisible.value = true
  recordFormRef.value?.clearValidate()
}

const submitRecord = async () => {
  const valid = await recordFormRef.value.validate().catch(() => false)
  if (!valid) return
  if (recordForm.createNextPlan && !recordForm.nextFollowupAt) {
    ElMessage.warning('勾选“生成下一次计划”时需填写建议下次随访时间')
    return
  }
  submitting.value = true
  try {
    await createFollowupRecord(route.params.id, {
      followedUpAt: recordForm.followedUpAt,
      followupMethod: recordForm.followupMethod,
      residentFeedback: recordForm.residentFeedback || undefined,
      healthSummary: recordForm.healthSummary || undefined,
      indicatorSummary: recordForm.indicatorSummary || undefined,
      interventionAdvice: recordForm.interventionAdvice || undefined,
      followupResult: recordForm.followupResult || undefined,
      nextFollowupAt: recordForm.nextFollowupAt || undefined,
      residentVisible: recordForm.residentVisible,
      remark: recordForm.remark || undefined,
      complete: recordForm.complete || undefined,
      createNextPlan: recordForm.createNextPlan || undefined
    })
    recordVisible.value = false
    ElMessage.success('随访记录已保存')
    await load()
  } finally {
    submitting.value = false
  }
}

// ---------------- 编辑计划 ----------------
const editVisible = ref(false)
const editSubmitting = ref(false)
const editFormRef = ref(null)
const editForm = reactive({
  planTitle: '',
  scheduledAt: null,
  followupMethod: 'PHONE',
  followupPurpose: '',
  residentVisible: true,
  remark: ''
})
const editRules = {
  planTitle: [{ required: true, message: '请输入计划标题', trigger: 'blur' }],
  scheduledAt: [{ required: true, message: '请选择计划随访时间', trigger: 'change' }],
  followupMethod: [{ required: true, message: '请选择随访方式', trigger: 'change' }]
}

const openEdit = () => {
  Object.assign(editForm, {
    planTitle: plan.value.planTitle,
    scheduledAt: plan.value.scheduledAt,
    followupMethod: plan.value.followupMethod,
    followupPurpose: plan.value.followupPurpose || '',
    residentVisible: plan.value.residentVisible !== false,
    remark: plan.value.remark || ''
  })
  editVisible.value = true
  editFormRef.value?.clearValidate()
}

const submitEdit = async () => {
  const valid = await editFormRef.value.validate().catch(() => false)
  if (!valid) return
  editSubmitting.value = true
  try {
    await updateFollowupPlan(route.params.id, {
      planTitle: editForm.planTitle,
      scheduledAt: editForm.scheduledAt,
      followupMethod: editForm.followupMethod,
      followupPurpose: editForm.followupPurpose || undefined,
      residentVisible: editForm.residentVisible,
      remark: editForm.remark || undefined
    })
    editVisible.value = false
    ElMessage.success('计划已更新')
    load()
  } finally {
    editSubmitting.value = false
  }
}

// ---------------- 完成 / 取消 ----------------
const doComplete = async () => {
  await ElMessageBox.confirm(`确认将计划「${plan.value.planTitle}」标记为已完成？`, '提示', { type: 'warning' })
  await completeFollowupPlan(route.params.id)
  ElMessage.success('计划已完成')
  load()
}

const doCancel = async () => {
  await ElMessageBox.confirm(`确认取消计划「${plan.value.planTitle}」？取消后不可再填写记录。`, '提示', { type: 'warning' })
  await cancelFollowupPlan(route.params.id)
  ElMessage.success('计划已取消')
  load()
}

const disableFuture = (date) => date.getTime() > Date.now()

onMounted(load)
watch(() => route.params.id, load)
</script>

<style scoped>
.page-container {
  max-width: 1100px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 16px;
}

.section {
  margin-bottom: 16px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-tags {
  display: flex;
  gap: 8px;
}

.actions {
  margin-top: 16px;
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  align-items: center;
}

.record-head {
  display: flex;
  align-items: center;
  gap: 10px;
}

.record-body {
  margin-top: 8px;
}

.pager {
  margin-top: 16px;
  justify-content: flex-end;
}

.muted {
  color: #909399;
  font-size: 13px;
}

.ml6 {
  margin-left: 6px;
}
</style>
