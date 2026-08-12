export interface SitemapEntry {
  path: string
  lastModified: string | null
  priority: number
}

/**
 * XML-escapes a URL that has already been percent-encoded.
 *
 * Order matters: percent-encode the Persian path segments first, then escape for XML.
 * Doing it the other way round would encode the `&amp;` sequences themselves.
 */
export function xmlEscape(value: string): string {
  return value
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&apos;')
}

/** Percent-encodes each path segment, leaving the separators intact. */
export function encodePath(path: string): string {
  return path
    .split('/')
    .map(segment => (segment ? encodeURIComponent(segment) : segment))
    .join('/')
}

export function renderUrlSet(siteUrl: string, entries: SitemapEntry[]): string {
  const urls = entries.map((entry) => {
    const loc = xmlEscape(siteUrl + encodePath(entry.path))
    const lastmod = entry.lastModified
      ? `\n    <lastmod>${new Date(entry.lastModified).toISOString()}</lastmod>`
      : ''
    return `  <url>\n    <loc>${loc}</loc>${lastmod}\n    <priority>${entry.priority}</priority>\n  </url>`
  }).join('\n')

  return `<?xml version="1.0" encoding="UTF-8"?>\n`
    + `<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">\n${urls}\n</urlset>\n`
}

export function renderSitemapIndex(siteUrl: string, paths: string[]): string {
  const entries = paths.map(path =>
    `  <sitemap>\n    <loc>${xmlEscape(siteUrl + path)}</loc>\n  </sitemap>`).join('\n')

  return `<?xml version="1.0" encoding="UTF-8"?>\n`
    + `<sitemapindex xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">\n${entries}\n</sitemapindex>\n`
}
