<script setup lang="ts">
const { banner, footer } = useAnnouncements()

// Organization and WebSite structured data belong on every public page, so they are
// emitted once here rather than repeated in each one.
useJsonLd().organization()
</script>

<template>
  <div class="min-h-screen flex flex-col">
    <a href="#main" class="skip-link">پرش به محتوای اصلی</a>

    <LayoutAnnouncementBanner :notice="banner" />
    <LayoutSiteHeader />

    <main id="main" class="flex-1">
      <slot />
    </main>

    <LayoutSiteFooter :notice="footer" />

    <!-- Keeps the bottom tab bar from covering the end of the page on mobile. -->
    <div class="h-[56px] lg:hidden" aria-hidden="true" />
    <LayoutMobileTabBar />
  </div>
</template>
