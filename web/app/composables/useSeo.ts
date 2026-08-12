interface SeoOptions {
  title: string
  description: string
  /** Defaults to the current path. Always a path — never carries a query string. */
  path?: string
  image?: string
  type?: 'website' | 'article'
  /** Set for filter permutations and anything that would duplicate another page. */
  noindex?: boolean
}

/**
 * Per-page metadata.
 *
 * Wrapped rather than called inline on each page so that the canonical link and the
 * robots directive can never be forgotten on one of them — the failure mode there is
 * silent and only shows up months later as duplicate-content dilution.
 */
export function useSeo(options: SeoOptions | (() => SeoOptions)) {
  const config = useRuntimeConfig()
  const route = useRoute()

  const resolved = computed(() => (typeof options === 'function' ? options() : options))

  // Percent-encode each path segment. Slugs are Persian, and the canonical URL has to
  // be byte-identical to what the sitemap emits — a canonical that differs from the
  // sitemap entry is two URLs for one page as far as a crawler is concerned.
  const url = computed(() => {
    const path = resolved.value.path ?? route.path
    const [pathname, query] = path.split('?')
    const encoded = (pathname ?? '')
      .split('/')
      .map(segment => (segment ? encodeURIComponent(decodeURIComponent(segment)) : segment))
      .join('/')
    return config.public.siteUrl + encoded + (query ? `?${query}` : '')
  })
  const image = computed(() => config.public.siteUrl + (resolved.value.image ?? '/og/default.png'))

  // The whole dev deployment is unindexable, regardless of the page.
  const robots = computed(() =>
    !config.public.indexable || resolved.value.noindex ? 'noindex, follow' : 'index, follow',
  )

  useSeoMeta({
    title: () => resolved.value.title,
    description: () => resolved.value.description,
    ogTitle: () => resolved.value.title,
    ogDescription: () => resolved.value.description,
    ogUrl: () => url.value,
    ogImage: () => image.value,
    ogType: () => resolved.value.type ?? 'website',
    ogSiteName: config.public.siteName,
    ogLocale: 'fa_IR',
    twitterCard: 'summary_large_image',
    twitterTitle: () => resolved.value.title,
    twitterDescription: () => resolved.value.description,
    twitterImage: () => image.value,
    robots: () => robots.value,
  })

  useHead({
    link: [{ rel: 'canonical', href: () => url.value }],
  })
}
