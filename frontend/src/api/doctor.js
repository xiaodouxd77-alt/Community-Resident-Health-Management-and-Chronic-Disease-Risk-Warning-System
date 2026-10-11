import request from '@/utils/request'

// 医生端接口

// ---------------- 预警 ----------------
export const getDoctorAlerts = (params) => request({ url: '/doctor/alerts', method: 'get', params })

export const getDoctorAlertDetail = (id) => request({ url: `/doctor/alerts/${id}`, method: 'get' })

export const getAlertHandlingRecords = (id) =>
  request({ url: `/doctor/alerts/${id}/handling-records`, method: 'get' })

export const handleAlert = (id, data) =>
  request({ url: `/doctor/alerts/${id}/handling`, method: 'post', data })

// ---------------- 居民档案 ----------------
export const getMyResidents = () => request({ url: '/doctor/residents', method: 'get' })

export const getResidentsPage = (params) => request({ url: '/doctor/residents/page', method: 'get', params })

export const getCommunities = () => request({ url: '/doctor/communities', method: 'get' })

export const getResidentDetail = (id) => request({ url: `/doctor/residents/${id}`, method: 'get' })

export const getResidentIndicators = (id, params) =>
  request({ url: `/doctor/residents/${id}/indicators`, method: 'get', params })

export const getResidentAssessments = (id, params) =>
  request({ url: `/doctor/residents/${id}/assessments`, method: 'get', params })

export const getResidentAlerts = (id, params) =>
  request({ url: `/doctor/residents/${id}/alerts`, method: 'get', params })

export const getResidentChronic = (id) =>
  request({ url: `/doctor/residents/${id}/chronic-diseases`, method: 'get' })

export const getResidentGroups = (id) =>
  request({ url: `/doctor/residents/${id}/groups`, method: 'get' })

// ---------------- 慢病患者管理 ----------------
export const getChronicList = (params) => request({ url: '/doctor/chronic-diseases', method: 'get', params })

export const createChronic = (data) => request({ url: '/doctor/chronic-diseases', method: 'post', data })

export const getChronicDetail = (id) => request({ url: `/doctor/chronic-diseases/${id}`, method: 'get' })

export const updateChronic = (id, data) =>
  request({ url: `/doctor/chronic-diseases/${id}`, method: 'put', data })

export const reviewChronic = (id, data) =>
  request({ url: `/doctor/chronic-diseases/${id}/review`, method: 'post', data })

export const changeChronicStatus = (id, data) =>
  request({ url: `/doctor/chronic-diseases/${id}/status`, method: 'post', data })

export const getChronicHistory = (id) =>
  request({ url: `/doctor/chronic-diseases/${id}/history`, method: 'get' })

// ---------------- 居民分组管理 ----------------
export const getGroupList = (params) => request({ url: '/doctor/resident-groups', method: 'get', params })

export const createGroup = (data) => request({ url: '/doctor/resident-groups', method: 'post', data })

export const getGroupDetail = (id) => request({ url: `/doctor/resident-groups/${id}`, method: 'get' })

export const updateGroup = (id, data) =>
  request({ url: `/doctor/resident-groups/${id}`, method: 'put', data })

export const getGroupMembers = (id, params) =>
  request({ url: `/doctor/resident-groups/${id}/members`, method: 'get', params })

export const addGroupMembers = (id, data) =>
  request({ url: `/doctor/resident-groups/${id}/members`, method: 'post', data })

export const removeGroupMember = (id, residentId) =>
  request({ url: `/doctor/resident-groups/${id}/members/${residentId}`, method: 'delete' })

// ---------------- 随访计划 ----------------
export const getFollowupPlans = (params) => request({ url: '/doctor/followup-plans', method: 'get', params })

export const createFollowupPlan = (data) => request({ url: '/doctor/followup-plans', method: 'post', data })

export const getFollowupPlanDetail = (id) => request({ url: `/doctor/followup-plans/${id}`, method: 'get' })

export const updateFollowupPlan = (id, data) =>
  request({ url: `/doctor/followup-plans/${id}`, method: 'put', data })

export const cancelFollowupPlan = (id) =>
  request({ url: `/doctor/followup-plans/${id}/cancel`, method: 'post' })

export const completeFollowupPlan = (id) =>
  request({ url: `/doctor/followup-plans/${id}/complete`, method: 'post' })

export const getTodoFollowupPlans = (params) =>
  request({ url: '/doctor/followup-plans/todo', method: 'get', params })

export const getUpcomingFollowupPlans = (params) =>
  request({ url: '/doctor/followup-plans/upcoming', method: 'get', params })

export const getFollowupStats = () => request({ url: '/doctor/followup-plans/stats', method: 'get' })

export const getResidentFollowupPlans = (residentId) =>
  request({ url: `/doctor/residents/${residentId}/followup-plans`, method: 'get' })

export const getResidentFollowupRecords = (residentId, params) =>
  request({ url: `/doctor/residents/${residentId}/followup-records`, method: 'get', params })

// ---------------- 随访记录 ----------------
export const createFollowupRecord = (planId, data) =>
  request({ url: `/doctor/followup-plans/${planId}/records`, method: 'post', data })

export const getFollowupPlanRecords = (planId, params) =>
  request({ url: `/doctor/followup-plans/${planId}/records`, method: 'get', params })

export const getFollowupRecordDetail = (id) =>
  request({ url: `/doctor/followup-records/${id}`, method: 'get' })

// ---------------- 健康干预方案 ----------------
export const getInterventionPlans = (params) =>
  request({ url: '/doctor/intervention-plans', method: 'get', params })

export const createInterventionPlan = (data) =>
  request({ url: '/doctor/intervention-plans', method: 'post', data })

export const getInterventionPlanDetail = (id) =>
  request({ url: `/doctor/intervention-plans/${id}`, method: 'get' })

export const updateInterventionPlan = (id, data) =>
  request({ url: `/doctor/intervention-plans/${id}`, method: 'put', data })

export const activateInterventionPlan = (id) =>
  request({ url: `/doctor/intervention-plans/${id}/activate`, method: 'post' })

export const suspendInterventionPlan = (id) =>
  request({ url: `/doctor/intervention-plans/${id}/suspend`, method: 'post' })

export const completeInterventionPlan = (id) =>
  request({ url: `/doctor/intervention-plans/${id}/complete`, method: 'post' })

export const cancelInterventionPlan = (id) =>
  request({ url: `/doctor/intervention-plans/${id}/cancel`, method: 'post' })

export const getInterventionPlanEffect = (id) =>
  request({ url: `/doctor/intervention-plans/${id}/effect`, method: 'get' })

export const getTodoInterventionPlans = () =>
  request({ url: '/doctor/intervention-plans/todo', method: 'get' })

export const getResidentInterventionPlans = (residentId) =>
  request({ url: `/doctor/residents/${residentId}/intervention-plans`, method: 'get' })

export const getResidentInterventionRecords = (residentId, params) =>
  request({ url: `/doctor/residents/${residentId}/intervention-records`, method: 'get', params })

// ---------------- 干预执行记录 ----------------
export const createInterventionRecord = (planId, data) =>
  request({ url: `/doctor/intervention-plans/${planId}/records`, method: 'post', data })

export const getInterventionPlanRecords = (planId, params) =>
  request({ url: `/doctor/intervention-plans/${planId}/records`, method: 'get', params })

export const getInterventionRecordDetail = (id) =>
  request({ url: `/doctor/intervention-records/${id}`, method: 'get' })

// ---------------- 高风险居民筛查 ----------------
export const getHighRiskResidents = (params) =>
  request({ url: '/doctor/high-risk-residents', method: 'get', params })

export const getHighRiskResidentDetail = (residentId) =>
  request({ url: `/doctor/high-risk-residents/${residentId}`, method: 'get' })

// ---------------- 慢病与预警统计 ----------------
export const getHealthOverview = () =>
  request({ url: '/doctor/health-statistics/overview', method: 'get' })

export const getChronicStatistics = () =>
  request({ url: '/doctor/health-statistics/chronic-diseases', method: 'get' })

export const getRiskDistribution = () =>
  request({ url: '/doctor/health-statistics/risk-distribution', method: 'get' })

export const getAlertStatistics = () =>
  request({ url: '/doctor/health-statistics/alerts', method: 'get' })

export const getAlertTrend = (params) =>
  request({ url: '/doctor/health-statistics/trend', method: 'get', params })

// ---------------- 在线咨询 ----------------
export const getDoctorConsultations = (params) =>
  request({ url: '/doctor/consultations', method: 'get', params })

export const getDoctorConsultationDetail = (id) =>
  request({ url: `/doctor/consultations/${id}`, method: 'get' })

export const getDoctorConsultationMessages = (id) =>
  request({ url: `/doctor/consultations/${id}/messages`, method: 'get' })

export const acceptConsultation = (id) =>
  request({ url: `/doctor/consultations/${id}/accept`, method: 'post' })

export const replyConsultation = (id, data) =>
  request({ url: `/doctor/consultations/${id}/messages`, method: 'post', data })

export const resolveConsultation = (id) =>
  request({ url: `/doctor/consultations/${id}/resolve`, method: 'post' })

export const closeConsultation = (id) =>
  request({ url: `/doctor/consultations/${id}/close`, method: 'post' })
