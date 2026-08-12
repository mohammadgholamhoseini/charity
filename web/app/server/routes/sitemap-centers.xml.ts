import { renderUrlSet, type SitemapEntry } from '../utils/sitemap'

export default defineCachedEventHandler(async (event) => {
  const config = useRuntimeConfig(event)
  setHeader(event, 'content-type', 'application/xml; charset=utf-8')

  const entries = await $fetch<SitemapEntry[]>(
    `${config.apiOrigin}/api/public/sitemap/centers`,
    { query: { page: 0, size: 5000 } },
  ).catch(() => [])

  return renderUrlSet(config.public.siteUrl, entries)
}, { maxAge: 3600, swr: true })
