import type { LocationQuery } from 'vue-router'
import { computed, reactive } from 'vue'
import { beforeEach, describe, expect, it, vi } from 'vitest'

/**
 * The URL-is-the-filter-state contract, tested against the real composable.
 *
 * `useListQuery` reads `useRoute` / `useRouter` / `computed` as bare identifiers, because Nuxt
 * auto-imports them. Stubbing them as globals is what lets the actual source file run here
 * unmodified -- the alternative is a Nuxt test environment that takes seconds to boot to check
 * arithmetic on a query string.
 *
 * What is worth pinning: paging is 1-based in the URL and 0-based to the API (an off-by-one here
 * silently skips or repeats a page), `apply` drops empty values instead of writing `?city=`, and
 * every navigation is a `replace` -- a `push` per checkbox click makes the back button useless.
 */

const route = reactive<{ query: LocationQuery, params: Record<string, string> }>({
  query: {},
  params: {},
})

const replace = vi.fn()

vi.stubGlobal('computed', computed)
vi.stubGlobal('useRoute', () => route)
vi.stubGlobal('useRouter', () => ({ replace, push: vi.fn() }))

const { useListQuery } = await import('~/composables/useListQuery')

const SCHEMA = { category: { multi: true }, city: { multi: true }, q: {} }

/** The query object handed to `router.replace` by the last call. */
function lastReplacedQuery(): Record<string, unknown> {
  return replace.mock.calls.at(-1)![0].query
}

beforeEach(() => {
  route.query = {}
  replace.mockClear()
})

describe('reading the URL', () => {
  it('gives a multi facet an array and a single facet a string', () => {
    route.query = { category: ['darman', 'tahsil'], q: 'کمک' }
    const { filters } = useListQuery(SCHEMA)

    expect(filters.value.category).toEqual(['darman', 'tahsil'])
    expect(filters.value.q).toBe('کمک')
  })

  it('wraps a single occurrence of a multi facet into an array', () => {
    // vue-router hands back a bare string when a key appears once, and an array when it
    // repeats. Without the wrap, `.includes` would match substrings of a single value.
    route.query = { category: 'darman' }
    const { filters, isSelected } = useListQuery(SCHEMA)

    expect(filters.value.category).toEqual(['darman'])
    expect(isSelected('category', 'darman')).toBe(true)
    expect(isSelected('category', 'dar')).toBe(false)
  })

  it('defaults a missing facet to an empty array or an empty string, never undefined', () => {
    const { filters } = useListQuery(SCHEMA)

    expect(filters.value.category).toEqual([])
    expect(filters.value.q).toBe('')
  })

  it('ignores keys the schema does not declare', () => {
    route.query = { category: 'darman', utm_source: 'telegram' }
    const { filters } = useListQuery(SCHEMA)

    expect(Object.keys(filters.value)).toEqual(['category', 'city', 'q'])
  })
})

describe('paging', () => {
  it('is 1-based in the URL and 0-based to the API', () => {
    route.query = { page: '3' }
    const { page, apiQuery } = useListQuery(SCHEMA)

    expect(page.value).toBe(3)
    expect(apiQuery.value.page).toBe(2)
  })

  it('treats a missing, zero, negative or junk page as the first page', () => {
    for (const value of [undefined, '0', '-4', 'abc', '']) {
      route.query = value === undefined ? {} : { page: value }
      const { page, apiQuery } = useListQuery(SCHEMA)

      expect(page.value, `page=${value}`).toBe(1)
      expect(apiQuery.value.page, `page=${value}`).toBe(0)
    }
  })

  it('carries the page size the caller asked for', () => {
    const { apiQuery } = useListQuery(SCHEMA, 24)

    expect(apiQuery.value.size).toBe(24)
  })
})

describe('the API query', () => {
  it('omits facets that are not set, rather than sending empty values', () => {
    route.query = { category: 'darman' }
    const { apiQuery } = useListQuery(SCHEMA)

    expect(apiQuery.value).toEqual({ page: 0, size: 12, category: ['darman'] })
    expect(apiQuery.value).not.toHaveProperty('q')
    expect(apiQuery.value).not.toHaveProperty('city')
  })
})

describe('hasActiveFilters', () => {
  it('is false when only the page is set, so a paged listing stays indexable', () => {
    route.query = { page: '2' }
    const { hasActiveFilters } = useListQuery(SCHEMA)

    expect(hasActiveFilters.value).toBe(false)
  })

  it('is true as soon as any facet is applied', () => {
    route.query = { city: 'مشهد' }
    expect(useListQuery(SCHEMA).hasActiveFilters.value).toBe(true)

    route.query = { q: 'کمک' }
    expect(useListQuery(SCHEMA).hasActiveFilters.value).toBe(true)
  })
})

describe('writing the URL', () => {
  it('replaces rather than pushes', () => {
    // A push per checkbox click means the back button has to be pressed a dozen times to
    // leave the page.
    useListQuery(SCHEMA).apply({ q: 'کمک' })

    expect(replace).toHaveBeenCalledTimes(1)
  })

  it('drops the page when a filter changes', () => {
    // Page 5 of an unfiltered list is not page 5 of a filtered one; keeping it lands the
    // visitor on an empty page.
    route.query = { page: '5' }
    useListQuery(SCHEMA).apply({ q: 'کمک' })

    expect(lastReplacedQuery()).not.toHaveProperty('page')
  })

  it('keeps the page when explicitly told to', () => {
    route.query = { page: '5' }
    useListQuery(SCHEMA).apply({ q: 'کمک' }, { keepPage: true })

    expect(lastReplacedQuery().page).toBe('5')
  })

  it('removes a key instead of writing an empty value', () => {
    // `?q=&city=` is an ugly URL, a distinct cache key for the same page, and a duplicate
    // for a crawler.
    route.query = { q: 'کمک', category: ['darman'] }
    useListQuery(SCHEMA).apply({ q: '', category: [] })

    const next = lastReplacedQuery()
    expect(next).not.toHaveProperty('q')
    expect(next).not.toHaveProperty('category')
  })

  it('leaves the facets it was not asked about alone', () => {
    route.query = { category: ['darman'], q: 'کمک' }
    useListQuery(SCHEMA).apply({ q: 'درمان' })

    expect(lastReplacedQuery().category).toEqual(['darman'])
  })
})

describe('toggle', () => {
  it('adds a value that is not selected', () => {
    route.query = { category: ['darman'] }
    useListQuery(SCHEMA).toggle('category', 'tahsil')

    expect(lastReplacedQuery().category).toEqual(['darman', 'tahsil'])
  })

  it('removes a value that is', () => {
    route.query = { category: ['darman', 'tahsil'] }
    useListQuery(SCHEMA).toggle('category', 'darman')

    expect(lastReplacedQuery().category).toEqual(['tahsil'])
  })

  it('drops the key entirely when the last value is removed', () => {
    route.query = { category: ['darman'] }
    useListQuery(SCHEMA).toggle('category', 'darman')

    expect(lastReplacedQuery()).not.toHaveProperty('category')
  })
})

describe('clear', () => {
  it('clears one facet and leaves the rest', () => {
    route.query = { category: ['darman'], q: 'کمک' }
    useListQuery(SCHEMA).clear('category')

    const next = lastReplacedQuery()
    expect(next).not.toHaveProperty('category')
    expect(next.q).toBe('کمک')
  })

  it('clears everything, paging included, when given no key', () => {
    route.query = { category: ['darman'], q: 'کمک', page: '3' }
    useListQuery(SCHEMA).clear()

    expect(lastReplacedQuery()).toEqual({})
  })
})
