<script setup lang="ts">
import type { NoticeResponse } from '~/types/api'

const props = defineProps<{ notice: NoticeResponse | null }>()

/**
 * Dismissal is stored in a cookie rather than localStorage.
 *
 * This is an intentional deviation from the design spec, which says localStorage.
 * Under SSR the server cannot read localStorage, so it would render the banner into
 * the HTML, paint it, and then have hydration remove it — a layout shift on every
 * single page load for every returning visitor, which would dominate the CLS score.
 * A cookie is visible to the server, so a dismissed banner is simply never rendered.
 */
const dismissed = useCookie<string | null>('yariju.banner', {
  maxAge: 60 * 60 * 24 * 90,
  sameSite: 'lax',
  default: () => null,
})

const visible = computed(() =>
  Boolean(props.notice) && dismissed.value !== String(props.notice?.id),
)

/** The banner is one line; the body's first line is all that fits. */
const firstLine = computed(() => props.notice?.content.split('\n')[0]?.trim() ?? '')

function dismiss() {
  if (props.notice) dismissed.value = String(props.notice.id)
}
</script>

<template>
  <div v-if="visible && notice" class="dark-panel">
    <div class="page-shell flex items-center justify-between gap-4 py-3.5">
      <div class="flex items-center gap-3 min-w-0">
        <span class="chip chip-highlight shrink-0">اطلاعیه</span>
        <p class="text-[15px] leading-7 truncate">{{ notice.title }}</p>
        <p class="text-[15px] leading-7 text-onink-2 truncate hidden lg:block">{{ firstLine }}</p>
      </div>
      <div class="flex items-center gap-4 shrink-0">
        <a
          v-if="notice.linkUrl"
          :href="notice.linkUrl"
          rel="noopener"
          class="text-[14px] font-semibold text-accent-2 hover:underline"
        >مشاهده</a>
        <button
          type="button"
          class="text-onink-3 hover:text-onink leading-none text-base p-1"
          aria-label="بستن اطلاعیه"
          @click="dismiss"
        >✕</button>
      </div>
    </div>
  </div>
</template>
