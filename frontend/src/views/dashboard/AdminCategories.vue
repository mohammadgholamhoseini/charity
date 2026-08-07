<script setup>
import { ref, onMounted } from 'vue'
import api from '../../api/client'
import { Tags, Plus, Pencil, Trash2, Check, X, Loader2 } from '@lucide/vue'

const categories = ref([])
const loading = ref(true)
const msg = ref('')
const showForm = ref(false)

const blank = () => ({ id: null, name: '', description: '', iconUrl: '', active: true })
const form = ref(blank())
const formError = ref('')
const formLoading = ref(false)

async function load() {
  loading.value = true
  try {
    const res = await api.get('/admin/categories')
    categories.value = res.data
  } finally {
    loading.value = false
  }
}

function openCreate() {
  form.value = blank()
  formError.value = ''
  showForm.value = true
}
function openEdit(c) {
  form.value = { id: c.id, name: c.name, description: c.description || '', iconUrl: c.iconUrl || '', active: c.active !== false }
  formError.value = ''
  showForm.value = true
}
async function submit() {
  formError.value = ''
  formLoading.value = true
  try {
    if (form.value.id) {
      await api.put(`/admin/categories/${form.value.id}`, form.value)
      msg.value = 'دسته‌بندی ویرایش شد.'
    } else {
      await api.post('/admin/categories', form.value)
      msg.value = 'دسته‌بندی جدید ایجاد شد.'
    }
    showForm.value = false
    await load()
  } catch (e) {
    formError.value = e.response?.data?.message || 'خطا در ثبت دسته‌بندی.'
  } finally {
    formLoading.value = false
  }
}
async function remove(id) {
  if (!confirm('حذف این دسته‌بندی؟')) return
  try {
    await api.delete(`/admin/categories/${id}`)
    msg.value = 'دسته‌بندی حذف شد.'
    await load()
  } catch (e) {
    msg.value = e.response?.data?.message || 'خطا در حذف دسته‌بندی.'
  }
}

onMounted(load)
</script>

<template>
  <div>
    <div class="flex items-center justify-between mb-5">
      <div class="flex items-center gap-2.5">
        <span class="grid place-items-center w-10 h-10 rounded-2xl bg-brand-100 dark:bg-brand-900/50 text-brand-600 dark:text-brand-300"><Tags :size="20" /></span>
        <h1 class="text-2xl font-bold text-slate-800 dark:text-white">مدیریت دسته‌بندی‌ها</h1>
      </div>
      <button @click="openCreate" class="btn-primary text-sm"><Plus :size="16" /> دسته‌بندی جدید</button>
    </div>
    <p v-if="msg" class="text-brand-700 dark:text-brand-300 text-sm mb-3 bg-brand-50 dark:bg-brand-900/20 rounded-xl py-2.5 px-3">{{ msg }}</p>

    <transition name="modal">
      <div v-if="showForm" class="card p-6 mb-6">
        <h2 class="text-lg font-bold text-slate-800 dark:text-white mb-4">{{ form.id ? 'ویرایش' : 'ثبت' }} دسته‌بندی</h2>
        <form @submit.prevent="submit" class="space-y-3">
          <input v-model="form.name" required placeholder="نام دسته‌بندی" class="input" />
          <textarea v-model="form.description" rows="2" placeholder="توضیحات" class="input resize-none"></textarea>
          <input v-model="form.iconUrl" placeholder="آیکون (اختیاری - URL)" class="input" />
          <label class="flex items-center gap-2 text-sm text-slate-600 dark:text-slate-300 cursor-pointer">
            <input type="checkbox" v-model="form.active" class="w-4 h-4 rounded accent-brand-600" /> فعال باشد
          </label>

          <p v-if="formError" class="text-red-500 text-sm bg-red-50 dark:bg-red-900/20 rounded-xl py-2.5 px-3">{{ formError }}</p>

          <div class="flex gap-2">
            <button :disabled="formLoading" type="submit" class="btn-primary">
              <Loader2 v-if="formLoading" :size="16" class="animate-spin" />
              <Check v-else :size="16" />
              {{ formLoading ? 'در حال ثبت...' : 'ذخیره' }}
            </button>
            <button type="button" @click="showForm = false" class="btn-ghost">انصراف</button>
          </div>
        </form>
      </div>
    </transition>

    <div v-if="loading" class="space-y-3">
      <div v-for="i in 3" :key="i" class="card h-20 animate-pulse bg-slate-100 dark:bg-slate-800/50"></div>
    </div>
    <div v-else-if="!categories.length" class="text-center py-16 card">
      <Tags :size="42" class="mx-auto mb-3 text-brand-500" />
      <p class="text-slate-500 dark:text-slate-400">دسته‌بندی‌ای ثبت نشده است.</p>
    </div>
    <div v-else class="space-y-3 stagger">
      <div v-for="c in categories" :key="c.id" class="card p-4 flex items-center justify-between gap-4">
        <div>
          <div class="font-bold text-slate-800 dark:text-white flex items-center gap-2">
            {{ c.name }}
            <span v-if="c.active" class="chip bg-emerald-50 dark:bg-emerald-900/30 text-emerald-700 dark:text-emerald-300">فعال</span>
            <span v-else class="chip bg-slate-100 dark:bg-slate-800 text-slate-400">غیرفعال</span>
          </div>
          <p v-if="c.description" class="text-sm text-slate-500 dark:text-slate-400 mt-1">{{ c.description }}</p>
        </div>
        <div class="flex gap-2 shrink-0">
          <button @click="openEdit(c)" class="text-sm inline-flex items-center gap-1 bg-slate-100 dark:bg-slate-800 text-slate-700 dark:text-slate-200 px-3 py-1.5 rounded-lg hover:bg-slate-200 dark:hover:bg-slate-700">
            <Pencil :size="13" /> ویرایش
          </button>
          <button @click="remove(c.id)" class="text-sm inline-flex items-center gap-1 bg-red-50 dark:bg-red-900/20 text-red-600 px-3 py-1.5 rounded-lg hover:bg-red-100 dark:hover:bg-red-900/40">
            <Trash2 :size="13" /> حذف
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.modal-enter-active, .modal-leave-active { transition: opacity 0.2s ease, transform 0.2s ease; }
.modal-enter-from, .modal-leave-to { opacity: 0; transform: translateY(-8px); }
</style>
