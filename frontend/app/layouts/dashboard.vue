<script setup lang="ts">
/**
 * One shell for both panels.
 *
 * The centre and admin panels are the same chrome with a different nav array, so
 * building two layouts would just be two places to fix every future change.
 */
const auth = useAuth()

const centerNav = [
  { label: 'داشبورد', to: '/dashboard' },
  { label: 'درخواست‌های من', to: '/dashboard/requests' },
  { label: 'ثبت درخواست جدید', to: '/dashboard/requests/new' },
  { label: 'دسته‌های مجاز مرکز', to: '/dashboard/categories' },
  { label: 'پروفایل من', to: '/profile' },
]

const adminNav = [
  { label: 'داشبورد', to: '/dashboard/admin' },
  { label: 'دسته‌بندی‌ها', to: '/dashboard/admin/categories' },
  { label: 'مراکز خیریه', to: '/dashboard/admin/centers' },
  { label: 'درخواست‌ها', to: '/dashboard/admin/requests' },
  { label: 'اطلاعیه‌ها', to: '/dashboard/admin/announcements' },
  // Not in the design's sidebar, but provinces and cities have to be manageable:
  // every centre needs a city, so without this screen a fresh deployment cannot
  // create its first centre.
  { label: 'استان‌ها و شهرها', to: '/dashboard/admin/locations' },
  { label: 'پروفایل من', to: '/profile' },
]

const nav = computed(() => (auth.isAdmin.value ? adminNav : centerNav))
const mobileNavOpen = ref(false)

// Signing out lives on the composable now, because the site header needs it too.
const signOut = () => auth.logout()
</script>

<template>
  <div class="min-h-screen flex" style="background: var(--color-surface-2)">
    <!-- sidebar -->
    <aside
      class="dark-panel w-[230px] shrink-0 flex-col justify-between p-6 hidden lg:flex"
    >
      <div class="flex flex-col gap-8">
        <BrandLogo :size="36" on-dark :show-tagline="false" />
        <nav class="flex flex-col gap-1">
          <NuxtLink
            v-for="item in nav"
            :key="item.to"
            :to="item.to"
            class="px-4 py-3 rounded-[10px] text-[14px] text-onink-2 hover:text-onink transition-colors"
            active-class="!text-onink"
            :style="$route.path === item.to ? 'background: var(--color-ink-3)' : ''"
          >{{ item.label }}</NuxtLink>
        </nav>
      </div>

      <button type="button" class="text-[14px] text-onink-3 hover:text-onink text-start" @click="signOut">
        خروج از حساب
      </button>
    </aside>

    <div class="flex-1 min-w-0 flex flex-col">
      <!-- topbar -->
      <header class="bg-surface border-b border-line-soft">
        <div class="px-5 lg:px-8 py-4 flex items-center justify-between gap-4">
          <div class="flex items-center gap-3 min-w-0">
            <button
              type="button"
              class="lg:hidden w-11 h-11 rounded-[10px] border border-line-soft"
              aria-label="منو"
              @click="mobileNavOpen = !mobileNavOpen"
            >☰</button>
            <div class="flex flex-col min-w-0">
              <span class="text-[16px] font-bold truncate">
                {{ auth.user.value?.fullName || auth.user.value?.username || 'پنل مدیریت' }}
              </span>
              <span class="text-[12px] text-muted">
                {{ auth.isAdmin.value ? 'حساب ادمین' : 'پنل مرکز خیریه' }}
              </span>
            </div>
          </div>

          <div class="flex items-center gap-3">
            <NuxtLink v-if="!auth.isAdmin.value" to="/dashboard/requests/new" class="btn btn-primary btn-sm">
              ثبت درخواست جدید
            </NuxtLink>
            <NuxtLink to="/" class="text-[13px] text-muted hover:text-accent">مشاهده سایت</NuxtLink>
          </div>
        </div>

        <nav v-if="mobileNavOpen" class="lg:hidden border-t border-line-soft px-5 py-3 flex flex-col">
          <NuxtLink
            v-for="item in nav"
            :key="item.to"
            :to="item.to"
            class="py-2.5 text-[14px] border-b border-surface-3 last:border-0"
            @click="mobileNavOpen = false"
          >{{ item.label }}</NuxtLink>
          <button type="button" class="py-2.5 text-[14px] text-danger text-start" @click="signOut">
            خروج از حساب
          </button>
        </nav>
      </header>

      <main class="flex-1 p-5 lg:p-8 min-w-0">
        <slot />
      </main>
    </div>
  </div>
</template>
