<script setup>
import { ref, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import api from '../../api/client'
import CaseCard from '../../components/CaseCard.vue'
import PageHeader from '../../components/PageHeader.vue'
import EmptyState from '../../components/EmptyState.vue'
import AppPagination from '../../components/AppPagination.vue'
import { HeartHandshake, Search, SlidersHorizontal, X } from '@lucide/vue'

const route = useRoute()
const cases = ref([])
const total = ref(0)
const page = ref(0)
const size = 9
const q = ref('')
const filter = ref('')
const categoryId = ref(null)
const categories = ref([])
const loading = ref(true)
const error = ref('')
const provinces = ref([])
const cities = ref([])
const provinceId = ref(null)
const cityId = ref(null)

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

async function load() {
  loading.value = true
  error.value = ''
  try {
    const response = await api.get('/public/cases', {
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
    cases.value = response.data.content
    total.value = response.data.totalElements
  } catch {
    error.value = 'درخواست‌ها دریافت نشدند. اتصال خود را بررسی و دوباره تلاش کنید.'
    cases.value = []
  } finally {
    loading.value = false
  }
}

function applyFilter() {
  page.value = 0
  load()
}

function setStatus(status) {
  filter.value = status
  applyFilter()
}

function pickCategory(id) {
  categoryId.value = categoryId.value === id ? null : id
  applyFilter()
}

function onProvinceChange() {
  cityId.value = null
  loadCities()
  applyFilter()
}

function resetFilters() {
  q.value = ''
  filter.value = ''
  categoryId.value = null
  provinceId.value = null
  cityId.value = null
  cities.value = []
  applyFilter()
}

onMounted(() => {
  filter.value = route.query.status || ''
  loadCategories()
  loadProvinces()
  load()
})
watch(page, load)
</script>

<template>
  <div class="page-shell">
    <PageHeader
      eyebrow="فرصت‌های یاری"
      title="درخواست‌های کمک"
      description="درخواست‌ها را بر اساس موضوع، وضعیت و مکان پیدا کنید و پیش از هر اقدامی جزئیات و مرکز ثبت‌کننده را بررسی کنید."
    />

    <section class="card mb-8 p-4 sm:p-6" aria-label="جستجو و فیلتر درخواست‌ها">
      <div class="flex flex-col gap-3 lg:flex-row">
        <div class="relative flex-1">
          <Search :size="18" class="pointer-events-none absolute right-4 top-1/2 -translate-y-1/2 text-slate-400" />
          <input v-model="q" class="input pr-11" placeholder="جستجو در عنوان و توضیحات..." @keyup.enter="applyFilter" />
        </div>
        <button class="btn-primary text-sm" @click="applyFilter"><Search :size="17" /> جستجو</button>
        <button class="btn-ghost text-sm" @click="resetFilters"><X :size="17" /> پاک‌کردن فیلترها</button>
      </div>

      <div class="mt-5 flex items-center gap-2 text-sm font-bold text-slate-700 dark:text-slate-200">
        <SlidersHorizontal :size="17" class="text-brand-600 dark:text-brand-300" /> فیلترهای دقیق‌تر
      </div>

      <div class="mt-3 grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
        <select v-model="provinceId" class="input" @change="onProvinceChange">
          <option :value="null">همه استان‌ها</option>
          <option v-for="province in provinces" :key="province.id" :value="province.id">{{ province.name }}</option>
        </select>
        <select v-model="cityId" :disabled="!provinceId" class="input disabled:opacity-50" @change="applyFilter">
          <option :value="null">همه شهرها</option>
          <option v-for="city in cities" :key="city.id" :value="city.id">{{ city.name }}</option>
        </select>
        <div class="flex gap-2 overflow-x-auto sm:col-span-2 lg:col-span-1">
          <button v-for="item in [{v:'',l:'همه'}, {v:'PUBLISHED',l:'فعال'}, {v:'COMPLETED',l:'تأمین‌شده'}]" :key="item.v"
            class="chip shrink-0 border"
            :class="filter === item.v ? 'border-brand-600 bg-brand-600 text-white' : 'border-slate-200 text-slate-600 dark:border-slate-700 dark:text-slate-300'"
            @click="setStatus(item.v)">
            {{ item.l }}
          </button>
        </div>
      </div>

      <div class="mt-4 flex gap-2 overflow-x-auto pb-1">
        <button class="chip shrink-0 border" :class="!categoryId ? 'border-brand-600 bg-brand-600 text-white' : 'border-slate-200 text-slate-600 dark:border-slate-700 dark:text-slate-300'" @click="pickCategory(null)">همه موضوع‌ها</button>
        <button v-for="category in categories" :key="category.id" class="chip shrink-0 border"
          :class="categoryId === category.id ? 'border-brand-600 bg-brand-600 text-white' : 'border-slate-200 text-slate-600 dark:border-slate-700 dark:text-slate-300'"
          @click="pickCategory(category.id)">
          {{ category.name }}
        </button>
      </div>
    </section>

    <div v-if="loading" class="grid gap-5 md:grid-cols-2 lg:grid-cols-3">
      <div v-for="index in 6" :key="index" class="card h-72 animate-pulse bg-slate-100 dark:bg-slate-800/50"></div>
    </div>
    <EmptyState v-else-if="error" title="خطا در دریافت درخواست‌ها" :description="error">
      <button class="btn-primary text-sm" @click="load">تلاش دوباره</button>
    </EmptyState>
    <EmptyState v-else-if="!cases.length" title="درخواستی پیدا نشد" description="عبارت جستجو یا فیلترها را تغییر دهید." :icon="HeartHandshake" />
    <div v-else class="stagger grid gap-5 md:grid-cols-2 lg:grid-cols-3">
      <CaseCard v-for="item in cases" :key="item.id" :item="item" />
    </div>

    <AppPagination :page="page" :total="total" :size="size" @update:page="page = $event" />
  </div>
</template>
