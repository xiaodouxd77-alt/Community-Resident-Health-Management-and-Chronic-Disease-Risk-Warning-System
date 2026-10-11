<template>
  <div class="page-container">
    <PageHeader title="社区公告" description="查看社区发布的公告与通知。">
      <el-button :icon="Refresh" @click="load">刷新</el-button>
    </PageHeader>

    <el-card>
      <FilterBar>
        <el-input v-model="query.keyword" placeholder="搜索公告标题" clearable style="width: 220px" @keyup.enter="handleSearch" />
        <el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
        <el-button :icon="RefreshLeft" @click="handleReset">重置</el-button>
      </FilterBar>

      <el-empty v-if="!loading && !notices.length" description="暂无有效公告" />
      <div v-else v-loading="loading" class="notice-list">
        <div v-for="n in notices" :key="n.id" class="notice-item" @click="openDetail(n.id)">
          <div class="notice-title">
            <el-tag v-if="n.isTop" type="danger" size="small" effect="dark" class="top-tag">置顶</el-tag>
            {{ n.title }}
          </div>
          <div class="notice-meta">
            <span class="muted">发布 {{ n.publishedAt || n.createTime }}</span>
            <span v-if="n.effectiveUntil" class="muted">有效期至 {{ n.effectiveUntil }}</span>
          </div>
        </div>
      </div>

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

    <el-dialog v-model="detailVisible" :title="detail?.title || '公告详情'" width="680px" v-loading="detailLoading">
      <template v-if="detail">
        <div class="detail-meta">
          <el-tag v-if="detail.isTop" type="danger" size="small" effect="dark">置顶</el-tag>
          <span class="muted">发布 {{ detail.publishedAt || '-' }}</span>
          <span v-if="detail.effectiveUntil" class="muted">有效期至 {{ detail.effectiveUntil }}</span>
        </div>
        <div class="detail-content">{{ detail.content }}</div>
      </template>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { Refresh, RefreshLeft, Search } from '@element-plus/icons-vue'
import { getNotices, getNoticeDetail } from '@/api/health'
import PageHeader from '@/components/PageHeader.vue'
import FilterBar from '@/components/FilterBar.vue'

const loading = ref(false)
const notices = ref([])
const total = ref(0)
const query = reactive({ page: 1, size: 10, keyword: '' })

const detailVisible = ref(false)
const detailLoading = ref(false)
const detail = ref(null)

const load = async () => {
  loading.value = true
  try {
    const res = await getNotices({
      page: query.page,
      size: query.size,
      keyword: query.keyword || undefined
    })
    notices.value = res.data.records || []
    total.value = res.data.total || 0
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  query.page = 1
  load()
}

const handleReset = () => {
  Object.assign(query, { keyword: '', page: 1 })
  load()
}

const openDetail = async (id) => {
  detail.value = null
  detailVisible.value = true
  detailLoading.value = true
  try {
    const res = await getNoticeDetail(id)
    detail.value = res.data
  } finally {
    detailLoading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.page-container {
  max-width: 1000px;
  margin: 0 auto;
}

.notice-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.notice-item {
  border: 1px solid #ebeef5;
  border-radius: 6px;
  padding: 12px 16px;
  cursor: pointer;
  transition: border-color 0.2s, box-shadow 0.2s;
}

.notice-item:hover {
  border-color: #c6e2ff;
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.12);
}

.notice-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 6px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.top-tag {
  flex: none;
}

.notice-meta {
  display: flex;
  gap: 14px;
}

.pager {
  margin-top: 16px;
  justify-content: flex-end;
}

.muted {
  color: #909399;
  font-size: 12px;
}

.detail-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.detail-content {
  white-space: pre-wrap;
  line-height: 1.8;
  color: #303133;
}
</style>
