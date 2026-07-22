<script setup>
import { ref, onMounted } from 'vue'
import api from '../../api/client'
import { Megaphone, Plus, Pencil, Trash2, Check, X, Loader2 } from '@lucide/vue'

const notices = ref([])
const loading = ref(true)
const editing = ref(null)
const form = ref({ title: '', content: '', position: 'FOOTER', active: true })
const error = ref('')

async function load() {
  loading.value = true
  try {
    const res = await api.get('/admin/notices')
    notices.value = res.data
  } finally {
    loading.value = false
  }
}

function reset() {
  editing.value = null
  form.value = { title: '', content: '', position: 'FOOTER', active: true }
}

function edit(n) {
  editing.value = n.id
  form.value = { title: n.title, content: n.content, position: n.position, active: n.active }
}

async function save() {
  error.value = ''
  try {
    if (editing.value) {
      await api.put(`/admin/notices/${editing.value}`, form.value)
    } else {
      await api.post('/admin/notices', form.value)
    }
    reset()
    await load()
  } catch (e) {
    error.value = e.response?.data?.message || 'خطا در ذخیره اطلاعیه.'
  }
}

async function remove(id) {
  if (!confirm('حذف اطلاعیه؟')) return
  try {
    await api.delete(`/admin/notices/${id}`)
    await load()
  } catch (e) {
    error.value = e.response?.data?.message || 'خطا در حذف اطلاعیه.'
  }
}

onMounted(load)
</script>

<template>
  <div class="max-w-3xl">
    <div class="flex items-center gap-2.5 mb-2">
      <span class="grid place-items-center w-10 h-10 rounded-2xl bg-brand-100 dark:bg-brand-900/50 text-brand-600 dark:text-brand-300"><Megaphone :size="20" /></span>
      <h1 class="text-2xl font-bold text-slate-800 dark:text-white">مدیریت اطلاعیه‌ها و شرایط</h1>
    </div>
    <p class="text-sm text-slate-500 dark:text-slate-400 mb-6">اطلاعیه‌های فعال در پاورقی سایت نمایش داده می‌شوند. از این بخش برای اعلام مسئولیت پرداخت استفاده کنید.</p>

    <form @submit.prevent="save" class="card p-6 space-y-3 mb-6">
      <input v-model="form.title" required placeholder="عنوان اطلاعیه" class="input" />
      <textarea v-model="form.content" required rows="3" placeholder="متن اطلاعیه" class="input resize-none"></textarea>
      <div class="flex flex-wrap gap-4 items-center">
        <select v-model="form.position" class="input sm:w-auto flex-1">
          <option value="FOOTER">پاورقی</option>
          <option value="BANNER">بنر بالای صفحه</option>
        </select>
        <label class="flex items-center gap-2 text-sm text-slate-600 dark:text-slate-300 cursor-pointer">
          <input type="checkbox" v-model="form.active" class="w-4 h-4 rounded accent-brand-600" /> فعال
        </label>
      </div>
      <div class="flex gap-2">
        <button type="submit" class="btn-primary">
          <component :is="editing ? Check : Plus" :size="16" />
          {{ editing ? 'بروزرسانی' : 'افزودن' }}
        </button>
        <button v-if="editing" type="button" @click="reset" class="btn-ghost">انصراف</button>
      </div>
      <p v-if="error" class="text-red-500 text-sm bg-red-50 dark:bg-red-900/20 rounded-xl py-2.5 px-3">{{ error }}</p>
    </form>

    <div v-if="loading" class="space-y-3">
      <div v-for="i in 3" :key="i" class="card h-24 animate-pulse bg-slate-100 dark:bg-slate-800/50"></div>
    </div>
    <div v-else-if="!notices.length" class="text-center py-16 card">
      <div class="text-5xl mb-3">📢</div>
      <p class="text-slate-500 dark:text-slate-400">اطلاعیه‌ای ثبت نشده است.</p>
    </div>
    <div v-else class="space-y-3 stagger">
      <div v-for="n in notices" :key="n.id" class="card p-4">
        <div class="flex items-start justify-between gap-4">
          <div class="flex-1">
            <div class="font-bold text-slate-800 dark:text-white flex items-center gap-2 flex-wrap">
              {{ n.title }}
              <span class="chip" :class="n.active ? 'bg-emerald-50 dark:bg-emerald-900/30 text-emerald-700 dark:text-emerald-300' : 'bg-slate-100 dark:bg-slate-800 text-slate-400'">
                {{ n.active ? 'فعال' : 'غیرفعال' }}
              </span>
              <span class="chip bg-slate-100 dark:bg-slate-800 text-slate-500 dark:text-slate-400">{{ n.position === 'BANNER' ? 'بنر' : 'پاورقی' }}</span>
            </div>
            <p class="text-sm text-slate-500 dark:text-slate-400 mt-1.5 leading-7">{{ n.content }}</p>
          </div>
          <div class="flex gap-2 shrink-0">
            <button @click="edit(n)" class="text-sm inline-flex items-center gap-1 bg-slate-100 dark:bg-slate-800 text-slate-700 dark:text-slate-200 px-3 py-1.5 rounded-lg hover:bg-slate-200 dark:hover:bg-slate-700">
              <Pencil :size="13" /> ویرایش
            </button>
            <button @click="remove(n.id)" class="text-sm inline-flex items-center gap-1 bg-red-50 dark:bg-red-900/20 text-red-600 px-3 py-1.5 rounded-lg hover:bg-red-100 dark:hover:bg-red-900/40">
              <Trash2 :size="13" /> حذف
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
