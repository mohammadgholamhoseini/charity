<script setup>
import { ref, onMounted } from 'vue'
import api from '../../api/client'
import { MapPin, Plus, Pencil, Trash2, Check, X, Loader2, ChevronDown, ChevronUp } from '@lucide/vue'

const provinces = ref([])
const loading = ref(true)
const msg = ref('')
const err = ref('')

/* ---------- Province form ---------- */
const showProvinceForm = ref(false)
const provinceForm = ref({ id: null, name: '' })
const provinceLoading = ref(false)
const provinceError = ref('')

/* ---------- City form ---------- */
const showCityForm = ref(false)
const cityForm = ref({ id: null, name: '', provinceId: null })
const cityLoading = ref(false)
const cityError = ref('')

const expanded = ref({})

async function load() {
  loading.value = true
  try {
    const res = await api.get('/admin/provinces')
    const citiesRes = await api.get('/admin/cities')
    const cities = citiesRes.data || []
    provinces.value = res.data.map((p) => ({
      ...p,
      cities: cities.filter((c) => c.provinceId === p.id)
    }))
  } catch (e) {
    err.value = 'خطا در بارگذاری استان‌ها.'
  } finally {
    loading.value = false
  }
}

function toggle(id) {
  expanded.value = { ...expanded.value, [id]: !expanded.value[id] }
}

/* ----- Provinces ----- */
function openProvinceCreate() {
  provinceForm.value = { id: null, name: '' }
  provinceError.value = ''
  showProvinceForm.value = true
}
function openProvinceEdit(p) {
  provinceForm.value = { id: p.id, name: p.name }
  provinceError.value = ''
  showProvinceForm.value = true
}
async function submitProvince() {
  provinceError.value = ''
  if (!provinceForm.value.name?.trim()) {
    provinceError.value = 'نام استان الزامی است.'
    return
  }
  provinceLoading.value = true
  try {
    if (provinceForm.value.id) {
      await api.put(`/admin/provinces/${provinceForm.value.id}`, { name: provinceForm.value.name })
      msg.value = 'استان ویرایش شد.'
    } else {
      await api.post('/admin/provinces', { name: provinceForm.value.name })
      msg.value = 'استان جدید ایجاد شد.'
    }
    showProvinceForm.value = false
    await load()
  } catch (e) {
    provinceError.value = e.response?.data?.message || 'خطا در ثبت استان.'
  } finally {
    provinceLoading.value = false
  }
}
async function removeProvince(id) {
  if (!confirm('حذف این استان؟ (شهرهای آن نیز حذف می‌شوند)')) return
  try {
    await api.delete(`/admin/provinces/${id}`)
    msg.value = 'استان حذف شد.'
    await load()
  } catch (e) {
    err.value = e.response?.data?.message || 'خطا در حذف استان.'
  }
}

/* ----- Cities ----- */
function openCityCreate(provinceId) {
  cityForm.value = { id: null, name: '', provinceId }
  cityError.value = ''
  showCityForm.value = true
}
function openCityEdit(province, city) {
  cityForm.value = { id: city.id, name: city.name, provinceId: province.id }
  cityError.value = ''
  showCityForm.value = true
}
async function submitCity() {
  cityError.value = ''
  if (!cityForm.value.name?.trim()) {
    cityError.value = 'نام شهر الزامی است.'
    return
  }
  if (!cityForm.value.provinceId) {
    cityError.value = 'انتخاب استان الزامی است.'
    return
  }
  cityLoading.value = true
  try {
    if (cityForm.value.id) {
      await api.put(`/admin/cities/${cityForm.value.id}`, { name: cityForm.value.name, provinceId: cityForm.value.provinceId })
      msg.value = 'شهر ویرایش شد.'
    } else {
      await api.post(`/admin/cities?provinceId=${cityForm.value.provinceId}`, { name: cityForm.value.name })
      msg.value = 'شهر جدید ایجاد شد.'
    }
    showCityForm.value = false
    await load()
  } catch (e) {
    cityError.value = e.response?.data?.message || 'خطا در ثبت شهر.'
  } finally {
    cityLoading.value = false
  }
}
async function removeCity(provinceId, id) {
  if (!confirm('حذف این شهر؟')) return
  try {
    await api.delete(`/admin/cities/${id}`)
    msg.value = 'شهر حذف شد.'
    await load()
  } catch (e) {
    err.value = e.response?.data?.message || 'خطا در حذف شهر.'
  }
}

onMounted(load)
</script>

<template>
  <div>
    <div class="flex items-center justify-between mb-5">
      <div class="flex items-center gap-2.5">
        <span class="grid place-items-center w-10 h-10 rounded-2xl bg-brand-100 dark:bg-brand-900/50 text-brand-600 dark:text-brand-300"><MapPin :size="20" /></span>
        <h1 class="text-2xl font-bold text-slate-800 dark:text-white">مدیریت استان‌ها و شهرها</h1>
      </div>
      <button @click="openProvinceCreate" class="btn-primary text-sm"><Plus :size="16" /> استان جدید</button>
    </div>

    <p v-if="msg" class="text-brand-700 dark:text-brand-300 text-sm mb-3 bg-brand-50 dark:bg-brand-900/20 rounded-xl py-2.5 px-3">{{ msg }}</p>
    <p v-if="err" class="text-red-500 text-sm mb-3 bg-red-50 dark:bg-red-900/20 rounded-xl py-2.5 px-3">{{ err }}</p>

    <!-- Province form -->
    <transition name="modal">
      <div v-if="showProvinceForm" class="card p-6 mb-6">
        <h2 class="text-lg font-bold text-slate-800 dark:text-white mb-4">{{ provinceForm.id ? 'ویرایش' : 'ثبت' }} استان</h2>
        <form @submit.prevent="submitProvince" class="space-y-3">
          <input v-model="provinceForm.name" required placeholder="نام استان" class="input" />
          <p v-if="provinceError" class="text-red-500 text-sm bg-red-50 dark:bg-red-900/20 rounded-xl py-2.5 px-3">{{ provinceError }}</p>
          <div class="flex gap-2">
            <button :disabled="provinceLoading" type="submit" class="btn-primary">
              <Loader2 v-if="provinceLoading" :size="16" class="animate-spin" />
              <Check v-else :size="16" />
              {{ provinceLoading ? 'در حال ثبت...' : 'ذخیره' }}
            </button>
            <button type="button" @click="showProvinceForm = false" class="btn-ghost">انصراف</button>
          </div>
        </form>
      </div>
    </transition>

    <!-- City form -->
    <transition name="modal">
      <div v-if="showCityForm" class="card p-6 mb-6">
        <h2 class="text-lg font-bold text-slate-800 dark:text-white mb-4">{{ cityForm.id ? 'ویرایش' : 'ثبت' }} شهر</h2>
        <form @submit.prevent="submitCity" class="space-y-3">
          <input v-model="cityForm.name" required placeholder="نام شهر" class="input" />
          <select v-model="cityForm.provinceId" class="input bg-white dark:bg-slate-900">
            <option :value="null">انتخاب استان</option>
            <option v-for="p in provinces" :key="p.id" :value="p.id">{{ p.name }}</option>
          </select>
          <p v-if="cityError" class="text-red-500 text-sm bg-red-50 dark:bg-red-900/20 rounded-xl py-2.5 px-3">{{ cityError }}</p>
          <div class="flex gap-2">
            <button :disabled="cityLoading" type="submit" class="btn-primary">
              <Loader2 v-if="cityLoading" :size="16" class="animate-spin" />
              <Check v-else :size="16" />
              {{ cityLoading ? 'در حال ثبت...' : 'ذخیره' }}
            </button>
            <button type="button" @click="showCityForm = false" class="btn-ghost">انصراف</button>
          </div>
        </form>
      </div>
    </transition>

    <div v-if="loading" class="space-y-3">
      <div v-for="i in 3" :key="i" class="card h-16 animate-pulse bg-slate-100 dark:bg-slate-800/50"></div>
    </div>
    <div v-else-if="!provinces.length" class="text-center py-16 card">
      <div class="text-5xl mb-3">🗺️</div>
      <p class="text-slate-500 dark:text-slate-400">استانی ثبت نشده است.</p>
    </div>
    <div v-else class="space-y-3 stagger">
      <div v-for="p in provinces" :key="p.id" class="card overflow-hidden">
        <div class="p-4 flex items-center justify-between gap-4">
          <button @click="toggle(p.id)" class="flex items-center gap-2 min-w-0 text-right">
            <component :is="expanded[p.id] ? ChevronUp : ChevronDown" :size="18" class="text-slate-400" />
            <span class="font-bold text-slate-800 dark:text-white">{{ p.name }}</span>
            <span class="chip bg-slate-100 dark:bg-slate-800 text-slate-400 text-xs">{{ (p.cities || []).length }} شهر</span>
          </button>
          <div class="flex gap-2 shrink-0">
            <button @click="openCityCreate(p.id)" class="text-sm inline-flex items-center gap-1 bg-brand-50 dark:bg-brand-900/30 text-brand-700 dark:text-brand-300 px-3 py-1.5 rounded-lg hover:bg-brand-100 dark:hover:bg-brand-900/50">
              <Plus :size="13" /> شهر
            </button>
            <button @click="openProvinceEdit(p)" class="text-sm inline-flex items-center gap-1 bg-slate-100 dark:bg-slate-800 text-slate-700 dark:text-slate-200 px-3 py-1.5 rounded-lg hover:bg-slate-200 dark:hover:bg-slate-700">
              <Pencil :size="13" /> ویرایش
            </button>
            <button @click="removeProvince(p.id)" class="text-sm inline-flex items-center gap-1 bg-red-50 dark:bg-red-900/20 text-red-600 px-3 py-1.5 rounded-lg hover:bg-red-100 dark:hover:bg-red-900/40">
              <Trash2 :size="13" /> حذف
            </button>
          </div>
        </div>

        <div v-if="expanded[p.id]" class="border-t border-slate-100 dark:border-slate-800 px-4 py-3 space-y-2 bg-slate-50/60 dark:bg-slate-800/30">
          <div v-if="!p.cities || !p.cities.length" class="text-sm text-slate-400 py-2">شهری ثبت نشده است.</div>
          <div v-for="c in (p.cities || [])" :key="c.id" class="flex items-center justify-between gap-4 rounded-xl bg-white dark:bg-slate-900 px-3 py-2">
            <span class="text-slate-700 dark:text-slate-200">{{ c.name }}</span>
            <div class="flex gap-2 shrink-0">
              <button @click="openCityEdit(p, c)" class="text-sm inline-flex items-center gap-1 bg-slate-100 dark:bg-slate-800 text-slate-700 dark:text-slate-200 px-3 py-1.5 rounded-lg hover:bg-slate-200 dark:hover:bg-slate-700">
                <Pencil :size="13" /> ویرایش
              </button>
              <button @click="removeCity(p.id, c.id)" class="text-sm inline-flex items-center gap-1 bg-red-50 dark:bg-red-900/20 text-red-600 px-3 py-1.5 rounded-lg hover:bg-red-100 dark:hover:bg-red-900/40">
                <Trash2 :size="13" /> حذف
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.modal-enter-active, .modal-leave-active { transition: opacity 0.2s ease, transform 0.2s ease; }
.modal-enter-from, .modal-leave-to { opacity: 0; transform: translateY(-8px); }
</style>
