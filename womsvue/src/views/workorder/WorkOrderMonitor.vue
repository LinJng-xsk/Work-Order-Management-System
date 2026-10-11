<template>
  <el-card shadow="never">
    <!-- ★ 状态分类标签（点击即筛选，默认全部） -->
    <div class="cat-menu">
      <el-radio-group v-model="query.status" size="small" @change="onCatChange">
        <el-radio-button value="">全部</el-radio-button>
        <el-radio-button value="1">待接单</el-radio-button>
        <el-radio-button value="2">处理中</el-radio-button>
        <el-radio-button value="3">待复核</el-radio-button>
        <el-radio-button value="4">已完成</el-radio-button>
        <el-radio-button value="6">已驳回</el-radio-button>
      </el-radio-group>
    </div>
    <div class="toolbar">
      <el-form inline>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 130px" @change="loadData">
            <el-option v-for="(v, k) in statusOptions" :key="k" :label="v" :value="k" />
          </el-select>
        </el-form-item>
        <el-form-item label="需求情况">
          <el-select v-model="query.needType" placeholder="全部" clearable style="width: 120px" @change="loadData">
            <el-option label="红（紧急）" value="红" />
            <el-option label="蓝（一般）" value="蓝" />
            <el-option label="绿（宽松）" value="绿" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="loadData">查询</el-button>
          <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
      <div class="legend">
        <span class="legend-item"><i class="dot red"></i>红：超当天累计超时</span>
        <span class="legend-item"><i class="dot blue"></i>蓝：超3天累计超时</span>
        <span class="legend-item"><i class="dot green"></i>绿：超5天累计超时</span>
      </div>
    </div>
    <el-table :data="list" v-loading="loading" border stripe :row-class-name="rowClass">
      <el-table-column prop="orderNo" label="工单编号" width="180" show-overflow-tooltip />
      <el-table-column prop="title" label="标题" min-width="150" show-overflow-tooltip />
      <el-table-column prop="deviceName" label="设备" width="120" show-overflow-tooltip />
      <el-table-column label="处理人" width="90" align="center">
        <template #default="{ row }">
          <span v-if="row.assigneeName">{{ row.assigneeName }}</span>
          <span v-else class="unassigned">未指派</span>
        </template>
      </el-table-column>
      <el-table-column label="需求情况" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="needType(row.needType)" size="small">{{ row.needType || '蓝' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="预约日期" width="110" align="center">
        <template #default="{ row }">{{ row.appointDate || '-' }}</template>
      </el-table-column>
      <el-table-column label="累计维修日" width="100" align="center">
        <template #default="{ row }">
          <span :class="{ warn: row.overDays > 0 }">{{ row.workDays }} 天</span>
        </template>
      </el-table-column>
      <el-table-column label="超时情况" width="120" align="center">
        <template #default="{ row }">
          <span v-if="row.overdue === 1 && row.status === '1'" class="overdue-text">超时未接单</span>
          <span v-else-if="row.overDays > 0" class="overdue-text">{{ row.overDays }} 天</span>
          <span v-else class="normal-text">未超时</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)" size="small">{{ statusText(row.status) }}</el-tag>
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
  </el-card>
</template>
<script setup>
import { ref, reactive, onMounted } from 'vue'
import { Search, Refresh } from '@element-plus/icons-vue'
import { getMonitorPage } from '../../api/workorder'
const statusOptions = { '0': '待提交', '1': '待接单', '2': '处理中', '3': '待复核', '4': '已完成', '5': '已关闭', '6': '已驳回' }
const list = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ pageNum: 1, pageSize: 20, status: '1', needType: '' })
const needType = (t) => ({ 红: 'danger', 蓝: 'primary', 绿: 'success' }[t] || 'primary')
const statusType = (s) => ({ '4': 'success', '5': 'info', '6': 'danger', '3': 'warning', '2': 'primary' }[s] || 'info')
const statusText = (s) => ({ '0': '待提交', '1': '待接单', '2': '处理中', '3': '待复核', '4': '已完成', '5': '已关闭', '6': '已驳回' }[s] || s || '-')
// ★ 状态分类标签点击：回到第 1 页再查
const onCatChange = () => {
  query.pageNum = 1
  loadData()
}
// 超时行高亮
const rowClass = ({ row }) => (row.overDays > 0 || (row.overdue === 1 && row.status === '1')) ? 'overdue-row' : ''
const loadData = async () => {
  loading.value = true
  try {
    const params = { pageNum: query.pageNum, pageSize: query.pageSize }
    if (query.status) params.status = query.status
    if (query.needType) params.needType = query.needType
    const data = await getMonitorPage(params)
    list.value = data.records || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}
const resetQuery = () => {
  query.pageNum = 1
  query.status = '1'
  query.needType = ''
  loadData()
}

onMounted(loadData)
</script>
<style scoped>
.cat-menu { margin-bottom: 12px; }
.toolbar { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 12px; }
.pager { display: flex; justify-content: flex-end; margin-top: 12px; }
.legend { display: flex; gap: 16px; padding-top: 6px; font-size: 13px; color: #606266; }
.legend-item { display: inline-flex; align-items: center; gap: 4px; }
.dot { width: 10px; height: 10px; border-radius: 50%; display: inline-block; }
.dot.red { background: #f56c6c; }
.dot.blue { background: #409eff; }
.dot.green { background: #67c23a; }
.unassigned { color: #c0c4cc; }
.overdue-text { color: #f56c6c; font-weight: 600; }
.normal-text { color: #67c23a; }
.warn { color: #e6a23c; font-weight: 600; }
:deep(.overdue-row) { background: #fef0f0 !important; }
</style>
