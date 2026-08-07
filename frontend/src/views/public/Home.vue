<script setup>
import { ref, onMounted, watch } from 'vue'
import { RouterLink } from 'vue-router'
import api from '../../api/client'
import CaseCard from '../../components/CaseCard.vue'
import EmptyState from '../../components/EmptyState.vue'
import AppPagination from '../../components/AppPagination.vue'
import {
  ArrowLeft,
  Building2,
  CheckCircle2,
  HandHeart,
  HeartHandshake,
  MapPin,
  Search,
  ShieldCheck,
  Sparkles,
  Users
} from '@lucide/vue'

const cases = ref([])
const total = ref(0)
const page = ref(0)
const size = 9
const categoryId = ref(null)
const categories = ref([])
const loading = ref(true)
const error = ref('')
const stats = ref({ active: 0, completed: 0, centers: 0 })
const provinces = ref([])
const cities = ref([])
const provinceId = ref(null)
const cityId = ref(null)

const nf = (value) => new Intl.NumberFormat('fa-IR').format(value || 0)

async function loadCategories() {
  try {
    const response = await api.get('/public/categories')
    categories.value = response.data
  } catch {}
}

async function loadProvinces() {
  try {
    const response = await api.get('/public/provinces')
    provinces.value = response.data
  } catch {}
}

async function loadCities() {
  if (!provinceId.value) {
    cities.value = []
    cityId.value = null
    return
  }
  try {
    const response = await api.get('/public/cities', { params: { provinceId: provinceId.value } })
    cities.value = response.data
  } catch {}
}

async function loadStats() {
  try {
    const [active, completed, centers] = await Promise.all([
      api.get('/public/cases', { params: { size: 1, status: 'PUBLISHED' } }),
      api.get('/public/cases', { params: { size: 1, status: 'COMPLETED' } }),
      api.get('/public/centers', { params: { size: 1, approved: true } })
    ])
    stats.value = {
      active: active.data.totalElements,
      completed: completed.data.totalElements,
      centers: centers.data.totalElements
    }
  } catch {}
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    const response = await api.get('/public/cases', {
      params: {
        page: page.value,
        size,
        categoryId: categoryId.value || undefined,
        provinceId: provinceId.value || undefined,
        cityId: cityId.value || undefined
      }
    })
    cases.value = response.data.content
    total.value = response.data.totalElements
  } catch {
    error.value = 'دریافت درخواست‌ها با مشکل روبه‌رو شد. لطفاً دوباره تلاش کنید.'
    cases.value = []
  } finally {
    loading.value = false
  }
}

function pickCategory(id) {
  categoryId.value = categoryId.value === id ? null : id
  page.value = 0
  load()
}

function onProvinceChange() {
  cityId.value = null
  loadCities()
  page.value = 0
  load()
}

function resetLocation() {
  provinceId.value = null
  cityId.value = null
  cities.value = []
  page.value = 0
  load()
}

onMounted(() => {
  loadCategories()
  loadProvinces()
  loadStats()
  load()
})
watch(page, load)
</script>

<template>
  <div>
    <section class="relative overflow-hidden bg-[#092733] text-white">
      <div class="girih absolute inset-0 text-white/[0.035]"></div>
      <div class="absolute -right-36 -top-28 h-96 w-96 rounded-full bg-brand-400/20 blur-3xl"></div>
      <div class="absolute -bottom-40 left-0 h-96 w-96 rounded-full bg-accent-500/15 blur-3xl"></div>

      <div class="relative mx-auto grid max-w-6xl items-center gap-10 px-4 py-16 sm:px-6 sm:py-20 lg:grid-cols-[1.08fr_.92fr] lg:py-24">
        <div class="text-center lg:text-right">
          <span class="mb-5 inline-flex items-center gap-2 rounded-full border border-brand-300/25 bg-brand-300/10 px-4 py-2 text-sm font-bold text-brand-200">
            <ShieldCheck :size="17" />
            درخواست‌های واقعی، ثبت‌شده توسط مراکز خیریه
          </span>
          <h1 class="font-display text-4xl font-black leading-[1.45] sm:text-5xl lg:text-[3.55rem]">
            یاری آگاهانه،<br />
            <span class="text-accent-300">اثر ماندگار</span>
          </h1>
          <p class="mx-auto mt-5 max-w-xl text-sm leading-8 text-slate-300 sm:text-base lg:mx-0">
            یاری‌جو مسیر شفاف دیدن نیازها، شناخت مرکز ثبت‌کننده و ارتباط مستقیم برای کمک را فراهم می‌کند؛ بدون واسطه‌گری مالی.
          </p>
          <div class="mt-7 flex flex-col justify-center gap-3 sm:flex-row lg:justify-start">
            <RouterLink to="/cases" class="btn-primary">
              مشاهده درخواست‌ها <ArrowLeft :size="18" />
            </RouterLink>
            <RouterLink to="/centers" class="inline-flex min-h-11 items-center justify-center gap-2 rounded-2xl border border-white/20 bg-white/5 px-5 py-2.5 font-bold text-white transition hover:bg-white/10">
              <Building2 :size="18" /> شناخت مراکز
            </RouterLink>
          </div>
          <div class="mt-8 flex flex-wrap justify-center gap-x-6 gap-y-3 text-xs text-slate-300 lg:justify-start">
            <span class="flex items-center gap-2"><CheckCircle2 :size="16" class="text-brand-300" /> مشاهده مرکز ثبت‌کننده</span>
            <span class="flex items-center gap-2"><CheckCircle2 :size="16" class="text-brand-300" /> دسترسی به مستندات</span>
            <span class="flex items-center gap-2"><CheckCircle2 :size="16" class="text-brand-300" /> ارتباط مستقیم</span>
          </div>
        </div>

        <div class="relative mx-auto w-full max-w-xl" aria-hidden="true">
          <svg viewBox="0 0 600 520" class="h-auto w-full drop-shadow-2xl">
            <defs>
              <linearGradient id="hero-card" x1="0" y1="0" x2="1" y2="1">
                <stop stop-color="#ffffff" stop-opacity=".98" />
                <stop offset="1" stop-color="#e9f7f1" stop-opacity=".92" />
              </linearGradient>
              <linearGradient id="hero-heart" x1="0" y1="0" x2="1" y2="1">
                <stop stop-color="#258d75" />
                <stop offset="1" stop-color="#196f5d" />
              </linearGradient>
            </defs>
            <circle cx="305" cy="255" r="205" fill="#ffffff" fill-opacity=".035" stroke="#ffffff" stroke-opacity=".08" />
            <circle cx="305" cy="255" r="155" fill="#46aa91" fill-opacity=".08" />
            <rect x="108" y="82" width="384" height="350" rx="38" fill="url(#hero-card)" />
            <rect x="140" y="118" width="110" height="16" rx="8" fill="#d6efe6" />
            <rect x="140" y="148" width="230" height="12" rx="6" fill="#dfe7e3" />
            <rect x="140" y="171" width="180" height="12" rx="6" fill="#edf2ef" />
            <circle cx="420" cy="145" r="42" fill="#fff2df" />
            <path d="M420 169C408 158 391 147 391 131C391 119 399 111 410 111C417 111 421 116 420 122C422 116 426 111 433 111C444 111 452 119 449 131C446 147 431 158 420 169Z" fill="url(#hero-heart)" />
            <path d="M420 122V151" stroke="#ef861c" stroke-width="5" stroke-linecap="round" />
            <rect x="140" y="226" width="320" height="76" rx="20" fill="#f5f8f6" />
            <circle cx="178" cy="264" r="20" fill="#d6efe6" />
            <path d="M169 265L176 272L188 256" stroke="#196f5d" stroke-width="4" stroke-linecap="round" stroke-linejoin="round" />
            <rect x="213" y="247" width="160" height="12" rx="6" fill="#b8c9c3" />
            <rect x="213" y="270" width="110" height="9" rx="4.5" fill="#dfe7e3" />
            <rect x="140" y="322" width="320" height="18" rx="9" fill="#d6efe6" />
            <rect x="140" y="322" width="214" height="18" rx="9" fill="#258d75" />
            <rect x="140" y="365" width="136" height="42" rx="15" fill="#196f5d" />
            <rect x="294" y="365" width="166" height="42" rx="15" fill="#fff2df" />
            <path d="M82 180C104 153 127 146 148 157" stroke="#ef861c" stroke-width="6" stroke-linecap="round" />
            <circle cx="72" cy="193" r="28" fill="#ef861c" />
            <path d="M62 193L69 200L83 185" stroke="white" stroke-width="5" stroke-linecap="round" stroke-linejoin="round" />
            <path d="M467 388C495 400 514 422 522 450" stroke="#7bc7b3" stroke-width="6" stroke-linecap="round" />
            <circle cx="531" cy="466" r="26" fill="#258d75" />
            <path d="M522 466H540M531 457V475" stroke="white" stroke-width="4" stroke-linecap="round" />
          </svg>
          <div class="absolute -bottom-3 right-3 rounded-2xl border border-white/10 bg-[#123c35]/90 px-4 py-3 text-right shadow-xl backdrop-blur sm:right-10">
            <div class="flex items-center gap-2 text-sm font-bold"><Sparkles :size="16" class="text-accent-300" /> یک انتخاب آگاهانه</div>
            <div class="mt-1 text-xs text-slate-300">از مشاهده نیاز تا ارتباط با مرکز</div>
          </div>
        </div>
      </div>

      <svg class="-mb-px block w-full text-[var(--semantic-bg)]" viewBox="0 0 1440 60" preserveAspectRatio="none">
        <path fill="currentColor" d="M0 60V28C310 76 611 4 912 25C1112 39 1288 61 1440 36V60H0Z" />
      </svg>
    </section>

    <section class="relative z-10 mx-auto -mt-2 max-w-5xl px-4 sm:px-6">
      <div class="card grid divide-y divide-slate-100 overflow-hidden p-2 sm:grid-cols-3 sm:divide-x sm:divide-x-reverse sm:divide-y-0 dark:divide-slate-800">
        <div class="flex items-center gap-4 px-5 py-5">
          <span class="grid h-12 w-12 place-items-center rounded-2xl bg-brand-50 text-brand-700 dark:bg-brand-950 dark:text-brand-300"><HandHeart :size="22" /></span>
          <div><div class="text-2xl font-black">{{ nf(stats.active) }}</div><div class="text-xs text-slate-500 dark:text-slate-400">درخواست فعال</div></div>
        </div>
        <div class="flex items-center gap-4 px-5 py-5">
          <span class="grid h-12 w-12 place-items-center rounded-2xl bg-emerald-50 text-emerald-700 dark:bg-emerald-950 dark:text-emerald-300"><HeartHandshake :size="22" /></span>
          <div><div class="text-2xl font-black">{{ nf(stats.completed) }}</div><div class="text-xs text-slate-500 dark:text-slate-400">یاری به‌ثمررسیده</div></div>
        </div>
        <div class="flex items-center gap-4 px-5 py-5">
          <span class="grid h-12 w-12 place-items-center rounded-2xl bg-accent-50 text-accent-700 dark:bg-accent-900/30 dark:text-accent-300"><Users :size="22" /></span>
          <div><div class="text-2xl font-black">{{ nf(stats.centers) }}</div><div class="text-xs text-slate-500 dark:text-slate-400">مرکز همکار</div></div>
        </div>
      </div>
    </section>

    <section id="cases" class="page-shell">
      <div class="mb-7 flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <p class="section-kicker"><HeartHandshake :size="17" /> فرصت‌های یاری</p>
          <h2 class="text-2xl font-black text-slate-900 dark:text-white sm:text-3xl">درخواست‌هایی که امروز به توجه نیاز دارند</h2>
          <p class="mt-2 text-sm leading-7 text-slate-500 dark:text-slate-400">فیلتر کنید، جزئیات را بخوانید و برای ادامه مستقیماً با مرکز خیریه در ارتباط باشید.</p>
        </div>
        <RouterLink to="/cases" class="btn-outline shrink-0 text-sm">مشاهده همه <ArrowLeft :size="16" /></RouterLink>
      </div>

      <div class="card mb-7 p-4 sm:p-5">
        <div class="flex flex-col gap-3 lg:flex-row lg:items-center">
          <div class="flex items-center gap-2 text-sm font-bold text-slate-700 dark:text-slate-200 lg:ml-auto">
            <Search :size="18" class="text-brand-600 dark:text-brand-300" /> پیدا کردن درخواست مناسب
          </div>
          <select v-model="provinceId" class="input lg:w-48" aria-label="انتخاب استان" @change="onProvinceChange">
            <option :value="null">همه استان‌ها</option>
            <option v-for="province in provinces" :key="province.id" :value="province.id">{{ province.name }}</option>
          </select>
          <select v-model="cityId" :disabled="!provinceId" class="input disabled:opacity-50 lg:w-48" aria-label="انتخاب شهر" @change="page = 0; load()">
            <option :value="null">همه شهرها</option>
            <option v-for="city in cities" :key="city.id" :value="city.id">{{ city.name }}</option>
          </select>
          <button v-if="provinceId || cityId" class="btn-ghost text-sm" @click="resetLocation"><MapPin :size="16" /> پاک‌کردن مکان</button>
        </div>
        <div class="mt-4 flex gap-2 overflow-x-auto pb-1">
          <button class="chip shrink-0 border transition" :class="!categoryId ? 'border-brand-600 bg-brand-600 text-white' : 'border-slate-200 text-slate-600 dark:border-slate-700 dark:text-slate-300'" @click="pickCategory(null)">همه موضوع‌ها</button>
          <button
            v-for="category in categories"
            :key="category.id"
            class="chip shrink-0 border transition"
            :class="categoryId === category.id ? 'border-brand-600 bg-brand-600 text-white' : 'border-slate-200 text-slate-600 dark:border-slate-700 dark:text-slate-300'"
            @click="pickCategory(category.id)"
          >
            {{ category.name }}
          </button>
        </div>
      </div>

      <div v-if="loading" class="grid gap-5 md:grid-cols-2 lg:grid-cols-3">
        <div v-for="index in 6" :key="index" class="card h-72 animate-pulse bg-slate-100 dark:bg-slate-800/50"></div>
      </div>
      <EmptyState v-else-if="error" title="دریافت اطلاعات ممکن نشد" :description="error">
        <button class="btn-primary text-sm" @click="load">تلاش دوباره</button>
      </EmptyState>
      <EmptyState v-else-if="!cases.length" title="درخواستی با این فیلتر پیدا نشد" description="فیلترها را تغییر دهید یا همه درخواست‌ها را ببینید." />
      <div v-else class="stagger grid gap-5 md:grid-cols-2 lg:grid-cols-3">
        <CaseCard v-for="item in cases" :key="item.id" :item="item" />
      </div>

      <AppPagination :page="page" :total="total" :size="size" @update:page="page = $event" />
    </section>

    <section class="mx-auto max-w-6xl px-4 pb-4 sm:px-6">
      <div class="relative overflow-hidden rounded-[2rem] bg-brand-700 px-6 py-9 text-white sm:px-10 sm:py-11">
        <div class="girih absolute inset-0 text-white/[0.04]"></div>
        <div class="relative flex flex-col items-start gap-6 md:flex-row md:items-center md:justify-between">
          <div>
            <p class="text-sm font-bold text-brand-200">برای مراکز خیریه</p>
            <h2 class="mt-2 text-2xl font-black">درخواست‌ها را شفاف و منظم منتشر کنید</h2>
            <p class="mt-2 max-w-2xl text-sm leading-7 text-brand-100">پنل مرکز، ثبت درخواست، مدارک و پیگیری وضعیت را در یک مسیر ساده گرد هم می‌آورد.</p>
          </div>
          <RouterLink to="/login" class="inline-flex min-h-11 shrink-0 items-center justify-center gap-2 rounded-2xl bg-white px-5 py-3 font-bold text-brand-800 transition hover:-translate-y-0.5">
            ورود به پنل <ArrowLeft :size="17" />
          </RouterLink>
        </div>
      </div>
    </section>
  </div>
</template>
