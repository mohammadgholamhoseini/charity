import { proxyRequest } from 'h3'

/**
 * Forwards browser API calls to the Java backend.
 *
 * Replaces the `location /api/` block the two nginx configs each carried. Doing it as
 * a runtime-configured route rather than a build-time `routeRules` proxy means one
 * image can serve both the dev and production stacks by environment variable alone,
 * instead of needing a separately-built image per backend host.
 *
 * Server-side renders bypass this entirely and call the backend directly — see the
 * api plugin — so there is no self-request hop during SSR.
 */
export default defineEventHandler(async (event) => {
  const { apiOrigin } = useRuntimeConfig(event)

  // Uploaded files are served through here; none of it should be indexed.
  setHeader(event, 'X-Robots-Tag', 'noindex')

  /**
   * Drop the browser's Origin before forwarding.
   *
   * The browser attaches Origin to every non-GET request, including same-origin ones.
   * Forwarded verbatim, it makes Spring's CORS filter treat a hop that is not
   * cross-origin at all — the browser talks only to this server, and this server talks
   * to the backend as an ordinary HTTP client — as a cross-origin one, and reject it
   * with 403 unless the site's own URL happens to be in `cors.allowed-origins`. That is
   * exactly how login broke on the dev deployment: GETs sent no Origin so the public
   * pages looked fine, while POST /auth/login answered 403 from behind the proxy and
   * 200 from curl.
   *
   * Stripping it does not weaken anything. CORS protects the browser, not the server:
   * a page on another origin calling this proxy still cannot read the response, because
   * we return no Access-Control-Allow-Origin. And the API authenticates with a bearer
   * token rather than a cookie, so a cross-site request carries no credentials to abuse.
   *
   * h3's mergeHeaders skips undefined values, so a header cannot be removed through
   * proxyRequest's options — it has to go before the request headers are collected.
   */
  delete event.node.req.headers.origin

  const method = event.method
  const result = await proxyRequest(event, `${apiOrigin}${event.path}`)

  // proxyRequest has already copied the upstream status onto the response by this point.
  if (mutates(method) && event.node.res.statusCode < 400 && !isAuthCall(event.path)) {
    await invalidateRenderedPages()
  }

  return result
})

function mutates(method: string) {
  return method !== 'GET' && method !== 'HEAD' && method !== 'OPTIONS'
}

/** Signing in changes nothing a public page renders, and it is the most frequent write. */
function isAuthCall(path: string) {
  return path.startsWith('/api/auth/')
}

/**
 * Drops the cached renders of the `swr` pages.
 *
 * Without this a centre publishes a request and then waits — up to five minutes on the
 * home page, two on the listing — watching a page that looks like nothing happened. The
 * cache is doing exactly what it was told; the missing half was ever telling it that the
 * content changed.
 *
 * This route is the right place to notice, and the only practical one. Every write from
 * either panel travels through here, because the panels are client-rendered and talk to
 * the API through this same origin. The alternative is the Java backend calling back into
 * Nitro after each publish, which means a shared secret, a URL each service has to know,
 * and a failure mode where a slow HTTP call is holding a database transaction open.
 *
 * The clear is deliberately not selective. A published request changes the home page and
 * the listing; a renamed category changes both plus the category pages; a new centre
 * changes `/centers`; an announcement changes every page that renders the banner. Any
 * mapping from endpoint to affected pages would be a list to forget to update, and the
 * cache is a handful of HTML documents that cost one render each to rebuild.
 *
 * `cache:` is Nitro's own mount for `defineCachedEventHandler` and the `swr` route rules,
 * which store under `cache:nitro:handlers:`. Nothing else in this app writes there.
 */
async function invalidateRenderedPages() {
  try {
    await useStorage('cache').clear()
  }
  catch {
    // A cache that would not clear is not a reason to fail a write that already succeeded:
    // the visitor's request went through, and the worst case is the delay we had before.
  }
}
