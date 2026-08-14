<script setup lang="ts">
import { ep } from '~/api/endpoints'
import type { CategoryResponse } from '~/types/api'

definePageMeta({ layout: 'dashboard', middleware: 'auth', role: 'ADMIN' })

const { $api } = useNuxtApp()
const { num } = useFormat()
const toast = useToast()

/**
 * The eight label swatches from the design, all in the cool half of the wheel so a
 * category chip never fights the page.
 *
 * This list is duplicated in the backend -- CategorySeeder seeds it and CategoryMapper
 * falls back to the last pair -- because the chosen colours are stored per row in the
 * database, not looked up from a palette key. Change one, change all three, and add a
 * migration for the rows that already exist.
 */
const SWATCHES = [
  { bg: '#D6F2F6', text: '#0B7F91' },
  { bg: '#FBE4E8', text: '#9B1C31' },
  { bg: '#DEE9FC', text: '#1F5FC0' },
  { bg: '#E7E6F7', text: '#4B3F9E' },
  { bg: '#DCEFE8', text: '#1E6E57' },
  { bg: '#E4EEDC', text: '#4A6B2E' },
  { bg: '#F2E4F2', text: '#7A3E75' },
  { bg: '#E3EAF7', text: '#254F8E' },
]

const rows = ref<CategoryResponse[]>([])
const loading = ref(true)
const loadError = ref(false)
const saving = ref(false)

const selected = ref<CategoryResponse | null>(null)
const form = reactive({
  name: '', slug: '', description: '', labelBg: SWATCHES[0]!.bg, labelText: SWATCHES[0]!.text,
  sortOrder: 0, active: true,
})

const deleteTarget = ref<CategoryResponse | null>(null)
const replacementId = ref<number | ''>('')

async function load() {
  loading.value = true
  loadError.value = false
  try {
    rows.value = await $api<CategoryResponse[]>(ep.adminCategories)
  }
  catch { loadError.value = true }
  finally { loading.value = false }
}

onMounted(load)

function startCreate() {
  selected.value = null
  Object.assign(form, {
    name: '', slug: '', description: '',
    labelBg: SWATCHES[0]!.bg, labelText: SWATCHES[0]!.text,
    sortOrder: rows.value.length + 1, active: true,
  })
}

function startEdit(category: CategoryResponse) {
  selected.value = category
  Object.assign(form, {
    name: category.name,
    slug: category.slug,
    description: category.description ?? '',
    labelBg: category.labelBg,
    labelText: category.labelText,
    sortOrder: category.sortOrder,
    active: category.active,
  })
}

async function save() {
  if (!form.name.trim()) {
    toast.error('نام دسته‌بندی الزامی است.')
    return
  }
  saving.value = true
  try {
    const body = { ...form, name: form.name.trim(), slug: form.slug.trim() }
    if (selected.value) await $api(ep.adminCategory(selected.value.id), { method: 'PUT', body })
    else await $api(ep.adminCategories, { method: 'POST', body })
    toast.success('دسته‌بندی ذخیره شد.')
    selected.value = null
    startCreate()
    load()
  }
  catch (error) { toast.error(apiErrorMessage(error)) }
  finally { saving.value = false }
}

/** Categories with requests behind them cannot be deleted without a replacement. */
const replacementOptions = computed(() =>
  rows.value.filter(row => row.id !== deleteTarget.value?.id))

async function confirmDelete() {
  if (!deleteTarget.value) return
  saving.value = true
  try {
    await $api(ep.adminCategory(deleteTarget.value.id), {
      method: 'DELETE',
      query: replacementId.value ? { replacementId: replacementId.value } : {},
    })
    toast.success('دسته‌بندی حذف شد.')
    deleteTarget.value = null
    replacementId.value = ''
    load()
  }
  catch (error) { toast.error(apiErrorMessage(error)) }
  finally { saving.value = false }
}

useHead({ title: 'دسته‌بندی‌ها — پنل ادمین' })
</script>

<template>
  <div class="flex flex-col gap-6">
    <div class="flex flex-wrap items-center justify-between gap-4">
      <h1 class="text-[24px] font-extrabold">دسته‌بندی‌ها</h1>
      <button type="button" class="btn btn-primary btn-sm" @click="startCreate">+ دسته‌بندی جدید</button>
    </div>

    <div class="grid gap-6 xl:grid-cols-[minmax(0,1fr)_380px] items-start">
      <UiErrorState v-if="loadError" @retry="load()" />

      <div v-else-if="loading" class="card-flat p-6 flex flex-col gap-2">
        <UiSkeleton v-for="n in 5" :key="n" variant="row" />
      </div>

      <section v-else class="card-flat overflow-x-auto">
        <table class="data-table">
          <thead>
            <tr>
              <th>دسته‌بندی</th>
              <th>نشانی یکتا</th>
              <th>درخواست فعال</th>
              <th>وضعیت</th>
              <th>عملیات</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="category in rows" :key="category.id" :class="{ 'is-selected': selected?.id === category.id }">
              <td>
                <div class="flex items-center gap-3">
                  <span
                    class="w-[26px] h-[26px] rounded-[7px] shrink-0"
                    :style="{ backgroundColor: category.labelBg }"
                    aria-hidden="true"
                  />
                  <span class="font-semibold">{{ category.name }}</span>
                </div>
              </td>
              <td class="ltr text-[13px] text-muted">{{ category.slug }}</td>
              <td>{{ num(category.activeRequestCount) }}</td>
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
                    @click="deleteTarget = category; replacementId = ''"
                  >حذف</button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </section>

      <!-- edit form -->
      <aside class="card-flat p-6 flex flex-col gap-5">
        <h2 class="text-[17px] font-bold">
          {{ selected ? 'ویرایش دسته‌بندی' : 'دسته‌بندی جدید' }}
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

        <fieldset class="flex flex-col gap-3">
          <legend class="label">رنگ برچسب</legend>
          <div class="flex flex-wrap gap-2">
            <button
              v-for="swatch in SWATCHES"
              :key="swatch.bg"
              type="button"
              class="w-10 h-10 rounded-[10px] transition-all"
              :style="{
                backgroundColor: swatch.bg,
                boxShadow: form.labelBg === swatch.bg ? 'inset 0 0 0 2px #0F3070' : 'none',
              }"
              :aria-label="`رنگ ${swatch.bg}`"
              :aria-pressed="form.labelBg === swatch.bg"
              @click="form.labelBg = swatch.bg; form.labelText = swatch.text"
            />
          </div>
          <span class="chip self-start mt-1" :style="{ backgroundColor: form.labelBg, color: form.labelText }">
            {{ form.name || 'پیش‌نمایش برچسب' }}
          </span>
        </fieldset>

        <UiSwitch v-model="form.active" label="وضعیت" description="دسته‌بندی غیرفعال در سایت دیده نمی‌شود." />

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
      title="حذف دسته‌بندی"
      size="sm"
      @close="deleteTarget = null"
    >
      <div v-if="deleteTarget" class="flex flex-col gap-5">
        <p class="text-[15px] leading-8 text-body">
          «{{ deleteTarget.name }}» حذف می‌شود.
        </p>

        <div
          v-if="deleteTarget.activeRequestCount > 0"
          class="p-4 rounded-[12px] text-[14px] leading-7"
          style="border: 1px solid var(--color-danger-line); background: var(--color-danger-bg); color: var(--color-danger)"
        >
          این دسته‌بندی در {{ num(deleteTarget.activeRequestCount) }} درخواست استفاده شده است.
          برای حذف، دسته‌بندی جایگزین را انتخاب کنید تا آن درخواست‌ها منتقل شوند.
        </div>

        <div class="flex flex-col">
          <label class="label" for="replacement">انتقال موارد به دسته</label>
          <select id="replacement" v-model="replacementId" class="field">
            <option value="">انتخاب کنید…</option>
            <option v-for="option in replacementOptions" :key="option.id" :value="option.id">
              {{ option.name }}
            </option>
          </select>
        </div>

        <div class="flex items-center gap-3">
          <button type="button" class="btn btn-danger" :disabled="saving" @click="confirmDelete">
            {{ saving ? 'در حال حذف…' : 'حذف دسته‌بندی' }}
          </button>
          <button type="button" class="btn btn-secondary" @click="deleteTarget = null">انصراف</button>
        </div>
      </div>
    </UiModal>
  </div>
</template>
