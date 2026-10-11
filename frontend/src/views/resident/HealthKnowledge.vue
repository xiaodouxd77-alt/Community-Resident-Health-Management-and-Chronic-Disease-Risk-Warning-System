<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>健康知识</span>
          <el-button :icon="Refresh" size="small" @click="load">刷新</el-button>
        </div>
      </template>

      <div class="filters">
        <el-input v-model="query.keyword" placeholder="搜索标题" clearable style="width: 220px" @keyup.enter="handleSearch" />
        <el-select v-model="query.category" placeholder="全部分类" clearable style="width: 150px">
          <el-option v-for="c in ARTICLE_CATEGORIES" :key="c.value" :label="c.label" :value="c.value" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
        <el-button :icon="RefreshLeft" @click="handleReset">重置</el-button>
      </div>

      <el-empty v-if="!loading && !articles.length" description="暂无已发布的健康知识" />
      <div v-else v-loading="loading" class="article-list">
        <div v-for="a in articles" :key="a.id" class="article-card" @click="openDetail(a.id)">
          <div class="article-title">{{ a.title }}</div>
          <div class="article-summary">{{ a.summary || '（暂无摘要）' }}</div>
          <div class="article-meta">
            <el-tag size="small" effect="plain">{{ a.categoryLabel }}</el-tag>
            <span class="muted">{{ a.publishedAt || a.createTime }}</span>
          </div>
        </div>
      </div>

      <el-pagination
        v-model:current-page="query.page"
        v-model:page-size="query.size"
        :total="total"
        :page-sizes="[6, 12, 24]"
        layout="total, sizes, prev, pager, next"
        class="pager"
        @size-change="load"
        @current-change="load"
      />
    </el-card>

    <el-dialog v-model="detailVisible" :title="detail?.title || '文章详情'" width="720px" v-loading="detailLoading">
      <template v-if="detail">
        <div class="detail-meta">
          <el-tag size="small" effect="plain">{{ detail.categoryLabel }}</el-tag>
          <span class="muted">发布于 {{ detail.publishedAt || '-' }}</span>
          <span class="muted">作者 {{ detail.authorName || '-' }}</span>
        </div>
        <p v-if="detail.summary" class="detail-summary">{{ detail.summary }}</p>
        <div class="detail-content">{{ detail.content }}</div>
        <el-alert
          type="info"
          :closable="false"
          show-icon
          class="notice"
          title="科普内容仅供参考，不能替代专业医疗建议。如有健康问题请咨询医生。"
        />
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
import { getHealthArticles, getHealthArticleDetail } from '@/api/health'
import { ARTICLE_CATEGORIES } from '@/utils/labels'

const loading = ref(false)
const articles = ref([])
const total = ref(0)
const query = reactive({ page: 1, size: 6, keyword: '', category: '' })

const detailVisible = ref(false)
const detailLoading = ref(false)
const detail = ref(null)

const load = async () => {
  loading.value = true
  try {
    const res = await getHealthArticles({
      page: query.page,
      size: query.size,
      keyword: query.keyword || undefined,
      category: query.category || undefined
    })
    articles.value = res.data.records || []
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
  Object.assign(query, { keyword: '', category: '', page: 1 })
  load()
}

const openDetail = async (id) => {
  detail.value = null
  detailVisible.value = true
  detailLoading.value = true
  try {
    const res = await getHealthArticleDetail(id)
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

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.filters {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  align-items: center;
  margin-bottom: 16px;
}

.article-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.article-card {
  border: 1px solid #ebeef5;
  border-radius: 6px;
  padding: 14px 16px;
  cursor: pointer;
  transition: box-shadow 0.2s, border-color 0.2s;
}

.article-card:hover {
  border-color: #c6e2ff;
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.12);
}

.article-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 6px;
}

.article-summary {
  color: #606266;
  font-size: 13px;
  margin-bottom: 8px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.article-meta {
  display: flex;
  align-items: center;
  gap: 12px;
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

.detail-summary {
  color: #606266;
  background: #f4f6f9;
  border-radius: 6px;
  padding: 8px 12px;
}

.detail-content {
  white-space: pre-wrap;
  line-height: 1.8;
  color: #303133;
}

.notice {
  margin-top: 14px;
}
</style>
