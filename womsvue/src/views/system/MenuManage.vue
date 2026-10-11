<template>
  <el-card shadow="never">
    <div class="toolbar">
      <span class="page-desc">菜单管理：RBAC 菜单权限树（新增的菜单需在角色管理中绑定权限标识后生效）</span>
      <el-button type="success" :icon="Plus" @click="openSave()">新增菜单</el-button>
    </div>
    <el-table
      :data="tree"
      v-loading="loading"
      border
      row-key="id"
      default-expand-all
      :tree-props="{ children: 'children' }"
    >
      <el-table-column label="菜单名称" min-width="200">
        <template #default="{ row }">
          <el-icon v-if="iconOf(row.icon)" style="vertical-align: -2px; margin-right: 6px; color: #409eff">
            <component :is="iconOf(row.icon)" />
          </el-icon>
          {{ row.menuName }}
        </template>
      </el-table-column>
      <el-table-column prop="path" label="路由路径" min-width="140" show-overflow-tooltip />
      <el-table-column prop="perms" label="权限标识" min-width="160" show-overflow-tooltip />
      <el-table-column label="类型" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="menuTypeType(row.menuType)" size="small">
            {{ menuTypeText(row.menuType) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="80" align="center">
        <template #default="{ row }">
          <el-tag :type="row.status === 0 ? 'success' : 'danger'" size="small">
            {{ row.status === 0 ? '正常' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="排序" prop="sort" width="70" align="center" />
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button size="small" type="primary" plain @click="openSave(row)">编辑</el-button>
          <el-button size="small" type="danger" plain @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-dialog v-model="saveVisible" :title="saveForm.id ? '编辑菜单' : '新增菜单'" width="520px">
      <el-form ref="saveFormRef" :model="saveForm" :rules="saveRules" label-width="90px">
        <el-form-item label="上级菜单">
          <el-select v-model="saveForm.parentId" placeholder="顶级菜单" clearable style="width: 100%">
            <el-option v-for="m in tree" :key="m.id" :label="m.menuName" :value="m.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="菜单名称" prop="menuName">
          <el-input v-model="saveForm.menuName" placeholder="如：工单管理" />
        </el-form-item>
        <el-form-item label="菜单类型">
          <el-radio-group v-model="saveForm.menuType">
            <el-radio :value="0">目录</el-radio>
            <el-radio :value="1">菜单</el-radio>
            <el-radio :value="2">按钮</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="路由路径">
          <el-input v-model="saveForm.path" placeholder="如：/workorder" />
        </el-form-item>
        <el-form-item label="权限标识">
          <el-input v-model="saveForm.perms" placeholder="如：workorder:list" />
        </el-form-item>
        <el-form-item label="图标">
          <el-input v-model="saveForm.icon" placeholder="Element Plus 图标名，如：Tickets" />
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
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import * as Icons from '@element-plus/icons-vue'
import { getMenuTree, saveMenu, updateMenu, deleteMenu } from '../../api/system'

const tree = ref([])
const loading = ref(false)
const saveVisible = ref(false)
const saveLoading = ref(false)
const saveFormRef = ref()
const saveForm = reactive({ id: null, parentId: null, menuName: '', menuType: 1, path: '', perms: '', icon: '', sort: 0, status: 0 })
const saveRules = {
  menuName: [{ required: true, message: '请输入菜单名称', trigger: 'blur' }]
}

// 图标名 -> 真实图标组件（找不到返回 undefined）
const iconOf = (name) => (name ? Icons[name] : undefined)
const menuTypeText = (t) => ({ 0: '目录', 1: '菜单', 2: '按钮' }[t] ?? '-')
const menuTypeType = (t) => ({ 0: 'warning', 1: 'primary', 2: 'info' }[t] ?? 'info')

const loadData = async () => {
  loading.value = true
  try {
    tree.value = await getMenuTree()
  } finally {
    loading.value = false
  }
}

const openSave = (row) => {
  saveVisible.value = true
  if (row) {
    Object.assign(saveForm, {
      ...row,
      parentId: row.parentId || null,
      menuType: row.menuType ?? 1,
      status: row.status ?? 0
    })
  } else {
    Object.assign(saveForm, { id: null, parentId: null, menuName: '', menuType: 1, path: '', perms: '', icon: '', sort: 0, status: 0 })
  }
}

const handleSave = async () => {
  await saveFormRef.value.validate()
  saveLoading.value = true
  try {
    if (saveForm.id) {
      await updateMenu(saveForm)
    } else {
      await saveMenu(saveForm)
    }
    ElMessage.success('保存成功')
    saveVisible.value = false
    loadData()
  } finally {
    saveLoading.value = false
  }
}

const handleDelete = async (row) => {
  await ElMessageBox.confirm(`确定删除菜单「${row.menuName}」吗？`, '提示', { type: 'warning' })
  await deleteMenu(row.id)
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
