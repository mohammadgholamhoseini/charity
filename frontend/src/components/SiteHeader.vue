<script setup>
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { Menu, X, LogIn, User as UserIcon, LogOut, LayoutDashboard } from '@lucide/vue'
import BrandLogo from './BrandLogo.vue'
import { ref, onMounted, onUnmounted } from 'vue'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
const router = useRouter()
const route = useRoute()
const mobileOpen = ref(false)
const profileOpen = ref(false)

const navItems = [
  { to: '/', label: 'خانه' },
  { to: '/centers', label: 'مراکز' },
  { to: '/cases', label: 'درخواست‌های کمک' },
  { to: '/cases?status=COMPLETED', label: 'کمک‌های انجام‌شده' }
]

function isActive(to) {
  if (to === '/') return route.path === '/'
  return route.path.startsWith(to.split('?')[0])
}

function logout() {
  auth.logout()
  profileOpen.value = false
  mobileOpen.value = false
  router.push('/')
}

function goProfile() {
  profileOpen.value = false
  mobileOpen.value = false
  router.push('/profile')
}

function goDashboard() {
  profileOpen.value = false
  mobileOpen.value = false
  router.push(auth.isAdmin ? '/dashboard/admin/cases' : '/dashboard')
}

const displayName = () => auth.user?.fullName || auth.user?.username || 'کاربر'

function handleClickOutside(e) {
  if (profileOpen.value && !e.target.closest('[data-profile-menu]')) {
    profileOpen.value = false
  }
}
onMounted(() => document.addEventListener('click', handleClickOutside))
onUnmounted(() => document.removeEventListener('click', handleClickOutside))
</script>

<template>
  <header class="sticky top-0 z-50 glass border-b border-slate-200/50 dark:border-slate-800/50">
    <div class="max-w-6xl mx-auto px-4 h-16 flex items-center justify-between gap-4">
      <!-- Logo -->
      <RouterLink to="/" class="group shrink-0">
        <BrandLogo full dark :mark="38" tagline="سامانهٔ کمک‌رسانی" />
      </RouterLink>

      <!-- Desktop nav -->
      <nav class="hidden md:flex items-center gap-1">
        <RouterLink
          v-for="item in navItems"
          :key="item.to"
          :to="item.to"
          class="px-3.5 py-2 rounded-xl text-sm font-medium transition"
          :class="isActive(item.to)
            ? 'bg-brand-50 dark:bg-brand-900/40 text-brand-700 dark:text-brand-300'
            : 'text-slate-600 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-800'"
        >
          {{ item.label }}
        </RouterLink>
      </nav>

      <!-- Right actions -->
      <div class="flex items-center gap-2">
        <template v-if="auth.isAuthenticated">
          <div class="relative" data-profile-menu>
            <button
              @click="profileOpen = !profileOpen"
              class="flex items-center gap-2 px-2.5 py-1.5 rounded-xl text-sm font-medium text-slate-700 dark:text-slate-200 hover:bg-slate-100 dark:hover:bg-slate-800 transition"
            >
              <span class="grid place-items-center w-8 h-8 rounded-full bg-brand-100 dark:bg-brand-900/50 text-brand-700 dark:text-brand-300">
                <UserIcon :size="16" />
              </span>
              <span class="hidden sm:block max-w-[120px] truncate">{{ displayName() }}</span>
            </button>

            <transition name="slide">
              <div v-if="profileOpen" class="absolute left-0 mt-2 w-52 card p-1.5 shadow-xl z-50">
                <button @click="goDashboard" class="flex items-center gap-2 w-full px-3 py-2 rounded-lg text-sm text-slate-700 dark:text-slate-200 hover:bg-slate-100 dark:hover:bg-slate-800">
                  <LayoutDashboard :size="16" /> پنل کاربری
                </button>
                <button @click="goProfile" class="flex items-center gap-2 w-full px-3 py-2 rounded-lg text-sm text-slate-700 dark:text-slate-200 hover:bg-slate-100 dark:hover:bg-slate-800">
                  <UserIcon :size="16" /> پروفایل من
                </button>
                <button @click="logout" class="flex items-center gap-2 w-full px-3 py-2 rounded-lg text-sm text-red-600 dark:text-red-400 hover:bg-red-50 dark:hover:bg-red-900/20">
                  <LogOut :size="16" /> خروج
                </button>
              </div>
            </transition>
          </div>
        </template>

        <template v-else>
          <RouterLink to="/login" class="btn-outline hidden sm:inline-flex text-sm">
            <LogIn :size="16" />
            ورود
          </RouterLink>
        </template>

        <button
          @click="mobileOpen = !mobileOpen"
          class="grid md:hidden place-items-center w-10 h-10 rounded-xl text-slate-600 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-800 transition"
          aria-label="منو"
        >
          <Menu v-if="!mobileOpen" :size="20" />
          <X v-else :size="20" />
        </button>
      </div>
    </div>

    <!-- Mobile menu -->
    <transition name="slide">
      <nav v-if="mobileOpen" class="md:hidden border-t border-slate-200/50 dark:border-slate-800/50 px-4 py-3 space-y-1 bg-white/90 dark:bg-slate-900/90 backdrop-blur-xl">
        <RouterLink
          v-for="item in navItems"
          :key="item.to"
          :to="item.to"
          @click="mobileOpen = false"
          class="block px-4 py-3 rounded-xl text-sm font-medium"
          :class="isActive(item.to)
            ? 'bg-brand-50 dark:bg-brand-900/40 text-brand-700 dark:text-brand-300'
            : 'text-slate-600 dark:text-slate-300'"
        >
          {{ item.label }}
        </RouterLink>

        <template v-if="auth.isAuthenticated">
          <button @click="goDashboard" class="flex items-center gap-2 w-full px-4 py-3 rounded-xl text-sm font-medium text-slate-600 dark:text-slate-300">
            <LayoutDashboard :size="16" /> پنل کاربری
          </button>
          <button @click="goProfile" class="flex items-center gap-2 w-full px-4 py-3 rounded-xl text-sm font-medium text-slate-600 dark:text-slate-300">
            <UserIcon :size="16" /> پروفایل من
          </button>
          <button @click="logout" class="flex items-center gap-2 w-full px-4 py-3 rounded-xl text-sm font-medium text-red-600 dark:text-red-400">
            <LogOut :size="16" /> خروج
          </button>
        </template>
        <template v-else>
          <RouterLink to="/login" @click="mobileOpen = false" class="btn-primary w-full mt-2 text-sm">
            <LogIn :size="16" />
            ورود به پنل
          </RouterLink>
        </template>
      </nav>
    </transition>
  </header>
</template>

<style scoped>
.slide-enter-active,
.slide-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}
.slide-enter-from,
.slide-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}
</style>
