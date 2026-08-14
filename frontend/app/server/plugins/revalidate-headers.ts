/**
 * Makes the browser revalidate the pages that Nitro caches.
 *
 * Nitro emits `s-maxage=<n>, stale-while-revalidate` for an `swr` route and nothing else.
 * A response carrying neither `max-age` nor `expires` lets a browser invent its own
 * freshness lifetime from `last-modified`, so a reload can be answered out of the visitor's
 * own cache without ever reaching us. The symptom is precise: F5 shows the old page and
 * Ctrl+F5 shows the new one, because Ctrl+F5 is the one reload that ignores that cache.
 *
 * This cannot be fixed with a `headers` route rule. Nitro's cache layer assembles the
 * response headers and then assigns `cache-control` unconditionally
 * (nitropack/dist/runtime/internal/cache.mjs), so whatever the route rule set is discarded.
 * Rewriting it here, after the cache layer has had its say and before the response is
 * flushed, is what actually sticks.
 *
 * Nothing stops being cached. `s-maxage` still governs the server-side window and any CDN
 * placed in front later; `max-age=0` only obliges the browser to ask, and an unchanged page
 * answers 304 off the etag Nitro already sends.
 *
 * Keyed on the presence of `s-maxage` so it applies to exactly the cached pages. Static
 * assets carry `max-age=31536000, immutable` with no `s-maxage` and are left alone.
 */
export default defineNitroPlugin((nitro) => {
  nitro.hooks.hook('beforeResponse', (event) => {
    const current = event.node.res.getHeader('cache-control')
    if (typeof current !== 'string' || !current.includes('s-maxage')) {
      return
    }
    if (current.includes('max-age=0')) {
      return
    }
    event.node.res.setHeader('cache-control', `public, max-age=0, must-revalidate, ${current}`)
  })
})
