/**
 * Persian display formatting.
 *
 * Two rules run through all of this: visible text uses Persian digits and the Jalali
 * calendar, while URLs, meta tags, JSON-LD and the sitemap keep Latin digits and
 * ISO-8601 — a crawler reading «۱۴۰۵/۰۵/۱۹» as a publication date gets nothing.
 */
const numberFormat = new Intl.NumberFormat('fa-IR')

const dateFormat = new Intl.DateTimeFormat('fa-IR-u-ca-persian', {
  year: 'numeric',
  month: 'long',
  day: 'numeric',
  // Pinned explicitly: without it the server renders in UTC and the browser in
  // +03:30, so a request created late in the evening shows a different day on each
  // side and hydration mismatches on every card.
  timeZone: 'Asia/Tehran',
})

const shortDateFormat = new Intl.DateTimeFormat('fa-IR-u-ca-persian', {
  year: 'numeric',
  month: '2-digit',
  day: '2-digit',
  timeZone: 'Asia/Tehran',
})

export function useFormat() {
  return {
    /** Persian digits with thousands separators. */
    num: (value: number | null | undefined) =>
      value == null ? '—' : numberFormat.format(value),

    /** An amount in toman, the unit the site quotes in. */
    toman: (value: number | null | undefined) =>
      value == null ? '—' : `${numberFormat.format(value)} تومان`,

    /** «۱۹ مرداد ۱۴۰۵» */
    date: (iso: string | null | undefined) =>
      iso ? dateFormat.format(new Date(iso)) : '—',

    /** «۱۴۰۵/۰۵/۱۹» — for dense table cells. */
    shortDate: (iso: string | null | undefined) =>
      iso ? shortDateFormat.format(new Date(iso)) : '—',

    /** The machine-readable half of a <time> element. Always Latin, always ISO. */
    isoDate: (iso: string | null | undefined) =>
      iso ? new Date(iso).toISOString() : undefined,
  }
}

/**
 * Folds the character and digit variants a Persian search term can be typed in.
 *
 * Someone on an Arabic keyboard layout types ي and ك rather than ی and ک; without
 * folding, their search matches nothing at all. Mirrors the backend's own
 * normalisation so both ends agree on what two strings being "the same" means.
 */
export function normalizePersianQuery(input: string): string {
  return input
    .replace(/ي/g, 'ی')
    .replace(/ك/g, 'ک')
    .replace(/ة/g, 'ه')
    .replace(/[أإٱ]/g, 'ا')
    .replace(/[٠-٩]/g, d => String(d.charCodeAt(0) - 0x0660))
    .replace(/[۰-۹]/g, d => String(d.charCodeAt(0) - 0x06F0))
    .replace(/‌/g, ' ')
    .trim()
}
