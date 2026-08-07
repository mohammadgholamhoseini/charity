<script setup>
import { RouterView, RouterLink, useRouter } from 'vue-router'
import { useAuthStore } from '../../stores/auth'
import { computed } from 'vue'
import { LogOut, LayoutDashboard, FileText, PlusCircle, Building2, Tags, Megaphone, Menu, X, MapPin } from '@lucide/vue'
import BrandLogo from '../../components/BrandLogo.vue'
import { ref } from 'vue'

const auth = useAuthStore()
const router = useRouter()
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
</script>

<template>
  <div class="min-h-screen flex bg-surface dark:bg-[#0a152e]">
    <!-- Desktop sidebar -->
    <aside class="w-64 bg-white dark:bg-slate-900 border-l border-slate-100 dark:border-slate-800 p-5 hidden md:flex flex-col sticky top-0 h-screen">
      <RouterLink to="/" class="mb-8 px-2">
        <BrandLogo full dark :mark="34" />
      </RouterLink>

      <div class="text-xs font-medium text-slate-400 px-3 mb-2">منو</div>
      <nav class="space-y-1 flex-1">
        <RouterLink to="/dashboard" class="flex items-center gap-3 px-3 py-2.5 rounded-xl text-slate-600 dark:text-slate-300 hover:bg-brand-50 dark:hover:bg-brand-900/30 hover:text-brand-700 dark:hover:text-brand-300 transition" exact-active-class="bg-brand-50 dark:bg-brand-900/40 text-brand-700 dark:text-brand-300 font-medium">
          <LayoutDashboard :size="18" /> داشبورد
        </RouterLink>
        <RouterLink v-for="l in links" :key="l.to" :to="l.to"
          class="flex items-center gap-3 px-3 py-2.5 rounded-xl text-slate-600 dark:text-slate-300 hover:bg-brand-50 dark:hover:bg-brand-900/30 hover:text-brand-700 dark:hover:text-brand-300 transition"
          active-class="bg-brand-50 dark:bg-brand-900/40 text-brand-700 dark:text-brand-300 font-medium">
          <component :is="l.icon" :size="18" /> {{ l.label }}
        </RouterLink>
      </nav>

      <button @click="logout" class="flex items-center gap-3 px-3 py-2.5 rounded-xl text-red-500 hover:bg-red-50 dark:hover:bg-red-900/20 transition mt-2">
        <LogOut :size="18" /> خروج
      </button>
    </aside>

    <!-- Main -->
    <div class="flex-1 min-w-0 flex flex-col">
      <header class="bg-white/80 dark:bg-slate-900/80 backdrop-blur border-b border-slate-100 dark:border-slate-800 h-16 flex items-center justify-between px-4 sm:px-6 sticky top-0 z-30">
        <button @click="mobileOpen = !mobileOpen" class="md:hidden grid place-items-center w-10 h-10 rounded-xl text-slate-600 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-800">
          <Menu v-if="!mobileOpen" :size="20" /><X v-else :size="20" />
        </button>
        <span class="text-sm text-slate-500 dark:text-slate-400 hidden sm:block">خوش‌آمدید، <b class="text-slate-700 dark:text-slate-200">{{ auth.user?.username }}</b></span>
        <div class="flex items-center gap-2 mr-auto sm:mr-0">
          <span class="chip bg-brand-50 dark:bg-brand-900/40 text-brand-700 dark:text-brand-300">
            {{ isAdmin ? 'ادمین' : (auth.user?.fullName || auth.user?.username) }}
          </span>
          <span class="grid place-items-center w-9 h-9 rounded-full bg-gradient-to-br from-brand-400 to-brand-600 text-white text-sm font-bold">
            {{ (auth.user?.username || '؟').charAt(0) }}
          </span>
        </div>
      </header>

      <!-- Mobile drawer -->
      <transition name="slide">
        <nav v-if="mobileOpen" class="md:hidden bg-white dark:bg-slate-900 border-b border-slate-100 dark:border-slate-800 px-4 py-3 space-y-1">
          <RouterLink to="/dashboard" @click="mobileOpen=false" class="flex items-center gap-3 px-3 py-3 rounded-xl text-slate-600 dark:text-slate-300">
            <LayoutDashboard :size="18" /> داشبورد
          </RouterLink>
          <RouterLink v-for="l in links" :key="l.to" :to="l.to" @click="mobileOpen=false"
            class="flex items-center gap-3 px-3 py-3 rounded-xl text-slate-600 dark:text-slate-300">
            <component :is="l.icon" :size="18" /> {{ l.label }}
          </RouterLink>
          <button @click="logout" class="flex w-full items-center gap-3 px-3 py-3 rounded-xl text-red-500">
            <LogOut :size="18" /> خروج
          </button>
        </nav>
      </transition>

      <main class="p-4 sm:p-6 flex-1">
        <RouterView />
      </main>
    </div>
  </div>
</template>

<style scoped>
.slide-enter-active, .slide-leave-active { transition: opacity 0.2s ease, transform 0.2s ease; }
.slide-enter-from, .slide-leave-to { opacity: 0; transform: translateY(-8px); }
</style>
