import { describe, expect, it } from 'vitest'
import { encodePath, renderSitemapIndex, renderUrlSet, xmlEscape } from '~/server/utils/sitemap'

/**
 * The sitemap writers, which are the one place in this app where Persian text has to survive two
 * encodings in the right order.
 *
 * Slugs are Persian, so a `<loc>` has to be percent-encoded to be a legal URL and XML-escaped to
 * be legal XML -- in that order. Doing it the other way round escapes the `&amp;` sequences that
 * percent-encoding produced, and the result is a sitemap Search Console rejects wholesale rather
 * than one entry at a time.
 */

const SITE = 'https://yariju.example'

describe('xmlEscape', () => {
  it('escapes all five XML entities', () => {
    expect(xmlEscape('&<>"\'')).toBe('&amp;&lt;&gt;&quot;&apos;')
  })

  it('escapes the ampersand first, so entities are not double-escaped', () => {
    // Replacing < before & would turn "&lt;" into "&amp;lt;".
    expect(xmlEscape('a<b&c')).toBe('a&lt;b&amp;c')
  })

  it('leaves Persian text untouched', () => {
    expect(xmlEscape('کمک هزینه')).toBe('کمک هزینه')
  })
})

describe('encodePath', () => {
  it('percent-encodes Persian segments', () => {
    expect(encodePath('/requests/کمک')).toBe('/requests/' + encodeURIComponent('کمک'))
  })

  it('leaves the separators alone', () => {
    // encodeURIComponent on the whole path would turn every slash into %2F and produce one
    // long segment.
    const encoded = encodePath('/requests/کمک-هزینه-rq-1042')
    expect(encoded.startsWith('/requests/')).toBe(true)
    expect(encoded.split('/')).toHaveLength(3)
  })

  it('preserves a leading and trailing slash', () => {
    expect(encodePath('/a/b/')).toBe('/a/b/')
  })

  it('is a no-op for an ASCII path', () => {
    expect(encodePath('/requests/plain-slug')).toBe('/requests/plain-slug')
  })

  it('encodes the characters that would otherwise break the URL', () => {
    expect(encodePath('/requests/a b&c')).toBe('/requests/a%20b%26c')
  })
})

describe('renderUrlSet', () => {
  it('produces a well-formed document with one url per entry', () => {
    const xml = renderUrlSet(SITE, [
      { path: '/requests/alpha', lastModified: '2026-08-30T10:00:00Z', priority: 0.8 },
      { path: '/requests/beta', lastModified: null, priority: 0.5 },
    ])

    expect(xml.startsWith('<?xml version="1.0" encoding="UTF-8"?>')).toBe(true)
    expect(xml).toContain('<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">')
    expect(xml.trimEnd().endsWith('</urlset>')).toBe(true)
    expect(xml.match(/<url>/g)).toHaveLength(2)
  })

  it('encodes then escapes, in that order', () => {
    // The whole point. `&` is produced by neither step here, so its presence in the output
    // would mean a raw character survived; `%26` proves encoding ran, and the absence of a
    // bare `&` proves escaping did not then mangle it.
    const xml = renderUrlSet(SITE, [
      { path: '/requests/a&b', lastModified: null, priority: 0.5 },
    ])

    expect(xml).toContain('%26')
    expect(xml).not.toMatch(/<loc>[^<]*&(?!amp;)/)
  })

  it('emits a Persian slug as a percent-encoded, XML-safe loc', () => {
    const xml = renderUrlSet(SITE, [
      { path: '/requests/کمک-هزینه-rq-1042', lastModified: null, priority: 0.7 },
    ])

    expect(xml).toContain(`<loc>${SITE}/requests/${encodeURIComponent('کمک-هزینه-rq-1042')}</loc>`)
    // A raw Persian character in a <loc> is what Search Console rejects.
    expect(xml).not.toContain('کمک')
  })

  it('omits lastmod entirely when there is no date, rather than emitting an empty element', () => {
    const withDate = renderUrlSet(SITE, [
      { path: '/a', lastModified: '2026-08-30T10:00:00Z', priority: 1 },
    ])
    const withoutDate = renderUrlSet(SITE, [
      { path: '/a', lastModified: null, priority: 1 },
    ])

    expect(withDate).toContain('<lastmod>2026-08-30T10:00:00.000Z</lastmod>')
    expect(withoutDate).not.toContain('lastmod')
  })

  it('writes lastmod in ISO-8601, whatever the input format', () => {
    const xml = renderUrlSet(SITE, [
      { path: '/a', lastModified: '2026-08-30T13:30:00+03:30', priority: 1 },
    ])

    expect(xml).toContain('<lastmod>2026-08-30T10:00:00.000Z</lastmod>')
  })

  it('still produces a valid document for an empty list', () => {
    // A centre with no published requests, or a first deployment. An unclosed urlset here
    // would make the whole sitemap unreadable rather than empty.
    const xml = renderUrlSet(SITE, [])

    expect(xml).toContain('<urlset')
    expect(xml).toContain('</urlset>')
    expect(xml).not.toContain('<url>')
  })
})

describe('renderSitemapIndex', () => {
  it('lists each child sitemap once', () => {
    const xml = renderSitemapIndex(SITE, ['/sitemap-requests.xml', '/sitemap-centers.xml'])

    expect(xml).toContain('<sitemapindex')
    expect(xml.match(/<sitemap>/g)).toHaveLength(2)
    expect(xml).toContain(`<loc>${SITE}/sitemap-requests.xml</loc>`)
  })

  it('escapes the child paths as well', () => {
    const xml = renderSitemapIndex(SITE, ['/sitemap.xml?page=1&size=2'])

    expect(xml).toContain('&amp;')
    expect(xml).not.toMatch(/<loc>[^<]*&(?!amp;)/)
  })
})
