<script setup lang="ts">
import type { NuxtError } from '#app'

const props = defineProps<{ error: NuxtError }>()

/** Distinct copy per status: "removed" and "never existed" are different situations. */
const content = computed(() => {
  switch (props.error.statusCode) {
    case 410:
      return {
        heading: 'این درخواست حذف شده است',
        body: 'این درخواست توسط مرکز یا ادمین از سایت برداشته شده است. می‌توانید درخواست‌های فعال دیگر را ببینید.',
      }
    case 404:
      return {
        heading: 'صفحه‌ای که دنبالش بودید پیدا نشد',
        body: 'ممکن است نشانی را اشتباه وارد کرده باشید یا این مورد دیگر منتشر نشده باشد.',
      }
    default:
      return {
        heading: 'خطایی رخ داد',
        body: 'مشکلی در نمایش این صفحه پیش آمد. کمی بعد دوباره تلاش کنید.',
      }
  }
})
</script>

<template>
  <div class="min-h-screen flex flex-col">
    <LayoutSiteHeader />
    <main class="flex-1 page-shell py-20 flex flex-col items-center text-center gap-5">
      <BrandMark :size="88" />
      <p class="eyebrow ltr">{{ error.statusCode }}</p>
      <h1 class="text-[28px] lg:text-[34px] font-extrabold">{{ content.heading }}</h1>
      <p class="text-[15px] leading-8 text-body max-w-lg">{{ content.body }}</p>
      <div class="flex flex-wrap justify-center gap-3 mt-3">
        <NuxtLink to="/requests" class="btn btn-primary" @click="clearError">مرور درخواست‌ها</NuxtLink>
        <NuxtLink to="/" class="btn btn-secondary" @click="clearError">بازگشت به صفحه اصلی</NuxtLink>
      </div>
    </main>
    <LayoutSiteFooter :notice="null" />
  </div>
</template>
