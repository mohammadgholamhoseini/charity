<script setup>
import { RouterLink } from 'vue-router'
import { Building2, HandHeart, ArrowLeft, MapPin } from '@lucide/vue'
import StatusBadge from './StatusBadge.vue'

defineProps({
  item: { type: Object, required: true }
})

function nf(value) {
  return new Intl.NumberFormat('fa-IR').format(value || 0)
}

const urgency = {
  URGENT: { label: 'فوری', cls: 'bg-red-50 text-red-700 dark:bg-red-950/60 dark:text-red-200' },
  HIGH: { label: 'اولویت بالا', cls: 'bg-orange-50 text-orange-700 dark:bg-orange-950/50 dark:text-orange-200' },
  MEDIUM: { label: 'اولویت متوسط', cls: 'bg-amber-50 text-amber-800 dark:bg-amber-950/50 dark:text-amber-200' },
  LOW: { label: 'اولویت عادی', cls: 'bg-slate-100 text-slate-600 dark:bg-slate-800 dark:text-slate-300' }
}
</script>

<template>
  <RouterLink
    :to="`/case/${item.id}`"
    class="group card flex min-h-72 flex-col overflow-hidden p-5 transition duration-300 hover:-translate-y-1 hover:shadow-xl"
  >
    <div class="mb-5 flex items-start justify-between gap-3">
      <span class="grid h-11 w-11 place-items-center rounded-2xl bg-brand-50 text-brand-700 dark:bg-brand-950 dark:text-brand-300">
        <HandHeart :size="21" />
      </span>
      <span v-if="item.urgency" class="chip" :class="urgency[item.urgency]?.cls">{{ urgency[item.urgency]?.label }}</span>
    </div>

    <div class="mb-3 flex flex-wrap items-center gap-2">
      <span v-if="item.categoryName" class="chip bg-brand-50 text-brand-800 dark:bg-brand-950 dark:text-brand-200">{{ item.categoryName }}</span>
      <StatusBadge v-if="item.status === 'COMPLETED'" :status="item.status" />
    </div>

    <h3 class="line-clamp-2 text-lg font-black leading-8 text-slate-900 transition group-hover:text-brand-700 dark:text-white dark:group-hover:text-brand-300">
      {{ item.title }}
    </h3>
    <p class="mt-2 line-clamp-2 flex-1 text-sm leading-7 text-slate-500 dark:text-slate-400">
      {{ item.details?.beneficiaryName || item.description || 'برای مشاهده توضیحات کامل وارد صفحه درخواست شوید.' }}
    </p>

    <div class="mt-5 border-t border-slate-100 pt-4 dark:border-slate-800">
      <div class="flex items-center gap-2 text-xs text-slate-500 dark:text-slate-400">
        <Building2 :size="15" class="shrink-0 text-brand-600 dark:text-brand-300" />
        <span class="truncate">{{ item.centerName || 'مرکز خیریه' }}</span>
        <span v-if="item.cityName" class="mr-auto flex items-center gap-1"><MapPin :size="13" /> {{ item.cityName }}</span>
      </div>
      <div class="mt-3 flex items-end justify-between gap-3">
        <div>
          <span class="text-xs text-slate-400">مبلغ مورد نیاز</span>
          <div class="font-black text-slate-800 dark:text-slate-100">{{ nf(item.amountNeeded) }} <span class="text-xs font-medium text-slate-400">تومان</span></div>
        </div>
        <span class="grid h-9 w-9 place-items-center rounded-xl bg-brand-50 text-brand-700 transition group-hover:bg-brand-600 group-hover:text-white dark:bg-brand-950 dark:text-brand-300">
          <ArrowLeft :size="17" />
        </span>
      </div>
    </div>
  </RouterLink>
</template>
