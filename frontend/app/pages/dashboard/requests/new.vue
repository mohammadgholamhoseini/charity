<script setup lang="ts">
import { ep } from '~/api/endpoints'
import type { CenterResponse } from '~/types/api'

definePageMeta({ layout: 'dashboard', middleware: 'auth', role: 'CENTER' })

const { $api } = useNuxtApp()
const toast = useToast()
const router = useRouter()

const center = ref<CenterResponse | null>(null)
const submitting = ref(false)

onMounted(async () => {
  try {
    center.value = await $api<CenterResponse>(ep.centerMe)
  }
  catch (error) {
    toast.error(apiErrorMessage(error))
  }
})

async function submit(payload: Record<string, unknown>, publish: boolean) {
  submitting.value = true
  try {
    await $api(ep.centerRequests, { method: 'POST', body: { ...payload, submit: publish } })
    toast.success(publish ? 'درخواست منتشر شد.' : 'پیش‌نویس ذخیره شد.')
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
      درخواست بلافاصله پس از ثبت در سایت منتشر می‌شود و نیازی به تأیید ادمین ندارد. مسئولیت
      صحت اطلاعات بر عهده مرکز ثبت‌کننده است.
    </div>

    <RequestForm
      :allowed-categories="center?.categories ?? []"
      :submitting="submitting"
      @submit="submit"
    />
  </div>
</template>
