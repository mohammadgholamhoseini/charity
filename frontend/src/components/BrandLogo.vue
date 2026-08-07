<script setup>
/*
  BrandLogo — امضای بصری «یاری جو»
  قلب ساخته‌شده از دو کف دست: نیمهٔ چپ سرمه‌ای (نیازمند) و نیمهٔ راست
  مرجانی (یاری‌رسان) که کنار هم قلبِ پیوند را می‌سازند.
  با `full` نشان+واژه‌مارک، بدون آن فقط نشان (مهر).
  با `dark` برای بافت تیره.
*/
const props = defineProps({
  full: { type: Boolean, default: false },
  dark: { type: Boolean, default: false },
  mark: { type: Number, default: 38 },
  text: { type: String, default: 'یاری جو' },
  tagline: { type: String, default: '' }
})

const left = props.dark ? '#86a9e8' : '#204e9d'
const right = props.dark ? '#ff9779' : '#ff704d'
</script>

<template>
  <span class="inline-flex items-center gap-2.5" :aria-label="props.text">
    <span
      class="grid place-items-center shrink-0"
      :style="{ width: props.mark + 'px', height: props.mark + 'px' }"
    >
      <svg :width="props.mark" :height="props.mark" viewBox="0 0 64 64" fill="none">
        <!-- نیمهٔ چپ — کف دستِ نیازمند (سرمه‌ای) -->
        <path
          d="M32 53 C20 44 9 35 9 24 C9 16 15 10 23 10 C28 10 31 13 32 16 Z"
          :fill="left"
        />
        <!-- نیمهٔ راست — کف دستِ یاری‌رسان (مرجانی) -->
        <path
          d="M32 53 C44 44 55 35 55 24 C55 16 49 10 41 10 C36 10 33 13 32 16 Z"
          :fill="right"
        />
        <!-- انگشتان نیمهٔ چپ -->
        <g :stroke="left" stroke-width="2.6" stroke-linecap="round">
          <path d="M11 21 L8 16" />
          <path d="M14 14 L12 9" />
          <path d="M18 11 L18 6" />
        </g>
        <!-- انگشتان نیمهٔ راست -->
        <g :stroke="right" stroke-width="2.6" stroke-linecap="round">
          <path d="M53 21 L56 16" />
          <path d="M50 14 L52 9" />
          <path d="M46 11 L46 6" />
        </g>
      </svg>
    </span>

    <span v-if="props.full" class="flex flex-col leading-none select-none">
      <span
        class="font-display font-black tracking-tight whitespace-nowrap"
        :class="props.dark ? 'text-white' : 'text-slate-900 dark:text-white'"
        :style="{ fontSize: (props.mark * 0.5) + 'px' }"
      >{{ props.text }}</span>
      <span
        v-if="props.tagline"
        class="font-sans text-[10px] mt-1 tracking-wide whitespace-nowrap"
        :class="props.dark ? 'text-accent-300/90' : 'text-slate-500 dark:text-slate-400'"
      >{{ props.tagline }}</span>
    </span>
  </span>
</template>