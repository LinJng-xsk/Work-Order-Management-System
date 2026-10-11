<template>
  <el-card shadow="never">
    <div class="toolbar">
      <el-form inline>
        <el-form-item label="关键词">
          <el-input
            v-model="query.keyword"
            placeholder="用户名/昵称"
            clearable
            style="width: 180px"
            @keyup.enter="loadData"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="loadData">查询</el-button>
        </el-form-item>
      </el-form>
      <el-button type="success" :icon="Plus" @click="openSave()">新增用户</el-button>
    </div>
    <el-table :data="list" v-loading="loading" border stripe>
      <el-table-column label="ID" width="90" align="center">
        <template #default="{ $index }">
          user-{{ (query.pageNum - 1) * query.pageSize + $index + 1 }}
        </template>
      </el-table-column>
      <el-table-column prop="username" label="用户名" width="130" />
      <el-table-column prop="nickname" label="昵称" width="130" />
      <el-table-column label="角色" width="110" align="center">
        <template #default="{ row }">
          <el-tag size="small" effect="light">{{ roleName(row.roleCode) }}</el-tag>
        </template>
      </el-table-column>
      <!-- ★ 工作日列：仅运维显示 -->
      <el-table-column label="工作日" width="150" align="center">
        <template #default="{ row }">
          <span v-if="row.roleCode === 'operator'">{{ workDaysText(row.workDays) }}</span>
          <span v-else style="color:#c0c4cc;">-</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="80" align="center">
        <template #default="{ row }">
          <el-tag :type="row.status === 0 ? 'success' : 'danger'" size="small">
            {{ row.status === 0 ? '正常' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="phone" label="手机号" width="120" />
      <el-table-column prop="createTime" label="创建时间" width="160" />
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <el-button size="small" type="primary" plain @click="openSave(row)">编辑</el-button>
          <el-button size="small" type="warning" plain @click="openResetPwd(row)">重置密码</el-button>
          <el-button size="small" type="danger" plain @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <div class="pager">
      <el-pagination
        v-model:current-page="query.pageNum"
        v-model:page-size="query.pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="loadData"
        @current-change="loadData"
      />
    </div>
    <el-dialog v-model="saveVisible" :title="saveForm.id ? '编辑用户' : '新增用户'" width="520px">
      <el-form ref="saveFormRef" :model="saveForm" :rules="saveRules" label-width="90px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="saveForm.username" :disabled="!!saveForm.id" />
        </el-form-item>
        <el-form-item v-if="!saveForm.id" label="初始密码" prop="password">
          <el-input v-model="saveForm.password" type="password" show-password placeholder="默认 123456" />
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="saveForm.nickname" />
        </el-form-item>
        <!-- ★ 角色：下拉不含"超级管理员"；编辑超管账号时角色锁定显示 -->
        <el-form-item label="角色" prop="roleId">
          <el-select v-if="!isLockSuperAdmin" v-model="saveForm.roleId" placeholder="选择角色" style="width: 100%">
            <el-option v-for="r in roles" :key="r.id" :label="r.roleName" :value="r.id" />
          </el-select>
          <el-tag v-else type="danger">超级管理员</el-tag>
        </el-form-item>
        <!-- ★ 工作日：仅运维角色显示（1=周一 … 7=周日 多选） -->
        <el-form-item v-if="isOperatorRole" label="工作日">
          <el-checkbox-group v-model="saveForm.workDaysArr">
            <el-checkbox v-for="d in weekDays" :key="d.v" :value="d.v">{{ d.label }}</el-checkbox>
          </el-checkbox-group>
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="saveForm.phone" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="saveForm.status" :active-value="0" :inactive-value="1" active-text="正常" inactive-text="禁用" />
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
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Plus } from '@element-plus/icons-vue'
import { getUserPage, saveUser, updateUser, deleteUser, resetUserPwd, getRolePage } from '../../api/system'
const list = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ pageNum: 1, pageSize: 10, keyword: '' })
const roles = ref([])
const roleName = (code) => ({ super_admin: '超级管理员', manager: '管理员', operator: '运维人员', employee: '用户' }[code] || code || '-')
// ★ 工作日字典
const weekDays = [{ v: 1, label: '周一' }, { v: 2, label: '周二' }, { v: 3, label: '周三' },
  { v: 4, label: '周四' }, { v: 5, label: '周五' }, { v: 6, label: '周六' }, { v: 7, label: '周日' }]
const workDaysText = (wd) => {
  if (!wd) return '未设置'
  return String(wd).split(',').filter(Boolean).map(Number).sort((a, b) => a - b).map((s) => {
    const d = weekDays.find((x) => x.v === s)
    return d ? d.label : ''
  }).join('、')
}

// ★ 当前编辑用户是否为运维角色（决定是否显示工作日表单项）
const isOperatorRole = computed(() => {
  const r = roles.value.find((x) => x.id === saveForm.roleId)
  return r?.roleCode === 'operator'
})
// ★ 编辑对象是否为超管（角色锁定，不可修改）
const isLockSuperAdmin = ref(false)
const lockedSuperRoleId = ref(null)
const loadData = async () => {
  loading.value = true
  try {
    const params = { pageNum: query.pageNum, pageSize: query.pageSize }
    if (query.keyword) params.keyword = query.keyword
    const data = await getUserPage(params)
    list.value = data.records || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}
const saveVisible = ref(false)
const saveLoading = ref(false)
const saveFormRef = ref()
const saveForm = reactive({ id: null, username: '', password: '123456', nickname: '', roleId: null, phone: '', status: 0, workDaysArr: [] })
const saveRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }],
  roleId: [{ required: true, message: '请选择角色', trigger: 'change' }]
}
const openSave = async (row) => {
  saveVisible.value = true
  const roleData = await getRolePage({ pageNum: 1, pageSize: 100 })
  // ★ 角色下拉过滤掉"超级管理员"：不允许通过界面授予超管
  roles.value = (roleData.records || []).filter((r) => r.roleCode !== 'super_admin')
  if (row) {
    Object.assign(saveForm, row, { roleId: row.roleId || null })
    // ★ 回显工作日（"1,3,5" → [1,3,5]）
    saveForm.workDaysArr = row.workDays ? String(row.workDays).split(',').map(Number) : []
    // ★ 超管账号：角色锁定，下拉不选、保存时保持原角色
    isLockSuperAdmin.value = row.roleCode === 'super_admin'
    lockedSuperRoleId.value = isLockSuperAdmin.value ? row.roleId : null
  } else {
    Object.assign(saveForm, { id: null, username: '', password: '123456', nickname: '', roleId: null, phone: '', status: 0, workDaysArr: [] })
    isLockSuperAdmin.value = false
    lockedSuperRoleId.value = null
  }
}
const handleSave = async () => {
  // ★ 超管账号：先把锁定角色填回，让表单校验通过
  if (isLockSuperAdmin.value) {
    saveForm.roleId = lockedSuperRoleId.value
  }
  await saveFormRef.value.validate()
  saveLoading.value = true
  try {
    // ★ 工作日数组转字符串提交
    const payload = { ...saveForm, roleIds: [saveForm.roleId], workDays: (saveForm.workDaysArr || []).join(',') }
    if (saveForm.id) {
      delete payload.password
      await updateUser(payload)
    } else {
      await saveUser(payload)
    }
    ElMessage.success('保存成功')
    saveVisible.value = false
    loadData()
  } finally {
    saveLoading.value = false
  }
}
const openResetPwd = async (row) => {
  const { value } = await ElMessageBox.prompt(`为「${row.username}」设置新密码`, '重置密码', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    inputPlaceholder: '新密码'
  })
  await resetUserPwd(row.id, { password: value })
  ElMessage.success('密码已重置')
}
const handleDelete = async (row) => {
  await ElMessageBox.confirm(`确定删除用户「${row.username}」吗？`, '提示', { type: 'warning' })
  await deleteUser(row.id)
  ElMessage.success('删除成功')
  loadData()
}
onMounted(loadData)
</script>
<style scoped>
.toolbar {
  display: flex;
  justify-content: space-between;
  margin-bottom: 12px;
}
.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}
</style>
