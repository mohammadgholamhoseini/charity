import type { AuthUser } from '~/types/api'

const TOKEN_KEY = 'yariju.token'
const USER_KEY = 'yariju.user'

/**
 * Auth state.
 *
 * The token stays in localStorage, so the server never knows who the visitor is.
 * That is deliberate: it keeps every server-rendered public page identical for all
 * visitors, which is what makes the `swr` caching in nuxt.config safe. Any part of
 * the UI that depends on being signed in has to sit inside <ClientOnly>.
 */
export function useAuth() {
  const token = useState<string | null>('auth:token', () => null)
  const user = useState<AuthUser | null>('auth:user', () => null)
  const ready = useState<boolean>('auth:ready', () => false)

  const isAuthenticated = computed(() => Boolean(token.value))
  const role = computed(() => user.value?.role ?? null)
  const isAdmin = computed(() => role.value === 'ADMIN')
  const isCenter = computed(() => role.value === 'CENTER')

  /**
   * Reads persisted state. Called once from a client plugin.
   *
   * The try/catch is not defensive padding: parsing this value unguarded inside a
   * store initialiser is what used to white-screen the entire app when the stored
   * JSON was corrupt, with no way out but clearing site data by hand.
   */
  function hydrate() {
    try {
      token.value = localStorage.getItem(TOKEN_KEY)
      const raw = localStorage.getItem(USER_KEY)
      user.value = raw ? (JSON.parse(raw) as AuthUser) : null
    }
    catch {
      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem(USER_KEY)
      token.value = null
      user.value = null
    }
    finally {
      ready.value = true
    }
  }

  function setSession(nextToken: string, nextUser: AuthUser) {
    token.value = nextToken
    user.value = nextUser
    localStorage.setItem(TOKEN_KEY, nextToken)
    localStorage.setItem(USER_KEY, JSON.stringify(nextUser))
  }

  /** Clears both the reactive state and storage, so guards cannot see a stale token. */
  function clear() {
    token.value = null
    user.value = null
    if (import.meta.client) {
      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem(USER_KEY)
    }
  }

  /**
   * Signs out and leaves for the login page.
   *
   * Lives here rather than in the dashboard layout because signing out has to be reachable from
   * the public pages too — a user who clicks «مشاهده سایت» out of the panel was previously stuck
   * with no way back out of their session.
   *
   * Note this is a client-side discard only: there is no logout endpoint, so the JWT stays valid
   * until it expires. Fine for a shared computer being closed, not a revocation mechanism.
   */
  async function logout() {
    clear()
    await navigateTo('/login')
  }

  /** Where a given role's panel starts. */
  const homePath = computed(() => (isAdmin.value ? '/dashboard/admin' : '/dashboard'))

  return {
    token, user, ready, isAuthenticated, role, isAdmin, isCenter,
    hydrate, setSession, clear, logout, homePath,
  }
}
