export interface Toast {
  id: number
  type: 'success' | 'error' | 'info'
  message: string
}

let nextId = 0

/**
 * Transient feedback.
 *
 * Exists to replace the native `alert()` and `confirm()` calls the old panels used —
 * ten of them — which are unstyleable, block the whole tab, and look nothing like the
 * rest of the interface.
 */
export function useToast() {
  const toasts = useState<Toast[]>('toasts', () => [])

  function push(type: Toast['type'], message: string, timeout = 4000) {
    const id = ++nextId
    toasts.value = [...toasts.value, { id, type, message }]
    if (import.meta.client) {
      setTimeout(() => dismiss(id), timeout)
    }
  }

  function dismiss(id: number) {
    toasts.value = toasts.value.filter(toast => toast.id !== id)
  }

  return {
    toasts,
    success: (message: string) => push('success', message),
    error: (message: string) => push('error', message, 6000),
    info: (message: string) => push('info', message),
    dismiss,
  }
}

/**
 * Turns an API failure into something worth showing a person.
 *
 * The backend sends a Persian `message` for anything the user can act on; anything
 * else gets a generic line rather than a stack trace or a status code.
 */
export function apiErrorMessage(error: unknown, fallback = 'خطایی رخ داد. دوباره تلاش کنید.'): string {
  const data = (error as { data?: { message?: string, fields?: Record<string, string> } })?.data
  if (data?.fields) {
    const first = Object.values(data.fields)[0]
    if (first) return first
  }
  return data?.message || fallback
}
