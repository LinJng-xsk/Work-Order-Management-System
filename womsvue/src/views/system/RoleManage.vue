<template>
  <el-card shadow="never">
    <div class="toolbar">
      <el-form inline>
        <el-form-item label="关键词">
          <el-input
            v-model="query.keyword"
            placeholder="角色名称/编码"
            clearable
            style="width: 180px"
            @keyup.enter="loadData"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="loadData">查询</el-button>
          <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
      <el-button type="success" :icon="Plus" @click="openSave()">新增角色</el-button>
    </div>
    <el-table :data="list" v-loading="loading" border stripe>
      <el-table-column label="ID" width="90">
        <template #default="{ row }">
          <el-tooltip :content="row.id" placement="top">
            <span class="id-cell">{{ String(row.id).slice(-6) }}</span>
          </el-tooltip>
        </template>
      </el-table-column>
      <el-table-column prop="roleName" label="角色名称" width="150" />
      <el-table-column prop="roleCode" label="角色编码" width="150" />
      <el-table-column prop="remark" label="备注" min-width="180" show-overflow-tooltip />
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button size="small" type="primary" plain @click="openSave(row)">编辑</el-button>
          <el-button size="small" type="danger" plain @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <!-- 新增/编辑弹窗不变，略 -->
  </el-card>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, Plus } from '@element-plus/icons-vue'
import { getRolePage, saveRole, updateRole, deleteRole } from '../../api/system'

const list = ref([])
const loading = ref(false)
const query = reactive({ pageNum: 1, pageSize: 100, keyword: '' })

const loadData = async () => {
  loading.value = true
  try {
    const params = { pageNum: query.pageNum, pageSize: query.pageSize }
    if (query.keyword) params.keyword = query.keyword
    const data = await getRolePage(params)
    list.value = data.records || []
  } finally {
    loading.value = false
  }
}
const resetQuery = () => {
  query.keyword = ''
  loadData()
}

const saveVisible = ref(false)
const saveLoading = ref(false)
const saveFormRef = ref()
const saveForm = reactive({ id: null, roleName: '', roleCode: '', remark: '' })
const saveRules = {
  roleName: [{ required: true, message: '请输入角色名称', trigger: 'blur' }],
  roleCode: [{ required: true, message: '请输入角色编码', trigger: 'blur' }]
}
const openSave = (row) => {
  saveVisible.value = true
  if (row) {
    Object.assign(saveForm, row)
  } else {
    Object.assign(saveForm, { id: null, roleName: '', roleCode: '', remark: '' })
  }
}
const handleSave = async () => {
  await saveFormRef.value.validate()
  saveLoading.value = true
  try {
    if (saveForm.id) {
      await updateRole(saveForm)
    } else {
      await saveRole(saveForm)
    }
    ElMessage.success('保存成功')
    saveVisible.value = false
    loadData()
  } finally {
    saveLoading.value = false
  }
}
const handleDelete = async (row) => {
  await ElMessageBox.confirm(`确定删除角色「${row.roleName}」吗？`, '提示', { type: 'warning' })
  await deleteRole(row.id)
  ElMessage.success('删除成功')
  loadData()
}
onMounted(loadData)
</script>

<style scoped>
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}
.id-cell {
  font-family: monospace;
  color: #909399;
  cursor: default;
}
</style>
