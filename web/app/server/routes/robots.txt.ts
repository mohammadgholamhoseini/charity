/**
 * robots.txt.
 *
 * The development deployment must never be indexed — two identical sites competing
 * for the same Persian queries is the fastest way to dilute both. That deployment
 * runs with NUXT_PUBLIC_INDEXABLE unset, and gets a blanket disallow.
 */
export default defineEventHandler((event) => {
  const config = useRuntimeConfig(event)
  setHeader(event, 'content-type', 'text/plain; charset=utf-8')

  if (!config.public.indexable) {
    return 'User-agent: *\nDisallow: /\n'
  }

  return [
    'User-agent: *',
    'Disallow: /dashboard/',
    'Disallow: /login',
    'Disallow: /api/',
    'Allow: /',
    '',
    `Sitemap: ${config.public.siteUrl}/sitemap.xml`,
    '',
  ].join('\n')
})
