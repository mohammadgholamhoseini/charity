import type { LocationQuery } from 'vue-router'

type FieldSpec = { multi?: boolean }
type Schema = Record<string, FieldSpec>

export type Filters = Record<string, string | string[]>

/**
 * Keeps list filters in the URL.
 *
 * The query string is the single source of truth, so a filtered listing is
 * shareable, survives a refresh, and works with the back button. Because `useFetch`
 * watches a reactive `query`, ticking a checkbox writes the URL and the refetch
 * follows on its own.
 */
export function useListQuery(schema: Schema, pageSize = 12) {
  const route = useRoute()
  const router = useRouter()

  function read(query: LocationQuery): Filters {
    const result: Filters = {}
    for (const [key, spec] of Object.entries(schema)) {
      const raw = query[key]
      if (spec.multi) {
        result[key] = raw == null ? [] : (Array.isArray(raw) ? raw : [raw]).filter(Boolean) as string[]
      }
      else {
        result[key] = (Array.isArray(raw) ? raw[0] : raw) ?? ''
      }
    }
    return result
  }

  const filters = computed(() => read(route.query))

  /** Pages are 1-based in the URL (what a person expects) and 0-based to the API. */
  const page = computed(() => Math.max(1, Number(route.query.page) || 1))

  const apiQuery = computed(() => {
    const query: Record<string, unknown> = { page: page.value - 1, size: pageSize }
    for (const [key, value] of Object.entries(filters.value)) {
      if (Array.isArray(value) ? value.length : value) query[key] = value
    }
    return query
  })

  /** True when anything other than paging is applied — drives the noindex decision. */
  const hasActiveFilters = computed(() =>
    Object.values(filters.value).some(v => (Array.isArray(v) ? v.length > 0 : Boolean(v))),
  )

  function apply(patch: Partial<Filters>, options: { keepPage?: boolean } = {}) {
    const next: Record<string, unknown> = { ...route.query, ...patch }
    if (!options.keepPage) delete next.page

    for (const key of Object.keys(next)) {
      const value = next[key]
      if (value == null || value === '' || (Array.isArray(value) && value.length === 0)) {
        delete next[key]
      }
    }
    // replace, not push: otherwise every checkbox click becomes a history entry and
    // the back button has to be pressed a dozen times to leave the page.
    router.replace({ query: next as LocationQuery })
  }

  /** Toggles one value of a multi-select facet. */
  function toggle(key: string, value: string) {
    const current = filters.value[key]
    const list = Array.isArray(current) ? [...current] : []
    const index = list.indexOf(value)
    if (index >= 0) list.splice(index, 1)
    else list.push(value)
    apply({ [key]: list })
  }

  function isSelected(key: string, value: string) {
    const current = filters.value[key]
    return Array.isArray(current) ? current.includes(value) : current === value
  }

  function clear(key?: string) {
    if (key) apply({ [key]: Array.isArray(filters.value[key]) ? [] : '' })
    else router.replace({ query: {} })
  }

  function goToPage(next: number) {
    apply({}, { keepPage: true })
    router.replace({ query: { ...route.query, page: next <= 1 ? undefined : String(next) } as LocationQuery })
  }

  return { filters, page, apiQuery, hasActiveFilters, apply, toggle, isSelected, clear, goToPage }
}
