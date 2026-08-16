<script setup lang="ts">
import type { NoticeResponse } from '~/types/api'

const props = defineProps<{ notice: NoticeResponse | null }>()

/**
 * Dismissal is deliberately NOT persisted.
 *
 * This banner carries the payment-liability notice, which is the one thing on the site that
 * every visitor is meant to see on every visit. A remembered dismissal defeats that: the
 * returning donor -- exactly the person the notice is written for -- would never see it again.
 * So ✕ means "get out of my way while I read this page", and a reload brings it back.
 *
 * Keeping the state in memory also makes the component honest under SSR. There is nothing for
 * the server to read, so the server and the browser always render the same thing, and nothing
 * is added or removed at hydration. The previous cookie could not manage that: `/`, `/requests`
 * and `/centers` carry `swr` route rules, so Nitro serves one cached render to everybody and no
 * cookie ever reaches it -- the banner shipped in the HTML regardless and had to be hidden again
 * by a head script. Both are gone.
 *
 * If this should ever be remembered again, note the trap that made the old cookie look broken:
 * `useCookie` reads values back through `destr`, so a stored "1" returns as the number 1 and a
 * strict comparison never matches. See AGENTS.md.
 */
const hidden = ref(false)

const visible = computed(() => Boolean(props.notice) && !hidden.value)

function dismiss() {
  hidden.value = true
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
  <div v-if="visible && notice" id="site-announcement" class="dark-panel">
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
