<script setup lang="ts">
/**
 * Status, urgency, category and neutral chips.
 *
 * Status and urgency resolve to CSS via a data attribute, because their colours are a
 * fixed enum. Category colours arrive from the API per row, so they have to be inline
 * styles — Tailwind only emits classes it can see literally in the source, so a
 * computed `bg-${slug}` would produce no CSS at all.
 */
const props = defineProps<{
  status?: string
  urgency?: string
  color?: { bg: string, text: string }
  variant?: 'dark' | 'gold' | 'neutral'
  label: string
  closable?: boolean
}>()

defineEmits<{ close: [] }>()

const style = computed(() =>
  props.color ? { backgroundColor: props.color.bg, color: props.color.text } : undefined,
)

const isPlain = computed(() =>
  props.variant === 'neutral'
  || (!props.status && !props.urgency && !props.color && !props.variant),
)
</script>

<template>
  <span
    class="chip"
    :class="{
      'chip-dark': variant === 'dark',
      'chip-gold': variant === 'gold',
      'chip-neutral': isPlain,
    }"
    :data-status="status"
    :data-urgency="urgency"
    :style="style"
  >
    <slot name="icon" />
    {{ label }}
    <button
      v-if="closable"
      type="button"
      class="opacity-60 hover:opacity-100 leading-none"
      :aria-label="`حذف ${label}`"
      @click="$emit('close')"
    >✕</button>
  </span>
</template>
