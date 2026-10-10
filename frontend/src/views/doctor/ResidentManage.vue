<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>居民档案管理</span>
          <span class="muted">仅展示您所负责的居民</span>
        </div>
      </template>

      <div class="filters">
        <el-input
          v-model="query.keyword"
          placeholder="姓名 / 居民编号"
          clearable
          style="width: 220px"
          @keyup.enter="handleSearch"
        />
        <el-select v-model="query.communityId" placeholder="全部社区" clearable style="width: 180px">
          <el-option v-for="c in communities" :key="c.id" :label="c.communityName" :value="c.id" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
        <el-button :icon="Refresh" @click="handleReset">重置</el-button>
      </div>

      <el-table v-loading="loading" :data="tableData" style="width: 100%" empty-text="暂无居民数据">
        <el-table-column prop="realName" label="姓名" width="120">
          <template #default="{ row }">{{ row.realName || '-' }}</template>
        </el-table-column>
        <el-table-column prop="residentNo" label="居民编号" width="150" />
        <el-table-column prop="genderLabel" label="性别" width="80" />
        <el-table-column prop="birthDate" label="出生日期" width="120" />
        <el-table-column prop="communityName" label="所属社区" width="130">
          <template #default="{ row }">{{ row.communityName || '-' }}</template>
        </el-table-column>
        <el-table-column label="档案核实" width="120">
          <template #default="{ row }">
            <el-tag :type="row.verified ? 'success' : 'info'" size="small">
              {{ row.verified ? '已核实' : '待核实' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="updateTime" label="最近更新" width="180" />
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="goDetail(row.id)">查看详情</el-button>
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
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Search, Refresh } from '@element-plus/icons-vue'
import { getResidentsPage, getCommunities } from '@/api/doctor'

const router = useRouter()
const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const communities = ref([])
const query = reactive({ page: 1, size: 10, keyword: '', communityId: '' })

const load = async () => {
  loading.value = true
  try {
    const res = await getResidentsPage({
      page: query.page,
      size: query.size,
      keyword: query.keyword || undefined,
      communityId: query.communityId || undefined
    })
    tableData.value = res.data.records || []
    total.value = res.data.total || 0
  } finally {
    loading.value = false
  }
}

const loadCommunities = async () => {
  const res = await getCommunities()
  communities.value = res.data || []
}

const handleSearch = () => {
  query.page = 1
  load()
}

const handleReset = () => {
  query.keyword = ''
  query.communityId = ''
  query.page = 1
  load()
}

const goDetail = (id) => router.push(`/doctor/residents/${id}`)

onMounted(() => {
  loadCommunities()
  load()
})
</script>

<style scoped>
.page-container {
  max-width: 1200px;
  margin: 0 auto;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
}

.filters {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 16px;
}

.pager {
  margin-top: 16px;
  justify-content: flex-end;
}

.muted {
  color: #909399;
  font-size: 13px;
}
</style>
