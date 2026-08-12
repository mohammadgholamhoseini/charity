<script setup lang="ts">
/** The bottom tab bar from the mobile design. Every target is at least 44px. */
const auth = useAuth()

const tabs = [
  { label: 'خانه', to: '/', icon: '⌂' },
  { label: 'درخواست‌ها', to: '/requests', icon: '☰' },
  { label: 'مراکز', to: '/centers', icon: '◎' },
]
</script>

<template>
  <nav
    class="lg:hidden fixed bottom-0 inset-x-0 z-40 bg-surface border-t border-line-soft"
    style="padding-bottom: env(safe-area-inset-bottom)"
    aria-label="ناوبری اصلی"
  >
    <div class="grid grid-cols-4">
      <NuxtLink
        v-for="tab in tabs"
        :key="tab.to"
        :to="tab.to"
        class="flex flex-col items-center justify-center gap-1 min-h-[56px] text-[11px] text-muted"
        active-class="text-brick-500"
      >
        <span class="text-lg leading-none" aria-hidden="true">{{ tab.icon }}</span>
        {{ tab.label }}
      </NuxtLink>

      <ClientOnly>
        <NuxtLink
          :to="auth.isAuthenticated.value ? auth.homePath.value : '/login'"
          class="flex flex-col items-center justify-center gap-1 min-h-[56px] text-[11px] text-muted"
          active-class="text-brick-500"
        >
          <span class="text-lg leading-none" aria-hidden="true">◍</span>
          {{ auth.isAuthenticated.value ? 'پنل' : 'ورود' }}
        </NuxtLink>
        <template #fallback>
          <span class="flex flex-col items-center justify-center gap-1 min-h-[56px] text-[11px] text-muted">
            <span class="text-lg leading-none" aria-hidden="true">◍</span>
            ورود
          </span>
        </template>
      </ClientOnly>
    </div>
  </nav>
</template>
