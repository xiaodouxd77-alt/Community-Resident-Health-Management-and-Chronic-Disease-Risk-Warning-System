<template>
  <div class="page-container">
    <PageHeader title="健康知识管理" description="维护健康科普文章，支持草稿、发布与下架。">
      <el-button :icon="Refresh" @click="load">刷新</el-button>
      <el-button type="primary" :icon="Plus" @click="openCreate">新建文章</el-button>
    </PageHeader>

    <el-card>
      <FilterBar>
        <el-input v-model="query.keyword" placeholder="标题关键词" clearable style="width: 200px" @keyup.enter="handleSearch" />
        <el-select v-model="query.category" placeholder="全部分类" clearable style="width: 150px">
          <el-option v-for="c in ARTICLE_CATEGORIES" :key="c.value" :label="c.label" :value="c.value" />
        </el-select>
        <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 130px">
          <el-option v-for="s in PUBLISH_STATUSES" :key="s.value" :label="s.label" :value="s.value" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
        <el-button :icon="RefreshLeft" @click="handleReset">重置</el-button>
      </FilterBar>

      <el-table v-loading="loading" :data="tableData" style="width: 100%" empty-text="暂无文章">
        <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
        <el-table-column prop="categoryLabel" label="分类" width="120" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="publishStatusTag(row.publishStatus)" size="small">{{ row.publishStatusLabel }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="authorName" label="作者" width="110">
          <template #default="{ row }">{{ row.authorName || '-' }}</template>
        </el-table-column>
        <el-table-column prop="publishedAt" label="发布时间" width="170">
          <template #default="{ row }">{{ row.publishedAt || '-' }}</template>
        </el-table-column>
        <el-table-column prop="updateTime" label="更新时间" width="170" />
        <el-table-column label="操作" width="230" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button v-if="row.publishStatus !== 'PUBLISHED'" link type="success" @click="handlePublish(row)">发布</el-button>
            <el-button v-if="row.publishStatus === 'PUBLISHED'" link type="warning" @click="handleOffline(row)">下架</el-button>
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

    <el-dialog v-model="formVisible" :title="form.id ? '编辑文章' : '新建文章'" width="760px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" maxlength="100" show-word-limit placeholder="请输入文章标题" />
        </el-form-item>
        <el-form-item label="分类" prop="category">
          <el-select v-model="form.category" placeholder="请选择分类" style="width: 220px">
            <el-option v-for="c in ARTICLE_CATEGORIES" :key="c.value" :label="c.label" :value="c.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="摘要">
          <el-input v-model="form.summary" type="textarea" :rows="2" maxlength="500" show-word-limit placeholder="选填，用于列表展示" />
        </el-form-item>
        <el-form-item label="封面地址">
          <el-input v-model="form.coverUrl" maxlength="255" placeholder="选填，图片链接" />
        </el-form-item>
        <el-form-item label="正文" prop="content">
          <el-input v-model="form.content" type="textarea" :rows="10" maxlength="20000" show-word-limit
            placeholder="正文以纯文本保存与展示，支持换行" />
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
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, RefreshLeft, Search, Plus } from '@element-plus/icons-vue'
import {
  getAdminArticles,
  createArticle,
  updateArticle,
  publishArticle,
  offlineArticle
} from '@/api/admin'
import { ARTICLE_CATEGORIES, PUBLISH_STATUSES, publishStatusTag } from '@/utils/labels'
import PageHeader from '@/components/PageHeader.vue'
import FilterBar from '@/components/FilterBar.vue'

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const query = reactive({ page: 1, size: 10, keyword: '', category: '', status: '' })

const formVisible = ref(false)
const saving = ref(false)
const formRef = ref(null)
const form = reactive({ id: null, title: '', category: '', summary: '', coverUrl: '', content: '' })
const rules = {
  title: [{ required: true, message: '请输入文章标题', trigger: 'blur' }],
  category: [{ required: true, message: '请选择分类', trigger: 'change' }],
  content: [{ required: true, message: '请填写正文', trigger: 'blur' }]
}

const load = async () => {
  loading.value = true
  try {
    const res = await getAdminArticles({
      page: query.page,
      size: query.size,
      keyword: query.keyword || undefined,
      category: query.category || undefined,
      status: query.status || undefined
    })
    tableData.value = res.data.records || []
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
  Object.assign(query, { keyword: '', category: '', status: '', page: 1 })
  load()
}

const openCreate = () => {
  Object.assign(form, { id: null, title: '', category: '', summary: '', coverUrl: '', content: '' })
  formVisible.value = true
  formRef.value?.clearValidate()
}

const openEdit = (row) => {
  Object.assign(form, {
    id: row.id,
    title: row.title,
    category: row.category,
    summary: row.summary || '',
    coverUrl: row.coverUrl || '',
    content: row.content || ''
  })
  formVisible.value = true
  formRef.value?.clearValidate()
}

const submitForm = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    const payload = {
      title: form.title,
      category: form.category,
      summary: form.summary || undefined,
      coverUrl: form.coverUrl || undefined,
      content: form.content
    }
    if (form.id) {
      await updateArticle(form.id, payload)
      ElMessage.success('已保存')
    } else {
      await createArticle(payload)
      ElMessage.success('已创建草稿')
    }
    formVisible.value = false
    await load()
  } finally {
    saving.value = false
  }
}

const handlePublish = async (row) => {
  try {
    await ElMessageBox.confirm('确认发布该文章？发布后居民可见。', '发布文章', { type: 'warning' })
  } catch (e) {
    return
  }
  await publishArticle(row.id)
  ElMessage.success('已发布')
  await load()
}

const handleOffline = async (row) => {
  try {
    await ElMessageBox.confirm('确认下架该文章？下架后居民将无法查看。', '下架文章', { type: 'warning' })
  } catch (e) {
    return
  }
  await offlineArticle(row.id)
  ElMessage.success('已下架')
  await load()
}

onMounted(load)
</script>

<style scoped>
.page-container {
  max-width: 1240px;
  margin: 0 auto;
}

.pager {
  margin-top: 16px;
  justify-content: flex-end;
}
</style>
