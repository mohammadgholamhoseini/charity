<script setup lang="ts">
import { ep } from '~/api/endpoints'
import type { NoticePlacement, NoticeResponse } from '~/types/api'

definePageMeta({ layout: 'dashboard', middleware: 'auth', role: 'ADMIN' })

const { $api } = useNuxtApp()
const { shortDate } = useFormat()
const toast = useToast()

const rows = ref<NoticeResponse[]>([])
const loading = ref(true)
const loadError = ref(false)
const saving = ref(false)

const selected = ref<NoticeResponse | null>(null)
const deleteTarget = ref<NoticeResponse | null>(null)
const filter = ref<NoticePlacement | 'ALL' | 'INACTIVE'>('ALL')

const form = reactive({
  title: '', content: '', placement: 'TOP_BANNER' as NoticePlacement,
  startAt: '', endAt: '', linkUrl: '', active: true,
})

const placements: { value: NoticePlacement, label: string, description: string }[] = [
  { value: 'TOP_BANNER', label: 'بنر بالای صفحه', description: 'نوار تیره بالای هدر، قابل بستن توسط بازدیدکننده.' },
  { value: 'FOOTER', label: 'پاورقی', description: 'یک خط در پاورقی همه صفحات.' },
]

const filtered = computed(() => {
  if (filter.value === 'ALL') return rows.value
  if (filter.value === 'INACTIVE') return rows.value.filter(row => !row.active)
  return rows.value.filter(row => row.placement === filter.value)
})

/** Only the first line shows in the banner, so that is what the preview renders. */
const previewLine = computed(() => form.content.split('\n')[0]?.trim() ?? '')

async function load() {
  loading.value = true
  loadError.value = false
  try { rows.value = await $api<NoticeResponse[]>(ep.adminNotices) }
  catch { loadError.value = true }
  finally { loading.value = false }
}

onMounted(load)

function startCreate() {
  selected.value = null
  Object.assign(form, {
    title: '', content: '', placement: 'TOP_BANNER',
    startAt: '', endAt: '', linkUrl: '', active: true,
  })
}

function startEdit(notice: NoticeResponse) {
  selected.value = notice
  Object.assign(form, {
    title: notice.title,
    content: notice.content,
    placement: notice.placement,
    // datetime-local wants `YYYY-MM-DDTHH:mm`; the API sends an ISO offset string.
    startAt: notice.startAt ? notice.startAt.slice(0, 16) : '',
    endAt: notice.endAt ? notice.endAt.slice(0, 16) : '',
    linkUrl: notice.linkUrl ?? '',
    active: notice.active,
  })
}

async function save(publish: boolean) {
  if (!form.title.trim() || !form.content.trim()) {
    toast.error('عنوان و متن اطلاعیه الزامی است.')
    return
  }
  saving.value = true
  try {
    const body = {
      title: form.title.trim(),
      content: form.content.trim(),
      placement: form.placement,
      startAt: form.startAt || null,
      endAt: form.endAt || null,
      linkUrl: form.linkUrl.trim() || null,
      active: publish ? true : form.active,
    }
    if (selected.value) await $api(ep.adminNotice(selected.value.id), { method: 'PUT', body })
    else await $api(ep.adminNotices, { method: 'POST', body })
    toast.success(publish ? 'اطلاعیه منتشر شد.' : 'اطلاعیه ذخیره شد.')
    startCreate()
    load()
  }
  catch (error) { toast.error(apiErrorMessage(error)) }
  finally { saving.value = false }
}

async function confirmDelete() {
  if (!deleteTarget.value) return
  saving.value = true
  try {
    await $api(ep.adminNotice(deleteTarget.value.id), { method: 'DELETE' })
    toast.success('اطلاعیه حذف شد.')
    deleteTarget.value = null
    load()
  }
  catch (error) { toast.error(apiErrorMessage(error)) }
  finally { saving.value = false }
}

function statusOf(notice: NoticeResponse) {
  if (notice.expired) return { label: 'منقضی', status: 'INACTIVE' }
  if (!notice.active) return { label: 'غیرفعال', status: 'INACTIVE' }
  return { label: 'فعال', status: 'PUBLISHED' }
}

useHead({ title: 'اطلاعیه‌ها — پنل ادمین' })
</script>

<template>
  <div class="flex flex-col gap-6">
    <div class="flex flex-wrap items-center justify-between gap-4">
      <div class="flex flex-col gap-1">
        <h1 class="text-[24px] font-extrabold">اطلاعیه‌ها</h1>
        <p class="text-[14px] text-muted">
          از هر محل نمایش، تنها یک اطلاعیه فعال در سایت نشان داده می‌شود.
        </p>
      </div>
      <button type="button" class="btn btn-primary btn-sm" @click="startCreate">+ اطلاعیه جدید</button>
    </div>

    <div class="flex flex-wrap gap-2">
      <button
        v-for="tab in [
          { value: 'ALL', label: 'همه' },
          { value: 'TOP_BANNER', label: 'بنر بالای صفحه' },
          { value: 'FOOTER', label: 'پاورقی' },
          { value: 'INACTIVE', label: 'غیرفعال' },
        ]"
        :key="tab.value"
        type="button"
        class="chip"
        :class="filter === tab.value ? 'chip-dark' : 'chip-neutral'"
        @click="filter = tab.value as typeof filter"
      >{{ tab.label }}</button>
    </div>

    <div class="grid gap-6 xl:grid-cols-[minmax(0,1fr)_420px] items-start">
      <div class="flex flex-col gap-6">
        <UiErrorState v-if="loadError" @retry="load()" />

        <div v-else-if="loading" class="card-flat p-6 flex flex-col gap-2">
          <UiSkeleton v-for="n in 3" :key="n" variant="row" />
        </div>

        <UiEmptyState v-else-if="!filtered.length" title="اطلاعیه‌ای در این دسته نیست" />

        <section v-else class="card-flat overflow-x-auto">
          <table class="data-table">
            <thead>
              <tr>
                <th>عنوان اطلاعیه</th>
                <th>محل نمایش</th>
                <th>وضعیت</th>
                <th>تاریخ</th>
                <th>عملیات</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="notice in filtered" :key="notice.id" :class="{ 'is-selected': selected?.id === notice.id }">
                <td class="font-semibold">{{ notice.title }}</td>
                <td><UiChip :label="notice.placementLabel" /></td>
                <td>
                  <UiChip :label="statusOf(notice).label" :status="statusOf(notice).status" />
                </td>
                <td class="ltr">{{ shortDate(notice.startAt ?? notice.createdAt) }}</td>
                <td>
                  <div class="flex items-center gap-3 text-[13px]">
                    <button type="button" class="text-accent hover:text-accent-600" @click="startEdit(notice)">
                      ویرایش
                    </button>
                    <button type="button" class="text-danger hover:underline" @click="deleteTarget = notice">
                      حذف
                    </button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </section>

        <!-- live preview of how it lands on the site -->
        <section class="card-flat p-6 flex flex-col gap-4">
          <h2 class="text-[17px] font-bold">پیش‌نمایش در سایت</h2>
          <div class="rounded-[16px] overflow-hidden border border-line-soft">
            <div
              v-if="form.placement === 'TOP_BANNER'"
              class="dark-panel flex items-center justify-between gap-3 px-4 py-3"
            >
              <div class="flex items-center gap-2 min-w-0">
                <span class="chip chip-highlight shrink-0">اطلاعیه</span>
                <span class="text-[13px] truncate">{{ form.title || 'عنوان اطلاعیه' }}</span>
              </div>
              <div class="flex items-center gap-3 shrink-0 text-[12px]">
                <span v-if="form.linkUrl" class="text-accent-2 font-semibold">مشاهده</span>
                <span class="text-onink-3">✕</span>
              </div>
            </div>

            <div class="flex items-center justify-between px-4 py-3 bg-surface border-b border-line-soft">
              <BrandLogo :size="26" :show-tagline="false" static />
              <span class="text-[11px] text-muted">درخواست‌ها · مراکز · درباره</span>
            </div>

            <div class="px-4 py-8 text-center text-[12px] text-muted-2 bg-surface">محتوای صفحه</div>

            <div class="px-4 py-3 bg-surface border-t border-line-soft flex flex-col gap-1">
              <span v-if="form.placement === 'FOOTER'" class="text-[11px] text-muted leading-6">
                <strong>{{ form.title || 'عنوان اطلاعیه' }}:</strong> {{ previewLine || 'متن اطلاعیه' }}
              </span>
              <span class="text-[10px] text-muted-2">© ۱۴۰۵ یاری‌جو</span>
            </div>
          </div>
        </section>
      </div>

      <aside class="card-flat p-6 flex flex-col gap-5">
        <h2 class="text-[17px] font-bold">{{ selected ? 'ویرایش اطلاعیه' : 'اطلاعیه جدید' }}</h2>

        <UiField v-model="form.title" label="عنوان" required :maxlength="90" counter />
        <UiField
          v-model="form.content"
          label="متن اطلاعیه"
          textarea
          :rows="4"
          required
          :maxlength="4000"
          hint="در بنر بالای صفحه فقط خط اول نمایش داده می‌شود."
        />

        <fieldset class="flex flex-col gap-3">
          <legend class="label">محل قرارگیری</legend>
          <label
            v-for="option in placements"
            :key="option.value"
            class="flex items-start gap-3 p-4 rounded-[12px] border cursor-pointer transition-colors"
            :class="form.placement === option.value ? 'border-accent bg-accent-50' : 'border-line-soft'"
          >
            <input v-model="form.placement" type="radio" :value="option.value" class="mt-1.5 accent-[var(--color-accent)]">
            <span class="flex flex-col gap-1">
              <span class="text-[14px] font-bold">{{ option.label }}</span>
              <span class="help">{{ option.description }}</span>
            </span>
          </label>
        </fieldset>

        <div class="grid gap-4 sm:grid-cols-2">
          <UiField v-model="form.startAt" label="شروع نمایش" type="datetime-local" ltr />
          <UiField v-model="form.endAt" label="پایان نمایش" type="datetime-local" ltr />
        </div>

        <UiField v-model="form.linkUrl" label="لینک دکمه (اختیاری)" ltr :maxlength="500" placeholder="https://" />

        <UiSwitch v-model="form.active" label="وضعیت" description="اطلاعیه غیرفعال در سایت نمایش داده نمی‌شود." />

        <div class="flex flex-wrap items-center gap-3 border-t border-surface-3 pt-5">
          <button type="button" class="btn btn-primary" :disabled="saving" @click="save(true)">
            {{ saving ? 'در حال ذخیره…' : 'انتشار اطلاعیه' }}
          </button>
          <button type="button" class="btn btn-secondary" :disabled="saving" @click="save(false)">
            ذخیره پیش‌نویس
          </button>
        </div>
      </aside>
    </div>

    <UiConfirm
      :open="Boolean(deleteTarget)"
      title="حذف اطلاعیه"
      :message="`«${deleteTarget?.title}» حذف می‌شود.`"
      confirm-label="حذف کن"
      tone="danger"
      :busy="saving"
      @confirm="confirmDelete"
      @close="deleteTarget = null"
    />
  </div>
</template>
