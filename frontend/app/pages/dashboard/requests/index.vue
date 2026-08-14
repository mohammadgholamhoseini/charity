<script setup lang="ts">
import { ep } from '~/api/endpoints'
import type { Page, RequestStatus, RequestSummary } from '~/types/api'

definePageMeta({ layout: 'dashboard', middleware: 'auth', role: 'CENTER' })

const { $api } = useNuxtApp()
const { shortDate } = useFormat()
const toast = useToast()

// No «در انتظار انتشار» or «رد شده» tab: a centre publishes its own requests, so neither
// status can be reached any more.
const tabs: { value: RequestStatus | '', label: string }[] = [
  { value: '', label: 'همه' },
  { value: 'DRAFT', label: 'پیش‌نویس' },
  { value: 'PUBLISHED', label: 'منتشرشده' },
  { value: 'COMPLETED', label: 'تکمیل‌شده' },
  { value: 'INACTIVE', label: 'غیرفعال' },
]

const activeTab = ref<RequestStatus | ''>('')
const rows = ref<RequestSummary[]>([])
const loading = ref(true)
const loadError = ref(false)
const deleteTarget = ref<RequestSummary | null>(null)
const busy = ref(false)

async function load() {
  loading.value = true
  loadError.value = false
  try {
    const data = await $api<Page<RequestSummary>>(ep.centerRequests, {
      query: { size: 50, sort: 'newest', ...(activeTab.value ? { status: activeTab.value } : {}) },
    })
    rows.value = data.content
  }
  catch {
    loadError.value = true
  }
  finally {
    loading.value = false
  }
}

onMounted(load)
watch(activeTab, load)

async function publish(request: RequestSummary) {
  try {
    await $api(ep.centerRequestSubmit(request.id), { method: 'POST' })
    toast.success('درخواست منتشر شد.')
    load()
  }
  catch (error) {
    toast.error(apiErrorMessage(error))
  }
}

async function confirmDelete() {
  if (!deleteTarget.value) return
  busy.value = true
  try {
    await $api(ep.centerRequest(deleteTarget.value.id), { method: 'DELETE' })
    toast.success('درخواست حذف شد.')
    deleteTarget.value = null
    load()
  }
  catch (error) {
    toast.error(apiErrorMessage(error))
  }
  finally {
    busy.value = false
  }
}

useHead({ title: 'درخواست‌های من — یاری‌جو' })
</script>

<template>
  <div class="flex flex-col gap-6">
    <div class="flex flex-wrap items-center justify-between gap-4">
      <h1 class="text-[24px] font-extrabold">درخواست‌های من</h1>
      <NuxtLink to="/dashboard/requests/new" class="btn btn-primary btn-sm">ثبت درخواست جدید</NuxtLink>
    </div>

    <div class="flex flex-wrap gap-2">
      <button
        v-for="tab in tabs"
        :key="tab.value"
        type="button"
        class="chip transition-colors"
        :class="activeTab === tab.value ? 'chip-dark' : 'chip-neutral'"
        @click="activeTab = tab.value"
      >
        {{ tab.label }}
      </button>
    </div>

    <UiErrorState v-if="loadError" @retry="load()" />

    <div v-else-if="loading" class="card-flat p-6 flex flex-col gap-2">
      <UiSkeleton v-for="n in 4" :key="n" variant="row" />
    </div>

    <UiEmptyState
      v-else-if="!rows.length"
      title="درخواستی در این وضعیت نیست"
      description="با تغییر تب بالا می‌توانید درخواست‌های دیگر را ببینید."
    />

    <section v-else class="card-flat overflow-x-auto">
      <table class="data-table">
        <thead>
          <tr>
            <th>عنوان درخواست</th>
            <th>دسته</th>
            <th>تاریخ ثبت</th>
            <th>وضعیت</th>
            <th>عملیات</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="request in rows" :key="request.id">
            <td>
              <div class="flex flex-col gap-1">
                <span class="font-semibold">{{ request.title }}</span>
                <span v-if="request.statusNote" class="text-[12px] text-danger">
                  دلیل: {{ request.statusNote }}
                </span>
              </div>
            </td>
            <td>{{ request.category?.name ?? '—' }}</td>
            <td class="ltr">{{ shortDate(request.createdAt) }}</td>
            <td><UiChip :label="request.statusLabel" :status="request.status" /></td>
            <td>
              <div class="flex items-center gap-3 text-[13px]">
                <NuxtLink :to="`/dashboard/requests/${request.id}`" class="text-accent hover:text-accent-600">
                  ویرایش
                </NuxtLink>
                <button
                  v-if="request.status === 'DRAFT'"
                  type="button"
                  class="text-accent hover:text-accent-600"
                  @click="publish(request)"
                >
                  انتشار
                </button>
                <button type="button" class="text-danger hover:underline" @click="deleteTarget = request">
                  حذف
                </button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </section>

    <UiConfirm
      :open="Boolean(deleteTarget)"
      title="حذف درخواست"
      :message="`آیا از حذف «${deleteTarget?.title}» مطمئن هستید؟ این درخواست از سایت برداشته می‌شود.`"
      confirm-label="حذف کن"
      tone="danger"
      :busy="busy"
      @confirm="confirmDelete"
      @close="deleteTarget = null"
    />
  </div>
</template>
