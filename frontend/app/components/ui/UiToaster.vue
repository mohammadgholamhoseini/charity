<script setup lang="ts">
const { toasts, dismiss } = useToast()

const tone: Record<string, string> = {
  success: 'bg-success-bg text-success border-success-line',
  error: 'bg-danger-bg text-danger border-danger-line',
  info: 'bg-surface-2 text-body border-line-soft',
}
</script>

<template>
  <div
    class="fixed bottom-6 inset-inline-start-6 z-50 flex flex-col gap-3 max-w-[min(92vw,380px)]"
    style="inset-inline-start: 24px"
    role="status"
    aria-live="polite"
  >
    <TransitionGroup name="toast">
      <div
        v-for="toast in toasts"
        :key="toast.id"
        class="flex items-start gap-3 px-5 py-4 rounded-[14px] border text-[14px] leading-7 shadow-card"
        :class="tone[toast.type]"
      >
        <span class="flex-1">{{ toast.message }}</span>
        <button
          type="button"
          class="opacity-60 hover:opacity-100 leading-none"
          aria-label="بستن"
          @click="dismiss(toast.id)"
        >✕</button>
      </div>
    </TransitionGroup>
  </div>
</template>

<style scoped>
.toast-enter-active,
.toast-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}
.toast-enter-from,
.toast-leave-to {
  opacity: 0;
  transform: translateY(8px);
}
</style>
