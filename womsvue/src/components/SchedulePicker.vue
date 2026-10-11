<template>
  <div class="sp-wrap">
    <div class="sp-head">
      <el-button size="small" text @click="prevMonth">&lt;</el-button>
      <span class="sp-title">{{ viewYear }}年{{ viewMonth + 1 }}月</span>
      <el-button size="small" text @click="nextMonth">&gt;</el-button>
    </div>
    <div class="sp-grid sp-grid-head">
      <div v-for="w in WEEK" :key="w" class="sp-week">{{ w }}</div>
    </div>
    <div class="sp-grid">
      <div v-for="(cell, i) in cells" :key="i" class="sp-cell" :class="{ 'sp-empty': !cell }">
        <template v-if="cell">
          <div class="sp-date" :class="{ 'sp-today': cell.dateStr === todayStr }">{{ cell.day }}</div>
          <div class="sp-slots">
            <span class="sp-slot" :class="slotClass(cell, SLOTS[0])" @click="pick(cell, SLOTS[0])">{{ SLOT_SHORT[0] }}</span>
            <span class="sp-slot" :class="slotClass(cell, SLOTS[1])" @click="pick(cell, SLOTS[1])">{{ SLOT_SHORT[1] }}</span>
          </div>
        </template>
      </div>
    </div>
    <!-- ★ 加载异常/为空时可见提示（正常时不显示） -->
    <div v-if="diag && (diag.includes('⚠️') || diag.includes('❌'))" class="sp-diag">{{ diag }}</div>
    <div class="sp-tip">
      <span class="sp-legend"><i class="sp-dot used"></i>已占用</span>
      <span class="sp-legend"><i class="sp-dot ok"></i>可选</span>
      <span class="sp-legend"><i class="sp-dot off"></i>非工作日/已过去</span>
    </div>
  </div>
</template>
<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { getOperatorSchedule, getMySchedule, getOperators } from '../api/workorder'
const props = defineProps({
  operatorId: { type: [Number, String], default: null },
  date: { type: String, default: '' },
  timeSlot: { type: String, default: '' },
  // ★ 接单弹窗：true = 查当前登录运维自己（后端识别，不需要 operatorId）
  mine: { type: Boolean, default: false }
})
const emit = defineEmits(['update:date', 'update:timeSlot'])
const WEEK = ['一', '二', '三', '四', '五', '六', '日']
const SLOTS = ['上午 9:00-11:00', '下午 14:30-17:00']
const SLOT_SHORT = ['上午', '下午']
const today = new Date()
const todayStr = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}-${String(today.getDate()).padStart(2, '0')}`
const viewYear = ref(today.getFullYear())
const viewMonth = ref(today.getMonth())
const schedule = ref({})
const workDays = ref(null)
const loading = ref(false)
const diag = ref('')
const pad = (n) => String(n).padStart(2, '0')
const dateStr = (y, m, d) => `${y}-${pad(m + 1)}-${pad(d)}`
const load = async () => {
  if (!props.mine && !props.operatorId) {
    diag.value = `⚠️ operatorId 无效（=${props.operatorId}），无法加载排期`
    schedule.value = {}
    workDays.value = null
    return
  }
  loading.value = true
  diag.value = props.mine ? '加载当前运维排期...' : `加载排期中（operatorId=${props.operatorId}）...`
  try {
    const lastDay = new Date(viewYear.value, viewMonth.value + 1, 0).getDate()
    const start = dateStr(viewYear.value, viewMonth.value, 1)
    const end = dateStr(viewYear.value, viewMonth.value, lastDay)
    try {
      let list = []
      if (props.mine) {
        // ★ mine：后端从 Token 识别当前用户，返回 schedule + workDays
        const raw = await getMySchedule(start, end)
        // ★ 兼容返回结构：{schedule,...} 或 {code,data:{schedule,...}}
        const payload = (raw && raw.schedule) ? raw : ((raw && raw.data) || {})
        list = (payload && payload.schedule) || []
        workDays.value = (payload && payload.workDays && payload.workDays.length) ? payload.workDays : null
      } else {
        list = await getOperatorSchedule(props.operatorId, start, end)
        try {
          const ops = await getOperators()
          const me = (ops || []).find((o) => Number(o.id) === Number(props.operatorId))
          workDays.value = me && me.workDays ? String(me.workDays).split(',').map((s) => Number(s.trim())).filter(Boolean) : null
        } catch (e) {
          workDays.value = null
        }
      }
      const map = {}
      ;(list || []).forEach((it) => {
        if (!it.date || !it.timeSlot) return
        if (!map[it.date]) map[it.date] = []
        if (!map[it.date].includes(it.timeSlot)) map[it.date].push(it.timeSlot)
      })
      schedule.value = map
      const n = (list || []).length
      diag.value = n > 0
        ? `✅ 排期 ${n} 条：${(list || []).map(x => x.date + ' ' + x.timeSlot).join('；')}`
        : `⚠️ 排期接口返回 0 条（${start}~${end}），请检查后端 schedule/mine 接口`
    } catch (e) {
      console.warn('[SchedulePicker] 排期请求失败', e)
      schedule.value = {}
      diag.value = `❌ 排期请求失败：${e && e.message ? e.message : e}`
    }
  } finally {
    loading.value = false
  }
}
watch(() => props.operatorId, load)
watch(() => props.mine, load)
watch([viewYear, viewMonth], load)
onMounted(load)
const cells = computed(() => {
  const first = new Date(viewYear.value, viewMonth.value, 1)
  const daysInMonth = new Date(viewYear.value, viewMonth.value + 1, 0).getDate()
  const lead = (first.getDay() + 6) % 7
  const tail = (7 - ((lead + daysInMonth) % 7)) % 7
  const arr = new Array(lead).fill(null)
  for (let d = 1; d <= daysInMonth; d++) {
    arr.push({ year: viewYear.value, month: viewMonth.value, day: d, dateStr: dateStr(viewYear.value, viewMonth.value, d) })
  }
  return arr.concat(new Array(tail).fill(null))
})
const isPast = (cell) => cell.dateStr < todayStr
const isWorkDay = (cell) => {
  if (!workDays.value) return true
  const d = new Date(cell.year, cell.month, cell.day)
  const dow = d.getDay() === 0 ? 7 : d.getDay()
  return workDays.value.includes(dow)
}
const sameSlot = (a, b) => (a || '').split(' ')[0] === (b || '').split(' ')[0]
const isUsed = (cell, slot) => {
  const list = schedule.value[cell.dateStr] || []
  return list.some((s) => sameSlot(s, slot))
}
const slotClass = (cell, slot) => {
  if (isUsed(cell, slot)) return 'sp-used'
  if (props.date === cell.dateStr && sameSlot(props.timeSlot, slot)) return 'sp-sel'
  if (isPast(cell) || !isWorkDay(cell)) return 'sp-off'
  return 'sp-ok'
}
const pick = (cell, slot) => {
  if (loading.value) return
  if (isPast(cell) || !isWorkDay(cell)) return
  const d = cell.dateStr
  const selected = props.date === d && sameSlot(props.timeSlot, slot)
  if (isUsed(cell, slot) && !selected) return
  if (selected) {
    emit('update:date', '')
    emit('update:timeSlot', '')
  } else {
    emit('update:date', d)
    emit('update:timeSlot', slot)
  }
}
const prevMonth = () => {
  if (viewMonth.value === 0) { viewMonth.value = 11; viewYear.value-- } else viewMonth.value--
}
const nextMonth = () => {
  if (viewMonth.value === 11) { viewMonth.value = 0; viewYear.value++ } else viewMonth.value++
}
</script>
<style scoped>
.sp-wrap { width: 100%; border: 1px solid #e5e7eb; border-radius: 8px; padding: 10px; box-sizing: border-box; }
.sp-head { display: flex; align-items: center; justify-content: center; gap: 12px; margin-bottom: 8px; }
.sp-title { font-size: 14px; font-weight: 700; color: #1f2937; }
.sp-grid { display: grid; grid-template-columns: repeat(7, 1fr); gap: 4px; }
.sp-grid-head { margin-bottom: 2px; }
.sp-week { text-align: center; font-size: 12px; color: #9ca3af; padding: 4px 0; }
.sp-cell { border: 1px solid #f0f0f0; border-radius: 6px; padding: 4px; min-height: 46px; box-sizing: border-box; }
.sp-empty { border: none; }
.sp-date { font-size: 12px; color: #374151; margin-bottom: 3px; }
.sp-today { color: #2563eb; font-weight: 700; }
.sp-slots { display: flex; flex-direction: column; gap: 3px; }
.sp-slot { font-size: 11px; text-align: center; border-radius: 4px; padding: 2px 0; cursor: pointer; user-select: none; }
.sp-ok { background: #e8f3ff; color: #2563eb; }
.sp-ok:hover { background: #dbeafe; }
.sp-used { background: #fee2e2; color: #dc2626; cursor: not-allowed; }
.sp-off { background: #f3f4f6; color: #d1d5db; cursor: not-allowed; }
.sp-sel { background: #2563eb; color: #fff; }
.sp-diag { margin-top: 6px; font-size: 12px; color: #d97706; line-height: 1.5; }
.sp-tip { display: flex; gap: 14px; margin-top: 8px; font-size: 12px; color: #6b7280; }
.sp-legend { display: inline-flex; align-items: center; gap: 4px; }
.sp-dot { width: 10px; height: 10px; border-radius: 50%; display: inline-block; }
.sp-dot.used { background: #fee2e2; border: 1px solid #dc2626; }
.sp-dot.ok { background: #e8f3ff; border: 1px solid #2563eb; }
.sp-dot.off { background: #f3f4f6; border: 1px solid #d1d5db; }
</style>
