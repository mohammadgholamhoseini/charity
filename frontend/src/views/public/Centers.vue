<script setup>
import { ref, onMounted, watch } from 'vue'
import { RouterLink } from 'vue-router'
import api from '../../api/client'
import PageHeader from '../../components/PageHeader.vue'
import EmptyState from '../../components/EmptyState.vue'
import AppPagination from '../../components/AppPagination.vue'
import { ArrowLeft, Building2, MapPin, Search, ShieldCheck } from '@lucide/vue'

const centers = ref([])
const total = ref(0)
const page = ref(0)
const size = 9
const q = ref('')
const provinceId = ref(null)
const provinces = ref([])
const loading = ref(true)
const error = ref('')

async function loadProvinces() {
  try {
    const response = await api.get('/public/provinces')
    provinces.value = response.data
  } catch {}
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    const response = await api.get('/public/centers', {
      params: {
        page: page.value,
        size,
        provinceId: provinceId.value || undefined,
        q: q.value || undefined
      }
    })
    centers.value = response.data.content
    total.value = response.data.totalElements
  } catch {
    error.value = 'فهرست مراکز دریافت نشد. لطفاً دوباره تلاش کنید.'
    centers.value = []
  } finally {
    loading.value = false
  }
}

function fileUrl(name) {
  return name ? `/api/public/files/${name}` : null
}

function search() {
  page.value = 0
  load()
}

onMounted(() => {
  loadProvinces()
  load()
})
watch(page, load)
</script>

<template>
  <div class="page-shell">
    <PageHeader
      eyebrow="شبکه اعتماد"
      title="مراکز خیریه"
      description="پروفایل مراکز، حوزه‌های فعالیت و درخواست‌های ثبت‌شده هر مرکز را یک‌جا ببینید."
    />

    <div class="card mb-8 flex flex-col gap-3 p-4 sm:flex-row sm:p-5">
      <div class="relative flex-1">
        <Search :size="18" class="pointer-events-none absolute right-4 top-1/2 -translate-y-1/2 text-slate-400" />
        <input v-model="q" class="input pr-11" placeholder="جستجوی نام مرکز..." @keyup.enter="search" />
      </div>
      <select v-model="provinceId" class="input sm:w-56" @change="search">
        <option :value="null">همه استان‌ها</option>
        <option v-for="province in provinces" :key="province.id" :value="province.id">{{ province.name }}</option>
      </select>
      <button class="btn-primary text-sm" @click="search"><Search :size="17" /> جستجو</button>
    </div>

    <div v-if="loading" class="grid gap-5 md:grid-cols-2 lg:grid-cols-3">
      <div v-for="index in 6" :key="index" class="card h-64 animate-pulse bg-slate-100 dark:bg-slate-800/50"></div>
    </div>
    <EmptyState v-else-if="error" title="خطا در دریافت مراکز" :description="error">
      <button class="btn-primary text-sm" @click="load">تلاش دوباره</button>
    </EmptyState>
    <EmptyState v-else-if="!centers.length" title="مرکزی پیدا نشد" description="نام یا استان دیگری را جستجو کنید." :icon="Building2" />

    <div v-else class="stagger grid gap-5 md:grid-cols-2 lg:grid-cols-3">
      <RouterLink v-for="center in centers" :key="center.id" :to="`/center/${center.id}`" class="group card flex flex-col p-5 transition duration-300 hover:-translate-y-1 hover:shadow-xl">
        <div class="flex items-center gap-4">
          <div class="grid h-16 w-16 shrink-0 place-items-center overflow-hidden rounded-2xl border border-slate-100 bg-brand-50 dark:border-slate-800 dark:bg-brand-950">
            <img v-if="center.logoUrl" :src="fileUrl(center.logoUrl)" :alt="`لوگوی ${center.name}`" class="h-full w-full object-cover" />
            <Building2 v-else :size="27" class="text-brand-600 dark:text-brand-300" />
          </div>
          <div class="min-w-0">
            <div class="flex items-center gap-1.5 text-xs font-bold text-brand-700 dark:text-brand-300"><ShieldCheck :size="14" /> مرکز همکار</div>
            <h2 class="mt-1 line-clamp-1 text-lg font-black text-slate-900 group-hover:text-brand-700 dark:text-white dark:group-hover:text-brand-300">{{ center.name }}</h2>
            <p v-if="center.province" class="mt-1 flex items-center gap-1 text-xs text-slate-400">
              <MapPin :size="13" /> {{ center.province.name }}<template v-if="center.city">، {{ center.city.name }}</template>
            </p>
          </div>
        </div>
        <p class="mt-4 line-clamp-3 flex-1 text-sm leading-7 text-slate-500 dark:text-slate-400">{{ center.description || 'اطلاعات کامل این مرکز و درخواست‌های آن را در صفحه مرکز مشاهده کنید.' }}</p>
        <div class="mt-4 flex flex-wrap gap-1.5 border-t border-slate-100 pt-4 dark:border-slate-800">
          <span v-for="category in (center.categories || []).slice(0, 3)" :key="category.id" class="chip bg-brand-50 text-brand-800 dark:bg-brand-950 dark:text-brand-200">{{ category.name }}</span>
          <span class="mr-auto grid h-9 w-9 place-items-center rounded-xl bg-slate-50 text-brand-700 transition group-hover:bg-brand-600 group-hover:text-white dark:bg-slate-800 dark:text-brand-300"><ArrowLeft :size="16" /></span>
        </div>
      </RouterLink>
    </div>

    <AppPagination :page="page" :total="total" :size="size" @update:page="page = $event" />
  </div>
</template>
