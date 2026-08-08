<script setup>
import { ref, onMounted, computed } from 'vue'
import { useAuthStore } from '../stores/auth'
import api from '../api/client'
import PageHeader from '../components/PageHeader.vue'
import UiAlert from '../components/UiAlert.vue'
import { User as UserIcon, Lock, Save, Loader2, ArrowRight, ImageUp } from '@lucide/vue'

const auth = useAuthStore()
const isAdmin = computed(() => auth.isAdmin)

const profile = ref(null)
const provinces = ref([])
const cities = ref([])
const loading = ref(true)
const saving = ref(false)
const msg = ref('')
const err = ref('')

const form = ref({
  fullName: '',
  centerName: '',
  description: '',
  contactPhone: '',
  address: '',
  cardNumber: '',
  sheba: '',
  provinceId: null,
  cityId: null
})

const logoUrl = ref(null)
const logoUploading = ref(false)
const logoInput = ref(null)

function fileUrl(name) {
  return name ? `/api/public/files/${name}` : null
}

async function onLogoChange(e) {
  const file = e.target.files?.[0]
  if (!file) return
  logoUploading.value = true
  err.value = ''
  try {
    const fd = new FormData()
    fd.append('file', file)
    const res = await api.post('/center/me/logo', fd, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    logoUrl.value = res.data.logoUrl
    profile.value = res.data
    msg.value = 'عکس پروفایل با موفقیت به‌روزرسانی شد.'
  } catch (e) {
    err.value = e.response?.data?.message || 'خطا در آپلود عکس.'
  } finally {
    logoUploading.value = false
  }
}

const adminForm = ref({
  fullName: '',
  email: '',
  currentPassword: '',
  newPassword: ''
})

async function loadProfile() {
  loading.value = true
  try {
    if (isAdmin.value) {
      const res = await api.get('/admin/me')
      profile.value = res.data
      adminForm.value.fullName = res.data.fullName || ''
      adminForm.value.email = res.data.email || ''
    } else {
      const res = await api.get('/center/me')
      profile.value = res.data
      logoUrl.value = res.data.logoUrl || null
      form.value.fullName = res.data.fullName || ''
      form.value.centerName = res.data.name || ''
      form.value.description = res.data.description || ''
      form.value.contactPhone = res.data.contactPhone || ''
      form.value.address = res.data.address || ''
      form.value.cardNumber = res.data.cardNumber || ''
      form.value.sheba = res.data.sheba || ''
      form.value.provinceId = res.data.province?.id || null
      form.value.cityId = res.data.city?.id || null
    }
    const p = await api.get('/public/provinces')
    provinces.value = p.data
    if (form.value.provinceId) {
      const c = await api.get('/public/cities', { params: { provinceId: form.value.provinceId } })
      cities.value = c.data
    }
  } catch (e) {
    err.value = 'خطا در بارگذاری اطلاعات پروفایل.'
  } finally {
    loading.value = false
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

async function saveCenter() {
  saving.value = true
  msg.value = ''
  err.value = ''
  try {
    const payload = {
      fullName: form.value.fullName,
      centerName: form.value.centerName,
      description: form.value.description,
      contactPhone: form.value.contactPhone,
      address: form.value.address,
      cardNumber: form.value.cardNumber,
      sheba: form.value.sheba,
      provinceId: form.value.provinceId || null,
      cityId: form.value.cityId || null
    }
    await api.put('/center/me', payload)
    if (auth.user) {
      auth.user.fullName = form.value.fullName
      localStorage.setItem('user', JSON.stringify(auth.user))
    }
    msg.value = 'پروفایل با موفقیت به‌روزرسانی شد.'
  } catch (e) {
    err.value = e.response?.data?.message || 'خطا در ذخیره‌سازی.'
  } finally {
    saving.value = false
  }
}

async function saveAdmin() {
  saving.value = true
  msg.value = ''
  err.value = ''
  try {
    const payload = {
      fullName: adminForm.value.fullName,
      email: adminForm.value.email,
      currentPassword: adminForm.value.currentPassword || undefined,
      newPassword: adminForm.value.newPassword || undefined
    }
    await api.put('/admin/me', payload)
    if (auth.user) {
      auth.user.fullName = adminForm.value.fullName
      auth.user.email = adminForm.value.email
      localStorage.setItem('user', JSON.stringify(auth.user))
    }
    adminForm.value.currentPassword = ''
    adminForm.value.newPassword = ''
    msg.value = 'پروفایل با موفقیت به‌روزرسانی شد.'
  } catch (e) {
    err.value = e.response?.data?.message || 'خطا در ذخیره‌سازی.'
  } finally {
    saving.value = false
  }
}

onMounted(loadProfile)
</script>

<template>
  <div class="max-w-2xl mx-auto px-4 py-12" dir="rtl">
    <RouterLink to="/" class="inline-flex items-center gap-1.5 text-xs text-slate-400 mb-5 hover:text-slate-600 dark:hover:text-slate-200">
      <ArrowRight :size="14" /> بازگشت به سایت
    </RouterLink>

    <PageHeader eyebrow="حساب کاربری" title="پروفایل من" description="اطلاعات هویتی و راه‌های ارتباطی حساب خود را به‌روز نگه دارید." />

    <p v-if="loading" class="text-slate-400">در حال بارگذاری...</p>

    <div v-else class="card p-6 sm:p-8 space-y-5">
      <UiAlert v-if="msg" type="success">{{ msg }}</UiAlert>
      <UiAlert v-if="err" type="error">{{ err }}</UiAlert>

      <!-- Admin -->
      <template v-if="isAdmin">
        <div>
          <label class="label">نام و نام خانوادگی</label>
          <input v-model="adminForm.fullName" required class="input" />
        </div>
        <div>
          <label class="label">ایمیل</label>
          <input v-model="adminForm.email" type="email" required class="input" />
        </div>
        <div>
          <label class="label">نام کاربری</label>
          <input :value="profile.username" disabled class="input opacity-60" />
        </div>
        <div class="border-t border-slate-100 dark:border-slate-800 pt-4">
          <p class="text-sm font-medium text-slate-600 dark:text-slate-300 mb-3">تغییر رمز عبور (اختیاری)</p>
          <div class="grid sm:grid-cols-2 gap-4">
            <div>
              <label class="label">رمز عبور فعلی</label>
              <input v-model="adminForm.currentPassword" type="password" class="input" />
            </div>
            <div>
              <label class="label">رمز عبور جدید</label>
              <input v-model="adminForm.newPassword" type="password" class="input" />
            </div>
          </div>
        </div>
        <button :disabled="saving" @click="saveAdmin" class="btn-primary w-full">
          <Loader2 v-if="saving" :size="18" class="animate-spin" />
          <Save v-else :size="18" />
          {{ saving ? 'در حال ذخیره...' : 'ذخیره تغییرات' }}
        </button>
      </template>

      <!-- Center -->
      <template v-else>
        <div class="flex items-center gap-4 p-4 rounded-2xl bg-slate-50 dark:bg-slate-800/40">
          <div class="shrink-0">
            <img v-if="logoUrl" :src="fileUrl(logoUrl)" alt="لوگو"
              class="w-20 h-20 rounded-2xl object-cover border border-slate-200 dark:border-slate-700" />
            <span v-else class="grid place-items-center w-20 h-20 rounded-2xl bg-brand-100 dark:bg-brand-900/40 text-brand-600 dark:text-brand-300">
              <UserIcon :size="28" />
            </span>
          </div>
          <div class="min-w-0">
            <p class="text-sm font-medium text-slate-700 dark:text-slate-200 mb-2">عکس پروفایل مرکز</p>
            <input ref="logoInput" type="file" accept="image/*" class="hidden" @change="onLogoChange" />
            <button :disabled="logoUploading" @click="logoInput?.click()"
              class="btn-outline text-sm inline-flex items-center gap-1.5">
              <ImageUp :size="16" />
              {{ logoUploading ? 'در حال آپلود...' : 'انتخاب عکس' }}
            </button>
          </div>
        </div>
        <div>
          <label class="label">نام کامل / مسئول</label>
          <input v-model="form.fullName" class="input" />
        </div>
        <div>
          <label class="label">نام مرکز</label>
          <input v-model="form.centerName" class="input" />
        </div>
        <div>
          <label class="label">نام کاربری (غیرقابل تغییر)</label>
          <input :value="profile.user?.username" disabled class="input opacity-60" />
        </div>
        <div class="grid sm:grid-cols-2 gap-4">
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
        <div>
          <label class="label">توضیحات</label>
          <textarea v-model="form.description" rows="3" class="input resize-none"></textarea>
        </div>
        <div class="grid sm:grid-cols-2 gap-4">
          <div>
            <label class="label">شماره تماس</label>
            <input v-model="form.contactPhone" class="input" />
          </div>
          <div>
            <label class="label">شماره کارت</label>
            <input v-model="form.cardNumber" class="input" />
          </div>
        </div>
        <div>
          <label class="label">شماره شبا</label>
          <input v-model="form.sheba" class="input" />
        </div>
        <div>
          <label class="label">آدرس</label>
          <input v-model="form.address" class="input" />
        </div>
        <button :disabled="saving" @click="saveCenter" class="btn-primary w-full">
          <Loader2 v-if="saving" :size="18" class="animate-spin" />
          <Save v-else :size="18" />
          {{ saving ? 'در حال ذخیره...' : 'ذخیره تغییرات' }}
        </button>
      </template>
    </div>
  </div>
</template>
