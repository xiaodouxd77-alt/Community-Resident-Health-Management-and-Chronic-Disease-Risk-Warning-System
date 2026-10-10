<template>
  <div class="page-container" v-loading="loading">
    <el-page-header @back="$router.push('/resident/alerts')" content="预警详情" class="page-header" />

    <template v-if="detail">
      <el-card class="section">
        <template #header>
          <div class="card-header">
            <span>{{ detail.alert.alertTitle }}</span>
            <el-tag :type="statusTag(detail.alert.status)" effect="dark">{{ detail.alert.statusLabel }}</el-tag>
          </div>
        </template>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="预警编号">{{ detail.alert.alertNo }}</el-descriptions-item>
          <el-descriptions-item label="风险等级">
            <el-tag :type="riskTag(detail.alert.riskLevel)" effect="dark" size="small">
              {{ detail.alert.riskLevelLabel }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="指标类型">{{ detail.alert.indicatorTypeLabel }}</el-descriptions-item>
          <el-descriptions-item label="测量值">{{ formatIndicatorValue(detail.alert) }}</el-descriptions-item>
          <el-descriptions-item label="测量时间">{{ detail.alert.measuredAt }}</el-descriptions-item>
          <el-descriptions-item label="预警时间">{{ detail.alert.createTime }}</el-descriptions-item>
          <el-descriptions-item label="负责医生" :span="2">
            {{ detail.alert.assignedDoctorName || '待分配' }}
          </el-descriptions-item>
          <el-descriptions-item label="触发原因" :span="2">
            {{ detail.alert.alertReason }}
          </el-descriptions-item>
        </el-descriptions>
      </el-card>

      <el-card class="section">
        <template #header><span>风险评估依据</span></template>
        <el-alert
          v-if="detail.assessment"
          :type="detail.assessment.riskLevel === 'LOW' ? 'info' : 'warning'"
          :closable="false"
          show-icon
          :title="detail.assessment.assessmentSummary"
        />
        <el-table
          v-if="detail.assessment?.matchedRules?.length"
          :data="detail.assessment.matchedRules"
          size="small"
          style="margin-top: 14px"
        >
          <el-table-column prop="ruleName" label="命中规则" width="160" />
          <el-table-column label="判定" width="220">
            <template #default="{ row }">
              {{ row.valueFieldLabel }} {{ row.actualValue }} {{ row.operator }} {{ row.thresholdValue }}
            </template>
          </el-table-column>
          <el-table-column label="风险" width="90">
            <template #default="{ row }">
              <el-tag :type="riskTag(row.riskLevel)" size="small">{{ row.riskLevelLabel }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="description" label="说明" min-width="260" show-overflow-tooltip />
        </el-table>
      </el-card>

      <el-card class="section">
        <template #header><span>医生处理记录</span></template>
        <el-empty v-if="!detail.handlingRecords?.length" description="医生尚未处理" />
        <el-timeline v-else>
          <el-timeline-item
            v-for="record in detail.handlingRecords"
            :key="record.id"
            :timestamp="record.createTime"
            placement="top"
            type="primary"
          >
            <div class="handling-item">
              <div class="handling-head">
                <el-tag size="small">{{ record.actionLabel }}</el-tag>
                <span class="muted">{{ record.doctorName }}</span>
              </div>
              <p>{{ record.handlingComment }}</p>
              <p v-if="record.followupRequired" class="muted">
                需要随访<span v-if="record.nextFollowupAt">，建议时间：{{ record.nextFollowupAt }}</span>
              </p>
            </div>
          </el-timeline-item>
        </el-timeline>
      </el-card>

      <el-card class="section">
        <template #header><span>相关历史指标</span></template>
        <el-empty v-if="!detail.recentIndicators?.length" description="暂无记录" />
        <el-table v-else :data="detail.recentIndicators" size="small">
          <el-table-column label="测量值" width="180">
            <template #default="{ row }">{{ formatIndicatorValue(row) }}</template>
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
    </template>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { getMyAlertDetail } from '@/api/health'
import { riskTag, statusTag, formatIndicatorValue } from '@/utils/labels'

const route = useRoute()
const loading = ref(false)
const detail = ref(null)

const load = async () => {
  loading.value = true
  try {
    const res = await getMyAlertDetail(route.params.id)
    detail.value = res.data
  } finally {
    loading.value = false
  }
}

onMounted(load)
watch(() => route.params.id, load)
</script>

<style scoped>
.page-container {
  max-width: 1000px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 16px;
}

.section {
  margin-bottom: 16px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.muted {
  color: #909399;
  font-size: 13px;
}

.handling-item p {
  margin: 6px 0 0;
  color: #606266;
}

.handling-head {
  display: flex;
  align-items: center;
  gap: 8px;
}
</style>
