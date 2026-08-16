/**
 * PENDING and REJECTED are no longer produced — centres publish their own requests and nothing
 * is approved or turned down. They stay in the union, and keep their chip colours, because the
 * backend enum keeps them too: a row written before V9 can still carry one.
 */
export type RequestStatus =
  | 'DRAFT'
  | 'PENDING'
  | 'PUBLISHED'
  | 'REJECTED'
  | 'COMPLETED'
  | 'INACTIVE'

export type Urgency = 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT'

export type NoticePlacement = 'TOP_BANNER' | 'FOOTER'

export interface CategoryRef {
  id: number
  name: string
  slug: string
  labelBg: string
  labelText: string
}

export interface CategoryResponse extends CategoryRef {
  description: string | null
  sortOrder: number
  iconUrl: string | null
  active: boolean
  activeRequestCount: number
  updatedAt: string | null
}

export interface CityRef {
  id: number
  name: string
  provinceId: number | null
  provinceName: string | null
}

export interface CenterRef {
  id: number
  name: string
  slug: string
  logoUrl: string | null
  contactPhone: string | null
  responseHours: string | null
  cityName: string | null
  provinceName: string | null
  activeRequestCount: number
}

export interface CenterCard {
  id: number
  name: string
  slug: string
  logoUrl: string | null
  description: string | null
  city: CityRef | null
  categories: CategoryRef[]
  activeRequestCount: number
}

export interface CenterPublicProfile extends CenterCard {
  canonicalUrl: string
  fullName: string | null
  contactPhone: string | null
  responseHours: string | null
  address: string | null
  cardNumber: string | null
  sheba: string | null
  updatedAt: string | null
}

export interface CenterResponse extends CenterCard {
  fullName: string | null
  contactPhone: string | null
  responseHours: string | null
  username: string | null
  email: string | null
  address: string | null
  cardNumber: string | null
  sheba: string | null
  status: 'APPROVED' | 'INACTIVE'
  statusLabel: string
  createdAt: string | null
  updatedAt: string | null
}

export interface RequestSummary {
  id: number
  code: string
  slug: string
  title: string
  summary: string | null
  amountNeeded: number
  status: RequestStatus
  statusLabel: string
  /** An admin took this down, so the owning centre cannot put it back — only an admin can. */
  lockedByAdmin: boolean
  /**
   * Every enabled messaging channel already carries this request. False is what puts the
   * «انتشار در کانال» button on the row; with no bot configured it is always true.
   */
  announced: boolean
  urgency: Urgency
  urgencyLabel: string
  category: CategoryRef | null
  /** A request has no location of its own — the city shown is the centre's. */
  center: CenterRef | null
  createdAt: string | null
  publishedAt: string | null
  updatedAt: string | null
}

export interface RequestDetail extends RequestSummary {
  canonicalUrl: string
  description: string | null
  amountCurrency: string
  isActive: boolean
  statusNote: string | null
  imageUrl: string | null
  documents: string[]
  details: Record<string, unknown>
  metaTitle: string | null
  metaDescription: string | null
}

export interface NoticeResponse {
  id: number
  title: string
  content: string
  placement: NoticePlacement
  placementLabel: string
  startAt: string | null
  endAt: string | null
  linkUrl: string | null
  active: boolean
  expired: boolean
  createdAt: string | null
  updatedAt: string | null
}

/** Spring Data's page envelope, narrowed to the fields actually consumed. */
export interface Page<T> {
  content: T[]
  totalElements: number
  totalPages: number
  number: number
  size: number
  first: boolean
  last: boolean
}

export interface AuthUser {
  userId: number
  username: string
  role: 'ADMIN' | 'CENTER'
  fullName: string | null
  centerId: number | null
}

export interface AuthResponse extends AuthUser {
  token: string
}
