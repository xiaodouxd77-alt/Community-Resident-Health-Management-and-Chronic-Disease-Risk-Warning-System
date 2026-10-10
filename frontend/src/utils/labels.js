// 前端展示用的字典与格式化工具，与后端枚举保持一致

export const INDICATOR_TYPES = [
  { value: 'BLOOD_PRESSURE', label: '血压', unit: 'mmHg' },
  { value: 'BLOOD_GLUCOSE', label: '血糖', unit: 'mmol/L' },
  { value: 'WEIGHT', label: '体重', unit: 'kg' },
  { value: 'HEART_RATE', label: '心率', unit: 'bpm' }
]

export const MEASUREMENT_CONTEXTS = [
  { value: 'FASTING', label: '空腹' },
  { value: 'POSTPRANDIAL', label: '餐后2小时' },
  { value: 'RANDOM', label: '随机' }
]

export const RISK_LEVELS = [
  { value: 'LOW', label: '低风险', tag: 'info' },
  { value: 'MEDIUM', label: '中风险', tag: 'warning' },
  { value: 'HIGH', label: '高风险', tag: 'danger' },
  { value: 'CRITICAL', label: '严重风险', tag: 'danger' }
]

export const ALERT_STATUSES = [
  { value: 'PENDING', label: '待处理', tag: 'danger' },
  { value: 'PROCESSING', label: '处理中', tag: 'warning' },
  { value: 'RESOLVED', label: '已完成', tag: 'success' }
]

export const HANDLING_ACTIONS = [
  { value: 'START', label: '开始处理' },
  { value: 'PROGRESS', label: '记录进展' },
  { value: 'RESOLVE', label: '完成处理' }
]

const find = (list, value) => list.find((item) => item.value === value)

export const indicatorLabel = (value) => find(INDICATOR_TYPES, value)?.label || value
export const contextLabel = (value) => (value ? find(MEASUREMENT_CONTEXTS, value)?.label || value : '-')
export const riskLabel = (value) => find(RISK_LEVELS, value)?.label || value
export const riskTag = (value) => find(RISK_LEVELS, value)?.tag || 'info'
export const statusLabel = (value) => find(ALERT_STATUSES, value)?.label || value
export const statusTag = (value) => find(ALERT_STATUSES, value)?.tag || 'info'
export const actionLabel = (value) => find(HANDLING_ACTIONS, value)?.label || value

/** 把血压显示成 165/105 mmHg，其余显示成 7.2 mmol/L */
export const formatIndicatorValue = (row) => {
  if (!row) return '-'
  const unit = row.unit || indicatorUnit(row.indicatorType)
  if (row.indicatorType === 'BLOOD_PRESSURE') {
    return `${strip(row.numericValue)}/${strip(row.secondaryValue)} ${unit}`
  }
  return `${strip(row.numericValue)} ${unit}`
}

export const indicatorUnit = (value) => find(INDICATOR_TYPES, value)?.unit || ''

export const strip = (value) => {
  if (value === null || value === undefined || value === '') return '-'
  const num = Number(value)
  return Number.isNaN(num) ? String(value) : String(parseFloat(num))
}

/** 生成一次表单提交的幂等键 */
export const newRequestId = () => {
  if (window.crypto && typeof window.crypto.randomUUID === 'function') {
    return window.crypto.randomUUID()
  }
  return `req-${Date.now()}-${Math.random().toString(16).slice(2, 10)}`
}

// ============ 第二阶段：慢病建档 / 居民分组 ============

export const DISEASE_TYPES = [
  { value: 'HYPERTENSION', label: '高血压' },
  { value: 'DIABETES', label: '糖尿病' },
  { value: 'OTHER', label: '其他慢病' }
]

export const DIAGNOSIS_STATUSES = [
  { value: 'PENDING_REVIEW', label: '待核实', tag: 'warning' },
  { value: 'CONFIRMED', label: '资料已核实', tag: 'success' },
  { value: 'REJECTED', label: '未通过核实', tag: 'info' }
]

export const MANAGEMENT_STATUSES = [
  { value: 'ACTIVE', label: '管理中', tag: 'success' },
  { value: 'SUSPENDED', label: '暂停管理', tag: 'warning' },
  { value: 'CLOSED', label: '终止管理', tag: 'info' }
]

export const GROUP_STATUSES = [
  { value: 'ACTIVE', label: '启用', tag: 'success' },
  { value: 'DISABLED', label: '停用', tag: 'info' }
]

export const diseaseTypeLabel = (value) => find(DISEASE_TYPES, value)?.label || value
export const diagnosisStatusLabel = (value) => find(DIAGNOSIS_STATUSES, value)?.label || value
export const diagnosisStatusTag = (value) => find(DIAGNOSIS_STATUSES, value)?.tag || 'info'
export const managementStatusLabel = (value) => find(MANAGEMENT_STATUSES, value)?.label || value
export const managementStatusTag = (value) => find(MANAGEMENT_STATUSES, value)?.tag || 'info'
export const groupStatusLabel = (value) => find(GROUP_STATUSES, value)?.label || value
export const groupStatusTag = (value) => find(GROUP_STATUSES, value)?.tag || 'info'
