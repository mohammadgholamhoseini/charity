/**
 * Makes the browser revalidate the pages Nitro caches, on every response including 304s.
 *
 * Nitro emits `s-maxage=<n>, stale-while-revalidate` and no `max-age` for an `swr` route. A
 * response with neither `max-age` nor `expires` lets a browser invent its own freshness
 * lifetime from `last-modified` and answer a reload out of its own cache without asking us.
 * The giveaway is F5 showing the old page while Ctrl+F5 shows the new one.
 *
 * Two earlier attempts missed, and both are worth knowing about:
 *
 *   - A `headers` route rule is discarded. Nitro's cache layer assembles the response
 *     headers and then assigns `cache-control` unconditionally, after route rules run.
 *   - A `beforeResponse` hook fixes the 200 and never sees the 304. h3's
 *     `handleCacheHeaders` writes `public, max-age=<n>, s-maxage=<n>` and calls
 *     `res.end()` itself, so the response never reaches the hook. That 304 is the whole
 *     bug: a 304's headers *replace* those on the browser's stored copy, so the first
 *     reload revalidated correctly and was told `max-age=300` — and every reload for the
 *     next five minutes was served from the browser's cache without a request at all.
 *
 * Wrapping `setHeader` catches every path, because both the cached-response replay and
 * `handleCacheHeaders` go through it.
 *
 * `stale-while-revalidate` is dropped rather than carried over. Nitro emits it with no
 * value, which is not a usable directive, and its only possible effect on a browser is
 * serving the stale copy we are trying to stop serving. `s-maxage` is preserved, so the
 * server-side window and any future CDN still cache; only the browser is obliged to ask,
 * and an unchanged page still costs one 304.
 *
 * Untouched: anything without `s-maxage`, which is every static asset
 * (`max-age=31536000, immutable`) and every uncached page.
 */
const SHARED_MAX_AGE = /s-maxage=(\d+)/

export default defineEventHandler((event) => {
  const res = event.node.res
  const setHeader = res.setHeader.bind(res)

  res.setHeader = function (name: string, value: never) {
    if (String(name).toLowerCase() === 'cache-control' && typeof value === 'string') {
      const shared = SHARED_MAX_AGE.exec(value)
      if (shared) {
        return setHeader(name, `public, max-age=0, must-revalidate, s-maxage=${shared[1]}` as never)
      }
    }
    return setHeader(name, value)
  } as typeof res.setHeader
})
