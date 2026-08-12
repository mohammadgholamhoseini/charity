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

  return proxyRequest(event, `${apiOrigin}${event.path}`)
})
