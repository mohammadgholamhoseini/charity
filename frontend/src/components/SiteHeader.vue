<script setup>
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { Menu, X, LogIn, User as UserIcon, LogOut, LayoutDashboard, ChevronDown } from '@lucide/vue'
import BrandLogo from './BrandLogo.vue'
import ThemeToggle from './ThemeToggle.vue'
import { ref, onMounted, onUnmounted, watch } from 'vue'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
const router = useRouter()
const route = useRoute()
const mobileOpen = ref(false)
const profileOpen = ref(false)

const navItems = [
  { to: '/', label: 'خانه' },
  { to: '/cases', label: 'درخواست‌ها' },
  { to: '/centers', label: 'مراکز خیریه' },
  { to: '/cases?status=COMPLETED', label: 'یاری‌های به‌ثمررسیده' }
]

function isActive(to) {
  if (to === '/') return route.path === '/'
  const [path, query] = to.split('?')
  if (!route.path.startsWith(path)) return false
  if (!query) return !route.query.status
  return new URLSearchParams(query).get('status') === route.query.status
}

function logout() {
  auth.logout()
  profileOpen.value = false
  mobileOpen.value = false
  router.push('/')
}

function navigate(path) {
  profileOpen.value = false
  mobileOpen.value = false
  router.push(path)
}

function handleClickOutside(event) {
  if (profileOpen.value && !event.target.closest('[data-profile-menu]')) profileOpen.value = false
}

watch(() => route.fullPath, () => {
  mobileOpen.value = false
  profileOpen.value = false
})
onMounted(() => document.addEventListener('click', handleClickOutside))
onUnmounted(() => document.removeEventListener('click', handleClickOutside))
</script>

<template>
  <header class="glass sticky top-0 z-50 border-b">
    <div class="mx-auto flex h-[72px] max-w-6xl items-center justify-between gap-3 px-4 sm:px-6">
      <RouterLink to="/" class="shrink-0 rounded-xl" aria-label="یاری‌جو، صفحه اصلی">
        <BrandLogo full :mark="40" tagline="پیوند امن برای یاری آگاهانه" />
      </RouterLink>

      <nav aria-label="ناوبری اصلی" class="hidden items-center gap-1 lg:flex">
        <RouterLink
          v-for="item in navItems"
          :key="item.to"
          :to="item.to"
          class="rounded-xl px-3.5 py-2 text-sm font-semibold transition"
          :class="isActive(item.to)
            ? 'bg-brand-50 text-brand-800 dark:bg-brand-950 dark:text-brand-200'
            : 'text-slate-600 hover:bg-white hover:text-slate-900 dark:text-slate-300 dark:hover:bg-slate-800 dark:hover:text-white'"
        >
          {{ item.label }}
        </RouterLink>
      </nav>

      <div class="flex items-center gap-2">
        <ThemeToggle />

        <div v-if="auth.isAuthenticated" class="relative" data-profile-menu>
          <button
            type="button"
            class="flex min-h-11 items-center gap-2 rounded-2xl border border-slate-200 bg-white px-2 py-1.5 text-sm font-semibold text-slate-700 transition hover:border-brand-200 dark:border-slate-700 dark:bg-slate-900 dark:text-slate-200"
            :aria-expanded="profileOpen"
            aria-haspopup="menu"
            @click="profileOpen = !profileOpen"
          >
            <span class="grid h-8 w-8 place-items-center rounded-xl bg-brand-50 text-brand-700 dark:bg-brand-950 dark:text-brand-300">
              <UserIcon :size="16" />
            </span>
            <span class="hidden max-w-28 truncate sm:block">{{ auth.user?.fullName || auth.user?.username }}</span>
            <ChevronDown :size="14" class="hidden sm:block" />
          </button>

          <transition name="slide">
            <div v-if="profileOpen" role="menu" class="card absolute left-0 z-50 mt-2 w-56 p-2 shadow-xl">
              <button class="flex w-full items-center gap-2 rounded-xl px-3 py-2.5 text-sm hover:bg-brand-50 dark:hover:bg-brand-950" @click="navigate(auth.isAdmin ? '/dashboard/admin/cases' : '/dashboard')">
                <LayoutDashboard :size="17" /> پنل کاربری
              </button>
              <button class="flex w-full items-center gap-2 rounded-xl px-3 py-2.5 text-sm hover:bg-brand-50 dark:hover:bg-brand-950" @click="navigate('/profile')">
                <UserIcon :size="17" /> پروفایل من
              </button>
              <button class="flex w-full items-center gap-2 rounded-xl px-3 py-2.5 text-sm text-red-600 hover:bg-red-50 dark:text-red-300 dark:hover:bg-red-950/40" @click="logout">
                <LogOut :size="17" /> خروج
              </button>
            </div>
          </transition>
        </div>

        <RouterLink v-else to="/login" class="btn-outline hidden text-sm sm:inline-flex">
          <LogIn :size="17" /> ورود مراکز
        </RouterLink>

        <button
          type="button"
          class="icon-btn lg:hidden"
          :aria-expanded="mobileOpen"
          aria-controls="mobile-navigation"
          aria-label="بازکردن منوی سایت"
          @click="mobileOpen = !mobileOpen"
        >
          <X v-if="mobileOpen" :size="21" />
          <Menu v-else :size="21" />
        </button>
      </div>
    </div>

    <transition name="slide">
      <nav v-if="mobileOpen" id="mobile-navigation" class="border-t border-slate-200/70 bg-white/95 px-4 py-3 dark:border-slate-800 dark:bg-slate-950/95 lg:hidden">
        <div class="mx-auto max-w-6xl space-y-1">
          <RouterLink
            v-for="item in navItems"
            :key="item.to"
            :to="item.to"
            class="block rounded-xl px-4 py-3 text-sm font-semibold"
            :class="isActive(item.to) ? 'bg-brand-50 text-brand-800 dark:bg-brand-950 dark:text-brand-200' : 'text-slate-600 dark:text-slate-300'"
          >
            {{ item.label }}
          </RouterLink>
          <RouterLink v-if="!auth.isAuthenticated" to="/login" class="btn-primary mt-3 w-full text-sm">
            <LogIn :size="17" /> ورود مراکز و مدیران
          </RouterLink>
        </div>
      </nav>
    </transition>
  </header>
</template>

<style scoped>
.slide-enter-active,
.slide-leave-active { transition: opacity 0.18s ease, transform 0.18s ease; }
.slide-enter-from,
.slide-leave-to { opacity: 0; transform: translateY(-6px); }
</style>
