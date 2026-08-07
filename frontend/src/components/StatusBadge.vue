<script setup>
import { computed } from 'vue'

const props = defineProps({
  status: { type: String, default: '' },
  label: { type: String, default: '' }
})

const labels = {
  PENDING: 'در انتظار',
  PUBLISHED: 'منتشرشده',
  COMPLETED: 'تأمین‌شده',
  REJECTED: 'ردشده',
  APPROVED: 'تأییدشده',
  INACTIVE: 'غیرفعال',
  ACTIVE: 'فعال'
}

const classes = {
  PENDING: 'bg-amber-50 text-amber-800 dark:bg-amber-950/50 dark:text-amber-200',
  PUBLISHED: 'bg-brand-50 text-brand-800 dark:bg-brand-950 dark:text-brand-200',
  COMPLETED: 'bg-emerald-50 text-emerald-800 dark:bg-emerald-950/50 dark:text-emerald-200',
  APPROVED: 'bg-emerald-50 text-emerald-800 dark:bg-emerald-950/50 dark:text-emerald-200',
  REJECTED: 'bg-red-50 text-red-700 dark:bg-red-950/50 dark:text-red-200',
  INACTIVE: 'bg-slate-100 text-slate-600 dark:bg-slate-800 dark:text-slate-300'
}

const text = computed(() => props.label || labels[props.status] || props.status)
const cls = computed(() => classes[props.status] || 'bg-slate-100 text-slate-600 dark:bg-slate-800 dark:text-slate-300')
</script>

<template>
  <span class="chip" :class="cls">{{ text }}</span>
</template>
