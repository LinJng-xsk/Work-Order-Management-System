<template>
  <el-card shadow="never">
    <div class="toolbar">
      <span class="page-desc">设备分类管理：用于设备资产分类维护</span>
      <el-button type="success" :icon="Plus" @click="openSave()">新增分类</el-button>
    </div>
    <el-table :data="list" v-loading="loading" border stripe>
      <!-- ★ ID 列：显示 cat-N，不再显示雪花 ID -->
      <el-table-column label="ID" width="80" align="center">
        <template #default="{ $index }">
          cat-{{ $index + 1 }}
        </template>
      </el-table-column>
      <el-table-column prop="categoryName" label="分类名称" min-width="180" />
      <el-table-column prop="sort" label="排序" width="90" align="center" />
      <el-table-column label="状态" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="row.status === 0 ? 'success' : 'danger'" size="small">
            {{ row.status === 0 ? '正常' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <!-- ★ 操作列：管理角色全部可编辑；其他人仅自己添加的分类可编辑 -->
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <template v-if="canEdit(row)">
            <el-button size="small" type="primary" plain @click="openSave(row)">编辑</el-button>
            <el-button size="small" type="danger" plain @click="handleDelete(row)">删除</el-button>
          </template>
          <span v-else style="color:#c0c4cc;font-size:13px;">—</span>
        </template>
      </el-table-column>
    </el-table>
    <el-dialog v-model="saveVisible" :title="saveForm.id ? '编辑分类' : '新增分类'" width="460px">
      <el-form ref="saveFormRef" :model="saveForm" :rules="saveRules" label-width="90px">
        <el-form-item label="分类名称" prop="categoryName">
          <el-input v-model="saveForm.categoryName" placeholder="如：打印机 / 空调 / 电脑" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="saveForm.sort" :min="0" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="saveForm.status" :active-value="0" :inactive-value="1" active-text="正常" inactive-text="停用" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="saveVisible = false">取消</el-button>
        <el-button type="primary" :loading="saveLoading" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>
<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { getCategoryList, saveCategory, updateCategory, deleteCategory } from '../../api/device'
import { useUserStore } from '../../store/user'

const userStore = useUserStore()
const myId = computed(() => userStore.userId ?? userStore.id ?? 0)
const isAdmin = computed(() => userStore.roles.includes('super_admin') || userStore.roles.includes('manager'))
// ★ 管理角色全权；其他人只能编辑/删除自己添加的分类（creatorId）
const canEdit = (row) => isAdmin.value || row.creatorId === myId.value

const list = ref([])
const loading = ref(false)
const saveVisible = ref(false)
const saveLoading = ref(false)
const saveFormRef = ref()
const saveForm = reactive({ id: null, parentId: 0, categoryName: '', sort: 0, status: 0 })
const saveRules = {
  categoryName: [{ required: true, message: '请输入分类名称', trigger: 'blur' }]
}
const loadData = async () => {
  loading.value = true
  try {
    list.value = await getCategoryList()
  } finally {
    loading.value = false
  }
}
const openSave = (row) => {
  saveVisible.value = true
  if (row) {
    Object.assign(saveForm, row)
  } else {
    Object.assign(saveForm, { id: null, parentId: 0, categoryName: '', sort: 0, status: 0 })
  }
}
const handleSave = async () => {
  await saveFormRef.value.validate()
  saveLoading.value = true
  try {
    if (saveForm.id) {
      await updateCategory(saveForm)
    } else {
      await saveCategory(saveForm)
    }
    ElMessage.success('保存成功')
    saveVisible.value = false
    loadData()
  } finally {
    saveLoading.value = false
  }
}
const handleDelete = async (row) => {
  await ElMessageBox.confirm(`确定删除分类「${row.categoryName}」吗？`, '提示', { type: 'warning' })
  await deleteCategory(row.id)
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
.page-desc {
  color: #6b7280;
  font-size: 13px;
}
</style>
