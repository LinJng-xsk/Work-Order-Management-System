<template>
  <el-tag :type="tagType" size="small" effect="light">{{ text }}</el-tag>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  status: { type: [Number, String], required: true }
})

// 工单状态编码：0待提交 1待接单 2处理中 3待复核 4已完成 5已关闭 6已驳回
const STATUS_MAP = {
  0: { text: '待提交', type: 'info' },
  1: { text: '待接单', type: 'warning' },
  2: { text: '处理中', type: 'primary' },
  3: { text: '待复核', type: 'success' },
  4: { text: '已完成', type: 'success' },
  5: { text: '已关闭', type: 'info' },
  6: { text: '已驳回', type: 'danger' }
}

const tagType = computed(() => STATUS_MAP[props.status]?.type || 'info')
const text = computed(() => STATUS_MAP[props.status]?.text || `未知(${props.status})`)
</script>
