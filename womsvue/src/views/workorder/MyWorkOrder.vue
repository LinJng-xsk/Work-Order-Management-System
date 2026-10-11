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
    <!-- 我的接单表格（指派给我的） -->
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
      <el-table-column label="操作" width="360" fixed="right">
        <template #default="{ row }">
          <el-button size="small" type="primary" plain @click="openDetail(row)">详情</el-button>
          <!-- ★ 对话：待接单(未确认)不给入口，其余状态（处理中/待复核等）可对话 -->
          <el-button v-if="canChat(row)" size="small" type="success" plain @click="openChat(row)">对话</el-button>
          <!-- ★ 接单：待接单（指派给我的，等确认） -->
          <el-button v-if="row.status === '1'" size="small" type="warning" plain @click="openAccept(row)">接单</el-button>
          <!-- ★ 不接：别人请求我接的单，我有权拒绝（填原因，用户可看到） -->
          <el-button v-if="row.status === '1'" size="small" type="danger" plain @click="doReject(row)">不接</el-button>
          <!-- ★ 提交复核：我处理中的单 -->
          <el-button v-if="row.status === '2'" size="small" type="success" plain @click="openSubmit(row)">提交复核</el-button>
          <!-- 待复核：等用户验收 -->
          <el-tag v-if="row.status === '3'" size="small" type="info" style="margin-left: 8px;">等待验收</el-tag>
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
        <el-input v-model="chatText" type="textarea" :rows="2" placeholder="与用户沟通维修进度 / 问题" />
        <el-button type="primary" :loading="chatLoading" @click="handleSendChat">发送</el-button>
      </div>
    </el-dialog>
    <!-- 接单弹窗（★ mine：后端从 Token 识别当前运维，日历标红自己的已占用时段） -->
    <el-dialog v-model="acceptVisible" title="接单 - 选择处理排期" width="560px">
      <el-form label-width="90px">
        <el-form-item label="预约排期" required>
          <SchedulePicker mine v-model:date="acceptForm.appointDate" v-model:timeSlot="acceptForm.timeSlot" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="acceptVisible = false">取消</el-button>
        <el-button type="primary" :loading="actionLoading" @click="handleAccept">确认接单</el-button>
      </template>
    </el-dialog>
    <!-- 提交复核弹窗 -->
    <el-dialog v-model="submitVisible" title="提交复核" width="520px">
      <el-form label-width="90px">
        <el-form-item label="维修方案" required>
          <el-input v-model="submitForm.repairContent" type="textarea" :rows="3" placeholder="如：更换配件并重新调试" />
        </el-form-item>
        <el-form-item label="维修费用" required>
          <el-input-number v-model="submitForm.repairCost" :min="0" :precision="2" :step="20" style="width: 200px" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="submitVisible = false">取消</el-button>
        <el-button type="primary" :loading="actionLoading" @click="handleSubmitReview">提交</el-button>
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
  acceptWorkOrder, submitReviewWorkOrder,
  rejectWorkOrder,
  getOrderMessages, sendOrderMessage
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
// ★ 对话入口：待接单（还没被接/未确认）完全不给；有处理人才可对话
const canChat = (row) => row.status !== '1' && !!row.assigneeId
// ===== 列表（指派给我的：后端 myOrders 自动按登录态取当前运维 id）=====
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
    const params = {
      pageNum: query.pageNum,
      pageSize: query.pageSize,
      myOrders: true
    }
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
// ===== 接单（用户请求指派给我、待确认的单）=====
const acceptVisible = ref(false)
const acceptForm = reactive({ appointDate: '', timeSlot: '' })
let acceptRow = null
const openAccept = (row) => {
  acceptRow = row
  Object.assign(acceptForm, { appointDate: '', timeSlot: '' })
  acceptVisible.value = true
}
const handleAccept = async () => {
  if (!acceptForm.appointDate) return ElMessage.warning('请选择处理日期')
  if (!acceptForm.timeSlot) return ElMessage.warning('请选择时间段')
  actionLoading.value = true
  try {
    await acceptWorkOrder(acceptRow.id, {
      appointDate: acceptForm.appointDate,
      timeSlot: acceptForm.timeSlot
    })
    ElMessage.success('接单成功')
    acceptVisible.value = false
    loadData()
  } finally {
    actionLoading.value = false
  }
}
// ===== 不接（拒绝别人请求我接的单，用户能看到原因）=====
const doReject = async (row) => {
  let reason = ''
  try {
    const { value } = await ElMessageBox.prompt('填写不接原因（可选）', '拒绝接单', {
      confirmButtonText: '确认不接',
      cancelButtonText: '取消',
      inputPlaceholder: '如：当日已排满'
    })
    reason = value || ''
  } catch (e) {
    return
  }
  actionLoading.value = true
  try {
    await rejectWorkOrder(row.id, reason)
    ElMessage.success('已拒单，工单回到待接单池')
    loadData()
  } finally {
    actionLoading.value = false
  }
}
// ===== 提交复核 =====
const submitVisible = ref(false)
const submitForm = reactive({ repairContent: '', repairCost: 0 })
let submitRow = null
const openSubmit = (row) => {
  submitRow = row
  Object.assign(submitForm, { repairContent: '', repairCost: 0 })
  submitVisible.value = true
}
const handleSubmitReview = async () => {
  if (!submitForm.repairContent.trim()) return ElMessage.warning('请填写维修方案')
  if (submitForm.repairCost == null || submitForm.repairCost < 0) return ElMessage.warning('请填写维修费用')
  actionLoading.value = true
  try {
    await submitReviewWorkOrder(submitRow.id, {
      repairContent: submitForm.repairContent,
      repairCost: submitForm.repairCost
    })
    ElMessage.success('已提交复核，等待用户验收')
    submitVisible.value = false
    loadData()
  } finally {
    actionLoading.value = false
  }
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
