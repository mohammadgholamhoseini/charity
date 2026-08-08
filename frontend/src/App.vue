<script setup>
import { RouterView, useRoute } from 'vue-router'
import SiteHeader from './components/SiteHeader.vue'
import SiteFooter from './components/SiteFooter.vue'
import { computed } from 'vue'

const route = useRoute()
const isDashboard = computed(() => route.path.startsWith('/dashboard'))
</script>

<template>
  <div class="min-h-screen flex flex-col">
    <a href="#main-content" class="skip-link">پرش به محتوای اصلی</a>
    <SiteHeader v-if="!isDashboard" />
    <main id="main-content" class="flex-1" :class="{ 'pb-0': isDashboard }" tabindex="-1">
      <RouterView />
    </main>
    <SiteFooter v-if="!isDashboard" />
  </div>
</template>
