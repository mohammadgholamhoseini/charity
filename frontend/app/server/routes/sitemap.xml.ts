import { renderSitemapIndex } from '../utils/sitemap'

/** A sitemap index, so the request feed can grow past one file without a rewrite. */
export default defineEventHandler((event) => {
  const config = useRuntimeConfig(event)
  setHeader(event, 'content-type', 'application/xml; charset=utf-8')

  return renderSitemapIndex(config.public.siteUrl, [
    '/sitemap-static.xml',
    '/sitemap-requests.xml',
    '/sitemap-centers.xml',
    '/sitemap-categories.xml',
  ])
})
