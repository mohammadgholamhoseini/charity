/**
 * Reads a Persian slug out of the route.
 *
 * Route params arrive percent-encoded, so they are decoded exactly once here and then
 * travel as raw text: the endpoint map does not encode, and the URL layer encodes on
 * the way out. Getting this wrong in either direction is silent — double-encoding
 * produces a path the backend cannot match, and comparing an encoded param against
 * the API's raw slug makes the canonical check fire a redirect on every single load.
 */
export function useRouteSlug(param = 'slug') {
  const route = useRoute()

  return computed(() => {
    const raw = String(route.params[param] ?? '')
    try {
      return decodeURIComponent(raw)
    }
    catch {
      // A malformed escape sequence is not worth throwing over; treat it as literal.
      return raw
    }
  })
}
