<script setup lang="ts">
import { ep } from '~/api/endpoints'
import type { CenterResponse, Page, RequestStatus, RequestSummary } from '~/types/api'

definePageMeta({ layout: 'dashboard', middleware: 'auth', role: 'CENTER' })

const { $api } = useNuxtApp()
const { num, shortDate } = useFormat()

const stats = ref<Record<RequestStatus, number> | null>(null)
const center = ref<CenterResponse | null>(null)
const recent = ref<RequestSummary[]>([])
const loading = ref(true)
const loadError = ref(false)

async function load() {
  loading.value = true
  loadError.value = false
  try {
    const [statsData, centerData, requestsData] = await Promise.all([
      $api<Record<RequestStatus, number>>(ep.centerRequestStats),
      $api<CenterResponse>(ep.centerMe),
      $api<Page<RequestSummary>>(ep.centerRequests, { query: { size: 5, sort: 'newest' } }),
    ])
    stats.value = statsData
    center.value = centerData
    recent.value = requestsData.content
  }
  catch {
    loadError.value = true
  }
  finally {
    loading.value = false
  }
}

onMounted(load)

const cards = computed(() => [
  { key: 'PUBLISHED' as const, label: 'منتشرشده' },
  { key: 'DRAFT' as const, label: 'پیش‌نویس' },
  { key: 'INACTIVE' as const, label: 'غیرفعال' },
  { key: 'COMPLETED' as const, label: 'تکمیل‌شده' },
])

useHead({ title: 'داشبورد مرکز — یاری‌جو' })
</script>

<template>
  <div class="flex flex-col gap-6">
    <div class="flex flex-col gap-1">
      <h1 class="text-[24px] font-extrabold">
        درخواست‌های {{ center?.name ?? 'مرکز شما' }}
      </h1>
      <p v-if="stats" class="text-[14px] text-muted">
        {{ num(stats.PUBLISHED) }} درخواست فعال · {{ num(stats.DRAFT) }} پیش‌نویس
      </p>
    </div>

    <UiErrorState v-if="loadError" @retry="load()" />

    <template v-else>
      <div class="grid gap-4 grid-cols-2 lg:grid-cols-4">
        <template v-if="loading">
          <div v-for="n in 4" :key="n" class="card-flat p-5"><UiSkeleton :lines="2" /></div>
        </template>
        <UiStatCard
          v-for="card in cards"
          v-else
          :key="card.key"
          :value="stats?.[card.key] ?? 0"
          :label="card.label"
        />
      </div>

      <section class="card-flat overflow-hidden">
        <div class="flex items-center justify-between gap-4 px-6 py-4 border-b border-line-soft">
          <h2 class="text-[17px] font-bold">آخرین درخواست‌ها</h2>
          <NuxtLink to="/dashboard/requests" class="text-[13px] text-brick-500 hover:text-brick-600">
            همه درخواست‌ها ←
          </NuxtLink>
        </div>

        <div v-if="loading" class="p-6"><UiSkeleton variant="row" /></div>

        <UiEmptyState
          v-else-if="!recent.length"
          title="هنوز درخواستی ثبت نکرده‌اید"
          description="اولین درخواست مرکز خود را ثبت کنید تا پس از تأیید ادمین در سایت منتشر شود."
        >
          <NuxtLink to="/dashboard/requests/new" class="btn btn-primary">ثبت درخواست جدید</NuxtLink>
        </UiEmptyState>

        <div v-else class="overflow-x-auto">
          <table class="data-table">
            <thead>
              <tr>
                <th>عنوان درخواست</th>
                <th>دسته</th>
                <th>تاریخ ثبت</th>
                <th>وضعیت</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="request in recent" :key="request.id">
                <td class="font-semibold">{{ request.title }}</td>
                <td>{{ request.category?.name ?? '—' }}</td>
                <td class="ltr">{{ shortDate(request.createdAt) }}</td>
                <td><UiChip :label="request.statusLabel" :status="request.status" /></td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>
    </template>
  </div>
</template>
