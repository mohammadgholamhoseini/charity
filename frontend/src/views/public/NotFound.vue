<script setup>
import { Compass, Home } from '@lucide/vue'
</script>
<template>
  <div class="max-w-4xl mx-auto px-4 py-24 text-center">
    <div class="relative inline-block mb-6">
      <div class="text-8xl animate-float">🧭</div>
      <div class="absolute -top-4 -right-4 w-20 h-20 bg-brand-300/30 rounded-full blur-xl"></div>
    </div>
    <h1 class="text-3xl font-extrabold text-slate-800 dark:text-white mb-2">صفحه یافت نشد</h1>
    <p class="text-slate-500 dark:text-slate-400 mb-8 max-w-md mx-auto">موردی که دنبال آن بودید وجود ندارد یا حذف شده است. بیایید شما را به مسیر درست بازگردانیم.</p>
    <RouterLink to="/" class="btn-primary">
      <Home :size="18" />
      بازگشت به خانه
    </RouterLink>
  </div>
</template>
