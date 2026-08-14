<script setup lang="ts">
/**
 * The one page here with a genuine rich-result opportunity: FAQPage is among the few
 * schema types Google still renders in the SERP, so the questions are written as real
 * questions people ask rather than as filler.
 */
const entries = [
  {
    question: 'آیا می‌توانم از طریق یاری‌جو مبلغی پرداخت کنم؟',
    answer:
      'خیر. یاری‌جو پرداخت آنلاین ندارد و هیچ وجهی دریافت نمی‌کند. پس از دیدن یک درخواست، '
      + 'مستقیماً با مرکز خیریه ثبت‌کننده تماس بگیرید و هماهنگی مالی را با خود مرکز انجام دهید.',
  },
  {
    question: 'از کجا بدانم یک مرکز خیریه معتبر است؟',
    answer:
      'همه مراکزی که در یاری‌جو نمایش داده می‌شوند، پیش از انتشار توسط ادمین ثبت و تأیید شده‌اند. '
      + 'ثبت‌نام عمومی وجود ندارد و هیچ مرکزی نمی‌تواند خودش حساب بسازد.',
  },
  {
    question: 'چطور می‌توانم برای مرکز خیریه‌ام حساب بسازم؟',
    answer:
      'حساب مراکز فقط توسط ادمین ساخته می‌شود. از صفحه تماس درخواست خود را ثبت کنید تا پس از بررسی، '
      + 'نام کاربری و رمز عبور موقت برای شما ایجاد شود.',
  },
  {
    question: 'چرا درخواستی که ثبت کردم هنوز در سایت دیده نمی‌شود؟',
    answer:
      'هر درخواست تازه در وضعیت «در انتظار انتشار» قرار می‌گیرد و پس از بررسی ادمین منتشر می‌شود. '
      + 'تا پیش از آن فقط در پنل خود مرکز قابل مشاهده است.',
  },
  {
    question: 'وضعیت‌های یک درخواست چه معنایی دارند؟',
    answer:
      '«در انتظار انتشار» یعنی هنوز بررسی نشده است. «منتشرشده» یعنی در سایت دیده می‌شود. '
      + '«تکمیل‌شده» یعنی کمک دریافت شده و از فهرست فعال خارج شده است. «رد شده» یعنی ادمین با ذکر دلیل '
      + 'آن را نپذیرفته، و «غیرفعال» یعنی موقتاً از فهرست خارج شده است.',
  },
  {
    question: 'آیا اطلاعات هویتی مددجو منتشر می‌شود؟',
    answer:
      'خیر. جزئیات هویتی مددجو منتشر نمی‌شود و کارت‌های درخواست عمداً بدون تصویر هستند. '
      + 'تنها شرح نیاز، مبلغ مورد نیاز، شهر و نام مرکز ثبت‌کننده نمایش داده می‌شود.',
  },
  {
    question: 'مبلغ نوشته‌شده روی هر درخواست دقیقاً چیست؟',
    answer:
      'مبلغی است که مرکز خیریه برای برطرف کردن آن نیاز اعلام کرده است. یاری‌جو میزان کمک جمع‌آوری‌شده '
      + 'را دنبال نمی‌کند؛ این عدد فقط برای این است که بدانید نیاز در چه ابعادی است.',
  },
  {
    question: 'چطور درخواست‌های شهر خودم را ببینم؟',
    answer:
      'در صفحه درخواست‌ها از فیلتر «شهر» استفاده کنید. می‌توانید هم‌زمان دسته‌بندی و سطح فوریت را هم '
      + 'انتخاب کنید و نشانی صفحه را برای دیگران بفرستید؛ فیلترها در نشانی ذخیره می‌شوند.',
  },
]

useSeo({
  title: 'سؤالات متداول — یاری‌جو',
  description: 'پاسخ پرسش‌های رایج درباره یاری‌جو: نحوه کمک، اعتبار مراکز، وضعیت درخواست‌ها و حریم مددجویان.',
})

useJsonLd().faq(entries)
</script>

<template>
  <LayoutProsePage
    title="سؤالات متداول"
    crumb-name="سؤالات متداول"
    crumb-path="/faq"
  >
    <div class="flex flex-col gap-5">
      <details
        v-for="entry in entries"
        :key="entry.question"
        class="card-flat p-5"
      >
        <summary class="text-[16px] font-bold text-ink cursor-pointer list-none flex items-center justify-between gap-4">
          {{ entry.question }}
          <span class="text-accent text-lg leading-none shrink-0" aria-hidden="true">+</span>
        </summary>
        <p class="mt-3 pt-3 border-t border-surface-3 leading-[2]">{{ entry.answer }}</p>
      </details>
    </div>
  </LayoutProsePage>
</template>

<style scoped>
details[open] summary span { transform: rotate(45deg); }
summary span { transition: transform 0.15s ease; display: inline-block; }
summary::-webkit-details-marker { display: none; }
</style>
