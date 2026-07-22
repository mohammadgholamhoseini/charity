<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import api from '../../api/client'
import { Plus, Upload, FileText, CheckCircle2, Loader2, X } from '@lucide/vue'

const router = useRouter()
const categories = ref([])
const form = ref({
  title: '', categoryId: '', description: '', amountNeeded: '',
  imageUrl: '', contactInfo: '', urgency: 'MEDIUM', details: {}
})
const detailKeys = ref([])
const error = ref('')
const success = ref('')
const loading = ref(false)
const createdId = ref(null)
const files = ref([])
const uploading = ref(false)

async function loadCenter() {
  try {
    const res = await api.get('/center/me')
    categories.value = res.data.categories || []
  } catch (e) {
    categories.value = []
  }
}

function addDetail() {
  detailKeys.value.push({ key: '', value: '' })
}
function removeDetail(i) {
  detailKeys.value.splice(i, 1)
}
function buildDetails() {
  const d = {}
  for (const row of detailKeys.value) {
    if (row.key.trim()) d[row.key.trim()] = row.value
  }
  return d
}

async function submit() {
  error.value = ''
  success.value = ''
  loading.value = true
  try {
    const details = buildDetails()
    if (form.value.details.beneficiaryName) details.beneficiaryName = form.value.details.beneficiaryName
    const payload = {
      title: form.value.title,
      categoryId: Number(form.value.categoryId),
      description: form.value.description || null,
      amountNeeded: Number(form.value.amountNeeded),
      imageUrl: form.value.imageUrl || null,
      contactInfo: form.value.contactInfo || null,
      urgency: form.value.urgency,
      details: Object.keys(details).length ? details : null
    }
    const res = await api.post('/center/cases', payload)
    createdId.value = res.data.id
    success.value = 'درخواست ثبت و منتشر شد. در صورت تمایل مدارک را بارگذاری کنید.'
    if (files.value.length) {
      await uploadDocuments()
    }
    if (error.value) {
      success.value = ''
    }
    setTimeout(() => router.push('/dashboard/cases'), 1500)
  } catch (e) {
    error.value = e.response?.data?.message || 'خطا در ثبت درخواست. مرکز شما باید تایید شده باشد.'
  } finally {
    loading.value = false
  }
}

async function uploadDocuments() {
  if (!createdId.value || !files.value.length) return
  uploading.value = true
  try {
    const fd = new FormData()
    for (const f of files.value) fd.append('files', f)
    await api.post(`/center/cases/${createdId.value}/documents`, fd, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  } catch (e) {
    error.value = 'درخواست ثبت شد اما بارگذاری مدارک با خطا مواجه شد.'
  } finally {
    uploading.value = false
  }
}

onMounted(loadCenter)
</script>

<template>
  <div class="max-w-2xl">
    <div class="flex items-center gap-2.5 mb-6">
      <span class="grid place-items-center w-10 h-10 rounded-2xl bg-brand-100 dark:bg-brand-900/50 text-brand-600 dark:text-brand-300"><Plus :size="20" /></span>
      <h1 class="text-2xl font-bold text-slate-800 dark:text-white">ثبت درخواست کمک جدید</h1>
    </div>

    <form @submit.prevent="submit" class="card p-6 sm:p-8 space-y-5">
      <div>
        <label class="label">عنوان درخواست</label>
        <input v-model="form.title" required placeholder="مثلاً کمک به درمان بیماری قلبی"
          class="input" />
      </div>

      <div>
        <label class="label">دسته‌بندی</label>
        <select v-model="form.categoryId" required
          class="input bg-white dark:bg-slate-900">
          <option value="">دسته‌بندی را انتخاب کنید (از دسته‌های مجاز مرکز)</option>
          <option v-for="c in categories" :key="c.id" :value="c.id">{{ c.name }}</option>
        </select>
      </div>

      <div>
        <label class="label">سطح فوریت</label>
        <select v-model="form.urgency" class="input bg-white dark:bg-slate-900">
          <option value="LOW">کم</option>
          <option value="MEDIUM">متوسط</option>
          <option value="HIGH">بالا</option>
          <option value="URGENT">فوری</option>
        </select>
      </div>

      <div>
        <label class="label">نام ذینفع (اختیاری)</label>
        <input v-model="form.details.beneficiaryName" placeholder="نام بیمار یا نیازمند"
          class="input" />
      </div>

      <div>
        <label class="label">توضیحات تکمیلی</label>
        <textarea v-model="form.description" rows="3" placeholder="شرح وضعیت و نیاز..."
          class="input resize-none"></textarea>
      </div>

      <div class="grid sm:grid-cols-2 gap-4">
        <div>
          <label class="label">مبلغ مورد نیاز (تومان)</label>
          <input v-model="form.amountNeeded" type="number" required placeholder="مثلاً ۵۰۰۰۰۰۰"
            class="input" />
        </div>
        <div>
          <label class="label">اطلاعات تماس</label>
          <input v-model="form.contactInfo" placeholder="شماره تماس یا روش ارتباط"
            class="input" />
        </div>
      </div>

      <div>
        <label class="label">آدرس تصویر (اختیاری - URL)</label>
        <input v-model="form.imageUrl" placeholder="https://..."
          class="input" />
      </div>

      <!-- Custom fields -->
      <div class="border border-dashed border-slate-200 dark:border-slate-700 rounded-2xl p-4">
        <div class="flex items-center justify-between mb-3">
          <span class="text-sm font-medium text-slate-600 dark:text-slate-300 flex items-center gap-1.5"><FileText :size="15" class="text-brand-500" /> فیلدهای اختصاصی (اختیاری)</span>
          <button type="button" @click="addDetail" class="text-xs inline-flex items-center gap-1 text-brand-700 dark:text-brand-300 font-medium hover:underline">
            <Plus :size="13" /> افزودن فیلد
          </button>
        </div>
        <div v-for="(row, i) in detailKeys" :key="i" class="grid grid-cols-2 gap-2 mb-2 items-center">
          <input v-model="row.key" placeholder="نام فیلد (مثلاً سن)"
            class="px-3 py-2 rounded-xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-900 text-sm focus:border-brand-500 outline-none" />
          <div class="flex gap-2">
            <input v-model="row.value" placeholder="مقدار"
              class="flex-1 px-3 py-2 rounded-xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-900 text-sm focus:border-brand-500 outline-none" />
            <button type="button" @click="removeDetail(i)" class="grid place-items-center w-9 h-9 rounded-xl text-red-500 hover:bg-red-50 dark:hover:bg-red-900/20">
              <X :size="16" />
            </button>
          </div>
        </div>
      </div>

      <!-- Documents -->
      <div class="border border-dashed border-slate-200 dark:border-slate-700 rounded-2xl p-4">
        <div class="text-sm font-medium text-slate-600 dark:text-slate-300 mb-2 flex items-center gap-1.5"><Upload :size="15" class="text-brand-500" /> بارگذاری مدارک و مستندات (اختیاری)</div>
        <input type="file" multiple @change="files = Array.from($event.target.files)"
          class="w-full text-sm text-slate-500 file:ml-3 file:py-2 file:px-4 file:rounded-xl file:border-0 file:bg-brand-50 dark:file:bg-brand-900/40 file:text-brand-700 dark:file:text-brand-300 hover:file:bg-brand-100 dark:hover:file:bg-brand-900/60 cursor-pointer" />
        <p v-if="files.length" class="text-xs text-slate-400 mt-2">{{ files.length }} فایل انتخاب شد.</p>
      </div>

      <p v-if="error" class="text-red-500 text-sm bg-red-50 dark:bg-red-900/20 rounded-xl py-2.5 px-3">{{ error }}</p>
      <p v-if="success" class="text-brand-700 dark:text-brand-300 text-sm font-medium bg-brand-50 dark:bg-brand-900/20 rounded-xl py-2.5 px-3 flex items-center gap-1.5">
        <CheckCircle2 :size="16" /> {{ success }}
      </p>
      <p v-if="uploading" class="text-slate-400 text-sm flex items-center gap-1.5"><Loader2 :size="15" class="animate-spin" /> در حال بارگذاری مدارک...</p>

      <button :disabled="loading" type="submit" class="btn-primary w-full">
        <Loader2 v-if="loading" :size="18" class="animate-spin" />
        <CheckCircle2 v-else :size="18" />
        {{ loading ? 'در حال ثبت...' : 'ثبت درخواست' }}
      </button>
    </form>
  </div>
</template>
