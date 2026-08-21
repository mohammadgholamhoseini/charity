<script setup lang="ts">
import { ep } from '~/api/endpoints'
import { uploadDocumentBatches } from '~/composables/useDocuments'
import type { StagedDocumentBatch } from '~/composables/useDocuments'
import type { CategoryResponse, CenterResponse, CityRef, Page } from '~/types/api'

definePageMeta({ layout: 'dashboard', middleware: 'auth', role: 'ADMIN' })

const { $api } = useNuxtApp()
const { num } = useFormat()
const toast = useToast()

const rows = ref<CenterResponse[]>([])
const categories = ref<CategoryResponse[]>([])
const cities = ref<CityRef[]>([])
const loading = ref(true)
const loadError = ref(false)
const saving = ref(false)

const selected = ref<CenterResponse | null>(null)
const deleteTarget = ref<CenterResponse | null>(null)

/**
 * Documents picked while creating a centre. The create endpoint is JSON -- its Persian field
 * errors are surfaced per field and turning it into a multipart call would change that envelope
 * for every field on the form -- so the files wait here until the centre has an id.
 */
const stagedDocuments = ref<StagedDocumentBatch[]>([])
const uploader = ref<{ clearStaged: () => void } | null>(null)

const centerDocumentEndpoint = computed(() =>
  (selected.value ? ep.adminCenterDocuments(selected.value.id) : null))

const centerDocumentDeleteEndpoint = computed(() => {
  const center = selected.value
  return center ? (documentId: number) => ep.adminCenterDocument(center.id, documentId) : null
})

const form = reactive({
  username: '', password: '', email: '',
  centerName: '', fullName: '', cityId: null as number | null,
  categoryIds: [] as number[],
  description: '', contactPhone: '', responseHours: '', address: '',
  cardNumber: '', sheba: '', active: true,
})

async function load() {
  loading.value = true
  loadError.value = false
  try {
    const data = await $api<Page<CenterResponse>>(ep.adminCenters, { query: { size: 100 } })
    rows.value = data.content
  }
  catch { loadError.value = true }
  finally { loading.value = false }
}

onMounted(async () => {
  await load()
  try {
    const [categoryData, cityData] = await Promise.all([
      $api<CategoryResponse[]>(ep.adminCategories),
      $api<CityRef[]>(ep.cities),
    ])
    categories.value = categoryData.filter(c => c.active)
    cities.value = cityData
  }
  catch { /* the table still works without the pickers */ }
})

function startCreate() {
  selected.value = null
  stagedDocuments.value = []
  uploader.value?.clearStaged()
  Object.assign(form, {
    username: '', password: '', email: '',
    centerName: '', fullName: '', cityId: null, categoryIds: [],
    description: '', contactPhone: '', responseHours: '', address: '',
    cardNumber: '', sheba: '', active: true,
  })
}

function startEdit(center: CenterResponse) {
  selected.value = center
  stagedDocuments.value = []
  Object.assign(form, {
    username: center.username ?? '', password: '', email: center.email ?? '',
    centerName: center.name, fullName: center.fullName ?? '',
    cityId: center.city?.id ?? null,
    categoryIds: center.categories.map(c => c.id),
    description: center.description ?? '',
    contactPhone: center.contactPhone ?? '',
    responseHours: center.responseHours ?? '',
    address: center.address ?? '',
    cardNumber: center.cardNumber ?? '',
    sheba: center.sheba ?? '',
    active: center.status === 'APPROVED',
  })
}

function toggleCategory(id: number) {
  const index = form.categoryIds.indexOf(id)
  if (index >= 0) form.categoryIds.splice(index, 1)
  else form.categoryIds.push(id)
}

async function save() {
  if (!form.centerName.trim()) return toast.error('نام مرکز الزامی است.')
  if (!form.cityId) return toast.error('انتخاب شهر الزامی است.')
  if (!form.categoryIds.length) return toast.error('حداقل یک دسته‌بندی مجاز انتخاب کنید.')

  saving.value = true
  try {
    if (selected.value) {
      await $api(ep.adminCenter(selected.value.id), {
        method: 'PUT',
        body: {
          centerName: form.centerName.trim(), fullName: form.fullName.trim() || null,
          cityId: form.cityId, categoryIds: form.categoryIds,
          description: form.description.trim() || null,
          contactPhone: form.contactPhone.trim() || null,
          responseHours: form.responseHours.trim() || null,
          address: form.address.trim() || null,
          cardNumber: form.cardNumber.trim() || null,
          sheba: form.sheba.trim() || null,
          active: form.active,
        },
      })
    }
    else {
      const created = await $api<CenterResponse>(ep.adminCenters, {
        method: 'POST',
        body: {
          username: form.username.trim(), password: form.password, email: form.email.trim(),
          centerName: form.centerName.trim(), fullName: form.fullName.trim() || null,
          cityId: form.cityId, categoryIds: form.categoryIds,
          description: form.description.trim() || null,
          contactPhone: form.contactPhone.trim() || null,
          responseHours: form.responseHours.trim() || null,
          address: form.address.trim() || null,
          cardNumber: form.cardNumber.trim() || null,
          sheba: form.sheba.trim() || null,
          active: form.active,
        },
      })

      if (stagedDocuments.value.length) {
        try {
          await uploadDocumentBatches<CenterResponse>(
            $api,
            ep.adminCenterDocuments(created.id),
            stagedDocuments.value,
          )
          stagedDocuments.value = []
          uploader.value?.clearStaged()
        }
        catch (error) {
          // The centre itself was created and is a perfectly good record -- documents are never
          // mandatory, so there is nothing to roll back. The form stays open on that centre so
          // the upload can be retried against the id we now know.
          selected.value = created
          stagedDocuments.value = []
          uploader.value?.clearStaged()
          toast.error(apiErrorMessage(error))
          toast.info('مرکز ساخته شد، اما بارگذاری مدارک ناتمام ماند. از همین فرم دوباره تلاش کنید.')
          await load()
          return
        }
      }
    }
    toast.success('مرکز ذخیره شد.')
    startCreate()
    load()
  }
  catch (error) { toast.error(apiErrorMessage(error)) }
  finally { saving.value = false }
}

/** The API answered 204, so the row is gone; dropping it locally saves a refetch. */
function onCenterDocumentRemoved(documentId: number) {
  if (!selected.value) return
  selected.value.documents = (selected.value.documents ?? []).filter(doc => doc.id !== documentId)
}

async function confirmDelete() {
  if (!deleteTarget.value) return
  saving.value = true
  try {
    await $api(ep.adminCenter(deleteTarget.value.id), { method: 'DELETE' })
    toast.success('مرکز حذف شد.')
    deleteTarget.value = null
    load()
  }
  catch (error) { toast.error(apiErrorMessage(error)) }
  finally { saving.value = false }
}

/* ------------------------------------------------------- password reset */

/**
 * The admin types the new password; nothing is generated and nothing is echoed back —
 * the endpoint answers 204, so the value only ever exists in this ref. It is cleared
 * when the dialog opens and again when it closes, so it never lingers in the DOM or in
 * component state after the dialog goes away.
 */
const passwordTarget = ref<CenterResponse | null>(null)
const newPassword = ref('')

function startPasswordReset(center: CenterResponse) {
  newPassword.value = ''
  passwordTarget.value = center
}

function closePasswordReset() {
  passwordTarget.value = null
  newPassword.value = ''
}

async function confirmPasswordReset() {
  if (!passwordTarget.value) return
  if (newPassword.value.length < 8) return toast.error('رمز عبور باید حداقل ۸ نویسه باشد.')

  saving.value = true
  try {
    await $api(ep.adminCenterPassword(passwordTarget.value.id), {
      method: 'POST',
      body: { newPassword: newPassword.value },
    })
    // No load() here: nothing in CenterResponse changes when the password does.
    toast.success('رمز عبور مرکز بازنشانی شد. آن را از راهی امن به مرکز برسانید.')
    closePasswordReset()
  }
  catch (error) { toast.error(apiErrorMessage(error)) }
  finally { saving.value = false }
}

useHead({ title: 'مراکز خیریه — پنل ادمین' })
</script>

<template>
  <div class="flex flex-col gap-6">
    <div class="flex flex-wrap items-center justify-between gap-4">
      <div class="flex flex-col gap-1">
        <h1 class="text-[24px] font-extrabold">مراکز خیریه</h1>
        <p class="text-[14px] text-muted">ثبت‌نام عمومی وجود ندارد؛ حساب مراکز فقط از این صفحه ساخته می‌شود.</p>
      </div>
      <button type="button" class="btn btn-primary btn-sm" @click="startCreate">+ ایجاد مرکز جدید</button>
    </div>

    <div class="grid gap-6 xl:grid-cols-[minmax(0,1fr)_400px] items-start">
      <UiErrorState v-if="loadError" @retry="load()" />

      <div v-else-if="loading" class="card-flat p-6 flex flex-col gap-2">
        <UiSkeleton v-for="n in 4" :key="n" variant="row" />
      </div>

      <UiEmptyState
        v-else-if="!rows.length"
        title="هنوز مرکزی ثبت نشده است"
        description="با فرم کنار همین صفحه اولین مرکز خیریه را ایجاد کنید."
      />

      <section v-else class="card-flat overflow-x-auto">
        <table class="data-table">
          <thead>
            <tr>
              <th>نام مرکز</th>
              <th>شهر</th>
              <th>دسته‌های فعال</th>
              <th>درخواست</th>
              <th>وضعیت</th>
              <th>عملیات</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="center in rows" :key="center.id" :class="{ 'is-selected': selected?.id === center.id }">
              <td>
                <div class="flex flex-col">
                  <span class="font-semibold">{{ center.name }}</span>
                  <span class="text-[12px] text-muted ltr">{{ center.username }}</span>
                </div>
              </td>
              <td>{{ center.city?.name ?? '—' }}</td>
              <td>
                <div class="flex flex-wrap gap-1">
                  <UiChip
                    v-for="category in center.categories.slice(0, 3)"
                    :key="category.id"
                    :label="category.name"
                    :color="{ bg: category.labelBg, text: category.labelText }"
                  />
                </div>
              </td>
              <td>{{ num(center.activeRequestCount) }}</td>
              <td>
                <UiChip
                  :label="center.statusLabel"
                  :status="center.status === 'APPROVED' ? 'PUBLISHED' : 'INACTIVE'"
                />
              </td>
              <td>
                <div class="flex items-center gap-3 text-[13px]">
                  <button type="button" class="text-accent hover:text-accent-600" @click="startEdit(center)">
                    ویرایش
                  </button>
                  <button type="button" class="text-accent hover:text-accent-600" @click="startPasswordReset(center)">
                    بازنشانی رمز
                  </button>
                  <button type="button" class="text-danger hover:underline" @click="deleteTarget = center">
                    حذف
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </section>

      <aside class="card-flat p-6 flex flex-col gap-5">
        <h2 class="text-[17px] font-bold">{{ selected ? 'ویرایش مرکز' : 'ایجاد مرکز جدید' }}</h2>

        <UiField v-model="form.centerName" label="نام مرکز" required :maxlength="255" />
        <UiField v-model="form.fullName" label="نام مسئول" :maxlength="255" />

        <div class="flex flex-col">
          <label class="label" for="center-city">شهر <span class="text-danger">*</span></label>
          <select id="center-city" v-model="form.cityId" class="field">
            <option :value="null" disabled>انتخاب کنید…</option>
            <option v-for="city in cities" :key="city.id" :value="city.id">
              {{ city.name }}<template v-if="city.provinceName"> — {{ city.provinceName }}</template>
            </option>
          </select>
        </div>

        <UiField v-model="form.contactPhone" label="تلفن" ltr :maxlength="255" />
        <UiField v-model="form.responseHours" label="ساعات پاسخ‌گویی" :maxlength="120" placeholder="شنبه تا چهارشنبه ۹ تا ۱۷" />
        <UiField v-model="form.address" label="نشانی" textarea :rows="2" :maxlength="1000" />

        <fieldset class="flex flex-col gap-3">
          <legend class="label">دسته‌های مجاز <span class="text-danger">*</span></legend>
          <p class="help">مرکز فقط در همین دسته‌ها می‌تواند درخواست ثبت کند.</p>
          <div class="flex flex-wrap gap-2">
            <button
              v-for="category in categories"
              :key="category.id"
              type="button"
              class="chip transition-all"
              :style="form.categoryIds.includes(category.id)
                ? { backgroundColor: category.labelBg, color: category.labelText, boxShadow: 'inset 0 0 0 1.5px currentColor' }
                : { backgroundColor: 'var(--color-surface-2)', color: 'var(--color-muted)' }"
              :aria-pressed="form.categoryIds.includes(category.id)"
              @click="toggleCategory(category.id)"
            >
              <span v-if="form.categoryIds.includes(category.id)" aria-hidden="true">✓</span>
              {{ category.name }}
            </button>
          </div>
        </fieldset>

        <div v-if="!selected" class="flex flex-col gap-4 border-t border-surface-3 pt-5">
          <h3 class="text-[15px] font-bold">حساب کاربری</h3>
          <UiField v-model="form.username" label="نام کاربری" ltr required :maxlength="60" />
          <UiField v-model="form.email" label="ایمیل" ltr type="email" required />
          <UiField
            v-model="form.password"
            label="رمز عبور موقت"
            type="password"
            revealable
            required
            hint="حداقل ۸ نویسه. مرکز پس از ورود می‌تواند آن را از صفحه پروفایل خود تغییر دهد."
          />
        </div>

        <!-- In create mode this stages files and `save()` posts them once the centre has an id;
             in edit mode it posts straight away and manages what is already there. -->
        <div class="border-t border-surface-3 pt-5">
          <DocumentUploader
            ref="uploader"
            :key="selected?.id ?? 'new'"
            scope="CENTER"
            heading="مدارک مرکز"
            description="مجوز فعالیت، اساسنامه و صورت‌های مالی مرکز. روی صفحه عمومی مرکز نمایش داده می‌شوند."
            :documents="selected?.documents ?? []"
            :endpoint="centerDocumentEndpoint"
            :delete-endpoint="centerDocumentDeleteEndpoint"
            @update:staged="stagedDocuments = $event"
            @uploaded="selected = $event as CenterResponse"
            @removed="onCenterDocumentRemoved"
          />
        </div>

        <UiSwitch
          v-model="form.active"
          label="وضعیت مرکز"
          description="غیرفعال کردن مرکز، درخواست‌های فعالش را هم از سایت خارج می‌کند."
        />

        <div class="flex items-center gap-3 border-t border-surface-3 pt-5">
          <button type="button" class="btn btn-primary" :disabled="saving" @click="save">
            {{ saving ? 'در حال ذخیره…' : (selected ? 'ذخیره تغییرات' : 'ایجاد مرکز') }}
          </button>
          <button type="button" class="btn btn-secondary" @click="startCreate">انصراف</button>
        </div>
      </aside>
    </div>

    <UiModal
      :open="Boolean(passwordTarget)"
      title="بازنشانی رمز عبور مرکز"
      size="sm"
      @close="closePasswordReset"
    >
      <div v-if="passwordTarget" class="flex flex-col gap-5">
        <p class="text-[15px] leading-8 text-body">
          رمز عبور تازه برای «{{ passwordTarget.name }}» با نام کاربری
          <span class="ltr font-semibold">{{ passwordTarget.username }}</span> تعیین می‌شود.
        </p>

        <UiField
          v-model="newPassword"
          label="رمز عبور جدید"
          type="password"
          revealable
          required
          :maxlength="100"
          hint="حداقل ۸ نویسه. بازنشانی، قفل موقت پانزده‌دقیقه‌ای حساب را هم برمی‌دارد. نشست‌هایی که هم‌اکنون وارد شده‌اند با بازنشانی بلافاصله بسته نمی‌شوند."
        />

        <div class="flex items-center gap-3">
          <button type="button" class="btn btn-primary" :disabled="saving" @click="confirmPasswordReset">
            {{ saving ? 'در حال ذخیره…' : 'بازنشانی رمز' }}
          </button>
          <button type="button" class="btn btn-secondary" :disabled="saving" @click="closePasswordReset">
            انصراف
          </button>
        </div>
      </div>
    </UiModal>

    <UiConfirm
      :open="Boolean(deleteTarget)"
      title="حذف مرکز"
      :message="`«${deleteTarget?.name}» و حساب کاربری آن حذف می‌شود. مرکزی که درخواست ثبت‌شده دارد قابل حذف نیست — به‌جای آن غیرفعالش کنید.`"
      confirm-label="حذف کن"
      tone="danger"
      :busy="saving"
      @confirm="confirmDelete"
      @close="deleteTarget = null"
    />
  </div>
</template>
