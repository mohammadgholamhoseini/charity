<script setup lang="ts">
import { Paperclip, Trash2, X } from 'lucide-vue-next'
import { ep } from '~/api/endpoints'
// Imported by name rather than left to auto-import: these are read from the template as well
// as from the script, and an explicit import is one less thing to be surprised by.
import {
  DOCUMENT_ACCEPT,
  DOCUMENT_ACCEPT_LABEL,
  DOCUMENT_BATCH_SIZE,
  DOCUMENT_MAX_PER_OWNER,
  documentExtension,
  documentLabel,
  uploadDocumentBatches,
  validateDocumentSelection,
} from '~/composables/useDocuments'
import type { StagedDocumentBatch } from '~/composables/useDocuments'
import type { DocumentCategoryResponse, DocumentFile, DocumentScope } from '~/types/api'

/**
 * The one upload-and-manage widget, used in all four panel places: a centre's request edit
 * page, a centre's own profile, the admin centre form, and the two creation forms.
 *
 * It has two modes, and the difference is only whether an owner exists yet:
 *
 *  - **upload mode** (`endpoint` given) posts each pick straight away and emits the record the
 *    API sends back;
 *  - **staging mode** (`endpoint` null) keeps the files in memory and emits them through
 *    `update:staged`, because a request or centre that does not exist yet has nothing to attach
 *    a document to. The parent posts them once it has an id.
 *
 * Two things here are load-bearing and both have bitten this codebase before: the body is a
 * `FormData` with no hand-set Content-Type (setting it omits the multipart boundary and the
 * upload fails), and the input is cleared after every attempt so picking the same file twice
 * still fires `change`.
 */
const props = withDefaults(defineProps<{
  scope: DocumentScope
  documents?: DocumentFile[]
  /** POST target. Leave null to stage locally instead. */
  endpoint?: string | null
  deleteEndpoint?: ((id: number) => string) | null
  disabled?: boolean
  /**
   * Set when the API will refuse a delete -- a request under an admin takedown answers 409.
   * The buttons render disabled with this sentence rather than failing on click.
   */
  readonlyReason?: string | null
  heading?: string
  description?: string
}>(), {
  documents: () => [],
  endpoint: null,
  deleteEndpoint: null,
  disabled: false,
  readonlyReason: null,
  heading: 'مدارک',
  description: '',
})

const emit = defineEmits<{
  /** The owning record as the upload endpoint returned it, with every document on it. */
  uploaded: [payload: unknown]
  removed: [documentId: number]
  'update:staged': [batches: StagedDocumentBatch[]]
}>()

const { $api } = useNuxtApp()
const { num, fileSize } = useFormat()
const toast = useToast()
const fieldId = useId()

const categories = ref<DocumentCategoryResponse[]>([])
const categoryId = ref<number | null>(null)
const title = ref('')
const confirmed = ref(false)
const staged = ref<StagedDocumentBatch[]>([])
const fileInput = ref<HTMLInputElement | null>(null)
const uploading = ref(false)
const progress = reactive({ done: 0, total: 0 })
const deleteTarget = ref<DocumentFile | null>(null)
const deleting = ref(false)

const staging = computed(() => !props.endpoint)
const stagedCount = computed(() => staged.value.reduce((sum, batch) => sum + batch.files.length, 0))
const totalCount = computed(() => props.documents.length + stagedCount.value)
/**
 * The confirmation is about the beneficiary, so it belongs to request documents and nowhere else.
 * A centre's own paperwork -- مجوز فعالیت, اساسنامه, صورت مالی -- names the organisation, never a
 * مددجو, and asking an admin creating a centre to certify that a licence scan does not identify a
 * beneficiary is a box that means nothing. A checkbox nobody can fail teaches people to tick
 * without reading, which costs us the one place the question is real.
 */
const needsConfirmation = computed(() => props.scope === 'REQUEST')

const canPick = computed(() =>
  (!needsConfirmation.value || confirmed.value)
  && Boolean(categoryId.value) && !props.disabled && !uploading.value)

/** Grouped the way the public page groups them, so the centre sees what a visitor will see. */
const groups = computed(() => {
  const byCategory = new Map<number, { name: string, items: DocumentFile[] }>()
  for (const doc of props.documents) {
    const key = doc.category?.id ?? 0
    const group = byCategory.get(key)
    if (group) group.items.push(doc)
    else byCategory.set(key, { name: doc.category?.name ?? 'سایر مدارک', items: [doc] })
  }
  return [...byCategory.entries()].map(([id, group]) => ({ id, ...group }))
})

onMounted(async () => {
  try {
    // The public list is active-only, which is exactly what a picker should offer: a category
    // an admin has deactivated keeps rendering on documents already filed under it, but no new
    // document may be filed there.
    categories.value = await $api<DocumentCategoryResponse[]>(ep.documentCategories, {
      query: { scope: props.scope },
    })
    categoryId.value = categories.value[0]?.id ?? null
  }
  catch { /* the rest of the page still works; the picker simply stays empty */ }
})

function pick() {
  fileInput.value?.click()
}

async function onPick(event: Event) {
  const input = event.target as HTMLInputElement
  const files = Array.from(input.files ?? [])
  try {
    if (!files.length) return
    if (!categoryId.value) return toast.error('ابتدا دسته‌بندی مدرک را انتخاب کنید.')

    const problem = validateDocumentSelection(files, totalCount.value)
    if (problem) return toast.error(problem)

    const batch: StagedDocumentBatch = {
      categoryId: categoryId.value,
      categoryName: categories.value.find(item => item.id === categoryId.value)?.name ?? '',
      // A title names a single document; with several files in flight there is nothing for it
      // to name, and the backend ignores it in that case.
      title: files.length === 1 ? (title.value.trim() || null) : null,
      files,
    }

    if (staging.value) {
      staged.value = [...staged.value, batch]
      emit('update:staged', staged.value)
      title.value = ''
      return
    }

    await upload([batch])
  }
  finally {
    // Cleared on every attempt, successful or not, so picking the same file again still fires
    // a change event.
    input.value = ''
  }
}

async function upload(batches: StagedDocumentBatch[]) {
  if (!props.endpoint) return
  uploading.value = true
  try {
    const result = await uploadDocumentBatches<unknown>(
      $api,
      props.endpoint,
      batches,
      (done, total) => { progress.done = done; progress.total = total },
    )
    title.value = ''
    toast.success('مدارک بارگذاری شد.')
    if (result) emit('uploaded', result)
  }
  catch (error) { toast.error(apiErrorMessage(error)) }
  finally {
    uploading.value = false
    progress.done = 0
    progress.total = 0
  }
}

function removeStaged(batchIndex: number, fileIndex: number) {
  const batch = staged.value[batchIndex]
  if (!batch) return
  batch.files.splice(fileIndex, 1)
  staged.value = staged.value.filter(item => item.files.length)
  emit('update:staged', staged.value)
}

/** Called by the parent once it has posted the staged files itself. */
function clearStaged() {
  staged.value = []
  emit('update:staged', staged.value)
}

defineExpose({ clearStaged })

async function confirmDelete() {
  const target = deleteTarget.value
  if (!target || !props.deleteEndpoint) return
  deleting.value = true
  try {
    await $api(props.deleteEndpoint(target.id), { method: 'DELETE' })
    toast.success('مدرک حذف شد.')
    emit('removed', target.id)
    deleteTarget.value = null
  }
  catch (error) { toast.error(apiErrorMessage(error)) }
  finally { deleting.value = false }
}
</script>

<template>
  <section class="flex flex-col gap-5">
    <div class="flex flex-wrap items-baseline justify-between gap-2">
      <h2 class="text-[17px] font-bold">{{ heading }}</h2>
      <span class="text-[12px] text-muted">
        {{ num(totalCount) }} از {{ num(DOCUMENT_MAX_PER_OWNER) }} مدرک
      </span>
    </div>

    <p v-if="description" class="help">{{ description }}</p>

    <!-- already uploaded -->
    <div v-if="documents.length" class="flex flex-col gap-4">
      <div v-for="group in groups" :key="group.id" class="flex flex-col gap-2">
        <span class="label mb-0">{{ group.name }}</span>
        <ul class="flex flex-col divide-y" style="border-color: var(--color-surface-3)">
          <li
            v-for="doc in group.items"
            :key="doc.id"
            class="py-2.5 first:pt-0 last:pb-0 flex items-center gap-3"
          >
            <span class="doc-ext shrink-0" :data-ext="documentExtension(doc)" aria-hidden="true">
              {{ documentExtension(doc) }}
            </span>
            <a
              :href="doc.url"
              target="_blank"
              rel="noopener nofollow"
              class="min-w-0 flex-1 flex flex-col hover:text-accent"
            >
              <span class="text-[14px] truncate">{{ documentLabel(doc) }}</span>
              <span v-if="doc.sizeBytes != null" class="text-[12px] text-muted">
                {{ fileSize(doc.sizeBytes) }}
              </span>
            </a>
            <button
              v-if="deleteEndpoint"
              type="button"
              class="shrink-0 text-danger disabled:opacity-45 disabled:cursor-not-allowed"
              :disabled="Boolean(readonlyReason) || disabled"
              :title="readonlyReason || undefined"
              :aria-label="`حذف ${documentLabel(doc)}`"
              @click="deleteTarget = doc"
            >
              <Trash2 :size="17" :stroke-width="1.75" aria-hidden="true" />
            </button>
          </li>
        </ul>
      </div>
    </div>

    <p
      v-if="readonlyReason"
      class="text-[13px] leading-7 p-4 rounded-[12px]"
      style="border: 1px solid var(--color-danger-line); background: var(--color-danger-bg); color: var(--color-danger)"
    >
      {{ readonlyReason }}
    </p>

    <!-- picker -->
    <div class="flex flex-col gap-4 border-t border-surface-3 pt-5">
      <div class="flex flex-col">
        <label class="label" :for="`${fieldId}-category`">دسته‌بندی مدرک</label>
        <select
          :id="`${fieldId}-category`"
          v-model="categoryId"
          class="field"
          :disabled="disabled || uploading || !categories.length"
        >
          <option :value="null" disabled>انتخاب کنید…</option>
          <option v-for="category in categories" :key="category.id" :value="category.id">
            {{ category.name }}
          </option>
        </select>
        <span v-if="!categories.length" class="help mt-2">
          هنوز دسته‌بندی مدرکی تعریف نشده است. با ادمین تماس بگیرید.
        </span>
      </div>

      <UiField
        v-model="title"
        label="عنوان مدرک (اختیاری)"
        :maxlength="255"
        :disabled="disabled || uploading"
        hint="فقط زمانی ثبت می‌شود که تنها یک فایل انتخاب کرده باشید."
      />

      <!-- Mandatory for request documents. They are served publicly and are the only ones that
           could carry a مددجو's name, so this is where the centre takes responsibility for that:
           it acknowledges the files go public, and certifies it holds written consent for any
           identifying detail visible on them. See `needsConfirmation`. -->
      <label
        v-if="needsConfirmation"
        class="flex items-start gap-3 text-[13px] leading-7 text-body cursor-pointer"
      >
        <input
          v-model="confirmed"
          type="checkbox"
          class="mt-1.5 w-[18px] h-[18px] shrink-0"
          style="accent-color: var(--color-accent)"
          :disabled="disabled"
        >
        <span>
          تأیید می‌کنم این مدارک روی صفحهٔ عمومی درخواست منتشر می‌شود، و اگر نام یا مشخصات هویتی
          مددجو روی آن‌ها دیده می‌شود، رضایت کتبی او را پیش از این گرفته‌ام.
        </span>
      </label>

      <input
        ref="fileInput"
        type="file"
        multiple
        :accept="DOCUMENT_ACCEPT"
        class="hidden"
        @change="onPick"
      >

      <div class="flex flex-wrap items-center gap-3">
        <button type="button" class="btn btn-secondary btn-sm" :disabled="!canPick" @click="pick">
          {{ uploading ? 'در حال بارگذاری…' : 'انتخاب فایل' }}
        </button>
        <span v-if="uploading && progress.total > 1" class="text-[13px] text-muted">
          دسته {{ num(progress.done + 1) }} از {{ num(progress.total) }}
        </span>
        <span v-else-if="needsConfirmation && !confirmed" class="help">
          برای بارگذاری، ابتدا تأییدیه بالا را علامت بزنید.
        </span>
      </div>

      <p class="help">
        {{ DOCUMENT_ACCEPT_LABEL }}
        هر بار حداکثر {{ num(DOCUMENT_BATCH_SIZE) }} فایل ارسال می‌شود؛ انتخاب بیشتر به‌صورت
        خودکار در چند مرحله بارگذاری می‌شود.
      </p>
      <p class="help">مدارک بارگذاری‌شده به‌صورت عمومی روی سایت نمایش داده می‌شوند.</p>
    </div>

    <!-- Picked but not yet posted. It sits below the picker on purpose: this list is the
         result of pressing «انتخاب فایل», and a result that renders above the control that
         produced it reads as though it were already there. -->
    <div v-if="staged.length" class="flex flex-col gap-2">
      <span class="label mb-0">فایل‌های آماده ارسال</span>
      <ul class="flex flex-col gap-2">
        <template v-for="(batch, batchIndex) in staged" :key="batchIndex">
          <li
            v-for="(file, fileIndex) in batch.files"
            :key="`${batchIndex}-${fileIndex}`"
            class="flex items-center gap-3 text-[13px] p-2.5 rounded-[10px]"
            style="background: var(--color-surface-2)"
          >
            <Paperclip :size="16" :stroke-width="1.75" class="shrink-0 text-muted" aria-hidden="true" />
            <span class="min-w-0 flex-1 truncate">{{ file.name }}</span>
            <span class="shrink-0 text-muted">{{ batch.categoryName }}</span>
            <span class="shrink-0 text-muted">{{ fileSize(file.size) }}</span>
            <button
              type="button"
              class="shrink-0 text-danger"
              :aria-label="`حذف ${file.name}`"
              @click="removeStaged(batchIndex, fileIndex)"
            >
              <X :size="16" :stroke-width="2" aria-hidden="true" />
            </button>
          </li>
        </template>
      </ul>
      <p class="help">این فایل‌ها پس از ثبت نهایی بارگذاری می‌شوند.</p>
    </div>

    <UiConfirm
      :open="Boolean(deleteTarget)"
      title="حذف مدرک"
      :message="`«${deleteTarget ? documentLabel(deleteTarget) : ''}» به‌طور کامل حذف می‌شود؛ فایل آن هم از روی سرور پاک می‌شود و این کار برگشت‌پذیر نیست.`"
      confirm-label="حذف کن"
      tone="danger"
      :busy="deleting"
      @confirm="confirmDelete"
      @close="deleteTarget = null"
    />
  </section>
</template>
