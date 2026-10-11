<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>我的随访</span>
          <el-button :icon="Refresh" size="small" @click="loadAll">刷新</el-button>
        </div>
      </template>

      <el-tabs v-model="activeTab">
        <!-- 随访计划 -->
        <el-tab-pane label="随访计划" name="plans">
          <el-table v-loading="planLoading" :data="plans" style="width: 100%" empty-text="暂无随访计划">
            <el-table-column prop="planTitle" label="计划标题" min-width="180" show-overflow-tooltip />
            <el-table-column prop="scheduledAt" label="计划随访时间" width="170" />
            <el-table-column prop="followupMethodLabel" label="随访方式" width="100" />
            <el-table-column label="状态" width="160">
              <template #default="{ row }">
                <el-tag :type="planStatusTag(row.planStatus)" size="small">{{ row.planStatusLabel }}</el-tag>
                <el-tag v-if="row.overdue" type="danger" size="small" effect="plain" class="ml6">已逾期</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="doctorName" label="负责医生" width="110" />
            <el-table-column prop="followupPurpose" label="随访目的" min-width="180" show-overflow-tooltip>
              <template #default="{ row }">{{ row.followupPurpose || '-' }}</template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <!-- 随访记录 -->
        <el-tab-pane label="随访记录" name="records">
          <el-table v-loading="recordLoading" :data="records" style="width: 100%" empty-text="暂无随访记录">
            <el-table-column prop="followedUpAt" label="随访时间" width="170" />
            <el-table-column prop="followupMethodLabel" label="随访方式" width="100" />
            <el-table-column prop="doctorName" label="随访医生" width="110" />
            <el-table-column prop="followupResult" label="随访结果" min-width="220" show-overflow-tooltip>
              <template #default="{ row }">{{ row.followupResult || '-' }}</template>
            </el-table-column>
            <el-table-column prop="nextFollowupAt" label="建议下次随访" width="170">
              <template #default="{ row }">{{ row.nextFollowupAt || '-' }}</template>
            </el-table-column>
            <el-table-column label="操作" width="90" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="openDetail(row.id)">详情</el-button>
              </template>
            </el-table-column>
          </el-table>

          <el-pagination
            v-model:current-page="recordQuery.page"
            v-model:page-size="recordQuery.size"
            :total="recordTotal"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next"
            class="pager"
            @size-change="loadRecords"
            @current-change="loadRecords"
          />
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- 随访记录详情 -->
    <el-dialog v-model="detailVisible" title="随访记录详情" width="600px">
      <template v-if="detail">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="随访时间">{{ detail.followedUpAt }}</el-descriptions-item>
          <el-descriptions-item label="随访方式">{{ detail.followupMethodLabel }}</el-descriptions-item>
          <el-descriptions-item label="随访医生">{{ detail.doctorName || '-' }}</el-descriptions-item>
          <el-descriptions-item v-if="detail.residentFeedback" label="我的反馈">
            {{ detail.residentFeedback }}
          </el-descriptions-item>
          <el-descriptions-item v-if="detail.healthSummary" label="健康情况摘要">
            {{ detail.healthSummary }}
          </el-descriptions-item>
          <el-descriptions-item v-if="detail.indicatorSummary" label="指标变化摘要">
            {{ detail.indicatorSummary }}
          </el-descriptions-item>
          <el-descriptions-item v-if="detail.interventionAdvice" label="干预建议">
            {{ detail.interventionAdvice }}
          </el-descriptions-item>
          <el-descriptions-item v-if="detail.followupResult" label="随访结果">
            {{ detail.followupResult }}
          </el-descriptions-item>
          <el-descriptions-item v-if="detail.nextFollowupAt" label="建议下次随访">
            {{ detail.nextFollowupAt }}
          </el-descriptions-item>
        </el-descriptions>
        <el-alert
          type="info"
          :closable="false"
          show-icon
          class="notice"
          title="随访结果仅为健康管理记录，不作为医疗诊断依据。如有不适请及时就医。"
        />
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import {
  getMyFollowupPlans,
  getMyFollowupRecords,
  getMyFollowupRecordDetail
} from '@/api/health'
import { planStatusTag } from '@/utils/labels'

const activeTab = ref('plans')
const plans = ref([])
const records = ref([])
const recordTotal = ref(0)
const planLoading = ref(false)
const recordLoading = ref(false)
const recordQuery = reactive({ page: 1, size: 10 })

const detailVisible = ref(false)
const detail = ref(null)

const loadPlans = async () => {
  planLoading.value = true
  try {
    const res = await getMyFollowupPlans()
    plans.value = res.data || []
  } finally {
    planLoading.value = false
  }
}

const loadRecords = async () => {
  recordLoading.value = true
  try {
    const res = await getMyFollowupRecords({ page: recordQuery.page, size: recordQuery.size })
    records.value = res.data.records || []
    recordTotal.value = res.data.total || 0
  } finally {
    recordLoading.value = false
  }
}

const loadAll = () => {
  loadPlans()
  loadRecords()
}

const openDetail = async (id) => {
  try {
    const res = await getMyFollowupRecordDetail(id)
    detail.value = res.data
    detailVisible.value = true
  } catch (error) {
    // 不存在或无权限查看，拦截器已提示
  }
}

onMounted(loadAll)
</script>

<style scoped>
.page-container {
  max-width: 1100px;
  margin: 0 auto;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.pager {
  margin-top: 16px;
  justify-content: flex-end;
}

.notice {
  margin-top: 14px;
}

.ml6 {
  margin-left: 6px;
}
</style>
