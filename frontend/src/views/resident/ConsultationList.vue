<template>
  <div class="page-container">
    <PageHeader title="我的在线咨询" description="向社区医生发起健康咨询，查看处理进展与回复内容。">
      <el-button :icon="Refresh" @click="load">刷新</el-button>
      <el-button type="primary" :icon="Plus" @click="openCreate">发起咨询</el-button>
    </PageHeader>

    <el-card>
      <el-alert
        type="warning"
        :closable="false"
        show-icon
        class="notice"
        title="在线咨询为健康管理服务，非急诊通道。如出现胸痛、呼吸困难、意识模糊等严重或紧急症状，请立即拨打 120 或前往医院，不要等待在线回复。"
      />

      <FilterBar>
        <el-input v-model="query.keyword" placeholder="标题 / 描述关键词" clearable style="width: 220px" @keyup.enter="handleSearch" />
        <el-select v-model="query.status" placeholder="咨询状态" clearable style="width: 140px">
          <el-option v-for="s in CONSULTATION_STATUSES" :key="s.value" :label="s.label" :value="s.value" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
        <el-button :icon="RefreshLeft" @click="handleReset">重置</el-button>
      </FilterBar>

      <el-table v-loading="loading" :data="tableData" style="width: 100%" empty-text="暂无咨询记录">
        <el-table-column prop="title" label="咨询标题" min-width="180" show-overflow-tooltip />
        <el-table-column prop="categoryLabel" label="分类" width="130" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="consultationStatusTag(row.status)" size="small">{{ row.statusLabel }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="doctorName" label="负责医生" width="110">
          <template #default="{ row }">{{ row.doctorName || '待受理' }}</template>
        </el-table-column>
        <el-table-column label="消息" width="70" align="center">
          <template #default="{ row }">{{ row.messageCount ?? 0 }}</template>
        </el-table-column>
        <el-table-column prop="createTime" label="发起时间" width="170" />
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">查看/回复</el-button>
            <el-button v-if="row.canCancel" link type="danger" @click="handleCancel(row)">取消</el-button>
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

    <!-- 发起咨询 -->
    <el-dialog v-model="createVisible" title="发起健康咨询" width="620px">
      <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-width="90px">
        <el-form-item label="咨询标题" prop="title">
          <el-input v-model="createForm.title" maxlength="100" show-word-limit placeholder="一句话描述您的咨询主题" />
        </el-form-item>
        <el-form-item label="咨询分类" prop="category">
          <el-select v-model="createForm.category" placeholder="请选择分类" style="width: 220px">
            <el-option v-for="c in CONSULTATION_CATEGORIES" :key="c.value" :label="c.label" :value="c.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="问题描述" prop="description">
          <el-input v-model="createForm.description" type="textarea" :rows="5" maxlength="2000" show-word-limit
            placeholder="请尽量描述清楚症状、时间、已采取的措施等，便于医生了解情况" />
        </el-form-item>
      </el-form>
      <el-alert
        type="info"
        :closable="false"
        show-icon
        title="医生回复仅提供健康管理建议，不构成正式医疗诊断。"
      />
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitCreate">提交咨询</el-button>
      </template>
    </el-dialog>

    <!-- 咨询详情 -->
    <el-dialog v-model="detailVisible" title="咨询详情" width="720px" v-loading="detailLoading">
      <template v-if="detail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="标题" :span="2">{{ detail.consultation.title }}</el-descriptions-item>
          <el-descriptions-item label="分类">{{ detail.consultation.categoryLabel }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="consultationStatusTag(detail.consultation.status)" size="small">
              {{ detail.consultation.statusLabel }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="负责医生">{{ detail.consultation.doctorName || '待受理' }}</el-descriptions-item>
          <el-descriptions-item label="发起时间">{{ detail.consultation.createTime }}</el-descriptions-item>
          <el-descriptions-item label="问题描述" :span="2" class="prewrap">{{ detail.consultation.description }}</el-descriptions-item>
        </el-descriptions>

        <el-divider content-position="left">对话记录</el-divider>
        <el-empty v-if="!messages.length" description="暂无回复" :image-size="60" />
        <div v-else class="chat">
          <div v-for="m in messages" :key="m.id" class="chat-item" :class="{ mine: m.senderRole === 'RESIDENT' }">
            <div class="chat-meta">
              <el-tag :type="m.senderRole === 'DOCTOR' ? 'primary' : 'success'" size="small">
                {{ m.senderRoleLabel }}·{{ m.senderName || '-' }}
              </el-tag>
              <span class="muted">{{ m.createTime }}</span>
            </div>
            <div class="chat-content">{{ m.content }}</div>
          </div>
        </div>

        <template v-if="detail.consultation.residentCanMessage">
          <el-divider content-position="left">补充说明</el-divider>
          <el-input v-model="messageContent" type="textarea" :rows="3" maxlength="1000" show-word-limit
            placeholder="补充您的症状变化或回答医生的问题" />
          <div class="send-bar">
            <el-button type="primary" :loading="sending" @click="submitMessage">发送</el-button>
          </div>
        </template>
        <el-alert v-else type="info" :closable="false" show-icon class="notice"
          title="该咨询已结束，如需继续咨询请重新发起。" />
      </template>
      <template #footer>
        <el-button v-if="detail?.consultation?.canCancel" type="danger" plain @click="handleCancel(detail.consultation)">
          取消咨询
        </el-button>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, RefreshLeft, Search, Plus } from '@element-plus/icons-vue'
import {
  createConsultation,
  getMyConsultations,
  getMyConsultationDetail,
  getMyConsultationMessages,
  sendConsultationMessage,
  cancelConsultation
} from '@/api/health'
import { CONSULTATION_CATEGORIES, CONSULTATION_STATUSES, consultationStatusTag } from '@/utils/labels'
import PageHeader from '@/components/PageHeader.vue'
import FilterBar from '@/components/FilterBar.vue'

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const query = reactive({ page: 1, size: 10, keyword: '', status: '' })

const createVisible = ref(false)
const submitting = ref(false)
const createFormRef = ref(null)
const createForm = reactive({ title: '', category: '', description: '' })
const createRules = {
  title: [{ required: true, message: '请输入咨询标题', trigger: 'blur' }],
  category: [{ required: true, message: '请选择咨询分类', trigger: 'change' }],
  description: [{ required: true, message: '请填写问题描述', trigger: 'blur' }]
}

const detailVisible = ref(false)
const detailLoading = ref(false)
const detail = ref(null)
const messages = ref([])
const messageContent = ref('')
const sending = ref(false)

const load = async () => {
  loading.value = true
  try {
    const res = await getMyConsultations({
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
  Object.assign(createForm, { title: '', category: '', description: '' })
  createVisible.value = true
  createFormRef.value?.clearValidate()
}

const submitCreate = async () => {
  const valid = await createFormRef.value.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    await createConsultation({
      title: createForm.title,
      category: createForm.category,
      description: createForm.description
    })
    ElMessage.success('咨询已提交，请等待医生受理')
    createVisible.value = false
    query.page = 1
    await load()
  } finally {
    submitting.value = false
  }
}

const loadMessages = async (id) => {
  const res = await getMyConsultationMessages(id)
  messages.value = res.data || []
}

const openDetail = async (row) => {
  detail.value = null
  messages.value = []
  messageContent.value = ''
  detailVisible.value = true
  detailLoading.value = true
  try {
    const res = await getMyConsultationDetail(row.id)
    detail.value = res.data
    await loadMessages(row.id)
  } finally {
    detailLoading.value = false
  }
}

const submitMessage = async () => {
  if (!messageContent.value.trim()) {
    ElMessage.warning('请输入补充内容')
    return
  }
  sending.value = true
  try {
    await sendConsultationMessage(detail.value.consultation.id, { content: messageContent.value })
    ElMessage.success('已发送')
    messageContent.value = ''
    await loadMessages(detail.value.consultation.id)
  } finally {
    sending.value = false
  }
}

const handleCancel = async (row) => {
  try {
    await ElMessageBox.confirm('确定取消该咨询吗？取消后不可恢复。', '取消咨询', { type: 'warning' })
  } catch (e) {
    return
  }
  await cancelConsultation(row.id)
  ElMessage.success('咨询已取消')
  detailVisible.value = false
  await load()
}

onMounted(load)
</script>

<style scoped>
.page-container {
  max-width: 1200px;
  margin: 0 auto;
}

.notice {
  margin-bottom: 14px;
}

.pager {
  margin-top: 16px;
  justify-content: flex-end;
}

.muted {
  color: #909399;
  font-size: 12px;
}

.prewrap {
  white-space: pre-wrap;
}

.chat-item {
  margin-bottom: 14px;
}

.chat-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.chat-content {
  background: #f4f6f9;
  border-radius: 6px;
  padding: 8px 12px;
  white-space: pre-wrap;
  color: #303133;
}

.chat-item.mine .chat-content {
  background: #eef6ff;
}

.send-bar {
  margin-top: 10px;
  text-align: right;
}
</style>
