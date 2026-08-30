import { describe, expect, it } from 'vitest'
import { normalizePersianQuery, useFormat } from '~/composables/useFormat'

/**
 * Persian display formatting, and the search-term folding that has to agree with the backend.
 *
 * The rule running through all of it: visible text uses Persian digits and the Jalali calendar,
 * while anything a machine reads -- URLs, meta tags, JSON-LD, the sitemap -- stays Latin and
 * ISO-8601. Most of the assertions below exist to stop those two from swapping places, which is
 * the failure that looks fine in the browser and quietly ruins every structured-data field.
 */

const f = useFormat()

/** Persian digits, so a test can say what it means without embedding the glyphs everywhere. */
const FA = '۰۱۲۳۴۵۶۷۸۹'
const toPersianDigits = (s: string) => s.replace(/\d/g, d => FA[Number(d)]!)

describe('numbers', () => {
  it('renders in Persian digits with separators', () => {
    expect(f.num(1234567)).toBe(toPersianDigits('1,234,567').replace(/,/g, '٬'))
  })

  it('renders an amount with its unit', () => {
    expect(f.toman(50000)).toContain('تومان')
    expect(f.toman(50000)).not.toMatch(/[0-9]/)
  })

  it('shows an em dash rather than a zero for a missing value', () => {
    // A missing amount and an amount of zero are different facts; rendering both as ۰ would
    // tell a visitor a centre needs nothing.
    expect(f.num(null)).toBe('—')
    expect(f.num(undefined)).toBe('—')
    expect(f.toman(null)).toBe('—')
    expect(f.num(0)).not.toBe('—')
  })
})

describe('file sizes', () => {
  it('steps through bytes, kilobytes and megabytes', () => {
    expect(f.fileSize(512)).toContain('بایت')
    expect(f.fileSize(2048)).toContain('کیلوبایت')
    expect(f.fileSize(5 * 1024 * 1024)).toContain('مگابایت')
  })

  it('uses the binary boundary, not the decimal one', () => {
    // 1000 bytes is still bytes; 1024 is a kilobyte.
    expect(f.fileSize(1000)).toContain('بایت')
    expect(f.fileSize(1000)).not.toContain('کیلوبایت')
    expect(f.fileSize(1024)).toContain('کیلوبایت')
  })

  it('never shows Latin digits', () => {
    for (const size of [0, 999, 1024, 1_500_000, 12_345_678]) {
      expect(f.fileSize(size)).not.toMatch(/[0-9]/)
    }
  })

  it('keeps a megabyte figure to one fraction digit', () => {
    // Otherwise «۱٫۲۳۴۵۶۷ مگابایت» reaches the upload list.
    const rendered = f.fileSize(1_300_000)
    const fractionDigits = rendered.split('٫')[1]?.match(/[۰-۹]/g)?.length ?? 0
    expect(fractionDigits).toBeLessThanOrEqual(1)
  })

  it('has no size to report for a missing file', () => {
    expect(f.fileSize(null)).toBe('—')
  })
})

describe('dates', () => {
  const iso = '2026-08-30T21:45:00Z'

  it('renders the Jalali calendar in Persian digits for a reader', () => {
    const rendered = f.date(iso)
    expect(rendered).not.toMatch(/[0-9]/)
    expect(rendered).toMatch(/[۰-۹]/)
  })

  it('keeps the machine-readable half Latin and ISO-8601', () => {
    // This is the <time datetime> value and every JSON-LD date. A crawler handed
    // «۱۴۰۵/۰۵/۱۹» gets nothing at all.
    expect(f.isoDate(iso)).toBe('2026-08-30T21:45:00.000Z')
  })

  it('pins the zone to Tehran on both sides of the render', () => {
    // Without the explicit timeZone the server formats in UTC and the browser in +03:30, so a
    // request created late in the evening shows a different day on each side and every card
    // hydration-mismatches. 21:45Z is 01:15 the next day in Tehran, which is exactly the window
    // that breaks.
    const lateEvening = f.date('2026-08-30T21:45:00Z')
    const nextMorning = f.date('2026-08-31T05:00:00Z')
    expect(lateEvening).toBe(nextMorning)
  })

  it('renders the short form in Persian digits too', () => {
    expect(f.shortDate(iso)).not.toMatch(/[0-9]/)
  })

  it('has nothing to show for a missing date', () => {
    expect(f.date(null)).toBe('—')
    expect(f.shortDate(undefined)).toBe('—')
    expect(f.isoDate(null)).toBeUndefined()
  })
})

/**
 * These have to keep agreeing with the backend's {@code SlugUtil.normalizePersian}, which folds
 * the same letters and the same two digit sets. If the two drift, a visitor's search term stops
 * matching content that was stored through the other one, and nothing anywhere reports an error.
 */
describe('search-term folding', () => {
  it('folds Arabic letter forms to their Persian equivalents', () => {
    expect(normalizePersianQuery('ياري')).toBe('یاری')
    expect(normalizePersianQuery('كمك')).toBe('کمک')
    expect(normalizePersianQuery('صدقة')).toBe('صدقه')
    expect(normalizePersianQuery('أحمد')).toBe('احمد')
    expect(normalizePersianQuery('إیران')).toBe('ایران')
  })

  it('folds both Arabic-Indic digit sets to Latin', () => {
    expect(normalizePersianQuery('٠١٢٣٤٥٦٧٨٩')).toBe('0123456789')
    expect(normalizePersianQuery('۰۱۲۳۴۵۶۷۸۹')).toBe('0123456789')
  })

  it('turns a zero-width non-joiner into a space so the words separate', () => {
    // A search term is tokenised, not slugified: the backend turns the same character into a
    // dash when building a URL, and into a separator when matching text.
    expect(normalizePersianQuery('یاری‌جو')).toBe('یاری جو')
  })

  it('trims, so a trailing space from a mobile keyboard does not change the query', () => {
    expect(normalizePersianQuery('  کمک  ')).toBe('کمک')
  })

  it('leaves text that is already canonical alone', () => {
    expect(normalizePersianQuery('کمک هزینه درمان')).toBe('کمک هزینه درمان')
  })

  it('is idempotent', () => {
    // It runs on every keystroke and its output is fed back in on the next one.
    const once = normalizePersianQuery('كمك ياري ٣ نفر')
    expect(normalizePersianQuery(once)).toBe(once)
  })
})
