/**
 * Guards the panel.
 *
 * Named, not global: a global middleware also runs during SSR of public pages, where
 * localStorage does not exist and the visitor would always look signed out. Because
 * `/dashboard/**` is `ssr: false`, this only ever runs in the browser, after the auth
 * plugin has hydrated the session — plugins run before the first route middleware.
 */
export default defineNuxtRouteMiddleware((to) => {
  const auth = useAuth()

  if (!auth.isAuthenticated.value) {
    return navigateTo({ path: '/login', query: { redirect: to.fullPath } })
  }

  const required = to.meta.role as 'ADMIN' | 'CENTER' | undefined
  if (required && auth.role.value !== required) {
    // Send them to their own panel rather than showing a 403 they can do nothing about.
    return navigateTo(auth.homePath.value)
  }
})
