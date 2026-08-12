<script setup lang="ts">
defineProps<{ modelValue: boolean, label: string, description?: string }>()
const emit = defineEmits<{ 'update:modelValue': [boolean] }>()
</script>

<template>
  <div class="flex items-center justify-between gap-5">
    <div class="flex flex-col">
      <span class="text-[14px] font-bold text-body-2">{{ label }}</span>
      <span v-if="description" class="help">{{ description }}</span>
    </div>
    <button
      type="button"
      role="switch"
      :aria-checked="modelValue"
      :aria-label="label"
      class="relative shrink-0 w-[52px] h-[30px] rounded-full transition-colors"
      :class="modelValue ? 'bg-brick-500' : 'bg-line'"
      @click="emit('update:modelValue', !modelValue)"
    >
      <span
        class="absolute top-[3px] w-6 h-6 rounded-full bg-white shadow-sm transition-all"
        :style="{ insetInlineStart: modelValue ? '25px' : '3px' }"
      />
    </button>
  </div>
</template>
