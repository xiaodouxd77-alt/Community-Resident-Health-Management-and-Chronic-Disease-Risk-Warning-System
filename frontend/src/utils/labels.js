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

// ============ 第三阶段：随访计划 / 随访记录 ============

export const FOLLOWUP_METHODS = [
  { value: 'PHONE', label: '电话' },
  { value: 'OFFLINE', label: '线下' },
  { value: 'ONLINE', label: '线上' },
  { value: 'OTHER', label: '其他' }
]

export const PLAN_STATUSES = [
  { value: 'PLANNED', label: '待执行', tag: 'warning' },
  { value: 'IN_PROGRESS', label: '进行中', tag: 'primary' },
  { value: 'COMPLETED', label: '已完成', tag: 'success' },
  { value: 'CANCELLED', label: '已取消', tag: 'info' }
]

export const followupMethodLabel = (value) => find(FOLLOWUP_METHODS, value)?.label || value
export const planStatusLabel = (value) => find(PLAN_STATUSES, value)?.label || value
export const planStatusTag = (value) => find(PLAN_STATUSES, value)?.tag || 'info'

// ============ 第四阶段：健康干预方案 / 执行记录 ============

export const INTERVENTION_TYPES = [
  { value: 'LIFESTYLE', label: '生活方式指导' },
  { value: 'DIET', label: '饮食指导' },
  { value: 'EXERCISE', label: '运动指导' },
  { value: 'MONITORING', label: '健康指标监测' },
  { value: 'FOLLOWUP', label: '随访管理' },
  { value: 'OTHER', label: '其他' }
]

export const INTERVENTION_STATUSES = [
  { value: 'DRAFT', label: '草稿', tag: 'info' },
  { value: 'ACTIVE', label: '执行中', tag: 'primary' },
  { value: 'COMPLETED', label: '已完成', tag: 'success' },
  { value: 'SUSPENDED', label: '已暂停', tag: 'warning' },
  { value: 'CANCELLED', label: '已取消', tag: 'info' }
]

export const EXECUTION_STATUSES = [
  { value: 'COMPLETED', label: '已执行', tag: 'success' },
  { value: 'PARTIAL', label: '部分执行', tag: 'warning' },
  { value: 'NOT_COMPLETED', label: '未执行', tag: 'danger' },
  { value: 'NEEDS_ADJUSTMENT', label: '需要调整', tag: 'danger' }
]

export const OPERATOR_ROLES = [
  { value: 'DOCTOR', label: '医生记录', tag: 'primary' },
  { value: 'RESIDENT', label: '居民反馈', tag: 'success' }
]

export const interventionTypeLabel = (value) => find(INTERVENTION_TYPES, value)?.label || value
export const interventionStatusLabel = (value) => find(INTERVENTION_STATUSES, value)?.label || value
export const interventionStatusTag = (value) =>
  find(INTERVENTION_STATUSES, value)?.tag || 'info'
export const executionStatusLabel = (value) =>
  value ? find(EXECUTION_STATUSES, value)?.label || value : '-'
export const executionStatusTag = (value) => find(EXECUTION_STATUSES, value)?.tag || 'info'
export const operatorRoleLabel = (value) => find(OPERATOR_ROLES, value)?.label || value
export const operatorRoleTag = (value) => find(OPERATOR_ROLES, value)?.tag || 'info'

// ============ 第五阶段：高风险筛查 / 统计 ============

/** 是否筛选（用于不确定条件的下拉） */
export const BOOL_OPTIONS = [
  { value: true, label: '是' },
  { value: false, label: '否' }
]

/** 图表配色：风险等级由低到高 */
export const RISK_COLORS = {
  LOW: '#67c23a',
  MEDIUM: '#e6a23c',
  HIGH: '#f56c6c',
  CRITICAL: '#c45656'
}

/** 图表配色：预警状态 */
export const ALERT_STATUS_COLORS = {
  PENDING: '#f56c6c',
  PROCESSING: '#e6a23c',
  RESOLVED: '#67c23a'
}

/** 图表配色：慢病类型 */
export const DISEASE_COLORS = ['#5470c6', '#91cc75', '#fac858', '#ee6666', '#73c0de']

/** 把 0~1 的比例格式化为百分比字符串 */
export const formatPercent = (value) => {
  const num = Number(value)
  if (Number.isNaN(num)) return '-'
  return `${(num * 100).toFixed(1)}%`
}

// ============ 第六阶段：在线咨询 / 健康知识 / 公告 ============

export const CONSULTATION_CATEGORIES = [
  { value: 'HEALTH_INDICATOR', label: '健康指标咨询' },
  { value: 'CHRONIC_MANAGEMENT', label: '慢病管理' },
  { value: 'LIFESTYLE', label: '生活方式指导' },
  { value: 'OTHER', label: '其他' }
]

export const CONSULTATION_STATUSES = [
  { value: 'PENDING', label: '待受理', tag: 'danger' },
  { value: 'PROCESSING', label: '处理中', tag: 'warning' },
  { value: 'RESOLVED', label: '已解决', tag: 'success' },
  { value: 'CLOSED', label: '已关闭', tag: 'info' }
]

export const PUBLISH_STATUSES = [
  { value: 'DRAFT', label: '草稿', tag: 'info' },
  { value: 'PUBLISHED', label: '已发布', tag: 'success' },
  { value: 'OFFLINE', label: '已下架', tag: 'warning' }
]

export const ARTICLE_CATEGORIES = [
  { value: 'CHRONIC', label: '慢病科普' },
  { value: 'DIET', label: '饮食营养' },
  { value: 'EXERCISE', label: '运动健康' },
  { value: 'LIFESTYLE', label: '生活方式' },
  { value: 'ELDERLY', label: '老年保健' },
  { value: 'OTHER', label: '其他' }
]

export const consultationCategoryLabel = (value) => find(CONSULTATION_CATEGORIES, value)?.label || value
export const consultationStatusLabel = (value) => find(CONSULTATION_STATUSES, value)?.label || value
export const consultationStatusTag = (value) => find(CONSULTATION_STATUSES, value)?.tag || 'info'
export const publishStatusLabel = (value) => find(PUBLISH_STATUSES, value)?.label || value
export const publishStatusTag = (value) => find(PUBLISH_STATUSES, value)?.tag || 'info'
export const articleCategoryLabel = (value) => find(ARTICLE_CATEGORIES, value)?.label || value
