import request from '@/utils/request'

// 居民端健康数据接口

export const getMyProfile = () => request({ url: '/health/profile', method: 'get' })

export const createMyProfile = (data) => request({ url: '/health/profile', method: 'post', data })

export const updateMyProfile = (data) => request({ url: '/health/profile', method: 'put', data })

export const getCommunities = () => request({ url: '/health/communities', method: 'get' })

export const createIndicator = (data) => request({ url: '/health/indicators', method: 'post', data })

export const getMyIndicators = (params) => request({ url: '/health/indicators', method: 'get', params })

export const getMyIndicatorDetail = (id) => request({ url: `/health/indicators/${id}`, method: 'get' })

export const getMyAssessments = (params) => request({ url: '/health/assessments', method: 'get', params })

export const getMyAlerts = (params) => request({ url: '/health/alerts', method: 'get', params })

export const getMyAlertDetail = (id) => request({ url: `/health/alerts/${id}`, method: 'get' })

export const getMyChronicDiseases = () => request({ url: '/health/chronic-diseases', method: 'get' })

// 我的随访（仅本人、且医生允许公开的内容）
export const getMyFollowupPlans = () => request({ url: '/health/followup-plans', method: 'get' })

export const getMyFollowupRecords = (params) => request({ url: '/health/followup-records', method: 'get', params })

export const getMyFollowupRecordDetail = (id) =>
  request({ url: `/health/followup-records/${id}`, method: 'get' })

// 我的健康干预（仅本人、且医生允许公开的内容）
export const getMyInterventionPlans = () => request({ url: '/health/intervention-plans', method: 'get' })

export const getMyInterventionPlanDetail = (id) =>
  request({ url: `/health/intervention-plans/${id}`, method: 'get' })

export const getMyInterventionPlanRecords = (id, params) =>
  request({ url: `/health/intervention-plans/${id}/records`, method: 'get', params })

export const submitInterventionFeedback = (id, data) =>
  request({ url: `/health/intervention-plans/${id}/feedback`, method: 'post', data })

// ============ 第六阶段：在线咨询 ============
export const createConsultation = (data) => request({ url: '/resident/consultations', method: 'post', data })

export const getMyConsultations = (params) => request({ url: '/resident/consultations', method: 'get', params })

export const getMyConsultationDetail = (id) => request({ url: `/resident/consultations/${id}`, method: 'get' })

export const getMyConsultationMessages = (id) =>
  request({ url: `/resident/consultations/${id}/messages`, method: 'get' })

export const sendConsultationMessage = (id, data) =>
  request({ url: `/resident/consultations/${id}/messages`, method: 'post', data })

export const cancelConsultation = (id) =>
  request({ url: `/resident/consultations/${id}/cancel`, method: 'post' })

// ============ 第六阶段：健康知识（只读已发布） ============
export const getHealthArticles = (params) => request({ url: '/resident/health-articles', method: 'get', params })

export const getHealthArticleDetail = (id) =>
  request({ url: `/resident/health-articles/${id}`, method: 'get' })

// ============ 第六阶段：社区公告（只读有效） ============
export const getNotices = (params) => request({ url: '/resident/notices', method: 'get', params })

export const getNoticeDetail = (id) => request({ url: `/resident/notices/${id}`, method: 'get' })
