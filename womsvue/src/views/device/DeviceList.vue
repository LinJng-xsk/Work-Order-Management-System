<template>
  <el-card shadow="never">
    <div class="toolbar">
      <el-form inline>
        <el-form-item label="关键词">
          <el-input
            v-model="query.keyword"
            placeholder="设备名称/编号"
            clearable
            style="width: 180px"
            @keyup.enter="loadData"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="loadData">查询</el-button>
        </el-form-item>
      </el-form>
      <el-button type="success" :icon="Plus" @click="openSave()">新增设备</el-button>
    </div>
    <el-table :data="list" v-loading="loading" border stripe>
      <el-table-column label="ID" width="80" align="center">
        <template #default="{ $index }">
          device-{{ (query.pageNum - 1) * query.pageSize + $index + 1 }}
        </template>
      </el-table-column>
      <el-table-column prop="deviceCode" label="设备编号" width="120" show-overflow-tooltip />
      <el-table-column prop="deviceName" label="设备名称" min-width="120" show-overflow-tooltip />
      <el-table-column prop="categoryName" label="分类" width="110" show-overflow-tooltip />
      <el-table-column label="状态" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="deviceStatusType(row.status)" size="small" effect="light">
            {{ deviceStatusText(row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="userName" label="使用人" width="100" show-overflow-tooltip />
      <el-table-column prop="location" label="安装位置" width="110" show-overflow-tooltip />
      <el-table-column prop="remark" label="备注" min-width="140" show-overflow-tooltip />
      <el-table-column prop="purchaseDate" label="购置日期" width="110" />
      <!-- ★ 操作列：管理角色全部可编辑；运维/用户仅自己添加的可编辑 -->
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
    <el-dialog v-model="saveVisible" :title="saveForm.id ? '编辑设备' : '新增设备'" width="520px">
      <el-form ref="saveFormRef" :model="saveForm" :rules="saveRules" label-width="90px">
        <el-form-item label="设备编号" prop="deviceCode">
          <el-input v-model="saveForm.deviceCode" placeholder="如：SB-001" :disabled="!!saveForm.id" />
        </el-form-item>
        <el-form-item label="设备名称" prop="deviceName">
          <el-input v-model="saveForm.deviceName" placeholder="如：惠普激光打印机" />
        </el-form-item>
        <el-form-item label="设备分类" prop="categoryId">
          <el-select v-model="saveForm.categoryId" placeholder="选择分类" style="width: 100%">
            <el-option v-for="c in categories" :key="c.id" :label="c.categoryName" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="saveForm.status" style="width: 100%">
            <el-option :value="0" label="在线" />
            <el-option :value="1" label="离线" />
            <el-option :value="2" label="维修中" />
            <el-option :value="3" label="报废" />
          </el-select>
        </el-form-item>
        <!-- ★ 使用人：新增自动绑定建单人昵称（不可改）；编辑时可改（设备换人使用） -->
        <el-form-item label="使用人">
          <el-input v-model="saveForm.userName" :disabled="!saveForm.id" placeholder="新增自动绑定建单人" />
        </el-form-item>
        <!-- ★ 安装位置：必填 -->
        <el-form-item label="安装位置" prop="location">
          <el-input v-model="saveForm.location" placeholder="如：x 楼 xxx 室" />
        </el-form-item>
        <el-form-item label="购置日期">
          <el-date-picker v-model="saveForm.purchaseDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="saveForm.remark" type="textarea" :rows="2" />
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
import { Search, Plus } from '@element-plus/icons-vue'
import { getDevicePage, saveDevice, updateDevice, deleteDevice, getCategoryList } from '../../api/device'
import { useUserStore } from '../../store/user'
const userStore = useUserStore()
const myId = computed(() => userStore.userId ?? userStore.id ?? 0)
const isAdmin = computed(() => userStore.roles.includes('super_admin') || userStore.roles.includes('manager'))
// ★ 管理角色全权；其他人只能编辑/删除自己添加的设备（creatorId）
const canEdit = (row) => isAdmin.value || row.creatorId === myId.value
const list = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ pageNum: 1, pageSize: 10, keyword: '' })
const deviceStatusType = (s) => ({ 0: 'success', 1: 'info', 2: 'warning', 3: 'danger' }[s] || 'info')
const deviceStatusText = (s) => ({ 0: '在线', 1: '离线', 2: '维修中', 3: '报废' }[s] || '未知')
const loadData = async () => {
  loading.value = true
  try {
    const params = { pageNum: query.pageNum, pageSize: query.pageSize }
    if (query.keyword) params.keyword = query.keyword
    const data = await getDevicePage(params)
    list.value = data.records || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}
// ===== 新增/编辑 =====
const saveVisible = ref(false)
const saveLoading = ref(false)
const saveFormRef = ref()
const categories = ref([])
const saveForm = reactive({ id: null, deviceCode: '', deviceName: '', categoryId: null, status: 0, userName: '', location: '', purchaseDate: '', remark: '' })
// ★ 安装位置必填
const saveRules = {
  deviceCode: [{ required: true, message: '请输入设备编号', trigger: 'blur' }],
  deviceName: [{ required: true, message: '请输入设备名称', trigger: 'blur' }],
  categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }],
  location: [{ required: true, message: '请输入安装位置', trigger: 'blur' }]
}
const openSave = async (row) => {
  saveVisible.value = true
  categories.value = await getCategoryList()
  if (row) {
    Object.assign(saveForm, row)
  } else {
    // ★ 新增：使用人自动绑定当前登录用户昵称
    Object.assign(saveForm, { id: null, deviceCode: '', deviceName: '', categoryId: null, status: 0, userName: userStore.nickname, location: '', purchaseDate: '', remark: '' })
  }
}
const handleSave = async () => {
  await saveFormRef.value.validate()
  saveLoading.value = true
  try {
    if (saveForm.id) {
      await updateDevice(saveForm)
    } else {
      await saveDevice(saveForm)
    }
    ElMessage.success('保存成功')
    saveVisible.value = false
    loadData()
  } finally {
    saveLoading.value = false
  }
}
const handleDelete = async (row) => {
  await ElMessageBox.confirm(`确定删除设备「${row.deviceName}」吗？`, '提示', { type: 'warning' })
  await deleteDevice(row.id)
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
