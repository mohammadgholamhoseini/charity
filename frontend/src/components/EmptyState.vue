<script setup>
import { Inbox } from '@lucide/vue'

defineProps({
  title: { type: String, default: 'موردی یافت نشد' },
  description: { type: String, default: '' },
  icon: { type: [Object, Function], default: Inbox }
})
</script>

<template>
  <div class="card px-6 py-14 text-center">
    <span class="mx-auto mb-4 grid h-14 w-14 place-items-center rounded-2xl bg-brand-50 text-brand-600 dark:bg-brand-950 dark:text-brand-300">
      <component :is="icon" :size="25" />
    </span>
    <h3 class="font-bold text-slate-800 dark:text-white">{{ title }}</h3>
    <p v-if="description" class="mx-auto mt-2 max-w-md text-sm leading-7 text-slate-500 dark:text-slate-400">{{ description }}</p>
    <div v-if="$slots.default" class="mt-5"><slot /></div>
  </div>
</template>
