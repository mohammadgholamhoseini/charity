<script setup lang="ts">
/**
 * A failed fetch inside an otherwise working page.
 *
 * Kept distinct from the empty state on purpose: "nothing matched your filters" and
 * "we could not reach the server" need completely different responses from the user,
 * and only one of them is worth offering a retry for.
 */
defineProps<{ title?: string, description?: string }>()
defineEmits<{ retry: [] }>()
</script>

<template>
  <div
    class="card-flat px-8 py-14 text-center flex flex-col items-center gap-3"
    style="border-color: var(--color-danger-line)"
  >
    <h3 class="text-[19px] font-bold">{{ title ?? 'دریافت اطلاعات ممکن نشد' }}</h3>
    <p class="text-[14px] text-muted max-w-md leading-8">
      {{ description ?? 'ارتباط با سرور برقرار نشد. اتصال خود را بررسی کنید و دوباره تلاش کنید.' }}
    </p>
    <button type="button" class="btn btn-secondary mt-2" @click="$emit('retry')">
      تلاش دوباره
    </button>
  </div>
</template>
