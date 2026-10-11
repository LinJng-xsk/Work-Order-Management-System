<template>
  <div>
    <el-card class="mb-16" shadow="never">
      <div class="chart-header">
        <span class="chart-title">月度工单总量统计</span>
        <el-date-picker
          v-model="year"
          type="year"
          placeholder="选择年份"
          value-format="YYYY"
          :clearable="false"
          :disabled-date="disableFuture"
          style="width: 120px"
          @change="onYearChange"
        />
      </div>
      <!-- ★ 标注：说明当月数据不完整的口径 -->
      <div class="chart-tip">注：当月数据统计至今日，次月月初才显示上个月的完整月度数据</div>
      <div ref="monthlyRef" class="chart"></div>
      <!-- ★ 点击月份柱子后展示的派单日历 -->
      <div v-if="detailVisible" class="month-detail">
        <div class="detail-title">{{ selectedMonthLabel }} 派单明细（点击上方柱状图可切换月份）</div>
        <div class="cal-grid">
          <div v-for="d in days" :key="d" class="cal-day">
            <div class="day-num" :class="{ active: dayMap[d] }">{{ d }}</div>
            <template v-if="dayMap[d]">
              <div class="mark-dot"></div>
              <div class="mark-names">{{ dayMap[d].join('、') }}</div>
            </template>
            <div v-else class="mark-none">—</div>
          </div>
        </div>
      </div>
    </el-card>
    <el-row :gutter="16">
      <el-col :span="12">
        <el-card shadow="never">
          <div class="chart-title">设备故障率分析</div>
          <div ref="faultRateRef" class="chart"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="never">
          <div class="chart-title">运维人员工作量排行</div>
          <div ref="workloadRef" class="chart"></div>
        </el-card>
      </el-col>
    </el-row>
    <el-card class="mt-16" shadow="never">
      <div class="chart-title">故障设备 TOP 排名</div>
      <div ref="faultTopRef" class="chart"></div>
    </el-card>
  </div>
</template>
<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import * as echarts from 'echarts'
import {
  getMonthlyStats,
  getMonthlyDetail,
  getDeviceFaultRate,
  getWorkerWorkload,
  getFaultTop
} from '../api/stats'

const today = new Date()
// ★ 年份选择器，默认今年
const year = ref(String(today.getFullYear()))
// ★ 禁止选择未来年份（2027 及以后置灰）
const disableFuture = (t) => t.getTime() > Date.now()

const monthlyRef = ref()
const faultRateRef = ref()
const workloadRef = ref()
const faultTopRef = ref()
let charts = []
// 同一 DOM 重复 init 会报错，先查已有实例直接复用
const initChart = (el) => {
  const existing = echarts.getInstanceByDom(el)
  if (existing) {
    return existing
  }
  const chart = echarts.init(el)
  charts.push(chart)
  return chart
}
// ===== 月度柱状图 =====
const loadMonthly = async () => {
  const params = { startTime: `${year.value}-01-01T00:00:00` }
  const data = await getMonthlyStats(params)
  // 后端只返回有数据的月份，补全 12 个月
  const map = {}
  ;(data || []).forEach((d) => {
    map[d.month] = d.total
  })
  const months = Array.from({ length: 12 }, (_, i) => {
    const m = String(i + 1).padStart(2, '0')
    return { label: `${year.value}-${m}月`, count: map[`${year.value}-${m}`] || 0 }
  })
  const chart = initChart(monthlyRef.value)
  chart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 40, right: 20, top: 30, bottom: 30 },
    xAxis: {
      type: 'category',
      data: months.map((m) => m.label)
    },
    yAxis: { type: 'value', minInterval: 1 },
    series: [
      {
        name: '工单数',
        type: 'bar',
        barMaxWidth: 36,
        itemStyle: { color: '#409eff', borderRadius: [4, 4, 0, 0] },
        data: months.map((m) => m.count)
      }
    ]
  })
  // ★ 点击某月柱子 → 展示该月派单日历
  chart.off('click')
  chart.on('click', (params) => {
    const m = /(\d{4})-(\d{2})月/.exec(params.name)
    if (m) {
      loadMonthlyDetail(m[1], Number(m[2]))
    }
  })
}
// ===== 点击月份后的派单日历 =====
const detailVisible = ref(false)
const selectedMonthLabel = ref('')
const dayMap = ref({})
const days = ref([])
const loadMonthlyDetail = async (y, month) => {
  const mm = String(month).padStart(2, '0')
  selectedMonthLabel.value = `${y}年${month}月`
  const data = await getMonthlyDetail({ month: `${y}-${mm}` })
  const map = {}
  ;(data || []).forEach((d) => {
    if (!map[d.day]) {
      map[d.day] = []
    }
    // ★ 处理人为空时显示"未指派"
    map[d.day].push(`${d.assigneeName || '未指派'}(${d.cnt})${d.timeSlot ? ' ' + d.timeSlot : ''}`)
  })
  dayMap.value = map
  days.value = Array.from({ length: new Date(y, month, 0).getDate() }, (_, i) =>
    String(i + 1).padStart(2, '0')
  )
  detailVisible.value = true
}
// 切换年份：刷新柱状图并收起日历（等点击柱子再看明细）
const onYearChange = () => {
  detailVisible.value = false
  loadMonthly()
}
// ===== 设备故障率饼图（★ 字段：deviceName / faultRate）=====
const loadFaultRate = async () => {
  const data = await getDeviceFaultRate()
  const chart = initChart(faultRateRef.value)
  chart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c}%' },
    legend: { bottom: 0, type: 'scroll' },
    // ★ 环形图中心：只显示设备总数，不再堆叠设备名
    title: {
      text: '设备总数\n' + ((data || []).length || 0) + '台',
      left: 'center',
      top: 'middle',
      textStyle: { fontSize: 14, fontWeight: 600, color: '#1f2937', lineHeight: 22 }
    },
    series: [
      {
        name: '设备故障率',
        type: 'pie',
        radius: ['40%', '65%'],
        // ★ label 显式外侧引线，避免设备名叠到圆环中心
        label: { show: true, formatter: '{b}', position: 'outside' },
        labelLine: { show: true },
        data: (data || []).map((d) => ({
          name: d.deviceName || '未知设备',
          value: Number(d.faultRate) || 0
        }))
      }
    ]
  })
}

// ===== 运维工作量横向条形图（★ 字段：workerName / finishedCount / avgHandleHours）=====
const loadWorkload = async () => {
  const data = await getWorkerWorkload()
  const chart = initChart(workloadRef.value)
  chart.setOption({
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      // ★ 悬停显示：数量 + 平均处理时长
      formatter: (params) => {
        const idx = params && params[0] ? params[0].dataIndex : -1
        if (idx < 0) return ''
        const d = (data || [])[idx]
        if (!d) return ''
        return `${d.workerName || '未知'}<br/>处理工单数：${d.finishedCount ?? 0} 单<br/>平均处理时长：${d.avgHandleHours == null ? '-' : d.avgHandleHours + ' 小时'}`
      }
    },
    grid: { left: 80, right: 30, top: 20, bottom: 30 },
    xAxis: { type: 'value', minInterval: 1 },
    yAxis: {
      type: 'category',
      data: (data || []).map((d) => d.workerName || '未知')
    },
    series: [
      {
        name: '处理工单数',
        type: 'bar',
        itemStyle: { color: '#67c23a' },
        data: (data || []).map((d) => d.finishedCount || 0)
      }
    ]
  })
}
// ===== 故障设备 TOP 排行（★ 字段：deviceName / faultCount）=====
const loadFaultTop = async () => {
  const data = await getFaultTop()
  const chart = initChart(faultTopRef.value)
  chart.setOption({
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: 100, right: 30, top: 20, bottom: 30 },
    xAxis: { type: 'value', minInterval: 1 },
    yAxis: {
      type: 'category',
      inverse: true,
      data: (data || []).map((d) => d.deviceName || '未知设备')
    },
    series: [
      {
        name: '故障次数',
        type: 'bar',
        itemStyle: { color: '#e6a23c' },
        data: (data || []).map((d) => d.faultCount || 0)
      }
    ]
  })
}
const handleResize = () => charts.forEach((c) => c.resize())
onMounted(async () => {
  loadMonthly()
  loadFaultRate()
  loadWorkload()
  loadFaultTop()
  window.addEventListener('resize', handleResize)
})
onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  charts.forEach((c) => c.dispose())
  charts = []
})
</script>
<style scoped>
.chart {
  height: 320px;
}
.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}
.chart-title {
  font-size: 15px;
  font-weight: 600;
  color: #1f2937;
  margin-bottom: 8px;
}
/* ★ 当月数据口径标注 */
.chart-tip {
  font-size: 12px;
  color: #909399;
  margin: -4px 0 6px;
}
.mb-16 {
  margin-bottom: 16px;
}
.mt-16 {
  margin-top: 16px;
}
/* ===== 派单日历 ===== */
.month-detail {
  margin-top: 12px;
  padding: 12px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  background: #fafafa;
}
.detail-title {
  font-size: 14px;
  font-weight: 600;
  color: #1f2937;
  margin-bottom: 10px;
}
.cal-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 8px;
}
.cal-day {
  min-height: 64px;
  padding: 6px;
  border: 1px solid #e5e7eb;
  border-radius: 4px;
  background: #fff;
  text-align: center;
}
.day-num {
  font-size: 13px;
  color: #6b7280;
  line-height: 1.6;
}
.day-num.active {
  font-weight: 700;
  color: #409eff;
}
.mark-dot {
  width: 8px;
  height: 8px;
  margin: 4px auto;
  border-radius: 50%;
  background: #f56c6c;
}
.mark-names {
  font-size: 11px;
  color: #f56c6c;
  line-height: 1.4;
  word-break: break-all;
}
.mark-none {
  font-size: 12px;
  color: #d1d5db;
}
</style>
