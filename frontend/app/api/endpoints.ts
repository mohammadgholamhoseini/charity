/**
 * Every API path the app knows, in one place.
 *
 * Pages never write a URL literal. When the backend renames something, this file is
 * the only edit — which is exactly what the CharityCase → Request rename would
 * otherwise have cost across two dozen components.
 */
export const ep = {
  // Public. Slugs are Persian and are NOT encoded here: route params arrive already
  // decoded, and the URL layer percent-encodes them once on the way out. Encoding here
  // as well produced `%25D8%25AC…` — a double-encoded path the backend cannot match.
  requests: '/public/requests',
  request: (slug: string) => `/public/requests/${slug}`,
  categories: '/public/categories',
  category: (slug: string) => `/public/categories/${slug}`,
  centers: '/public/centers',
  center: (slug: string) => `/public/centers/${slug}`,
  provinces: '/public/provinces',
  cities: '/public/cities',
  announcements: '/public/announcements',
  sitemapIndex: '/public/sitemap/index',
  sitemapRequests: '/public/sitemap/requests',
  sitemapCenters: '/public/sitemap/centers',
  sitemapCategories: '/public/sitemap/categories',

  // auth
  login: '/auth/login',

  // centre panel
  centerMe: '/center/me',
  centerLogo: '/center/me/logo',
  centerRequests: '/center/requests',
  centerRequestStats: '/center/requests/stats',
  centerRequest: (id: number) => `/center/requests/${id}`,
  centerRequestSubmit: (id: number) => `/center/requests/${id}/submit`,
  centerRequestComplete: (id: number) => `/center/requests/${id}/complete`,
  centerRequestStatus: (id: number) => `/center/requests/${id}/status`,
  centerRequestAnnounce: (id: number) => `/center/requests/${id}/announce`,
  centerRequestDocuments: (id: number) => `/center/requests/${id}/documents`,

  // admin panel
  adminRequests: '/admin/requests',
  adminRequestStats: '/admin/requests/stats',
  adminRequest: (id: number) => `/admin/requests/${id}`,
  adminRequestStatus: (id: number) => `/admin/requests/${id}/status`,
  adminRequestAnnounce: (id: number) => `/admin/requests/${id}/announce`,
  adminCenters: '/admin/centers',
  adminCenter: (id: number) => `/admin/centers/${id}`,
  adminCenterCategories: (id: number) => `/admin/centers/${id}/categories`,
  adminCenterActivate: (id: number) => `/admin/centers/${id}/activate`,
  adminCenterDeactivate: (id: number) => `/admin/centers/${id}/deactivate`,
  adminCenterPassword: (id: number) => `/admin/centers/${id}/password`,
  adminCategories: '/admin/categories',
  adminCategory: (id: number) => `/admin/categories/${id}`,
  adminNotices: '/admin/notices',
  adminNotice: (id: number) => `/admin/notices/${id}`,
  adminProvinces: '/admin/provinces',
  adminProvince: (id: number) => `/admin/provinces/${id}`,
  adminCities: '/admin/cities',
  adminCity: (id: number) => `/admin/cities/${id}`,
  adminMe: '/admin/me',
} as const
