<script setup lang="ts">
import { ep } from '~/api/endpoints'
import type { CityRef } from '~/types/api'

/**
 * Province and city management.
 *
 * Not in the design's admin sidebar, but kept: every centre requires a city, so
 * without this screen a deployment whose seeded locations do not cover a new centre
 * has no way to add one.
 */
definePageMeta({ layout: 'dashboard', middleware: 'auth', role: 'ADMIN' })

interface Province { id: number, name: string }

const { $api } = useNuxtApp()
const toast = useToast()

const provinces = ref<Province[]>([])
const cities = ref<CityRef[]>([])
const activeProvince = ref<number | null>(null)
const loading = ref(true)
const saving = ref(false)

const newProvince = ref('')
const newCity = ref('')

async function loadProvinces() {
  provinces.value = await $api<Province[]>(ep.adminProvinces)
}

async function loadCities() {
  cities.value = activeProvince.value
    ? await $api<CityRef[]>(ep.adminCities, { query: { provinceId: activeProvince.value } })
    : []
}

onMounted(async () => {
  try {
    await loadProvinces()
    activeProvince.value = provinces.value[0]?.id ?? null
    await loadCities()
  }
  catch (error) { toast.error(apiErrorMessage(error)) }
  finally { loading.value = false }
})

watch(activeProvince, loadCities)

async function addProvince() {
  if (!newProvince.value.trim()) return
  saving.value = true
  try {
    await $api(ep.adminProvinces, { method: 'POST', body: { name: newProvince.value.trim() } })
    newProvince.value = ''
    await loadProvinces()
    toast.success('استان اضافه شد.')
  }
  catch (error) { toast.error(apiErrorMessage(error)) }
  finally { saving.value = false }
}

async function addCity() {
  if (!newCity.value.trim() || !activeProvince.value) return
  saving.value = true
  try {
    await $api(ep.adminCities, {
      method: 'POST',
      query: { provinceId: activeProvince.value },
      body: { name: newCity.value.trim() },
    })
    newCity.value = ''
    await loadCities()
    toast.success('شهر اضافه شد.')
  }
  catch (error) { toast.error(apiErrorMessage(error)) }
  finally { saving.value = false }
}

async function removeCity(city: CityRef) {
  try {
    await $api(ep.adminCity(city.id), { method: 'DELETE' })
    await loadCities()
    toast.success('شهر حذف شد.')
  }
  catch (error) { toast.error(apiErrorMessage(error)) }
}

async function removeProvince(province: Province) {
  try {
    await $api(ep.adminProvince(province.id), { method: 'DELETE' })
    await loadProvinces()
    if (activeProvince.value === province.id) activeProvince.value = provinces.value[0]?.id ?? null
    toast.success('استان حذف شد.')
  }
  catch (error) { toast.error(apiErrorMessage(error)) }
}

useHead({ title: 'استان‌ها و شهرها — پنل ادمین' })
</script>

<template>
  <div class="flex flex-col gap-6">
    <div class="flex flex-col gap-1">
      <h1 class="text-[24px] font-extrabold">استان‌ها و شهرها</h1>
      <p class="text-[14px] text-muted">
        شهر برای هر مرکز و هر درخواست الزامی است. استان یا شهری که در جایی استفاده شده باشد قابل حذف نیست.
      </p>
    </div>

    <div v-if="loading" class="card-flat p-6"><UiSkeleton :lines="4" /></div>

    <div v-else class="grid gap-6 lg:grid-cols-2 items-start">
      <section class="card-flat p-6 flex flex-col gap-4">
        <h2 class="text-[17px] font-bold">استان‌ها</h2>

        <div class="flex gap-2">
          <input v-model="newProvince" class="field" placeholder="نام استان جدید" @keyup.enter="addProvince">
          <button type="button" class="btn btn-primary btn-sm shrink-0" :disabled="saving" @click="addProvince">
            افزودن
          </button>
        </div>

        <ul class="flex flex-col divide-y max-h-[420px] overflow-y-auto" style="border-color: var(--color-surface-3)">
          <li
            v-for="province in provinces"
            :key="province.id"
            class="flex items-center justify-between gap-3 py-2.5"
          >
            <button
              type="button"
              class="text-[14px] flex-1 text-start"
              :class="activeProvince === province.id ? 'font-bold text-accent' : ''"
              @click="activeProvince = province.id"
            >{{ province.name }}</button>
            <button type="button" class="text-[12px] text-danger hover:underline" @click="removeProvince(province)">
              حذف
            </button>
          </li>
        </ul>
      </section>

      <section class="card-flat p-6 flex flex-col gap-4">
        <h2 class="text-[17px] font-bold">
          شهرهای {{ provinces.find(p => p.id === activeProvince)?.name ?? '—' }}
        </h2>

        <div class="flex gap-2">
          <input
            v-model="newCity"
            class="field"
            placeholder="نام شهر جدید"
            :disabled="!activeProvince"
            @keyup.enter="addCity"
          >
          <button
            type="button"
            class="btn btn-primary btn-sm shrink-0"
            :disabled="saving || !activeProvince"
            @click="addCity"
          >افزودن</button>
        </div>

        <UiEmptyState v-if="!cities.length" title="شهری ثبت نشده است" />

        <ul v-else class="flex flex-col divide-y max-h-[420px] overflow-y-auto" style="border-color: var(--color-surface-3)">
          <li v-for="city in cities" :key="city.id" class="flex items-center justify-between gap-3 py-2.5">
            <span class="text-[14px]">{{ city.name }}</span>
            <button type="button" class="text-[12px] text-danger hover:underline" @click="removeCity(city)">
              حذف
            </button>
          </li>
        </ul>
      </section>
    </div>
  </div>
</template>
