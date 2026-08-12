<script setup lang="ts">
/** The mark locked up with the wordmark and its tagline. */
const props = withDefaults(defineProps<{
  size?: number
  onDark?: boolean
  /** Hides the tagline where vertical space is tight, e.g. the dashboard sidebar. */
  showTagline?: boolean
  /** Renders as plain markup instead of a link — for use when already inside one. */
  static?: boolean
}>(), {
  size: 42,
  onDark: false,
  showTagline: true,
  static: false,
})

const config = useRuntimeConfig()
const wordmarkSize = computed(() => `${Math.round(props.size * 0.48)}px`)
</script>

<template>
  <NuxtLink v-if="!static" to="/" class="inline-flex items-center gap-3">
    <BrandMark :size="size" :on-dark="onDark" />
    <span class="flex flex-col leading-tight">
      <span
        class="font-extrabold"
        :class="onDark ? 'text-ondark' : 'text-ink-900'"
        :style="{ fontSize: wordmarkSize }"
      >{{ config.public.siteName }}</span>
      <span
        v-if="showTagline"
        class="text-[11px] tracking-[1px]"
        :class="onDark ? 'text-ondark-3' : 'text-muted-2'"
      >{{ config.public.siteTagline }}</span>
    </span>
  </NuxtLink>

  <div v-else class="inline-flex items-center gap-3">
    <BrandMark :size="size" :on-dark="onDark" />
    <span class="flex flex-col leading-tight">
      <span
        class="font-extrabold"
        :class="onDark ? 'text-ondark' : 'text-ink-900'"
        :style="{ fontSize: wordmarkSize }"
      >{{ config.public.siteName }}</span>
      <span
        v-if="showTagline"
        class="text-[11px] tracking-[1px]"
        :class="onDark ? 'text-ondark-3' : 'text-muted-2'"
      >{{ config.public.siteTagline }}</span>
    </span>
  </div>
</template>
