<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import api from '../../api/client'
import { Building2, MapPin, Loader2, Search } from '@lucide/vue'

const router = useRouter()
const centers = ref([])
const total = ref(0)
const page = ref(0)
const size = 9
const q = ref('')
const provinceId = ref(null)
const provinces = ref([])
const loading = ref(true)

async function loadProvinces() {
  try {
    const res = await api.get('/public/provinces')
    provinces.value = res.data
  } catch (e) {}
}

async function load() {
  loading.value = true
  try {
    const res = await api.get('/public/centers', {
      params: {
        page: page.value,
        size,
        provinceId: provinceId.value || undefined,
        q: q.value || undefined
      }
    })
    centers.value = res.data.content
    total.value = res.data.totalElements
  } finally {
    loading.value = false
  }
}

function goCenter(id) {
  router.push(`/center/${id}`)
}

function fileUrl(name) {
  return name ? `/api/public/files/${name}` : null
}

function search() {
  page.value = 0
  load()
}

onMounted(() => { loadProvinces(); load() })
</script>

<template>
  <div class="max-w-6xl mx-auto px-4 py-12" dir="rtl">
    <div class="flex items-center gap-2.5 mb-2">
      <span class="grid place-items-center w-10 h-10 rounded-2xl bg-brand-100 dark:bg-brand-900/50 text-brand-600 dark:text-brand-300">
        <Building2 :size="20" />
      </span>
      <h1 class="text-2xl sm:text-3xl font-extrabold text-slate-800 dark:text-white">مراکز خیریه</h1>
    </div>
    <p class="text-slate-500 dark:text-slate-400 text-sm mb-6">مراکز همکار را مشاهده کرده و با انتخاب هر مرکز، جزئیات و درخواست‌های آن را ببینید.</p>

    <!-- filters -->
    <div class="flex flex-wrap gap-3 mb-8">
      <div class="relative flex-1 min-w-[200px]">
        <Search :size="18" class="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400" />
        <input
          v-model="q"
          @keyup.enter="search"
          type="text"
          placeholder="جستجوی نام مرکز..."
          class="input w-full pr-10"
        />
      </div>
      <select v-model="provinceId" @change="page = 0; load()" class="input w-full sm:w-48 bg-white dark:bg-slate-900">
        <option :value="null">همه استان‌ها</option>
        <option v-for="p in provinces" :key="p.id" :value="p.id">{{ p.name }}</option>
      </select>
    </div>

    <div v-if="loading" class="grid md:grid-cols-3 gap-5">
      <div v-for="i in 6" :key="i" class="card h-56 animate-pulse bg-slate-100 dark:bg-slate-800/50"></div>
    </div>

    <div v-else-if="!centers.length" class="text-center py-16 card">
      <div class="text-5xl mb-3">🏢</div>
      <p class="text-slate-500 dark:text-slate-400">مرکزی با این جستجو یافت نشد.</p>
    </div>

    <div v-else class="grid md:grid-cols-3 gap-5 stagger">
      <button
        v-for="c in centers"
        :key="c.id"
        @click="goCenter(c.id)"
        class="card p-5 text-right hover:shadow-xl hover:shadow-brand-600/10 hover:-translate-y-1 transition-all duration-300 flex flex-col group"
      >
        <div class="flex items-center gap-3 mb-3">
          <div class="shrink-0 w-14 h-14 rounded-2xl bg-slate-100 dark:bg-slate-800 grid place-items-center overflow-hidden">
            <img v-if="c.logoUrl" :src="fileUrl(c.logoUrl)" alt="لوگو" class="w-full h-full object-cover" />
            <Building2 v-else :size="24" class="text-brand-500" />
          </div>
          <div class="min-w-0">
            <h3 class="font-bold text-slate-800 dark:text-white group-hover:text-brand-600 dark:group-hover:text-brand-300 transition line-clamp-1">
              {{ c.name }}
            </h3>
            <p v-if="c.province" class="text-xs text-slate-400 flex items-center gap-1 mt-0.5">
              <MapPin :size="12" /> {{ c.province.name }}<template v-if="c.city"> / {{ c.city.name }}</template>
            </p>
          </div>
        </div>

        <p class="text-sm text-slate-500 dark:text-slate-400 line-clamp-2 mb-3 flex-1">
          {{ c.description || 'بدون توضیحات' }}
        </p>

        <div class="flex flex-wrap gap-1.5 pt-3 border-t border-slate-100 dark:border-slate-800">
          <span v-for="cat in (c.categories || []).slice(0, 3)" :key="cat.id"
            class="chip bg-brand-50 dark:bg-brand-900/40 text-brand-700 dark:text-brand-300">{{ cat.name }}</span>
        </div>
      </button>
    </div>

    <div v-if="total > size" class="flex justify-center gap-2 mt-10">
      <button :disabled="page === 0" @click="page--" class="btn-ghost disabled:opacity-40">قبلی</button>
      <span class="px-4 py-2 text-slate-500 dark:text-slate-400 text-sm self-center">صفحه {{ page + 1 }}</span>
      <button :disabled="(page + 1) * size >= total" @click="page++" class="btn-ghost disabled:opacity-40">بعدی</button>
    </div>
  </div>
</template>
