<script setup>
import { AlertCircle, CheckCircle2, Info } from '@lucide/vue'
import { computed } from 'vue'

const props = defineProps({
  type: { type: String, default: 'info' }
})

const meta = computed(() => ({
  success: { icon: CheckCircle2, cls: 'border-emerald-200 bg-emerald-50 text-emerald-800 dark:border-emerald-800 dark:bg-emerald-950/40 dark:text-emerald-200' },
  error: { icon: AlertCircle, cls: 'border-red-200 bg-red-50 text-red-800 dark:border-red-900 dark:bg-red-950/40 dark:text-red-200' },
  warning: { icon: AlertCircle, cls: 'border-amber-200 bg-amber-50 text-amber-900 dark:border-amber-800 dark:bg-amber-950/40 dark:text-amber-100' },
  info: { icon: Info, cls: 'border-brand-200 bg-brand-50 text-brand-800 dark:border-brand-800 dark:bg-brand-950/40 dark:text-brand-100' }
}[props.type] || {}))
</script>

<template>
  <div role="status" class="flex items-start gap-3 rounded-2xl border px-4 py-3 text-sm leading-7" :class="meta.cls">
    <component :is="meta.icon" :size="18" class="mt-1 shrink-0" />
    <div><slot /></div>
  </div>
</template>
