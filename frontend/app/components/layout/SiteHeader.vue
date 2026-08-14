<script setup lang="ts">
const nav = [
  { label: 'درخواست‌ها', to: '/requests' },
  { label: 'مراکز خیریه', to: '/centers' },
  { label: 'درباره ما', to: '/about' },
  { label: 'تماس', to: '/contact' },
]

const auth = useAuth()
const mobileOpen = ref(false)
const route = useRoute()

watch(() => route.fullPath, () => { mobileOpen.value = false })
</script>

<template>
  <header class="bg-surface border-b border-line-soft">
    <div class="page-shell flex items-center justify-between gap-6 py-5">
      <BrandLogo :size="42" />

      <nav class="hidden lg:flex items-center gap-8 text-[15px] text-body-2">
        <NuxtLink
          v-for="item in nav"
          :key="item.to"
          :to="item.to"
          class="hover:text-accent transition-colors"
          active-class="text-accent font-semibold"
        >{{ item.label }}</NuxtLink>
      </nav>

      <div class="hidden lg:flex items-center gap-3">
        <!--
          Auth state is client-only, because the token lives in localStorage and the
          server therefore always renders the signed-out view. The fallback reserves
          the same width so the header does not jump when hydration fills it in.
        -->
        <ClientOnly>
          <NuxtLink
            v-if="auth.isAuthenticated.value"
            :to="auth.homePath.value"
            class="text-[15px] font-semibold text-accent px-4 py-2.5 hover:text-accent-600"
          >پنل من</NuxtLink>
          <NuxtLink
            v-else
            to="/login"
            class="text-[15px] font-semibold text-accent px-4 py-2.5 hover:text-accent-600"
          >ورود مراکز</NuxtLink>
          <template #fallback>
            <span class="text-[15px] font-semibold px-4 py-2.5 opacity-0" aria-hidden="true">ورود مراکز</span>
          </template>
        </ClientOnly>

        <NuxtLink to="/requests" class="btn btn-primary">مرور درخواست‌ها</NuxtLink>
      </div>

      <button
        type="button"
        class="lg:hidden w-11 h-11 inline-flex items-center justify-center rounded-[10px] border border-line-soft"
        :aria-expanded="mobileOpen"
        aria-label="منو"
        @click="mobileOpen = !mobileOpen"
      >
        <span class="text-xl leading-none">{{ mobileOpen ? '✕' : '☰' }}</span>
      </button>
    </div>

    <Transition name="drawer">
      <nav v-if="mobileOpen" class="lg:hidden border-t border-line-soft bg-surface">
        <div class="page-shell py-4 flex flex-col">
          <NuxtLink
            v-for="item in nav"
            :key="item.to"
            :to="item.to"
            class="py-3 text-[15px] border-b border-surface-3 last:border-0"
          >{{ item.label }}</NuxtLink>
          <ClientOnly>
            <NuxtLink
              :to="auth.isAuthenticated.value ? auth.homePath.value : '/login'"
              class="py-3 text-[15px] font-semibold text-accent"
            >{{ auth.isAuthenticated.value ? 'پنل من' : 'ورود مراکز' }}</NuxtLink>
          </ClientOnly>
        </div>
      </nav>
    </Transition>
  </header>
</template>

<style scoped>
.drawer-enter-active,
.drawer-leave-active { transition: opacity 0.15s ease; }
.drawer-enter-from,
.drawer-leave-to { opacity: 0; }
</style>
