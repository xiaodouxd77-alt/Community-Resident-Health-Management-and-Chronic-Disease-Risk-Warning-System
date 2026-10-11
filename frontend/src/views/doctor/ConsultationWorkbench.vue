<template>
  <div class="page-container">
    <PageHeader title="在线咨询工作台" description="受理居民健康咨询并回复健康管理建议。">
      <el-button :icon="Refresh" @click="load">刷新</el-button>
    </PageHeader>

    <el-card>
      <el-alert
        type="info"
        :closable="false"
        show-icon
        class="notice"
        title="仅可处理您管理范围内居民的咨询。回复内容为健康管理建议，不构成正式诊断；请勿通过在线咨询处理急症。"
      />

      <el-tabs v-model="activeStatus" @tab-change="handleTabChange">
        <el-tab-pane label="待受理" name="PENDING" />
        <el-tab-pane label="处理中" name="PROCESSING" />
        <el-tab-pane label="已解决" name="RESOLVED" />
        <el-tab-pane label="全部" name="" />
      </el-tabs>

      <FilterBar>
        <el-input v-model="query.keyword" placeholder="标题 / 描述关键词" clearable style="width: 240px" @keyup.enter="handleSearch" />
        <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
      </FilterBar>

      <el-table v-loading="loading" :data="tableData" style="width: 100%" empty-text="暂无咨询">
        <el-table-column label="居民" width="150">
          <template #default="{ row }">
            <div>{{ row.residentName || '-' }}</div>
            <div class="muted">{{ row.residentNo }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="咨询标题" min-width="180" show-overflow-tooltip />
        <el-table-column prop="categoryLabel" label="分类" width="130" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="consultationStatusTag(row.status)" size="small">{{ row.statusLabel }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="消息" width="70" align="center">
          <template #default="{ row }">{{ row.messageCount ?? 0 }}</template>
        </el-table-column>
        <el-table-column prop="createTime" label="发起时间" width="170" />
        <el-table-column label="操作" width="170" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 'PENDING'" link type="primary" @click="handleAccept(row)">受理</el-button>
            <el-button link type="primary" @click="openDetail(row)">查看</el-button>
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

    <el-dialog v-model="detailVisible" title="咨询详情" width="720px" v-loading="detailLoading">
      <template v-if="detail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="居民">{{ detail.consultation.residentName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="居民编号">{{ detail.consultation.residentNo || '-' }}</el-descriptions-item>
          <el-descriptions-item label="标题" :span="2">{{ detail.consultation.title }}</el-descriptions-item>
          <el-descriptions-item label="分类">{{ detail.consultation.categoryLabel }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="consultationStatusTag(detail.consultation.status)" size="small">
              {{ detail.consultation.statusLabel }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="发起时间">{{ detail.consultation.createTime }}</el-descriptions-item>
          <el-descriptions-item label="受理时间">{{ detail.consultation.acceptedAt || '-' }}</el-descriptions-item>
          <el-descriptions-item label="问题描述" :span="2" class="prewrap">{{ detail.consultation.description }}</el-descriptions-item>
        </el-descriptions>

        <el-divider content-position="left">对话记录</el-divider>
        <el-empty v-if="!messages.length" description="暂无消息" :image-size="60" />
        <div v-else class="chat">
          <div v-for="m in messages" :key="m.id" class="chat-item" :class="{ mine: m.senderRole === 'DOCTOR' }">
            <div class="chat-meta">
              <el-tag :type="m.senderRole === 'DOCTOR' ? 'primary' : 'success'" size="small">
                {{ m.senderRoleLabel }}·{{ m.senderName || '-' }}
              </el-tag>
              <span class="muted">{{ m.createTime }}</span>
            </div>
            <div class="chat-content">{{ m.content }}</div>
          </div>
        </div>

        <template v-if="canReply">
          <el-divider content-position="left">回复居民</el-divider>
          <el-input v-model="replyContent" type="textarea" :rows="3" maxlength="1000" show-word-limit
            placeholder="请输入回复内容（建议仅作健康管理指导）" />
          <div class="send-bar">
            <el-button type="primary" :loading="replying" @click="submitReply">发送回复</el-button>
          </div>
        </template>

        <el-alert
          v-if="isMyProcessing"
          type="info"
          :closable="false"
          show-icon
          class="notice"
          title="处理完成后可将咨询标记为已解决；如无需继续跟踪可关闭。"
        />
      </template>
      <template #footer>
        <el-button v-if="detail?.consultation?.status === 'PENDING'" type="primary" @click="handleAccept(detail.consultation, true)">
          受理该咨询
        </el-button>
        <el-button v-if="isMyProcessing" type="success" plain @click="handleResolve">标记已解决</el-button>
        <el-button v-if="isMyProcessing" type="info" plain @click="handleClose">关闭咨询</el-button>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useUserStore } from '@/stores/user'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, Search } from '@element-plus/icons-vue'
import {
  getDoctorConsultations,
  getDoctorConsultationDetail,
  getDoctorConsultationMessages,
  acceptConsultation,
  replyConsultation,
  resolveConsultation,
  closeConsultation
} from '@/api/doctor'
import { consultationStatusTag } from '@/utils/labels'
import PageHeader from '@/components/PageHeader.vue'
import FilterBar from '@/components/FilterBar.vue'

const userStore = useUserStore()

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const activeStatus = ref('PENDING')
const query = reactive({ page: 1, size: 10, keyword: '', status: 'PENDING' })

const detailVisible = ref(false)
const detailLoading = ref(false)
const detail = ref(null)
const messages = ref([])
const replyContent = ref('')
const replying = ref(false)

const myDoctorId = computed(() => userStore.userInfo?.id)
const isMyProcessing = computed(() => {
  const c = detail.value?.consultation
  return c && c.status === 'PROCESSING' && c.doctorId === myDoctorId.value
})
const canReply = computed(() => isMyProcessing.value)

const load = async () => {
  loading.value = true
  try {
    const res = await getDoctorConsultations({
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

const handleTabChange = () => {
  query.status = activeStatus.value
  query.page = 1
  load()
}

const handleSearch = () => {
  query.page = 1
  load()
}

const handleAccept = async (row, fromDialog = false) => {
  try {
    await ElMessageBox.confirm('确认受理该咨询？受理后由您负责回复。', '受理咨询', { type: 'warning' })
  } catch (e) {
    return
  }
  await acceptConsultation(row.id)
  ElMessage.success('已受理')
  if (fromDialog) {
    await openDetail(row)
  }
  await load()
}

const loadMessages = async (id) => {
  const res = await getDoctorConsultationMessages(id)
  messages.value = res.data || []
}

const openDetail = async (row) => {
  detail.value = null
  messages.value = []
  replyContent.value = ''
  detailVisible.value = true
  detailLoading.value = true
  try {
    const res = await getDoctorConsultationDetail(row.id)
    detail.value = res.data
    await loadMessages(row.id)
  } finally {
    detailLoading.value = false
  }
}

const submitReply = async () => {
  if (!replyContent.value.trim()) {
    ElMessage.warning('请输入回复内容')
    return
  }
  replying.value = true
  try {
    await replyConsultation(detail.value.consultation.id, { content: replyContent.value })
    ElMessage.success('回复已发送')
    replyContent.value = ''
    await loadMessages(detail.value.consultation.id)
    await load()
  } finally {
    replying.value = false
  }
}

const handleResolve = async () => {
  try {
    await ElMessageBox.confirm('将该咨询标记为已解决？', '标记已解决', { type: 'warning' })
  } catch (e) {
    return
  }
  const res = await resolveConsultation(detail.value.consultation.id)
  detail.value.consultation = res.data
  ElMessage.success('已标记为已解决')
  await load()
}

const handleClose = async () => {
  try {
    await ElMessageBox.confirm('关闭该咨询？关闭后不可继续回复。', '关闭咨询', { type: 'warning' })
  } catch (e) {
    return
  }
  const res = await closeConsultation(detail.value.consultation.id)
  detail.value.consultation = res.data
  ElMessage.success('咨询已关闭')
  await load()
}

onMounted(load)
</script>

<style scoped>
.page-container {
  max-width: 1240px;
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
