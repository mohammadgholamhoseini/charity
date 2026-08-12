<script setup lang="ts">
/** A list that legitimately has nothing in it. Not a failure — see UiErrorState. */
defineProps<{ title: string, description?: string }>()
</script>

<template>
  <div class="card-flat px-8 py-14 text-center flex flex-col items-center gap-3">
    <div
      class="w-14 h-14 rounded-full bg-cream-100 flex items-center justify-center text-2xl text-muted-2"
      aria-hidden="true"
    >
      ؟
    </div>
    <h3 class="text-[19px] font-bold">{{ title }}</h3>
    <p v-if="description" class="text-[14px] text-muted max-w-md leading-8">{{ description }}</p>
    <div class="mt-2"><slot /></div>
  </div>
</template>
