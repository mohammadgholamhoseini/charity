<script setup lang="ts">
/**
 * Loading placeholder.
 *
 * The design does not cover loading states, so this is invented — deliberately
 * neutral and brand-free, mirroring the real card's shape so nothing shifts when the
 * content arrives.
 */
withDefaults(defineProps<{
  variant?: 'text' | 'card' | 'row'
  lines?: number
}>(), { variant: 'text', lines: 3 })
</script>

<template>
  <div v-if="variant === 'card'" class="card p-6 flex flex-col gap-4" aria-hidden="true">
    <div class="flex gap-2">
      <div class="skel h-6 w-20 rounded-full" />
      <div class="skel h-6 w-24 rounded-full" />
    </div>
    <div class="skel h-6 w-4/5" />
    <div class="skel h-4 w-full" />
    <div class="skel h-4 w-3/5" />
    <div class="skel h-11 w-full rounded-full mt-2" />
  </div>

  <div v-else-if="variant === 'row'" class="flex items-center gap-4 py-4" aria-hidden="true">
    <div class="skel h-4 flex-1" />
    <div class="skel h-4 w-24" />
    <div class="skel h-6 w-20 rounded-full" />
  </div>

  <div v-else class="flex flex-col gap-2" aria-hidden="true">
    <div
      v-for="line in lines"
      :key="line"
      class="skel h-4"
      :class="line === lines ? 'w-3/5' : 'w-full'"
    />
  </div>
</template>
