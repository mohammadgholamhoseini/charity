<script setup>
import { RouterLink } from 'vue-router'
import { Building2, HandHeart } from '@lucide/vue'

const props = defineProps({
  item: { type: Object, required: true }
})

function nf(v) {
  return new Intl.NumberFormat('fa-IR').format(v || 0)
}

const URGENCY = {
  URGENT: { label: 'فوری', cls: 'bg-red-500 text-white' },
  HIGH: { label: 'بالا', cls: 'bg-orange-500 text-white' },
  MEDIUM: { label: 'متوسط', cls: 'bg-amber-400 text-slate-900' },
  LOW: { label: 'کم', cls: 'bg-slate-400 text-slate-900' }
}

const urgencyMeta = URGENCY[props.item.urgency] || URGENCY.MEDIUM
</script>

<template>
  <RouterLink
    :to="`/case/${item.id}`"
    class="group card p-5 hover:shadow-xl hover:shadow-brand-600/10 hover:-translate-y-1 transition-all duration-300 flex flex-col"
  >
    <div class="flex items-start justify-between gap-2 mb-3">
      <div class="flex items-center gap-2 min-w-0">
        <span class="grid place-items-center w-9 h-9 rounded-xl bg-brand-100 dark:bg-brand-900/40 text-brand-600 dark:text-brand-300 shrink-0">
          <HandHeart :size="18" />
        </span>
        <span v-if="item.categoryName" class="chip text-brand-700 dark:text-brand-300 truncate">
          {{ item.categoryName }}
        </span>
      </div>
      <span v-if="item.urgency" class="chip shrink-0" :class="urgencyMeta.cls">
        {{ urgencyMeta.label }}
      </span>
    </div>

    <h3 class="font-bold text-slate-800 dark:text-white group-hover:text-brand-600 dark:group-hover:text-brand-300 transition line-clamp-1 mb-2">
      {{ item.title }}
    </h3>

    <p class="text-sm text-slate-500 dark:text-slate-400 line-clamp-2 mb-4 flex-1">
      {{ item.details?.beneficiaryName || item.description }}
    </p>

    <div class="flex items-center justify-between pt-3 border-t border-slate-100 dark:border-slate-800">
      <div class="flex items-center gap-1.5 text-xs text-slate-500 dark:text-slate-400 min-w-0">
        <Building2 :size="14" />
        <span class="line-clamp-1 max-w-[140px]">{{ item.centerName || 'مرکز خیریه' }}</span>
      </div>
      <span class="inline-flex items-center gap-1 text-sm font-bold text-slate-700 dark:text-slate-200">
        {{ nf(item.amountNeeded) }}
        <span class="text-xs font-normal text-slate-400">تومان</span>
      </span>
    </div>
  </RouterLink>
</template>
