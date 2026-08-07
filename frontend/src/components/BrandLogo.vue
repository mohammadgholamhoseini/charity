<script setup>
import { computed } from 'vue'

const props = defineProps({
  full: { type: Boolean, default: false },
  inverse: { type: Boolean, default: false },
  mono: { type: Boolean, default: false },
  mark: { type: Number, default: 40 },
  text: { type: String, default: 'یاری‌جو' },
  tagline: { type: String, default: '' }
})

const primary = computed(() => props.mono ? 'currentColor' : props.inverse ? '#7bc7b3' : '#196f5d')
const accent = computed(() => props.mono ? 'currentColor' : '#ef861c')
</script>

<template>
  <span class="inline-flex items-center gap-2.5" :aria-label="props.text">
    <svg
      :width="props.mark"
      :height="props.mark"
      viewBox="0 0 72 72"
      fill="none"
      role="img"
      aria-hidden="true"
      class="shrink-0 overflow-visible"
    >
      <path
        d="M36 62C29 55 15 46 11 34C7 22 14 12 24 12C31 12 35 17 36 22C37 17 41 12 48 12C58 12 65 22 61 34C57 46 43 55 36 62Z"
        :fill="primary"
        opacity=".14"
      />
      <path
        d="M35.8 59.2C28.6 51.9 17.2 44.7 13.5 34.1C10.5 25.5 15.3 17.4 23.5 17.4C29.7 17.4 34 22.1 35.8 27.2"
        :stroke="primary"
        stroke-width="6"
        stroke-linecap="round"
      />
      <path
        d="M36.2 59.2C43.4 51.9 54.8 44.7 58.5 34.1C61.5 25.5 56.7 17.4 48.5 17.4C42.3 17.4 38 22.1 36.2 27.2"
        :stroke="accent"
        stroke-width="6"
        stroke-linecap="round"
      />
      <path d="M25 36C29 39 32.5 40.5 36 40.5C39.5 40.5 43 39 47 36" :stroke="primary" stroke-width="4" stroke-linecap="round" />
      <path d="M36 27V47" :stroke="accent" stroke-width="4" stroke-linecap="round" />
      <circle cx="36" cy="53" r="2.8" :fill="accent" />
    </svg>

    <span v-if="props.full" class="flex flex-col leading-none select-none">
      <span
        class="font-display font-black tracking-tight whitespace-nowrap"
        :class="props.inverse ? 'text-white' : 'text-slate-900 dark:text-white'"
        :style="{ fontSize: (props.mark * 0.48) + 'px' }"
      >{{ props.text }}</span>
      <span
        v-if="props.tagline"
        class="mt-1 whitespace-nowrap text-[10px] font-medium tracking-wide"
        :class="props.inverse ? 'text-white/65' : 'text-slate-500 dark:text-slate-400'"
      >{{ props.tagline }}</span>
    </span>
  </span>
</template>
