<template>
  <div class="page-container" v-loading="loading">
    <el-page-header @back="$router.push('/doctor/interventions')" content="干预方案详情" class="page-header" />

    <template v-if="plan">
      <el-card class="section">
        <template #header>
          <div class="card-header">
            <span>{{ plan.planName }}</span>
            <div class="header-tags">
              <el-tag :type="interventionStatusTag(plan.planStatus)" effect="dark">{{ plan.planStatusLabel }}</el-tag>
              <el-tag v-if="plan.latestExecutionStatus" :type="executionStatusTag(plan.latestExecutionStatus)" effect="plain">
                最近：{{ plan.latestExecutionStatusLabel }}
              </el-tag>
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
          <el-descriptions-item label="干预类型">{{ plan.interventionTypeLabel }}</el-descriptions-item>
          <el-descriptions-item label="起止日期">{{ plan.startDate || '-' }} ~ {{ plan.endDate || '-' }}</el-descriptions-item>
          <el-descriptions-item label="居民可见">{{ plan.residentVisible ? '是' : '否' }}</el-descriptions-item>
          <el-descriptions-item label="执行记录数">{{ plan.recordCount ?? 0 }}</el-descriptions-item>
          <el-descriptions-item label="干预目标" :span="2">{{ plan.interventionGoal || '-' }}</el-descriptions-item>
          <el-descriptions-item label="干预措施" :span="2">{{ plan.interventionContent || '-' }}</el-descriptions-item>
          <el-descriptions-item label="备注" :span="2">{{ plan.remark || '-' }}</el-descriptions-item>
        </el-descriptions>

        <div class="actions">
          <el-button v-if="canActivate" type="primary" @click="doAction('activate', '启动')">
            {{ plan.planStatus === 'SUSPENDED' ? '恢复执行' : '启动方案' }}
          </el-button>
          <el-button v-if="canSuspend" @click="doAction('suspend', '暂停')">暂停</el-button>
          <el-button v-if="canComplete" type="success" plain @click="doAction('complete', '完成')">完成方案</el-button>
          <el-button v-if="canCancel" type="danger" plain @click="doAction('cancel', '取消')">取消</el-button>
          <el-button v-if="canAddRecord" type="primary" :icon="EditPen" @click="openRecordDialog">新增执行记录</el-button>
          <span v-if="isTerminal" class="muted">方案已{{ plan.planStatusLabel }}，不可再变更或记录</span>
          <span v-else-if="plan.planStatus === 'DRAFT'" class="muted">草稿方案需先启动执行</span>
        </div>
      </el-card>

      <!-- 阶段性效果 -->
      <el-card class="section">
        <template #header><span>阶段性效果</span></template>
        <el-descriptions :column="3" border>
          <el-descriptions-item label="记录总数">{{ effect.recordCount ?? 0 }}</el-descriptions-item>
          <el-descriptions-item label="医生记录">{{ effect.doctorRecordCount ?? 0 }}</el-descriptions-item>
          <el-descriptions-item label="居民反馈">{{ effect.residentFeedbackCount ?? 0 }}</el-descriptions-item>
          <el-descriptions-item label="最近执行时间">{{ effect.lastExecutedAt || '-' }}</el-descriptions-item>
          <el-descriptions-item label="最近执行情况">{{ effect.latestExecutionStatusLabel || '-' }}</el-descriptions-item>
          <el-descriptions-item label="需跟进">
            <el-tag :type="effect.needsAttention ? 'danger' : 'success'" size="small">
              {{ effect.needsAttention ? '需要' : '正常' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="效果摘要" :span="3">{{ effect.latestEffectSummary || '-' }}</el-descriptions-item>
          <el-descriptions-item label="后续计划" :span="3">{{ effect.latestNextAction || '-' }}</el-descriptions-item>
        </el-descriptions>
      </el-card>

      <!-- 关联信息 -->
      <el-card v-if="plan.chronicDiseaseRecordId || plan.alertId || plan.followupPlanId" class="section">
        <template #header><span>关联信息</span></template>
        <el-descriptions :column="1" border>
          <el-descriptions-item v-if="plan.chronicDiseaseRecordId" label="关联慢病档案">
            {{ plan.chronicDiseaseTypeLabel || '慢病档案' }}（档案编号 {{ plan.chronicDiseaseRecordId }}）
            <el-button link type="primary" @click="$router.push('/doctor/chronic')">前往慢病管理</el-button>
          </el-descriptions-item>
          <el-descriptions-item v-if="plan.alertId" label="关联预警">
            {{ plan.alertTitle || '预警' }}
            <el-tag :type="riskTag(plan.alertRiskLevel)" size="small" class="ml6">{{ plan.alertRiskLevelLabel }}</el-tag>
            <el-button link type="primary" @click="$router.push(`/doctor/alerts/${plan.alertId}`)">查看</el-button>
          </el-descriptions-item>
          <el-descriptions-item v-if="plan.followupPlanId" label="关联随访计划">
            {{ plan.followupPlanTitle || '随访计划' }}
            <el-button link type="primary" @click="$router.push(`/doctor/followups/plan/${plan.followupPlanId}`)">查看</el-button>
          </el-descriptions-item>
        </el-descriptions>
      </el-card>

      <!-- 执行记录 -->
      <el-card class="section">
        <template #header>
          <div class="card-header">
            <span>执行记录（按时间倒序）</span>
            <el-button v-if="canAddRecord" type="primary" size="small" :icon="Plus" @click="openRecordDialog">新增记录</el-button>
          </div>
        </template>

        <el-empty v-if="!records.length" description="暂无执行记录" />
        <el-timeline v-else>
          <el-timeline-item
            v-for="r in records"
            :key="r.id"
            :timestamp="r.executedAt"
            placement="top"
            :type="r.operatorRole === 'RESIDENT' ? 'success' : 'primary'"
          >
            <div class="record">
              <div class="record-head">
                <el-tag :type="operatorRoleTag(r.operatorRole)" size="small">{{ r.operatorRoleLabel }}</el-tag>
                <span class="muted">{{ r.operatorName || '-' }}</span>
                <el-tag v-if="r.executionStatus" :type="executionStatusTag(r.executionStatus)" size="small" effect="plain">
                  {{ r.executionStatusLabel }}
                </el-tag>
                <el-tag v-if="!r.residentVisible" size="small" type="info" effect="plain">仅医生可见</el-tag>
              </div>
              <el-descriptions :column="1" size="small" class="record-body">
                <el-descriptions-item v-if="r.executionContent" label="执行内容">{{ r.executionContent }}</el-descriptions-item>
                <el-descriptions-item v-if="r.residentFeedback" label="居民反馈">{{ r.residentFeedback }}</el-descriptions-item>
                <el-descriptions-item v-if="r.effectSummary" label="效果摘要">{{ r.effectSummary }}</el-descriptions-item>
                <el-descriptions-item v-if="r.nextAction" label="后续计划">{{ r.nextAction }}</el-descriptions-item>
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

    <!-- 新增执行记录 -->
    <el-dialog v-model="recordVisible" title="新增干预执行记录" width="640px">
      <el-form ref="recordFormRef" :model="recordForm" :rules="recordRules" label-width="120px">
        <el-form-item label="实际执行时间" prop="executedAt">
          <el-date-picker
            v-model="recordForm.executedAt"
            type="datetime"
            value-format="YYYY-MM-DD HH:mm:ss"
            placeholder="选择时间"
            :disabled-date="disableFuture"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="执行情况" prop="executionStatus">
          <el-select v-model="recordForm.executionStatus" placeholder="选填" style="width: 100%">
            <el-option v-for="s in EXECUTION_STATUSES" :key="s.value" :label="s.label" :value="s.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="执行内容" prop="executionContent">
          <el-input v-model="recordForm.executionContent" type="textarea" :rows="2" maxlength="1000" show-word-limit placeholder="选填" />
        </el-form-item>
        <el-form-item label="居民反馈" prop="residentFeedback">
          <el-input v-model="recordForm.residentFeedback" type="textarea" :rows="2" maxlength="500" show-word-limit placeholder="选填，记录居民口头反馈" />
        </el-form-item>
        <el-form-item label="阶段性效果" prop="effectSummary">
          <el-input v-model="recordForm.effectSummary" type="textarea" :rows="2" maxlength="500" show-word-limit placeholder="选填" />
        </el-form-item>
        <el-form-item label="后续计划" prop="nextAction">
          <el-input v-model="recordForm.nextAction" type="textarea" :rows="2" maxlength="500" show-word-limit placeholder="选填" />
        </el-form-item>
        <el-form-item label="居民可见">
          <el-switch v-model="recordForm.residentVisible" />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="recordForm.remark" type="textarea" :rows="2" maxlength="255" show-word-limit placeholder="选填，居民不可见" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="recordVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitRecord">提交记录</el-button>
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
  getInterventionPlanDetail,
  getInterventionPlanEffect,
  activateInterventionPlan,
  suspendInterventionPlan,
  completeInterventionPlan,
  cancelInterventionPlan,
  getInterventionPlanRecords,
  createInterventionRecord
} from '@/api/doctor'
import {
  EXECUTION_STATUSES,
  interventionStatusTag,
  executionStatusTag,
  operatorRoleTag,
  riskTag
} from '@/utils/labels'

const route = useRoute()
const loading = ref(false)
const plan = ref(null)
const effect = ref({})
const records = ref([])
const recordTotal = ref(0)
const recordQuery = reactive({ page: 1, size: 10 })

const isTerminal = computed(() => {
  const s = plan.value?.planStatus
  return s === 'COMPLETED' || s === 'CANCELLED'
})
const canActivate = computed(() => ['DRAFT', 'SUSPENDED'].includes(plan.value?.planStatus))
const canSuspend = computed(() => plan.value?.planStatus === 'ACTIVE')
const canComplete = computed(() => ['ACTIVE', 'SUSPENDED'].includes(plan.value?.planStatus))
const canCancel = computed(() => !isTerminal.value)
const canAddRecord = computed(() => ['ACTIVE', 'SUSPENDED'].includes(plan.value?.planStatus))

const nowStr = () => {
  const d = new Date()
  const p = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`
}

const load = async () => {
  loading.value = true
  try {
    const [planRes, effectRes] = await Promise.all([
      getInterventionPlanDetail(route.params.id),
      getInterventionPlanEffect(route.params.id)
    ])
    plan.value = planRes.data
    effect.value = effectRes.data || {}
    await loadRecords()
  } finally {
    loading.value = false
  }
}

const loadRecords = async () => {
  const res = await getInterventionPlanRecords(route.params.id, { page: recordQuery.page, size: recordQuery.size })
  records.value = res.data.records || []
  recordTotal.value = res.data.total || 0
}

// ---------------- 状态操作 ----------------
const actions = {
  activate: activateInterventionPlan,
  suspend: suspendInterventionPlan,
  complete: completeInterventionPlan,
  cancel: cancelInterventionPlan
}
const confirmText = {
  activate: '确认启动该干预方案？',
  suspend: '确认暂停该干预方案？',
  complete: '确认将该方案标记为已完成？',
  cancel: '确认取消该方案？取消后不可再记录。'
}

const doAction = async (type, label) => {
  await ElMessageBox.confirm(confirmText[type], '提示', { type: 'warning' })
  await actions[type](route.params.id)
  ElMessage.success(`已${label}`)
  load()
}

// ---------------- 新增执行记录 ----------------
const recordVisible = ref(false)
const submitting = ref(false)
const recordFormRef = ref(null)
const recordForm = reactive({
  executedAt: '',
  executionStatus: '',
  executionContent: '',
  residentFeedback: '',
  effectSummary: '',
  nextAction: '',
  residentVisible: true,
  remark: ''
})
const recordRules = {
  executedAt: [{ required: true, message: '请选择实际执行时间', trigger: 'change' }]
}

const openRecordDialog = () => {
  Object.assign(recordForm, {
    executedAt: nowStr(),
    executionStatus: '',
    executionContent: '',
    residentFeedback: '',
    effectSummary: '',
    nextAction: '',
    residentVisible: true,
    remark: ''
  })
  recordVisible.value = true
  recordFormRef.value?.clearValidate()
}

const submitRecord = async () => {
  const valid = await recordFormRef.value.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    await createInterventionRecord(route.params.id, {
      executedAt: recordForm.executedAt,
      executionStatus: recordForm.executionStatus || undefined,
      executionContent: recordForm.executionContent || undefined,
      residentFeedback: recordForm.residentFeedback || undefined,
      effectSummary: recordForm.effectSummary || undefined,
      nextAction: recordForm.nextAction || undefined,
      residentVisible: recordForm.residentVisible,
      remark: recordForm.remark || undefined
    })
    recordVisible.value = false
    ElMessage.success('执行记录已保存')
    await load()
  } finally {
    submitting.value = false
  }
}

const disableFuture = (date) => date.getTime() > Date.now()

onMounted(load)
watch(() => route.params.id, load)
</script>

<style scoped>
.page-container {
  max-width: 1120px;
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
  flex-wrap: wrap;
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
