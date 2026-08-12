import { ep } from '~/api/endpoints'
import type { NoticeResponse } from '~/types/api'

/**
 * The announcements currently on display.
 *
 * Fetched once per render and shared through a keyed `useAsyncData`, so the banner
 * and the footer do not each issue their own request. Failures are swallowed
 * deliberately: an announcement is decoration, and losing it must never take the
 * page down with it.
 */
export function useAnnouncements() {
  const { $api } = useNuxtApp()

  const { data } = useAsyncData(
    'announcements',
    () => $api<Record<string, NoticeResponse>>(ep.announcements).catch(() => ({})),
    { default: () => ({} as Record<string, NoticeResponse>) },
  )

  return {
    banner: computed(() => data.value?.TOP_BANNER ?? null),
    footer: computed(() => data.value?.FOOTER ?? null),
  }
}
