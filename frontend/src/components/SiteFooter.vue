<script setup>
import { ref, onMounted } from 'vue'
import { Mail, Phone, ShieldCheck, ArrowLeft, Building2, HeartHandshake } from '@lucide/vue'
import { RouterLink } from 'vue-router'
import BrandLogo from './BrandLogo.vue'
import api from '../api/client'

const footer = ref([])

async function load() {
  try {
    const response = await api.get('/public/notices')
    footer.value = response.data.footer || []
  } catch {
    footer.value = []
  }
}

onMounted(load)
</script>

<template>
  <footer class="relative mt-20 overflow-hidden bg-[#092733] text-white">
    <div class="girih absolute inset-0 text-white/[0.035]"></div>
    <div class="absolute -left-32 -top-32 h-80 w-80 rounded-full bg-brand-400/15 blur-3xl"></div>
    <div class="absolute -bottom-40 -right-24 h-96 w-96 rounded-full bg-accent-500/10 blur-3xl"></div>

    <div class="relative mx-auto max-w-6xl px-4 py-14 sm:px-6">
      <div class="grid gap-10 lg:grid-cols-[1.2fr_.8fr_.9fr]">
        <div>
          <BrandLogo full inverse :mark="44" tagline="پیوند امن برای یاری آگاهانه" />
          <p class="mt-5 max-w-md text-sm leading-8 text-slate-300">
            یاری‌جو درخواست‌های واقعی را از مسیر مراکز خیریه به شما نشان می‌دهد تا انتخاب، ارتباط و یاری با آگاهی بیشتری انجام شود.
          </p>
          <div class="mt-5 inline-flex items-center gap-2 rounded-2xl border border-white/10 bg-white/5 px-4 py-3 text-xs text-slate-300">
            <ShieldCheck :size="17" class="text-brand-300" />
            نمایش شفاف مرکز ثبت‌کننده و مستندات هر درخواست
          </div>
        </div>

        <div>
          <h2 class="font-bold">دسترسی سریع</h2>
          <div class="mt-4 space-y-3 text-sm text-slate-300">
            <RouterLink to="/cases" class="flex items-center gap-2 transition hover:text-white"><HeartHandshake :size="16" /> درخواست‌های کمک</RouterLink>
            <RouterLink to="/centers" class="flex items-center gap-2 transition hover:text-white"><Building2 :size="16" /> مراکز خیریه</RouterLink>
            <RouterLink to="/login" class="flex items-center gap-2 transition hover:text-white"><ArrowLeft :size="16" /> ورود به پنل</RouterLink>
          </div>
        </div>

        <div>
          <h2 class="font-bold">اطلاعیه و شرایط</h2>
          <div v-if="footer.length" class="mt-4 space-y-3">
            <div v-for="notice in footer" :key="notice.id" class="rounded-2xl border border-white/10 bg-white/5 p-4">
              <div class="text-sm font-bold">{{ notice.title }}</div>
              <p class="mt-1 text-xs leading-6 text-slate-300">{{ notice.content }}</p>
            </div>
          </div>
          <p v-else class="mt-4 text-sm leading-7 text-slate-400">
            یاری‌جو واسطه پرداخت نیست؛ هماهنگی مالی مستقیماً با مرکز خیریه انجام می‌شود.
          </p>
        </div>
      </div>

      <div class="mt-10 flex flex-col gap-4 border-t border-white/10 pt-6 text-xs text-slate-400 sm:flex-row sm:items-center sm:justify-between">
        <span>© ۱۴۰۵ یاری‌جو — همه حقوق محفوظ است.</span>
        <span class="flex flex-wrap items-center gap-4">
          <span class="flex items-center gap-1.5"><Mail :size="14" /> پشتیبانی@yarijoo.ir</span>
          <span class="flex items-center gap-1.5" dir="ltr"><Phone :size="14" /> 021-12345678</span>
        </span>
      </div>
    </div>
  </footer>
</template>
