<script setup lang="ts">
import { ep } from '~/api/endpoints'
import type { DocumentCategoryResponse, DocumentScope } from '~/types/api'

/**
 * The two document-category lists, on one page.
 *
 * They are column-identical and share one table behind a `scope` discriminator, so two pages
 * would have been the same screen twice. The tab is the scope: it filters the list query and it
 * is what a newly created category is filed under.
 *
 * Deliberately not merged into `categories.vue`. That page manages the public need taxonomy --
 * the one joined by `center_categories`, counted per request and painted per row -- and these
 * two lists have nothing to do with it.
 */
definePageMeta({ layout: 'dashboard', middleware: 'auth', role: 'ADMIN' })

const { $api } = useNuxtApp()
const toast = useToast()

const tabs: { value: DocumentScope, label: string }[] = [
  { value: 'REQUEST', label: 'مدارک درخواست' },
  { value: 'CENTER', label: 'مدارک مرکز' },
]

const scope = ref<DocumentScope>('REQUEST')
const rows = ref<DocumentCategoryResponse[]>([])
const loading = ref(true)
const loadError = ref(false)
const saving = ref(false)

const selected = ref<DocumentCategoryResponse | null>(null)
const form = reactive({ name: '', slug: '', description: '', sortOrder: '0', active: true })

const deleteTarget = ref<DocumentCategoryResponse | null>(null)
const replacementId = ref<number | ''>('')

/**
 * Set from the API's own 409, which carries the number of documents still filed under the
 * category. An unused category deletes unconditionally and ignores `replacementId`, so the
 * replacement picker only appears once the delete has actually been refused -- asking for one
 * up front would demand an answer for a question that usually does not arise.
 */
const deleteConflict = ref<string | null>(null)

function startDelete(category: DocumentCategoryResponse) {
  deleteTarget.value = category
  replacementId.value = ''
  deleteConflict.value = null
}

function closeDelete() {
  deleteTarget.value = null
  replacementId.value = ''
  deleteConflict.value = null
}

async function load() {
  loading.value = true
  loadError.value = false
  try {
    rows.value = await $api<DocumentCategoryResponse[]>(ep.adminDocumentCategories, {
      query: { scope: scope.value },
    })
  }
  catch { loadError.value = true }
  finally { loading.value = false }
}

onMounted(load)

function startCreate() {
  selected.value = null
  Object.assign(form, {
    name: '', slug: '', description: '',
    sortOrder: String(rows.value.length + 1),
    active: true,
  })
}

function startEdit(category: DocumentCategoryResponse) {
  selected.value = category
  Object.assign(form, {
    name: category.name,
    slug: category.slug,
    description: category.description ?? '',
    sortOrder: String(category.sortOrder),
    active: category.active,
  })
}

/** Switching tab is a different list and a different scope, so the open form goes with it. */
async function selectScope(value: DocumentScope) {
  if (scope.value === value) return
  scope.value = value
  selected.value = null
  closeDelete()
  await load()
  startCreate()
}

async function save() {
  if (!form.name.trim()) {
    toast.error('نام دسته‌بندی الزامی است.')
    return
  }
  saving.value = true
  try {
    const body = {
      // The scope of an existing category is not editable: moving a category between the two
      // lists would take its documents with it into a list they do not belong in.
      scope: selected.value?.scope ?? scope.value,
      name: form.name.trim(),
      slug: form.slug.trim(),
      description: form.description.trim() || null,
      sortOrder: Number(form.sortOrder) || 0,
      active: form.active,
    }
    if (selected.value) await $api(ep.adminDocumentCategory(selected.value.id), { method: 'PUT', body })
    else await $api(ep.adminDocumentCategories, { method: 'POST', body })
    toast.success('دسته‌بندی مدرک ذخیره شد.')
    await load()
    startCreate()
  }
  catch (error) { toast.error(apiErrorMessage(error)) }
  finally { saving.value = false }
}

/** A replacement has to share the scope; the API refuses one from the other list. */
const replacementOptions = computed(() =>
  rows.value.filter(row => row.id !== deleteTarget.value?.id))

async function confirmDelete() {
  if (!deleteTarget.value) return
  saving.value = true
  try {
    await $api(ep.adminDocumentCategory(deleteTarget.value.id), {
      method: 'DELETE',
      query: replacementId.value ? { replacementId: replacementId.value } : {},
    })
    toast.success('دسته‌بندی مدرک حذف شد.')
    deleteTarget.value = null
    replacementId.value = ''
    deleteConflict.value = null
    await load()
  }
  catch (error) {
    const code = (error as { data?: { code?: string } })?.data?.code
    if (code === 'DOCUMENT_CATEGORY_IN_USE') {
      // The dialog stays open and grows a replacement picker; the API's message already says
      // how many documents are in the way.
      deleteConflict.value = apiErrorMessage(error)
    }
    else toast.error(apiErrorMessage(error))
  }
  finally { saving.value = false }
}

useHead({ title: 'دسته‌بندی مدارک — پنل ادمین' })
</script>

<template>
  <div class="flex flex-col gap-6">
    <div class="flex flex-wrap items-center justify-between gap-4">
      <div class="flex flex-col gap-1">
        <h1 class="text-[24px] font-extrabold">دسته‌بندی مدارک</h1>
        <p class="text-[14px] text-muted">
          مدارک درخواست‌ها و مدارک مراکز هرکدام فهرست جداگانه خود را دارند.
        </p>
      </div>
      <button type="button" class="btn btn-primary btn-sm" @click="startCreate">+ دسته‌بندی جدید</button>
    </div>

    <div class="flex flex-wrap gap-2" role="tablist">
      <button
        v-for="tab in tabs"
        :key="tab.value"
        type="button"
        role="tab"
        class="px-4 py-2.5 rounded-[10px] text-[14px] border transition-colors"
        :class="scope === tab.value
          ? 'border-accent bg-accent-50 font-bold text-accent'
          : 'border-line-soft text-body hover:bg-surface-2'"
        :aria-selected="scope === tab.value"
        @click="selectScope(tab.value)"
      >
        {{ tab.label }}
      </button>
    </div>

    <div class="grid gap-6 xl:grid-cols-[minmax(0,1fr)_380px] items-start">
      <UiErrorState v-if="loadError" @retry="load()" />

      <div v-else-if="loading" class="card-flat p-6 flex flex-col gap-2">
        <UiSkeleton v-for="n in 4" :key="n" variant="row" />
      </div>

      <UiEmptyState
        v-else-if="!rows.length"
        title="هنوز دسته‌بندی‌ای در این فهرست نیست"
        description="با فرم کنار همین صفحه اولین دسته‌بندی را بسازید."
      />

      <section v-else class="card-flat overflow-x-auto">
        <table class="data-table">
          <thead>
            <tr>
              <th>دسته‌بندی</th>
              <th>نشانی یکتا</th>
              <th>ترتیب</th>
              <th>وضعیت</th>
              <th>عملیات</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="category in rows" :key="category.id" :class="{ 'is-selected': selected?.id === category.id }">
              <td>
                <div class="flex flex-col">
                  <span class="font-semibold">{{ category.name }}</span>
                  <span v-if="category.description" class="text-[12px] text-muted">
                    {{ category.description }}
                  </span>
                </div>
              </td>
              <td class="ltr text-[13px] text-muted">{{ category.slug }}</td>
              <td>{{ category.sortOrder }}</td>
              <td>
                <UiChip
                  :label="category.active ? 'فعال' : 'غیرفعال'"
                  :status="category.active ? 'PUBLISHED' : 'INACTIVE'"
                />
              </td>
              <td>
                <div class="flex items-center gap-3 text-[13px]">
                  <button type="button" class="text-accent hover:text-accent-600" @click="startEdit(category)">
                    ویرایش
                  </button>
                  <button
                    type="button"
                    class="text-danger hover:underline"
                    @click="startDelete(category)"
                  >حذف</button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </section>

      <aside class="card-flat p-6 flex flex-col gap-5">
        <h2 class="text-[17px] font-bold">
          {{ selected ? 'ویرایش دسته‌بندی' : 'دسته‌بندی جدید' }}
          <span class="text-[13px] font-normal text-muted">
            — {{ tabs.find(tab => tab.value === (selected?.scope ?? scope))?.label }}
          </span>
        </h2>

        <UiField v-model="form.name" label="نام دسته‌بندی" required :maxlength="255" />
        <UiField
          v-model="form.slug"
          label="نشانی یکتا"
          ltr
          :maxlength="120"
          hint="اگر خالی بماند، از روی نام ساخته می‌شود. فقط حروف کوچک انگلیسی، عدد و خط تیره."
        />
        <UiField v-model="form.description" label="توضیح کوتاه" textarea :rows="2" :maxlength="500" />
        <UiField v-model="form.sortOrder" label="ترتیب نمایش" ltr hint="عدد کوچک‌تر بالاتر نمایش داده می‌شود." />

        <UiSwitch
          v-model="form.active"
          label="وضعیت"
          description="دسته‌بندی غیرفعال از فهرست بارگذاری حذف می‌شود، اما مدارکی که پیش‌تر زیر آن ثبت شده‌اند با همان نام نمایش داده می‌شوند."
        />

        <p
          class="text-[13px] leading-7 text-muted p-4 rounded-[12px]"
          style="border: 1px dashed var(--color-line)"
        >
          برای کنار گذاشتن یک دسته‌بندی، غیرفعال کردن بر حذف ترجیح دارد: حذف، همه مدارک آن دسته را
          به دسته‌ای دیگر منتقل می‌کند.
        </p>

        <div class="flex items-center gap-3 border-t border-surface-3 pt-5">
          <button type="button" class="btn btn-primary" :disabled="saving" @click="save">
            {{ saving ? 'در حال ذخیره…' : 'ذخیره' }}
          </button>
          <button type="button" class="btn btn-secondary" @click="startCreate">انصراف</button>
        </div>
      </aside>
    </div>

    <UiModal
      :open="Boolean(deleteTarget)"
      title="حذف دسته‌بندی مدرک"
      size="sm"
      @close="closeDelete"
    >
      <div v-if="deleteTarget" class="flex flex-col gap-5">
        <p class="text-[15px] leading-8 text-body">
          «{{ deleteTarget.name }}» حذف می‌شود. اگر فقط می‌خواهید این دسته‌بندی دیگر در فهرست
          بارگذاری دیده نشود، به‌جای حذف آن را غیرفعال کنید.
        </p>

        <template v-if="deleteConflict">
          <div
            class="p-4 rounded-[12px] text-[14px] leading-7"
            style="border: 1px solid var(--color-danger-line); background: var(--color-danger-bg); color: var(--color-danger)"
          >
            {{ deleteConflict }}
          </div>

          <div class="flex flex-col">
            <label class="label" for="doc-replacement">انتقال مدارک به دسته</label>
            <select id="doc-replacement" v-model="replacementId" class="field">
              <option value="">انتخاب کنید…</option>
              <option v-for="option in replacementOptions" :key="option.id" :value="option.id">
                {{ option.name }}
              </option>
            </select>
          </div>
        </template>

        <div class="flex items-center gap-3">
          <button type="button" class="btn btn-danger" :disabled="saving" @click="confirmDelete">
            {{ saving ? 'در حال حذف…' : 'حذف دسته‌بندی' }}
          </button>
          <button type="button" class="btn btn-secondary" @click="closeDelete">انصراف</button>
        </div>
      </div>
    </UiModal>
  </div>
</template>
