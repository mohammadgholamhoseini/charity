<script setup lang="ts">
/**
 * Server-side pagination.
 *
 * Rendered as real links rather than buttons so a crawler can actually follow them —
 * button-based paging leaves every page past the first unreachable, which on a
 * listing site is most of the content.
 */
const props = defineProps<{
  page: number
  totalPages: number
  totalElements: number
}>()

const route = useRoute()

/** A short window around the current page, with the first and last always reachable. */
const pages = computed(() => {
  const total = props.totalPages
  const current = props.page
  if (total <= 7) return Array.from({ length: total }, (_, i) => i + 1)

  const window = new Set<number>([1, total, current])
  for (let offset = 1; offset <= 2; offset++) {
    if (current - offset > 1) window.add(current - offset)
    if (current + offset < total) window.add(current + offset)
  }
  return [...window].sort((a, b) => a - b)
})

function linkTo(page: number) {
  const query = { ...route.query }
  if (page <= 1) delete query.page
  else query.page = String(page)
  return { path: route.path, query }
}
</script>

<template>
  <nav v-if="totalPages > 1" class="flex items-center justify-center gap-2 mt-10" aria-label="صفحه‌بندی">
    <NuxtLink
      v-if="page > 1"
      :to="linkTo(page - 1)"
      class="btn btn-secondary btn-sm"
      rel="prev"
    >
      قبلی
    </NuxtLink>

    <template v-for="(item, index) in pages" :key="item">
      <span v-if="index > 0 && item - pages[index - 1]! > 1" class="text-muted-2 px-1">…</span>
      <NuxtLink
        :to="linkTo(item)"
        class="w-[42px] h-[42px] inline-flex items-center justify-center rounded-[10px] text-[14px] transition-colors"
        :class="item === page
          ? 'bg-brick-500 text-white font-bold'
          : 'border border-line-soft text-body hover:bg-cream-100'"
        :aria-current="item === page ? 'page' : undefined"
      >
        {{ item }}
      </NuxtLink>
    </template>

    <NuxtLink
      v-if="page < totalPages"
      :to="linkTo(page + 1)"
      class="btn btn-secondary btn-sm"
      rel="next"
    >
      بعدی
    </NuxtLink>
  </nav>
</template>
