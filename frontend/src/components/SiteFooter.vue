<script setup>
import { ref, onMounted } from 'vue'
import { HeartHandshake, Mail, Phone, ShieldCheck } from '@lucide/vue'
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
  <footer class="relative mt-16 overflow-hidden bg-gradient-to-br from-brand-800 via-brand-900 to-emerald-950 text-emerald-50">
    <!-- decorative glow -->
    <div class="absolute -top-24 -left-24 w-72 h-72 bg-brand-500/20 rounded-full blur-3xl"></div>
    <div class="absolute -bottom-32 -right-20 w-80 h-80 bg-emerald-400/10 rounded-full blur-3xl"></div>

    <div class="relative max-w-6xl mx-auto px-4 py-12">
      <div class="grid md:grid-cols-3 gap-8 mb-10">
        <!-- Brand -->
        <div>
          <div class="flex items-center gap-2.5 mb-4">
            <span class="grid place-items-center w-10 h-10 rounded-2xl bg-white/10 backdrop-blur text-white">
              <HeartHandshake :size="22" />
            </span>
            <span class="font-extrabold text-lg">همدلی</span>
          </div>
          <p class="text-emerald-100/70 text-sm leading-7 max-w-xs">
            پلتفرمی برای پیوند دادن دست‌های یاری‌رسان به نیازمندان، با شفافیت و اعتماد.
          </p>
        </div>

        <!-- Notices -->
        <div class="md:col-span-2 space-y-3">
          <div v-for="n in footer" :key="n.id" class="rounded-2xl bg-white/5 border border-white/10 p-4 backdrop-blur">
            <div class="font-bold text-white mb-1 flex items-center gap-2">
              <ShieldCheck :size="16" class="text-brand-300" />
              {{ n.title }}
            </div>
            <p class="text-sm text-emerald-100/70 leading-7">{{ n.content }}</p>
          </div>
        </div>
      </div>

      <div class="flex flex-col sm:flex-row items-center justify-between gap-4 pt-6 border-t border-white/10 text-sm text-emerald-100/60">
        <span>© ۱۴۰۳ پلتفرم همدلی — صرفاً اطلاع‌رسانی</span>
        <span class="flex items-center gap-4">
          <span class="flex items-center gap-1.5"><Mail :size="14" /> پشتیبانی@hamdli.ir</span>
          <span class="flex items-center gap-1.5"><Phone :size="14" /> ۰۲۱-۱۲۳۴۵۶۷۸</span>
        </span>
      </div>
      <p class="mt-4 text-xs text-emerald-100/50 leading-6">
        مسئولیت تراکنش‌های مالی و پرداخت‌ها بر عهده کاربر و مرکز خیریه است؛ این پلتفرم واسطه پرداخت نیست.
      </p>
    </div>
  </footer>
</template>
