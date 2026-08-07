<script setup>
import { RouterView, RouterLink, useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '../../stores/auth'
import { computed, ref, watch } from 'vue'
import {
  Building2,
  FileText,
  LayoutDashboard,
  LogOut,
  MapPin,
  Megaphone,
  Menu,
  PlusCircle,
  Tags,
  User,
  X
} from '@lucide/vue'
import BrandLogo from '../../components/BrandLogo.vue'
import ThemeToggle from '../../components/ThemeToggle.vue'

const auth = useAuthStore()
const router = useRouter()
const route = useRoute()
const isAdmin = computed(() => auth.isAdmin)
const mobileOpen = ref(false)

const adminLinks = [
  { to: '/dashboard/admin/cases', label: 'مدیریت درخواست‌ها', icon: FileText },
  { to: '/dashboard/admin/centers', label: 'مدیریت مراکز', icon: Building2 },
  { to: '/dashboard/admin/categories', label: 'دسته‌بندی‌ها', icon: Tags },
  { to: '/dashboard/admin/locations', label: 'استان‌ها و شهرها', icon: MapPin },
  { to: '/dashboard/admin/notices', label: 'اطلاعیه‌ها', icon: Megaphone }
]
const centerLinks = [
  { to: '/dashboard/cases', label: 'درخواست‌های من', icon: FileText },
  { to: '/dashboard/cases/new', label: 'ثبت درخواست جدید', icon: PlusCircle }
]
const links = computed(() => isAdmin.value ? adminLinks : centerLinks)

function logout() {
  auth.logout()
  router.push('/')
}

watch(() => route.fullPath, () => { mobileOpen.value = false })
</script>

<template>
  <div class="min-h-screen bg-[var(--semantic-bg)] md:flex">
    <aside class="sticky top-0 hidden h-screen w-72 shrink-0 flex-col border-l border-slate-200 bg-white p-5 dark:border-slate-800 dark:bg-[#0b222c] md:flex">
      <RouterLink to="/" class="rounded-xl px-2 py-1">
        <BrandLogo full :mark="40" tagline="پنل مدیریت" />
      </RouterLink>

      <div class="mt-8 rounded-2xl bg-brand-50 p-4 dark:bg-brand-950/70">
        <div class="text-xs font-bold text-brand-700 dark:text-brand-300">{{ isAdmin ? 'مدیریت سامانه' : 'مرکز خیریه' }}</div>
        <div class="mt-1 truncate font-black text-slate-900 dark:text-white">{{ auth.user?.fullName || auth.user?.username }}</div>
      </div>

      <nav class="mt-7 flex-1 space-y-1" aria-label="منوی داشبورد">
        <RouterLink
          to="/dashboard"
          class="flex min-h-11 items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-semibold text-slate-600 transition hover:bg-brand-50 hover:text-brand-800 dark:text-slate-300 dark:hover:bg-brand-950 dark:hover:text-brand-200"
          exact-active-class="bg-brand-50 text-brand-800 dark:bg-brand-950 dark:text-brand-200"
        >
          <LayoutDashboard :size="18" /> نمای کلی
        </RouterLink>
        <RouterLink
          v-for="link in links"
          :key="link.to"
          :to="link.to"
          class="flex min-h-11 items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-semibold text-slate-600 transition hover:bg-brand-50 hover:text-brand-800 dark:text-slate-300 dark:hover:bg-brand-950 dark:hover:text-brand-200"
          active-class="bg-brand-50 text-brand-800 dark:bg-brand-950 dark:text-brand-200"
        >
          <component :is="link.icon" :size="18" /> {{ link.label }}
        </RouterLink>
      </nav>

      <div class="space-y-1 border-t border-slate-100 pt-4 dark:border-slate-800">
        <RouterLink to="/profile" class="flex min-h-11 items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-semibold text-slate-600 hover:bg-slate-50 dark:text-slate-300 dark:hover:bg-slate-800">
          <User :size="18" /> پروفایل من
        </RouterLink>
        <button class="flex min-h-11 w-full items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-semibold text-red-600 hover:bg-red-50 dark:text-red-300 dark:hover:bg-red-950/30" @click="logout">
          <LogOut :size="18" /> خروج
        </button>
      </div>
    </aside>

    <div class="min-w-0 flex-1">
      <header class="glass sticky top-0 z-40 flex h-[72px] items-center justify-between border-b px-4 sm:px-6">
        <div class="flex items-center gap-3">
          <button class="icon-btn md:hidden" :aria-expanded="mobileOpen" aria-label="منوی داشبورد" @click="mobileOpen = !mobileOpen">
            <X v-if="mobileOpen" :size="20" />
            <Menu v-else :size="20" />
          </button>
          <div>
            <div class="text-xs text-slate-400">فضای کاری</div>
            <div class="text-sm font-bold text-slate-800 dark:text-slate-100">{{ isAdmin ? 'مدیریت یاری‌جو' : 'پنل مرکز خیریه' }}</div>
          </div>
        </div>
        <div class="flex items-center gap-2">
          <ThemeToggle />
          <RouterLink to="/profile" class="hidden min-h-11 items-center gap-2 rounded-2xl border border-slate-200 bg-white px-3 text-sm font-semibold text-slate-700 dark:border-slate-700 dark:bg-slate-900 dark:text-slate-200 sm:flex">
            <span class="grid h-8 w-8 place-items-center rounded-xl bg-brand-50 text-brand-700 dark:bg-brand-950 dark:text-brand-300"><User :size="16" /></span>
            {{ auth.user?.fullName || auth.user?.username }}
          </RouterLink>
        </div>
      </header>

      <transition name="drawer">
        <div v-if="mobileOpen" class="fixed inset-0 z-30 bg-slate-950/35 backdrop-blur-sm md:hidden" @click.self="mobileOpen = false">
          <nav class="absolute right-0 top-[72px] h-[calc(100%-72px)] w-[min(86vw,320px)] overflow-y-auto border-l border-slate-200 bg-white p-4 shadow-2xl dark:border-slate-800 dark:bg-[#0b222c]">
            <RouterLink to="/dashboard" class="flex min-h-12 items-center gap-3 rounded-xl px-3 text-sm font-semibold text-slate-700 dark:text-slate-200"><LayoutDashboard :size="18" /> نمای کلی</RouterLink>
            <RouterLink v-for="link in links" :key="link.to" :to="link.to" class="flex min-h-12 items-center gap-3 rounded-xl px-3 text-sm font-semibold text-slate-700 dark:text-slate-200">
              <component :is="link.icon" :size="18" /> {{ link.label }}
            </RouterLink>
            <RouterLink to="/profile" class="flex min-h-12 items-center gap-3 rounded-xl px-3 text-sm font-semibold text-slate-700 dark:text-slate-200"><User :size="18" /> پروفایل من</RouterLink>
            <button class="flex min-h-12 w-full items-center gap-3 rounded-xl px-3 text-sm font-semibold text-red-600 dark:text-red-300" @click="logout"><LogOut :size="18" /> خروج</button>
          </nav>
        </div>
      </transition>

      <main class="dashboard-content p-4 sm:p-6 lg:p-8">
        <RouterView />
      </main>
    </div>
  </div>
</template>

<style scoped>
.drawer-enter-active,
.drawer-leave-active { transition: opacity 0.18s ease; }
.drawer-enter-active nav,
.drawer-leave-active nav { transition: transform 0.2s ease; }
.drawer-enter-from,
.drawer-leave-to { opacity: 0; }
.drawer-enter-from nav,
.drawer-leave-to nav { transform: translateX(100%); }
</style>
