<script setup lang="ts">
const props = withDefaults(defineProps<{
  modelValue: string | number | null
  label?: string
  type?: string
  hint?: string
  error?: string
  required?: boolean
  placeholder?: string
  /** Latin content — usernames, slugs, phone numbers, URLs. */
  ltr?: boolean
  textarea?: boolean
  rows?: number
  maxlength?: number
  counter?: boolean
  /** Adds the «نمایش» toggle for password fields. */
  revealable?: boolean
  disabled?: boolean
}>(), { type: 'text', rows: 4 })

const emit = defineEmits<{ 'update:modelValue': [string] }>()

const id = useId()
const revealed = ref(false)
const inputType = computed(() => (props.revealable && revealed.value ? 'text' : props.type))
const length = computed(() => String(props.modelValue ?? '').length)
</script>

<template>
  <div class="flex flex-col">
    <label v-if="label" :for="id" class="label">
      {{ label }}
      <span v-if="required" class="text-danger" aria-hidden="true">*</span>
    </label>

    <div class="relative">
      <textarea
        v-if="textarea"
        :id="id"
        class="field resize-y"
        :class="{ 'is-error': error, 'ltr': ltr }"
        :style="{ minHeight: `${rows * 22 + 22}px` }"
        :value="modelValue ?? ''"
        :placeholder="placeholder"
        :maxlength="maxlength"
        :disabled="disabled"
        :aria-invalid="Boolean(error)"
        :aria-describedby="error || hint ? `${id}-help` : undefined"
        @input="emit('update:modelValue', ($event.target as HTMLTextAreaElement).value)"
      />
      <input
        v-else
        :id="id"
        class="field"
        :class="{ 'is-error': error, 'ltr': ltr, 'pe-20': revealable }"
        :type="inputType"
        :value="modelValue ?? ''"
        :placeholder="placeholder"
        :maxlength="maxlength"
        :disabled="disabled"
        :aria-invalid="Boolean(error)"
        :aria-describedby="error || hint ? `${id}-help` : undefined"
        @input="emit('update:modelValue', ($event.target as HTMLInputElement).value)"
      >
      <button
        v-if="revealable"
        type="button"
        class="absolute top-1/2 -translate-y-1/2 text-[13px] font-semibold text-brick-500 hover:text-brick-600"
        style="inset-inline-end: 14px"
        @click="revealed = !revealed"
      >
        {{ revealed ? 'پنهان' : 'نمایش' }}
      </button>
    </div>

    <div v-if="error || hint || counter" :id="`${id}-help`" class="flex justify-between gap-3 mt-2">
      <span v-if="error" class="error-text">{{ error }}</span>
      <span v-else-if="hint" class="help">{{ hint }}</span>
      <span v-if="counter && maxlength" class="help ltr shrink-0">{{ length }}/{{ maxlength }}</span>
    </div>
  </div>
</template>
