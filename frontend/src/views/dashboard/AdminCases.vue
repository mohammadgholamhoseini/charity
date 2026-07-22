<script setup>
import { ref, onMounted } from 'vue'
import api from '../../api/client'
import EditCaseModal from './EditCaseModal.vue'
import { Pencil, Trash2, CheckCircle2, FileText, Loader2 } from '@lucide/vue'

const cases = ref([])
const loading = ref(true)
const filter = ref('')
const msg = ref('')
const categories = ref([])

const editing = ref(null)
const showModal = ref(false)

async function load() {
  loading.value = true
  try {
    const res = await api.get('/admin/cases', {
      params: { status: filter.value || undefined }
    })
    cases.value = res.data.content
  } finally {
    loading.value = false
  }
}

async function loadCategories() {
  try {
    const res = await api.get('/admin/categories')
    categories.value = res.data || []
  } catch (e) {
    categories.value = []
  }
}

onMounted(() => { load(); loadCategories() })

async function complete(id) {
  msg.value = ''
  try {
    await api.post(`/admin/cases/${id}/complete`)
    msg.value = 'درخواست به وضعیت تأمین شده تغییر یافت.'
    await load()
  } catch (e) {
    msg.value = 'خطا در بروزرسانی وضعیت.'
  }
}

function openEdit(c) {
  editing.value = c
  showModal.value = true
}

async function remove(id) {
  if (!confirm('آیا از حذف این درخواست اطمینان دارید؟')) return
  msg.value = ''
  try {
    await api.delete(`/admin/cases/${id}`)
    await load()
  } catch (e) {
    msg.value = e.response?.data?.message || 'خطا در حذف درخواست.'
  }
}

const statusLabel = {
  PENDING: 'در انتظار', PUBLISHED: 'منتشر', COMPLETED: 'تکمیل', REJECTED: 'رد شده'
}
const statusColor = {
  PENDING: 'bg-amber-50 text-amber-700 dark:bg-amber-900/30 dark:text-amber-300',
  PUBLISHED: 'bg-brand-50 text-brand-700 dark:bg-brand-900/40 dark:text-brand-300',
  COMPLETED: 'bg-slate-100 text-slate-600 dark:bg-slate-800 dark:text-slate-300',
  REJECTED: 'bg-red-50 text-red-600 dark:bg-red-900/30 dark:text-red-300'
}

const filters = [
  { v: '', label: 'همه' },
  { v: 'PENDING', label: 'در انتظار' },
  { v: 'PUBLISHED', label: 'منتشر' },
  { v: 'COMPLETED', label: 'تکمیل' }
]
</script>

<template>
  <div>
    <div class="flex items-center gap-2.5 mb-5">
      <span class="grid place-items-center w-10 h-10 rounded-2xl bg-brand-100 dark:bg-brand-900/50 text-brand-600 dark:text-brand-300"><FileText :size="20" /></span>
      <h1 class="text-2xl font-bold text-slate-800 dark:text-white">مدیریت درخواست‌ها</h1>
    </div>
    <p v-if="msg" class="text-brand-700 dark:text-brand-300 text-sm mb-3 bg-brand-50 dark:bg-brand-900/20 rounded-xl py-2.5 px-3">{{ msg }}</p>

    <div class="flex gap-2 mb-5 flex-wrap">
      <button v-for="f in filters" :key="f.v" @click="filter=f.v; load()"
        class="chip border transition"
        :class="filter === f.v ? 'bg-brand-600 text-white border-brand-600' : 'border-slate-200 dark:border-slate-700 text-slate-600 dark:text-slate-300 hover:border-brand-300'">
        {{ f.label }}
      </button>
    </div>

    <div v-if="loading" class="space-y-3">
      <div v-for="i in 4" :key="i" class="card h-20 animate-pulse bg-slate-100 dark:bg-slate-800/50"></div>
    </div>
    <div v-else-if="!cases.length" class="text-center py-16 card">
      <div class="text-5xl mb-3">📋</div>
      <p class="text-slate-500 dark:text-slate-400">موردی یافت نشد.</p>
    </div>
    <div v-else class="space-y-3 stagger">
      <div v-for="c in cases" :key="c.id" class="card p-4 flex flex-col sm:flex-row sm:items-center justify-between gap-3">
        <div>
          <div class="font-bold text-slate-800 dark:text-white">{{ c.title }} <span class="text-slate-400 dark:text-slate-500 text-sm font-normal">— {{ c.centerName }}</span></div>
          <div class="text-sm text-slate-400 dark:text-slate-500 mt-1">{{ c.categoryName }} | {{ new Intl.NumberFormat('fa-IR').format(c.amountNeeded) }} تومان</div>
        </div>
        <div class="flex items-center gap-2 flex-wrap">
          <span :class="statusColor[c.status]" class="chip font-medium">{{ statusLabel[c.status] }}</span>
          <button v-if="c.status !== 'COMPLETED'" @click="openEdit(c)" class="text-sm inline-flex items-center gap-1 bg-slate-100 dark:bg-slate-800 text-slate-700 dark:text-slate-200 px-3 py-1.5 rounded-lg hover:bg-slate-200 dark:hover:bg-slate-700">
            <Pencil :size="13" /> ویرایش
          </button>
          <button v-if="c.status !== 'COMPLETED'" @click="remove(c.id)" class="text-sm inline-flex items-center gap-1 bg-red-50 dark:bg-red-900/20 text-red-600 px-3 py-1.5 rounded-lg hover:bg-red-100 dark:hover:bg-red-900/40">
            <Trash2 :size="13" /> حذف
          </button>
          <button v-if="c.status === 'PUBLISHED'" @click="complete(c.id)" class="text-sm inline-flex items-center gap-1 bg-emerald-600 text-white px-3 py-1.5 rounded-lg hover:bg-emerald-700">
            <CheckCircle2 :size="13" /> تأمین شده
          </button>
        </div>
      </div>
    </div>

    <EditCaseModal v-if="showModal" :case-id="editing.id" :initial="editing"
      :categories="categories" endpoint="/admin/cases"
      @close="showModal = false" @saved="showModal = false; load()" />
  </div>
</template>
