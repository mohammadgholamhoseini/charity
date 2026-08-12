<script setup lang="ts">
defineProps<{ value: number | string, label: string, status?: string, active?: boolean }>()
const { num } = useFormat()
</script>

<template>
  <div
    class="card-flat p-5 flex flex-col gap-2 text-start w-full transition-colors"
    :class="active ? 'border-brick-500' : ''"
  >
    <span class="text-[26px] font-extrabold leading-none">
      {{ typeof value === 'number' ? num(value) : value }}
    </span>
    <span class="text-[13px] text-muted">{{ label }}</span>
    <span v-if="status" class="chip self-start mt-1" :data-status="status">{{ label }}</span>
  </div>
</template>
