<script setup lang="ts">
/** Shared shell for the text-only pages, so they cannot drift apart typographically. */
defineProps<{ title: string, lead?: string, crumbName: string, crumbPath: string }>()
</script>

<template>
  <div class="page-shell py-8 flex flex-col gap-6 max-w-[760px]">
    <LayoutBreadcrumbs :crumbs="[{ name: crumbName, path: crumbPath }]" />
    <h1 class="text-[28px] lg:text-[34px] font-extrabold">{{ title }}</h1>
    <p v-if="lead" class="text-[17px] leading-[2.1] text-body">{{ lead }}</p>
    <div class="flex flex-col gap-6 text-[15px] leading-[2] text-body">
      <slot />
    </div>
  </div>
</template>
