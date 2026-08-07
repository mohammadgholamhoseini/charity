<script setup>
import { Moon, Sun } from '@lucide/vue'
import { useTheme } from '../composables/useTheme'

const { isDark, toggleDark } = useTheme()
</script>

<template>
  <button
    type="button"
    class="icon-btn"
    :aria-label="isDark ? 'فعال‌کردن تم روشن' : 'فعال‌کردن تم تاریک'"
    :title="isDark ? 'تم روشن' : 'تم تاریک'"
    @click="toggleDark"
  >
    <Sun v-if="isDark" :size="19" />
    <Moon v-else :size="19" />
  </button>
</template>
