import { beforeEach, describe, expect, it, vi } from 'vitest'

/**
 * The cache-control rewrite, tested on the path that broke it twice.
 *
 * Two earlier attempts at this bug missed because they only ever saw the 200. The middleware
 * works by wrapping `res.setHeader` precisely so that it also catches h3's `handleCacheHeaders`,
 * which writes the 304's headers and calls `res.end()` itself. Since a 304's headers *replace*
 * those on the browser's stored copy, a `max-age` written there is what made every reload for the
 * next five minutes skip the network entirely.
 *
 * So the test drives `res.setHeader` directly, in both orders and with both kinds of value --
 * that is the whole surface the middleware has.
 */

vi.stubGlobal('defineEventHandler', (handler: unknown) => handler)

const handler = (await import('~/server/middleware/revalidate-headers'))
  .default as (event: { node: { res: FakeResponse } }) => void

interface FakeResponse {
  setHeader: (name: string, value: unknown) => void
  written: Array<[string, unknown]>
}

function makeResponse(): FakeResponse {
  const written: Array<[string, unknown]> = []
  return {
    written,
    setHeader(name: string, value: unknown) {
      written.push([name, value])
    },
  }
}

let res: FakeResponse

/** Applies the middleware to a fresh response, the way Nitro would on every request. */
beforeEach(() => {
  res = makeResponse()
  handler({ node: { res } })
})

const lastValue = () => res.written.at(-1)![1]

describe('a cached page response', () => {
  it('gains an explicit max-age=0 so the browser has to revalidate', () => {
    res.setHeader('cache-control', 's-maxage=300, stale-while-revalidate')

    expect(lastValue()).toBe('public, max-age=0, must-revalidate, s-maxage=300')
  })

  it('keeps the shared max-age, so the server window and any CDN still cache', () => {
    // Only the browser is obliged to ask. An unchanged page still costs one 304.
    res.setHeader('cache-control', 's-maxage=600')

    expect(lastValue()).toContain('s-maxage=600')
  })

  it('drops stale-while-revalidate', () => {
    // Nitro emits it with no value, which is not a usable directive, and its only possible
    // effect on a browser is serving the stale copy this exists to stop serving.
    res.setHeader('cache-control', 's-maxage=300, stale-while-revalidate')

    expect(String(lastValue())).not.toContain('stale-while-revalidate')
  })

  it('is rewritten however many times the header is set', () => {
    // The 200 and the 304 both go through setHeader, and a request can write it twice.
    res.setHeader('cache-control', 's-maxage=300, stale-while-revalidate')
    res.setHeader('cache-control', 'public, max-age=300, s-maxage=300')

    expect(lastValue()).toBe('public, max-age=0, must-revalidate, s-maxage=300')
  })

  it('catches the header whatever case it is written in', () => {
    // h3 and Nitro do not agree on the casing, and HTTP header names are case-insensitive.
    res.setHeader('Cache-Control', 's-maxage=300')

    expect(lastValue()).toBe('public, max-age=0, must-revalidate, s-maxage=300')
  })
})

describe('everything without a shared max-age is left exactly as it was', () => {
  it('leaves an immutable static asset alone', () => {
    // Rewriting this would cost a revalidation request per asset per page load.
    const original = 'max-age=31536000, immutable'
    res.setHeader('cache-control', original)

    expect(lastValue()).toBe(original)
  })

  it('leaves an uncached page alone', () => {
    res.setHeader('cache-control', 'no-cache')

    expect(lastValue()).toBe('no-cache')
  })

  it('leaves other headers alone', () => {
    res.setHeader('content-type', 'text/html')
    res.setHeader('x-nitro-prerender', '/requests')

    expect(res.written).toEqual([
      ['content-type', 'text/html'],
      ['x-nitro-prerender', '/requests'],
    ])
  })

  it('does not choke on a non-string value', () => {
    // `setHeader` accepts numbers and arrays; the guard has to survive both.
    res.setHeader('content-length', 1234)
    res.setHeader('set-cookie', ['a=1', 'b=2'])

    expect(res.written).toEqual([
      ['content-length', 1234],
      ['set-cookie', ['a=1', 'b=2']],
    ])
  })
})
