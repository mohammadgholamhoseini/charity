import { renderUrlSet, type SitemapEntry } from '../utils/sitemap'

/**
 * Request URLs, sourced from the backend's sitemap feed.
 *
 * Cached, because this is the one route a crawler can hit repeatedly and it walks the
 * whole published set. The XML is rendered here rather than by the API because the
 * canonical host and the URL shape are the frontend's to decide.
 */
export default defineCachedEventHandler(async (event) => {
  const config = useRuntimeConfig(event)
  setHeader(event, 'content-type', 'application/xml; charset=utf-8')

  const entries = await $fetch<SitemapEntry[]>(
    `${config.apiOrigin}/api/public/sitemap/requests`,
    { query: { page: 0, size: 5000 } },
  ).catch(() => [])

  return renderUrlSet(config.public.siteUrl, entries)
}, { maxAge: 3600, swr: true })
