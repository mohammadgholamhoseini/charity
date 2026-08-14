<script setup lang="ts">
import { ep } from '~/api/endpoints'
import type { CategoryResponse, CityRef, Page, RequestSummary } from '~/types/api'

/**
 * An indexable landing page for one category.
 *
 * The filtered listing at /requests?category=… is noindex, because a four-dimension
 * filter sidebar generates thousands of near-duplicate URLs. This page is the
 * indexable version of the single highest-value facet: a real path, a unique H1 and
 * its own intro copy, which is what actually earns a ranking for «کمک درمان» and the
 * like.
 */
const route = useRoute()
const { $api } = useNuxtApp()
const { num } = useFormat()

const slug = useRouteSlug()

const { data: category, error: categoryError } = await useFetch<CategoryResponse>(
  () => ep.category(slug.value),
  { $fetch: $api },
)

if (categoryError.value || !category.value) {
  throw createError({ statusCode: 404, statusMessage: 'دسته‌بندی یافت نشد', fatal: true })
}

const { filters, page, apiQuery, hasActiveFilters, toggle, isSelected, clear }
  = useListQuery({ urgency: { multi: true }, city: { multi: true }, sort: {} })

const query = computed(() => ({ ...apiQuery.value, category: slug.value }))

const { data, pending, error, refresh } = await useFetch<Page<RequestSummary>>(ep.requests, {
  $fetch: $api,
  query,
})

const { data: cities } = await useAsyncData('category:cities', () =>
  $api<CityRef[]>(ep.cities).catch(() => []))

useSeo(() => ({
  title: `درخواست‌های ${category.value!.name} — یاری‌جو`,
  description: category.value!.description
    ?? `فهرست درخواست‌های کمک در دسته ${category.value!.name}، ثبت‌شده توسط مراکز خیریه معتبر.`,
  path: `/requests/category/${category.value!.slug}`,
  noindex: hasActiveFilters.value,
}))

watchEffect(() => {
  if (data.value?.content?.length) {
    useJsonLd().itemList(data.value.content)
  }
})
</script>

<template>
  <div v-if="category">
    <div class="page-shell pt-8 pb-6 flex flex-col gap-5">
      <LayoutBreadcrumbs
        :crumbs="[
          { name: 'درخواست‌ها', path: '/requests' },
          { name: category.name, path: `/requests/category/${category.slug}` },
        ]"
      />
      <div class="flex items-center gap-4">
        <span
          class="w-[38px] h-[38px] rounded-[9px] shrink-0"
          :style="{ backgroundColor: category.labelBg }"
          aria-hidden="true"
        />
        <h1 class="text-[26px] lg:text-[34px] font-extrabold">درخواست‌های {{ category.name }}</h1>
      </div>
      <p v-if="category.description" class="text-[16px] leading-[2.1] text-body max-w-[640px]">
        {{ category.description }}
      </p>
      <p class="text-[14px] text-muted">{{ num(data?.totalElements ?? 0) }} درخواست فعال در این دسته</p>
    </div>

    <div class="bg-surface-2 py-8">
      <div class="page-shell grid gap-7 lg:grid-cols-[260px_1fr] items-start">
        <RequestFilterSidebar
          :categories="[]"
          :cities="(cities ?? []).slice(0, 40)"
          :is-selected="isSelected"
          hide-category
          @toggle="toggle"
          @clear="clear()"
        />

        <div class="flex flex-col gap-6 min-w-0">
          <div v-if="pending" class="grid gap-6 md:grid-cols-2">
            <UiSkeleton v-for="n in 4" :key="n" variant="card" />
          </div>

          <UiErrorState v-else-if="error" @retry="refresh()" />

          <UiEmptyState
            v-else-if="!data?.content.length"
            title="در این دسته درخواست فعالی نیست"
            description="می‌توانید دسته‌های دیگر را ببینید یا همه درخواست‌ها را مرور کنید."
          >
            <NuxtLink to="/requests" class="btn btn-secondary">همه درخواست‌ها</NuxtLink>
          </UiEmptyState>

          <div v-else class="grid gap-6 md:grid-cols-2 items-start">
            <RequestCard v-for="request in data.content" :key="request.id" :request="request" />
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
  </div>
</template>
