<script setup lang="ts">
import { ep } from '~/api/endpoints'
import type { CenterResponse, CityRef } from '~/types/api'

definePageMeta({ layout: 'dashboard', middleware: 'auth', role: 'CENTER' })

const { $api } = useNuxtApp()
const toast = useToast()

const cities = ref<CityRef[]>([])
const loading = ref(true)
const saving = ref(false)

const form = reactive({
  centerName: '', fullName: '', cityId: null as number | null,
  description: '', contactPhone: '', responseHours: '', address: '',
  cardNumber: '', sheba: '',
})

async function load() {
  loading.value = true
  try {
    const [center, cityData] = await Promise.all([
      $api<CenterResponse>(ep.centerMe),
      $api<CityRef[]>(ep.cities),
    ])
    cities.value = cityData
    Object.assign(form, {
      centerName: center.name,
      fullName: center.fullName ?? '',
      cityId: center.city?.id ?? null,
      description: center.description ?? '',
      contactPhone: center.contactPhone ?? '',
      responseHours: center.responseHours ?? '',
      address: center.address ?? '',
      cardNumber: center.cardNumber ?? '',
      sheba: center.sheba ?? '',
    })
  }
  catch (error) { toast.error(apiErrorMessage(error)) }
  finally { loading.value = false }
}

onMounted(load)

async function save() {
  if (!form.centerName.trim()) return toast.error('نام مرکز الزامی است.')
  saving.value = true
  try {
    await $api(ep.centerMe, {
      method: 'PUT',
      body: {
        centerName: form.centerName.trim(),
        fullName: form.fullName.trim() || null,
        cityId: form.cityId,
        description: form.description.trim() || null,
        contactPhone: form.contactPhone.trim() || null,
        responseHours: form.responseHours.trim() || null,
        address: form.address.trim() || null,
        cardNumber: form.cardNumber.trim() || null,
        sheba: form.sheba.trim() || null,
      },
    })
    toast.success('اطلاعات مرکز ذخیره شد.')
  }
  catch (error) { toast.error(apiErrorMessage(error)) }
  finally { saving.value = false }
}

useHead({ title: 'تنظیمات مرکز — یاری‌جو' })
</script>

<template>
  <div class="flex flex-col gap-6 max-w-2xl">
    <h1 class="text-[24px] font-extrabold">تنظیمات مرکز</h1>

    <div v-if="loading" class="card-flat p-6"><UiSkeleton :lines="6" /></div>

    <form v-else class="card-flat p-6 flex flex-col gap-5" @submit.prevent="save">
      <UiField v-model="form.centerName" label="نام مرکز" required :maxlength="255" />
      <UiField v-model="form.fullName" label="نام مسئول" :maxlength="255" />

      <div class="flex flex-col">
        <label class="label" for="city">شهر</label>
        <select id="city" v-model="form.cityId" class="field">
          <option :value="null">انتخاب نشده</option>
          <option v-for="city in cities" :key="city.id" :value="city.id">
            {{ city.name }}<template v-if="city.provinceName"> — {{ city.provinceName }}</template>
          </option>
        </select>
      </div>

      <UiField v-model="form.contactPhone" label="تلفن تماس" ltr :maxlength="255" />
      <UiField
        v-model="form.responseHours"
        label="ساعات پاسخ‌گویی"
        :maxlength="120"
        placeholder="شنبه تا چهارشنبه ۹ تا ۱۷"
        hint="روی کارت مرکز در صفحه هر درخواست نمایش داده می‌شود."
      />
      <UiField v-model="form.description" label="معرفی مرکز" textarea :rows="3" :maxlength="1000" />
      <UiField v-model="form.address" label="نشانی" textarea :rows="2" :maxlength="1000" />

      <div class="grid gap-5 sm:grid-cols-2 border-t border-cream-200 pt-5">
        <UiField v-model="form.cardNumber" label="شماره کارت" ltr :maxlength="255" />
        <UiField v-model="form.sheba" label="شماره شبا" ltr :maxlength="255" />
      </div>
      <p class="help">
        این اطلاعات در صفحه عمومی مرکز نمایش داده می‌شود، چون پرداخت مستقیماً با خود مرکز انجام می‌شود.
      </p>

      <div class="border-t border-cream-200 pt-5">
        <button type="submit" class="btn btn-primary" :disabled="saving">
          {{ saving ? 'در حال ذخیره…' : 'ذخیره تغییرات' }}
        </button>
      </div>
    </form>
  </div>
</template>
