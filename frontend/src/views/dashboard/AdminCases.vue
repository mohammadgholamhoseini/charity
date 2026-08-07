<script setup>
import { ref, onMounted } from 'vue'
import api from '../../api/client'
import EditCaseModal from './EditCaseModal.vue'
import PageHeader from '../../components/PageHeader.vue'
import EmptyState from '../../components/EmptyState.vue'
import StatusBadge from '../../components/StatusBadge.vue'
import UiAlert from '../../components/UiAlert.vue'
import { Pencil, Trash2, CheckCircle2, FileText } from '@lucide/vue'

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

const filters = [
  { v: '', label: 'همه' },
  { v: 'PENDING', label: 'در انتظار' },
  { v: 'PUBLISHED', label: 'منتشر' },
  { v: 'COMPLETED', label: 'تکمیل' }
]
</script>

<template>
  <div>
    <PageHeader eyebrow="نظارت محتوا" title="مدیریت درخواست‌ها" description="وضعیت، محتوای درخواست و اطلاعات مرکز ثبت‌کننده را بررسی و مدیریت کنید." />
    <UiAlert v-if="msg" class="mb-4">{{ msg }}</UiAlert>

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
    <EmptyState v-else-if="!cases.length" title="درخواستی در این وضعیت نیست" description="فیلتر دیگری را انتخاب کنید." :icon="FileText" />
    <div v-else class="space-y-3 stagger">
      <div v-for="c in cases" :key="c.id" class="card p-4 flex flex-col sm:flex-row sm:items-center justify-between gap-3">
        <div>
          <div class="font-bold text-slate-800 dark:text-white">{{ c.title }} <span class="text-slate-400 dark:text-slate-500 text-sm font-normal">— {{ c.centerName }}</span></div>
          <div class="text-sm text-slate-400 dark:text-slate-500 mt-1">{{ c.categoryName }} | {{ new Intl.NumberFormat('fa-IR').format(c.amountNeeded) }} تومان</div>
        </div>
        <div class="flex items-center gap-2 flex-wrap">
          <StatusBadge :status="c.status" />
          <button v-if="c.status !== 'COMPLETED'" @click="openEdit(c)" class="action-button bg-slate-100 text-slate-700 hover:bg-slate-200 dark:bg-slate-800 dark:text-slate-200 dark:hover:bg-slate-700">
            <Pencil :size="13" /> ویرایش
          </button>
          <button v-if="c.status !== 'COMPLETED'" @click="remove(c.id)" class="action-button bg-red-50 text-red-600 hover:bg-red-100 dark:bg-red-950/40 dark:text-red-300">
            <Trash2 :size="13" /> حذف
          </button>
          <button v-if="c.status === 'PUBLISHED'" @click="complete(c.id)" class="action-button bg-emerald-600 text-white hover:bg-emerald-700">
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
