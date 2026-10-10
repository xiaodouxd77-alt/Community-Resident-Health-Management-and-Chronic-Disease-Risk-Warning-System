<template>
  <div class="page-container">
    <el-card>
      <template #header><span>风险评估结果</span></template>

      <el-table
        v-loading="loading"
        :data="tableData"
        style="width: 100%"
        empty-text="暂无评估结果"
      >
        <el-table-column prop="assessmentCode" label="评估编号" width="210" />
        <el-table-column prop="indicatorTypeLabel" label="指标" width="90" />
        <el-table-column label="风险等级" width="120">
          <template #default="{ row }">
            <el-tag :type="riskTag(row.riskLevel)" effect="dark" size="small">
              {{ row.riskLevelLabel }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="assessmentSummary" label="评估摘要" min-width="320" show-overflow-tooltip />
        <el-table-column prop="ruleVersion" label="规则版本" width="100" />
        <el-table-column prop="assessedAt" label="评估时间" width="180" />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="showDetail(row)">依据</el-button>
            <el-button v-if="row.alertId" link type="danger" @click="goAlert(row.alertId)">预警</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="query.page"
        v-model:page-size="query.size"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        class="pager"
        @size-change="load"
        @current-change="load"
      />
    </el-card>

    <el-dialog v-model="detailVisible" title="评估依据" width="680px">
      <template v-if="current">
        <el-alert
          :type="current.riskLevel === 'LOW' ? 'info' : 'warning'"
          :closable="false"
          show-icon
          :title="current.assessmentSummary"
        />
        <el-table
          v-if="current.matchedRules && current.matchedRules.length"
          :data="current.matchedRules"
          size="small"
          style="margin-top: 16px"
        >
          <el-table-column prop="ruleName" label="命中规则" width="150" />
          <el-table-column label="判定" width="200">
            <template #default="{ row }">
              {{ row.valueFieldLabel }} {{ row.actualValue }} {{ row.operator }} {{ row.thresholdValue }}
            </template>
          </el-table-column>
          <el-table-column label="风险" width="90">
            <template #default="{ row }">
              <el-tag :type="riskTag(row.riskLevel)" size="small">{{ row.riskLevelLabel }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="description" label="说明" min-width="240" show-overflow-tooltip />
        </el-table>
        <el-empty v-else description="本次未命中任何规则" />
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getMyAssessments } from '@/api/health'
import { riskTag } from '@/utils/labels'

const router = useRouter()
const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const detailVisible = ref(false)
const current = ref(null)
const query = reactive({ page: 1, size: 10 })

const load = async () => {
  loading.value = true
  try {
    const res = await getMyAssessments({ page: query.page, size: query.size })
    tableData.value = res.data.records || []
    total.value = res.data.total || 0
  } finally {
    loading.value = false
  }
}

const showDetail = (row) => {
  current.value = row
  detailVisible.value = true
}

const goAlert = (id) => router.push(`/resident/alerts/${id}`)

onMounted(load)
</script>

<style scoped>
.page-container {
  max-width: 1200px;
  margin: 0 auto;
}

.pager {
  margin-top: 16px;
  justify-content: flex-end;
}
</style>
