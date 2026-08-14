<script setup lang="ts">
import { ep } from '~/api/endpoints'
import type { CenterPublicProfile, Page, RequestSummary } from '~/types/api'

const route = useRoute()
const { $api } = useNuxtApp()
const { num } = useFormat()

const slug = useRouteSlug()

const { data: center, error } = await useFetch<CenterPublicProfile>(
  () => ep.center(slug.value),
  { $fetch: $api },
)

if (error.value || !center.value) {
  throw createError({ statusCode: 404, statusMessage: 'مرکز خیریه یافت نشد', fatal: true })
}

const { page, apiQuery } = useListQuery({})
const query = computed(() => ({ ...apiQuery.value, center: slug.value }))

const { data: requests, pending } = await useFetch<Page<RequestSummary>>(ep.requests, {
  $fetch: $api,
  query,
})

useSeo(() => ({
  title: `${center.value!.name} — مرکز خیریه${center.value!.city ? ` در ${center.value!.city.name}` : ''} — یاری‌جو`,
  description: center.value!.description
    ?? `درخواست‌های کمک ثبت‌شده توسط ${center.value!.name}. برای همکاری مستقیماً با مرکز تماس بگیرید.`,
  path: `/centers/${center.value!.slug}`,
}))

useJsonLd().centerProfile(center.value!)
</script>

<template>
  <div v-if="center" class="page-shell py-8 flex flex-col gap-8">
    <LayoutBreadcrumbs
      :crumbs="[
        { name: 'مراکز خیریه', path: '/centers' },
        { name: center.name, path: `/centers/${center.slug}` },
      ]"
    />

    <section class="dark-panel rounded-[20px] p-7 lg:p-9 flex flex-wrap items-center justify-between gap-6">
      <div class="flex items-center gap-4">
        <BrandMark :size="56" on-dark />
        <div class="flex flex-col gap-1">
          <h1 class="text-[24px] lg:text-[30px] font-extrabold text-onink">{{ center.name }}</h1>
          <p v-if="center.city" class="text-[14px] text-onink-3">
            {{ center.city.name }}<template v-if="center.city.provinceName">، {{ center.city.provinceName }}</template>
          </p>
        </div>
      </div>
      <a v-if="center.contactPhone" :href="`tel:${center.contactPhone}`" class="btn btn-highlight">
        تماس با مرکز
      </a>
    </section>

    <div class="grid gap-8 lg:grid-cols-[1fr_320px] items-start">
      <div class="flex flex-col gap-6 min-w-0">
        <p v-if="center.description" class="text-[16px] leading-[2.1] text-body">
          {{ center.description }}
        </p>

        <h2 class="text-[22px] font-extrabold">
          درخواست‌های این مرکز
          <span class="text-[14px] font-normal text-muted">({{ num(requests?.totalElements ?? 0) }})</span>
        </h2>

        <div v-if="pending" class="grid gap-6 md:grid-cols-2">
          <UiSkeleton v-for="n in 2" :key="n" variant="card" />
        </div>

        <UiEmptyState
          v-else-if="!requests?.content.length"
          title="این مرکز در حال حاضر درخواست فعالی ندارد"
        />

        <div v-else class="grid gap-6 md:grid-cols-2 items-start">
          <RequestCard v-for="request in requests.content" :key="request.id" :request="request" />
        </div>

        <UiPagination
          v-if="requests"
          :page="page"
          :total-pages="requests.totalPages"
          :total-elements="requests.totalElements"
        />
      </div>

      <aside class="card-flat p-6 flex flex-col gap-4 lg:sticky lg:top-6">
        <h2 class="text-[17px] font-bold">اطلاعات تماس</h2>
        <dl class="flex flex-col gap-3 text-[14px]">
          <div v-if="center.contactPhone" class="flex justify-between gap-3">
            <dt class="text-muted">تلفن</dt>
            <dd class="ltr">{{ center.contactPhone }}</dd>
          </div>
          <div v-if="center.responseHours" class="flex justify-between gap-3">
            <dt class="text-muted">ساعات پاسخ‌گویی</dt>
            <dd class="text-body-2">{{ center.responseHours }}</dd>
          </div>
          <div v-if="center.address" class="flex flex-col gap-1">
            <dt class="text-muted">نشانی</dt>
            <dd class="text-body-2 leading-7">{{ center.address }}</dd>
          </div>
          <div v-if="center.cardNumber" class="flex justify-between gap-3">
            <dt class="text-muted">شماره کارت</dt>
            <dd class="ltr">{{ center.cardNumber }}</dd>
          </div>
          <div v-if="center.sheba" class="flex justify-between gap-3">
            <dt class="text-muted">شبا</dt>
            <dd class="ltr text-[12px]">{{ center.sheba }}</dd>
          </div>
        </dl>

        <div v-if="center.categories.length" class="border-t border-surface-3 pt-4 flex flex-col gap-3">
          <span class="label mb-0">دسته‌های فعال</span>
          <div class="flex flex-wrap gap-1.5">
            <UiChip
              v-for="category in center.categories"
              :key="category.id"
              :label="category.name"
              :color="{ bg: category.labelBg, text: category.labelText }"
            />
          </div>
        </div>

        <p
          class="text-[13px] leading-7 text-muted p-4 rounded-[14px] mt-2"
          style="border: 1px dashed var(--color-line)"
        >
          پرداخت آنلاین در یاری‌جو فعال نیست. هرگونه همکاری مالی مستقیماً با خود مرکز انجام می‌شود.
        </p>
      </aside>
    </div>
  </div>
</template>
