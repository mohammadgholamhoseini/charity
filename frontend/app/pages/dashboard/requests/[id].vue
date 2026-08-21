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

/**
 * Uploading is allowed in every status this page can show -- including COMPLETED, and including
 * a request an admin has taken down: supplying the documents an admin asked for is how a centre
 * answers a takedown. Deleting is the one thing a takedown blocks, and the API answers 409, so
 * the buttons render disabled with the reason rather than failing on click.
 */
const documentDeleteEndpoint = (documentId: number) => ep.centerRequestDocument(id, documentId)

const documentsLockedReason = computed(() =>
  (request.value?.lockedByAdmin
    ? 'این درخواست توسط ادمین غیرفعال شده است؛ تا زمان رفع محدودیت حذف مدرک ممکن نیست. بارگذاری مدرک تازه همچنان انجام می‌شود.'
    : null))

function onDocumentRemoved(documentId: number) {
  if (!request.value) return
  request.value.documents = (request.value.documents ?? []).filter(doc => doc.id !== documentId)
}

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
        <!-- Says why the status controls are absent. Without this the centre sees a deactivated
             request it cannot restore and has nothing telling it that is deliberate. -->
        <p v-if="request.lockedByAdmin" class="mt-1">
          این درخواست توسط ادمین غیرفعال شده است؛ برای انتشار دوباره با ادمین تماس بگیرید.
        </p>
      </div>

      <RequestForm
        :allowed-categories="center?.categories ?? []"
        :initial="request"
        :submitting="submitting"
        editing
        @submit="submit"
      />

      <!-- Outside the form on purpose: the form is unusable for a COMPLETED request and for one
           under an admin takedown, and both of those still accept documents. -->
      <section class="card-flat p-6 lg:p-8">
        <DocumentUploader
          scope="REQUEST"
          heading="مدارک درخواست"
          description="مدارک این درخواست در صفحه عمومی آن نمایش داده می‌شود و همراه اعلان کانال‌ها ارسال می‌شود."
          :documents="request.documents ?? []"
          :endpoint="ep.centerRequestDocuments(id)"
          :delete-endpoint="documentDeleteEndpoint"
          :readonly-reason="documentsLockedReason"
          @uploaded="request = $event as RequestDetail"
          @removed="onDocumentRemoved"
        />
      </section>
    </template>

    <UiEmptyState v-else title="درخواست یافت نشد" />
  </div>
</template>
