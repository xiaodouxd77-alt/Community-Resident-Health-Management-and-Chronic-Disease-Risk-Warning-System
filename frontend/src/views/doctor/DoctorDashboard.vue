<template>
  <div v-loading="loading">
    <el-card class="banner-card">
      <h2>医生工作台</h2>
      <p class="muted">当前仅展示您授权管理范围内的居民预警</p>
    </el-card>

    <el-row :gutter="16" class="stat-row">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card clickable" @click="goPending">
          <div class="stat-label">待处理预警</div>
          <div class="stat-value danger">{{ counts.pending }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card clickable" @click="goStatus('PROCESSING')">
          <div class="stat-label">处理中</div>
          <div class="stat-value warning">{{ counts.processing }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card clickable" @click="goStatus('RESOLVED')">
          <div class="stat-label">已完成</div>
          <div class="stat-value success">{{ counts.resolved }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-label">管理居民数</div>
          <div class="stat-value">{{ residentCount }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-card>
      <template #header><span>待办提醒</span></template>
      <el-empty v-if="counts.pending === 0" description="暂无待处理预警" />
      <div v-else class="todo">
        <el-icon class="todo-icon"><Bell /></el-icon>
        <span>您有 <b>{{ counts.pending }}</b> 条待处理预警，请及时处理。</span>
        <el-button type="primary" size="small" @click="goPending">立即处理</el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Bell } from '@element-plus/icons-vue'
import { getDoctorAlerts, getMyResidents } from '@/api/doctor'

const router = useRouter()
const loading = ref(false)
const counts = reactive({ pending: 0, processing: 0, resolved: 0 })
const residentCount = ref(0)

const load = async () => {
  loading.value = true
  try {
    const [pending, processing, resolved, residents] = await Promise.all([
      getDoctorAlerts({ page: 1, size: 1, status: 'PENDING' }),
      getDoctorAlerts({ page: 1, size: 1, status: 'PROCESSING' }),
      getDoctorAlerts({ page: 1, size: 1, status: 'RESOLVED' }),
      getMyResidents()
    ])
    counts.pending = pending.data.total || 0
    counts.processing = processing.data.total || 0
    counts.resolved = resolved.data.total || 0
    residentCount.value = (residents.data || []).length
  } finally {
    loading.value = false
  }
}

const goPending = () => router.push({ path: '/doctor/alerts', query: { status: 'PENDING' } })
const goStatus = (status) => router.push({ path: '/doctor/alerts', query: { status } })

onMounted(load)
</script>

<style scoped>
.banner-card {
  margin-bottom: 16px;
}

.banner-card h2 {
  color: #303133;
  margin-bottom: 6px;
}

.muted {
  color: #909399;
  font-size: 13px;
}

.stat-row {
  margin-bottom: 16px;
}

.stat-card {
  text-align: center;
}

.stat-card.clickable {
  cursor: pointer;
}

.stat-label {
  color: #909399;
  font-size: 13px;
  margin-bottom: 8px;
}

.stat-value {
  font-size: 26px;
  font-weight: 600;
  color: #1f6fd6;
}

.danger { color: #f56c6c; }
.warning { color: #e6a23c; }
.success { color: #3ab7a0; }

.todo {
  display: flex;
  align-items: center;
  gap: 10px;
  color: #606266;
}

.todo-icon {
  color: #e6a23c;
  font-size: 20px;
}
</style>
