<script setup lang="ts">
import type { CategoryRef, CityRef, RequestDetail, Urgency } from '~/types/api'

/**
 * Create/edit form for a request.
 *
 * The category select is limited to the centre's own allowed categories — the backend
 * enforces the same rule, so offering anything wider would only produce a rejected
 * submission.
 */
const props = defineProps<{
  allowedCategories: CategoryRef[]
  cities: CityRef[]
  initial?: RequestDetail | null
  submitting?: boolean
  /** Edit mode drops the draft/submit split; the request already has a status. */
  editing?: boolean
}>()

const emit = defineEmits<{ submit: [payload: Record<string, unknown>, submitForReview: boolean] }>()

const form = reactive({
  title: props.initial?.title ?? '',
  categoryId: props.initial?.category?.id ?? null as number | null,
  cityId: props.initial?.city?.id ?? null as number | null,
  urgency: (props.initial?.urgency ?? 'MEDIUM') as Urgency,
  amountNeeded: props.initial?.amountNeeded != null ? String(props.initial.amountNeeded) : '',
  deadline: props.initial?.deadline ?? '',
  description: props.initial?.description ?? '',
  contactInfo: props.initial?.contactInfo ?? '',
  beneficiaryName: (props.initial?.details?.beneficiaryName as string) ?? '',
})

const errors = reactive<Record<string, string>>({})

const urgencyOptions: { value: Urgency, label: string }[] = [
  { value: 'LOW', label: 'کم' },
  { value: 'MEDIUM', label: 'متوسط' },
  { value: 'HIGH', label: 'بالا' },
  { value: 'URGENT', label: 'فوری' },
]

function validate(): boolean {
  Object.keys(errors).forEach(key => delete errors[key])
  if (!form.title.trim()) errors.title = 'عنوان درخواست الزامی است'
  if (!form.categoryId) errors.categoryId = 'انتخاب دسته‌بندی الزامی است'
  if (!form.cityId) errors.cityId = 'انتخاب شهر الزامی است'
  const amount = Number(form.amountNeeded)
  if (!form.amountNeeded || Number.isNaN(amount) || amount <= 0) {
    errors.amountNeeded = 'مبلغ مورد نیاز را به عدد وارد کنید'
  }
  return Object.keys(errors).length === 0
}

function buildPayload() {
  return {
    title: form.title.trim(),
    categoryId: form.categoryId,
    cityId: form.cityId,
    urgency: form.urgency,
    amountNeeded: Number(form.amountNeeded),
    deadline: form.deadline || null,
    description: form.description.trim() || null,
    contactInfo: form.contactInfo.trim() || null,
    details: form.beneficiaryName.trim() ? { beneficiaryName: form.beneficiaryName.trim() } : {},
  }
}

function send(submitForReview: boolean) {
  if (!validate()) return
  emit('submit', buildPayload(), submitForReview)
}
</script>

<template>
  <form class="card-flat p-6 lg:p-8 flex flex-col gap-6" novalidate @submit.prevent="send(true)">
    <div class="flex flex-col gap-1">
      <h2 class="text-[20px] font-bold">{{ editing ? 'ویرایش درخواست' : 'ثبت درخواست جدید' }}</h2>
      <p class="help">دسته‌بندی را از فهرست دسته‌های مجاز مرکز انتخاب کنید.</p>
    </div>

    <UiField
      v-model="form.title"
      label="عنوان درخواست"
      required
      :error="errors.title"
      :maxlength="255"
      placeholder="مثلاً: جراحی قلب برای کودک ۷ ساله"
    />

    <div class="grid gap-5 sm:grid-cols-2">
      <div class="flex flex-col">
        <label class="label" for="category">دسته‌بندی <span class="text-danger">*</span></label>
        <select id="category" v-model="form.categoryId" class="field" :class="{ 'is-error': errors.categoryId }">
          <option :value="null" disabled>انتخاب کنید…</option>
          <option v-for="category in allowedCategories" :key="category.id" :value="category.id">
            {{ category.name }}
          </option>
        </select>
        <span v-if="errors.categoryId" class="error-text mt-2">{{ errors.categoryId }}</span>
        <span v-else-if="!allowedCategories.length" class="help mt-2">
          هنوز دسته‌بندی مجازی برای مرکز شما تعیین نشده است. با ادمین تماس بگیرید.
        </span>
      </div>

      <div class="flex flex-col">
        <label class="label" for="city">شهر <span class="text-danger">*</span></label>
        <select id="city" v-model="form.cityId" class="field" :class="{ 'is-error': errors.cityId }">
          <option :value="null" disabled>انتخاب کنید…</option>
          <option v-for="city in cities" :key="city.id" :value="city.id">
            {{ city.name }}<template v-if="city.provinceName"> — {{ city.provinceName }}</template>
          </option>
        </select>
        <span v-if="errors.cityId" class="error-text mt-2">{{ errors.cityId }}</span>
      </div>
    </div>

    <fieldset class="flex flex-col gap-3">
      <legend class="label">سطح فوریت</legend>
      <div class="grid grid-cols-4 gap-2">
        <button
          v-for="option in urgencyOptions"
          :key="option.value"
          type="button"
          class="py-3 rounded-[10px] text-[14px] border transition-colors"
          :class="form.urgency === option.value
            ? 'border-brick-500 bg-brick-50 font-bold text-brick-500'
            : 'border-line-soft text-body hover:bg-cream-100'"
          :aria-pressed="form.urgency === option.value"
          @click="form.urgency = option.value"
        >
          {{ option.label }}
        </button>
      </div>
    </fieldset>

    <div class="grid gap-5 sm:grid-cols-2">
      <UiField
        v-model="form.amountNeeded"
        label="مبلغ مورد نیاز (تومان)"
        required
        ltr
        :error="errors.amountNeeded"
        placeholder="450000000"
        hint="این مبلغ به بازدیدکننده نشان داده می‌شود."
      />
      <UiField v-model="form.deadline" label="مهلت" type="date" ltr />
    </div>

    <UiField
      v-model="form.description"
      label="شرح نیاز"
      textarea
      :rows="4"
      :maxlength="3000"
      counter
      placeholder="توضیح دهید نیاز چیست، چرا ضروری است و مرکز چه بررسی‌ای انجام داده است."
    />

    <div class="grid gap-5 sm:grid-cols-2">
      <UiField
        v-model="form.beneficiaryName"
        label="نام مددجو (منتشر نمی‌شود)"
        hint="فقط برای بررسی داخلی و پیام‌رسان‌ها استفاده می‌شود."
      />
      <UiField v-model="form.contactInfo" label="اطلاعات تماس" :maxlength="500" />
    </div>

    <div class="flex flex-wrap items-center gap-3 border-t border-cream-200 pt-6">
      <button type="submit" class="btn btn-primary" :disabled="submitting">
        {{ submitting ? 'در حال ارسال…' : (editing ? 'ذخیره تغییرات' : 'ارسال برای بررسی') }}
      </button>
      <button
        v-if="!editing"
        type="button"
        class="btn btn-secondary"
        :disabled="submitting"
        @click="send(false)"
      >
        ذخیره پیش‌نویس
      </button>
      <p v-if="!editing" class="help">
        پیش‌نویس در سایت منتشر نمی‌شود و هر زمان می‌توانید آن را برای بررسی ارسال کنید.
      </p>
    </div>
  </form>
</template>
