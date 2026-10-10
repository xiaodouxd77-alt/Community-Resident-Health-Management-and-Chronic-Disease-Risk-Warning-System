<template>
  <div class="page-container" v-loading="loading">
    <el-page-header @back="$router.push('/doctor/alerts')" content="预警处理" class="page-header" />

    <template v-if="detail">
      <el-card class="section">
        <template #header>
          <div class="card-header">
            <span>{{ detail.alert.alertTitle }}</span>
            <el-tag :type="statusTag(detail.alert.status)" effect="dark">{{ detail.alert.statusLabel }}</el-tag>
          </div>
        </template>

        <el-descriptions :column="2" border>
          <el-descriptions-item label="预警编号">{{ detail.alert.alertNo }}</el-descriptions-item>
          <el-descriptions-item label="风险等级">
            <el-tag :type="riskTag(detail.alert.riskLevel)" effect="dark" size="small">
              {{ detail.alert.riskLevelLabel }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="居民">
            {{ residentText }}
          </el-descriptions-item>
          <el-descriptions-item label="联系电话">{{ detail.resident?.phone || '-' }}</el-descriptions-item>
          <el-descriptions-item label="指标类型">{{ detail.alert.indicatorTypeLabel }}</el-descriptions-item>
          <el-descriptions-item label="测量值">{{ formatIndicatorValue(detail.alert) }}</el-descriptions-item>
          <el-descriptions-item label="测量时间">{{ detail.alert.measuredAt }}</el-descriptions-item>
          <el-descriptions-item label="预警时间">{{ detail.alert.createTime }}</el-descriptions-item>
          <el-descriptions-item label="触发原因" :span="2">
            {{ detail.alert.alertReason }}
          </el-descriptions-item>
        </el-descriptions>
      </el-card>

      <el-card class="section">
        <template #header><span>风险评估依据</span></template>
        <el-alert
          v-if="detail.assessment"
          :type="detail.assessment.riskLevel === 'LOW' ? 'info' : 'warning'"
          :closable="false"
          show-icon
          :title="detail.assessment.assessmentSummary"
        />
        <el-table
          v-if="detail.assessment?.matchedRules?.length"
          :data="detail.assessment.matchedRules"
          size="small"
          style="margin-top: 14px"
        >
          <el-table-column prop="ruleName" label="命中规则" width="160" />
          <el-table-column label="判定" width="220">
            <template #default="{ row }">
              {{ row.valueFieldLabel }} {{ row.actualValue }} {{ row.operator }} {{ row.thresholdValue }}
            </template>
          </el-table-column>
          <el-table-column label="风险" width="90">
            <template #default="{ row }">
              <el-tag :type="riskTag(row.riskLevel)" size="small">{{ row.riskLevelLabel }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="description" label="说明" min-width="260" show-overflow-tooltip />
        </el-table>
      </el-card>

      <el-card class="section">
        <template #header><span>处理记录</span></template>
        <el-form
          ref="formRef"
          :model="handleForm"
          :rules="handleRules"
          label-width="90px"
          class="handle-form"
        >
          <el-form-item label="处理动作" prop="actionType">
            <el-radio-group v-model="handleForm.actionType" :disabled="!availableActions.length">
              <el-radio-button
                v-for="action in availableActions"
                :key="action.value"
                :value="action.value"
              >
                {{ action.label }}
              </el-radio-button>
            </el-radio-group>
            <span v-if="!availableActions.length" class="muted block-hint">该预警已完成处理</span>
          </el-form-item>

          <el-form-item label="处理意见" prop="handlingComment">
            <el-input
              v-model="handleForm.handlingComment"
              type="textarea"
              :rows="3"
              maxlength="500"
              show-word-limit
              :disabled="!availableActions.length"
              placeholder="请填写诊断/处置意见，居民可在其预警详情中看到"
            />
          </el-form-item>

          <el-form-item label="需要随访">
            <el-switch v-model="handleForm.followupRequired" :disabled="!availableActions.length" />
          </el-form-item>

          <el-form-item v-if="handleForm.followupRequired" label="随访时间">
            <el-date-picker
              v-model="handleForm.nextFollowupAt"
              type="datetime"
              value-format="YYYY-MM-DD HH:mm:ss"
              placeholder="选择建议随访时间"
              :disabled-date="disablePast"
              style="width: 260px"
            />
          </el-form-item>

          <el-form-item v-if="availableActions.length">
            <el-button type="primary" :loading="submitting" @click="submitHandle">提交处理</el-button>
          </el-form-item>
        </el-form>

        <el-divider content-position="left">历史处理意见</el-divider>
        <el-empty v-if="!detail.handlingRecords?.length" description="尚未处理" />
        <el-timeline v-else>
          <el-timeline-item
            v-for="record in detail.handlingRecords"
            :key="record.id"
            :timestamp="record.createTime"
            placement="top"
            type="primary"
          >
            <div class="handling-item">
              <div class="handling-head">
                <el-tag size="small">{{ record.actionLabel }}</el-tag>
                <span class="muted">{{ record.doctorName }}</span>
              </div>
              <p>{{ record.handlingComment }}</p>
              <p v-if="record.followupRequired" class="muted">
                需要随访<span v-if="record.nextFollowupAt">，建议时间：{{ record.nextFollowupAt }}</span>
              </p>
            </div>
          </el-timeline-item>
        </el-timeline>
      </el-card>

      <el-card class="section">
        <template #header><span>该居民同类指标历史</span></template>
        <el-empty v-if="!detail.recentIndicators?.length" description="暂无记录" />
        <el-table v-else :data="detail.recentIndicators" size="small">
          <el-table-column label="测量值" width="180">
            <template #default="{ row }">{{ formatIndicatorValue(row) }}</template>
          </el-table-column>
          <el-table-column prop="measuredAt" label="测量时间" width="180" />
          <el-table-column label="评估" width="110">
            <template #default="{ row }">
              <el-tag v-if="row.assessmentRiskLevel" :type="riskTag(row.assessmentRiskLevel)" size="small">
                {{ row.assessmentRiskLevelLabel }}
              </el-tag>
              <span v-else>-</span>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </template>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getDoctorAlertDetail, handleAlert } from '@/api/doctor'
import { HANDLING_ACTIONS, riskTag, statusTag, formatIndicatorValue } from '@/utils/labels'

const route = useRoute()
const loading = ref(false)
const submitting = ref(false)
const detail = ref(null)
const formRef = ref(null)

const handleForm = reactive({
  actionType: '',
  handlingComment: '',
  followupRequired: false,
  nextFollowupAt: null
})

// 状态机：PENDING → PROCESSING → RESOLVED，处理动作由当前状态约束
const availableActions = computed(() => {
  const status = detail.value?.alert?.status
  if (status === 'PENDING') return HANDLING_ACTIONS.filter((a) => a.value === 'START')
  if (status === 'PROCESSING') return HANDLING_ACTIONS.filter((a) => a.value !== 'START')
  return []
})

const residentText = computed(() => {
  const r = detail.value?.resident
  if (!r) return '-'
  return `${r.realName || r.residentNo}（${r.residentNo}）`
})

const handleRules = computed(() => ({
  actionType: [{ required: true, message: '请选择处理动作', trigger: 'change' }],
  handlingComment: [{ required: true, message: '请填写处理意见', trigger: 'blur' }]
}))

const disablePast = (date) => date.getTime() < Date.now() - 24 * 60 * 60 * 1000

const resetHandleForm = () => {
  handleForm.handlingComment = ''
  handleForm.followupRequired = false
  handleForm.nextFollowupAt = null
  handleForm.actionType = availableActions.value[0]?.value || ''
  formRef.value?.clearValidate()
}

const load = async () => {
  loading.value = true
  try {
    const res = await getDoctorAlertDetail(route.params.id)
    detail.value = res.data
    resetHandleForm()
  } finally {
    loading.value = false
  }
}

const submitHandle = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  if (handleForm.followupRequired && !handleForm.nextFollowupAt) {
    ElMessage.warning('请选择建议随访时间')
    return
  }

  submitting.value = true
  try {
    const payload = {
      actionType: handleForm.actionType,
      handlingComment: handleForm.handlingComment,
      followupRequired: handleForm.followupRequired,
      nextFollowupAt: handleForm.followupRequired ? handleForm.nextFollowupAt : undefined
    }
    const res = await handleAlert(route.params.id, payload)
    detail.value = res.data
    resetHandleForm()
    ElMessage.success('处理已提交')
  } catch (error) {
    // 错误信息已由请求拦截器统一提示
  } finally {
    submitting.value = false
  }
}

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

.handle-form {
  max-width: 720px;
}

.muted {
  color: #909399;
  font-size: 13px;
}

.block-hint {
  margin-left: 12px;
}

.handling-item p {
  margin: 6px 0 0;
  color: #606266;
}

.handling-head {
  display: flex;
  align-items: center;
  gap: 8px;
}
</style>
