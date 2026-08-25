<script setup lang="ts">
import { ep } from '~/api/endpoints'
import type { CategoryResponse, Page, RequestSummary } from '~/types/api'

const { $api } = useNuxtApp()
const { num } = useFormat()

const { data: latest, pending: latestPending, error: latestError, refresh: refreshLatest }
  = await useAsyncData('home:latest', () =>
    $api<Page<RequestSummary>>(ep.requests, { query: { size: 6, sort: 'urgent' } }))

const { data: categories } = await useAsyncData('home:categories', () =>
  $api<CategoryResponse[]>(ep.categories).catch(() => []))

const { data: centersPage } = await useAsyncData('home:centers', () =>
  $api<Page<unknown>>(ep.centers, { query: { size: 1 } }).catch(() => null))

const activeRequests = computed(() => latest.value?.totalElements ?? 0)
const centerCount = computed(() => centersPage.value?.totalElements ?? 0)
const categoryCount = computed(() => categories.value?.length ?? 0)
const completedCount = computed(() =>
  (categories.value ?? []).reduce((sum, c) => sum + c.activeRequestCount, 0))

useSeo({
  title: 'یاری‌جو — شبکه مراکز خیریه',
  description:
    'درخواست‌های واقعی کمک از مراکز خیریه معتبر را ببینید و مستقیم با مرکز ثبت‌کننده تماس بگیرید. '
    + 'یاری‌جو سامانه اطلاع‌رسانی است و پرداخت آنلاین ندارد.',
})
</script>

<template>
  <div>
    <!-- hero -->
    <section class="page-shell grid gap-12 py-14 lg:py-16 lg:grid-cols-[1.05fr_.95fr] lg:items-center">
      <div class="flex flex-col gap-6">
        <span class="chip chip-stat self-start">
          اکنون {{ num(activeRequests) }} درخواست فعال از {{ num(centerCount) }} مرکز
        </span>

        <!-- The LCP element on this page. Nothing above it blocks rendering except
             the preloaded, self-hosted font. -->
        <h1 class="text-[32px] lg:text-[56px] font-extrabold leading-[1.35]">
          به مهر، میهن خویش را کنیم آباد.
        </h1>

        <!-- The h1 is a slogan, so it carries no keyword and does not say what the site is.
             Both jobs moved here and into useSeo above -- keep "مراکز خیریه" and the
             contact-the-centre instruction in this paragraph if it is ever reworded. -->
        <p class="text-[17px] lg:text-[18px] leading-[2.1] text-body max-w-[520px]">
          یاری‌جو برای همین است: مراکز خیریه نیازهایی را که از نزدیک دیده‌اند اینجا می‌نویسند
          تا یاری شما زودتر به آن برسد. بخوانید و مستقیم با خودِ مرکز تماس بگیرید؛
          میهن را همین دست‌ها آباد می‌کند.
        </p>

        <div class="flex flex-col sm:flex-row gap-3">
          <NuxtLink to="/requests" class="btn btn-primary">مرور درخواست‌ها</NuxtLink>
          <NuxtLink to="/centers" class="btn btn-secondary">مراکز خیریه</NuxtLink>
        </div>

        <!-- These chips used to live in a card floating over the artwork. The design
             wants the artwork frameless, and a translucent card over a multiplied
             image reads as a smudge, so they sit under the buttons instead. -->
        <div v-if="categories?.length" class="flex flex-wrap items-center gap-2">
          <span class="eyebrow">دسته‌های فعال</span>
          <UiChip
            v-for="category in categories.slice(0, 3)"
            :key="category.id"
            :label="category.name"
            :color="{ bg: category.labelBg, text: category.labelText }"
          />
        </div>
      </div>

      <!-- Frameless by design: no card, border or shadow -- see .hero-art in main.css,
           which also explains why the blend mode is there. -->
      <div class="flex items-center justify-center">
        <img
          src="/hero-hands.webp"
          width="900"
          height="533"
          alt="دست‌های برافراشته، نماد مشارکت داوطلبانه در کار خیر"
          class="hero-art"
        >
      </div>
    </section>

    <!-- latest requests -->
    <section class="bg-surface-2 py-14">
      <div class="page-shell">
        <div class="flex items-end justify-between gap-4 mb-8">
          <div class="flex flex-col gap-2">
            <span class="eyebrow">تازه‌ترین‌ها</span>
            <h2 class="text-[26px] lg:text-[32px] font-extrabold">آخرین درخواست‌ها</h2>
          </div>
          <NuxtLink to="/requests" class="text-[15px] font-semibold text-accent hover:text-accent-600">
            همه درخواست‌ها ←
          </NuxtLink>
        </div>

        <div v-if="latestPending" class="grid gap-6 md:grid-cols-2 lg:grid-cols-3">
          <UiSkeleton v-for="n in 3" :key="n" variant="card" />
        </div>

        <UiErrorState v-else-if="latestError" @retry="refreshLatest()" />

        <UiEmptyState
          v-else-if="!latest?.content.length"
          title="هنوز درخواستی منتشر نشده است"
          description="به‌زودی درخواست‌های مراکز خیریه در این بخش نمایش داده می‌شود."
        />

        <div v-else class="grid gap-6 md:grid-cols-2 lg:grid-cols-3 items-start">
          <RequestCard
            v-for="request in latest.content"
            :key="request.id"
            :request="request"
          />
        </div>
      </div>
    </section>

    <!-- categories -->
    <section v-if="categories?.length" class="page-shell py-14">
      <div class="flex flex-col gap-2 mb-8">
        <span class="eyebrow">دسته‌بندی‌ها</span>
        <h2 class="text-[26px] lg:text-[32px] font-extrabold">بر اساس نوع نیاز جست‌وجو کنید</h2>
      </div>

      <div class="grid gap-5 grid-cols-2 lg:grid-cols-4">
        <NuxtLink
          v-for="category in categories"
          :key="category.id"
          :to="`/requests/category/${encodeURIComponent(category.slug)}`"
          class="card-flat p-5 flex flex-col gap-3 hover:border-line transition-colors"
        >
          <span
            class="w-[38px] h-[38px] rounded-[9px]"
            :style="{ backgroundColor: category.labelBg }"
            aria-hidden="true"
          />
          <span class="text-[18px] font-bold">{{ category.name }}</span>
          <span class="text-[13px] text-muted">
            {{ num(category.activeRequestCount) }} درخواست فعال
          </span>
        </NuxtLink>
      </div>
    </section>

    <!-- centres panel -->
    <section class="dark-panel py-16">
      <div class="page-shell grid gap-10 lg:grid-cols-2 lg:items-center">
        <div class="flex flex-col gap-5">
          <h2 class="text-[28px] lg:text-[38px] font-extrabold text-onink leading-[1.4]">
            مرکز خیریه هستید؟
          </h2>
          <p class="text-[16px] leading-[2] text-onink-2 max-w-[520px]">
            حساب مراکز فقط توسط ادمین ایجاد می‌شود. پس از دریافت حساب، می‌توانید درخواست‌های مرکز خود را
            در دسته‌بندی‌های مجاز ثبت کنید و وضعیت آن‌ها را دنبال کنید.
          </p>
          <div class="flex flex-col sm:flex-row gap-3">
            <NuxtLink to="/login" class="btn btn-highlight">ورود به پنل مراکز</NuxtLink>
            <NuxtLink to="/contact" class="btn btn-outline-light">درخواست ایجاد حساب</NuxtLink>
          </div>
        </div>

        <div class="grid grid-cols-2 gap-4">
          <div
            v-for="stat in [
              { value: centerCount, label: 'مرکز خیریه فعال' },
              { value: activeRequests, label: 'درخواست فعال' },
              { value: completedCount, label: 'درخواست پاسخ‌داده‌شده' },
              { value: categoryCount, label: 'دسته‌بندی فعال' },
            ]"
            :key="stat.label"
            class="rounded-[16px] p-5 flex flex-col gap-1.5"
            style="background: var(--color-ink-2)"
          >
            <span class="text-[28px] font-extrabold text-accent-2 leading-none">{{ num(stat.value) }}</span>
            <span class="text-[13px] text-onink-3">{{ stat.label }}</span>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>
