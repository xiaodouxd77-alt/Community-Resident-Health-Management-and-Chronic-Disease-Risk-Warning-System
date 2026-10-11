<template>
  <div class="page-container">
    <PageHeader title="社区公告管理" description="发布与维护社区公告，支持有效期设置与置顶。">
      <el-button :icon="Refresh" @click="load">刷新</el-button>
      <el-button type="primary" :icon="Plus" @click="openCreate">新建公告</el-button>
    </PageHeader>

    <el-card>
      <FilterBar>
        <el-input v-model="query.keyword" placeholder="标题关键词" clearable style="width: 200px" @keyup.enter="handleSearch" />
        <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 130px">
          <el-option v-for="s in PUBLISH_STATUSES" :key="s.value" :label="s.label" :value="s.value" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
        <el-button :icon="RefreshLeft" @click="handleReset">重置</el-button>
      </FilterBar>

      <el-table v-loading="loading" :data="tableData" style="width: 100%" empty-text="暂无公告">
        <el-table-column label="标题" min-width="220" show-overflow-tooltip>
          <template #default="{ row }">
            <el-tag v-if="row.isTop" type="danger" size="small" effect="dark" class="mr6">置顶</el-tag>
            {{ row.title }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="publishStatusTag(row.publishStatus)" size="small">{{ row.publishStatusLabel }}</el-tag>
            <el-tag v-if="row.publishStatus === 'PUBLISHED' && row.expired" type="info" size="small" class="ml6">已过期</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="publisherName" label="发布人" width="110">
          <template #default="{ row }">{{ row.publisherName || '-' }}</template>
        </el-table-column>
        <el-table-column label="有效期" width="330">
          <template #default="{ row }">
            {{ row.effectiveFrom || '不限' }} ~ {{ row.effectiveUntil || '不限' }}
          </template>
        </el-table-column>
        <el-table-column prop="publishedAt" label="发布时间" width="170">
          <template #default="{ row }">{{ row.publishedAt || '-' }}</template>
        </el-table-column>
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

    <el-dialog v-model="formVisible" :title="form.id ? '编辑公告' : '新建公告'" width="700px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="公告标题" prop="title">
          <el-input v-model="form.title" maxlength="100" show-word-limit placeholder="请输入公告标题" />
        </el-form-item>
        <el-form-item label="正文" prop="content">
          <el-input v-model="form.content" type="textarea" :rows="8" maxlength="5000" show-word-limit
            placeholder="公告正文，支持换行" />
        </el-form-item>
        <el-form-item label="生效开始">
          <el-date-picker v-model="form.effectiveFrom" type="datetime" value-format="YYYY-MM-DD HH:mm:ss"
            placeholder="不填表示立即生效" style="width: 240px" />
        </el-form-item>
        <el-form-item label="生效结束">
          <el-date-picker v-model="form.effectiveUntil" type="datetime" value-format="YYYY-MM-DD HH:mm:ss"
            placeholder="不填表示长期有效" style="width: 240px" />
        </el-form-item>
        <el-form-item label="置顶">
          <el-switch v-model="form.isTop" />
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
  getAdminNotices,
  createNotice,
  updateNotice,
  publishNotice,
  offlineNotice
} from '@/api/admin'
import { PUBLISH_STATUSES, publishStatusTag } from '@/utils/labels'
import PageHeader from '@/components/PageHeader.vue'
import FilterBar from '@/components/FilterBar.vue'

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const query = reactive({ page: 1, size: 10, keyword: '', status: '' })

const formVisible = ref(false)
const saving = ref(false)
const formRef = ref(null)
const form = reactive({ id: null, title: '', content: '', effectiveFrom: null, effectiveUntil: null, isTop: false })
const rules = {
  title: [{ required: true, message: '请输入公告标题', trigger: 'blur' }],
  content: [{ required: true, message: '请填写公告正文', trigger: 'blur' }]
}

const load = async () => {
  loading.value = true
  try {
    const res = await getAdminNotices({
      page: query.page,
      size: query.size,
      keyword: query.keyword || undefined,
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
  Object.assign(query, { keyword: '', status: '', page: 1 })
  load()
}

const openCreate = () => {
  Object.assign(form, { id: null, title: '', content: '', effectiveFrom: null, effectiveUntil: null, isTop: false })
  formVisible.value = true
  formRef.value?.clearValidate()
}

const openEdit = (row) => {
  Object.assign(form, {
    id: row.id,
    title: row.title,
    content: row.content || '',
    effectiveFrom: row.effectiveFrom || null,
    effectiveUntil: row.effectiveUntil || null,
    isTop: !!row.isTop
  })
  formVisible.value = true
  formRef.value?.clearValidate()
}

const submitForm = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  if (form.effectiveFrom && form.effectiveUntil && form.effectiveFrom > form.effectiveUntil) {
    ElMessage.error('生效开始时间不能晚于结束时间')
    return
  }
  saving.value = true
  try {
    const payload = {
      title: form.title,
      content: form.content,
      effectiveFrom: form.effectiveFrom || undefined,
      effectiveUntil: form.effectiveUntil || undefined,
      isTop: form.isTop
    }
    if (form.id) {
      await updateNotice(form.id, payload)
      ElMessage.success('已保存')
    } else {
      await createNotice(payload)
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
    await ElMessageBox.confirm('确认发布该公告？发布后居民在有效期内可见。', '发布公告', { type: 'warning' })
  } catch (e) {
    return
  }
  await publishNotice(row.id)
  ElMessage.success('已发布')
  await load()
}

const handleOffline = async (row) => {
  try {
    await ElMessageBox.confirm('确认下架该公告？下架后居民将无法查看。', '下架公告', { type: 'warning' })
  } catch (e) {
    return
  }
  await offlineNotice(row.id)
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

.mr6 {
  margin-right: 6px;
}

.ml6 {
  margin-left: 6px;
}
</style>
