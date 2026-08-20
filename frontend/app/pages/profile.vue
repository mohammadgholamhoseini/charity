<script setup lang="ts">
import { ep } from '~/api/endpoints'
import type { CenterResponse, CityRef } from '~/types/api'

/**
 * One profile page for both roles, as it was before the panel was split in two.
 *
 * Deliberately not under `/dashboard`: `/profile` is the URL this page has always had and the one
 * people have bookmarked. That does mean it sits outside the `/dashboard/**` route rule, so it
 * carries its own `ssr: false` and noindex in nuxt.config — this page renders a signed-in user's
 * own data and must never be server-rendered or crawled.
 *
 * The branch on role is what master did too. The two halves share nothing but the chrome, so this
 * is one page with two forms rather than an abstraction over both.
 */
definePageMeta({ layout: 'dashboard', middleware: 'auth' })

interface AdminProfile {
  id: number
  username: string
  email: string
  fullName: string | null
  lastLoginAt: string | null
}

const { $api } = useNuxtApp()
const { date } = useFormat()
const auth = useAuth()
const toast = useToast()

const loading = ref(true)
const saving = ref(false)

/* ---------------------------------------------------------------- admin */

const admin = ref<AdminProfile | null>(null)
const adminForm = reactive({ fullName: '', email: '', currentPassword: '', newPassword: '' })

async function loadAdmin() {
  const data = await $api<AdminProfile>(ep.adminMe)
  admin.value = data
  adminForm.fullName = data.fullName ?? ''
  adminForm.email = data.email
}

async function saveAdmin() {
  if (adminForm.newPassword && !adminForm.currentPassword) {
    toast.error('برای تغییر رمز عبور، رمز فعلی را وارد کنید.')
    return
  }
  saving.value = true
  try {
    await $api(ep.adminMe, {
      method: 'PUT',
      body: {
        fullName: adminForm.fullName.trim() || null,
        email: adminForm.email.trim(),
        currentPassword: adminForm.currentPassword || null,
        newPassword: adminForm.newPassword || null,
      },
    })
    adminForm.currentPassword = ''
    adminForm.newPassword = ''
    toast.success('حساب شما به‌روزرسانی شد.')
    await loadAdmin()
  }
  catch (error) { toast.error(apiErrorMessage(error)) }
  finally { saving.value = false }
}

/* --------------------------------------------------------------- centre */

const center = ref<CenterResponse | null>(null)
const cities = ref<CityRef[]>([])
const logoInput = ref<HTMLInputElement | null>(null)
const uploadingLogo = ref(false)

const centerForm = reactive({
  centerName: '', fullName: '', cityId: null as number | null,
  description: '', contactPhone: '', responseHours: '', address: '',
  cardNumber: '', sheba: '', currentPassword: '', newPassword: '',
})

/**
 * `logoUrl` arrives as an absolute URL already — the API builds it with AppUrls.fileUrl, the same
 * helper behind a request's image and documents. Do not prefix it with the files route; the entity
 * stores a bare filename but the DTO does not expose that.
 */
const logoSrc = computed(() => center.value?.logoUrl ?? null)

async function loadCenter() {
  const [data, cityData] = await Promise.all([
    $api<CenterResponse>(ep.centerMe),
    $api<CityRef[]>(ep.cities),
  ])
  center.value = data
  cities.value = cityData
  Object.assign(centerForm, {
    centerName: data.name,
    fullName: data.fullName ?? '',
    cityId: data.city?.id ?? null,
    description: data.description ?? '',
    contactPhone: data.contactPhone ?? '',
    responseHours: data.responseHours ?? '',
    address: data.address ?? '',
    cardNumber: data.cardNumber ?? '',
    sheba: data.sheba ?? '',
    currentPassword: '',
    newPassword: '',
  })
}

async function saveCenter() {
  if (!centerForm.centerName.trim()) return toast.error('نام مرکز الزامی است.')
  if (centerForm.newPassword && !centerForm.currentPassword) {
    return toast.error('برای تغییر رمز عبور، رمز فعلی را وارد کنید.')
  }
  saving.value = true
  try {
    await $api(ep.centerMe, {
      method: 'PUT',
      body: {
        centerName: centerForm.centerName.trim(),
        fullName: centerForm.fullName.trim() || null,
        cityId: centerForm.cityId,
        description: centerForm.description.trim() || null,
        contactPhone: centerForm.contactPhone.trim() || null,
        responseHours: centerForm.responseHours.trim() || null,
        address: centerForm.address.trim() || null,
        cardNumber: centerForm.cardNumber.trim() || null,
        sheba: centerForm.sheba.trim() || null,
        // Both null unless the centre filled the pair in — then the password changes.
        currentPassword: centerForm.currentPassword || null,
        newPassword: centerForm.newPassword || null,
      },
    })
    centerForm.currentPassword = ''
    centerForm.newPassword = ''
    toast.success('اطلاعات مرکز ذخیره شد.')
  }
  catch (error) { toast.error(apiErrorMessage(error)) }
  finally { saving.value = false }
}

/**
 * The logo endpoint takes multipart, so the body is a FormData and the Content-Type is left for
 * the browser to set — setting it by hand omits the multipart boundary and the upload fails.
 */
async function uploadLogo(event: Event) {
  const file = (event.target as HTMLInputElement).files?.[0]
  if (!file) return
  uploadingLogo.value = true
  try {
    const body = new FormData()
    body.append('file', file)
    center.value = await $api<CenterResponse>(ep.centerLogo, { method: 'POST', body })
    toast.success('لوگوی مرکز به‌روزرسانی شد.')
  }
  catch (error) { toast.error(apiErrorMessage(error)) }
  finally {
    uploadingLogo.value = false
    // Cleared so picking the same file again still fires a change event.
    if (logoInput.value) logoInput.value.value = ''
  }
}

/* ----------------------------------------------------------------- load */

onMounted(async () => {
  loading.value = true
  try {
    await (auth.isAdmin.value ? loadAdmin() : loadCenter())
  }
  catch (error) { toast.error(apiErrorMessage(error)) }
  finally { loading.value = false }
})

useHead({ title: 'پروفایل من — یاری‌جو' })
</script>

<template>
  <div class="flex flex-col gap-6 max-w-2xl">
    <h1 class="text-[24px] font-extrabold">پروفایل من</h1>

    <div v-if="loading" class="card-flat p-6"><UiSkeleton :lines="6" /></div>

    <!-- admin -->
    <form v-else-if="auth.isAdmin.value" class="card-flat p-6 flex flex-col gap-5" @submit.prevent="saveAdmin">
      <div class="flex flex-col gap-1">
        <span class="label mb-0">نام کاربری</span>
        <span class="ltr text-[15px] font-semibold">{{ admin?.username }}</span>
        <span v-if="admin?.lastLoginAt" class="help">آخرین ورود: {{ date(admin.lastLoginAt) }}</span>
      </div>

      <UiField v-model="adminForm.fullName" label="نام و نام خانوادگی" :maxlength="255" />
      <UiField v-model="adminForm.email" label="ایمیل" type="email" ltr />

      <div class="border-t border-surface-3 pt-5 flex flex-col gap-5">
        <h2 class="text-[16px] font-bold">تغییر رمز عبور</h2>
        <UiField v-model="adminForm.currentPassword" label="رمز عبور فعلی" type="password" revealable />
        <UiField
          v-model="adminForm.newPassword"
          label="رمز عبور جدید"
          type="password"
          revealable
          hint="حداقل ۸ نویسه. تغییر رمز، قفل موقت حساب را هم برمی‌دارد."
        />
      </div>

      <div class="border-t border-surface-3 pt-5">
        <button type="submit" class="btn btn-primary" :disabled="saving">
          {{ saving ? 'در حال ذخیره…' : 'ذخیره تغییرات' }}
        </button>
      </div>
    </form>

    <!-- centre -->
    <form v-else class="card-flat p-6 flex flex-col gap-5" @submit.prevent="saveCenter">
      <div class="flex items-center gap-4">
        <div
          class="w-[76px] h-[76px] rounded-[16px] overflow-hidden shrink-0 flex items-center justify-center"
          style="background: var(--color-surface-2); color: var(--color-muted-2)"
        >
          <img v-if="logoSrc" :src="logoSrc" alt="لوگوی مرکز" class="w-full h-full object-cover">
          <span v-else class="text-[12px]">بدون لوگو</span>
        </div>
        <div class="flex flex-col gap-2">
          <span class="label mb-0">لوگوی مرکز</span>
          <input
            ref="logoInput"
            type="file"
            accept="image/jpeg,image/png,image/webp"
            class="hidden"
            @change="uploadLogo"
          >
          <button
            type="button"
            class="btn btn-secondary btn-sm"
            :disabled="uploadingLogo"
            @click="logoInput?.click()"
          >{{ uploadingLogo ? 'در حال بارگذاری…' : 'انتخاب عکس' }}</button>
          <span class="help">JPG، PNG یا WebP. روی صفحه عمومی مرکز نمایش داده می‌شود.</span>
        </div>
      </div>

      <div class="flex flex-col gap-1 border-t border-surface-3 pt-5">
        <span class="label mb-0">نام کاربری</span>
        <span class="ltr text-[15px] font-semibold">{{ center?.username }}</span>
        <span class="help">غیرقابل تغییر است؛ برای تغییر با ادمین تماس بگیرید.</span>
      </div>

      <UiField v-model="centerForm.centerName" label="نام مرکز" required :maxlength="255" />
      <UiField v-model="centerForm.fullName" label="نام مسئول" :maxlength="255" />

      <div class="flex flex-col">
        <label class="label" for="city">شهر</label>
        <select id="city" v-model="centerForm.cityId" class="field">
          <option :value="null">انتخاب نشده</option>
          <option v-for="city in cities" :key="city.id" :value="city.id">
            {{ city.name }}<template v-if="city.provinceName"> — {{ city.provinceName }}</template>
          </option>
        </select>
      </div>

      <UiField v-model="centerForm.contactPhone" label="تلفن تماس" ltr :maxlength="255" />
      <UiField
        v-model="centerForm.responseHours"
        label="ساعات پاسخ‌گویی"
        :maxlength="120"
        placeholder="شنبه تا چهارشنبه ۹ تا ۱۷"
        hint="روی کارت مرکز در صفحه هر درخواست نمایش داده می‌شود."
      />
      <UiField v-model="centerForm.description" label="معرفی مرکز" textarea :rows="3" :maxlength="1000" />
      <UiField v-model="centerForm.address" label="نشانی" textarea :rows="2" :maxlength="1000" />

      <div class="grid gap-5 sm:grid-cols-2 border-t border-surface-3 pt-5">
        <UiField v-model="centerForm.cardNumber" label="شماره کارت" ltr :maxlength="255" />
        <UiField v-model="centerForm.sheba" label="شماره شبا" ltr :maxlength="255" />
      </div>
      <p class="help">
        این اطلاعات در صفحه عمومی مرکز نمایش داده می‌شود، چون پرداخت مستقیماً با خود مرکز انجام می‌شود.
      </p>

      <div class="border-t border-surface-3 pt-5 flex flex-col gap-5">
        <h2 class="text-[16px] font-bold">تغییر رمز عبور</h2>
        <UiField v-model="centerForm.currentPassword" label="رمز عبور فعلی" type="password" revealable />
        <UiField
          v-model="centerForm.newPassword"
          label="رمز عبور جدید"
          type="password"
          revealable
          hint="حداقل ۸ نویسه. تغییر رمز، قفل موقت حساب را هم برمی‌دارد."
        />
      </div>

      <div class="border-t border-surface-3 pt-5">
        <button type="submit" class="btn btn-primary" :disabled="saving">
          {{ saving ? 'در حال ذخیره…' : 'ذخیره تغییرات' }}
        </button>
      </div>
    </form>
  </div>
</template>
