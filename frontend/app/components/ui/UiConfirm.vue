<script setup lang="ts">
/**
 * Replaces the native `confirm()` the old panels used for every destructive action.
 * Native dialogs cannot be styled, block the whole tab, and give no room to explain
 * what is about to happen.
 */
withDefaults(defineProps<{
  open: boolean
  title: string
  message: string
  confirmLabel?: string
  tone?: 'default' | 'danger'
  busy?: boolean
}>(), { confirmLabel: 'تأیید', tone: 'default', busy: false })

defineEmits<{ confirm: [], close: [] }>()
</script>

<template>
  <UiModal :open="open" :title="title" size="sm" @close="$emit('close')">
    <p class="text-[15px] leading-8 text-body">{{ message }}</p>
    <slot />
    <div class="flex items-center gap-3 mt-7">
      <button
        type="button"
        class="btn"
        :class="tone === 'danger' ? 'btn-danger' : 'btn-primary'"
        :disabled="busy"
        @click="$emit('confirm')"
      >
        {{ busy ? 'در حال انجام…' : confirmLabel }}
      </button>
      <button type="button" class="btn btn-secondary" :disabled="busy" @click="$emit('close')">
        انصراف
      </button>
    </div>
  </UiModal>
</template>
