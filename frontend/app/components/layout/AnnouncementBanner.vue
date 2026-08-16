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
/**
 * The stored value is compared as a string on both sides, and that is not cosmetic.
 *
 * `useCookie` writes the value out raw and reads it back through `destr`, so a cookie set to
 * the string "1" comes back as the NUMBER 1. The old strict `!==` against `String(notice.id)`
 * therefore never matched: the click wrote the cookie correctly and the banner stayed put, on
 * that page and on every page after it. Chrome made it look even stranger -- its CookieStore
 * watcher re-reads the cookie right after the write, so the value flipped from "1" back to 1
 * before the banner could disappear.
 */
const dismissed = useCookie<string | number | null>('yariju.banner', {
  maxAge: 60 * 60 * 24 * 90,
  sameSite: 'lax',
  default: () => null,
})

const visible = computed(() =>
  Boolean(props.notice) && String(dismissed.value) !== String(props.notice?.id),
)

function dismiss() {
  if (props.notice) dismissed.value = String(props.notice.id)
}

/**
 * A dismissed banner still arrives in the HTML of `/`, `/requests` and `/centers`, because those
 * three carry `swr` route rules: Nitro caches one rendered page for everybody and no cookie
 * reaches that render. Hydration would then rip the banner out and shift the whole page up --
 * the exact layout shift the cookie was chosen to avoid, just moved to the cached routes.
 *
 * So the id is baked into a tiny head script that hides the banner before first paint if the
 * cookie matches. The script is identical for every visitor -- it only reads the cookie at
 * runtime -- so it is safe to cache. Vue's `v-if` above is still the source of truth once the
 * app is running; this only covers the gap before hydration.
 */
useHead(() => ({
  script: props.notice
    ? [{
        key: 'yariju-banner-dismissed',
        innerHTML: '(function(){try{'
          + 'var c=document.cookie.split(\'; \');'
          + 'for(var i=0;i<c.length;i++){'
          + 'var p=c[i].split(\'=\');'
          + 'if(p[0]!==\'yariju.banner\')continue;'
          // Coerced through Number so nothing from the API can end this string literal early.
          + `if(p.slice(1).join('=').split('"').join('')!=='${Number(props.notice.id)}')return;`
          + 'var s=document.createElement(\'style\');'
          + 's.textContent=\'#site-announcement{display:none}\';'
          + 'document.head.appendChild(s);return;}'
          + '}catch(e){}})()',
      }]
    : [],
}))
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
