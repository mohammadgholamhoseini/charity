<script setup lang="ts">
const config = useRuntimeConfig()

useSeo({
  title: 'تماس با یاری‌جو',
  description: 'راه‌های تماس با یاری‌جو برای مراکز خیریه، درخواست ایجاد حساب و گزارش مشکل.',
})
</script>

<template>
  <LayoutProsePage
    title="تماس با ما"
    crumb-name="تماس"
    crumb-path="/contact"
    lead="برای درخواست ایجاد حساب مرکز، گزارش یک مورد یا هر پرسش دیگری با ما در تماس باشید."
  >
    <div class="grid gap-5 sm:grid-cols-2">
      <div class="card-flat p-6 flex flex-col gap-2">
        <span class="eyebrow">تلفن پشتیبانی</span>
        <a
          :href="`tel:${config.public.supportPhone}`"
          class="text-[20px] font-bold ltr text-brick-500 hover:text-brick-600"
        >{{ config.public.supportPhone }}</a>
        <span class="help">شنبه تا چهارشنبه، ۹ تا ۱۷</span>
      </div>

      <div class="card-flat p-6 flex flex-col gap-2">
        <span class="eyebrow">پست الکترونیک</span>
        <a
          href="mailto:info@yariju.ir"
          class="text-[20px] font-bold ltr text-brick-500 hover:text-brick-600"
        >info@yariju.ir</a>
        <span class="help">پاسخ‌گویی حداکثر تا دو روز کاری</span>
      </div>
    </div>

    <section class="flex flex-col gap-3">
      <h2 class="text-[20px] font-bold text-ink-900">مرکز خیریه هستید؟</h2>
      <p>
        برای ایجاد حساب، نام مرکز، شهر، شماره تماس و دسته‌بندی‌هایی که در آن‌ها فعالیت می‌کنید را
        برای ما بفرستید. پس از بررسی، نام کاربری و رمز عبور موقت برای شما ساخته می‌شود و از طریق
        صفحه ورود می‌توانید وارد پنل شوید.
      </p>
    </section>

    <section class="flex flex-col gap-3">
      <h2 class="text-[20px] font-bold text-ink-900">گزارش مورد مشکوک</h2>
      <p>
        یاری‌جو هرگز از شما نمی‌خواهد وجهی به حساب این سایت واریز کنید. اگر با درخواستی مواجه شدید که
        به نظرتان جعلی یا نادرست است، نشانی آن صفحه و کد درخواست را برای ما بفرستید تا بررسی شود.
      </p>
    </section>
  </LayoutProsePage>
</template>
