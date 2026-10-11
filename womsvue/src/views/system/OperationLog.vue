<template>
  <el-card shadow="never">
    <div class="toolbar">
      <el-form inline>
        <el-form-item label="模块">
          <el-input v-model="query.module" placeholder="模块名" clearable style="width: 140px" @keyup.enter="loadData" />
        </el-form-item>
        <el-form-item label="操作人">
          <el-input v-model="query.operatorName" placeholder="用户名" clearable style="width: 140px" @keyup.enter="loadData" />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="query.operationType" placeholder="全部" clearable style="width: 110px">
            <el-option label="新增" value="新增" />
            <el-option label="修改" value="修改" />
            <el-option label="删除" value="删除" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="loadData">查询</el-button>
          <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </div>
    <el-table :data="list" v-loading="loading" border stripe>
      <el-table-column prop="module" label="模块" width="110" />
      <el-table-column label="操作类型" width="90" align="center">
        <template #default="{ row }">
          <el-tag v-if="row.operationType" :type="opType(row.operationType)" size="small">
            {{ row.operationType }}
          </el-tag>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column prop="description" label="操作" width="150" show-overflow-tooltip />
      <el-table-column prop="requestMethod" label="HTTP" width="80" align="center" />
      <el-table-column prop="operatorName" label="操作人" width="100" align="center" />
      <el-table-column prop="ip" label="IP" width="130" />
      <el-table-column label="状态" width="80" align="center">
        <template #default="{ row }">
          <el-tag :type="row.status === 0 ? 'success' : 'danger'" size="small">
            {{ row.status === 0 ? '成功' : '失败' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="costTime" label="耗时(ms)" width="90" align="right" />
      <el-table-column label="操作时间" width="170">
        <template #default="{ row }">{{ fmtTime(row.createTime) }}</template>
      </el-table-column>
      <!-- ★ 成功操作：\n 转 <br> 换行显示，多条内容分行清晰 -->
        <!-- ★ 成功操作：完整多行显示，不截断 -->
      <el-table-column label="成功操作" min-width="280">
        <template #default="{ row }">
          <span v-html="renderDetail(row.detail)" class="log-detail"></span>
        </template>
      </el-table-column>

      <!-- 失败原因：内容长自动换行完整显示 -->
      <el-table-column label="失败原因" min-width="220">
        <template #default="{ row }">
          <div class="cell-wrap">{{ row.errorMsg || '-' }}</div>
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
import { getOperationLogPage } from '../../api/system'
const list = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ pageNum: 1, pageSize: 10, module: '', operatorName: '', operationType: '' })
const opType = (t) => ({ 新增: 'success', 修改: 'warning', 删除: 'danger', 查询: 'info' }[t] || 'info')
const fmtTime = (t) => (t ? String(t).replace('T', ' ').slice(0, 19) : '-')
// ★ 转义特殊字符，防止破坏布局（detail 来自请求参数）
const escapeHtml = (s) =>
  String(s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;')
// ★ 后端 detail 用 \n 换行，渲染前转成 <br> 才能真正换行
const renderDetail = (d) => (d ? escapeHtml(d).replace(/\n/g, '<br>') : '-')
const loadData = async () => {
  loading.value = true
  try {
    const params = { pageNum: query.pageNum, pageSize: query.pageSize }
    if (query.module) params.module = query.module
    if (query.operatorName) params.operatorName = query.operatorName
    if (query.operationType) params.operationType = query.operationType
    const data = await getOperationLogPage(params)
    list.value = data.records || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}
const resetQuery = () => {
  query.pageNum = 1
  query.module = ''
  query.operatorName = ''
  query.operationType = ''
  loadData()
}
onMounted(loadData)
</script>
<style scoped>
.toolbar { margin-bottom: 12px; }
.pager { display: flex; justify-content: flex-end; margin-top: 12px; }
/* ★ 成功操作：多行明细的样式 */
.log-detail {
  line-height: 1.7;
  word-break: break-all;
}
.cell-wrap {
  white-space: pre-line;
  word-break: normal;
  overflow-wrap: break-word;
  line-height: 1.5;
  padding: 4px 0;
}
</style>
