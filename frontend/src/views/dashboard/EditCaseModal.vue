<script setup>
import { ref, watch } from 'vue'
import api from '../../api/client'
import { Plus, X, Save, Loader2, FileText } from '@lucide/vue'

const props = defineProps({
  caseId: { type: [Number, String], required: true },
  initial: { type: Object, required: true },
  categories: { type: Array, default: () => [] },
  endpoint: { type: String, default: '/center/cases' }
})
const emit = defineEmits(['saved', 'close'])

const form = ref({
  title: '', categoryId: '', description: '', amountNeeded: '',
  imageUrl: '', contactInfo: '', urgency: 'MEDIUM', details: {}
})
const detailKeys = ref([])
const error = ref('')
const saving = ref(false)

function prefill() {
  const c = props.initial
  form.value = {
    title: c.title || '',
    categoryId: c.categoryId || '',
    description: c.description || '',
    amountNeeded: c.amountNeeded ?? '',
    imageUrl: c.imageUrl || '',
    contactInfo: c.contactInfo || '',
    urgency: c.urgency || 'MEDIUM',
    details: {}
  }
  const details = c.details || {}
  detailKeys.value = []
  for (const [k, v] of Object.entries(details)) {
    if (k === 'beneficiaryName') {
      form.value.details.beneficiaryName = v
    } else {
      detailKeys.value.push({ key: k, value: String(v) })
    }
  }
}

function addDetail() {
  detailKeys.value.push({ key: '', value: '' })
}
function removeDetail(i) {
  detailKeys.value.splice(i, 1)
}
function buildDetails() {
  const d = { ...(form.value.details || {}) }
  for (const row of detailKeys.value) {
    if (row.key.trim()) d[row.key.trim()] = row.value
  }
  return Object.keys(d).length ? d : null
}

async function save() {
  error.value = ''
  saving.value = true
  try {
    const payload = {
      title: form.value.title,
      categoryId: Number(form.value.categoryId),
      description: form.value.description || null,
      amountNeeded: Number(form.value.amountNeeded),
      imageUrl: form.value.imageUrl || null,
      contactInfo: form.value.contactInfo || null,
      urgency: form.value.urgency,
      details: buildDetails()
    }
    await api.put(`${props.endpoint}/${props.caseId}`, payload)
    emit('saved')
  } catch (e) {
    error.value = e.response?.data?.message || 'خطا در ذخیره تغییرات.'
  } finally {
    saving.value = false
  }
}

watch(() => props.initial, prefill, { immediate: true })
</script>

<template>
  <div class="fixed inset-0 z-50 bg-black/40 backdrop-blur-sm flex items-center justify-center p-4" @click.self="$emit('close')">
    <transition name="modal" appear>
      <div class="glass rounded-3xl w-full max-w-2xl max-h-[90vh] overflow-y-auto p-6 sm:p-8 shadow-2xl">
        <div class="flex items-center justify-between mb-5">
          <h2 class="text-xl font-bold text-slate-800 dark:text-white">ویرایش درخواست</h2>
          <button @click="$emit('close')" class="grid place-items-center w-9 h-9 rounded-xl text-slate-400 hover:bg-slate-100 dark:hover:bg-slate-800">
            <X :size="20" />
          </button>
        </div>

        <form @submit.prevent="save" class="space-y-4">
          <div>
            <label class="label">عنوان درخواست</label>
            <input v-model="form.title" required placeholder="عنوان"
              class="input" />
          </div>

          <div>
            <label class="label">دسته‌بندی</label>
            <select v-model="form.categoryId" required
              class="input bg-white dark:bg-slate-900">
              <option value="">دسته‌بندی را انتخاب کنید</option>
              <option v-for="c in categories" :key="c.id" :value="c.id">{{ c.name }}</option>
            </select>
          </div>

          <div>
            <label class="label">نام ذینفع (اختیاری)</label>
            <input v-model="form.details.beneficiaryName" placeholder="نام ذینفع"
              class="input" />
          </div>

          <div>
            <label class="label">توضیحات تکمیلی</label>
            <textarea v-model="form.description" rows="3" placeholder="توضیحات"
              class="input resize-none"></textarea>
          </div>

          <div>
            <label class="label">سطح فوریت</label>
            <select v-model="form.urgency" class="input bg-white dark:bg-slate-900">
              <option value="LOW">کم</option>
              <option value="MEDIUM">متوسط</option>
              <option value="HIGH">بالا</option>
              <option value="URGENT">فوری</option>
            </select>
          </div>

          <div class="grid sm:grid-cols-2 gap-4">
            <div>
              <label class="label">مبلغ مورد نیاز (تومان)</label>
              <input v-model="form.amountNeeded" type="number" required placeholder="مبلغ"
                class="input" />
            </div>
            <div>
              <label class="label">اطلاعات تماس</label>
              <input v-model="form.contactInfo" placeholder="تماس"
                class="input" />
            </div>
          </div>

          <div>
            <label class="label">آدرس تصویر (اختیاری - URL)</label>
            <input v-model="form.imageUrl" placeholder="https://..."
              class="input" />
          </div>

          <div class="border border-dashed border-slate-200 dark:border-slate-700 rounded-2xl p-4">
            <div class="flex items-center justify-between mb-3">
              <span class="text-sm font-medium text-slate-600 dark:text-slate-300 flex items-center gap-1.5"><FileText :size="15" class="text-brand-500" /> فیلدهای اختصاصی (اختیاری)</span>
              <button type="button" @click="addDetail" class="text-xs inline-flex items-center gap-1 text-brand-700 dark:text-brand-300 font-medium hover:underline">
                <Plus :size="13" /> افزودن فیلد
              </button>
            </div>
            <div v-for="(row, i) in detailKeys" :key="i" class="grid grid-cols-2 gap-2 mb-2 items-center">
              <input v-model="row.key" placeholder="نام فیلد"
                class="px-3 py-2 rounded-xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-900 text-sm focus:border-brand-500 outline-none" />
              <div class="flex gap-2">
                <input v-model="row.value" placeholder="مقدار"
                  class="flex-1 px-3 py-2 rounded-xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-900 text-sm focus:border-brand-500 outline-none" />
                <button type="button" @click="removeDetail(i)" class="grid place-items-center w-9 h-9 rounded-xl text-red-500 hover:bg-red-50 dark:hover:bg-red-900/20">
                  <X :size="16" />
                </button>
              </div>
            </div>
          </div>

          <p class="text-xs text-slate-400 dark:text-slate-500">مدارک از طریق فرم ثبت جدید قابل بارگذاری است.</p>

          <p v-if="error" class="text-red-500 text-sm bg-red-50 dark:bg-red-900/20 rounded-xl py-2.5 px-3">{{ error }}</p>

          <div class="flex gap-3 pt-2">
            <button :disabled="saving" type="submit" class="btn-primary flex-1">
              <Loader2 v-if="saving" :size="18" class="animate-spin" />
              <Save v-else :size="18" />
              {{ saving ? 'در حال ذخیره...' : 'ذخیره تغییرات' }}
            </button>
            <button type="button" @click="$emit('close')" class="btn-ghost">انصراف</button>
          </div>
        </form>
      </div>
    </transition>
  </div>
</template>

<style scoped>
.modal-enter-active, .modal-leave-active { transition: opacity 0.2s ease, transform 0.2s ease; }
.modal-enter-from, .modal-leave-to { opacity: 0; transform: scale(0.96) translateY(10px); }
</style>
