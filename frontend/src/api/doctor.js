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
