<script setup lang="ts">
import { ep } from '~/api/endpoints'
import { uploadDocumentBatches } from '~/composables/useDocuments'
import type { StagedDocumentBatch } from '~/composables/useDocuments'
import type { CenterResponse, RequestDetail } from '~/types/api'

definePageMeta({ layout: 'dashboard', middleware: 'auth', role: 'CENTER' })

const { $api } = useNuxtApp()
const { num } = useFormat()
const toast = useToast()
const router = useRouter()

const center = ref<CenterResponse | null>(null)
const submitting = ref(false)
const progress = reactive({ done: 0, total: 0 })

onMounted(async () => {
  try {
    center.value = await $api<CenterResponse>(ep.centerMe)
  }
  catch (error) {
    toast.error(apiErrorMessage(error))
  }
})

/**
 * The created request exists and is a legitimate record -- documents are never mandatory, so
 * there is nothing to roll back and no orphan to clean up. Send the centre to the edit page,
 * where the uploader lives, rather than losing what was already saved.
 */
function finishOnEditPage(id: number, error: unknown, note: string) {
  toast.error(apiErrorMessage(error))
  toast.info(`${note} درخواست حذف نشده است؛ از همین صفحه می‌توانید آن را کامل کنید.`)
  router.push(`/dashboard/requests/${id}`)
}

async function submit(
  payload: Record<string, unknown>,
  publish: boolean,
  documents: StagedDocumentBatch[] = [],
) {
  const hasDocuments = documents.some(batch => batch.files.length)
  submitting.value = true
  try {
    // The ordering here is load-bearing. `RequestPublishedEvent` fires on create-with-publish
    // and on the first transition to PUBLISHED, and never again on its own -- so a request
    // published before its documents land is announced to Telegram and Bale without them,
    // permanently. With documents staged, create as a draft, upload, then submit.
    const created = await $api<RequestDetail>(ep.centerRequests, {
      method: 'POST',
      body: { ...payload, submit: hasDocuments ? false : publish },
    })

    if (hasDocuments) {
      try {
        await uploadDocumentBatches(
          $api,
          ep.centerRequestDocuments(created.id),
          documents,
          (done, total) => { progress.done = done; progress.total = total },
        )
      }
      catch (error) {
        return finishOnEditPage(created.id, error, 'درخواست به‌صورت پیش‌نویس ثبت شد، اما بارگذاری مدارک کامل نشد.')
      }

      if (publish) {
        try {
          await $api(ep.centerRequestSubmit(created.id), { method: 'POST' })
        }
        catch (error) {
          return finishOnEditPage(created.id, error, 'مدارک بارگذاری شد، اما انتشار درخواست انجام نشد.')
        }
      }
    }

    toast.success(publish ? 'درخواست منتشر شد.' : 'پیش‌نویس ذخیره شد.')
    router.push('/dashboard/requests')
  }
  catch (error) {
    toast.error(apiErrorMessage(error))
  }
  finally {
    submitting.value = false
    progress.done = 0
    progress.total = 0
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
      مدارکی که پیوست می‌کنید نیز همراه درخواست به‌صورت عمومی منتشر می‌شود؛ ابتدا درخواست ثبت
      و سپس مدارک بارگذاری می‌شود.
    </div>

    <p v-if="submitting && progress.total > 1" class="help">
      در حال بارگذاری مدارک — دسته {{ num(progress.done + 1) }} از {{ num(progress.total) }}
    </p>

    <RequestForm
      :allowed-categories="center?.categories ?? []"
      :submitting="submitting"
      @submit="submit"
    />
  </div>
</template>
