import request from '@/utils/request'

// 管理端接口

// ---------------- 全局健康统计 ----------------
export const getAdminHealthOverview = () =>
  request({ url: '/admin/health-statistics/overview', method: 'get' })

export const getAdminChronicStatistics = () =>
  request({ url: '/admin/health-statistics/chronic-diseases', method: 'get' })

export const getAdminRiskDistribution = () =>
  request({ url: '/admin/health-statistics/risk-distribution', method: 'get' })

export const getAdminAlertStatistics = () =>
  request({ url: '/admin/health-statistics/alerts', method: 'get' })

export const getAdminAlertTrend = (params) =>
  request({ url: '/admin/health-statistics/trend', method: 'get', params })

// ---------------- 健康知识管理 ----------------
export const getAdminArticles = (params) =>
  request({ url: '/admin/health-articles', method: 'get', params })

export const getAdminArticleDetail = (id) =>
  request({ url: `/admin/health-articles/${id}`, method: 'get' })

export const createArticle = (data) =>
  request({ url: '/admin/health-articles', method: 'post', data })

export const updateArticle = (id, data) =>
  request({ url: `/admin/health-articles/${id}`, method: 'put', data })

export const publishArticle = (id) =>
  request({ url: `/admin/health-articles/${id}/publish`, method: 'post' })

export const offlineArticle = (id) =>
  request({ url: `/admin/health-articles/${id}/offline`, method: 'post' })

// ---------------- 公告管理 ----------------
export const getAdminNotices = (params) => request({ url: '/admin/notices', method: 'get', params })

export const getAdminNoticeDetail = (id) => request({ url: `/admin/notices/${id}`, method: 'get' })

export const createNotice = (data) => request({ url: '/admin/notices', method: 'post', data })

export const updateNotice = (id, data) => request({ url: `/admin/notices/${id}`, method: 'put', data })

export const publishNotice = (id) => request({ url: `/admin/notices/${id}/publish`, method: 'post' })

export const offlineNotice = (id) => request({ url: `/admin/notices/${id}/offline`, method: 'post' })
