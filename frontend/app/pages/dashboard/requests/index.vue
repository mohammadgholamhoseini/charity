<script setup lang="ts">
import { ep } from '~/api/endpoints'
import type { Page, RequestStatus, RequestSummary } from '~/types/api'

definePageMeta({ layout: 'dashboard', middleware: 'auth', role: 'CENTER' })

const { $api } = useNuxtApp()
const { shortDate } = useFormat()
const toast = useToast()

// No «در انتظار انتشار» or «رد شده» tab: a centre publishes its own requests, so neither
// status can be reached any more.
const tabs: { value: RequestStatus | '', label: string }[] = [
  { value: '', label: 'همه' },
  { value: 'DRAFT', label: 'پیش‌نویس' },
  { value: 'PUBLISHED', label: 'منتشرشده' },
  { value: 'COMPLETED', label: 'تکمیل‌شده' },
  { value: 'INACTIVE', label: 'غیرفعال' },
]

const activeTab = ref<RequestStatus | ''>('')
const rows = ref<RequestSummary[]>([])
const loading = ref(true)
const loadError = ref(false)
const deleteTarget = ref<RequestSummary | null>(null)
const announceTarget = ref<RequestSummary | null>(null)
const busy = ref(false)

const statusTarget = ref<RequestSummary | null>(null)
const nextStatus = ref<RequestStatus>('PUBLISHED')
const note = ref('')
const noteRequired = computed(() => nextStatus.value === 'INACTIVE')

/**
 * Mirrors RequestStatusPolicy.CENTER_ALLOWED on the server. The server is still the authority —
 * this exists so the dialog does not offer a move that would come back as a 409.
 *
 * COMPLETED is empty on purpose: a completed request is the record that its need was met, and a
 * centre republishing one would put a listing back in front of donors for something already paid
 * for. Only an admin can move it, and only to «غیرفعال». Same reason the delete button is gone
 * from those rows.
 */
const CENTER_TRANSITIONS: Record<string, RequestStatus[]> = {
  DRAFT: ['PUBLISHED', 'INACTIVE'],
  PUBLISHED: ['COMPLETED', 'INACTIVE'],
  COMPLETED: [],
  INACTIVE: ['PUBLISHED'],
}

const STATUS_LABELS: Record<string, { label: string, description: string }> = {
  PUBLISHED: { label: 'منتشرشده', description: 'در فهرست عمومی و صفحه دسته نمایش داده می‌شود.' },
  COMPLETED: { label: 'تکمیل‌شده', description: 'کمک دریافت شده؛ از فهرست فعال خارج می‌شود اما نشانی آن باقی می‌ماند.' },
  INACTIVE: { label: 'غیرفعال', description: 'موقتاً از سایت برداشته می‌شود؛ بعداً می‌توانید دوباره منتشرش کنید.' },
}

const statusOptions = computed(() =>
  (CENTER_TRANSITIONS[statusTarget.value?.status ?? ''] ?? [])
    .map(value => ({ value, ...STATUS_LABELS[value]! })))

function openStatusChange(request: RequestSummary) {
  statusTarget.value = request
  nextStatus.value = statusOptions.value[0]?.value ?? request.status
  note.value = ''
}

async function saveStatus() {
  if (!statusTarget.value) return
  if (noteRequired.value && !note.value.trim()) {
    toast.error('برای غیرفعال کردن درخواست، ثبت دلیل الزامی است.')
    return
  }
  busy.value = true
  try {
    await $api(ep.centerRequestStatus(statusTarget.value.id), {
      method: 'POST',
      body: { status: nextStatus.value, note: note.value.trim() || null },
    })
    toast.success('وضعیت درخواست تغییر کرد.')
    statusTarget.value = null
    load()
  }
  catch (error) {
    toast.error(apiErrorMessage(error))
  }
  finally {
    busy.value = false
  }
}

async function load() {
  loading.value = true
  loadError.value = false
  try {
    const data = await $api<Page<RequestSummary>>(ep.centerRequests, {
      query: { size: 50, sort: 'newest', ...(activeTab.value ? { status: activeTab.value } : {}) },
    })
    rows.value = data.content
  }
  catch {
    loadError.value = true
  }
  finally {
    loading.value = false
  }
}

onMounted(load)
watch(activeTab, load)

async function publish(request: RequestSummary) {
  try {
    await $api(ep.centerRequestSubmit(request.id), { method: 'POST' })
    toast.success('درخواست منتشر شد.')
    load()
  }
  catch (error) {
    toast.error(apiErrorMessage(error))
  }
}

/**
 * Sends a request to the channel by hand.
 *
 * The automatic announcement fires once, when the request is first published, so one bad moment
 * on the bot API leaves it unannounced for good. This is the only way back.
 */
async function confirmAnnounce() {
  if (!announceTarget.value) return
  busy.value = true
  try {
    await $api(ep.centerRequestAnnounce(announceTarget.value.id), { method: 'POST' })
    toast.success('درخواست در کانال اعلام شد.')
    announceTarget.value = null
    load()
  }
  catch (error) {
    toast.error(apiErrorMessage(error))
  }
  finally {
    busy.value = false
  }
}

async function confirmDelete() {
  if (!deleteTarget.value) return
  busy.value = true
  try {
    await $api(ep.centerRequest(deleteTarget.value.id), { method: 'DELETE' })
    toast.success('درخواست حذف شد.')
    deleteTarget.value = null
    load()
  }
  catch (error) {
    toast.error(apiErrorMessage(error))
  }
  finally {
    busy.value = false
  }
}

useHead({ title: 'درخواست‌های من — یاری‌جو' })
</script>

<template>
  <div class="flex flex-col gap-6">
    <div class="flex flex-wrap items-center justify-between gap-4">
      <h1 class="text-[24px] font-extrabold">درخواست‌های من</h1>
      <NuxtLink to="/dashboard/requests/new" class="btn btn-primary btn-sm">ثبت درخواست جدید</NuxtLink>
    </div>

    <div class="flex flex-wrap gap-2">
      <button
        v-for="tab in tabs"
        :key="tab.value"
        type="button"
        class="chip transition-colors"
        :class="activeTab === tab.value ? 'chip-dark' : 'chip-neutral'"
        @click="activeTab = tab.value"
      >
        {{ tab.label }}
      </button>
    </div>

    <UiErrorState v-if="loadError" @retry="load()" />

    <div v-else-if="loading" class="card-flat p-6 flex flex-col gap-2">
      <UiSkeleton v-for="n in 4" :key="n" variant="row" />
    </div>

    <UiEmptyState
      v-else-if="!rows.length"
      title="درخواستی در این وضعیت نیست"
      description="با تغییر تب بالا می‌توانید درخواست‌های دیگر را ببینید."
    />

    <section v-else class="card-flat overflow-x-auto">
      <table class="data-table">
        <thead>
          <tr>
            <th>عنوان درخواست</th>
            <th>دسته</th>
            <th>تاریخ ثبت</th>
            <th>وضعیت</th>
            <th>عملیات</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="request in rows" :key="request.id">
            <td>
              <div class="flex flex-col gap-1">
                <span class="font-semibold">{{ request.title }}</span>
                <!-- The full reason lives on the detail page; here it is enough to say why the
                     status controls are missing for this row. -->
                <span v-if="request.lockedByAdmin" class="text-[12px] text-danger">
                  توسط ادمین غیرفعال شده است
                </span>
              </div>
            </td>
            <td>{{ request.category?.name ?? '—' }}</td>
            <td class="ltr">{{ shortDate(request.createdAt) }}</td>
            <td><UiChip :label="request.statusLabel" :status="request.status" /></td>
            <td>
              <div class="flex items-center gap-3 text-[13px]">
                <NuxtLink :to="`/dashboard/requests/${request.id}`" class="text-accent hover:text-accent-600">
                  ویرایش
                </NuxtLink>
                <button
                  v-if="request.status === 'DRAFT'"
                  type="button"
                  class="text-accent hover:text-accent-600"
                  @click="publish(request)"
                >
                  انتشار
                </button>
                <!-- Only shows on a live request the channel does not already carry. -->
                <button
                  v-if="request.status === 'PUBLISHED' && !request.announced"
                  type="button"
                  class="text-accent hover:text-accent-600"
                  @click="announceTarget = request"
                >
                  انتشار در کانال
                </button>
                <!--
                  A completed request is frozen: no status change, no delete. It records that the
                  need was met, and reopening or erasing it is not the centre's to do.
                -->
                <button
                  v-if="!request.lockedByAdmin && request.status !== 'COMPLETED'"
                  type="button"
                  class="text-accent hover:text-accent-600"
                  @click="openStatusChange(request)"
                >
                  تغییر وضعیت
                </button>
                <button
                  v-if="request.status !== 'COMPLETED'"
                  type="button"
                  class="text-danger hover:underline"
                  @click="deleteTarget = request"
                >
                  حذف
                </button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </section>

    <UiModal
      :open="Boolean(statusTarget)"
      title="تغییر وضعیت درخواست"
      size="md"
      @close="statusTarget = null"
    >
      <div v-if="statusTarget" class="flex flex-col gap-5">
        <p class="text-[14px] text-muted">
          {{ statusTarget.title }} · <span class="ltr">{{ statusTarget.code }}</span>
        </p>

        <fieldset class="flex flex-col gap-3">
          <legend class="label">وضعیت جدید</legend>
          <label
            v-for="option in statusOptions"
            :key="option.value"
            class="flex items-start gap-3 p-4 rounded-[12px] border cursor-pointer transition-colors"
            :class="nextStatus === option.value ? 'border-accent bg-accent-50' : 'border-line-soft'"
          >
            <input v-model="nextStatus" type="radio" :value="option.value" class="mt-1.5 accent-[var(--color-accent)]">
            <span class="flex flex-col gap-1">
              <span class="text-[14px] font-bold">{{ option.label }}</span>
              <span class="help">{{ option.description }}</span>
            </span>
          </label>
        </fieldset>

        <UiField
          v-model="note"
          label="یادداشت"
          textarea
          :rows="3"
          :required="noteRequired"
          :maxlength="1000"
          :hint="noteRequired
            ? 'برای غیرفعال کردن درخواست، ثبت دلیل الزامی است.'
            : 'اختیاری — فقط برای شما و ادمین قابل مشاهده است.'"
        />

        <div class="flex items-center gap-3">
          <button type="button" class="btn btn-primary" :disabled="busy" @click="saveStatus">
            {{ busy ? 'در حال ذخیره…' : 'ثبت تغییر وضعیت' }}
          </button>
          <button type="button" class="btn btn-secondary" @click="statusTarget = null">انصراف</button>
        </div>
      </div>
    </UiModal>

    <UiConfirm
      :open="Boolean(announceTarget)"
      title="انتشار در کانال"
      :message="`«${announceTarget?.title}» در کانال بله اعلام می‌شود. پیام پس از ارسال قابل بازگشت نیست.`"
      confirm-label="ارسال کن"
      :busy="busy"
      @confirm="confirmAnnounce"
      @close="announceTarget = null"
    />

    <UiConfirm
      :open="Boolean(deleteTarget)"
      title="حذف درخواست"
      :message="`آیا از حذف «${deleteTarget?.title}» مطمئن هستید؟ این درخواست از سایت برداشته می‌شود.`"
      confirm-label="حذف کن"
      tone="danger"
      :busy="busy"
      @confirm="confirmDelete"
      @close="deleteTarget = null"
    />
  </div>
</template>
