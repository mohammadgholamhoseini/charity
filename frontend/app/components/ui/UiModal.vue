<script setup lang="ts">
const props = defineProps<{ open: boolean, title: string, size?: 'sm' | 'md' | 'lg' }>()
const emit = defineEmits<{ close: [] }>()

const panel = ref<HTMLElement | null>(null)

// Stops the page behind the dialog scrolling with the wheel while it is open.
const locked = useScrollLock(import.meta.client ? document.body : null)
watch(() => props.open, (open) => {
  locked.value = open
  if (open) nextTick(() => panel.value?.focus())
})
onUnmounted(() => { locked.value = false })

onKeyStroke('Escape', () => { if (props.open) emit('close') })

const width = computed(() => ({ sm: 'max-w-md', md: 'max-w-xl', lg: 'max-w-3xl' }[props.size ?? 'md']))
</script>

<template>
  <Teleport to="body">
    <Transition name="modal">
      <div
        v-if="open"
        class="fixed inset-0 z-50 flex items-center justify-center p-5"
        style="background: rgb(15 48 112 / 0.45)"
        @click.self="emit('close')"
      >
        <div
          ref="panel"
          class="card w-full p-7 max-h-[85vh] overflow-y-auto outline-none"
          :class="width"
          role="dialog"
          aria-modal="true"
          :aria-label="title"
          tabindex="-1"
        >
          <div class="flex items-start justify-between gap-4 mb-5">
            <h2 class="text-[20px] font-bold leading-8">{{ title }}</h2>
            <button
              type="button"
              class="text-muted hover:text-ink leading-none text-lg"
              aria-label="بستن"
              @click="emit('close')"
            >✕</button>
          </div>
          <slot />
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.modal-enter-active,
.modal-leave-active { transition: opacity 0.18s ease; }
.modal-enter-from,
.modal-leave-to { opacity: 0; }
</style>
