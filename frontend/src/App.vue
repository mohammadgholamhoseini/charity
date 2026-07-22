<script setup>
import { RouterView, useRoute } from 'vue-router'
import SiteHeader from './components/SiteHeader.vue'
import SiteFooter from './components/SiteFooter.vue'
import { computed, watch } from 'vue'

const route = useRoute()
const isDashboard = computed(() => route.path.startsWith('/dashboard'))

// Scroll to top on route change
watch(
  () => route.path,
  () => window.scrollTo({ top: 0, behavior: 'instant' })
)
</script>

<template>
  <div class="min-h-screen flex flex-col">
    <SiteHeader v-if="!isDashboard" />
    <main class="flex-1" :class="{ 'pb-0': isDashboard }">
      <RouterView v-slot="{ Component }">
        <transition name="page" mode="out-in">
          <component :is="Component" />
        </transition>
      </RouterView>
    </main>
    <SiteFooter v-if="!isDashboard" />
  </div>
</template>

<style>
.page-enter-active,
.page-leave-active {
  transition: opacity 0.25s ease, transform 0.25s ease;
}
.page-enter-from {
  opacity: 0;
  transform: translateY(8px);
}
.page-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}
</style>
