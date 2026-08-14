<script setup lang="ts">
import { ep } from '~/api/endpoints'

definePageMeta({ layout: 'dashboard', middleware: 'auth', role: 'ADMIN' })

interface Profile {
  id: number
  username: string
  email: string
  fullName: string | null
  lastLoginAt: string | null
}

const { $api } = useNuxtApp()
const { date } = useFormat()
const toast = useToast()

const profile = ref<Profile | null>(null)
const loading = ref(true)
const saving = ref(false)

const form = reactive({ fullName: '', email: '', currentPassword: '', newPassword: '' })

async function load() {
  loading.value = true
  try {
    const data = await $api<Profile>(ep.adminMe)
    profile.value = data
    form.fullName = data.fullName ?? ''
    form.email = data.email
  }
  catch (error) { toast.error(apiErrorMessage(error)) }
  finally { loading.value = false }
}

onMounted(load)

async function save() {
  if (form.newPassword && !form.currentPassword) {
    toast.error('برای تغییر رمز عبور، رمز فعلی را وارد کنید.')
    return
  }
  saving.value = true
  try {
    await $api(ep.adminMe, {
      method: 'PUT',
      body: {
        fullName: form.fullName.trim() || null,
        email: form.email.trim(),
        currentPassword: form.currentPassword || null,
        newPassword: form.newPassword || null,
      },
    })
    form.currentPassword = ''
    form.newPassword = ''
    toast.success('حساب شما به‌روزرسانی شد.')
    load()
  }
  catch (error) { toast.error(apiErrorMessage(error)) }
  finally { saving.value = false }
}

useHead({ title: 'تنظیمات حساب — پنل ادمین' })
</script>

<template>
  <div class="flex flex-col gap-6 max-w-2xl">
    <h1 class="text-[24px] font-extrabold">تنظیمات حساب</h1>

    <div v-if="loading" class="card-flat p-6"><UiSkeleton :lines="4" /></div>

    <form v-else class="card-flat p-6 flex flex-col gap-5" @submit.prevent="save">
      <div class="flex flex-col gap-1">
        <span class="label mb-0">نام کاربری</span>
        <span class="ltr text-[15px] font-semibold">{{ profile?.username }}</span>
        <span v-if="profile?.lastLoginAt" class="help">
          آخرین ورود: {{ date(profile.lastLoginAt) }}
        </span>
      </div>

      <UiField v-model="form.fullName" label="نام و نام خانوادگی" :maxlength="255" />
      <UiField v-model="form.email" label="ایمیل" type="email" ltr />

      <div class="border-t border-surface-3 pt-5 flex flex-col gap-5">
        <h2 class="text-[16px] font-bold">تغییر رمز عبور</h2>
        <UiField v-model="form.currentPassword" label="رمز عبور فعلی" type="password" revealable />
        <UiField
          v-model="form.newPassword"
          label="رمز عبور جدید"
          type="password"
          revealable
          hint="حداقل ۸ نویسه. تغییر رمز، قفل موقت حساب را هم برمی‌دارد."
        />
      </div>

      <div class="border-t border-surface-3 pt-5">
        <button type="submit" class="btn btn-primary" :disabled="saving">
          {{ saving ? 'در حال ذخیره…' : 'ذخیره تغییرات' }}
        </button>
      </div>
    </form>
  </div>
</template>
