<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>我的慢病档案</span>
          <span class="muted">慢病建档由医生依据诊断资料审核建立，居民不可自行修改</span>
        </div>
      </template>

      <el-table v-loading="loading" :data="list" style="width: 100%" empty-text="暂无慢病档案">
        <el-table-column prop="diseaseTypeLabel" label="慢病类型" width="120" />
        <el-table-column label="资料状态" width="140">
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
        <el-table-column prop="diagnosisDate" label="诊断日期" width="130" />
        <el-table-column prop="managingDoctorName" label="负责医生" width="120" />
        <el-table-column prop="createTime" label="建档时间" width="180" />
        <el-table-column prop="updateTime" label="最近更新" width="180" />
        <el-table-column label="操作" width="90" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="showDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="detailVisible" title="慢病档案详情" width="560px">
      <el-descriptions v-if="current" :column="1" border>
        <el-descriptions-item label="慢病类型">{{ current.diseaseTypeLabel }}</el-descriptions-item>
        <el-descriptions-item label="资料状态">
          <el-tag :type="diagnosisStatusTag(current.diagnosisStatus)" size="small">
            {{ current.diagnosisStatusLabel }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="管理状态">
          <el-tag :type="managementStatusTag(current.managementStatus)" size="small">
            {{ current.managementStatusLabel }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="诊断日期">{{ current.diagnosisDate || '-' }}</el-descriptions-item>
        <el-descriptions-item label="诊断依据">{{ current.diagnosisBasis || '-' }}</el-descriptions-item>
        <el-descriptions-item label="负责医生">{{ current.managingDoctorName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="审核医生">{{ current.reviewedByName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="审核时间">{{ current.reviewedAt || '-' }}</el-descriptions-item>
        <el-descriptions-item label="备注">{{ current.remark || '-' }}</el-descriptions-item>
        <el-descriptions-item label="建档时间">{{ current.createTime }}</el-descriptions-item>
        <el-descriptions-item label="最近更新">{{ current.updateTime }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getMyChronicDiseases } from '@/api/health'
import { diagnosisStatusTag, managementStatusTag } from '@/utils/labels'

const loading = ref(false)
const list = ref([])
const detailVisible = ref(false)
const current = ref(null)

const load = async () => {
  loading.value = true
  try {
    const res = await getMyChronicDiseases()
    list.value = res.data || []
  } finally {
    loading.value = false
  }
}

const showDetail = (row) => {
  current.value = row
  detailVisible.value = true
}

onMounted(load)
</script>

<style scoped>
.page-container {
  max-width: 1100px;
  margin: 0 auto;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
}

.muted {
  color: #909399;
  font-size: 13px;
}
</style>
