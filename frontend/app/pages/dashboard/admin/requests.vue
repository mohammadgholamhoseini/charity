<script setup lang="ts">
import { ep } from '~/api/endpoints'
import type { CategoryResponse, CenterResponse, Page, RequestStatus, RequestSummary, Urgency } from '~/types/api'

definePageMeta({ layout: 'dashboard', middleware: 'auth', role: 'ADMIN' })

const { $api } = useNuxtApp()
const { shortDate, num } = useFormat()
const toast = useToast()

const tabs: { value: RequestStatus | '', label: string }[] = [
  { value: '', label: 'همه' },
  { value: 'PUBLISHED', label: 'منتشرشده' },
  { value: 'DRAFT', label: 'پیش‌نویس' },
  { value: 'COMPLETED', label: 'تکمیل‌شده' },
  { value: 'INACTIVE', label: 'غیرفعال' },
]

/**
 * Every status an admin can put a request into. Approving is not among them any more: centres
 * publish their own requests, so what is left is taking one down, putting it back, or marking it
 * met. This list also feeds the stat cards and the legend at the foot of the page, which is why it
 * is flat rather than keyed by the row's current status — for that, see ADMIN_TRANSITIONS.
 */
const statusOptions: { value: RequestStatus, label: string, description: string }[] = [
  { value: 'PUBLISHED', label: 'منتشرشده', description: 'در فهرست عمومی و صفحه دسته نمایش داده می‌شود.' },
  { value: 'COMPLETED', label: 'تکمیل‌شده', description: 'کمک دریافت شده؛ از فهرست فعال خارج می‌شود اما نشانی آن باقی می‌ماند.' },
  { value: 'INACTIVE', label: 'غیرفعال', description: 'خروج از فهرست بدون حذف؛ ثبت دلیل اجباری است.' },
]

/**
 * Mirrors RequestStatusPolicy.ADMIN_ALLOWED on the server, the same way the centre panel mirrors
 * CENTER_ALLOWED. The dialog used to offer all three statuses regardless of the row it was opened
 * on, so it could propose a move the server answers with a 409.
 *
 * COMPLETED leads only to «غیرفعال», and that is deliberate: a completed request cannot be
 * reopened by anyone, but one that turns out to be fraudulent or wrong still has to be removable
 * from the site. It is the only way out, and only an admin has it.
 */
const ADMIN_TRANSITIONS: Record<string, RequestStatus[]> = {
  DRAFT: ['PUBLISHED', 'INACTIVE'],
  PUBLISHED: ['COMPLETED', 'INACTIVE'],
  COMPLETED: ['INACTIVE'],
  INACTIVE: ['PUBLISHED'],
}

const activeTab = ref<RequestStatus | ''>('')
const categoryFilter = ref<number | ''>('')
const centerFilter = ref<number | ''>('')
const urgencyFilter = ref<Urgency | ''>('')

const rows = ref<RequestSummary[]>([])
const stats = ref<Record<RequestStatus, number> | null>(null)
const categories = ref<CategoryResponse[]>([])
const centers = ref<CenterResponse[]>([])
const loading = ref(true)
const loadError = ref(false)

const selected = ref<RequestSummary | null>(null)
const nextStatus = ref<RequestStatus>('PUBLISHED')
const note = ref('')
const saving = ref(false)
const deleteTarget = ref<RequestSummary | null>(null)
const announceTarget = ref<RequestSummary | null>(null)

const noteRequired = computed(() => nextStatus.value === 'INACTIVE')

/** The statuses the open row may actually move to, in the order statusOptions declares them. */
const modalOptions = computed(() => {
  const allowed = ADMIN_TRANSITIONS[selected.value?.status ?? ''] ?? []
  return statusOptions.filter(option => allowed.includes(option.value))
})

async function load() {
  loading.value = true
  loadError.value = false
  try {
    const [data, statsData] = await Promise.all([
      $api<Page<RequestSummary>>(ep.adminRequests, {
        query: {
          size: 50,
          sort: 'newest',
          ...(activeTab.value ? { status: activeTab.value } : {}),
          ...(categoryFilter.value ? { categoryId: categoryFilter.value } : {}),
          ...(centerFilter.value ? { centerId: centerFilter.value } : {}),
          ...(urgencyFilter.value ? { urgency: urgencyFilter.value } : {}),
        },
      }),
      $api<Record<RequestStatus, number>>(ep.adminRequestStats),
    ])
    rows.value = data.content
    stats.value = statsData
  }
  catch {
    loadError.value = true
  }
  finally {
    loading.value = false
  }
}

onMounted(async () => {
  await load()
  try {
    const [categoryData, centerData] = await Promise.all([
      $api<CategoryResponse[]>(ep.adminCategories),
      $api<Page<CenterResponse>>(ep.adminCenters, { query: { size: 100 } }),
    ])
    categories.value = categoryData
    centers.value = centerData.content
  }
  catch { /* filters degrade to "all"; the table itself still works */ }
})

watch([activeTab, categoryFilter, centerFilter, urgencyFilter], load)

function openStatusChange(request: RequestSummary) {
  selected.value = request
  // Not the current status: it is no longer among the choices for a completed request, and a
  // radio group with nothing selected is worse than one defaulted to the only move available.
  nextStatus.value = modalOptions.value[0]?.value ?? request.status
  note.value = ''
}

async function saveStatus() {
  if (!selected.value) return
  if (noteRequired.value && !note.value.trim()) {
    toast.error('برای غیرفعال کردن درخواست، ثبت دلیل الزامی است.')
    return
  }
  saving.value = true
  try {
    await $api(ep.adminRequestStatus(selected.value.id), {
      method: 'POST',
      body: { status: nextStatus.value, note: note.value.trim() || null },
    })
    toast.success('وضعیت درخواست تغییر کرد.')
    selected.value = null
    load()
  }
  catch (error) {
    toast.error(apiErrorMessage(error))
  }
  finally {
    saving.value = false
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
  saving.value = true
  try {
    await $api(ep.adminRequestAnnounce(announceTarget.value.id), { method: 'POST' })
    toast.success('درخواست در کانال اعلام شد.')
    announceTarget.value = null
    load()
  }
  catch (error) {
    toast.error(apiErrorMessage(error))
  }
  finally {
    saving.value = false
  }
}

async function confirmDelete() {
  if (!deleteTarget.value) return
  saving.value = true
  try {
    await $api(ep.adminRequest(deleteTarget.value.id), { method: 'DELETE' })
    toast.success('درخواست حذف شد.')
    deleteTarget.value = null
    load()
  }
  catch (error) {
    toast.error(apiErrorMessage(error))
  }
  finally {
    saving.value = false
  }
}

useHead({ title: 'درخواست‌ها — پنل ادمین' })
</script>

<template>
  <div class="flex flex-col gap-6">
    <div class="flex flex-wrap items-center justify-between gap-4">
      <div class="flex flex-col gap-1">
        <h1 class="text-[24px] font-extrabold">درخواست‌ها</h1>
        <p class="text-[14px] text-muted">تغییر وضعیت، ویرایش و حذف درخواست‌های همه مراکز</p>
      </div>
    </div>

    <!-- one card per status -->
    <div class="grid gap-4 grid-cols-2 lg:grid-cols-5">
      <button
        v-for="option in statusOptions"
        :key="option.value"
        type="button"
        class="text-start"
        @click="activeTab = activeTab === option.value ? '' : option.value"
      >
        <UiStatCard
          :value="stats?.[option.value] ?? 0"
          :label="option.label"
          :active="activeTab === option.value"
        />
      </button>
    </div>

    <div class="flex flex-wrap gap-2">
      <button
        v-for="tab in tabs"
        :key="tab.value"
        type="button"
        class="chip transition-colors"
        :class="activeTab === tab.value ? 'chip-dark' : 'chip-neutral'"
        @click="activeTab = tab.value"
      >{{ tab.label }}</button>
    </div>

    <div class="flex flex-wrap gap-3">
      <select v-model="categoryFilter" class="field w-auto min-w-[160px]">
        <option value="">همه دسته‌ها</option>
        <option v-for="category in categories" :key="category.id" :value="category.id">
          {{ category.name }}
        </option>
      </select>
      <select v-model="centerFilter" class="field w-auto min-w-[160px]">
        <option value="">همه مراکز</option>
        <option v-for="center in centers" :key="center.id" :value="center.id">{{ center.name }}</option>
      </select>
      <select v-model="urgencyFilter" class="field w-auto min-w-[160px]">
        <option value="">هر سطح فوریت</option>
        <option value="URGENT">فوری</option>
        <option value="HIGH">بالا</option>
        <option value="MEDIUM">متوسط</option>
        <option value="LOW">کم</option>
      </select>
    </div>

    <UiErrorState v-if="loadError" @retry="load()" />

    <div v-else-if="loading" class="card-flat p-6 flex flex-col gap-2">
      <UiSkeleton v-for="n in 5" :key="n" variant="row" />
    </div>

    <UiEmptyState v-else-if="!rows.length" title="درخواستی با این فیلترها یافت نشد" />

    <section v-else class="card-flat overflow-x-auto">
      <table class="data-table">
        <thead>
          <tr>
            <th>عنوان درخواست</th>
            <th>مرکز</th>
            <th>دسته</th>
            <th>تاریخ ثبت</th>
            <th>وضعیت</th>
            <th>عملیات</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="request in rows" :key="request.id" :class="{ 'is-selected': selected?.id === request.id }">
            <td>
              <div class="flex items-center gap-2 flex-wrap">
                <span class="font-semibold">{{ request.title }}</span>
                <UiChip :label="request.urgencyLabel" :urgency="request.urgency" />
              </div>
            </td>
            <td>{{ request.center?.name ?? '—' }}</td>
            <td>{{ request.category?.name ?? '—' }}</td>
            <td class="ltr">{{ shortDate(request.createdAt) }}</td>
            <td>
              <button type="button" class="inline-flex" @click="openStatusChange(request)">
                <UiChip :label="`${request.statusLabel} ↓`" :status="request.status" />
              </button>
            </td>
            <td>
              <div class="flex items-center gap-3 text-[13px]">
                <button type="button" class="text-accent hover:text-accent-600" @click="openStatusChange(request)">
                  تغییر وضعیت
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
                <NuxtLink
                  :to="`/requests/${encodeURIComponent(request.slug)}`"
                  target="_blank"
                  class="text-accent hover:text-accent-600"
                >مشاهده</NuxtLink>
                <button type="button" class="text-danger hover:underline" @click="deleteTarget = request">
                  حذف
                </button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </section>

    <!-- status rules, spelled out with the enum codes -->
    <section class="dark-panel rounded-[20px] p-6 flex flex-col gap-3">
      <h2 class="text-[17px] font-bold text-onink">قاعده وضعیت‌ها</h2>
      <ul class="flex flex-col gap-2 text-[14px] text-onink-2 leading-8">
        <li v-for="option in statusOptions" :key="option.value" class="flex flex-wrap items-baseline gap-2">
          <!-- The label, not the enum name: this legend is read by admins, not developers. -->
          <strong class="text-accent-2">{{ option.label }}</strong>
          <span>— {{ option.description }}</span>
        </li>
      </ul>
    </section>

    <!-- status change -->
    <UiModal
      :open="Boolean(selected)"
      :title="`تغییر وضعیت درخواست`"
      size="md"
      @close="selected = null"
    >
      <div v-if="selected" class="flex flex-col gap-5">
        <p class="text-[14px] text-muted">
          {{ selected.title }} · {{ selected.center?.name }} ·
          <span class="ltr">{{ selected.code }}</span>
        </p>

        <fieldset class="flex flex-col gap-3">
          <legend class="label">وضعیت جدید</legend>
          <label
            v-for="option in modalOptions"
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
            ? 'برای غیرفعال کردن درخواست، ثبت دلیل اجباری است و به مرکز نمایش داده می‌شود.'
            : 'اختیاری — برای مرکز ثبت‌کننده قابل مشاهده است.'"
        />

        <div class="flex items-center gap-3">
          <button type="button" class="btn btn-primary" :disabled="saving" @click="saveStatus">
            {{ saving ? 'در حال ذخیره…' : 'ثبت تغییر وضعیت' }}
          </button>
          <button type="button" class="btn btn-secondary" @click="selected = null">انصراف</button>
        </div>
      </div>
    </UiModal>

    <UiConfirm
      :open="Boolean(announceTarget)"
      title="انتشار در کانال"
      :message="`«${announceTarget?.title}» در کانال بله اعلام می‌شود. پیام پس از ارسال قابل بازگشت نیست.`"
      confirm-label="ارسال کن"
      :busy="saving"
      @confirm="confirmAnnounce"
      @close="announceTarget = null"
    />

    <UiConfirm
      :open="Boolean(deleteTarget)"
      title="حذف درخواست"
      :message="`«${deleteTarget?.title}» از سایت برداشته می‌شود. نشانی آن به‌جای ۴۰۴، پاسخ ۴۱۰ خواهد داد.`"
      confirm-label="حذف کن"
      tone="danger"
      :busy="saving"
      @confirm="confirmDelete"
      @close="deleteTarget = null"
    />
  </div>
</template>
