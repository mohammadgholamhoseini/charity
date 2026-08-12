import type { RequestDetail, CenterPublicProfile, RequestSummary } from '~/types/api'

export interface Crumb { name: string, path: string }

/**
 * Structured data.
 *
 * All values here stay in Latin digits and ISO-8601 regardless of what the page
 * displays — a crawler parsing a Jalali date string gets nothing usable.
 */
export function useJsonLd() {
  const config = useRuntimeConfig()
  const site = config.public.siteUrl

  function emit(data: Record<string, unknown> | Record<string, unknown>[]) {
    useHead({
      script: [{
        type: 'application/ld+json',
        innerHTML: JSON.stringify(data),
      }],
    })
  }

  /** Emitted once from the default layout. */
  function organization() {
    emit([
      {
        '@context': 'https://schema.org',
        '@type': 'NGO',
        'name': config.public.siteName,
        'url': site,
        'logo': `${site}/favicon.svg`,
        'description': 'شبکه اطلاع‌رسانی درخواست‌های مراکز خیریه',
        'areaServed': { '@type': 'Country', 'name': 'Iran' },
      },
      {
        '@context': 'https://schema.org',
        '@type': 'WebSite',
        'name': config.public.siteName,
        'url': site,
        'inLanguage': 'fa-IR',
        'potentialAction': {
          '@type': 'SearchAction',
          'target': { '@type': 'EntryPoint', 'urlTemplate': `${site}/requests?q={search_term_string}` },
          'query-input': 'required name=search_term_string',
        },
      },
    ])
  }

  function breadcrumbs(crumbs: Crumb[]) {
    emit({
      '@context': 'https://schema.org',
      '@type': 'BreadcrumbList',
      'itemListElement': crumbs.map((crumb, index) => ({
        '@type': 'ListItem',
        'position': index + 1,
        'name': crumb.name,
        'item': site + crumb.path,
      })),
    })
  }

  function itemList(requests: RequestSummary[], basePath = '/requests') {
    emit({
      '@context': 'https://schema.org',
      '@type': 'ItemList',
      'itemListElement': requests.map((request, index) => ({
        '@type': 'ListItem',
        'position': index + 1,
        'name': request.title,
        'url': `${site}${basePath}/${encodeURIComponent(request.slug)}`,
      })),
    })
  }

  /**
   * A request is modelled as a Demand — schema.org defines it as a public,
   * non-binding announcement by an organisation seeking goods or services, which is
   * literally what this is.
   *
   * Deliberately not Offer/Product/DonateAction: those all signal something
   * transactable, and this site takes no payments. Worth being honest that Demand
   * produces no rich result anywhere; the real SEO value on this page comes from the
   * canonical, the breadcrumbs and a genuine 410 for withdrawn requests.
   */
  function requestDetail(request: RequestDetail) {
    emit({
      '@context': 'https://schema.org',
      '@type': 'ItemPage',
      'name': request.title,
      'url': request.canonicalUrl,
      'datePublished': request.publishedAt ?? undefined,
      'dateModified': request.updatedAt ?? undefined,
      'inLanguage': 'fa-IR',
      'mainEntity': {
        '@type': 'Demand',
        'name': request.title,
        'description': request.summary ?? undefined,
        'seller': request.center
          ? {
              '@type': 'NGO',
              'name': request.center.name,
              'url': `${site}/centers/${encodeURIComponent(request.center.slug)}`,
            }
          : undefined,
        'priceSpecification': {
          '@type': 'PriceSpecification',
          'price': request.amountNeeded,
          'priceCurrency': request.amountCurrency,
        },
        'availabilityEnds': request.deadline ?? undefined,
        'areaServed': request.city ? { '@type': 'City', 'name': request.city.name } : undefined,
      },
    })
  }

  function centerProfile(center: CenterPublicProfile) {
    emit({
      '@context': 'https://schema.org',
      '@type': 'NGO',
      'name': center.name,
      'url': center.canonicalUrl,
      'description': center.description ?? undefined,
      'telephone': center.contactPhone ?? undefined,
      'logo': center.logoUrl ?? undefined,
      'address': center.city
        ? {
            '@type': 'PostalAddress',
            'addressLocality': center.city.name,
            'addressRegion': center.city.provinceName ?? undefined,
            'streetAddress': center.address ?? undefined,
            'addressCountry': 'IR',
          }
        : undefined,
    })
  }

  function faq(entries: { question: string, answer: string }[]) {
    emit({
      '@context': 'https://schema.org',
      '@type': 'FAQPage',
      'mainEntity': entries.map(entry => ({
        '@type': 'Question',
        'name': entry.question,
        'acceptedAnswer': { '@type': 'Answer', 'text': entry.answer },
      })),
    })
  }

  return { organization, breadcrumbs, itemList, requestDetail, centerProfile, faq }
}
