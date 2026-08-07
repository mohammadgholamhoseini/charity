<script setup>
import { ref, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api from '../../api/client'
import CaseCard from '../../components/CaseCard.vue'
import { Search, LayoutGrid, SlidersHorizontal } from '@lucide/vue'

const route = useRoute()
const router = useRouter()
const cases = ref([])
const total = ref(0)
const page = ref(0)
const size = 9
const q = ref('')
const categoryId = ref(null)
const categories = ref([])
const loading = ref(true)

const provinces = ref([])
const cities = ref([])
const provinceId = ref(null)
const cityId = ref(null)

async function loadCategories() {
  try {
    const res = await api.get('/public/categories')
    categories.value = res.data
  } catch (e) {}
}
async function loadProvinces() {
  try {
    const res = await api.get('/public/provinces')
    provinces.value = res.data
  } catch (e) {}
}
async function loadCities() {
  if (!provinceId.value) {
    cities.value = []
    cityId.value = null
    return
  }
  try {
    const res = await api.get('/public/cities', { params: { provinceId: provinceId.value } })
    cities.value = res.data
  } catch (e) {}
}

async function load() {
  loading.value = true
  try {
    const res = await api.get('/public/cases', {
      params: {
        page: page.value,
        size,
        q: q.value || undefined,
        categoryId: categoryId.value || undefined,
        provinceId: provinceId.value || undefined,
        cityId: cityId.value || undefined,
        centerId: route.query.centerId || undefined,
        status: filter.value || undefined
      }
    })
    cases.value = res.data.content
    total.value = res.data.totalElements
  } finally {
    loading.value = false
  }
}

function search() {
  page.value = 0
  load()
}
const filter = ref('')
function setFilter(status) {
  filter.value = filter.value === status ? '' : status
  page.value = 0
  load()
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
function onCityChange() {
  page.value = 0
  load()
}

onMounted(() => {
  if (route.query.status) {
    filter.value = route.query.status
  }
  loadCategories(); loadProvinces(); load()
})
watch(page, load)
</script>

<template>
  <div class="max-w-6xl mx-auto px-4 py-12">
    <!-- Header -->
    <div class="relative overflow-hidden card p-8 mb-9 bg-gradient-to-br from-brand-50 to-brand-50 dark:from-brand-900/20 dark:to-brand-900/10">
      <div class="absolute -left-10 -bottom-10 w-40 h-40 bg-brand-300/20 rounded-full blur-2xl"></div>
      <div class="relative flex items-center gap-3 mb-2">
        <span class="grid place-items-center w-11 h-11 rounded-2xl bg-gradient-to-br from-brand-500 to-brand-600 text-white shadow-lg shadow-brand-600/30">
          <LayoutGrid :size="22" />
        </span>
        <div>
          <h1 class="text-2xl sm:text-3xl font-extrabold text-slate-800 dark:text-white">درخواست‌های کمک</h1>
          <p class="text-slate-500 dark:text-slate-400 text-sm mt-0.5">لیست درخواست‌هایی که منتظر یاری شما هستند.</p>
        </div>
      </div>
    </div>

    <!-- Controls -->
    <div class="flex flex-col lg:flex-row lg:items-center gap-4 mb-6">
      <div class="relative flex-1">
        <Search :size="18" class="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400" />
        <input
          v-model="q"
          @keyup.enter="search"
          type="text"
          placeholder="جستجو بر اساس عنوان یا توضیحات..."
          class="input pr-10"
        />
      </div>
      <div class="flex items-center gap-1.5 text-slate-400 text-sm shrink-0">
        <SlidersHorizontal :size="16" />
        <span>فیلترها:</span>
      </div>
    </div>

    <!-- Filters -->
    <div class="flex flex-wrap gap-3 mb-6">
      <select v-model="provinceId" @change="onProvinceChange" class="input w-full sm:w-48">
        <option :value="null">همه استان‌ها</option>
        <option v-for="p in provinces" :key="p.id" :value="p.id">{{ p.name }}</option>
      </select>
      <select v-model="cityId" @change="onCityChange" :disabled="!provinceId" class="input w-full sm:w-48 disabled:opacity-50">
        <option :value="null">همه شهرها</option>
        <option v-for="c in cities" :key="c.id" :value="c.id">{{ c.name }}</option>
      </select>
      <button @click="provinceId=null; cityId=null; cities=[]; page=0; load()" class="btn-ghost">حذف فیلتر مکان</button>
    </div>

    <!-- Status filter -->
    <div class="flex flex-wrap gap-2 mb-4">
      <button @click="setFilter('')" class="chip border transition"
        :class="!filter ? 'bg-brand-600 text-white border-brand-600' : 'border-slate-200 dark:border-slate-700 text-slate-600 dark:text-slate-300 hover:border-brand-300'">
        همه
      </button>
      <button @click="setFilter('PUBLISHED')" class="chip border transition"
        :class="filter === 'PUBLISHED' ? 'bg-brand-600 text-white border-brand-600' : 'border-slate-200 dark:border-slate-700 text-slate-600 dark:text-slate-300 hover:border-brand-300'">
        فعال
      </button>
      <button @click="setFilter('COMPLETED')" class="chip border transition"
        :class="filter === 'COMPLETED' ? 'bg-brand-600 text-white border-brand-600' : 'border-slate-200 dark:border-slate-700 text-slate-600 dark:text-slate-300 hover:border-brand-300'">
        تأمین شده
      </button>
    </div>

    <!-- Categories -->
    <div class="flex flex-wrap gap-2 mb-8">
      <button
        @click="pickCategory(null)"
        class="chip border transition"
        :class="!categoryId ? 'bg-brand-600 text-white border-brand-600' : 'border-slate-200 dark:border-slate-700 text-slate-600 dark:text-slate-300 hover:border-brand-300'"
      >
        همه
      </button>
      <button
        v-for="cat in categories"
        :key="cat.id"
        @click="pickCategory(cat.id)"
        class="chip border transition"
        :class="categoryId === cat.id ? 'bg-brand-600 text-white border-brand-600' : 'border-slate-200 dark:border-slate-700 text-slate-600 dark:text-slate-300 hover:border-brand-300'"
      >
        {{ cat.name }}
      </button>
    </div>

    <!-- Grid -->
    <div v-if="loading" class="grid md:grid-cols-3 gap-5">
      <div v-for="i in 6" :key="i" class="card h-64 animate-pulse bg-slate-100 dark:bg-slate-800/50"></div>
    </div>
    <div v-else-if="!cases.length" class="text-center py-16 card">
      <div class="text-5xl mb-3">🔍</div>
      <p class="text-slate-500 dark:text-slate-400">موردی با این جستجو یافت نشد.</p>
    </div>
    <div v-else class="grid md:grid-cols-3 gap-5 stagger">
      <CaseCard v-for="item in cases" :key="item.id" :item="item" />
    </div>

    <div v-if="total > size" class="flex justify-center gap-2 mt-10">
      <button :disabled="page === 0" @click="page--" class="btn-ghost disabled:opacity-40">قبلی</button>
      <span class="px-4 py-2 text-slate-500 dark:text-slate-400 text-sm self-center">صفحه {{ page + 1 }}</span>
      <button :disabled="(page + 1) * size >= total" @click="page++" class="btn-ghost disabled:opacity-40">بعدی</button>
    </div>
  </div>
</template>
