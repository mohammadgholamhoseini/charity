<script setup lang="ts">
import { ep } from '~/api/endpoints'
import type { CategoryResponse, CenterResponse, Page, RequestStatus } from '~/types/api'

definePageMeta({ layout: 'dashboard', middleware: 'auth', role: 'ADMIN' })

const { $api } = useNuxtApp()

const stats = ref<Record<RequestStatus, number> | null>(null)
const centerCount = ref(0)
const categoryCount = ref(0)
const loading = ref(true)
const loadError = ref(false)

async function load() {
  loading.value = true
  loadError.value = false
  try {
    const [statsData, centers, categories] = await Promise.all([
      $api<Record<RequestStatus, number>>(ep.adminRequestStats),
      $api<Page<CenterResponse>>(ep.adminCenters, { query: { size: 1 } }),
      $api<CategoryResponse[]>(ep.adminCategories),
    ])
    stats.value = statsData
    centerCount.value = centers.totalElements
    categoryCount.value = categories.length
  }
  catch { loadError.value = true }
  finally { loading.value = false }
}

onMounted(load)

const shortcuts = [
  { label: 'درخواست‌ها', to: '/dashboard/admin/requests', hint: 'غیرفعال‌کردن با ثبت دلیل، ویرایش سئو' },
  { label: 'مدیریت مراکز خیریه', to: '/dashboard/admin/centers', hint: 'ایجاد حساب و تعیین دسته‌های مجاز' },
  { label: 'دسته‌بندی‌ها', to: '/dashboard/admin/categories', hint: 'نام، نشانی یکتا و رنگ برچسب' },
  { label: 'اطلاعیه‌ها', to: '/dashboard/admin/announcements', hint: 'بنر بالای صفحه و پاورقی' },
]

useHead({ title: 'داشبورد ادمین — یاری‌جو' })
</script>

<template>
  <div class="flex flex-col gap-6">
    <h1 class="text-[24px] font-extrabold">داشبورد ادمین</h1>

    <UiErrorState v-if="loadError" @retry="load()" />

    <template v-else>
      <div class="grid gap-4 grid-cols-2 lg:grid-cols-4">
        <template v-if="loading">
          <div v-for="n in 4" :key="n" class="card-flat p-5"><UiSkeleton :lines="2" /></div>
        </template>
        <template v-else>
          <UiStatCard :value="stats?.PUBLISHED ?? 0" label="منتشرشده" />
          <UiStatCard :value="stats?.INACTIVE ?? 0" label="غیرفعال" />
          <UiStatCard :value="centerCount" label="مرکز خیریه" />
          <UiStatCard :value="categoryCount" label="دسته‌بندی" />
        </template>
      </div>

      <div class="grid gap-4 sm:grid-cols-2">
        <NuxtLink
          v-for="shortcut in shortcuts"
          :key="shortcut.to"
          :to="shortcut.to"
          class="card-flat p-5 flex flex-col gap-1.5 hover:border-line transition-colors"
        >
          <span class="text-[16px] font-bold">{{ shortcut.label }}</span>
          <span class="help">{{ shortcut.hint }}</span>
        </NuxtLink>
      </div>
    </template>
  </div>
</template>
