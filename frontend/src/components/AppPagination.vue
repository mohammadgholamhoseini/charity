<script setup>
import { ChevronLeft, ChevronRight } from '@lucide/vue'

defineProps({
  page: { type: Number, required: true },
  total: { type: Number, required: true },
  size: { type: Number, required: true }
})

defineEmits(['update:page'])
</script>

<template>
  <nav v-if="total > size" aria-label="صفحه‌بندی" class="mt-10 flex items-center justify-center gap-2">
    <button
      class="btn-ghost px-3"
      :disabled="page === 0"
      aria-label="صفحه قبل"
      @click="$emit('update:page', page - 1)"
    >
      <ChevronRight :size="17" /> قبلی
    </button>
    <span class="min-w-20 text-center text-sm font-semibold text-slate-500 dark:text-slate-400">
      صفحه {{ new Intl.NumberFormat('fa-IR').format(page + 1) }}
    </span>
    <button
      class="btn-ghost px-3"
      :disabled="(page + 1) * size >= total"
      aria-label="صفحه بعد"
      @click="$emit('update:page', page + 1)"
    >
      بعدی <ChevronLeft :size="17" />
    </button>
  </nav>
</template>
