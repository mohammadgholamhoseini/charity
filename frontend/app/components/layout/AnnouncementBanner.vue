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

function dismiss() {
  if (props.notice) dismissed.value = String(props.notice.id)
}
</script>

<template>
  <!--
    The text wraps rather than being clipped to one line.

    It used to `truncate` both the title and the first line of the body, which failed in a way
    the length limits hid: because both paragraphs were flex items with no shrink control, they
    shrank in proportion and BOTH were cut — a fourteen-character title lost its last three
    characters on a 1180px screen while there was room to spare. And the body was fed through a
    split on "\n", so a notice written as one paragraph handed its entire text to `truncate`.

    An announcement is the one piece of text on the site that exists to be read, so it is shown
    in full. The row wraps, and below `lg` it stacks; `items-start` keeps the chip aligned with
    the first line of a body that is now several lines tall.
  -->
  <div v-if="visible && notice" class="dark-panel">
    <div class="page-shell flex items-start justify-between gap-4 py-3.5">
      <div class="flex flex-col sm:flex-row sm:items-start gap-2 sm:gap-3 min-w-0">
        <span class="chip chip-highlight shrink-0 self-start">اطلاعیه</span>
        <div class="flex flex-col gap-1 min-w-0">
          <p class="text-[15px] leading-7 font-semibold">{{ notice.title }}</p>
          <p class="text-[15px] leading-7 text-onink-2 whitespace-pre-line">{{ notice.content }}</p>
        </div>
      </div>
      <div class="flex items-start gap-2 shrink-0">
        <a
          v-if="notice.linkUrl"
          :href="notice.linkUrl"
          rel="noopener"
          class="text-[14px] font-semibold text-accent-2 hover:underline py-2.5"
        >مشاهده</a>
        <!-- 44px, like every other control here. It was 21x24. -->
        <button
          type="button"
          class="w-11 h-11 shrink-0 flex items-center justify-center text-onink-3 hover:text-onink leading-none text-base"
          aria-label="بستن اطلاعیه"
          @click="dismiss"
        >✕</button>
      </div>
    </div>
  </div>
</template>
