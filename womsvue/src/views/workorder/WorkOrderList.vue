<template>
  <el-card shadow="never">
    <!-- ==================== 查询栏 ==================== -->
    <div class="toolbar">
      <el-form inline>
        <!-- ★ 状态筛选：仅管理员可见 -->
        <el-form-item v-if="isAdmin" label="状态">
          <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 130px">
            <el-option v-for="(v, k) in statusOptions" :key="k" :label="v" :value="k" />
          </el-select>
        </el-form-item>
        <el-form-item label="编号">
          <el-input v-model="query.keywordNo" placeholder="工单编号" clearable style="width: 150px" @keyup.enter="loadData" />
        </el-form-item>
        <el-form-item label="标题">
          <el-input v-model="query.keywordTitle" placeholder="工单标题" clearable style="width: 150px" @keyup.enter="loadData" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="query.keywordDesc" placeholder="问题描述" clearable style="width: 170px" @keyup.enter="loadData" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="loadData">查询</el-button>
          <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
      <el-button type="success" :icon="Plus" @click="openCreate">新建工单</el-button>
    </div>
    <!-- ==================== 工单列表 ==================== -->
    <el-table :data="list" v-loading="loading" border stripe>
      <el-table-column prop="id" label="工单编号" width="150" show-overflow-tooltip />
      <el-table-column prop="title" label="标题" min-width="140" show-overflow-tooltip />
      <el-table-column prop="deviceName" label="设备" width="130" show-overflow-tooltip />
      <el-table-column label="需求情况" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="needType(row.needType)" size="small">{{ row.needType || '蓝' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="150" align="center">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)" size="small" class="status-tag">
            <div class="st-main">
              {{ statusText(row.status) }}
              <el-tag v-if="row.overdue === 1 && row.status === '1'" type="danger" size="small" class="overdue-tag">超时</el-tag>
            </div>
            <div v-if="row.rejectCount > 0" class="st-sub">{{ rejectText(row.rejectCount) }}</div>
            <!-- ★ 我的拒接次数：仅拒接过该单的运维自己可见（后端只给当前登录人填值，其他人恒 0） -->
            <div v-if="row.myRejectCount > 0" class="st-sub">{{ rejectAssignText(row.myRejectCount) }}</div>
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
      <!-- ★ 操作列：按 角色+归属+状态 三要素控制 -->
      <el-table-column label="操作" width="380" fixed="right">
        <template #default="{ row }">
          <el-button size="small" type="primary" plain @click="openDetail(row)">详情</el-button>
          <el-button v-if="canChat(row)" size="small" type="success" plain @click="openChat(row)">对话</el-button>
          <el-button v-if="canRequest(row)" size="small" type="warning" plain @click="openRequest(row)">请求</el-button>
          <el-button v-if="canEdit(row)" size="small" type="info" plain @click="openEdit(row)">编辑</el-button>
          <el-button v-if="canAccept(row)" size="small" type="warning" plain @click="openAccept(row)">接单</el-button>
          <el-button v-if="canAssign(row) && row.status === '1'" size="small" type="primary" plain @click="openAssign(row)">指派</el-button>
          <el-button v-if="canAssign(row) && row.status === '2'" size="small" type="primary" plain @click="openAssign(row)">重派</el-button>
          <el-button v-if="canProcess(row)" size="small" type="primary" plain @click="openProcess(row)">处理</el-button>
          <el-button v-if="canReview(row)" size="small" type="success" plain @click="openReview(row)">验收</el-button>
          <!-- ★ 取消请求/取消接单：待确认或处理中均可取消 -->
          <el-button v-if="canCancelAssign(row)" size="small" type="warning" plain @click="doCancelAssign(row)">
            {{ row.status === '1' ? '取消请求' : '取消接单' }}
          </el-button>
          <el-button v-if="canClose(row)" size="small" type="info" plain @click="doAction(row, 'close')">关闭</el-button>
          <el-button v-if="canReopen(row)" size="small" type="info" plain @click="doAction(row, 'reopen')">重开</el-button>
          <!-- ★ 报废：管理员全权；运维仅待复核（已提交维修方案）的单 -->
          <el-button v-if="canScrap(row)" size="small" type="danger" plain @click="handleScrap(row)">报废</el-button>
          <el-button v-if="canDelete(row)" size="small" type="danger" plain @click="handleDelete(row)">删除</el-button>
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
    <!-- ==================== 新建工单弹窗 ==================== -->
    <el-dialog v-model="createVisible" title="新建工单" width="600px" destroy-on-close>
      <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-width="90px">
        <el-form-item label="设备" prop="deviceId">
          <el-select v-model="createForm.deviceId" placeholder="输入名称/编号搜索设备" filterable remote
                     :remote-method="searchDevices" :loading="deviceLoading" style="width: 100%">
            <el-option v-for="d in deviceOptions" :key="d.id" :label="`${d.deviceName} - ${d.deviceCode}`" :value="d.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="标题" prop="title">
          <el-input v-model="createForm.title" placeholder="故障描述标题" />
        </el-form-item>
        <el-form-item label="故障描述">
          <el-input v-model="createForm.description" type="textarea" :rows="2" placeholder="详细描述故障现象" />
        </el-form-item>
        <el-form-item label="需求情况">
          <el-radio-group v-model="createForm.needType">
            <el-radio value="红">红（紧急）</el-radio>
            <el-radio value="蓝">蓝（一般）</el-radio>
            <el-radio value="绿">绿（宽松）</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="指定运维">
          <el-select v-model="createForm.assigneeId" placeholder="不指定（进待接单池）" clearable style="width: 100%"
                     @change="onSelectOperator">
            <el-option v-for="o in operatorOptions" :key="o.id" :label="operatorLabel(o)" :value="o.id" />
          </el-select>
        </el-form-item>
        <template v-if="createForm.assigneeId">
          <el-form-item label="预约排期" required>
            <SchedulePicker :operator-id="createForm.assigneeId"
                            v-model:date="createForm.appointDate"
                            v-model:timeSlot="createForm.timeSlot" />
          </el-form-item>
        </template>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="createLoading" @click="handleCreate">提交</el-button>
      </template>
    </el-dialog>
    <!-- ==================== 工单详情弹窗 ==================== -->
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
    <!-- ==================== 工单沟通弹窗 ==================== -->
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
        <el-input v-model="chatText" type="textarea" :rows="2" placeholder="与对方沟通维修进度 / 问题" />
        <el-button type="primary" :loading="chatLoading" @click="handleSendChat">发送</el-button>
      </div>
    </el-dialog>
    <!-- ==================== 接单确认弹窗（工单详情 + 排期日历） ==================== -->
    <el-dialog v-model="acceptVisible" :title="acceptTitle" width="620px" destroy-on-close>
      <div class="accept-info" v-if="acceptDetail">
        <div class="accept-info-title">
          <span class="ai-label">{{ acceptDetail.title || '-' }}</span>
          <el-tag :type="needType(acceptDetail.needType)" size="small">{{ acceptDetail.needType || '蓝' }}</el-tag>
          <el-tag :type="priorityType(acceptDetail.priority)" size="small" effect="plain">{{ priorityText(acceptDetail.priority) }}</el-tag>
          <el-tag :type="statusType(acceptDetail.status)" size="small">{{ statusText(acceptDetail.status) }}</el-tag>
        </div>
        <div class="accept-info-grid">
          <div class="ai-item"><span class="ai-key">设备</span>{{ acceptDetail.deviceName || '-' }}</div>
          <div class="ai-item"><span class="ai-key">报修人</span>{{ acceptDetail.reporterName || '-' }}</div>
          <div class="ai-item"><span class="ai-key">电话</span>{{ acceptDetail.reporterPhone || '-' }}</div>
          <div class="ai-item"><span class="ai-key">位置</span>{{ acceptDetail.location || '-' }}</div>
        </div>
        <div class="accept-info-desc" v-if="acceptDetail.description">
          <span class="ai-key">故障描述</span>{{ acceptDetail.description }}
        </div>
      </div>
      <el-form label-width="90px">
        <el-form-item label="预约排期" required>
          <SchedulePicker v-if="acceptVisible" mine :key="'accept'"
                          v-model:date="acceptForm.appointDate"
                          v-model:timeSlot="acceptForm.timeSlot" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="acceptVisible = false">取消</el-button>
        <el-button type="primary" :loading="actionLoading" @click="handleAccept">确认接单</el-button>
      </template>
    </el-dialog>
    <!-- ==================== 请求 / 指派弹窗 ==================== -->
    <el-dialog v-model="assignVisible" :title="assignTitle" width="560px" destroy-on-close>
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
          <div v-else class="cost-tip">请先选择处理人员，再查看其排期</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="assignVisible = false">取消</el-button>
        <el-button type="primary" :loading="actionLoading" @click="handleAssign">确认{{ isAssign ? '指派' : '请求' }}</el-button>
      </template>
    </el-dialog>
    <!-- ==================== 编辑工单弹窗 ==================== -->
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
    <!-- ==================== 处理（录入维修方案）弹窗 ==================== -->
    <el-dialog v-model="processVisible" title="录入维修方案" width="500px">
      <el-form label-width="90px">
        <el-form-item label="维修方案" required>
          <el-input v-model="processForm.plan" type="textarea" :rows="4" placeholder="填写维修方案 / 处理结果" />
        </el-form-item>
        <el-form-item label="维修费用">
          <el-input-number v-model="processForm.repairCost" :min="0" :precision="2" style="width: 100%" />
          <div class="cost-tip">元（如无费用可填 0）</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="processVisible = false">取消</el-button>
        <el-button type="primary" :loading="actionLoading" @click="handleProcess">提交</el-button>
      </template>
    </el-dialog>
    <!-- ==================== 验收评价弹窗 ==================== -->
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
import { Search, Refresh, Plus } from '@element-plus/icons-vue'
import StatusTag from '../../components/StatusTag.vue'
import SchedulePicker from '../../components/SchedulePicker.vue'
import { useUserStore } from '../../store/user'
import {
  getWorkOrderPage, getWorkOrderDetail, getWorkOrderRecord,
  createWorkOrder, editWorkOrder, acceptWorkOrder, assignWorkOrder,
  cancelAssignWorkOrder, getRejectedOperators,
  submitReviewWorkOrder, reviewWorkOrder, reopenWorkOrder, closeWorkOrder,
  deleteWorkOrder, scrapWorkOrder, getOperators,
  getOrderMessages, sendOrderMessage
} from '../../api/workorder'
import { getDevicePage } from '../../api/device'
const userStore = useUserStore()
// ★ 用户 id：19 位大数，必须【字符串】比较，Number() 会丢精度导致按钮全部消失
const myIdStr = computed(() => String(userStore.userId ?? userStore.id ?? ''))
const isAdmin = computed(() => userStore.roles.includes('super_admin') || userStore.roles.includes('manager'))
const isOperator = computed(() => userStore.roles.includes('operator'))
// ★ 纯运维（非管理员）：工单管理 = 接单大厅，强制只看待接单
const isOperatorOnly = computed(() => isOperator.value && !isAdmin.value)
// ★ 员工（非管理员非运维）：工单管理 = 待接单池 + 自己的单（自己的可编辑/删除，别人的只看详情）
const isEmployeeOnly = computed(() => !isAdmin.value && !isOperator.value)
// ==================== 按钮显示矩阵（角色 ∩ 归属 ∩ 状态） ====================
// ==================== 按钮显示矩阵（角色 ∩ 归属 ∩ 状态） ====================
// ★ 对话：待接单/未指派完全不给入口；必须有处理人；管理员看全部，报修人/处理人限本人

// ★ 对话：待接单/未指派完全不给入口；必须有处理人；管理员看全部，报修人/处理人限本人
const canChat = (row) =>
  row.status !== '1' && !!row.assigneeId &&
  (isAdmin.value || String(row.reporterId) === myIdStr.value || String(row.assigneeId) === myIdStr.value)
const canRequest = (row) => String(row.reporterId) === myIdStr.value && row.status === '1' && !row.assigneeId
const canEdit = (row) => String(row.reporterId) === myIdStr.value && row.status === '1'
// ★ 接单：待接单 + 运维/管理 + 【不是自己报修的】（防假单）
const canAccept = (row) => row.status === '1' && (isAdmin.value || isOperator.value) && String(row.reporterId) !== myIdStr.value
const canAssign = (row) => (row.status === '1' || row.status === '2') && isAdmin.value
const canProcess = (row) => row.status === '2' && (isAdmin.value || String(row.assigneeId) === myIdStr.value)
const canReview = (row) => row.status === '3' && (isAdmin.value || String(row.reporterId) === myIdStr.value)
// ★ 取消请求/取消接单：报修人自己的单，待确认(1)或处理中(2)且已指派
const canCancelAssign = (row) => String(row.reporterId) === myIdStr.value && row.assigneeId && (row.status === '1' || row.status === '2')
const canClose = (row) => row.status === '4' && isAdmin.value
const canReopen = (row) => row.status === '6' && isAdmin.value
// ★ 报废：管理员全权；运维仅【自己接单 + 待复核】的单
const canScrap = (row) => isAdmin.value || (String(row.assigneeId) === myIdStr.value && row.status === '3')
const canDelete = (row) => isAdmin.value || String(row.reporterId) === myIdStr.value
// ==================== 展示常量与格式化 ====================
const statusOptions = { '0': '待提交', '1': '待接单', '2': '处理中', '3': '待复核', '4': '已完成', '5': '已关闭', '6': '已驳回', '7': '已报废' }
const statusType = (s) => ({ '4': 'success', '5': 'info', '6': 'danger', '3': 'warning', '2': 'primary', '7': 'danger' }[s] || 'info')
const needType = (t) => ({ 红: 'danger', 蓝: 'primary', 绿: 'success' }[t] || 'primary')
const statusText = (s) => statusOptions[s] || s || '-'
const priorityText = (p) => ({ 1: '低', 2: '中', 3: '高', 4: '紧急' }[p] || '-')
const priorityType = (p) => ({ 1: 'info', 2: 'primary', 3: 'warning', 4: 'danger' }[p] || 'info')
const rejectText = (n) => {
  const c = n == null ? 0 : Number(n)
  return c > 999 ? '已驳回999+次' : `已驳回${c}次`
}
// ★ 我的拒接次数文案（与"已驳回"同款）
const rejectAssignText = (n) => {
  const c = n == null ? 0 : Number(n)
  return c > 999 ? '拒接999+次' : `拒接${c}次`
}
const fmtTime = (t) => (t ? String(t).replace('T', ' ').slice(0, 19) : '-')
const weekMap = { 1: '周一', 2: '周二', 3: '周三', 4: '周四', 5: '周五', 6: '周六', 7: '周日' }
// ★ 工作日显示排序：1-7 顺序
const workDaysText = (wd) => {
  if (!wd) return '未设置'
  return String(wd).split(',').filter(Boolean).map(Number).sort((a, b) => a - b).map((s) => weekMap[s] || '').join('、')
}
const operatorLabel = (o) => `${o.nickname || o.username || o.id}（工作日：${workDaysText(o.workDays)}）`
// ==================== 工单列表查询 ====================
const list = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({
  pageNum: 1,
  pageSize: 10,
  status: null,
  keywordNo: '',
  keywordTitle: '',
  keywordDesc: ''
})
/** 加载工单分页列表：按角色区分数据范围 */
const loadData = async () => {
  loading.value = true
  try {
    const params = { pageNum: query.pageNum, pageSize: query.pageSize, sortByStatus: true }
    if (isEmployeeOnly.value) {
      // ★ 用户：待接单池 ∪ 自己报修的全部（后端 employeePool）
      params.status = '1'
    } else if (isOperatorOnly.value) {
      // ★ 运维：接单大厅，强制待接单；隐藏"被请求给我"的单（在我的接单里接/不接）
      params.status = '1'
      params.excludeMyAssigned = true
    } else if (query.status !== null && query.status !== '') {
      // ★ 管理员：按状态筛选
      params.status = query.status
    }
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
  query.status = null
  query.keywordNo = ''
  query.keywordTitle = ''
  query.keywordDesc = ''
  loadData()
}
// ==================== 新建工单 ====================
const createVisible = ref(false)
const createLoading = ref(false)
const createFormRef = ref()
const deviceOptions = ref([])
const deviceLoading = ref(false)
const operatorOptions = ref([])
const createForm = reactive({ deviceId: null, title: '', description: '', needType: '蓝', assigneeId: null, appointDate: '', timeSlot: '' })
const createRules = {
  deviceId: [{ required: true, message: '请选择设备', trigger: 'change' }],
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }]
}
const searchDevices = async (kw) => {
  deviceLoading.value = true
  try {
    const data = await getDevicePage({ pageNum: 1, pageSize: 20, keyword: kw || undefined, excludeProcessing: true })
    deviceOptions.value = data.records || []
  } finally {
    deviceLoading.value = false
  }
}
const openCreate = async () => {
  createVisible.value = true
  Object.assign(createForm, { deviceId: null, title: '', description: '', needType: '蓝', assigneeId: null, appointDate: '', timeSlot: '' })
  await searchDevices('')
  const data = await getOperators()
  // ★ 运维建单排除自己（字符串比较，防精度丢失）
  operatorOptions.value = (data || []).filter((o) => String(o.id) !== myIdStr.value)
}
const handleCreate = async () => {
  try {
    await createFormRef.value.validate()
  } catch (e) {
    return
  }
  if (createForm.assigneeId) {
    if (!createForm.appointDate) return ElMessage.warning('请选择预约日期')
    if (!createForm.timeSlot) return ElMessage.warning('请选择时间段')
  }
  createLoading.value = true
  try {
    const selected = operatorOptions.value.find((o) => String(o.id) === String(createForm.assigneeId))
    await createWorkOrder({
      deviceId: createForm.deviceId,
      title: createForm.title,
      description: createForm.description,
      needType: createForm.needType,
      assigneeId: createForm.assigneeId || null,
      assigneeName: selected ? selected.nickname : null,
      appointDate: createForm.appointDate || null,
      timeSlot: createForm.timeSlot || null
    })
    ElMessage.success('工单创建成功')
    createVisible.value = false
    loadData()
  } finally {
    createLoading.value = false
  }
}
const onSelectOperator = () => {
  createForm.appointDate = ''
  createForm.timeSlot = ''
  assignForm.appointDate = ''
  assignForm.timeSlot = ''
}
// ==================== 工单详情 ====================
const detailVisible = ref(false)
const detail = ref(null)
const records = ref([])
const openDetail = async (row) => {
  detailVisible.value = true
  detail.value = await getWorkOrderDetail(row.id)
  records.value = await getWorkOrderRecord(row.id)
}
// ==================== 工单沟通 ====================
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
// ==================== 接单确认 ====================
const acceptVisible = ref(false)
const acceptDetail = ref(null)
const acceptForm = reactive({ appointDate: '', timeSlot: '' })
const acceptTitle = ref('接单确认')
let acceptRow = null
const openAccept = async (row) => {
  acceptRow = row
  // ★ 被请求给我 → 确认接单；别人 → 抢单接单
  acceptTitle.value = String(row.assigneeId) === myIdStr.value ? '确认接单' : '抢单接单'
  acceptForm.appointDate = ''
  acceptForm.timeSlot = ''
  try {
    acceptDetail.value = await getWorkOrderDetail(row.id)
    acceptVisible.value = true
  } catch (e) {
    ElMessage.error('加载工单详情失败，请重试')
  }
}
const handleAccept = async () => {
  if (!acceptForm.appointDate) return ElMessage.warning('请选择处理日期')
  if (!acceptForm.timeSlot) return ElMessage.warning('请选择时间段')
  actionLoading.value = true
  try {
    await acceptWorkOrder(acceptRow.id, { appointDate: acceptForm.appointDate, timeSlot: acceptForm.timeSlot })
    ElMessage.success('接单成功')
    acceptVisible.value = false
    loadData()
  } catch (e) {
    acceptForm.appointDate = ''
    acceptForm.timeSlot = ''
  } finally {
    actionLoading.value = false
  }
}
// ==================== 请求 / 指派 ====================
const assignVisible = ref(false)
const assignForm = reactive({ handlerId: null, appointDate: '', timeSlot: '' })
const assignTitle = ref('')
const isAssign = ref(true)
let assignRow = null
const openRequest = async (row) => {
  assignRow = row
  isAssign.value = false
  assignTitle.value = '请求指定运维'
  Object.assign(assignForm, { handlerId: null, appointDate: '', timeSlot: '' })
  assignVisible.value = true
  const rejected = await getRejectedOperators(row.id)
  const data = await getOperators()
  // ★ 字符串比较（防精度丢失）；同时排除拒过该单的运维
  operatorOptions.value = (data || []).filter((o) => !rejected.includes(String(o.id)) && String(o.id) !== myIdStr.value)
}
const openAssign = async (row) => {
  assignRow = row
  isAssign.value = true
  assignTitle.value = row.status === '2' ? '重新派遣处理人员' : '指派处理人员'
  Object.assign(assignForm, { handlerId: row.assigneeId || null, appointDate: '', timeSlot: '' })
  assignVisible.value = true
  const data = await getOperators()
  operatorOptions.value = data || []
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
    ElMessage.success(isAssign.value ? '指派成功，待运维确认' : '请求成功，待运维确认')
    assignVisible.value = false
    loadData()
  } finally {
    actionLoading.value = false
  }
}
// ==================== 编辑工单 ====================
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
// ==================== 处理（提交复核） ====================
const processVisible = ref(false)
const processForm = reactive({ plan: '', repairCost: 0 })
let processRow = null
const openProcess = (row) => {
  processRow = row
  processForm.plan = ''
  processForm.repairCost = 0
  processVisible.value = true
}
const handleProcess = async () => {
  if (!processForm.plan) return ElMessage.warning('请填写维修方案')
  actionLoading.value = true
  try {
    await submitReviewWorkOrder(processRow.id, { repairContent: processForm.plan, repairCost: processForm.repairCost })
    ElMessage.success('处理完成，已流转到待复核')
    processVisible.value = false
    loadData()
  } finally {
    actionLoading.value = false
  }
}
// ==================== 验收评价 ====================
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
// ==================== 一键动作（关闭 / 重开） ====================
const actionLoading = ref(false)
const doAction = async (row, action) => {
  const map = { close: closeWorkOrder, reopen: reopenWorkOrder }
  const text = { close: '关闭', reopen: '重新打开' }
  await ElMessageBox.confirm(`确定执行【${text[action]}】操作吗？`, '提示', { type: 'warning' })
  actionLoading.value = true
  try {
    await map[action](row.id)
    ElMessage.success('操作成功')
    loadData()
  } finally {
    actionLoading.value = false
  }
}
// ==================== 报修人取消请求 / 取消接单 ====================
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
// ==================== 设备报废 ====================
const handleScrap = async (row) => {
  await ElMessageBox.confirm(
    `确定报废工单「${row.title}」对应的设备吗？\n报废后该设备不可再报修，设备上所有在途工单将一并关闭。`,
    '报废确认',
    { type: 'warning' }
  )
  await scrapWorkOrder(row.id)
  ElMessage.success('报废成功，设备已标记报废')
  loadData()
}
// ==================== 删除工单 ====================
const handleDelete = async (row) => {
  await ElMessageBox.confirm(`确定删除工单「${row.title}」吗？删除后所有角色同步删除该工单。`, '提示', { type: 'warning' })
  await deleteWorkOrder(row.id)
  ElMessage.success('删除成功')
  loadData()
}
onMounted(loadData)
</script>
<style scoped>
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
.accept-info {
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  padding: 10px 12px;
  margin-bottom: 14px;
  background: #fafafa;
}
.accept-info-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
  flex-wrap: wrap;
}
.ai-label {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}
.accept-info-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 4px 16px;
  font-size: 13px;
  color: #606266;
}
.ai-item {
  line-height: 1.7;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.ai-key {
  display: inline-block;
  width: 56px;
  color: #909399;
  margin-right: 4px;
  flex-shrink: 0;
}
.accept-info-desc {
  margin-top: 6px;
  font-size: 13px;
  color: #606266;
  line-height: 1.6;
  background: #f0f2f5;
  border-radius: 4px;
  padding: 6px 8px;
  max-height: 64px;
  overflow-y: auto;
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
