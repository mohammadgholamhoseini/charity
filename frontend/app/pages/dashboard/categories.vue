<script setup lang="ts">
import { ep } from '~/api/endpoints'
import type { CenterResponse } from '~/types/api'

definePageMeta({ layout: 'dashboard', middleware: 'auth', role: 'CENTER' })

const { $api } = useNuxtApp()

const center = ref<CenterResponse | null>(null)
const loading = ref(true)
const loadError = ref(false)

async function load() {
  loading.value = true
  loadError.value = false
  try { center.value = await $api<CenterResponse>(ep.centerMe) }
  catch { loadError.value = true }
  finally { loading.value = false }
}

onMounted(load)

useHead({ title: 'دسته‌های مجاز مرکز — یاری‌جو' })
</script>

<template>
  <div class="flex flex-col gap-6 max-w-2xl">
    <div class="flex flex-col gap-1">
      <h1 class="text-[24px] font-extrabold">دسته‌های مجاز مرکز</h1>
      <p class="text-[14px] text-muted">
        مرکز شما فقط در این دسته‌ها می‌تواند درخواست ثبت کند. تغییر آن‌ها توسط ادمین انجام می‌شود.
      </p>
    </div>

    <UiErrorState v-if="loadError" @retry="load()" />
    <div v-else-if="loading" class="card-flat p-6"><UiSkeleton :lines="3" /></div>

    <UiEmptyState
      v-else-if="!center?.categories.length"
      title="هنوز دسته‌بندی مجازی تعیین نشده است"
      description="برای تعیین دسته‌های مجاز مرکز خود با ادمین تماس بگیرید."
    >
      <NuxtLink to="/contact" class="btn btn-secondary">تماس با پشتیبانی</NuxtLink>
    </UiEmptyState>

    <section v-else class="card-flat p-6 flex flex-col gap-4">
      <div class="flex flex-wrap gap-2">
        <UiChip
          v-for="category in center.categories"
          :key="category.id"
          :label="category.name"
          :color="{ bg: category.labelBg, text: category.labelText }"
        />
      </div>
      <p class="help border-t border-surface-3 pt-4">
        اگر دسته‌بندی دیگری لازم دارید، از ادمین بخواهید آن را به فهرست مجاز مرکز اضافه کند.
      </p>
    </section>
  </div>
</template>
