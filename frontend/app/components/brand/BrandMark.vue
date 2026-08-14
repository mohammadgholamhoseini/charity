<script setup lang="ts">
/**
 * The «حلقه‌ی همبستگی» mark: a 270° arc closed by three equally sized nodes.
 *
 * Below 32px the stroke and nodes are thickened, otherwise the arc thins out to the
 * point where the ring reads as a broken circle rather than a deliberate opening.
 */
const props = withDefaults(defineProps<{
  size?: number
  /** Inverts the ring for placement on the ink-coloured surfaces. */
  onDark?: boolean
  /** Single-colour rendering, for print or a monochrome context. */
  mono?: boolean
}>(), {
  size: 42,
  onDark: false,
  mono: false,
})

const small = computed(() => props.size < 32)
const strokeWidth = computed(() => (small.value ? 8.5 : 6.5))
const nodeRadius = computed(() => (small.value ? 5.8 : 5.2))

/**
 * Written out rather than read from the CSS tokens: an SVG `stroke` cannot take a
 * Tailwind class, and `var(--color-accent)` inside an attribute breaks the moment the
 * mark is inlined somewhere without the stylesheet (the favicon, an OG card, an email).
 * Keep these in step with the @theme block and with public/favicon.svg by hand.
 */
const ringColor = computed(() => {
  if (props.mono) return '#0F3070'
  return props.onDark ? '#FFFFFF' : '#C4234F'
})
const nodeColor = computed(() => (props.mono ? '#0F3070' : '#12ADBC'))
const centerColor = computed(() => {
  if (props.mono) return '#0F3070'
  return props.onDark ? '#12ADBC' : '#C4234F'
})
</script>

<template>
  <svg
    :width="size"
    :height="size"
    viewBox="0 0 64 64"
    fill="none"
    role="img"
    aria-label="یاری‌جو"
  >
    <path
      d="M32 11 A21 21 0 1 1 11 32"
      :stroke="ringColor"
      :stroke-width="strokeWidth"
      stroke-linecap="round"
    />
    <circle cx="32" cy="11" :r="nodeRadius" :fill="nodeColor" />
    <circle cx="11" cy="32" :r="nodeRadius" :fill="nodeColor" />
    <circle cx="32" cy="32" :r="nodeRadius" :fill="centerColor" />
  </svg>
</template>
