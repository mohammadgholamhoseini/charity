<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter, RouterLink } from 'vue-router'
import api from '../../api/client'
import CaseCard from '../../components/CaseCard.vue'
import { ArrowRight, Phone, MapPin, CreditCard, Landmark, Heart, ShieldAlert, Building2 } from '@lucide/vue'

const route = useRoute()
const router = useRouter()
const center = ref(null)
const cases = ref([])
const loading = ref(true)

function nf(v) {
  return new Intl.NumberFormat('fa-IR').format(v)
}

function fileUrl(name) {
  return name ? `/api/public/files/${name}` : null
}

async function load() {
  loading.value = true
  try {
    const c = await api.get(`/public/centers/${route.params.id}`)
    center.value = c.data
    const res = await api.get('/public/cases', { params: { size: 50, centerId: Number(route.params.id) } })
    cases.value = res.data.content || []
  } catch (e) {
    router.push('/')
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="max-w-4xl mx-auto px-4 py-10" v-if="!loading && center">
    <RouterLink to="/centers" class="inline-flex items-center gap-1.5 text-brand-700 dark:text-brand-300 text-sm mb-5 hover:gap-2.5 transition-all">
      <ArrowRight :size="16" /> بازگشت به لیست مراکز
    </RouterLink>

    <!-- Profile card -->
    <div class="card overflow-hidden">
      <div class="relative h-36 bg-gradient-to-br from-brand-500 via-brand-600 to-emerald-700">
        <div class="absolute -bottom-10 right-6 w-20 h-20 rounded-3xl bg-white dark:bg-slate-900 grid place-items-center text-3xl shadow-lg border-4 border-white dark:border-slate-900 overflow-hidden">
          <img v-if="center.logoUrl" :src="fileUrl(center.logoUrl)" alt="لوگو" class="w-full h-full object-cover" />
          <Building2 v-else :size="32" class="text-brand-500" />
        </div>
        <div class="absolute top-4 left-5 chip bg-white/15 backdrop-blur text-white">
          مرکز خیریه
        </div>
      </div>

      <div class="pt-14 px-6 pb-6">
        <h1 class="text-2xl font-extrabold text-slate-800 dark:text-white">{{ center.name }}</h1>
        <p v-if="center.description" class="text-slate-600 dark:text-slate-300 mt-2 leading-7">{{ center.description }}</p>

        <div class="flex flex-wrap gap-2 mt-4" v-if="center.categories && center.categories.length">
          <span v-for="cat in center.categories" :key="cat.id"
            class="chip bg-brand-50 dark:bg-brand-900/40 text-brand-700 dark:text-brand-300">{{ cat.name }}</span>
        </div>

        <!-- Contact grid -->
        <div class="grid sm:grid-cols-2 gap-3 mt-6">
          <div class="rounded-2xl bg-slate-50 dark:bg-slate-800/50 p-4 flex items-center gap-3" v-if="center.contactPhone">
            <span class="grid place-items-center w-9 h-9 rounded-xl bg-brand-100 dark:bg-brand-900/50 text-brand-600 dark:text-brand-300"><Phone :size="16" /></span>
            <div><div class="text-xs text-slate-400">تلفن تماس</div><div class="font-medium text-slate-700 dark:text-slate-200" dir="ltr">{{ center.contactPhone }}</div></div>
          </div>
          <div class="rounded-2xl bg-slate-50 dark:bg-slate-800/50 p-4 flex items-center gap-3" v-if="center.address">
            <span class="grid place-items-center w-9 h-9 rounded-xl bg-brand-100 dark:bg-brand-900/50 text-brand-600 dark:text-brand-300"><MapPin :size="16" /></span>
            <div><div class="text-xs text-slate-400">آدرس</div><div class="font-medium text-slate-700 dark:text-slate-200">{{ center.address }}</div></div>
          </div>
          <div class="rounded-2xl bg-slate-50 dark:bg-slate-800/50 p-4 flex items-center gap-3" v-if="center.cardNumber">
            <span class="grid place-items-center w-9 h-9 rounded-xl bg-brand-100 dark:bg-brand-900/50 text-brand-600 dark:text-brand-300"><CreditCard :size="16" /></span>
            <div><div class="text-xs text-slate-400">شماره کارت</div><div class="font-medium text-slate-700 dark:text-slate-200 font-mono" dir="ltr">{{ center.cardNumber }}</div></div>
          </div>
          <div class="rounded-2xl bg-slate-50 dark:bg-slate-800/50 p-4 flex items-center gap-3" v-if="center.sheba">
            <span class="grid place-items-center w-9 h-9 rounded-xl bg-brand-100 dark:bg-brand-900/50 text-brand-600 dark:text-brand-300"><Landmark :size="16" /></span>
            <div><div class="text-xs text-slate-400">شماره شبا</div><div class="font-medium text-slate-700 dark:text-slate-200 font-mono" dir="ltr">{{ center.sheba }}</div></div>
          </div>
        </div>

        <div class="mt-5 flex items-start gap-3 bg-amber-50 dark:bg-amber-900/20 border border-amber-100 dark:border-amber-800 rounded-2xl p-4 text-sm text-amber-800 dark:text-amber-200 leading-7">
          <ShieldAlert :size="18" class="shrink-0 mt-0.5" />
          <span>این پلتفرم صرفاً اطلاع‌رسانی است و مسئولیت پرداخت‌ها بر عهده خود کاربر و مرکز خیریه می‌باشد.</span>
        </div>
      </div>
    </div>

    <!-- Center cases -->
    <h2 class="text-xl font-bold text-slate-800 dark:text-white mt-10 mb-5 flex items-center gap-2">
      <Heart :size="20" class="text-brand-500" /> درخواست‌های این مرکز
    </h2>
    <div v-if="!cases.length" class="text-slate-400 dark:text-slate-500 py-10 text-center card">درخواست فعالی یافت نشد.</div>
    <div v-else class="grid sm:grid-cols-2 gap-5 stagger">
      <CaseCard v-for="c in cases" :key="c.id" :item="c" />
    </div>
  </div>
  <div v-else class="text-center py-20 text-slate-400 flex flex-col items-center gap-3">
    <div class="w-10 h-10 border-2 border-brand-300 border-t-transparent rounded-full animate-spin"></div>
    در حال بارگذاری...
  </div>
</template>
