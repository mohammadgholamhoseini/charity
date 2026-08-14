<script setup lang="ts">
import { ep } from '~/api/endpoints'
import type { AuthResponse } from '~/types/api'

definePageMeta({ layout: 'auth' })

const { $api } = useNuxtApp()
const auth = useAuth()
const route = useRoute()
const config = useRuntimeConfig()

/**
 * The role tabs are a hint, not a gate: the backend decides the role from the account
 * itself. They exist because the design has them and they tell a centre it is in the
 * right place — but the form submits identically either way.
 */
const tab = ref<'CENTER' | 'ADMIN'>('CENTER')

const username = ref('')
const password = ref('')
const remember = ref(true)
const submitting = ref(false)
const errorMessage = ref('')

async function submit() {
  errorMessage.value = ''
  if (!username.value.trim() || !password.value) {
    errorMessage.value = 'نام کاربری و رمز عبور را وارد کنید.'
    return
  }

  submitting.value = true
  try {
    const response = await $api<AuthResponse>(ep.login, {
      method: 'POST',
      body: { username: username.value.trim(), password: password.value },
    })
    auth.setSession(response.token, {
      userId: response.userId,
      username: response.username,
      role: response.role,
      fullName: response.fullName,
      centerId: response.centerId,
    })
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : null
    await navigateTo(redirect ?? (response.role === 'ADMIN' ? '/dashboard/admin' : '/dashboard'))
  }
  catch (error) {
    errorMessage.value = apiErrorMessage(error, 'ورود ممکن نشد. دوباره تلاش کنید.')
  }
  finally {
    submitting.value = false
  }
}

useSeo({
  title: 'ورود به پنل — یاری‌جو',
  description: 'ورود مراکز خیریه و ادمین به پنل مدیریت یاری‌جو.',
  noindex: true,
})
</script>

<template>
  <div class="grid lg:grid-cols-2 min-h-screen">
    <!-- dark side -->
    <section class="dark-panel flex flex-col justify-between gap-10 p-8 lg:p-14 order-2 lg:order-1">
      <BrandLogo :size="42" on-dark />

      <div class="flex flex-col gap-5 max-w-md">
        <h1 class="text-[28px] lg:text-[38px] font-extrabold text-onink leading-[1.4]">
          ورود به پنل مراکز و ادمین
        </h1>
        <p class="text-[15px] leading-[2] text-onink-2">
          یاری‌جو ثبت‌نام عمومی ندارد. حساب مراکز خیریه فقط توسط ادمین ایجاد می‌شود؛
          اگر مرکز شما هنوز حساب ندارد، با پشتیبانی تماس بگیرید.
        </p>

        <div class="grid grid-cols-3 gap-4 mt-2">
          <div v-for="stat in [
            { value: '۳۱', label: 'مرکز خیریه' },
            { value: '۱۲۴', label: 'درخواست فعال' },
            { value: '۸', label: 'دسته‌بندی' },
          ]" :key="stat.label" class="flex flex-col gap-1">
            <span class="text-[24px] font-extrabold text-accent-2">{{ stat.value }}</span>
            <span class="text-[12px] text-onink-3">{{ stat.label }}</span>
          </div>
        </div>
      </div>

      <p class="text-[14px] text-onink-3">
        پشتیبانی: <span class="ltr">{{ config.public.supportPhone }}</span>
      </p>
    </section>

    <!-- light side -->
    <section class="flex items-center justify-center p-8 lg:p-14 order-1 lg:order-2">
      <form class="w-full max-w-[420px] flex flex-col gap-6" novalidate @submit.prevent="submit">
        <div class="flex flex-col gap-2">
          <h2 class="text-[24px] font-extrabold">خوش آمدید</h2>
          <p class="text-[14px] text-muted">برای ادامه وارد حساب خود شوید.</p>
        </div>

        <div class="grid grid-cols-2 p-1 rounded-[999px]" style="background: var(--color-surface-3)">
          <button
            v-for="option in [{ value: 'CENTER', label: 'مرکز خیریه' }, { value: 'ADMIN', label: 'ادمین' }]"
            :key="option.value"
            type="button"
            class="py-2.5 rounded-[999px] text-[14px] font-semibold transition-colors"
            :class="tab === option.value ? 'bg-surface text-ink shadow-sm' : 'text-muted'"
            @click="tab = option.value as 'CENTER' | 'ADMIN'"
          >
            {{ option.label }}
          </button>
        </div>

        <UiField
          v-model="username"
          label="نام کاربری"
          ltr
          required
          autocomplete="username"
          :error="errorMessage && !password ? '' : undefined"
        />

        <UiField
          v-model="password"
          label="رمز عبور"
          type="password"
          revealable
          required
          :error="errorMessage || undefined"
          :hint="errorMessage ? undefined : 'پس از ۵ تلاش ناموفق، حساب موقتاً قفل می‌شود.'"
        />

        <label class="flex items-center gap-3 text-[14px] cursor-pointer">
          <input v-model="remember" type="checkbox" class="w-[17px] h-[17px] rounded-[5px] accent-[var(--color-accent)]">
          مرا به خاطر بسپار
        </label>

        <button type="submit" class="btn btn-primary w-full text-[16px] font-bold" :disabled="submitting">
          {{ submitting ? 'در حال ورود…' : 'ورود به پنل' }}
        </button>

        <div class="flex items-center gap-3 text-[13px] text-muted-2">
          <span class="h-px flex-1" style="background: var(--color-line-soft)" />
          یا
          <span class="h-px flex-1" style="background: var(--color-line-soft)" />
        </div>

        <p class="text-[13px] leading-7 text-muted text-center">
          حساب مراکز فقط توسط ادمین ایجاد می‌شود.
          <NuxtLink to="/contact" class="text-accent hover:text-accent-600">درخواست ایجاد حساب</NuxtLink>
        </p>

        <NuxtLink to="/" class="text-[13px] text-muted hover:text-accent text-center">
          ← بازگشت به صفحه اصلی
        </NuxtLink>
      </form>
    </section>
  </div>
</template>
