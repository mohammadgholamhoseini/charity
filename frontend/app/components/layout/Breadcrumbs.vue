<script setup lang="ts">
import type { Crumb } from '~/composables/useJsonLd'

const props = defineProps<{ crumbs: Crumb[] }>()

// The same array drives both the visible trail and the BreadcrumbList structured
// data, so the two cannot drift apart.
useJsonLd().breadcrumbs([{ name: 'خانه', path: '/' }, ...props.crumbs])
</script>

<template>
  <nav aria-label="مسیر صفحه" class="flex items-center gap-2 text-[13px] text-muted flex-wrap">
    <NuxtLink to="/" class="hover:text-accent">خانه</NuxtLink>
    <template v-for="(crumb, index) in crumbs" :key="crumb.path">
      <span aria-hidden="true">←</span>
      <NuxtLink
        v-if="index < crumbs.length - 1"
        :to="crumb.path"
        class="hover:text-accent"
      >{{ crumb.name }}</NuxtLink>
      <span v-else class="text-body-2">{{ crumb.name }}</span>
    </template>
  </nav>
</template>
