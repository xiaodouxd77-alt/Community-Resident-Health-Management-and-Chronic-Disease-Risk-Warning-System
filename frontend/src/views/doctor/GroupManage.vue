<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>居民分组管理</span>
          <el-button type="primary" :icon="Plus" @click="openCreate">新建分组</el-button>
        </div>
      </template>

      <div class="filters">
        <el-input v-model="query.keyword" placeholder="分组名称 / 编码" clearable style="width: 200px" @keyup.enter="handleSearch" />
        <el-select v-model="query.communityId" placeholder="全部社区" clearable style="width: 170px">
          <el-option v-for="c in communities" :key="c.id" :label="c.communityName" :value="c.id" />
        </el-select>
        <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 130px">
          <el-option v-for="s in GROUP_STATUSES" :key="s.value" :label="s.label" :value="s.value" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
        <el-button :icon="Refresh" @click="handleReset">重置</el-button>
      </div>

      <el-table v-loading="loading" :data="tableData" style="width: 100%" empty-text="暂无分组">
        <el-table-column prop="groupName" label="分组名称" width="180" />
        <el-table-column prop="groupCode" label="分组编码" width="180" />
        <el-table-column prop="description" label="分组说明" min-width="200" show-overflow-tooltip />
        <el-table-column prop="communityName" label="所属社区" width="130">
          <template #default="{ row }">{{ row.communityName || '-' }}</template>
        </el-table-column>
        <el-table-column prop="managerDoctorName" label="负责医生" width="110" />
        <el-table-column prop="memberCount" label="成员数" width="90" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="groupStatusTag(row.status)" size="small">{{ row.statusLabel }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">成员</el-button>
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
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

    <!-- 新建 / 编辑 -->
    <el-dialog v-model="formVisible" :title="formMode === 'create' ? '新建分组' : '编辑分组'" width="520px">
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="90px">
        <el-form-item label="分组名称" prop="groupName">
          <el-input v-model="form.groupName" maxlength="100" placeholder="如：高血压管理组" />
        </el-form-item>
        <el-form-item v-if="formMode === 'create'" label="分组编码" prop="groupCode">
          <el-input v-model="form.groupCode" maxlength="32" placeholder="选填，留空自动生成" />
        </el-form-item>
        <el-form-item label="分组说明" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="2" maxlength="255" show-word-limit placeholder="选填" />
        </el-form-item>
        <el-form-item label="所属社区" prop="communityId">
          <el-select v-model="form.communityId" clearable placeholder="选填" style="width: 100%">
            <el-option v-for="c in communities" :key="c.id" :label="c.communityName" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="formMode === 'edit'" label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio value="ACTIVE">启用</el-radio>
            <el-radio value="DISABLED">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>

    <!-- 分组详情 / 成员 -->
    <el-dialog v-model="detailVisible" :title="group ? `分组成员 - ${group.groupName}` : '分组成员'" width="760px">
      <template v-if="group">
        <el-descriptions :column="3" border size="small" class="mb12">
          <el-descriptions-item label="分组编码">{{ group.groupCode }}</el-descriptions-item>
          <el-descriptions-item label="所属社区">{{ group.communityName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="负责医生">{{ group.managerDoctorName || '-' }}</el-descriptions-item>
        </el-descriptions>

        <div class="member-toolbar">
          <el-input v-model="memberKeyword" placeholder="搜索成员姓名 / 编号" clearable style="width: 220px" @keyup.enter="loadMembers" />
          <el-button :icon="Search" @click="loadMembers">搜索</el-button>
          <div class="spacer" />
          <el-button type="primary" :icon="Plus" @click="openAddMembers">添加居民</el-button>
        </div>

        <el-table v-loading="memberLoading" :data="members" size="small" empty-text="暂无成员">
          <el-table-column prop="residentName" label="姓名" width="100" />
          <el-table-column prop="residentNo" label="居民编号" width="150" />
          <el-table-column prop="genderLabel" label="性别" width="70" />
          <el-table-column prop="communityName" label="社区" width="120">
            <template #default="{ row }">{{ row.communityName || '-' }}</template>
          </el-table-column>
          <el-table-column prop="addedAt" label="加入时间" width="170" />
          <el-table-column label="操作" width="90">
            <template #default="{ row }">
              <el-button link type="danger" @click="removeMember(row)">移除</el-button>
            </template>
          </el-table-column>
        </el-table>

        <el-pagination
          v-model:current-page="memberQuery.page"
          :page-size="memberQuery.size"
          :total="memberTotal"
          layout="total, prev, pager, next"
          class="pager"
          @current-change="loadMembers"
        />
      </template>
    </el-dialog>

    <!-- 添加居民 -->
    <el-dialog v-model="addVisible" title="添加居民到分组" width="520px">
      <el-select
        v-model="selectedResidentIds"
        multiple
        filterable
        collapse-tags
        collapse-tags-tooltip
        placeholder="选择居民（可多选）"
        style="width: 100%"
      >
        <el-option
          v-for="r in candidates"
          :key="r.id"
          :label="`${r.realName || r.residentNo}（${r.residentNo}）`"
          :value="r.id"
        />
      </el-select>
      <p class="muted tip">仅可选择您管理范围内、且与分组社区一致的居民。</p>
      <template #footer>
        <el-button @click="addVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" :disabled="!selectedResidentIds.length" @click="submitAddMembers">
          添加
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search, Refresh } from '@element-plus/icons-vue'
import {
  getGroupList,
  createGroup,
  updateGroup,
  getGroupMembers,
  addGroupMembers,
  removeGroupMember,
  getMyResidents,
  getResidentsPage,
  getCommunities
} from '@/api/doctor'
import { GROUP_STATUSES, groupStatusTag } from '@/utils/labels'

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const communities = ref([])
const query = reactive({ page: 1, size: 10, keyword: '', communityId: '', status: '' })

const submitting = ref(false)
const formVisible = ref(false)
const formMode = ref('create')
const editId = ref(null)
const formRef = ref(null)
const form = reactive({ groupName: '', groupCode: '', description: '', communityId: null, status: 'ACTIVE' })
const formRules = {
  groupName: [{ required: true, message: '请输入分组名称', trigger: 'blur' }]
}

const detailVisible = ref(false)
const group = ref(null)
const members = ref([])
const memberTotal = ref(0)
const memberKeyword = ref('')
const memberQuery = reactive({ page: 1, size: 10 })
const memberLoading = ref(false)

const addVisible = ref(false)
const candidates = ref([])
const selectedResidentIds = ref([])

const load = async () => {
  loading.value = true
  try {
    const res = await getGroupList({
      page: query.page,
      size: query.size,
      keyword: query.keyword || undefined,
      communityId: query.communityId || undefined,
      status: query.status || undefined
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
  Object.assign(query, { keyword: '', communityId: '', status: '', page: 1 })
  load()
}

const openCreate = () => {
  formMode.value = 'create'
  Object.assign(form, { groupName: '', groupCode: '', description: '', communityId: null, status: 'ACTIVE' })
  formVisible.value = true
  formRef.value?.clearValidate()
}

const openEdit = (row) => {
  formMode.value = 'edit'
  editId.value = row.id
  Object.assign(form, {
    groupName: row.groupName,
    groupCode: row.groupCode,
    description: row.description || '',
    communityId: row.communityId || null,
    status: row.status
  })
  formVisible.value = true
  formRef.value?.clearValidate()
}

const submitForm = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    if (formMode.value === 'create') {
      await createGroup({
        groupName: form.groupName,
        groupCode: form.groupCode || undefined,
        description: form.description || undefined,
        communityId: form.communityId || undefined
      })
      ElMessage.success('分组已创建')
    } else {
      await updateGroup(editId.value, {
        groupName: form.groupName,
        description: form.description || undefined,
        communityId: form.communityId || undefined,
        status: form.status
      })
      ElMessage.success('分组已更新')
    }
    formVisible.value = false
    load()
  } finally {
    submitting.value = false
  }
}

const loadMembers = async () => {
  memberLoading.value = true
  try {
    const res = await getGroupMembers(group.value.id, {
      page: memberQuery.page,
      size: memberQuery.size,
      keyword: memberKeyword.value || undefined
    })
    members.value = res.data.records || []
    memberTotal.value = res.data.total || 0
  } finally {
    memberLoading.value = false
  }
}

const openDetail = async (row) => {
  group.value = row
  memberKeyword.value = ''
  memberQuery.page = 1
  detailVisible.value = true
  loadMembers()
}

const openAddMembers = async () => {
  if (group.value.communityId) {
    const res = await getResidentsPage({ page: 1, size: 100, communityId: group.value.communityId })
    candidates.value = res.data.records || []
  } else {
    const res = await getMyResidents()
    candidates.value = res.data || []
  }
  selectedResidentIds.value = []
  addVisible.value = true
}

const submitAddMembers = async () => {
  submitting.value = true
  try {
    const res = await addGroupMembers(group.value.id, { residentIds: selectedResidentIds.value })
    const { added, skipped } = res.data || {}
    ElMessage.success(`已添加 ${added ?? 0} 人${skipped ? `，${skipped} 人已在分组中` : ''}`)
    addVisible.value = false
    loadMembers()
    load()
  } finally {
    submitting.value = false
  }
}

const removeMember = async (row) => {
  await ElMessageBox.confirm(`确认将「${row.residentName}」移出分组？`, '提示', { type: 'warning' })
  await removeGroupMember(group.value.id, row.residentId)
  ElMessage.success('已移除')
  loadMembers()
  load()
}

onMounted(() => {
  loadCommunities()
  load()
})
</script>

<style scoped>
.page-container {
  max-width: 1280px;
  margin: 0 auto;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.filters {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 16px;
}

.pager {
  margin-top: 14px;
  justify-content: flex-end;
}

.mb12 {
  margin-bottom: 12px;
}

.member-toolbar {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
}

.spacer {
  flex: 1;
}

.muted {
  color: #909399;
  font-size: 12px;
}

.tip {
  margin-top: 10px;
}
</style>
