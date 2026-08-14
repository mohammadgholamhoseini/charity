<script setup lang="ts">
import type { NoticeResponse } from '~/types/api'

defineProps<{ notice: NoticeResponse | null }>()

const config = useRuntimeConfig()
const year = 1405
</script>

<template>
  <footer class="border-t border-line-soft bg-surface mt-auto">
    <div class="page-shell py-10 flex flex-col gap-6">
      <div class="flex flex-wrap items-center justify-between gap-6">
        <BrandLogo :size="34" :show-tagline="false" />
        <nav class="flex flex-wrap items-center gap-6 text-[14px] text-body">
          <NuxtLink to="/terms" class="hover:text-accent">قوانین</NuxtLink>
          <NuxtLink to="/privacy" class="hover:text-accent">حریم خصوصی</NuxtLink>
          <NuxtLink to="/faq" class="hover:text-accent">سؤالات متداول</NuxtLink>
          <NuxtLink to="/contact" class="hover:text-accent">تماس</NuxtLink>
        </nav>
      </div>

      <!-- The footer-placement announcement. Rendered as text, never as HTML. -->
      <p
        v-if="notice"
        class="text-[14px] leading-8 text-muted border-t border-surface-3 pt-5"
      >
        <span class="font-semibold text-body-2">{{ notice.title }}:</span>
        {{ notice.content }}
        <a
          v-if="notice.linkUrl"
          :href="notice.linkUrl"
          rel="noopener"
          class="text-accent hover:text-accent-600 me-1"
        >مشاهده</a>
      </p>

      <div class="flex flex-wrap items-center justify-between gap-3 text-[13px] text-muted-2">
        <span>© {{ year }} {{ config.public.siteName }} — تمامی حقوق محفوظ است.</span>
        <span class="ltr">{{ config.public.supportPhone }}</span>
      </div>
    </div>
  </footer>
</template>
