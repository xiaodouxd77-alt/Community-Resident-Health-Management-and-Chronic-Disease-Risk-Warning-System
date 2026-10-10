<template>
  <div class="page-container" v-loading="loading">
    <el-card class="section">
      <template #header>
        <div class="card-header">
          <span>我的健康档案</span>
          <div>
            <el-tag v-if="profile" :type="profile.verified ? 'success' : 'info'" effect="plain" size="small">
              {{ profile.verified ? '资料已核实' : '居民自述（未核实）' }}
            </el-tag>
            <el-button v-if="profile" type="primary" :icon="Edit" class="ml8" @click="openEdit">
              编辑档案
            </el-button>
          </div>
        </div>
      </template>

      <el-empty v-if="!profile" description="尚未建立健康档案">
        <el-button type="primary" :icon="Plus" @click="openCreate">创建我的健康档案</el-button>
      </el-empty>

      <template v-else>
        <el-descriptions :column="3" border>
          <el-descriptions-item label="姓名">{{ profile.realName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="性别">{{ profile.genderLabel || '-' }}</el-descriptions-item>
          <el-descriptions-item label="出生日期">{{ profile.birthDate || '-' }}</el-descriptions-item>
          <el-descriptions-item label="居民编号">{{ profile.residentNo }}</el-descriptions-item>
          <el-descriptions-item label="所属社区">{{ profile.communityName || '未填写' }}</el-descriptions-item>
          <el-descriptions-item label="身高">{{ profile.height ? profile.height + ' cm' : '-' }}</el-descriptions-item>
          <el-descriptions-item label="联系电话">{{ profile.phone || '-' }}</el-descriptions-item>
          <el-descriptions-item label="建档时间">{{ profile.createTime || '-' }}</el-descriptions-item>
          <el-descriptions-item label="最近更新">{{ profile.updateTime || '-' }}</el-descriptions-item>
          <el-descriptions-item label="既往病史" :span="3">
            {{ profile.pastHistory || '未填写' }}
          </el-descriptions-item>
          <el-descriptions-item label="家族病史" :span="3">
            {{ profile.familyHistory || '未填写' }}
          </el-descriptions-item>
          <el-descriptions-item label="过敏史" :span="3">
            {{ profile.allergyHistory || '未填写' }}
          </el-descriptions-item>
          <el-descriptions-item label="健康备注" :span="3">
            {{ profile.healthRemark || '未填写' }}
          </el-descriptions-item>
          <el-descriptions-item v-if="profile.verified" label="核实信息" :span="3">
            {{ profile.verifiedDoctorName || '医生' }} 于 {{ profile.verifiedAt }} 核实
          </el-descriptions-item>
        </el-descriptions>
        <p class="muted tip">
          注：既往病史、家族病史、过敏史等为居民自述信息，仅供参考；经医生核实的资料以"资料已核实"标识为准。
        </p>
      </template>
    </el-card>

    <el-card v-if="profile" class="section">
      <el-tabs v-model="activeTab">
        <el-tab-pane label="健康指标" name="indicators">
          <div class="tab-actions">
            <el-button link type="primary" @click="$router.push('/resident/indicators')">查看全部指标历史</el-button>
          </div>
          <el-table v-loading="tabLoading.indicators" :data="indicators" size="small" empty-text="暂无指标记录">
            <el-table-column prop="indicatorTypeLabel" label="指标" width="100" />
            <el-table-column label="测量值" width="180">
              <template #default="{ row }">{{ formatIndicatorValue(row) }}</template>
            </el-table-column>
            <el-table-column prop="measuredAt" label="测量时间" width="180" />
            <el-table-column label="风险评估" width="120">
              <template #default="{ row }">
                <el-tag v-if="row.assessmentRiskLevel" :type="riskTag(row.assessmentRiskLevel)" size="small">
                  {{ row.assessmentRiskLevelLabel }}
                </el-tag>
                <span v-else>-</span>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="慢病档案" name="chronic">
          <div class="tab-actions">
            <el-button link type="primary" @click="$router.push('/resident/chronic')">查看我的慢病档案</el-button>
          </div>
          <el-table v-loading="tabLoading.chronic" :data="chronicList" size="small" empty-text="暂无慢病档案">
            <el-table-column prop="diseaseTypeLabel" label="慢病类型" width="120" />
            <el-table-column label="资料状态" width="130">
              <template #default="{ row }">
                <el-tag :type="diagnosisStatusTag(row.diagnosisStatus)" size="small">
                  {{ row.diagnosisStatusLabel }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="管理状态" width="120">
              <template #default="{ row }">
                <el-tag :type="managementStatusTag(row.managementStatus)" size="small">
                  {{ row.managementStatusLabel }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="managingDoctorName" label="负责医生" width="120" />
            <el-table-column prop="createTime" label="建档时间" width="180" />
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="预警历史" name="alerts">
          <div class="tab-actions">
            <el-button link type="primary" @click="$router.push('/resident/alerts')">查看全部预警</el-button>
          </div>
          <el-table v-loading="tabLoading.alerts" :data="alerts" size="small" empty-text="暂无预警记录">
            <el-table-column prop="alertTitle" label="预警" width="200" />
            <el-table-column label="风险等级" width="110">
              <template #default="{ row }">
                <el-tag :type="riskTag(row.riskLevel)" effect="dark" size="small">{{ row.riskLevelLabel }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="statusTag(row.status)" size="small">{{ row.statusLabel }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createTime" label="预警时间" width="180" />
            <el-table-column label="操作" width="90">
              <template #default="{ row }">
                <el-button link type="primary" @click="$router.push(`/resident/alerts/${row.id}`)">查看</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <el-dialog v-model="formVisible" :title="formMode === 'create' ? '创建健康档案' : '编辑健康档案'" width="560px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="姓名" prop="realName">
          <el-input v-model="form.realName" placeholder="请输入真实姓名" />
        </el-form-item>
        <el-form-item label="性别" prop="gender">
          <el-select v-model="form.gender" style="width: 100%">
            <el-option label="男" value="MALE" />
            <el-option label="女" value="FEMALE" />
            <el-option label="未知" value="UNKNOWN" />
          </el-select>
        </el-form-item>
        <el-form-item label="出生日期" prop="birthDate">
          <el-date-picker
            v-model="form.birthDate"
            type="date"
            value-format="YYYY-MM-DD"
            :disabled-date="disableFuture"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="所属社区" prop="communityId">
          <el-select v-model="form.communityId" clearable placeholder="请选择" style="width: 100%">
            <el-option v-for="c in communities" :key="c.id" :label="c.communityName" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="身高(cm)" prop="height">
          <el-input-number v-model="form.height" :min="30" :max="250" :precision="1" :controls="false" style="width: 160px" />
        </el-form-item>
        <el-form-item label="既往病史" prop="pastHistory">
          <el-input v-model="form.pastHistory" type="textarea" :rows="2" maxlength="500" show-word-limit placeholder="选填" />
        </el-form-item>
        <el-form-item label="家族病史" prop="familyHistory">
          <el-input v-model="form.familyHistory" type="textarea" :rows="2" maxlength="500" show-word-limit placeholder="选填" />
        </el-form-item>
        <el-form-item label="过敏史" prop="allergyHistory">
          <el-input v-model="form.allergyHistory" type="textarea" :rows="2" maxlength="500" show-word-limit placeholder="选填" />
        </el-form-item>
        <el-form-item label="健康备注" prop="healthRemark">
          <el-input v-model="form.healthRemark" type="textarea" :rows="2" maxlength="500" show-word-limit placeholder="选填" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Edit, Plus } from '@element-plus/icons-vue'
import {
  getMyProfile,
  createMyProfile,
  updateMyProfile,
  getCommunities,
  getMyIndicators,
  getMyAlerts,
  getMyChronicDiseases
} from '@/api/health'
import {
  formatIndicatorValue,
  riskTag,
  statusTag,
  diagnosisStatusTag,
  managementStatusTag
} from '@/utils/labels'

const loading = ref(false)
const profile = ref(null)
const communities = ref([])
const activeTab = ref('indicators')

const indicators = ref([])
const chronicList = ref([])
const alerts = ref([])
const tabLoading = reactive({ indicators: false, chronic: false, alerts: false })

const formVisible = ref(false)
const formMode = ref('create')
const saving = ref(false)
const formRef = ref(null)
const form = reactive({
  realName: '',
  gender: 'UNKNOWN',
  birthDate: null,
  communityId: null,
  height: undefined,
  pastHistory: '',
  familyHistory: '',
  allergyHistory: '',
  healthRemark: ''
})

const rules = {
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }]
}

const disableFuture = (date) => date.getTime() > Date.now()

const resetForm = () => {
  Object.assign(form, {
    realName: '', gender: 'UNKNOWN', birthDate: null, communityId: null, height: undefined,
    pastHistory: '', familyHistory: '', allergyHistory: '', healthRemark: ''
  })
}

const loadProfile = async () => {
  loading.value = true
  try {
    const res = await getMyProfile()
    profile.value = res.data
    if (profile.value) {
      await loadTabs()
    }
  } finally {
    loading.value = false
  }
}

const loadTabs = async () => {
  tabLoading.indicators = true
  tabLoading.chronic = true
  tabLoading.alerts = true
  try {
    const [ind, chr, alt] = await Promise.all([
      getMyIndicators({ page: 1, size: 10 }),
      getMyChronicDiseases(),
      getMyAlerts({ page: 1, size: 10 })
    ])
    indicators.value = ind.data.records || []
    chronicList.value = chr.data || []
    alerts.value = alt.data.records || []
  } finally {
    tabLoading.indicators = false
    tabLoading.chronic = false
    tabLoading.alerts = false
  }
}

const loadCommunities = async () => {
  const res = await getCommunities()
  communities.value = res.data || []
}

const openCreate = () => {
  formMode.value = 'create'
  resetForm()
  formVisible.value = true
  formRef.value?.clearValidate()
}

const openEdit = () => {
  formMode.value = 'edit'
  Object.assign(form, {
    realName: profile.value.realName || '',
    gender: profile.value.gender || 'UNKNOWN',
    birthDate: profile.value.birthDate || null,
    communityId: profile.value.communityId || null,
    height: profile.value.height ?? undefined,
    pastHistory: profile.value.pastHistory || '',
    familyHistory: profile.value.familyHistory || '',
    allergyHistory: profile.value.allergyHistory || '',
    healthRemark: profile.value.healthRemark || ''
  })
  formVisible.value = true
  formRef.value?.clearValidate()
}

const submitForm = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    const payload = { ...form }
    if (formMode.value === 'create') {
      await createMyProfile(payload)
      ElMessage.success('健康档案已创建')
    } else {
      await updateMyProfile(payload)
      ElMessage.success('健康档案已更新')
    }
    formVisible.value = false
    await loadProfile()
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  loadCommunities()
  loadProfile()
})
</script>

<style scoped>
.page-container {
  max-width: 1100px;
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

.ml8 {
  margin-left: 8px;
}

.muted {
  color: #909399;
  font-size: 13px;
}

.tip {
  margin-top: 12px;
}

.tab-actions {
  text-align: right;
  margin-bottom: 8px;
}
</style>
