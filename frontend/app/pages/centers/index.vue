<script setup lang="ts">
import { ep } from '~/api/endpoints'
import type { CenterCard, Page } from '~/types/api'

const { $api } = useNuxtApp()
const { num } = useFormat()
const { page, apiQuery } = useListQuery({})

const { data, pending, error, refresh } = await useFetch<Page<CenterCard>>(ep.centers, {
  $fetch: $api,
  query: apiQuery,
})

useSeo(() => ({
  title: page.value > 1 ? `مراکز خیریه — صفحه ${page.value} — یاری‌جو` : 'مراکز خیریه — یاری‌جو',
  description: 'فهرست مراکز خیریه فعال در یاری‌جو، به همراه شهر، دسته‌بندی‌های مجاز و تعداد درخواست فعال هر مرکز.',
}))
</script>

<template>
  <div>
    <div class="page-shell pt-8 pb-6 flex flex-col gap-4">
      <LayoutBreadcrumbs :crumbs="[{ name: 'مراکز خیریه', path: '/centers' }]" />
      <h1 class="text-[26px] lg:text-[34px] font-extrabold">مراکز خیریه</h1>
      <p class="text-[15px] leading-8 text-body max-w-[640px]">
        همه مراکز فهرست‌شده توسط ادمین یاری‌جو ثبت و تأیید شده‌اند. برای همکاری، مستقیماً با مرکز تماس بگیرید.
      </p>
    </div>

    <div class="bg-cream-100 py-8">
      <div class="page-shell">
        <div v-if="pending" class="grid gap-6 md:grid-cols-2 lg:grid-cols-3">
          <UiSkeleton v-for="n in 6" :key="n" variant="card" />
        </div>

        <UiErrorState v-else-if="error" @retry="refresh()" />

        <UiEmptyState
          v-else-if="!data?.content.length"
          title="هنوز مرکزی ثبت نشده است"
          description="به‌زودی مراکز خیریه در این بخش نمایش داده می‌شوند."
        />

        <div v-else class="grid gap-6 md:grid-cols-2 lg:grid-cols-3 items-start">
          <NuxtLink
            v-for="center in data.content"
            :key="center.id"
            :to="`/centers/${encodeURIComponent(center.slug)}`"
            class="card p-6 flex flex-col gap-4 h-full hover:border-line transition-colors"
          >
            <div class="flex items-center gap-3">
              <BrandMark :size="38" />
              <h2 class="text-[18px] font-bold leading-7">{{ center.name }}</h2>
            </div>

            <p v-if="center.description" class="text-[14px] leading-7 text-body line-clamp-3">
              {{ center.description }}
            </p>

            <div class="flex flex-wrap gap-1.5">
              <UiChip
                v-for="category in center.categories.slice(0, 3)"
                :key="category.id"
                :label="category.name"
                :color="{ bg: category.labelBg, text: category.labelText }"
              />
            </div>

            <dl class="border-t border-cream-200 pt-4 mt-auto flex items-center justify-between text-[13px]">
              <div class="flex flex-col">
                <dt class="text-muted">شهر</dt>
                <dd class="text-body-2 font-semibold">{{ center.city?.name ?? '—' }}</dd>
              </div>
              <div class="flex flex-col text-end">
                <dt class="text-muted">درخواست فعال</dt>
                <dd class="text-body-2 font-semibold">{{ num(center.activeRequestCount) }}</dd>
              </div>
            </dl>
          </NuxtLink>
        </div>

        <UiPagination
          v-if="data"
          :page="page"
          :total-pages="data.totalPages"
          :total-elements="data.totalElements"
        />
      </div>
    </div>
  </div>
</template>
