<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>我的健康干预</span>
          <el-button :icon="Refresh" size="small" @click="load">刷新</el-button>
        </div>
      </template>

      <el-table v-loading="loading" :data="plans" style="width: 100%" empty-text="暂无健康干预方案">
        <el-table-column prop="planName" label="方案名称" min-width="180" show-overflow-tooltip />
        <el-table-column prop="interventionTypeLabel" label="干预类型" width="130" />
        <el-table-column prop="doctorName" label="负责医生" width="110" />
        <el-table-column label="起止日期" width="200">
          <template #default="{ row }">{{ row.startDate || '-' }} ~ {{ row.endDate || '-' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="interventionStatusTag(row.planStatus)" size="small">{{ row.planStatusLabel }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="110" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">查看详情</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="detailVisible" title="干预方案详情" width="720px">
      <template v-if="detail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="方案名称" :span="2">{{ detail.planName }}</el-descriptions-item>
          <el-descriptions-item label="干预类型">{{ detail.interventionTypeLabel }}</el-descriptions-item>
          <el-descriptions-item label="负责医生">{{ detail.doctorName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="起止日期">{{ detail.startDate || '-' }} ~ {{ detail.endDate || '-' }}</el-descriptions-item>
          <el-descriptions-item label="状态">{{ detail.planStatusLabel }}</el-descriptions-item>
          <el-descriptions-item label="干预目标" :span="2">{{ detail.interventionGoal || '-' }}</el-descriptions-item>
          <el-descriptions-item label="干预措施 / 医生建议" :span="2">{{ detail.interventionContent || '-' }}</el-descriptions-item>
        </el-descriptions>

        <el-alert
          type="info"
          :closable="false"
          show-icon
          class="notice"
          title="干预建议仅用于日常健康管理，不作为疾病诊断。如有不适请及时就医。"
        />

        <el-divider content-position="left">执行记录</el-divider>
        <el-empty v-if="!records.length" description="暂无公开的执行记录" />
        <el-timeline v-else>
          <el-timeline-item
            v-for="r in records"
            :key="r.id"
            :timestamp="r.executedAt"
            placement="top"
            :type="r.operatorRole === 'RESIDENT' ? 'success' : 'primary'"
          >
            <div class="record-head">
              <el-tag :type="operatorRoleTag(r.operatorRole)" size="small">{{ r.operatorRoleLabel }}</el-tag>
              <el-tag v-if="r.executionStatus" :type="executionStatusTag(r.executionStatus)" size="small" effect="plain">
                {{ r.executionStatusLabel }}
              </el-tag>
            </div>
            <p v-if="r.executionContent" class="record-line">执行内容：{{ r.executionContent }}</p>
            <p v-if="r.residentFeedback" class="record-line">反馈：{{ r.residentFeedback }}</p>
            <p v-if="r.nextAction" class="record-line">后续安排：{{ r.nextAction }}</p>
          </el-timeline-item>
        </el-timeline>

        <template v-if="canFeedback">
          <el-divider content-position="left">提交执行反馈</el-divider>
          <el-form ref="feedbackFormRef" :model="feedbackForm" :rules="feedbackRules" label-width="100px">
            <el-form-item label="反馈内容" prop="residentFeedback">
              <el-input v-model="feedbackForm.residentFeedback" type="textarea" :rows="2" maxlength="500" show-word-limit placeholder="请填写您的执行情况或建议" />
            </el-form-item>
            <el-form-item label="执行情况">
              <el-select v-model="feedbackForm.executionStatus" clearable placeholder="选填" style="width: 200px">
                <el-option v-for="s in EXECUTION_STATUSES" :key="s.value" :label="s.label" :value="s.value" />
              </el-select>
            </el-form-item>
            <el-form-item label="反馈时间">
              <el-date-picker
                v-model="feedbackForm.executedAt"
                type="datetime"
                value-format="YYYY-MM-DD HH:mm:ss"
                placeholder="默认当前时间"
                :disabled-date="disableFuture"
                style="width: 240px"
              />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="submitting" @click="submitFeedback">提交反馈</el-button>
            </el-form-item>
          </el-form>
        </template>
        <el-alert
          v-else
          type="warning"
          :closable="false"
          show-icon
          title="方案当前状态不可提交反馈"
        />
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import {
  getMyInterventionPlans,
  getMyInterventionPlanDetail,
  getMyInterventionPlanRecords,
  submitInterventionFeedback
} from '@/api/health'
import {
  EXECUTION_STATUSES,
  interventionStatusTag,
  executionStatusTag,
  operatorRoleTag
} from '@/utils/labels'

const loading = ref(false)
const plans = ref([])
const detailVisible = ref(false)
const detail = ref(null)
const records = ref([])
const submitting = ref(false)
const feedbackFormRef = ref(null)

const feedbackForm = reactive({
  residentFeedback: '',
  executionStatus: '',
  executedAt: null
})
const feedbackRules = {
  residentFeedback: [{ required: true, message: '请填写反馈内容', trigger: 'blur' }]
}

const canFeedback = computed(() => ['ACTIVE', 'SUSPENDED'].includes(detail.value?.planStatus))

const load = async () => {
  loading.value = true
  try {
    const res = await getMyInterventionPlans()
    plans.value = res.data || []
  } finally {
    loading.value = false
  }
}

const loadRecords = async (planId) => {
  const res = await getMyInterventionPlanRecords(planId, { page: 1, size: 50 })
  records.value = res.data.records || []
}

const openDetail = async (row) => {
  const res = await getMyInterventionPlanDetail(row.id)
  detail.value = res.data
  resetFeedbackForm()
  detailVisible.value = true
  await loadRecords(row.id)
}

const resetFeedbackForm = () => {
  feedbackForm.residentFeedback = ''
  feedbackForm.executionStatus = ''
  feedbackForm.executedAt = null
  feedbackFormRef.value?.clearValidate()
}

const submitFeedback = async () => {
  const valid = await feedbackFormRef.value.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    await submitInterventionFeedback(detail.value.id, {
      residentFeedback: feedbackForm.residentFeedback,
      executionStatus: feedbackForm.executionStatus || undefined,
      executedAt: feedbackForm.executedAt || undefined
    })
    ElMessage.success('反馈已提交')
    resetFeedbackForm()
    await loadRecords(detail.value.id)
  } finally {
    submitting.value = false
  }
}

const disableFuture = (date) => date.getTime() > Date.now()

onMounted(load)
</script>

<style scoped>
.page-container {
  max-width: 1120px;
  margin: 0 auto;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.notice {
  margin-top: 14px;
}

.record-head {
  display: flex;
  align-items: center;
  gap: 8px;
}

.record-line {
  margin: 6px 0 0;
  color: #606266;
}
</style>
