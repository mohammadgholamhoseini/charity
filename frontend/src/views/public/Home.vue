<script setup>
import { ref, onMounted, watch } from 'vue'
import api from '../../api/client'
import CaseCard from '../../components/CaseCard.vue'
import { HandHeart, Users, CheckCircle2 } from '@lucide/vue'

const cases = ref([])
const total = ref(0)
const page = ref(0)
const size = 9
const categoryId = ref(null)
const categories = ref([])
const loading = ref(true)
const stats = ref({ active: 0, completed: 0, centers: 0 })

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
  } catch (e) {}
}

async function load() {
  loading.value = true
  try {
    const res = await api.get('/public/cases', {
          params: {
            page: page.value,
            size,
            categoryId: categoryId.value || undefined,
            provinceId: provinceId.value || undefined,
            cityId: cityId.value || undefined
          }
    })
    cases.value = res.data.content
    total.value = res.data.totalElements
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
function onCityChange() {
  page.value = 0
  load()
}

onMounted(() => { loadCategories(); loadProvinces(); loadStats(); load() })
watch(page, load)
</script>

<template>
  <div>
    <!-- Minimal Hero -->
    <section class="relative overflow-hidden bg-gradient-to-br from-brand-600 via-brand-700 to-emerald-800 text-white">
      <div class="absolute -top-24 -right-20 w-96 h-96 bg-emerald-400/20 rounded-full blur-3xl"></div>
      <div class="relative max-w-6xl mx-auto px-4 pt-14 pb-12">
        <h1 class="text-3xl md:text-4xl font-extrabold mb-3 leading-tight">
          دستان مهربانی را به هم برسانیم
        </h1>
        <p class="text-brand-50/90 max-w-xl mb-5 leading-7 text-sm md:text-base">
          مراکز خیریه درخواست‌های کمک را ثبت می‌کنند و ما آن‌ها را با دست‌های یاری‌رسان پیوند می‌دهیم.
        </p>

        <!-- compact stats -->
        <div class="flex flex-wrap gap-6 mt-6">
          <div class="flex items-center gap-2">
            <span class="grid place-items-center w-9 h-9 rounded-xl bg-white/15"><HandHeart :size="18" /></span>
            <div>
              <div class="text-lg font-extrabold leading-none">{{ new Intl.NumberFormat('fa-IR').format(stats.active) }}</div>
              <div class="text-xs text-brand-50/80">درخواست فعال</div>
            </div>
          </div>
          <div class="flex items-center gap-2">
            <span class="grid place-items-center w-9 h-9 rounded-xl bg-white/15"><CheckCircle2 :size="18" /></span>
            <div>
              <div class="text-lg font-extrabold leading-none">{{ new Intl.NumberFormat('fa-IR').format(stats.completed) }}</div>
              <div class="text-xs text-brand-50/80">کمک انجام‌شده</div>
            </div>
          </div>
          <div class="flex items-center gap-2">
            <span class="grid place-items-center w-9 h-9 rounded-xl bg-white/15"><Users :size="18" /></span>
            <div>
              <div class="text-lg font-extrabold leading-none">{{ new Intl.NumberFormat('fa-IR').format(stats.centers) }}</div>
              <div class="text-xs text-brand-50/80">مرکز همکار</div>
            </div>
          </div>
        </div>
      </div>
      <svg class="block w-full text-[var(--color-surface)] dark:text-[#0a1411] -mb-1" viewBox="0 0 1440 60" preserveAspectRatio="none">
        <path fill="currentColor" d="M0,60 C480,0 960,0 1440,60 L1440,60 L0,60 Z"></path>
      </svg>
    </section>

    <!-- Cases -->
    <section id="cases" class="max-w-6xl mx-auto px-4 py-12">
      <div class="flex flex-col sm:flex-row sm:items-end justify-between gap-4 mb-6">
        <div>
          <h2 class="text-2xl sm:text-3xl font-extrabold text-slate-800 dark:text-white">درخواست‌های کمک</h2>
          <p class="text-slate-500 dark:text-slate-400 text-sm mt-1">با انتخاب هر مورد، جزئیات و راه‌های کمک را ببینید.</p>
        </div>
      </div>

      <!-- filters -->
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

      <div class="flex flex-wrap gap-2 mb-7">
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
        <button :disabled="page === 0" @click="page--"
          class="btn-ghost disabled:opacity-40">قبلی</button>
        <span class="px-4 py-2 text-slate-500 dark:text-slate-400 text-sm self-center">صفحه {{ page + 1 }}</span>
        <button :disabled="(page + 1) * size >= total" @click="page++"
          class="btn-ghost disabled:opacity-40">بعدی</button>
      </div>
    </section>
  </div>
</template>
