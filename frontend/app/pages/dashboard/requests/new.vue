<script setup lang="ts">
import { ep } from '~/api/endpoints'
import type { CenterResponse, CityRef } from '~/types/api'

definePageMeta({ layout: 'dashboard', middleware: 'auth', role: 'CENTER' })

const { $api } = useNuxtApp()
const toast = useToast()
const router = useRouter()

const center = ref<CenterResponse | null>(null)
const cities = ref<CityRef[]>([])
const submitting = ref(false)

onMounted(async () => {
  try {
    const [centerData, cityData] = await Promise.all([
      $api<CenterResponse>(ep.centerMe),
      $api<CityRef[]>(ep.cities),
    ])
    center.value = centerData
    cities.value = cityData
  }
  catch (error) {
    toast.error(apiErrorMessage(error))
  }
})

async function submit(payload: Record<string, unknown>, submitForReview: boolean) {
  submitting.value = true
  try {
    await $api(ep.centerRequests, { method: 'POST', body: { ...payload, submit: submitForReview } })
    toast.success(submitForReview
      ? 'درخواست برای بررسی ادمین ارسال شد.'
      : 'پیش‌نویس ذخیره شد.')
    router.push('/dashboard/requests')
  }
  catch (error) {
    toast.error(apiErrorMessage(error))
  }
  finally {
    submitting.value = false
  }
}

useHead({ title: 'ثبت درخواست جدید — یاری‌جو' })
</script>

<template>
  <div class="flex flex-col gap-6 max-w-3xl">
    <h1 class="text-[24px] font-extrabold">ثبت درخواست جدید</h1>

    <div
      class="text-[13px] leading-7 text-muted p-4 rounded-[14px]"
      style="border: 1px dashed var(--color-line)"
    >
      درخواست پس از ثبت در وضعیت «در انتظار انتشار» قرار می‌گیرد و تا زمان تأیید ادمین در سایت
      نمایش داده نمی‌شود.
    </div>

    <RequestForm
      :allowed-categories="center?.categories ?? []"
      :cities="cities"
      :submitting="submitting"
      @submit="submit"
    />
  </div>
</template>
