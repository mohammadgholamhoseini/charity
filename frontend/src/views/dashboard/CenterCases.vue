<script setup>
import { ref, onMounted } from 'vue'
import { RouterLink } from 'vue-router'
import api from '../../api/client'
import EditCaseModal from './EditCaseModal.vue'
import PageHeader from '../../components/PageHeader.vue'
import EmptyState from '../../components/EmptyState.vue'
import StatusBadge from '../../components/StatusBadge.vue'
import { Plus, Pencil, Trash2, CheckCircle2, FileText, HandHeart } from '@lucide/vue'

const cases = ref([])
const loading = ref(true)
const categories = ref([])

const editing = ref(null)
const showModal = ref(false)

async function load() {
  loading.value = true
  try {
    const res = await api.get('/center/cases')
    cases.value = res.data.content
  } finally {
    loading.value = false
  }
}

async function loadCenter() {
  try {
    const res = await api.get('/center/me')
    categories.value = res.data.categories || []
  } catch (e) {
    categories.value = []
  }
}

onMounted(() => { load(); loadCenter() })

async function complete(id) {
  try {
    await api.post(`/center/cases/${id}/complete`)
    await load()
  } catch (e) {
    alert(e.response?.data?.message || 'خطا در بروزرسانی وضعیت.')
  }
}

function openEdit(c) {
  editing.value = c
  showModal.value = true
}

async function remove(id) {
  if (!confirm('آیا از حذف این درخواست اطمینان دارید؟')) return
  try {
    await api.delete(`/center/cases/${id}`)
    await load()
  } catch (e) {
    alert(e.response?.data?.message || 'خطا در حذف درخواست.')
  }
}
</script>

<template>
  <div>
    <PageHeader eyebrow="مدیریت درخواست" title="درخواست‌های من" description="درخواست‌های مرکز را ویرایش، پیگیری یا پس از تأمین تکمیل کنید.">
      <template #actions><RouterLink to="/dashboard/cases/new" class="btn-primary text-sm"><Plus :size="16" /> ثبت درخواست جدید</RouterLink></template>
    </PageHeader>

    <div v-if="loading" class="space-y-3">
      <div v-for="i in 3" :key="i" class="card h-20 animate-pulse bg-slate-100 dark:bg-slate-800/50"></div>
    </div>
    <EmptyState v-else-if="!cases.length" title="هنوز درخواستی ثبت نشده" description="اولین درخواست مرکز را با اطلاعات کامل و شفاف ثبت کنید." :icon="FileText">
      <RouterLink to="/dashboard/cases/new" class="btn-primary mt-4 text-sm"><Plus :size="16" /> ثبت اولین درخواست</RouterLink>
    </EmptyState>

    <div v-else class="space-y-3 stagger">
      <div v-for="c in cases" :key="c.id" class="card p-4 flex flex-col sm:flex-row sm:items-center justify-between gap-3 hover:shadow-md transition">
        <div class="flex items-center gap-3">
          <span class="grid place-items-center w-11 h-11 rounded-xl bg-brand-50 dark:bg-brand-950 text-brand-600 dark:text-brand-300 shrink-0"><HandHeart :size="20" /></span>
          <div>
            <div class="font-bold text-slate-800 dark:text-white">{{ c.title }}</div>
            <div class="text-sm text-slate-400 dark:text-slate-500">{{ c.categoryName }} — {{ new Intl.NumberFormat('fa-IR').format(c.amountNeeded) }} تومان</div>
          </div>
        </div>
        <div class="flex items-center gap-2 flex-wrap">
          <StatusBadge :status="c.status" />
          <template v-if="c.status !== 'COMPLETED'">
            <button @click="openEdit(c)" class="text-xs inline-flex items-center gap-1 bg-slate-100 dark:bg-slate-800 text-slate-700 dark:text-slate-200 px-3 py-1.5 rounded-lg hover:bg-slate-200 dark:hover:bg-slate-700">
              <Pencil :size="13" /> ویرایش
            </button>
            <button @click="remove(c.id)" class="text-xs inline-flex items-center gap-1 bg-red-50 dark:bg-red-900/20 text-red-600 px-3 py-1.5 rounded-lg hover:bg-red-100 dark:hover:bg-red-900/40">
              <Trash2 :size="13" /> حذف
            </button>
          </template>
          <button v-if="c.status === 'PUBLISHED'" @click="complete(c.id)"
            class="text-xs inline-flex items-center gap-1 bg-emerald-600 text-white px-3 py-1.5 rounded-lg hover:bg-emerald-700">
            <CheckCircle2 :size="13" /> تأمین شده
          </button>
        </div>
      </div>
    </div>

    <EditCaseModal v-if="showModal" :case-id="editing.id" :initial="editing"
      :categories="categories" endpoint="/center/cases"
      @close="showModal = false" @saved="showModal = false; load()" />
  </div>
</template>
