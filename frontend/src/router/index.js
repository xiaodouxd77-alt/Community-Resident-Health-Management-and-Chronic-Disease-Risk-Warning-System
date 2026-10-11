import { createRouter, createWebHistory } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'

const RESIDENT = ['RESIDENT']
const DOCTOR = ['DOCTOR']
const ADMIN = ['ADMIN']

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue')
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/Register.vue')
  },
  {
    path: '/',
    name: 'Layout',
    component: () => import('@/views/Layout.vue'),
    redirect: '/home',
    children: [
      {
        path: '/home',
        name: 'Home',
        component: () => import('@/views/Home.vue')
      },
      // 居民端
      {
        path: '/resident/indicators/new',
        name: 'IndicatorEntry',
        component: () => import('@/views/resident/IndicatorEntry.vue'),
        meta: { roles: RESIDENT }
      },
      {
        path: '/resident/indicators',
        name: 'IndicatorHistory',
        component: () => import('@/views/resident/IndicatorHistory.vue'),
        meta: { roles: RESIDENT }
      },
      {
        path: '/resident/assessments',
        name: 'Assessments',
        component: () => import('@/views/resident/AssessmentList.vue'),
        meta: { roles: RESIDENT }
      },
      {
        path: '/resident/alerts',
        name: 'MyAlerts',
        component: () => import('@/views/resident/AlertList.vue'),
        meta: { roles: RESIDENT }
      },
      {
        path: '/resident/alerts/:id',
        name: 'MyAlertDetail',
        component: () => import('@/views/resident/AlertDetail.vue'),
        meta: { roles: RESIDENT }
      },
      {
        path: '/resident/profile',
        name: 'ResidentProfile',
        component: () => import('@/views/resident/HealthProfile.vue'),
        meta: { roles: RESIDENT }
      },
      {
        path: '/resident/chronic',
        name: 'MyChronic',
        component: () => import('@/views/resident/ChronicList.vue'),
        meta: { roles: RESIDENT }
      },
      {
        path: '/resident/followups',
        name: 'MyFollowups',
        component: () => import('@/views/resident/FollowupList.vue'),
        meta: { roles: RESIDENT }
      },
      {
        path: '/resident/interventions',
        name: 'MyInterventions',
        component: () => import('@/views/resident/MyIntervention.vue'),
        meta: { roles: RESIDENT }
      },
      {
        path: '/resident/consultations',
        name: 'MyConsultations',
        component: () => import('@/views/resident/ConsultationList.vue'),
        meta: { roles: RESIDENT }
      },
      {
        path: '/resident/knowledge',
        name: 'HealthKnowledge',
        component: () => import('@/views/resident/HealthKnowledge.vue'),
        meta: { roles: RESIDENT }
      },
      {
        path: '/resident/notices',
        name: 'NoticeBoard',
        component: () => import('@/views/resident/NoticeBoard.vue'),
        meta: { roles: RESIDENT }
      },
      // 医生端
      {
        path: '/doctor/alerts',
        name: 'AlertManage',
        component: () => import('@/views/doctor/AlertManage.vue'),
        meta: { roles: DOCTOR }
      },
      {
        path: '/doctor/alerts/:id',
        name: 'DoctorAlertDetail',
        component: () => import('@/views/doctor/AlertDetail.vue'),
        meta: { roles: DOCTOR }
      },
      {
        path: '/doctor/residents',
        name: 'ResidentManage',
        component: () => import('@/views/doctor/ResidentManage.vue'),
        meta: { roles: DOCTOR }
      },
      {
        path: '/doctor/residents/:id',
        name: 'ResidentDetail',
        component: () => import('@/views/doctor/ResidentDetail.vue'),
        meta: { roles: DOCTOR }
      },
      {
        path: '/doctor/chronic',
        name: 'ChronicManage',
        component: () => import('@/views/doctor/ChronicManage.vue'),
        meta: { roles: DOCTOR }
      },
      {
        path: '/doctor/groups',
        name: 'GroupManage',
        component: () => import('@/views/doctor/GroupManage.vue'),
        meta: { roles: DOCTOR }
      },
      {
        path: '/doctor/followups/workbench',
        name: 'FollowupWorkbench',
        component: () => import('@/views/doctor/FollowupWorkbench.vue'),
        meta: { roles: DOCTOR }
      },
      {
        path: '/doctor/followups',
        name: 'FollowupManage',
        component: () => import('@/views/doctor/FollowupManage.vue'),
        meta: { roles: DOCTOR }
      },
      {
        path: '/doctor/followups/plan/:id',
        name: 'FollowupDetail',
        component: () => import('@/views/doctor/FollowupDetail.vue'),
        meta: { roles: DOCTOR }
      },
      {
        path: '/doctor/interventions',
        name: 'InterventionManage',
        component: () => import('@/views/doctor/InterventionManage.vue'),
        meta: { roles: DOCTOR }
      },
      {
        path: '/doctor/interventions/:id',
        name: 'InterventionDetail',
        component: () => import('@/views/doctor/InterventionDetail.vue'),
        meta: { roles: DOCTOR }
      },
      {
        path: '/doctor/high-risk-residents',
        name: 'HighRiskResident',
        component: () => import('@/views/doctor/HighRiskResident.vue'),
        meta: { roles: DOCTOR }
      },
      {
        path: '/doctor/statistics',
        name: 'DoctorStatistics',
        component: () => import('@/views/doctor/StatisticsDashboard.vue'),
        meta: { roles: DOCTOR }
      },
      {
        path: '/doctor/consultations',
        name: 'ConsultationWorkbench',
        component: () => import('@/views/doctor/ConsultationWorkbench.vue'),
        meta: { roles: DOCTOR }
      },
      // 管理端统计
      {
        path: '/admin/statistics',
        name: 'AdminStatistics',
        component: () => import('@/views/admin/Statistics.vue'),
        meta: { roles: ADMIN }
      },
      {
        path: '/admin/articles',
        name: 'AdminArticles',
        component: () => import('@/views/admin/ArticleManage.vue'),
        meta: { roles: ADMIN }
      },
      {
        path: '/admin/notices',
        name: 'AdminNotices',
        component: () => import('@/views/admin/NoticeManage.vue'),
        meta: { roles: ADMIN }
      },
      // 管理端（沿用原有用户管理）
      {
        path: '/users',
        name: 'Users',
        component: () => import('@/views/Users.vue'),
        meta: { roles: ADMIN }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach(async (to, from, next) => {
  const userStore = useUserStore()

  if (to.path === '/login' || to.path === '/register') {
    return userStore.isLoggedIn ? next('/') : next()
  }

  if (!userStore.isLoggedIn) {
    return next('/login')
  }

  // 刷新页面后 userInfo 可能尚未加载，先加载再判断角色
  if (!userStore.userInfo) {
    await userStore.getUserInfo()
    if (!userStore.isLoggedIn) {
      return next('/login')
    }
  }

  const roles = to.meta?.roles
  if (roles && roles.length && !roles.includes(userStore.role)) {
    ElMessage.error('无权访问该页面')
    return next('/home')
  }

  return next()
})

export default router
