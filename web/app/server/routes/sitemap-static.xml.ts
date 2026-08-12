import { renderUrlSet } from '../utils/sitemap'

/** The handful of fixed pages. Listed explicitly so none is silently dropped. */
export default defineEventHandler((event) => {
  const config = useRuntimeConfig(event)
  setHeader(event, 'content-type', 'application/xml; charset=utf-8')

  return renderUrlSet(config.public.siteUrl, [
    { path: '/', lastModified: null, priority: 1.0 },
    { path: '/requests', lastModified: null, priority: 0.9 },
    { path: '/centers', lastModified: null, priority: 0.7 },
    { path: '/about', lastModified: null, priority: 0.5 },
    { path: '/contact', lastModified: null, priority: 0.5 },
    { path: '/faq', lastModified: null, priority: 0.5 },
    { path: '/terms', lastModified: null, priority: 0.3 },
    { path: '/privacy', lastModified: null, priority: 0.3 },
  ])
})
