<template>
  <div class="page-container">
    <el-card class="section stats-card">
      <template #header>
        <div class="card-header">
          <span>随访工作台</span>
          <el-button :icon="Refresh" size="small" @click="loadAll">刷新</el-button>
        </div>
      </template>
      <div class="stats" v-loading="statsLoading">
        <div class="stat" :class="{ clickable: true }" @click="goList({ planStatus: 'PLANNED' })">
          <div class="stat-value warning">{{ stats.plannedCount ?? 0 }}</div>
          <div class="stat-label">待执行</div>
        </div>
        <div class="stat clickable" @click="goList({ planStatus: 'IN_PROGRESS' })">
          <div class="stat-value primary">{{ stats.inProgressCount ?? 0 }}</div>
          <div class="stat-label">进行中</div>
        </div>
        <div class="stat clickable" @click="goList({ planStatus: 'COMPLETED' })">
          <div class="stat-value success">{{ stats.completedCount ?? 0 }}</div>
          <div class="stat-label">已完成</div>
        </div>
        <div class="stat clickable" @click="goList({ overdueOnly: true })">
          <div class="stat-value danger">{{ stats.overdueCount ?? 0 }}</div>
          <div class="stat-label">已逾期</div>
        </div>
      </div>
    </el-card>

    <el-card class="section">
      <template #header>
        <div class="card-header">
          <span>近期待办</span>
          <el-button link type="primary" @click="goList({ planStatus: 'PLANNED' })">查看全部</el-button>
        </div>
      </template>
      <el-table v-loading="todoLoading" :data="todoList" style="width: 100%" empty-text="暂无待办随访计划">
        <el-table-column label="居民" width="160">
          <template #default="{ row }">
            <div>{{ row.residentName || '-' }}</div>
            <div class="muted">{{ row.residentNo }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="planTitle" label="计划标题" min-width="180" show-overflow-tooltip />
        <el-table-column prop="scheduledAt" label="计划时间" width="170" />
        <el-table-column prop="followupMethodLabel" label="方式" width="90" />
        <el-table-column label="状态" width="130">
          <template #default="{ row }">
            <el-tag :type="planStatusTag(row.planStatus)" size="small">{{ row.planStatusLabel }}</el-tag>
            <el-tag v-if="row.overdue" type="danger" size="small" effect="plain" class="ml6">已逾期</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="goDetail(row.id)">处理</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card class="section">
      <template #header>
        <div class="card-header">
          <span>近 7 天到期 / 逾期</span>
          <el-button link type="primary" @click="goList({ overdueOnly: true })">查看逾期</el-button>
        </div>
      </template>
      <el-table v-loading="upcomingLoading" :data="upcomingList" style="width: 100%" empty-text="近 7 天暂无到期计划">
        <el-table-column label="居民" width="160">
          <template #default="{ row }">
            <div>{{ row.residentName || '-' }}</div>
            <div class="muted">{{ row.residentNo }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="planTitle" label="计划标题" min-width="180" show-overflow-tooltip />
        <el-table-column prop="scheduledAt" label="计划时间" width="170" />
        <el-table-column prop="followupMethodLabel" label="方式" width="90" />
        <el-table-column label="是否逾期" width="110">
          <template #default="{ row }">
            <el-tag :type="row.overdue ? 'danger' : 'info'" size="small" effect="plain">
              {{ row.overdue ? '已逾期' : '即将到期' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="goDetail(row.id)">处理</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Refresh } from '@element-plus/icons-vue'
import {
  getFollowupStats,
  getTodoFollowupPlans,
  getUpcomingFollowupPlans
} from '@/api/doctor'
import { planStatusTag } from '@/utils/labels'

const router = useRouter()
const stats = reactive({ plannedCount: 0, inProgressCount: 0, completedCount: 0, overdueCount: 0 })
const todoList = ref([])
const upcomingList = ref([])
const statsLoading = ref(false)
const todoLoading = ref(false)
const upcomingLoading = ref(false)

const loadStats = async () => {
  statsLoading.value = true
  try {
    const res = await getFollowupStats()
    Object.assign(stats, res.data || {})
  } finally {
    statsLoading.value = false
  }
}

const loadTodo = async () => {
  todoLoading.value = true
  try {
    const res = await getTodoFollowupPlans({ limit: 8 })
    todoList.value = res.data || []
  } finally {
    todoLoading.value = false
  }
}

const loadUpcoming = async () => {
  upcomingLoading.value = true
  try {
    const res = await getUpcomingFollowupPlans({ days: 7 })
    upcomingList.value = res.data || []
  } finally {
    upcomingLoading.value = false
  }
}

const loadAll = () => {
  loadStats()
  loadTodo()
  loadUpcoming()
}

const goDetail = (id) => router.push(`/doctor/followups/plan/${id}`)

const goList = (query) => router.push({ path: '/doctor/followups', query })

onMounted(loadAll)
</script>

<style scoped>
.page-container {
  max-width: 1320px;
  margin: 0 auto;
}

.section {
  margin-bottom: 16px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.stats {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.stat {
  border-radius: 8px;
  padding: 20px;
  text-align: center;
  background: #f7f9fc;
  border: 1px solid #eef1f5;
}

.stat.clickable {
  cursor: pointer;
  transition: box-shadow 0.2s;
}

.stat.clickable:hover {
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.08);
}

.stat-value {
  font-size: 30px;
  font-weight: 700;
}

.stat-value.warning {
  color: #e6a23c;
}

.stat-value.primary {
  color: #1f6fd6;
}

.stat-value.success {
  color: #67c23a;
}

.stat-value.danger {
  color: #f56c6c;
}

.stat-label {
  margin-top: 6px;
  color: #909399;
  font-size: 13px;
}

.muted {
  color: #909399;
  font-size: 12px;
}

.ml6 {
  margin-left: 6px;
}
</style>
