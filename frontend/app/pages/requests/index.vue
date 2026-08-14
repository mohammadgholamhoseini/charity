<script setup lang="ts">
import { ep } from '~/api/endpoints'
import type { CategoryResponse, CityRef, Page, RequestSummary } from '~/types/api'

const { $api } = useNuxtApp()
const { num } = useFormat()
const route = useRoute()

const { filters, page, apiQuery, hasActiveFilters, apply, toggle, isSelected, clear }
  = useListQuery({
    category: { multi: true },
    urgency: { multi: true },
    status: { multi: true },
    city: { multi: true },
    q: {},
    sort: {},
  })

// The reactive `query` makes useFetch refetch on its own whenever a filter changes.
const { data, pending, error, refresh } = await useFetch<Page<RequestSummary>>(ep.requests, {
  $fetch: $api,
  query: apiQuery,
})

const { data: categories } = await useAsyncData('requests:categories', () =>
  $api<CategoryResponse[]>(ep.categories).catch(() => []))

const { data: cities } = await useAsyncData('requests:cities', () =>
  $api<CityRef[]>(ep.cities).catch(() => []))

/** Cities that actually have a centre behind them, so the list stays usable. */
const cityOptions = computed(() => (cities.value ?? []).slice(0, 40))

const searchTerm = ref(String(filters.value.q ?? ''))
watch(() => filters.value.q, value => { searchTerm.value = String(value ?? '') })

const runSearch = useDebounceFn(() => {
  apply({ q: normalizePersianQuery(searchTerm.value) })
}, 350)

/** Removable chips for whatever is currently applied. */
const activeChips = computed(() => {
  const chips: { key: string, value: string, label: string }[] = []
  const labels: Record<string, string> = {
    URGENT: 'فوری', HIGH: 'بالا', MEDIUM: 'متوسط', LOW: 'کم',
    PUBLISHED: 'منتشرشده', COMPLETED: 'تکمیل‌شده',
  }
  for (const slug of filters.value.category as string[]) {
    const found = (categories.value ?? []).find(c => c.slug === slug)
    chips.push({ key: 'category', value: slug, label: found?.name ?? slug })
  }
  for (const value of filters.value.urgency as string[]) {
    chips.push({ key: 'urgency', value, label: labels[value] ?? value })
  }
  for (const value of filters.value.city as string[]) {
    chips.push({ key: 'city', value, label: value })
  }
  for (const value of filters.value.status as string[]) {
    chips.push({ key: 'status', value, label: labels[value] ?? value })
  }
  return chips
})

const total = computed(() => data.value?.totalElements ?? 0)

useSeo(() => ({
  title: page.value > 1
    ? `درخواست‌های کمک — صفحه ${page.value} — یاری‌جو`
    : 'درخواست‌های کمک — یاری‌جو',
  description:
    'فهرست درخواست‌های کمک ثبت‌شده توسط مراکز خیریه معتبر. بر اساس دسته‌بندی، شهر و سطح فوریت فیلتر کنید.',
  // Page 2 self-canonicals rather than pointing at page 1 — canonicalising deep pages
  // onto the first is the classic way to get most of a listing de-indexed.
  path: page.value > 1 ? `${route.path}?page=${page.value}` : route.path,
  // Filter permutations are near-duplicates of each other. They stay crawlable so link
  // equity still reaches the requests, but out of the index. The category and city
  // landing pages exist as the indexable versions of those two facets.
  noindex: hasActiveFilters.value,
}))

watchEffect(() => {
  if (data.value?.content?.length) useJsonLd().itemList(data.value.content)
})
</script>

<template>
  <div>
    <div class="page-shell pt-8 pb-6 flex flex-col gap-5">
      <LayoutBreadcrumbs :crumbs="[{ name: 'درخواست‌ها', path: '/requests' }]" />

      <div class="flex flex-wrap items-end justify-between gap-5">
        <div class="flex flex-col gap-2">
          <h1 class="text-[26px] lg:text-[34px] font-extrabold">درخواست‌های ثبت‌شده</h1>
          <p class="text-[14px] text-muted">
            {{ num(total) }} درخواست بر اساس فیلترهای انتخابی
          </p>
        </div>

        <div class="flex flex-wrap items-center gap-3">
          <label class="sr-only" for="q">جست‌وجو</label>
          <input
            id="q"
            v-model="searchTerm"
            type="search"
            class="field w-[260px]"
            style="border-radius: 999px"
            placeholder="جست‌وجو در عنوان یا مرکز…"
            @input="runSearch()"
          >
          <label class="sr-only" for="sort">مرتب‌سازی</label>
          <select
            id="sort"
            class="field w-[170px]"
            style="border-radius: 999px"
            :value="filters.sort || 'urgent'"
            @change="apply({ sort: ($event.target as HTMLSelectElement).value })"
          >
            <option value="urgent">فوری‌ترین</option>
            <option value="newest">جدیدترین</option>
            <option value="oldest">قدیمی‌ترین</option>
            <option value="amount_desc">بیشترین مبلغ</option>
            <option value="amount_asc">کمترین مبلغ</option>
          </select>
        </div>
      </div>
    </div>

    <div class="bg-surface-2 py-8">
      <div class="page-shell grid gap-7 lg:grid-cols-[260px_1fr] items-start">
        <RequestFilterSidebar
          :categories="categories ?? []"
          :cities="cityOptions"
          :is-selected="isSelected"
          @toggle="toggle"
          @clear="clear()"
        />

        <div class="flex flex-col gap-6 min-w-0">
          <div v-if="activeChips.length" class="flex flex-wrap items-center gap-2">
            <span class="text-[13px] text-muted">فیلتر فعال:</span>
            <UiChip
              v-for="chip in activeChips"
              :key="`${chip.key}:${chip.value}`"
              :label="chip.label"
              closable
              @close="toggle(chip.key, chip.value)"
            />
          </div>

          <div v-if="pending" class="grid gap-6 md:grid-cols-2">
            <UiSkeleton v-for="n in 4" :key="n" variant="card" />
          </div>

          <UiErrorState v-else-if="error" @retry="refresh()" />

          <UiEmptyState
            v-else-if="!data?.content.length"
            title="درخواستی با این فیلترها پیدا نشد"
            description="بازه فیلترها را کمی بازتر کنید یا عبارت جست‌وجو را تغییر دهید."
          >
            <button type="button" class="btn btn-secondary" @click="clear()">پاک کردن فیلترها</button>
          </UiEmptyState>

          <div v-else class="grid gap-6 md:grid-cols-2 items-start">
            <RequestCard
              v-for="request in data.content"
              :key="request.id"
              :request="request"
            />
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
