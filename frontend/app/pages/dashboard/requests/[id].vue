<script setup lang="ts">
import { ep } from '~/api/endpoints'
import type { CenterResponse, RequestDetail } from '~/types/api'

definePageMeta({ layout: 'dashboard', middleware: 'auth', role: 'CENTER' })

const route = useRoute()
const router = useRouter()
const { $api } = useNuxtApp()
const toast = useToast()

const id = Number(route.params.id)
const request = ref<RequestDetail | null>(null)
const center = ref<CenterResponse | null>(null)
const loading = ref(true)
const submitting = ref(false)

onMounted(async () => {
  try {
    const [requestData, centerData] = await Promise.all([
      $api<RequestDetail>(ep.centerRequest(id)),
      $api<CenterResponse>(ep.centerMe),
    ])
    request.value = requestData
    center.value = centerData
  }
  catch (error) {
    toast.error(apiErrorMessage(error))
  }
  finally {
    loading.value = false
  }
})

async function submit(payload: Record<string, unknown>) {
  submitting.value = true
  try {
    await $api(ep.centerRequest(id), { method: 'PUT', body: payload })
    toast.success('تغییرات ذخیره شد.')
    router.push('/dashboard/requests')
  }
  catch (error) {
    toast.error(apiErrorMessage(error))
  }
  finally {
    submitting.value = false
  }
}

useHead({ title: 'ویرایش درخواست — یاری‌جو' })
</script>

<template>
  <div class="flex flex-col gap-6 max-w-3xl">
    <h1 class="text-[24px] font-extrabold">ویرایش درخواست</h1>

    <div v-if="loading" class="card-flat p-6"><UiSkeleton :lines="6" /></div>

    <template v-else-if="request">
      <div class="flex flex-wrap items-center gap-2">
        <UiChip :label="request.statusLabel" :status="request.status" />
        <span class="text-[13px] text-muted ltr">{{ request.code }}</span>
      </div>

      <div
        v-if="request.statusNote"
        class="p-4 rounded-[14px] text-[14px] leading-7"
        style="background: var(--color-danger-bg); color: var(--color-danger)"
      >
        <strong>دلیل غیرفعال شدن:</strong> {{ request.statusNote }}
      </div>

      <RequestForm
        :allowed-categories="center?.categories ?? []"
        :initial="request"
        :submitting="submitting"
        editing
        @submit="submit"
      />
    </template>

    <UiEmptyState v-else title="درخواست یافت نشد" />
  </div>
</template>
