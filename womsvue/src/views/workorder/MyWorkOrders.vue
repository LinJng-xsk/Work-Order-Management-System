<template>
  <el-card shadow="never">
    <!-- 分类菜单（点击即筛选，默认处理中） -->
    <div class="cat-menu">
      <el-radio-group v-model="query.status" size="small" @change="loadData">
        <el-radio-button value="">全部</el-radio-button>
        <el-radio-button value="1">待接单</el-radio-button>
        <el-radio-button value="2">处理中</el-radio-button>
        <el-radio-button value="3">待复核</el-radio-button>
        <el-radio-button value="4">已完成</el-radio-button>
        <el-radio-button value="6">已驳回</el-radio-button>
      </el-radio-group>
    </div>
    <!-- 查询栏：三字段独立关键词（同时生效） -->
    <div class="toolbar">
      <el-form inline>
        <el-form-item label="编号">
          <el-input v-model="query.keywordNo" placeholder="工单编号" clearable style="width: 170px" @keyup.enter="loadData" />
        </el-form-item>
        <el-form-item label="标题">
          <el-input v-model="query.keywordTitle" placeholder="工单标题" clearable style="width: 170px" @keyup.enter="loadData" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="query.keywordDesc" placeholder="问题描述" clearable style="width: 190px" @keyup.enter="loadData" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="loadData">查询</el-button>
          <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </div>
    <!-- 我的工单表格 -->
    <el-table :data="list" v-loading="loading" border stripe>
      <el-table-column prop="id" label="工单编号" width="170" show-overflow-tooltip />
      <el-table-column prop="title" label="标题" min-width="140" show-overflow-tooltip />
      <el-table-column prop="deviceName" label="设备" width="130" show-overflow-tooltip />
      <el-table-column label="需求情况" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="needType(row.needType)" size="small">{{ row.needType || '蓝' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="140" align="center">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)" size="small" class="status-tag">
            <div class="st-main">
              {{ statusText(row.status) }}
              <el-tag v-if="row.overdue === 1 && row.status === '1'" type="danger" size="small" class="overdue-tag">超时</el-tag>
            </div>
            <div v-if="row.rejectCount > 0" class="st-sub">{{ rejectText(row.rejectCount) }}</div>
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="处理人" width="130" align="center">
        <template #default="{ row }">
          <span>{{ row.assigneeName || '未指派' }}</span>
          <div v-if="row.assigneeName" style="font-size:12px;color:#909399;">{{ row.appointDate }} {{ row.timeSlot }}</div>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" width="160">
        <template #default="{ row }">{{ fmtTime(row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="340" fixed="right">
        <template #default="{ row }">
          <el-button size="small" type="primary" plain @click="openDetail(row)">详情</el-button>
          <!-- ★ 对话：待接单（没被接/未确认）不给入口；有处理人且非待接单才可对话 -->
          <el-button v-if="canChat(row)" size="small" type="success" plain @click="openChat(row)">对话</el-button>
          <el-button v-if="row.status === '1'" size="small" type="info" plain @click="openEdit(row)">编辑</el-button>
          <el-button v-if="row.status === '1' && !row.assigneeId" size="small" type="warning" plain @click="openRequest(row)">请求</el-button>
          <!-- ★ 取消请求/取消接单：待确认或处理中均可取消 -->
          <el-button v-if="(row.status === '1' || row.status === '2') && row.assigneeId" size="small" type="warning" plain @click="doCancelAssign(row)">
            {{ row.status === '1' ? '取消请求' : '取消接单' }}
          </el-button>
          <el-button v-if="row.status === '3'" size="small" type="success" plain @click="openReview(row)">验收</el-button>
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
    <!-- 详情弹窗 -->
    <el-dialog v-model="detailVisible" title="工单详情" width="680px">
      <el-descriptions :column="2" border v-if="detail">
        <el-descriptions-item label="工单编号">{{ detail.id }}</el-descriptions-item>
        <el-descriptions-item label="状态"><StatusTag :status="detail.status" /></el-descriptions-item>
        <el-descriptions-item label="标题" :span="2">{{ detail.title }}</el-descriptions-item>
        <el-descriptions-item label="故障描述" :span="2">{{ detail.description || '-' }}</el-descriptions-item>
        <el-descriptions-item label="设备">{{ detail.deviceName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="处理人">{{ detail.assigneeName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="报修人">{{ detail.reporterName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="需求情况">{{ detail.needType || '-' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ fmtTime(detail.createTime) }}</el-descriptions-item>
        <el-descriptions-item label="处理日期">{{ detail.appointDate || '-' }}</el-descriptions-item>
        <el-descriptions-item label="时间段">{{ detail.timeSlot || '-' }}</el-descriptions-item>
        <el-descriptions-item label="维修方案" :span="2">{{ detail.repairContent || '-' }}</el-descriptions-item>
        <el-descriptions-item label="维修费用">{{ detail.repairCost ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="验收评分">{{ detail.evaluateScore ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="验收评价" :span="2">{{ detail.evaluateComment || '-' }}</el-descriptions-item>
      </el-descriptions>
      <el-divider content-position="left">流转记录</el-divider>
      <el-timeline v-if="records.length">
        <el-timeline-item v-for="r in records" :key="r.id" :timestamp="fmtTime(r.createTime)"
                          :type="r.action === 'reject' || r.action === 'reject_assign' ? 'danger' : 'primary'">
          <b>{{ r.action }}</b> —— {{ r.operatorName || '-' }}：{{ r.remark || '-' }}
        </el-timeline-item>
      </el-timeline>
      <el-empty v-else description="暂无流转记录" :image-size="60" />
    </el-dialog>
    <!-- 对话弹窗 -->
    <el-dialog v-model="chatVisible" :title="`工单沟通 - ${chatRow ? chatRow.title : ''}`" width="560px">
      <div class="chat-box" v-if="messages.length">
        <div v-for="m in messages" :key="m.id" class="chat-item">
          <div class="chat-head">
            <el-tag size="small" :type="chatRoleType(m.senderRole)" effect="plain">{{ chatRoleText(m.senderRole) }}</el-tag>
            <span>{{ m.senderName }}</span>
            <span class="chat-time">{{ fmtTime(m.createTime) }}</span>
          </div>
          <div class="chat-content">{{ m.content }}</div>
        </div>
      </div>
      <el-empty v-else description="暂无沟通记录" :image-size="50" />
      <div class="chat-send">
        <el-input v-model="chatText" type="textarea" :rows="2" placeholder="与运维沟通维修进度 / 问题" />
        <el-button type="primary" :loading="chatLoading" @click="handleSendChat">发送</el-button>
      </div>
    </el-dialog>
    <!-- 编辑弹窗 -->
    <el-dialog v-model="editVisible" title="编辑工单" width="520px">
      <el-form label-width="90px">
        <el-form-item label="标题" required>
          <el-input v-model="editForm.title" />
        </el-form-item>
        <el-form-item label="故障描述">
          <el-input v-model="editForm.description" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="需求情况">
          <el-radio-group v-model="editForm.needType">
            <el-radio value="红">红（紧急）</el-radio>
            <el-radio value="蓝">蓝（一般）</el-radio>
            <el-radio value="绿">绿（宽松）</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="actionLoading" @click="handleEdit">保存</el-button>
      </template>
    </el-dialog>
    <!-- 请求指定运维弹窗 -->
    <el-dialog v-model="assignVisible" title="请求指定运维" width="560px">
      <el-form label-width="90px">
        <el-form-item label="处理人员" required>
          <el-select v-model="assignForm.handlerId" placeholder="选择运维人员" style="width: 100%" @change="onSelectOperator">
            <el-option v-for="o in operatorOptions" :key="o.id" :label="operatorLabel(o)" :value="o.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="预约排期" required>
          <SchedulePicker v-if="assignForm.handlerId" :operator-id="assignForm.handlerId"
                          v-model:date="assignForm.appointDate"
                          v-model:timeSlot="assignForm.timeSlot" />
          <div v-else class="cost-tip">请先选择运维人员，再查看其排期</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="assignVisible = false">取消</el-button>
        <el-button type="primary" :loading="actionLoading" @click="handleAssign">确认请求</el-button>
      </template>
    </el-dialog>
    <!-- 验收弹窗 -->
    <el-dialog v-model="reviewVisible" title="验收评价" width="500px">
      <el-form label-width="80px">
        <el-form-item label="验收结果">
          <el-radio-group v-model="reviewForm.approve">
            <el-radio :value="true">通过</el-radio>
            <el-radio :value="false">不通过</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="!reviewForm.approve" label="说明">
          <el-alert type="warning" :closable="false" title="不通过将回到待接单池，并累计驳回次数，其他运维可再次接单" />
        </el-form-item>
        <el-form-item label="评分">
          <el-rate v-model="reviewForm.score" :max="5" />
        </el-form-item>
        <el-form-item label="评价">
          <el-input v-model="reviewForm.comment" type="textarea" :rows="3" placeholder="维修质量评价（可选）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reviewVisible = false">取消</el-button>
        <el-button type="primary" :loading="actionLoading" @click="handleReview">提交</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>
<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh } from '@element-plus/icons-vue'
import StatusTag from '../../components/StatusTag.vue'
import SchedulePicker from '../../components/SchedulePicker.vue'
import { useUserStore } from '../../store/user'
import {
  getWorkOrderPage, getWorkOrderDetail, getWorkOrderRecord,
  editWorkOrder, assignWorkOrder, cancelAssignWorkOrder,
  getRejectedOperators, reviewWorkOrder, deleteWorkOrder,
  getOperators, getOrderMessages, sendOrderMessage
} from '../../api/workorder'
const userStore = useUserStore()
// ★ 19 位大 id：必须字符串比较，Number() 会丢精度
const myIdStr = computed(() => String(userStore.userId ?? userStore.id ?? ''))
// ===== 常量 =====
const statusOptions = { '0': '待提交', '1': '待接单', '2': '处理中', '3': '待复核', '4': '已完成', '5': '已关闭', '6': '已驳回', '7': '已报废' }
const statusType = (s) => ({ '4': 'success', '5': 'info', '6': 'danger', '3': 'warning', '2': 'primary', '7': 'danger' }[s] || 'info')
const needType = (t) => ({ 红: 'danger', 蓝: 'primary', 绿: 'success' }[t] || 'primary')
const statusText = (s) => statusOptions[s] || s || '-'
const rejectText = (n) => {
  const c = n == null ? 0 : Number(n)
  return c > 999 ? '已驳回999+次' : `已驳回${c}次`
}
const fmtTime = (t) => (t ? String(t).replace('T', ' ').slice(0, 19) : '-')
const weekMap = { 1: '周一', 2: '周二', 3: '周三', 4: '周四', 5: '周五', 6: '周六', 7: '周日' }
// ★ 工作日显示排序：1-7 顺序
const workDaysText = (wd) => {
  if (!wd) return '未设置'
  return String(wd).split(',').filter(Boolean).map(Number).sort((a, b) => a - b).map((s) => weekMap[s] || '').join('、')
}
const operatorLabel = (o) => `${o.nickname || o.username || o.id}（工作日：${workDaysText(o.workDays)}）`
// ★ 对话入口：待接单（没被接/未确认）完全不给；有处理人才可对话
const canChat = (row) => row.status !== '1' && !!row.assigneeId
// ===== 列表（只查自己报修的）=====
const list = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({
  pageNum: 1,
  pageSize: 10,
  status: '2',            // ★ 分类菜单：默认"处理中"
  keywordNo: '',          // ★ 编号关键词（id 或 WO 号）
  keywordTitle: '',       // ★ 标题关键词
  keywordDesc: ''         // ★ 描述关键词
})
const loadData = async () => {
  loading.value = true
  try {
    const params = { pageNum: query.pageNum, pageSize: query.pageSize, myReporter: true }
    if (query.status !== '' && query.status !== null) params.status = query.status
    if (query.keywordNo) params.keywordNo = query.keywordNo
    if (query.keywordTitle) params.keywordTitle = query.keywordTitle
    if (query.keywordDesc) params.keywordDesc = query.keywordDesc
    const data = await getWorkOrderPage(params)
    list.value = data.records || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}
const resetQuery = () => {
  query.pageNum = 1
  query.status = '2'
  query.keywordNo = ''
  query.keywordTitle = ''
  query.keywordDesc = ''
  loadData()
}
// ===== 详情 =====
const detailVisible = ref(false)
const detail = ref(null)
const records = ref([])
const openDetail = async (row) => {
  detailVisible.value = true
  detail.value = await getWorkOrderDetail(row.id)
  records.value = await getWorkOrderRecord(row.id)
}
// ===== 对话 =====
const chatVisible = ref(false)
const chatRow = ref(null)
const messages = ref([])
const chatText = ref('')
const chatLoading = ref(false)
const chatRoleText = (r) => ({ super_admin: '管理员', manager: '管理员', operator: '运维', employee: '用户' }[r] || r || '-')
const chatRoleType = (r) => ({ super_admin: 'danger', manager: 'danger', operator: 'primary', employee: 'success' }[r] || 'info')
const openChat = async (row) => {
  chatRow.value = row
  chatVisible.value = true
  chatText.value = ''
  messages.value = await getOrderMessages(row.id)
}
const handleSendChat = async () => {
  if (!chatText.value.trim()) return ElMessage.warning('请输入消息内容')
  chatLoading.value = true
  try {
    await sendOrderMessage(chatRow.value.id, { content: chatText.value })
    chatText.value = ''
    messages.value = await getOrderMessages(chatRow.value.id)
    ElMessage.success('已发送')
  } finally {
    chatLoading.value = false
  }
}
// ===== 编辑（仅待接单）=====
const editVisible = ref(false)
const editForm = reactive({ title: '', description: '', needType: '蓝' })
let editRow = null
const openEdit = (row) => {
  editRow = row
  Object.assign(editForm, { title: row.title, description: row.description || '', needType: row.needType || '蓝' })
  editVisible.value = true
}
const handleEdit = async () => {
  if (!editForm.title.trim()) return ElMessage.warning('请输入标题')
  actionLoading.value = true
  try {
    await editWorkOrder(editRow.id, { title: editForm.title, description: editForm.description, needType: editForm.needType })
    ElMessage.success('保存成功')
    editVisible.value = false
    loadData()
  } finally {
    actionLoading.value = false
  }
}
// ===== 请求指定运维 =====
const assignVisible = ref(false)
const assignForm = reactive({ handlerId: null, appointDate: '', timeSlot: '' })
const operatorOptions = ref([])
let assignRow = null
const openRequest = async (row) => {
  assignRow = row
  Object.assign(assignForm, { handlerId: null, appointDate: '', timeSlot: '' })
  assignVisible.value = true
  const rejected = await getRejectedOperators(row.id)
  const data = await getOperators()
  // ★ 字符串比较（19 位大 id，Number() 会丢精度）；排除拒过该单的运维和自己
  operatorOptions.value = (data || []).filter((o) => !rejected.includes(String(o.id)) && String(o.id) !== myIdStr.value)
}
const onSelectOperator = () => {
  assignForm.appointDate = ''
  assignForm.timeSlot = ''
}
const handleAssign = async () => {
  if (!assignForm.handlerId) return ElMessage.warning('请选择处理人')
  if (!assignForm.appointDate) return ElMessage.warning('请选择处理日期')
  if (!assignForm.timeSlot) return ElMessage.warning('请选择时间段')
  const selected = operatorOptions.value.find((o) => String(o.id) === String(assignForm.handlerId))
  actionLoading.value = true
  try {
    await assignWorkOrder(assignRow.id, {
      assigneeId: assignForm.handlerId,
      assigneeName: selected ? selected.nickname : '',
      appointDate: assignForm.appointDate,
      timeSlot: assignForm.timeSlot
    })
    ElMessage.success('请求成功，待运维确认')
    assignVisible.value = false
    loadData()
  } finally {
    actionLoading.value = false
  }
}
// ===== 取消请求 / 取消接单（待确认或处理中均可取消）=====
const doCancelAssign = async (row) => {
  const tip = row.status === '1'
    ? `确定取消对运维「${row.assigneeName}」的请求吗？工单将回到待接单池。`
    : `确定取消运维「${row.assigneeName}」的接单吗？工单将回到待接单池。`
  await ElMessageBox.confirm(tip, '提示', { type: 'warning' })
  actionLoading.value = true
  try {
    await cancelAssignWorkOrder(row.id)
    ElMessage.success('已取消，工单回到待接单池')
    loadData()
  } finally {
    actionLoading.value = false
  }
}
// ===== 验收 =====
const reviewVisible = ref(false)
const reviewForm = reactive({ approve: true, score: 5, comment: '' })
let reviewRow = null
const openReview = (row) => {
  reviewRow = row
  reviewForm.approve = true
  reviewForm.score = 5
  reviewForm.comment = ''
  reviewVisible.value = true
}
const handleReview = async () => {
  actionLoading.value = true
  try {
    await reviewWorkOrder(reviewRow.id, {
      score: reviewForm.score,
      comment: reviewForm.comment,
      pass: reviewForm.approve
    })
    ElMessage.success(reviewForm.approve ? '验收通过，工单已完成' : '已驳回，工单回到待接单池')
    reviewVisible.value = false
    loadData()
  } finally {
    actionLoading.value = false
  }
}
// ===== 删除 =====
const handleDelete = async (row) => {
  await ElMessageBox.confirm(`确定删除工单「${row.title}」吗？`, '提示', { type: 'warning' })
  await deleteWorkOrder(row.id)
  ElMessage.success('删除成功')
  loadData()
}
// ===== 通用动作 loading =====
const actionLoading = ref(false)
onMounted(loadData)
</script>
<style scoped>
.cat-menu {
  margin-bottom: 12px;
}
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 12px;
}
.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}
.status-tag {
  height: auto;
  padding: 3px 10px;
  display: inline-flex;
  flex-direction: column;
  align-items: center;
  line-height: 1.5;
  white-space: nowrap;
}
.st-main {
  font-size: 13px;
  font-weight: 600;
}
.st-sub {
  font-size: 12px;
  opacity: 0.85;
}
.overdue-tag {
  margin-left: 4px;
}
.cost-tip {
  font-size: 12px;
  color: #909399;
  line-height: 1.6;
}
.chat-box {
  max-height: 220px;
  overflow-y: auto;
  margin-bottom: 8px;
}
.chat-item {
  margin-bottom: 10px;
}
.chat-head {
  font-size: 12px;
  color: #6b7280;
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 2px;
}
.chat-content {
  background: #f3f4f6;
  border-radius: 6px;
  padding: 8px 10px;
  font-size: 13px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}
.chat-send {
  display: flex;
  gap: 8px;
  margin-top: 8px;
}
.chat-send :deep(.el-input) {
  flex: 1;
}
</style>
