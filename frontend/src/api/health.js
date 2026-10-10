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
