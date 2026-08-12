/**
 * Restores the session from localStorage before any route middleware runs.
 *
 * Plugins run ahead of the first middleware, so the named `auth` middleware on the
 * panel pages can rely on the state already being populated. This is also why the
 * panel uses a named middleware rather than a global one: a global middleware also
 * executes during SSR of public pages, where localStorage does not exist.
 */
export default defineNuxtPlugin(() => {
  useAuth().hydrate()
})
