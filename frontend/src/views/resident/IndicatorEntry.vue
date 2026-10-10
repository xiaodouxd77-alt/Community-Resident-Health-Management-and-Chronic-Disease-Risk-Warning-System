<template>
  <div class="entry-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>录入健康指标</span>
          <span class="muted">提交后将由系统进行风险评估，异常指标会自动生成预警</span>
        </div>
      </template>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="110px"
        class="entry-form"
      >
        <el-form-item label="指标类型" prop="indicatorType">
          <el-radio-group v-model="form.indicatorType" @change="handleTypeChange">
            <el-radio-button
              v-for="type in INDICATOR_TYPES"
              :key="type.value"
              :value="type.value"
            >
              {{ type.label }}
            </el-radio-button>
          </el-radio-group>
        </el-form-item>

        <!-- 血压：收缩压 / 舒张压 -->
        <template v-if="form.indicatorType === 'BLOOD_PRESSURE'">
          <el-form-item label="收缩压" prop="numericValue">
            <el-input-number v-model="form.numericValue" :controls="false" :precision="0" placeholder="如 120" class="num-input" />
            <span class="unit">{{ currentUnit }}</span>
          </el-form-item>
          <el-form-item label="舒张压" prop="secondaryValue">
            <el-input-number v-model="form.secondaryValue" :controls="false" :precision="0" placeholder="如 80" class="num-input" />
            <span class="unit">{{ currentUnit }}</span>
          </el-form-item>
        </template>

        <!-- 其它指标：单数值 -->
        <template v-else>
          <el-form-item label="测量数值" prop="numericValue">
            <el-input-number
              v-model="form.numericValue"
              :controls="false"
              :precision="form.indicatorType === 'BLOOD_GLUCOSE' ? 1 : (form.indicatorType === 'WEIGHT' ? 1 : 0)"
              placeholder="请输入测量值"
              class="num-input"
            />
            <span class="unit">{{ currentUnit }}</span>
          </el-form-item>
        </template>

        <!-- 血糖测量场景 -->
        <el-form-item v-if="form.indicatorType === 'BLOOD_GLUCOSE'" label="测量场景" prop="measurementContext">
          <el-select v-model="form.measurementContext" placeholder="请选择" style="width: 220px">
            <el-option
              v-for="ctx in MEASUREMENT_CONTEXTS"
              :key="ctx.value"
              :label="ctx.label"
              :value="ctx.value"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="测量时间" prop="measuredAt">
          <el-date-picker
            v-model="form.measuredAt"
            type="datetime"
            value-format="YYYY-MM-DD HH:mm:ss"
            placeholder="选择测量时间"
            :disabled-date="disableFuture"
            style="width: 260px"
          />
        </el-form-item>

        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="2" maxlength="255" show-word-limit placeholder="选填" />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="submitting" :disabled="submitting" @click="handleSubmit">
            提交并评估
          </el-button>
          <el-button @click="resetForm">重置</el-button>
          <el-button link type="primary" @click="$router.push('/resident/indicators')">查看历史记录</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 提交结果 -->
    <el-dialog v-model="resultVisible" :title="resultTitle" width="560px">
      <el-alert
        v-if="result"
        :type="result.alertCreated ? 'warning' : 'success'"
        :closable="false"
        show-icon
        :title="result.message"
      />
      <div v-if="result" class="result-body">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="本次测量">
            {{ formatIndicatorValue(result.record) }}
          </el-descriptions-item>
          <el-descriptions-item label="评估等级">
            <el-tag :type="riskTag(result.assessment?.riskLevel)" effect="dark">
              {{ result.assessment?.riskLevelLabel || '-' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="评估说明">
            {{ result.assessment?.assessmentSummary || '-' }}
          </el-descriptions-item>
        </el-descriptions>

        <div v-if="result.alert" class="alert-box">
          <div class="alert-title">
            <el-icon><Warning /></el-icon>
            <span>{{ result.alert.alertTitle }}</span>
          </div>
          <p class="alert-reason">{{ result.alert.alertReason }}</p>
          <el-button type="primary" size="small" @click="goAlertDetail(result.alert.id)">
            查看预警详情
          </el-button>
        </div>
        <p v-else class="muted note">
          未触发预警不代表完全健康，请继续保持定期测量；如有不适请咨询医生。
        </p>
      </div>
      <template #footer>
        <el-button @click="resultVisible = false">继续录入</el-button>
        <el-button type="primary" @click="$router.push('/resident/indicators')">查看历史</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Warning } from '@element-plus/icons-vue'
import { createIndicator } from '@/api/health'
import {
  INDICATOR_TYPES,
  MEASUREMENT_CONTEXTS,
  indicatorUnit,
  formatIndicatorValue,
  riskTag,
  newRequestId
} from '@/utils/labels'

const router = useRouter()
const formRef = ref(null)
const submitting = ref(false)
const resultVisible = ref(false)
const result = ref(null)
const requestId = ref(newRequestId())

const emptyForm = () => ({
  indicatorType: 'BLOOD_PRESSURE',
  numericValue: undefined,
  secondaryValue: undefined,
  measurementContext: undefined,
  measuredAt: nowString(),
  remark: ''
})

const form = reactive(emptyForm())

function nowString() {
  const d = new Date()
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

const currentUnit = computed(() => indicatorUnit(form.indicatorType))
const resultTitle = computed(() => (result.value?.alertCreated ? '保存成功，已生成预警' : '保存成功'))

const disableFuture = (date) => date.getTime() > Date.now()

// 与后端一致的可接受区间校验（明显不合理的数据才拒绝）
const rangeValidator = (min, max, label) => (rule, value, callback) => {
  if (value === undefined || value === null || value === '') {
    return callback(new Error(`请输入${label}`))
  }
  if (value < min || value > max) {
    return callback(new Error(`${label}超出可接受范围（${min}~${max}），请检查后重新输入`))
  }
  return callback()
}

const rules = computed(() => {
  const base = {
    indicatorType: [{ required: true, message: '请选择指标类型', trigger: 'change' }],
    measuredAt: [{ required: true, message: '请选择测量时间', trigger: 'change' }]
  }
  switch (form.indicatorType) {
    case 'BLOOD_PRESSURE':
      return {
        ...base,
        numericValue: [{ validator: rangeValidator(40, 300, '收缩压(mmHg)'), trigger: 'blur' }],
        secondaryValue: [
          { required: true, message: '请输入舒张压', trigger: 'blur' },
          { validator: rangeValidator(20, 200, '舒张压(mmHg)'), trigger: 'blur' },
          {
            validator: (rule, value, callback) => {
              if (value && form.numericValue && Number(form.numericValue) <= Number(value)) {
                return callback(new Error('收缩压应大于舒张压'))
              }
              return callback()
            },
            trigger: 'blur'
          }
        ]
      }
    case 'BLOOD_GLUCOSE':
      return {
        ...base,
        numericValue: [{ validator: rangeValidator(0.5, 50, '血糖(mmol/L)'), trigger: 'blur' }],
        measurementContext: [{ required: true, message: '请选择测量场景', trigger: 'change' }]
      }
    case 'WEIGHT':
      return { ...base, numericValue: [{ validator: rangeValidator(1, 500, '体重(kg)'), trigger: 'blur' }] }
    case 'HEART_RATE':
      return { ...base, numericValue: [{ validator: rangeValidator(20, 300, '心率(bpm)'), trigger: 'blur' }] }
    default:
      return base
  }
})

const handleTypeChange = () => {
  form.numericValue = undefined
  form.secondaryValue = undefined
  form.measurementContext = undefined
  formRef.value?.clearValidate()
}

const resetForm = () => {
  Object.assign(form, emptyForm())
  requestId.value = newRequestId()
  formRef.value?.clearValidate()
}

const handleSubmit = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    const payload = {
      indicatorType: form.indicatorType,
      numericValue: form.numericValue,
      unit: currentUnit.value,
      measuredAt: form.measuredAt,
      remark: form.remark || undefined,
      requestId: requestId.value
    }
    if (form.indicatorType === 'BLOOD_PRESSURE') {
      payload.secondaryValue = form.secondaryValue
    }
    if (form.indicatorType === 'BLOOD_GLUCOSE') {
      payload.measurementContext = form.measurementContext
    }
    const res = await createIndicator(payload)
    result.value = res.data
    resultVisible.value = true
    // 提交成功后更换幂等键，避免下一次录入被当作重复请求
    requestId.value = newRequestId()
  } catch (error) {
    // 错误信息已由请求拦截器统一提示
    ElMessage.warning('本次提交未完成，请根据提示修改后重试')
  } finally {
    submitting.value = false
  }
}

const goAlertDetail = (id) => {
  resultVisible.value = false
  router.push(`/resident/alerts/${id}`)
}
</script>

<style scoped>
.entry-container {
  max-width: 900px;
  margin: 0 auto;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
}

.muted {
  color: #909399;
  font-size: 13px;
}

.entry-form {
  max-width: 640px;
}

.num-input {
  width: 220px;
}

.unit {
  margin-left: 10px;
  color: #909399;
}

.result-body {
  margin-top: 16px;
}

.alert-box {
  margin-top: 16px;
  padding: 14px;
  border-radius: 8px;
  background: #fdf6ec;
  border: 1px solid #f5dab1;
}

.alert-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-weight: 600;
  color: #b88230;
  margin-bottom: 8px;
}

.alert-reason {
  color: #a06c1a;
  font-size: 13px;
  margin-bottom: 10px;
}

.note {
  margin-top: 14px;
}
</style>
