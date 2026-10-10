<template>
  <div class="home-container">
    <ResidentDashboard v-if="userStore.isResident" />
    <DoctorDashboard v-else-if="userStore.isDoctor" />
    <el-card v-else>
      <template #header>
        <div class="card-header"><span>欢迎回来</span></div>
      </template>
      <div class="welcome">
        <h2>{{ userStore.userInfo?.realName || userStore.userInfo?.username }}</h2>
        <p>当前角色：管理员</p>
        <el-button type="primary" @click="$router.push('/users')">进入用户管理</el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { useUserStore } from '@/stores/user'
import ResidentDashboard from '@/views/resident/ResidentDashboard.vue'
import DoctorDashboard from '@/views/doctor/DoctorDashboard.vue'

const userStore = useUserStore()
</script>

<style scoped>
.home-container {
  max-width: 1200px;
  margin: 0 auto;
}

.welcome {
  padding: 20px;
}

.welcome h2 {
  margin-bottom: 10px;
  color: #303133;
}

.welcome p {
  color: #909399;
  margin-bottom: 16px;
}
</style>
