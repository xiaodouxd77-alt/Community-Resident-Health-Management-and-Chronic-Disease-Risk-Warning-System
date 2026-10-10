<template>
  <div v-loading="loading">
    <el-card class="profile-card">
      <div class="profile-head">
        <div>
          <h2>{{ profile?.realName || '未填写姓名' }}</h2>
          <p class="muted">居民编号：{{ profile?.residentNo || '尚未建档' }}</p>
        </div>
        <el-button :icon="Edit" @click="openProfileDialog">完善档案</el-button>
      </div>
      <el-descriptions :column="3" border>
        <el-descriptions-item label="性别">{{ profile?.genderLabel || '-' }}</el-descriptions-item>
        <el-descriptions-item label="出生日期">{{ profile?.birthDate || '-' }}</el-descriptions-item>
        <el-descriptions-item label="联系电话">{{ profile?.phone || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-row :gutter="16" class="stat-row">
      <el-col :span="8">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-label">最近测量时间</div>
          <div class="stat-value small">{{ latestMeasuredAt || '暂无记录' }}</div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-label">最近一次评估</div>
          <div class="stat-value">
            <el-tag v-if="latestAssessment" :type="riskTag(latestAssessment.riskLevel)" effect="dark">
              {{ latestAssessment.riskLevelLabel }}
            </el-tag>
            <span v-else class="muted">暂无评估</span>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover" class="stat-card clickable" @click="$router.push('/resident/alerts')">
          <div class="stat-label">待处理预警</div>
          <div class="stat-value">
            <span :class="{ danger: pendingAlertCount > 0 }">{{ pendingAlertCount }}</span>
            <span class="unit">条</span>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card class="actions-card">
      <template #header><span>快捷操作</span></template>
      <el-button type="primary" :icon="Plus" @click="$router.push('/resident/indicators/new')">
        录入健康指标
      </el-button>
      <el-button :icon="Clock" @click="$router.push('/resident/indicators')">查看指标历史</el-button>
      <el-button :icon="DataAnalysis" @click="$router.push('/resident/assessments')">评估结果</el-button>
      <el-button :icon="Warning" @click="$router.push('/resident/alerts')">我的预警</el-button>
    </el-card>

    <el-card class="recent-card">
      <template #header><span>最近指标</span></template>
      <el-empty v-if="recentIndicators.length === 0" description="暂无健康指标记录" />
      <el-table v-else :data="recentIndicators" style="width: 100%">
        <el-table-column prop="indicatorTypeLabel" label="指标" width="100" />
        <el-table-column label="测量值" width="180">
          <template #default="{ row }">{{ formatIndicatorValue(row) }}</template>
        </el-table-column>
        <el-table-column prop="measurementContextLabel" label="场景" width="110">
          <template #default="{ row }">{{ row.measurementContextLabel || '-' }}</template>
        </el-table-column>
        <el-table-column prop="measuredAt" label="测量时间" width="180" />
        <el-table-column label="评估" width="110">
          <template #default="{ row }">
            <el-tag v-if="row.assessmentRiskLevel" :type="riskTag(row.assessmentRiskLevel)" size="small">
              {{ row.assessmentRiskLevelLabel }}
            </el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="profileDialogVisible" title="完善健康档案" width="420px">
      <el-form :model="profileForm" :rules="profileRules" ref="profileFormRef" label-width="90px">
        <el-form-item label="姓名" prop="realName">
          <el-input v-model="profileForm.realName" placeholder="请输入真实姓名" />
        </el-form-item>
        <el-form-item label="性别" prop="gender">
          <el-select v-model="profileForm.gender" placeholder="请选择" style="width: 100%">
            <el-option label="男" value="MALE" />
            <el-option label="女" value="FEMALE" />
            <el-option label="未知" value="UNKNOWN" />
          </el-select>
        </el-form-item>
        <el-form-item label="出生日期" prop="birthDate">
          <el-date-picker
            v-model="profileForm.birthDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="选择日期"
            style="width: 100%"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="profileDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveProfile">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Edit, Plus, Clock, DataAnalysis, Warning } from '@element-plus/icons-vue'
import { getMyProfile, updateMyProfile, getMyIndicators, getMyAssessments, getMyAlerts } from '@/api/health'
import { formatIndicatorValue, riskTag } from '@/utils/labels'

const loading = ref(false)
const profile = ref(null)
const recentIndicators = ref([])
const latestAssessment = ref(null)
const latestMeasuredAt = ref('')
const pendingAlertCount = ref(0)

const profileDialogVisible = ref(false)
const saving = ref(false)
const profileFormRef = ref(null)
const profileForm = reactive({ realName: '', gender: 'UNKNOWN', birthDate: null })
const profileRules = {
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }]
}

const load = async () => {
  loading.value = true
  try {
    const [profileRes, indicatorsRes, assessmentsRes, alertsRes] = await Promise.all([
      getMyProfile(),
      getMyIndicators({ page: 1, size: 5 }),
      getMyAssessments({ page: 1, size: 1 }),
      getMyAlerts({ page: 1, size: 1, status: 'PENDING' })
    ])
    profile.value = profileRes.data
    recentIndicators.value = indicatorsRes.data.records || []
    latestMeasuredAt.value = recentIndicators.value[0]?.measuredAt || ''
    latestAssessment.value = (assessmentsRes.data.records || [])[0] || null
    pendingAlertCount.value = alertsRes.data.total || 0
  } finally {
    loading.value = false
  }
}

const openProfileDialog = () => {
  profileForm.realName = profile.value?.realName || ''
  profileForm.gender = profile.value?.gender || 'UNKNOWN'
  profileForm.birthDate = profile.value?.birthDate || null
  profileDialogVisible.value = true
}

const saveProfile = async () => {
  await profileFormRef.value.validate()
  saving.value = true
  try {
    const res = await updateMyProfile(profileForm)
    profile.value = res.data
    ElMessage.success('档案已更新')
    profileDialogVisible.value = false
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.profile-card {
  margin-bottom: 16px;
}

.profile-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 16px;
}

.profile-head h2 {
  color: #303133;
  margin-bottom: 4px;
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
  font-size: 22px;
  font-weight: 600;
  color: #1f6fd6;
}

.stat-value.small {
  font-size: 16px;
}

.stat-value .unit {
  font-size: 13px;
  color: #909399;
  margin-left: 4px;
}

.danger {
  color: #e6a23c;
}

.actions-card,
.recent-card {
  margin-bottom: 16px;
}
</style>
