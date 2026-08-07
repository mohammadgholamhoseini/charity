<script setup>
import { ref } from 'vue'
import { useRouter, useRoute, RouterLink } from 'vue-router'
import api from '../api/client'
import { useAuthStore } from '../stores/auth'
import { User, Lock, Eye, EyeOff, LogIn, ArrowRight } from '@lucide/vue'
import BrandLogo from '../components/BrandLogo.vue'

const auth = useAuthStore()
const router = useRouter()
const route = useRoute()

const form = ref({ username: '', password: '' })
const error = ref('')
const loading = ref(false)
const showPass = ref(false)

async function submit() {
  error.value = ''
  loading.value = true
  try {
    const res = await api.post('/auth/login', form.value)
    const { token, role, userId, fullName } = res.data
    auth.setSession(token, { username: res.data.username, role, id: userId, fullName })
    const redirect = route.query.redirect || (role === 'ADMIN' ? '/dashboard/admin/cases' : '/dashboard')
    router.push(redirect)
  } catch (e) {
    const msg = e.response?.data?.message || e.message || 'خطای ناشناخته'
    error.value = 'نام کاربری یا رمز عبور اشتباه است. (' + msg + ')'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="min-h-screen flex items-center justify-center px-4 relative overflow-hidden bg-gradient-to-br from-brand-50 via-surface to-brand-100 dark:from-brand-950 dark:via-[#0a152e] dark:to-brand-950">
    <div class="absolute -top-24 -right-20 w-96 h-96 bg-brand-400/20 rounded-full blur-3xl"></div>
    <div class="absolute -bottom-24 -left-20 w-80 h-80 bg-accent-500/15 rounded-full blur-3xl"></div>

    <div class="w-full max-w-md card p-8 relative animate-fade-up">
      <div class="text-center mb-7">
        <div class="inline-flex justify-center mb-4">
          <BrandLogo full dark :mark="64" tagline="سامانهٔ کمک‌رسانی" />
        </div>
        <h1 class="text-2xl font-extrabold text-slate-800 dark:text-white">ورود به پنل</h1>
        <p class="text-slate-400 text-sm mt-1">برای مراکز خیریه و ادمین</p>
      </div>

      <form @submit.prevent="submit" class="space-y-4">
        <div>
          <label class="label">نام کاربری</label>
          <div class="relative">
            <User :size="18" class="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400" />
            <input v-model="form.username" required placeholder="نام کاربری"
              class="input pr-10" />
          </div>
        </div>

        <div>
          <label class="label">رمز عبور</label>
          <div class="relative">
            <Lock :size="18" class="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400" />
            <input v-model="form.password" :type="showPass ? 'text' : 'password'" required placeholder="رمز عبور"
              class="input pr-10 pl-10" />
            <button type="button" @click="showPass = !showPass"
              class="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600 dark:hover:text-slate-200">
              <Eye v-if="!showPass" :size="18" />
              <EyeOff v-else :size="18" />
            </button>
          </div>
        </div>

        <p v-if="error" class="text-red-500 text-sm text-center bg-red-50 dark:bg-red-900/20 rounded-xl py-2.5">{{ error }}</p>

        <button :disabled="loading" type="submit" class="btn-primary w-full">
          <LogIn v-if="!loading" :size="18" />
          <span v-else class="w-5 h-5 border-2 border-white/40 border-t-white rounded-full animate-spin"></span>
          {{ loading ? 'در حال ورود...' : 'ورود' }}
        </button>
      </form>

      <RouterLink to="/" class="inline-flex items-center gap-1.5 text-xs text-slate-400 mt-5 hover:text-slate-600 dark:hover:text-slate-200">
        <ArrowRight :size="14" /> بازگشت به سایت
      </RouterLink>
    </div>
  </div>
</template>
