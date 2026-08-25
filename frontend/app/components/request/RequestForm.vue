<script setup lang="ts">
import type { StagedDocumentBatch } from '~/composables/useDocuments'
import type { CategoryRef, RequestDetail, Urgency } from '~/types/api'

/**
 * Create/edit form for a request.
 *
 * The category select is limited to the centre's own allowed categories — the backend
 * enforces the same rule, so offering anything wider would only produce a rejected
 * submission.
 *
 * A request carries no city, deadline, contact details or beneficiary name. The location and
 * phone number belong to the centre and are shown from there; the beneficiary is never named
 * anywhere, which is what the privacy page promises.
 */
const props = defineProps<{
  allowedCategories: CategoryRef[]
  initial?: RequestDetail | null
  submitting?: boolean
  /** Edit mode drops the draft/publish split; the request already has a status. */
  editing?: boolean
}>()

/**
 * The third argument carries files the centre picked here.
 *
 * In create mode there is no request id yet, so nothing can be uploaded from inside this form —
 * the files are staged and handed to the page, which creates the request first and posts them
 * against the new id. In edit mode the uploader lives on the page instead, outside this form,
 * so it stays reachable for a COMPLETED request or one under an admin takedown, where this form
 * is not usable at all.
 */
const emit = defineEmits<{
  submit: [payload: Record<string, unknown>, publish: boolean, documents: StagedDocumentBatch[]]
}>()

const stagedDocuments = ref<StagedDocumentBatch[]>([])

const form = reactive({
  title: props.initial?.title ?? '',
  categoryId: props.initial?.category?.id ?? null as number | null,
  urgency: (props.initial?.urgency ?? 'MEDIUM') as Urgency,
  amountNeeded: props.initial?.amountNeeded != null ? String(props.initial.amountNeeded) : '',
  description: props.initial?.description ?? '',
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
    urgency: form.urgency,
    amountNeeded: Number(form.amountNeeded),
    description: form.description.trim() || null,
  }
}

function send(publish: boolean) {
  if (!validate()) return
  emit('submit', buildPayload(), publish, stagedDocuments.value)
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

    <fieldset class="flex flex-col gap-3">
      <legend class="label">سطح فوریت</legend>
      <div class="grid grid-cols-4 gap-2">
        <button
          v-for="option in urgencyOptions"
          :key="option.value"
          type="button"
          class="py-3 rounded-[10px] text-[14px] border transition-colors"
          :class="form.urgency === option.value
            ? 'border-accent bg-accent-50 font-bold text-accent'
            : 'border-line-soft text-body hover:bg-surface-2'"
          :aria-pressed="form.urgency === option.value"
          @click="form.urgency = option.value"
        >
          {{ option.label }}
        </button>
      </div>
    </fieldset>

    <UiField
      v-model="form.amountNeeded"
      label="مبلغ مورد نیاز (تومان)"
      required
      ltr
      :error="errors.amountNeeded"
      placeholder="450000000"
      hint="این مبلغ به بازدیدکننده نشان داده می‌شود."
    />

    <UiField
      v-model="form.description"
      label="شرح نیاز"
      textarea
      :rows="4"
      :maxlength="3000"
      counter
      placeholder="توضیح دهید نیاز چیست، چرا ضروری است و مرکز چه بررسی‌ای انجام داده است."
      hint="آوردن نام، تلفن یا نشانی مددجو اختیاری و بر عهدهٔ شماست؛ فقط در صورتی که رضایت کتبی او را از پیش گرفته باشید."
    />

    <!-- Create mode only: there is no request id to attach a document to yet, so the files are
         staged here and the page posts them once the request exists. On the edit page the
         uploader sits outside this form. -->
    <div v-if="!editing" class="border-t border-surface-3 pt-6">
      <DocumentUploader
        scope="REQUEST"
        heading="مدارک درخواست"
        description="مدارک پس از ثبت درخواست بارگذاری می‌شوند."
        @update:staged="stagedDocuments = $event"
      />
    </div>

    <p class="help">
      شهر و اطلاعات تماس از پروفایل مرکز شما نمایش داده می‌شود. یاری‌جو فیلد جداگانه‌ای برای
      مشخصات مددجو ندارد؛ اگر چنین مشخصاتی را در متن شرح نیاز بیاورید، مسئولیتش با شماست و باید
      رضایت کتبی مددجو را از پیش گرفته باشید. مدارکی هم که بارگذاری می‌کنید در صفحه عمومی درخواست
      و در کانال‌های اطلاع‌رسانی منتشر می‌شود؛ پیش از بارگذاری مطمئن شوید نام، کد ملی، نشانی و
      دیگر مشخصات هویتی مددجو روی آن‌ها دیده نمی‌شود.
    </p>

    <div class="flex flex-wrap items-center gap-3 border-t border-surface-3 pt-6">
      <button type="submit" class="btn btn-primary" :disabled="submitting">
        {{ submitting ? 'در حال ارسال…' : (editing ? 'ذخیره تغییرات' : 'انتشار درخواست') }}
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
        درخواست بلافاصله در سایت منتشر می‌شود. پیش‌نویس منتشر نمی‌شود و هر زمان می‌توانید آن را
        منتشر کنید.
      </p>
    </div>
  </form>
</template>
