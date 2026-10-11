<template>
  <div class="profile-page">
    <!-- 基本信息 -->
    <el-card class="p-card">
      <template #header>
        <div class="p-header">
          <span>个人资料</span>
          <el-tag v-if="isOperator" type="warning">运维人员</el-tag>
          <el-tag v-else-if="isEmployee" type="success">用户</el-tag>
          <el-tag v-else type="danger">管理角色</el-tag>
        </div>
      </template>
      <el-form :model="info" label-width="90px" style="max-width: 460px">
        <el-form-item label="用户名">
          <el-input :model-value="info.username" disabled />
        </el-form-item>
        <el-form-item label="昵称">
          <el-input v-model="info.nickname" placeholder="请输入昵称" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="info.phone" placeholder="请输入手机号" maxlength="11" />
        </el-form-item>
        <el-form-item v-if="isOperator" label="工作日">
          <el-checkbox-group v-model="workDaysArr">
            <el-checkbox v-for="(d, i) in WEEK_LABELS" :key="i" :value="String(i + 1)">{{ d }}</el-checkbox>
          </el-checkbox-group>
        </el-form-item>
        <el-form-item label="创建时间">
          <el-input :model-value="formatTime(info.createTime)" disabled />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="saving" @click="handleSave">保存修改</el-button>
          <el-button v-if="canDelete" type="danger" plain @click="handleDelete">注销账户</el-button>
        </el-form-item>
      </el-form>
    </el-card>
    <!-- 资金：运维 -->
    <el-card v-if="isOperator" class="p-card">
      <template #header>我的资金（工单维修费收入）</template>
      <div class="stat-row">
        <div class="stat-box">
          <div class="stat-num">¥{{ fmt(overview.totalIncome) }}</div>
          <div class="stat-label">累计赚取</div>
        </div>
        <div class="stat-box">
          <div class="stat-num">¥{{ fmt(overview.monthIncome) }}</div>
          <div class="stat-label">本月收入</div>
        </div>
        <div class="stat-box">
          <div class="stat-num">{{ overview.monthOrders }}</div>
          <div class="stat-label">本月接单数</div>
        </div>
      </div>
      <!-- ★ 图表：近30天每日到账 -->
      <el-divider content-position="left">近30天每日到账</el-divider>
      <div ref="chartRef" class="p-chart"></div>
      <el-divider content-position="left">每日到账明细（最近30天）</el-divider>
      <el-table :data="daily.records" stripe size="small">
        <el-table-column prop="finishTime" label="到账时间" width="180">
          <template #default="{ row }">{{ formatTime(row.finishTime) }}</template>
        </el-table-column>
        <el-table-column prop="orderNo" label="工单号" min-width="200" />
        <el-table-column prop="title" label="工单标题" min-width="140" />
        <el-table-column prop="repairCost" label="到账金额" width="110">
          <template #default="{ row }">¥{{ fmt(row.repairCost) }}</template>
        </el-table-column>
      </el-table>
      <el-pagination
        v-model:current-page="dailyPage"
        :page-size="daily.size"
        :total="daily.total"
        layout="total, prev, pager, next"
        class="p-pager"
        @current-change="loadDaily"
      />
    </el-card>
    <!-- 资金：用户押金 -->
    <el-card v-else-if="isEmployee" class="p-card">
      <template #header>我的押金</template>
      <div class="stat-row">
        <div class="stat-box">
          <div class="stat-num">¥{{ fmt(deposit.pendingDeposit) }}</div>
          <div class="stat-label">在途押金</div>
        </div>
        <div class="stat-box">
          <div class="stat-num">¥{{ fmt(deposit.refundedDeposit) }}</div>
          <div class="stat-label">已退回押金</div>
        </div>
        <div class="stat-box">
          <div class="stat-num">¥{{ fmt(deposit.totalDeposit) }}</div>
          <div class="stat-label">累计缴纳押金</div>
        </div>
      </div>
      <!-- ★ 图表：押金构成 -->
      <el-divider content-position="left">押金构成</el-divider>
      <div ref="pieRef" class="p-chart"></div>
      <el-alert
        type="info"
        :closable="false"
        title="提交工单时缴纳押金，工单验收完成 / 关闭 / 报废后自动退回。"
        style="margin-top: 10px"
      />
    </el-card>
  </div>
</template>
<script setup>
import { ref, reactive, nextTick, onMounted, onBeforeUnmount } from 'vue'
import * as echarts from 'echarts'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRouter } from 'vue-router'
import { useUserStore } from '../store/user'
import {
  getMyInfo, updateMyInfo, deleteMyAccount,
  getFinanceOverview, getFinanceDaily, getDepositOverview
} from '../api/profile'
const router = useRouter()
const userStore = useUserStore()
const roles = userStore.roles || []
const isOperator = roles.includes('operator')
const isEmployee = roles.includes('employee')
const canDelete = isOperator || isEmployee
const WEEK_LABELS = ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
const info = reactive({ username: '', nickname: '', phone: '', workDays: '', createTime: '' })
const workDaysArr = ref([])
const saving = ref(false)
const overview = reactive({ totalIncome: 0, monthIncome: 0, monthOrders: 0 })
const daily = reactive({ records: [], total: 0, size: 10 })
const dailyPage = ref(1)
const deposit = reactive({ pendingDeposit: 0, refundedDeposit: 0, totalDeposit: 0 })
// ★ 图表
const chartRef = ref()
const pieRef = ref()
let chart = null
let pieChart = null
const fmt = (v) => (v === null || v === undefined ? '0.00' : Number(v).toFixed(2))
const formatTime = (t) => (t ? String(t).replace('T', ' ').slice(0, 19) : '-')
const initChart = (el) => {
  const existing = echarts.getInstanceByDom(el)
  if (existing) return existing
  return echarts.init(el)
}
const loadInfo = async () => {
  const data = await getMyInfo()
  Object.assign(info, data)
  if (data.workDays) {
    workDaysArr.value = String(data.workDays).split(',').filter(Boolean)
  }
}
const loadOverview = async () => {
  Object.assign(overview, await getFinanceOverview())
}
const loadDaily = async () => {
  const data = await getFinanceDaily({ pageNum: dailyPage.value, pageSize: daily.size })
  daily.records = data.records || []
  daily.total = data.total || 0
}
// ★ 运维折线图：拉最近30天全量（100 条内足够），按日期聚合
const loadIncomeChart = async () => {
  const data = await getFinanceDaily({ pageNum: 1, pageSize: 100 })
  await nextTick()
  if (!chartRef.value) return
  const map = {}
  ;(data.records || []).forEach((r) => {
    const day = r.finishTime ? String(r.finishTime).slice(0, 10) : null
    if (!day) return
    map[day] = (map[day] || 0) + Number(r.repairCost || 0)
  })
  const days = []
  const now = new Date()
  for (let i = 29; i >= 0; i--) {
    const d = new Date(now.getTime() - i * 86400000)
    const key = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
    days.push({
      label: `${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`,
      val: map[key] || 0
    })
  }
  chart = initChart(chartRef.value)
  chart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 50, right: 20, top: 30, bottom: 30 },
    xAxis: { type: 'category', data: days.map((d) => d.label), axisLabel: { interval: 4 } },
    yAxis: { type: 'value' },
    series: [
      {
        name: '到账金额',
        type: 'line',
        smooth: true,
        areaStyle: { opacity: 0.15 },
        itemStyle: { color: '#67c23a' },
        data: days.map((d) => d.val)
      }
    ]
  })
}
const loadDeposit = async () => {
  Object.assign(deposit, await getDepositOverview())
  await nextTick()
  if (!pieRef.value) return
  pieChart = initChart(pieRef.value)
  pieChart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: ¥{c}' },
    legend: { bottom: 0 },
    series: [
      {
        name: '押金',
        type: 'pie',
        radius: ['40%', '65%'],
        data: [
          { name: '在途押金', value: Number(deposit.pendingDeposit) || 0 },
          { name: '已退回押金', value: Number(deposit.refundedDeposit) || 0 }
        ]
      }
    ]
  })
}
const handleSave = async () => {
  if (!info.nickname) {
    ElMessage.warning('昵称不能为空')
    return
  }
  saving.value = true
  try {
    await updateMyInfo({
      nickname: info.nickname,
      phone: info.phone,
      workDays: isOperator ? [...workDaysArr.value].sort().join(',') : null
    })
    ElMessage.success('保存成功')
    // ★ 昵称已同步到历史工单，刷新右上角昵称
    userStore.fetchUserInfo && userStore.fetchUserInfo()
  } finally {
    saving.value = false
  }
}
const handleDelete = async () => {
  await ElMessageBox.confirm('注销后账号将无法登录，确定注销吗？', '注销账户', {
    type: 'warning',
    confirmButtonText: '确定注销',
    cancelButtonText: '取消'
  })
  await deleteMyAccount()
  ElMessage.success('账户已注销')
  userStore.logout()
  router.push('/login')
}
const handleResize = () => {
  chart && chart.resize()
  pieChart && pieChart.resize()
}
onMounted(async () => {
  await loadInfo()
  if (isOperator) {
    loadOverview()
    loadIncomeChart()
    loadDaily()
  } else if (isEmployee) {
    loadDeposit()
  }
  window.addEventListener('resize', handleResize)
})
onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  chart && chart.dispose()
  pieChart && pieChart.dispose()
})
</script>
<style scoped>
.profile-page {
  padding: 4px;
}
.p-card {
  margin-bottom: 16px;
}
.p-header {
  display: flex;
  align-items: center;
  gap: 10px;
}
.stat-row {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
}
.stat-box {
  flex: 1;
  min-width: 160px;
  background: #f8fafc;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 16px;
  text-align: center;
}
.stat-num {
  font-size: 24px;
  font-weight: 700;
  color: #2563eb;
}
.stat-label {
  margin-top: 6px;
  font-size: 13px;
  color: #6b7280;
}
/* ★ 图表 */
.p-chart {
  height: 260px;
  width: 100%;
}
.p-pager {
  margin-top: 12px;
  justify-content: flex-end;
}
</style>
