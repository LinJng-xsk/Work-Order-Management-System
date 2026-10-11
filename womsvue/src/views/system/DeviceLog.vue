<template>
  <el-card shadow="never">
    <div class="toolbar">
      <el-form inline>
        <el-form-item label="关键词">
          <el-input
            v-model="query.keyword"
            placeholder="设备编号/名称"
            clearable
            style="width: 200px"
            @keyup.enter="loadData"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="loadData">查询</el-button>
        </el-form-item>
      </el-form>
    </div>
    <el-table :data="list" v-loading="loading" border stripe>
      <el-table-column prop="deviceCode" label="设备编号" width="180" show-overflow-tooltip />
      <el-table-column prop="deviceName" label="设备名称" min-width="160" show-overflow-tooltip />
      <el-table-column label="操作类型" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="actionType(row.action)" size="small">
            {{ actionText(row.action) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="userName" label="使用者" width="120" />
      <el-table-column label="操作时间" width="180">
        <template #default="{ row }">{{ fmtTime(row.createTime) }}</template>
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
  </el-card>
</template>
<script setup>
import { ref, reactive, onMounted } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { getDeviceLogPage } from '../../api/system'

const list = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ pageNum: 1, pageSize: 10, keyword: '' })

const actionText = (a) => ({ create: '新增', update: '修改', delete: '删除' }[a] || a)
const actionType = (a) => ({ create: 'success', update: 'warning', delete: 'danger' }[a] || 'info')
const fmtTime = (t) => (t ? String(t).replace('T', ' ').slice(0, 19) : '-')

const loadData = async () => {
  loading.value = true
  try {
    const params = { pageNum: query.pageNum, pageSize: query.pageSize }
    if (query.keyword) params.keyword = query.keyword
    const data = await getDeviceLogPage(params)
    list.value = data.records || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
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
