<script setup lang="ts">
import { documentExtension, documentLabel } from '~/composables/useDocuments'
import type { DocumentFile } from '~/types/api'

/**
 * The view of a set of documents — read-only by default, which is how the request page and the
 * centre page use it. `deletable` turns on a delete button per row for the admin panel; it takes
 * a plain list rather than either owner, so all three callers share it.
 *
 * The API sends one flat list already ordered by category sort order and then by position, so
 * the grouping here only has to preserve the order it arrives in: a Map keyed by category id
 * keeps insertion order and needs no sorting of its own.
 *
 * Every `url` is absolute — the API builds it with AppUrls.fileUrl — so it goes straight into
 * the href. `nofollow` because an uploaded PDF is not a page we are recommending, and
 * `noopener` because these open in a new tab.
 */
const props = withDefaults(defineProps<{
  documents: DocumentFile[]
  /**
   * Panel-only. Adds a delete affordance beside each row and emits `delete`; the parent owns the
   * confirmation and the API call. Off by default, so the two public pages that render this
   * component server-side are untouched by it.
   */
  deletable?: boolean
}>(), { deletable: false })

defineEmits<{ delete: [doc: DocumentFile] }>()

const { fileSize } = useFormat()

const groups = computed(() => {
  const byCategory = new Map<number, { name: string, items: DocumentFile[] }>()
  for (const doc of props.documents) {
    const key = doc.category?.id ?? 0
    const group = byCategory.get(key)
    if (group) group.items.push(doc)
    else byCategory.set(key, { name: doc.category?.name ?? 'سایر مدارک', items: [doc] })
  }
  return [...byCategory.entries()].map(([id, group]) => ({ id, ...group }))
})
</script>

<template>
  <div class="flex flex-col gap-5">
    <section v-for="group in groups" :key="group.id" class="flex flex-col gap-2">
      <h3 class="text-[14px] font-bold text-body-2">{{ group.name }}</h3>

      <ul class="flex flex-col divide-y" style="border-color: var(--color-surface-3)">
        <li v-for="doc in group.items" :key="doc.id" class="py-2.5 first:pt-0 last:pb-0 flex items-center gap-3">
          <a
            :href="doc.url"
            target="_blank"
            rel="noopener nofollow"
            class="flex-1 min-w-0 flex items-center gap-3 group"
          >
            <!-- Static rules keyed on the attribute; the value is never interpolated into a
                 class name, because Tailwind emits nothing for a class it cannot see. -->
            <span class="doc-ext shrink-0" :data-ext="documentExtension(doc)" aria-hidden="true">
              {{ documentExtension(doc) }}
            </span>
            <span class="min-w-0 flex flex-col">
              <span class="text-[14px] leading-7 truncate group-hover:text-accent">
                {{ documentLabel(doc) }}
              </span>
              <span v-if="doc.sizeBytes != null" class="text-[12px] text-muted">
                {{ fileSize(doc.sizeBytes) }}
              </span>
            </span>
          </a>
          <button
            v-if="deletable"
            type="button"
            class="shrink-0 text-danger text-[13px] hover:underline"
            :aria-label="`حذف ${documentLabel(doc)}`"
            @click="$emit('delete', doc)"
          >حذف</button>
        </li>
      </ul>
    </section>
  </div>
</template>
