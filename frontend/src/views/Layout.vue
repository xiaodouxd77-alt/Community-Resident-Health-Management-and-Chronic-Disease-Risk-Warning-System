<template>
  <el-container class="layout-container">
    <el-header class="header">
      <div class="logo">
        <el-icon class="logo-icon"><FirstAidKit /></el-icon>
        <span>社区健康管理与慢病预警</span>
      </div>
      <el-menu
        mode="horizontal"
        :default-active="activeMenu"
        class="menu"
        router
      >
        <el-menu-item v-for="item in menus" :key="item.path" :index="item.path">
          {{ item.title }}
        </el-menu-item>
      </el-menu>
      <div class="user-info">
        <el-tag v-if="userStore.role" size="small" class="role-tag" effect="plain">
          {{ roleLabel }}
        </el-tag>
        <el-dropdown @command="handleCommand">
          <span class="el-dropdown-link">
            <el-icon><User /></el-icon>
            {{ userStore.userInfo?.realName || userStore.userInfo?.username }}
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
</template>

<script setup>
import { computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const userStore = useUserStore()

const RESIDENT_MENUS = [
  { path: '/home', title: '首页' },
  { path: '/resident/profile', title: '健康档案' },
  { path: '/resident/indicators/new', title: '录入指标' },
  { path: '/resident/indicators', title: '指标历史' },
  { path: '/resident/assessments', title: '评估结果' },
  { path: '/resident/alerts', title: '我的预警' },
  { path: '/resident/chronic', title: '慢病档案' }
]

const DOCTOR_MENUS = [
  { path: '/home', title: '首页' },
  { path: '/doctor/residents', title: '居民档案' },
  { path: '/doctor/chronic', title: '慢病患者' },
  { path: '/doctor/groups', title: '居民分组' },
  { path: '/doctor/alerts', title: '预警管理' }
]

const ADMIN_MENUS = [
  { path: '/home', title: '首页' },
  { path: '/users', title: '用户管理' }
]

const menus = computed(() => {
  if (userStore.isDoctor) return DOCTOR_MENUS
  if (userStore.isAdmin) return ADMIN_MENUS
  return RESIDENT_MENUS
})

const roleLabel = computed(() => {
  const map = { RESIDENT: '居民', DOCTOR: '社区医生', ADMIN: '管理员' }
  return map[userStore.role] || userStore.role
})

const activeMenu = computed(() => {
  const path = route.path
  // 详情页高亮对应列表菜单
  if (path.startsWith('/resident/alerts')) return '/resident/alerts'
  if (path.startsWith('/resident/indicators/')) return '/resident/indicators'
  if (path.startsWith('/doctor/alerts')) return '/doctor/alerts'
  if (path.startsWith('/doctor/residents/')) return '/doctor/residents'
  return path
})

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
.layout-container {
  height: 100vh;
  background: #f5f7fa;
}

.header {
  display: flex;
  align-items: center;
  background: #fff;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
  padding: 0 24px;
}

.logo {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 17px;
  font-weight: 600;
  color: #1f6fd6;
  margin-right: 40px;
  white-space: nowrap;
}

.logo-icon {
  font-size: 22px;
}

.menu {
  flex: 1;
  border: none;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-left: 20px;
}

.role-tag {
  color: #1f6fd6;
  border-color: #b6d6f5;
  background: #eef6ff;
}

.el-dropdown-link {
  cursor: pointer;
  display: flex;
  align-items: center;
  color: #606266;
}

.main {
  padding: 20px;
  overflow-y: auto;
}
</style>
