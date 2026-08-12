import { ep } from '~/api/endpoints'

/**
 * The single HTTP client.
 *
 * Replaces the old axios singleton, with two of its bugs fixed: a 401 now clears the
 * reactive auth state as well as storage (previously the store kept a stale token in
 * memory, so guards and the UI still believed the user was signed in until a full
 * reload), and the redirect goes through the router rather than assigning
 * `window.location.href`, which threw away the SPA and the intended destination.
 */
export default defineNuxtPlugin(() => {
  const config = useRuntimeConfig()
  const auth = useAuth()

  const api = $fetch.create({
    // During SSR, talk to the backend directly. Going through our own /api proxy
    // would mean the server issuing an HTTP request to itself for every render.
    baseURL: import.meta.server
      ? `${config.apiOrigin}/api`
      : config.public.apiBase,
    retry: 0,

    onRequest({ options, request }) {
      if (import.meta.client && auth.token.value && String(request) !== ep.login) {
        options.headers.set('Authorization', `Bearer ${auth.token.value}`)
      }
    },

    onResponseError({ response }) {
      if (!import.meta.client || response.status !== 401) return

      auth.clear()
      const route = useRoute()
      if (route.path.startsWith('/dashboard')) {
        navigateTo({ path: '/login', query: { redirect: route.fullPath } })
      }
    },
  })

  return { provide: { api } }
})
