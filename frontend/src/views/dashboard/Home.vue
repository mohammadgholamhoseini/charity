<script setup>
import { useAuthStore } from '../../stores/auth'
import { computed } from 'vue'
import { FileText, Building2, PlusCircle, LayoutList, ArrowLeft, Sparkles } from '@lucide/vue'

const auth = useAuthStore()
const role = computed(() => auth.role)

const cards = computed(() => {
  if (role.value === 'ADMIN') {
    return [
      { to: '/dashboard/admin/cases', icon: FileText, title: 'مدیریت درخواست‌ها', desc: 'تایید و انتشار درخواست‌های کمک', color: 'from-brand-500 to-brand-600' },
      { to: '/dashboard/admin/centers', icon: Building2, title: 'تایید مراکز', desc: 'بررسی و فعال‌سازی مراکز خیریه', color: 'from-brand-500 to-brand-600' },
      { to: '/dashboard/admin/categories', icon: LayoutList, title: 'دسته‌بندی‌ها', desc: 'مدیریت موضوعات درخواست‌ها', color: 'from-accent-500 to-accent-600' },
      { to: '/dashboard/admin/notices', icon: Sparkles, title: 'اطلاعیه‌ها', desc: 'مدیریت اطلاعیه‌های سایت', color: 'from-amber-500 to-amber-600' }
    ]
  }
  return [
      { to: '/dashboard/cases/new', icon: PlusCircle, title: 'ثبت درخواست جدید', desc: 'نیاز، مبلغ و مستندات را شفاف ثبت کنید', color: 'from-brand-500 to-brand-600' },
    { to: '/dashboard/cases', icon: FileText, title: 'درخواست‌های من', desc: 'پیگیری وضعیت درخواست‌های ثبت‌شده', color: 'from-brand-500 to-brand-600' }
  ]
})
</script>

<template>
  <div>
    <div class="mb-7">
      <h1 class="text-2xl sm:text-3xl font-extrabold text-slate-800 dark:text-white">داشبورد</h1>
      <p class="text-slate-500 dark:text-slate-400 mt-1">مسیرهای اصلی پنل یاری‌جو را از اینجا مدیریت کنید.</p>
    </div>

    <div class="grid sm:grid-cols-2 xl:grid-cols-3 gap-4 stagger">
      <RouterLink v-for="c in cards" :key="c.to" :to="c.to"
        class="card p-5 hover:-translate-y-1 hover:shadow-xl transition-all group">
        <span class="grid place-items-center w-12 h-12 rounded-2xl text-white shadow-lg mb-4 bg-gradient-to-br group-hover:scale-105 transition"
          :class="c.color">
          <component :is="c.icon" :size="22" />
        </span>
        <div class="font-bold text-slate-800 dark:text-white text-lg">{{ c.title }}</div>
        <p class="text-sm text-slate-400 dark:text-slate-500 mt-1">{{ c.desc }}</p>
        <span class="inline-flex items-center gap-1 text-brand-600 dark:text-brand-300 text-sm mt-3 font-medium">
          مشاهده <ArrowLeft :size="14" />
        </span>
      </RouterLink>
    </div>
  </div>
</template>
