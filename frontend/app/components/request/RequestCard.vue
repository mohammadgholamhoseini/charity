<script setup lang="ts">
import type { RequestSummary } from '~/types/api'

/**
 * A request card.
 *
 * Deliberately has no image: the design omits photography from request cards
 * entirely, which also keeps the beneficiary unidentifiable.
 */
const props = defineProps<{ request: RequestSummary, compact?: boolean }>()

const { toman, shortDate, isoDate } = useFormat()

const to = computed(() => `/requests/${encodeURIComponent(props.request.slug)}`)
</script>

<template>
  <article class="card p-6 flex flex-col gap-4 h-full">
    <div class="flex flex-wrap items-center gap-2">
      <UiChip
        v-if="request.category"
        :label="request.category.name"
        :color="{ bg: request.category.labelBg, text: request.category.labelText }"
      />
      <UiChip :label="request.statusLabel" :status="request.status" />
      <UiChip :label="`فوریت: ${request.urgencyLabel}`" :urgency="request.urgency" />
    </div>

    <h3 class="text-[20px] font-bold leading-[1.7]" :class="{ 'text-[17px]': compact }">
      <NuxtLink :to="to" class="hover:text-brick-500 transition-colors">{{ request.title }}</NuxtLink>
    </h3>

    <p v-if="request.summary" class="text-[14px] leading-7 text-body line-clamp-3">
      {{ request.summary }}
    </p>

    <!-- Meta table. The amount row is an addition to the design: requests carry a
         required amount, and a visitor needs to see the scale of the need. -->
    <dl class="border-t border-cream-200 pt-4 mt-auto flex flex-col gap-2.5 text-[13px]">
      <div v-if="request.center" class="flex items-center justify-between gap-3">
        <dt class="text-muted">مرکز ثبت‌کننده</dt>
        <dd class="text-body-2 font-semibold truncate">{{ request.center.name }}</dd>
      </div>
      <div v-if="request.center?.cityName" class="flex items-center justify-between gap-3">
        <dt class="text-muted">شهر</dt>
        <dd class="text-body-2">{{ request.center.cityName }}</dd>
      </div>
      <div class="flex items-center justify-between gap-3">
        <dt class="text-muted">مبلغ مورد نیاز</dt>
        <dd class="text-body-2 font-semibold">{{ toman(request.amountNeeded) }}</dd>
      </div>
      <div class="flex items-center justify-between gap-3">
        <dt class="text-muted">تاریخ ثبت</dt>
        <dd class="text-body-2">
          <time :datetime="isoDate(request.publishedAt ?? request.createdAt)">
            {{ shortDate(request.publishedAt ?? request.createdAt) }}
          </time>
        </dd>
      </div>
    </dl>

    <NuxtLink :to="to" class="btn btn-secondary w-full">جزئیات و تماس با مرکز</NuxtLink>
  </article>
</template>
