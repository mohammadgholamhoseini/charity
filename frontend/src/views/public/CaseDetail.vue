<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter, RouterLink } from 'vue-router'
import api from '../../api/client'
import { ArrowRight, Building2, Phone, FileText, ShieldAlert, Heart, CheckCircle2, Clock } from '@lucide/vue'

const route = useRoute()
const router = useRouter()
const item = ref(null)
const detailEntries = computed(() => {
  const d = item.value?.details || {}
  return Object.entries(d).filter(([k]) => k !== 'beneficiaryName')
})
const loading = ref(true)

function nf(v) {
  return new Intl.NumberFormat('fa-IR').format(v || 0)
}
function docUrl(f) {
  return `/api/public/files/${f}`
}
const statusMeta = computed(() => {
  switch (item.value?.status) {
    case 'PUBLISHED': return { label: 'فعال', cls: 'bg-brand-50 text-brand-700 dark:bg-brand-900/40 dark:text-brand-300', icon: Clock }
    case 'COMPLETED': return { label: 'تأمین شده', cls: 'bg-emerald-50 text-emerald-700 dark:bg-emerald-900/30 dark:text-emerald-300', icon: CheckCircle2 }
    case 'PENDING': return { label: 'در انتظار', cls: 'bg-amber-50 text-amber-700 dark:bg-amber-900/30 dark:text-amber-300', icon: Clock }
    case 'REJECTED': return { label: 'رد شده', cls: 'bg-red-50 text-red-700 dark:bg-red-900/30 dark:text-red-300', icon: ShieldAlert }
    default: return { label: item.value?.status, cls: 'bg-slate-100 text-slate-500', icon: Clock }
  }
})
const progress = computed(() => {
  if (!item.value?.amountNeeded) return 0
  return Math.min(100, Math.round(((item.value.amountCollected || 0) / item.value.amountNeeded) * 100))
})

async function load() {
  loading.value = true
  try {
    const c = await api.get(`/public/cases/${route.params.id}`)
    item.value = c.data
  } catch (e) {
    router.push('/cases')
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="max-w-4xl mx-auto px-4 py-10" v-if="!loading && item">
    <RouterLink to="/cases" class="inline-flex items-center gap-1.5 text-brand-700 dark:text-brand-300 text-sm mb-5 hover:gap-2.5 transition-all">
      <ArrowRight :size="16" />
      بازگشت به لیست
    </RouterLink>

    <div class="card overflow-hidden">
      <!-- Hero image -->
      <div class="relative h-60 bg-gradient-to-br from-brand-200 to-emerald-300 dark:from-brand-900/50 dark:to-emerald-900/40">
        <img v-if="item.imageUrl" :src="item.imageUrl" class="w-full h-full object-cover" alt="" />
        <div v-else class="w-full h-full grid place-items-center text-7xl opacity-80">🏥</div>
        <div class="absolute inset-0 bg-gradient-to-t from-black/40 to-transparent"></div>
        <div class="absolute bottom-4 right-4 flex items-center gap-2">
          <span v-if="item.categoryName" class="chip bg-white/85 backdrop-blur text-brand-700 shadow-sm">{{ item.categoryName }}</span>
          <span class="chip backdrop-blur" :class="statusMeta.cls">
            <component :is="statusMeta.icon" :size="13" />
            {{ statusMeta.label }}
          </span>
        </div>
      </div>

      <div class="p-6 sm:p-8">
        <h1 class="text-2xl sm:text-3xl font-extrabold text-slate-800 dark:text-white">{{ item.title }}</h1>
        <p v-if="item.details?.beneficiaryName" class="text-slate-500 dark:text-slate-400 mt-1.5 flex items-center gap-1.5">
          <Heart :size="15" class="text-brand-500" /> ذینفع: {{ item.details.beneficiaryName }}
        </p>

        <!-- Center -->
        <RouterLink v-if="item.centerId" :to="`/center/${item.centerId}`"
          class="inline-flex items-center gap-1.5 mt-4 chip bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-300 hover:bg-slate-200 dark:hover:bg-slate-700 transition">
          <Building2 :size="14" /> {{ item.centerName }}
        </RouterLink>

        <!-- Progress / amount -->
        <div class="mt-6 card bg-brand-50/60 dark:bg-brand-900/20 border-brand-100 dark:border-brand-800 p-5">
          <div class="flex items-end justify-between mb-3">
            <div>
              <div class="text-xs text-slate-500 dark:text-slate-400">مبلغ مورد نیاز</div>
              <div class="font-extrabold text-xl text-brand-700 dark:text-brand-300">{{ nf(item.amountNeeded) }} <span class="text-sm font-normal">تومان</span></div>
            </div>
            <div v-if="item.amountCollected" class="text-left">
              <div class="text-xs text-slate-500 dark:text-slate-400">تاکنون</div>
              <div class="font-bold text-emerald-600 dark:text-emerald-400">{{ nf(item.amountCollected) }}</div>
            </div>
          </div>
          <div class="h-2.5 rounded-full bg-white/70 dark:bg-slate-800 overflow-hidden">
            <div class="h-full rounded-full bg-gradient-to-l from-brand-500 to-emerald-400 transition-all duration-700" :style="{ width: progress + '%' }"></div>
          </div>
          <div class="text-xs text-slate-500 dark:text-slate-400 mt-2">٪{{ nf(progress) }} تأمین شده</div>
        </div>

        <!-- Detail entries -->
        <div class="grid sm:grid-cols-2 gap-3 mt-6" v-if="detailEntries.length">
          <div v-for="[k, v] in detailEntries" :key="k" class="rounded-2xl bg-slate-50 dark:bg-slate-800/50 p-4">
            <div class="text-xs text-slate-400 dark:text-slate-500 mb-1">{{ k }}</div>
            <div class="font-medium text-slate-700 dark:text-slate-200">{{ v }}</div>
          </div>
        </div>

        <!-- Description -->
        <p v-if="item.description" class="text-slate-600 dark:text-slate-300 leading-8 mt-6">{{ item.description }}</p>
        <p v-if="item.contactInfo" class="mt-4 inline-flex items-center gap-2 text-sm text-slate-600 dark:text-slate-300 bg-slate-50 dark:bg-slate-800/50 rounded-xl px-4 py-2.5">
          <Phone :size="15" class="text-brand-500" /> تماس: {{ item.contactInfo }}
        </p>

        <!-- Documents -->
        <div class="mt-7" v-if="item.documents && item.documents.length">
          <h3 class="font-bold text-slate-700 dark:text-slate-200 mb-3 flex items-center gap-2">
            <FileText :size="18" class="text-brand-500" /> مدارک و مستندات
          </h3>
          <div class="space-y-2">
            <a v-for="(f, i) in item.documents" :key="i" :href="docUrl(f)" target="_blank"
              class="flex items-center justify-between rounded-2xl px-4 py-3 text-sm bg-brand-50 dark:bg-brand-900/20 text-brand-700 dark:text-brand-300 hover:bg-brand-100 dark:hover:bg-brand-900/40 transition">
              <span class="flex items-center gap-2"><FileText :size="15" /> مدرک {{ nf(i + 1) }}</span>
              <span class="font-medium opacity-80">مشاهده / دانلود</span>
            </a>
          </div>
        </div>

        <!-- Disclaimer -->
        <div class="mt-7 flex items-start gap-3 bg-amber-50 dark:bg-amber-900/20 border border-amber-100 dark:border-amber-800 rounded-2xl p-4 text-sm text-amber-800 dark:text-amber-200 leading-7">
          <ShieldAlert :size="18" class="shrink-0 mt-0.5" />
          <span>این پلتفرم صرفاً اطلاع‌رسانی است و مسئولیت پرداخت‌ها بر عهده خود کاربر و مرکز خیریه می‌باشد.</span>
        </div>
      </div>
    </div>
  </div>
  <div v-else class="text-center py-20 text-slate-400 flex flex-col items-center gap-3">
    <div class="w-10 h-10 border-2 border-brand-300 border-t-transparent rounded-full animate-spin"></div>
    در حال بارگذاری...
  </div>
</template>
