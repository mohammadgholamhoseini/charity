<script setup>
import { ref, onMounted } from 'vue'
import api from '../../api/client'
import { Building2, Plus, Check, X, Loader2, Mail, Phone, Tag, FileText } from '@lucide/vue'

const centers = ref([])
const categories = ref([])
const provinces = ref([])
const cities = ref([])
const loading = ref(true)
const msg = ref('')
const showForm = ref(false)

const form = ref({
  username: '', password: '', email: '', fullName: '',
  centerName: '', categoryIds: [], description: '',
  contactPhone: '', address: '', cardNumber: '', sheba: '',
  provinceId: null, cityId: null
})
const formError = ref('')
const formLoading = ref(false)

async function load() {
  loading.value = true
  try {
    const c = await api.get('/admin/centers')
    centers.value = Array.isArray(c.data) ? c.data : (c.data.content || [])
  } catch (e) {
    msg.value = 'خطا در بارگذاری لیست مراکز.'
  }
  try {
    const cat = await api.get('/admin/categories')
    categories.value = Array.isArray(cat.data) ? cat.data : (cat.data.content || [])
  } catch (e) {
    msg.value = 'خطا در بارگذاری دسته‌بندی‌ها.'
  }
  try {
    const p = await api.get('/public/provinces')
    provinces.value = p.data
  } catch (e) {}
  loading.value = false
}
async function approve(id) {
  msg.value = ''
  try {
    await api.post(`/admin/centers/${id}/approve`)
    msg.value = 'مرکز تایید شد و اکنون می‌تواند درخواست ثبت کند.'
    await load()
  } catch (e) {
    msg.value = e.response?.data?.message || 'خطا در تایید مرکز.'
  }
}
async function reject(id) {
  try {
    await api.post(`/admin/centers/${id}/reject`)
    await load()
  } catch (e) {
    msg.value = e.response?.data?.message || 'خطا در رد مرکز.'
  }
}
async function deactivate(id) {
  if (!confirm('غیرفعال کردن مرکز باعث مخفی شدن تمام درخواست‌های فعال آن می‌شود. ادامه می‌دهید؟')) return
  try {
    await api.post(`/admin/centers/${id}/deactivate`)
    msg.value = 'مرکز غیرفعال شد.'
    await load()
  } catch (e) {
    msg.value = e.response?.data?.message || 'خطا در غیرفعال‌سازی مرکز.'
  }
}
async function activate(id) {
  try {
    await api.post(`/admin/centers/${id}/activate`)
    await load()
  } catch (e) {
    msg.value = e.response?.data?.message || 'خطا در فعال‌سازی مرکز.'
  }
}
async function remove(id) {
  if (!confirm('حذف مرکز غیرقابل بازگشت است. ادامه می‌دهید؟')) return
  try {
    await api.delete(`/admin/centers/${id}`)
    msg.value = 'مرکز حذف شد.'
    await load()
  } catch (e) {
    msg.value = e.response?.data?.message || 'خطا در حذف مرکز.'
  }
}
async function onProvinceChange() {
  form.value.cityId = null
  if (form.value.provinceId) {
    const res = await api.get('/public/cities', { params: { provinceId: form.value.provinceId } })
    cities.value = res.data
  } else {
    cities.value = []
  }
}
function openForm() {
  form.value = {
    username: '', password: '', email: '', fullName: '',
    centerName: '', categoryIds: [], description: '',
    contactPhone: '', address: '', cardNumber: '', sheba: '',
    provinceId: null, cityId: null
  }
  cities.value = []
  formError.value = ''
  showForm.value = true
}
async function submit() {
  formError.value = ''
  formLoading.value = true
  try {
    const payload = {
      ...form.value,
      categoryIds: (form.value.categoryIds || []).map(Number),
      provinceId: form.value.provinceId || null,
      cityId: form.value.cityId || null
    }
    await api.post('/admin/centers', payload)
    msg.value = 'مرکز جدید توسط ادمین ایجاد شد.'
    showForm.value = false
    await load()
  } catch (e) {
    formError.value = e.response?.data?.message || 'خطا در ثبت مرکز.'
  } finally {
    formLoading.value = false
  }
}
onMounted(load)

const statusLabel = {
  PENDING: 'در انتظار تایید', APPROVED: 'تایید شده', REJECTED: 'رد شده', INACTIVE: 'غیرفعال'
}
const statusColor = {
  PENDING: 'bg-amber-50 text-amber-700 dark:bg-amber-900/30 dark:text-amber-300',
  APPROVED: 'bg-brand-50 text-brand-700 dark:bg-brand-900/40 dark:text-brand-300',
  REJECTED: 'bg-red-50 text-red-600 dark:bg-red-900/30 dark:text-red-300',
  INACTIVE: 'bg-slate-100 text-slate-600 dark:bg-slate-800 dark:text-slate-400'
}
</script>

<template>
  <div>
    <div class="flex items-center justify-between mb-5">
      <div class="flex items-center gap-2.5">
        <span class="grid place-items-center w-10 h-10 rounded-2xl bg-brand-100 dark:bg-brand-900/50 text-brand-600 dark:text-brand-300"><Building2 :size="20" /></span>
        <h1 class="text-2xl font-bold text-slate-800 dark:text-white">مدیریت مراکز خیریه</h1>
      </div>
      <button @click="openForm" class="btn-primary text-sm"><Plus :size="16" /> ثبت مرکز جدید</button>
    </div>
    <p v-if="msg" class="text-brand-700 dark:text-brand-300 text-sm mb-3 bg-brand-50 dark:bg-brand-900/20 rounded-xl py-2.5 px-3">{{ msg }}</p>

    <!-- Form -->
    <transition name="modal">
      <div v-if="showForm" class="card p-6 mb-6">
        <h2 class="text-lg font-bold text-slate-800 dark:text-white mb-4">ثبت مرکز جدید</h2>
        <form @submit.prevent="submit" class="space-y-3">
          <div class="grid sm:grid-cols-2 gap-3">
            <input v-model="form.username" required placeholder="نام کاربری" class="input" />
            <input v-model="form.password" type="password" required placeholder="رمز عبور (۶+ کاراکتر)" class="input" />
          </div>
          <input v-model="form.email" type="email" required placeholder="ایمیل" class="input" />
          <input v-model="form.centerName" required placeholder="نام مرکز (مثلاً بیمارستان امام)" class="input" />
          <input v-model="form.fullName" placeholder="نام کامل / مسئول (اختیاری)" class="input" />
          <div>
            <label class="label">دسته‌بندی‌های مجاز (چندتایی - Ctrl/⌘ برای انتخاب چندگانه)</label>
            <select v-model="form.categoryIds" multiple class="input bg-white dark:bg-slate-900 min-h-[100px]">
              <option v-for="c in categories" :key="c.id" :value="c.id">{{ c.name }}</option>
            </select>
          </div>
          <div class="grid sm:grid-cols-2 gap-3">
            <div>
              <label class="label">استان</label>
              <select v-model="form.provinceId" @change="onProvinceChange" class="input bg-white dark:bg-slate-900">
                <option :value="null">انتخاب استان</option>
                <option v-for="p in provinces" :key="p.id" :value="p.id">{{ p.name }}</option>
              </select>
            </div>
            <div>
              <label class="label">شهر</label>
              <select v-model="form.cityId" :disabled="!form.provinceId" class="input bg-white dark:bg-slate-900 disabled:opacity-50">
                <option :value="null">انتخاب شهر</option>
                <option v-for="c in cities" :key="c.id" :value="c.id">{{ c.name }}</option>
              </select>
            </div>
          </div>
          <textarea v-model="form.description" rows="2" placeholder="توضیحات مرکز" class="input resize-none"></textarea>
          <div class="grid sm:grid-cols-2 gap-3">
            <input v-model="form.contactPhone" placeholder="شماره تماس" class="input" />
            <input v-model="form.cardNumber" placeholder="شماره کارت (اختیاری)" class="input" />
          </div>
          <input v-model="form.address" placeholder="آدرس (اختیاری)" class="input" />

          <p v-if="formError" class="text-red-500 text-sm bg-red-50 dark:bg-red-900/20 rounded-xl py-2.5 px-3">{{ formError }}</p>

          <div class="flex gap-2">
            <button :disabled="formLoading" type="submit" class="btn-primary">
              <Loader2 v-if="formLoading" :size="16" class="animate-spin" />
              <Check v-else :size="16" />
              {{ formLoading ? 'در حال ثبت...' : 'ثبت مرکز' }}
            </button>
            <button type="button" @click="showForm = false" class="btn-ghost">انصراف</button>
          </div>
        </form>
      </div>
    </transition>

    <div v-if="loading" class="space-y-3">
      <div v-for="i in 3" :key="i" class="card h-28 animate-pulse bg-slate-100 dark:bg-slate-800/50"></div>
    </div>
    <div v-else-if="!centers.length" class="text-center py-16 card">
      <div class="text-5xl mb-3">🏥</div>
      <p class="text-slate-500 dark:text-slate-400">مرکزی ثبت نشده است.</p>
    </div>
    <div v-else class="space-y-3 stagger">
      <div v-for="c in centers" :key="c.id" class="card p-5">
        <div class="flex flex-col sm:flex-row sm:items-start justify-between gap-4">
          <div class="flex-1">
            <div class="flex items-center gap-2">
              <span class="grid place-items-center w-9 h-9 rounded-xl bg-brand-50 dark:bg-brand-900/40 text-brand-600 dark:text-brand-300">🏢</span>
              <div class="font-bold text-slate-800 dark:text-white">{{ c.name }}</div>
            </div>
            <div class="text-sm text-slate-400 dark:text-slate-500 mt-2 flex flex-wrap items-center gap-x-3 gap-y-1">
              <span class="inline-flex items-center gap-1"><Mail :size="13" /> {{ c.user?.email }}</span>
              <span v-if="c.contactPhone" class="inline-flex items-center gap-1"><Phone :size="13" /> {{ c.contactPhone }}</span>
            </div>
            <div class="text-xs text-slate-500 dark:text-slate-400 mt-2 flex items-center gap-1.5 flex-wrap">
              <Tag :size="12" class="text-brand-500" />
              <span v-for="cat in (c.categories || [])" :key="cat.id" class="chip bg-brand-50 dark:bg-brand-900/40 text-brand-700 dark:text-brand-300">{{ cat.name }}</span>
              <span v-if="!c.categories?.length" class="text-slate-400">بدون دسته</span>
            </div>
            <p v-if="c.description" class="text-sm text-slate-500 dark:text-slate-400 mt-2">{{ c.description }}</p>
            <p v-if="c.fullName" class="text-xs text-slate-500 dark:text-slate-400 mt-1">مسئول: {{ c.fullName }}</p>
            <p v-if="c.province?.name || c.city?.name" class="text-xs text-slate-500 dark:text-slate-400 mt-1">
              مکان: {{ c.province?.name }}{{ c.city?.name ? ' / ' + c.city?.name : '' }}
            </p>
          </div>
           <div class="flex items-center gap-2 shrink-0">
             <span :class="statusColor[c.status]" class="chip font-medium">{{ statusLabel[c.status] }}</span>
             <button v-if="c.status !== 'APPROVED'" @click="approve(c.id)" class="text-sm inline-flex items-center gap-1 bg-brand-600 text-white px-3 py-1.5 rounded-lg hover:bg-brand-700">
               <Check :size="13" /> تایید
             </button>
             <button v-if="c.status === 'APPROVED'" @click="deactivate(c.id)" class="text-sm inline-flex items-center gap-1 bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-300 px-3 py-1.5 rounded-lg hover:bg-slate-200 dark:hover:bg-slate-700">
               <X :size="13" /> غیرفعال‌سازی
             </button>
             <button v-if="c.status === 'INACTIVE'" @click="activate(c.id)" class="text-sm inline-flex items-center gap-1 bg-brand-50 dark:bg-brand-900/40 text-brand-700 dark:text-brand-300 px-3 py-1.5 rounded-lg hover:bg-brand-100 dark:hover:bg-brand-900/60">
               <Check :size="13" /> فعال‌سازی
             </button>
             <button v-if="c.status !== 'REJECTED'" @click="reject(c.id)" class="text-sm inline-flex items-center gap-1 bg-red-50 dark:bg-red-900/20 text-red-600 px-3 py-1.5 rounded-lg hover:bg-red-100 dark:hover:bg-red-900/40">
               <X :size="13" /> رد
             </button>
             <button @click="remove(c.id)" class="text-sm inline-flex items-center gap-1 bg-red-50 dark:bg-red-900/20 text-red-600 px-3 py-1.5 rounded-lg hover:bg-red-100 dark:hover:bg-red-900/40">
               <FileText :size="13" /> حذف
             </button>
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
