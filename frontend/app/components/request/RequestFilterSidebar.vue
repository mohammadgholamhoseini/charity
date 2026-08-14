<script setup lang="ts">
import type { CategoryResponse, CityRef } from '~/types/api'

/**
 * The filter sidebar.
 *
 * Fully controlled: it fetches nothing and touches no router state itself. That keeps
 * the query-string synchronisation in exactly one place — the page — and makes this
 * component reusable for the category and city landing pages, where one facet is
 * fixed by the URL rather than chosen here.
 */
defineProps<{
  categories: CategoryResponse[]
  cities: CityRef[]
  isSelected: (key: string, value: string) => boolean
  hideCategory?: boolean
  hideCity?: boolean
}>()

const emit = defineEmits<{ toggle: [key: string, value: string], clear: [] }>()

const urgencies = [
  { value: 'URGENT', label: 'فوری' },
  { value: 'HIGH', label: 'بالا' },
  { value: 'MEDIUM', label: 'متوسط' },
  { value: 'LOW', label: 'کم' },
]

const statuses = [
  { value: 'PUBLISHED', label: 'منتشرشده' },
  { value: 'COMPLETED', label: 'تکمیل‌شده' },
]
</script>

<template>
  <aside class="card-flat p-6 self-start flex flex-col gap-7">
    <div class="flex items-center justify-between">
      <h2 class="text-[17px] font-bold">فیلترها</h2>
      <button type="button" class="text-[13px] text-accent hover:text-accent-600" @click="emit('clear')">
        پاک کردن
      </button>
    </div>

    <fieldset v-if="!hideCategory" class="flex flex-col gap-3">
      <legend class="label">دسته‌بندی</legend>
      <label
        v-for="category in categories"
        :key="category.id"
        class="flex items-center gap-3 cursor-pointer text-[14px] min-h-[28px]"
      >
        <input
          type="checkbox"
          class="w-[17px] h-[17px] rounded-[5px] accent-[var(--color-accent)]"
          :checked="isSelected('category', category.slug)"
          @change="emit('toggle', 'category', category.slug)"
        >
        <span class="flex-1">{{ category.name }}</span>
        <span class="text-[12px] text-muted-2">{{ category.activeRequestCount }}</span>
      </label>
    </fieldset>

    <fieldset class="flex flex-col gap-3">
      <legend class="label">سطح فوریت</legend>
      <label
        v-for="urgency in urgencies"
        :key="urgency.value"
        class="flex items-center gap-3 cursor-pointer text-[14px] min-h-[28px]"
      >
        <input
          type="checkbox"
          class="w-[17px] h-[17px] rounded-[5px] accent-[var(--color-accent)]"
          :checked="isSelected('urgency', urgency.value)"
          @change="emit('toggle', 'urgency', urgency.value)"
        >
        {{ urgency.label }}
      </label>
    </fieldset>

    <fieldset v-if="!hideCity && cities.length" class="flex flex-col gap-3">
      <legend class="label">شهر</legend>
      <div class="max-h-[240px] overflow-y-auto flex flex-col gap-3 pe-1">
        <label
          v-for="city in cities"
          :key="city.id"
          class="flex items-center gap-3 cursor-pointer text-[14px] min-h-[28px]"
        >
          <input
            type="checkbox"
            class="w-[17px] h-[17px] rounded-[5px] accent-[var(--color-accent)]"
            :checked="isSelected('city', city.name)"
            @change="emit('toggle', 'city', city.name)"
          >
          {{ city.name }}
        </label>
      </div>
    </fieldset>

    <fieldset class="flex flex-col gap-3">
      <legend class="label">وضعیت</legend>
      <label
        v-for="status in statuses"
        :key="status.value"
        class="flex items-center gap-3 cursor-pointer text-[14px] min-h-[28px]"
      >
        <input
          type="checkbox"
          class="w-[17px] h-[17px] rounded-[5px] accent-[var(--color-accent)]"
          :checked="isSelected('status', status.value)"
          @change="emit('toggle', 'status', status.value)"
        >
        {{ status.label }}
      </label>
      <p class="help">
        درخواست‌های «در انتظار انتشار» و «رد شده» در سایت نمایش داده نمی‌شوند.
      </p>
    </fieldset>
  </aside>
</template>
