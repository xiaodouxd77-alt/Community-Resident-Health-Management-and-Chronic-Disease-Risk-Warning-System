<template>
  <el-container class="layout">
    <el-aside class="sidebar" :width="collapsed ? '64px' : '230px'">
      <div class="brand">
        <el-icon class="brand__icon"><FirstAidKit /></el-icon>
        <span v-show="!collapsed" class="brand__text">社区健康管理平台</span>
      </div>
      <el-scrollbar class="nav-scroll">
        <el-menu
          :default-active="activeMenu"
          :collapse="collapsed"
          :collapse-transition="false"
          class="nav-menu"
          router
        >
          <template v-for="group in menuGroups" :key="group.key">
            <el-sub-menu v-if="group.children.length > 1" :index="group.key">
              <template #title>
                <el-icon><component :is="group.icon" /></el-icon>
                <span>{{ group.title }}</span>
              </template>
              <el-menu-item v-for="item in group.children" :key="item.path" :index="item.path">
                <el-icon><component :is="item.icon" /></el-icon>
                <span>{{ item.title }}</span>
              </el-menu-item>
            </el-sub-menu>
            <el-menu-item v-else :index="group.children[0].path">
              <el-icon><component :is="group.children[0].icon" /></el-icon>
              <span>{{ group.children[0].title }}</span>
            </el-menu-item>
          </template>
        </el-menu>
      </el-scrollbar>
    </el-aside>

    <el-container class="content-wrap">
      <el-header class="topbar">
        <div class="topbar__left">
          <el-icon class="collapse-btn" @click="toggleCollapse">
            <Expand v-if="collapsed" />
            <Fold v-else />
          </el-icon>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item v-for="(crumb, i) in breadcrumb" :key="i">{{ crumb }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="topbar__right">
          <el-tooltip :content="noticeTip" placement="bottom">
            <el-icon class="topbar__icon" @click="goNotice"><Bell /></el-icon>
          </el-tooltip>
          <el-dropdown @command="handleCommand">
            <span class="user-trigger">
              <el-icon><User /></el-icon>
              <span class="user-name">{{ userStore.userInfo?.realName || userStore.userInfo?.username || '用户' }}</span>
              <el-tag size="small" effect="plain" class="role-tag">{{ roleLabel }}</el-tag>
              <el-icon class="el-icon--right"><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed, ref, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import router from '@/router'
import { useUserStore } from '@/stores/user'
import {
  FirstAidKit, House, Odometer, User, UserFilled, EditPen, TrendCharts, DataAnalysis,
  Warning, Notebook, Calendar, ChatDotRound, Reading, Bell, Grid, Aim,
  Fold, Expand, ArrowDown
} from '@element-plus/icons-vue'

const route = useRoute()
const userStore = useUserStore()

const COLLAPSE_KEY = 'layout-collapsed'
const collapsed = ref(localStorage.getItem(COLLAPSE_KEY) === '1')

const toggleCollapse = () => {
  collapsed.value = !collapsed.value
}

watch(collapsed, (val) => {
  localStorage.setItem(COLLAPSE_KEY, val ? '1' : '0')
})

const RESIDENT_GROUPS = [
  {
    key: 'g-workbench',
    title: '健康总览',
    icon: Odometer,
    children: [{ path: '/home', title: '首页概览', icon: House }]
  },
  {
    key: 'g-health',
    title: '我的健康',
    icon: FirstAidKit,
    children: [
      { path: '/resident/profile', title: '健康档案', icon: User },
      { path: '/resident/indicators/new', title: '录入指标', icon: EditPen },
      { path: '/resident/indicators', title: '指标历史', icon: TrendCharts },
      { path: '/resident/assessments', title: '评估结果', icon: DataAnalysis },
      { path: '/resident/alerts', title: '我的预警', icon: Warning }
    ]
  },
  {
    key: 'g-chronic',
    title: '慢病管理',
    icon: Notebook,
    children: [
      { path: '/resident/chronic', title: '慢病档案', icon: Notebook },
      { path: '/resident/followups', title: '我的随访', icon: Calendar },
      { path: '/resident/interventions', title: '健康干预', icon: FirstAidKit }
    ]
  },
  {
    key: 'g-service',
    title: '健康服务',
    icon: ChatDotRound,
    children: [
      { path: '/resident/consultations', title: '在线咨询', icon: ChatDotRound },
      { path: '/resident/knowledge', title: '健康知识', icon: Reading },
      { path: '/resident/notices', title: '通知公告', icon: Bell }
    ]
  }
]

const DOCTOR_GROUPS = [
  {
    key: 'g-workbench',
    title: '工作台',
    icon: Odometer,
    children: [{ path: '/home', title: '首页概览', icon: House }]
  },
  {
    key: 'g-residents',
    title: '居民管理',
    icon: UserFilled,
    children: [
      { path: '/doctor/residents', title: '居民档案', icon: User },
      { path: '/doctor/groups', title: '居民分组', icon: Grid }
    ]
  },
  {
    key: 'g-risk',
    title: '风险预警',
    icon: Warning,
    children: [
      { path: '/doctor/alerts', title: '预警管理', icon: Warning },
      { path: '/doctor/high-risk-residents', title: '高危筛查', icon: Aim }
    ]
  },
  {
    key: 'g-chronic',
    title: '慢病管理',
    icon: Notebook,
    children: [{ path: '/doctor/chronic', title: '慢病患者', icon: Notebook }]
  },
  {
    key: 'g-followup',
    title: '随访干预',
    icon: Calendar,
    children: [
      { path: '/doctor/followups/workbench', title: '随访工作台', icon: Odometer },
      { path: '/doctor/followups', title: '随访计划', icon: Calendar },
      { path: '/doctor/interventions', title: '健康干预', icon: FirstAidKit }
    ]
  },
  {
    key: 'g-consult',
    title: '在线咨询',
    icon: ChatDotRound,
    children: [{ path: '/doctor/consultations', title: '咨询工作台', icon: ChatDotRound }]
  },
  {
    key: 'g-stats',
    title: '数据统计',
    icon: DataAnalysis,
    children: [{ path: '/doctor/statistics', title: '统计分析', icon: DataAnalysis }]
  }
]

const ADMIN_GROUPS = [
  {
    key: 'g-workbench',
    title: '工作台',
    icon: Odometer,
    children: [{ path: '/home', title: '首页概览', icon: House }]
  },
  {
    key: 'g-stats',
    title: '数据统计',
    icon: DataAnalysis,
    children: [{ path: '/admin/statistics', title: '统计分析', icon: DataAnalysis }]
  },
  {
    key: 'g-users',
    title: '用户与权限',
    icon: UserFilled,
    children: [{ path: '/users', title: '用户管理', icon: User }]
  },
  {
    key: 'g-knowledge',
    title: '健康知识',
    icon: Reading,
    children: [{ path: '/admin/articles', title: '知识管理', icon: Reading }]
  },
  {
    key: 'g-notice',
    title: '通知公告',
    icon: Bell,
    children: [{ path: '/admin/notices', title: '公告管理', icon: Bell }]
  }
]

const menuGroups = computed(() => {
  if (userStore.isDoctor) return DOCTOR_GROUPS
  if (userStore.isAdmin) return ADMIN_GROUPS
  return RESIDENT_GROUPS
})

// path -> { groupTitle, itemTitle } 便于面包屑与高亮查找
const menuIndex = computed(() => {
  const map = {}
  menuGroups.value.forEach((group) => {
    group.children.forEach((item) => {
      map[item.path] = { groupTitle: group.title, itemTitle: item.title }
    })
  })
  return map
})

const roleLabel = computed(() => {
  const map = { RESIDENT: '居民', DOCTOR: '社区医生', ADMIN: '管理员' }
  return map[userStore.role] || userStore.role || ''
})

const activeMenu = computed(() => {
  const path = route.path
  if (path.startsWith('/resident/alerts/')) return '/resident/alerts'
  if (path === '/resident/indicators/new') return '/resident/indicators/new'
  if (path.startsWith('/resident/indicators/')) return '/resident/indicators'
  if (path === '/resident/indicators') return '/resident/indicators'
  if (path.startsWith('/doctor/alerts/')) return '/doctor/alerts'
  if (path.startsWith('/doctor/residents/')) return '/doctor/residents'
  if (path.startsWith('/doctor/followups/plan/')) return '/doctor/followups'
  if (path === '/doctor/followups/workbench') return '/doctor/followups/workbench'
  if (path.startsWith('/doctor/interventions/')) return '/doctor/interventions'
  return path
})

const breadcrumb = computed(() => {
  const info = menuIndex.value[activeMenu.value]
  if (info) return [info.groupTitle, info.itemTitle]
  return ['首页概览']
})

const noticeTip = computed(() => {
  if (userStore.isDoctor) return '预警提醒'
  if (userStore.isAdmin) return '公告管理'
  return '通知公告'
})

const goNotice = () => {
  if (userStore.isDoctor) return router.push('/doctor/alerts')
  if (userStore.isAdmin) return router.push('/admin/notices')
  return router.push('/resident/notices')
}

onMounted(() => {
  if (!userStore.userInfo) {
    userStore.getUserInfo()
  }
})

const handleCommand = (command) => {
  if (command === 'logout') {
    userStore.logout()
  }
}
</script>

<style scoped>
.layout {
  height: 100vh;
  background: var(--app-bg);
}

/* 侧边栏 */
.sidebar {
  background: var(--surface);
  border-right: 1px solid var(--border-color);
  display: flex;
  flex-direction: column;
  transition: width 0.22s ease;
  overflow: hidden;
}

.brand {
  height: var(--header-height);
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 18px;
  border-bottom: 1px solid var(--border-color-light);
  color: var(--brand-primary);
  white-space: nowrap;
}

.brand__icon {
  font-size: 22px;
  flex: none;
}

.brand__text {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
}

.nav-scroll {
  flex: 1;
  padding: 8px 0;
}

.nav-menu {
  border-right: none;
}

.nav-menu:not(.el-menu--collapse) {
  width: 100%;
}

.content-wrap {
  min-width: 0;
}

/* 顶栏 */
.topbar {
  height: var(--header-height);
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: var(--surface);
  border-bottom: 1px solid var(--border-color);
  padding: 0 20px;
}

.topbar__left {
  display: flex;
  align-items: center;
  gap: 14px;
  min-width: 0;
}

.collapse-btn {
  font-size: 18px;
  color: var(--text-secondary);
  cursor: pointer;
  padding: 4px;
  border-radius: var(--radius-sm);
}

.collapse-btn:hover {
  background: var(--surface-muted);
  color: var(--brand-primary);
}

.topbar__right {
  display: flex;
  align-items: center;
  gap: 18px;
}

.topbar__icon {
  font-size: 18px;
  color: var(--text-secondary);
  cursor: pointer;
}

.topbar__icon:hover {
  color: var(--brand-primary);
}

.user-trigger {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  color: var(--text-regular);
  outline: none;
}

.user-name {
  font-size: 14px;
  color: var(--text-primary);
}

.role-tag {
  color: var(--brand-primary);
  border-color: var(--el-color-primary-light-7);
  background: var(--brand-primary-soft);
}

/* 主工作区 */
.main {
  padding: 20px 24px;
  overflow-y: auto;
  background: var(--app-bg);
}
</style>
