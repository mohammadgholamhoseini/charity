<script setup lang="ts">
import { ep } from '~/api/endpoints'
import type { Page, RequestDetail, RequestSummary } from '~/types/api'

const route = useRoute()
const { $api } = useNuxtApp()
const { toman, date, shortDate, isoDate, num } = useFormat()

const slug = useRouteSlug()

// Fixed string key plus an explicit watch on the slug: useAsyncData's first argument
// must be a string, and passing a function there silently makes that function the
// handler instead. The watch is what refetches when navigating between two requests.
const { data: request, error } = await useAsyncData(
  'request-detail',
  () => $api<RequestDetail>(ep.request(slug.value)),
  { watch: [slug] },
)

if (error.value || !request.value) {
  const status = (error.value as { statusCode?: number })?.statusCode
  // `fatal` makes Nitro emit a real status code rather than a 200 carrying an error
  // page. A soft 404 is the single most damaging SEO mistake available on a detail
  // page — the URL stays indexed and keeps ranking for content that is gone.
  throw createError({
    statusCode: status === 410 ? 410 : 404,
    statusMessage: status === 410 ? 'این درخواست حذف شده است' : 'درخواست یافت نشد',
    fatal: true,
  })
}

// The backend answers with the canonical slug; if the title has since been edited the
// visitor arrived on a stale URL and gets redirected rather than shown a duplicate.
if (request.value.slug !== slug.value) {
  await navigateTo(`/requests/${encodeURIComponent(request.value.slug)}`, {
    redirectCode: 301,
    replace: true,
  })
}

const { data: similar } = await useAsyncData(
  'request-similar',
  () => $api<Page<RequestSummary>>(ep.requests, {
    query: { category: request.value?.category?.slug, size: 4 },
  }).catch(() => null),
  { watch: [slug] },
)

const similarRequests = computed(() =>
  (similar.value?.content ?? []).filter(item => item.id !== request.value?.id).slice(0, 3))

// Only the statuses a visitor can actually land on. A public URL never resolves to anything
// else -- the others answer 404 -- so listing them here only invited questions.
const statusExplainer = [
  { value: 'PUBLISHED', label: 'منتشرشده' },
  { value: 'COMPLETED', label: 'تکمیل‌شده' },
]

useSeo(() => ({
  title: request.value!.metaTitle ?? request.value!.title,
  description: request.value!.metaDescription ?? request.value!.summary ?? '',
  path: `/requests/${request.value!.slug}`,
  type: 'article',
}))

useJsonLd().requestDetail(request.value!)
</script>

<template>
  <div v-if="request" class="page-shell py-8">
    <div class="grid gap-8 lg:grid-cols-[1fr_360px] items-start">
      <!-- main column -->
      <article class="flex flex-col gap-6 min-w-0">
        <LayoutBreadcrumbs
          :crumbs="[
            { name: 'درخواست‌ها', path: '/requests' },
            ...(request.category
              ? [{ name: request.category.name, path: `/requests/category/${request.category.slug}` }]
              : []),
            { name: request.title, path: `/requests/${request.slug}` },
          ]"
        />

        <div class="flex flex-wrap items-center gap-2">
          <UiChip
            v-if="request.category"
            :label="request.category.name"
            :color="{ bg: request.category.labelBg, text: request.category.labelText }"
          />
          <UiChip :label="request.statusLabel" :status="request.status" />
          <UiChip :label="`فوریت: ${request.urgencyLabel}`" :urgency="request.urgency" />
          <span class="text-[13px] text-muted ltr">{{ request.code }}</span>
        </div>

        <h1 class="text-[28px] lg:text-[40px] font-extrabold leading-[1.4]">{{ request.title }}</h1>

        <p v-if="request.description" class="text-[16px] lg:text-[17px] leading-[2.2] text-body">
          {{ request.description }}
        </p>

        <p class="text-[14px] leading-8 text-muted">
          این درخواست تنها جهت اطلاع‌رسانی منتشر شده است؛ برای همکاری مستقیماً با مرکز ثبت‌کننده تماس بگیرید.
        </p>

        <!-- declared need -->
        <section class="card p-6 flex flex-col gap-5">
          <div class="flex items-center justify-between gap-4">
            <h2 class="text-[19px] font-bold">نیاز اعلام‌شده</h2>
            <UiChip :label="`سطح فوریت: ${request.urgencyLabel}`" :urgency="request.urgency" />
          </div>

          <dl class="flex flex-col gap-4">
            <div v-if="request.category" class="grid grid-cols-[130px_1fr] gap-3 text-[14px]">
              <dt class="text-muted">نوع نیاز</dt>
              <dd class="text-body-2">{{ request.category.name }}</dd>
            </div>
            <div class="grid grid-cols-[130px_1fr] gap-3 text-[14px]">
              <dt class="text-muted">مبلغ مورد نیاز</dt>
              <dd class="text-body-2 font-bold">{{ toman(request.amountNeeded) }}</dd>
            </div>
            <div v-if="request.center?.cityName" class="grid grid-cols-[130px_1fr] gap-3 text-[14px]">
              <dt class="text-muted">محل انجام</dt>
              <dd class="text-body-2">
                {{ request.center.cityName }}<template v-if="request.center.provinceName">، {{ request.center.provinceName }}</template>
              </dd>
            </div>
            <div class="grid grid-cols-[130px_1fr] gap-3 text-[14px]">
              <dt class="text-muted">وضعیت درخواست</dt>
              <dd class="text-body-2">
                {{ request.statusLabel }} —
                <time :datetime="isoDate(request.publishedAt ?? request.createdAt)">
                  {{ shortDate(request.publishedAt ?? request.createdAt) }}
                </time>
              </dd>
            </div>
          </dl>

          <p class="text-[13px] leading-7 text-muted border-t border-surface-3 pt-4">
            یاری‌جو مشخصات مددجو را جداگانه ثبت نمی‌کند؛ هر چه در متن این درخواست آمده را مرکز
            ثبت‌کننده نوشته و مسئولیتش با همان مرکز است.
            مراکز فهرست‌شده در یاری‌جو توسط ادمین ثبت و تأیید شده‌اند.
          </p>
        </section>

        <!-- documents -->
        <section v-if="request.documents?.length" class="card p-6 flex flex-col gap-5">
          <h2 class="text-[19px] font-bold">مدارک</h2>
          <DocumentList :documents="request.documents" />
          <p class="text-[13px] leading-7 text-muted border-t border-surface-3 pt-4">
            این مدارک را مرکز ثبت‌کننده بارگذاری کرده و مسئولیت صحت آن‌ها با همان مرکز است.
            فایل‌ها در زبانه تازه باز می‌شوند.
          </p>
        </section>

        <!-- status lifecycle -->
        <section class="card-flat p-6 flex flex-col gap-4">
          <h2 class="text-[19px] font-bold">وضعیت و دسته‌بندی</h2>
          <p class="text-[14px] leading-8 text-body">
            وضعیت این درخواست «{{ request.statusLabel }}» است؛ پس از دریافت کامل کمک، مرکز آن را به
            «تکمیل‌شده» تغییر می‌دهد و از فهرست فعال خارج می‌شود.
          </p>
          <div class="flex flex-wrap gap-2">
            <UiChip
              v-for="item in statusExplainer"
              :key="item.value"
              :label="item.label"
              :status="item.value"
            />
          </div>
        </section>
      </article>

      <!-- sidebar -->
      <aside class="flex flex-col gap-5 lg:sticky lg:top-6">
        <section v-if="request.center" class="dark-panel rounded-[20px] p-6 flex flex-col gap-5">
          <div class="flex items-center gap-3">
            <BrandMark :size="38" on-dark />
            <div class="flex flex-col">
              <span class="eyebrow" style="color: var(--color-onink-3)">مرکز ثبت‌کننده</span>
              <span class="text-[17px] font-bold text-onink">{{ request.center.name }}</span>
            </div>
          </div>

          <dl class="flex flex-col gap-3 text-[14px] border-t pt-4" style="border-color: var(--color-ink-3)">
            <div v-if="request.center.cityName" class="flex justify-between gap-3">
              <dt class="text-onink-3">شهر</dt>
              <dd class="text-onink-2">{{ request.center.cityName }}</dd>
            </div>
            <div v-if="request.center.contactPhone" class="flex justify-between gap-3">
              <dt class="text-onink-3">شماره تماس</dt>
              <dd class="text-onink-2 ltr">{{ request.center.contactPhone }}</dd>
            </div>
            <div v-if="request.center.responseHours" class="flex justify-between gap-3">
              <dt class="text-onink-3">ساعات پاسخ‌گویی</dt>
              <dd class="text-onink-2">{{ request.center.responseHours }}</dd>
            </div>
            <div class="flex justify-between gap-3">
              <dt class="text-onink-3">درخواست فعال</dt>
              <dd class="text-onink-2">{{ num(request.center.activeRequestCount) }} مورد</dd>
            </div>
          </dl>

          <a
            v-if="request.center.contactPhone"
            :href="`tel:${request.center.contactPhone}`"
            class="btn btn-highlight w-full"
          >تماس با مرکز</a>

          <NuxtLink
            :to="`/centers/${encodeURIComponent(request.center.slug)}`"
            class="btn btn-outline-light w-full"
          >همه درخواست‌های این مرکز</NuxtLink>
        </section>

        <section v-if="similarRequests.length" class="card-flat p-6 flex flex-col gap-4">
          <h2 class="text-[17px] font-bold">درخواست‌های مشابه</h2>
          <ul class="flex flex-col divide-y" style="border-color: var(--color-surface-3)">
            <li v-for="item in similarRequests" :key="item.id" class="py-3 first:pt-0 last:pb-0">
              <NuxtLink
                :to="`/requests/${encodeURIComponent(item.slug)}`"
                class="text-[14px] leading-7 hover:text-accent"
              >
                {{ item.title }}
              </NuxtLink>
              <p class="text-[12px] text-muted mt-1">
                {{ item.center?.name }}<template v-if="item.center?.cityName"> — {{ item.center.cityName }}</template>
              </p>
            </li>
          </ul>
        </section>

        <p
          class="text-[13px] leading-7 text-muted p-5 rounded-[16px]"
          style="border: 1px dashed var(--color-line)"
        >
          در حال حاضر پرداخت آنلاین در یاری‌جو فعال نیست. هرگونه همکاری مستقیماً با مرکز خیریه انجام می‌شود.
        </p>
      </aside>
    </div>
  </div>
</template>
