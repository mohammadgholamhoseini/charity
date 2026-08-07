<script setup>
import { ref, onMounted } from 'vue'
import { Mail, Phone, ShieldCheck } from '@lucide/vue'
import BrandLogo from './BrandLogo.vue'
import api from '../api/client'

const footer = ref([])

async function load() {
  try {
    const res = await api.get('/public/notices')
    footer.value = res.data.footer || []
  } catch (e) {
    footer.value = []
  }
}

onMounted(load)
</script>

<template>
  <footer class="relative mt-16 overflow-hidden bg-gradient-to-br from-brand-800 via-brand-900 to-brand-950 text-brand-50">
    <!-- decorative glow -->
    <div class="absolute -top-24 -left-24 w-72 h-72 bg-brand-500/20 rounded-full blur-3xl"></div>
    <div class="absolute -bottom-32 -right-20 w-80 h-80 bg-accent-500/10 rounded-full blur-3xl"></div>

    <div class="relative max-w-6xl mx-auto px-4 py-12">
      <div class="grid md:grid-cols-3 gap-8 mb-10">
        <!-- Brand -->
        <div>
          <div class="mb-4">
            <BrandLogo full dark :mark="38" tagline="سامانهٔ کمک‌رسانی" />
          </div>
          <p class="text-brand-100/70 text-sm leading-7 max-w-xs">
            یاری جو، پلی است میان دست‌های یاری‌رسان و نیازمندان؛ با شفافیت و اعتماد.
          </p>
        </div>

        <!-- Notices -->
        <div class="md:col-span-2 space-y-3">
          <div v-for="n in footer" :key="n.id" class="rounded-2xl bg-white/5 border border-white/10 p-4 backdrop-blur">
            <div class="font-bold text-white mb-1 flex items-center gap-2">
              <ShieldCheck :size="16" class="text-brand-300" />
              {{ n.title }}
            </div>
            <p class="text-sm text-brand-100/70 leading-7">{{ n.content }}</p>
          </div>
        </div>
      </div>

      <div class="flex flex-col sm:flex-row items-center justify-between gap-4 pt-6 border-t border-white/10 text-sm text-brand-100/60">
        <span>© ۱۴۰۳ یاری جو — صرفاً اطلاع‌رسانی</span>
        <span class="flex items-center gap-4">
          <span class="flex items-center gap-1.5"><Mail :size="14" /> پشتیبانی@yariju.com</span>
          <span class="flex items-center gap-1.5"><Phone :size="14" /> ۰۲۱-۱۲۳۴۵۶۷۸</span>
        </span>
      </div>
      <p class="mt-4 text-xs text-brand-100/50 leading-6">
        مسئولیت تراکنش‌های مالی و پرداخت‌ها بر عهده کاربر و مرکز خیریه است؛ یاری جو واسطه پرداخت نیست.
      </p>
    </div>
  </footer>
</template>
