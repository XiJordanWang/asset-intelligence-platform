export type AssetCategory =
  | 'BUILDING' | 'PLANT_MACHINERY' | 'IT_EQUIPMENT' | 'VEHICLE' | 'FURNITURE' | 'LAND' | 'CONSTRUCTION_IN_PROGRESS'
export type AssetStatus = 'PENDING' | 'IN_SERVICE' | 'UNDER_MAINTENANCE' | 'DISPOSED'
export type RequestStatus = 'DRAFT' | 'PENDING_APPROVAL' | 'APPROVED' | 'REJECTED' | 'CAPITALISED'
export type ApproverRole = 'PLANT_MANAGER' | 'RECEIVING_PLANT_MANAGER' | 'FINANCE_CONTROLLER' | 'CFO'
export type ChangeType = 'TRANSFER' | 'DISPOSAL' | 'MODIFICATION'
export type ChangeStatus = 'DRAFT' | 'PENDING_APPROVAL' | 'APPROVED' | 'REJECTED' | 'CANCELLED'
export type DisposalReason = 'DAMAGED' | 'OBSOLETE' | 'TECHNOLOGY_UPGRADE' | 'SOLD' | 'DONATED' | 'LOST' | 'OTHER'
export const DISPOSAL_REASONS: DisposalReason[] = ['DAMAGED', 'OBSOLETE', 'TECHNOLOGY_UPGRADE', 'SOLD', 'DONATED', 'LOST', 'OTHER']

export const CATEGORIES: AssetCategory[] = [
  'BUILDING', 'PLANT_MACHINERY', 'IT_EQUIPMENT', 'VEHICLE', 'FURNITURE', 'LAND', 'CONSTRUCTION_IN_PROGRESS',
]

export interface Dashboard {
  asOf: string
  totals: {
    assetCount: number; siteCount: number; supplierCount: number
    originalValue: number; accumulatedDepreciation: number; impairment: number; netBookValue: number
  }
  plants: {
    plantCode: string; originalValue: number; netBookValue: number; yearToDateDepreciation: number
    byCategory: Partial<Record<AssetCategory, { netBookValue: number; depreciatedPercent: number }>>
  }[]
  sites: {
    code: string; name: string; plantCode: string; city: string
    latitude: number; longitude: number; assetCount: number; netBookValue: number
  }[]
  lastTwelveMonths: { month: string; additions: number; additionsValue: number; retirements: number; retirementsValue: number }[]
  assetsByStatus: Partial<Record<AssetStatus, number>>
}

export interface Asset {
  tag: string; name: string; category: AssetCategory; status: AssetStatus
  siteCode: string; plantCode: string; supplier: string | null; custodian: string | null
  originalValue: number; accumulatedDepreciation: number; impairment: number; netBookValue: number
  inServiceDate: string | null; lastVerifiedDate: string | null
  lockedByChange: string | null; disposedDate: string | null
}

export interface Page<T> {
  content: T[]
  page: { size: number; number: number; totalElements: number; totalPages: number }
}

export interface Step {
  order: number; role: ApproverRole; decision: 'PENDING' | 'APPROVED' | 'REJECTED'
  decidedBy: string | null; comment: string | null; decidedAt: string | null
}

export interface InvestmentRequest {
  reference: string; title: string; justification: string | null; category: AssetCategory
  amount: number; siteCode: string; requestedBy: string; status: RequestStatus
  waitingOn: ApproverRole | null; capitalisedAssetTag: string | null
  createdAt: string; updatedAt: string; steps: Step[]
}

export interface AssetChange {
  reference: string; type: ChangeType; status: ChangeStatus; assetTag: string; assetName: string
  fromSiteCode: string; targetSiteCode: string | null; disposalReason: DisposalReason | null
  netBookValue: number | null; requestedBy: string; reason: string | null; waitingOn: ApproverRole | null
  createdAt: string; updatedAt: string; steps: Step[]
  fieldChanges: { field: string; oldValue: string | null; newValue: string }[]
}

export type CampaignStatus = 'PLANNED' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED'
export type CountResult = 'PENDING' | 'FOUND' | 'DAMAGED' | 'MISSING'

export interface SiteProgress {
  siteCode: string; siteName: string; coordinator: string; status: 'OPEN' | 'SUBMITTED'
  total: number; counted: number; found: number; damaged: number; missing: number
}

export interface Campaign {
  reference: string; title: string; scopePlant: string | null; scopeCategory: AssetCategory | null
  samplePercent: number; photoRequired: boolean; status: CampaignStatus; dueDate: string; createdBy: string
  createdAt: string; startedAt: string | null; completedAt: string | null
  total: number; counted: number; missing: number; sites: SiteProgress[]
}

export interface SiteTask {
  campaignReference: string; campaignStatus: CampaignStatus; progress: SiteProgress
  lines: { assetTag: string; assetName: string; category: AssetCategory; custodian: string | null
    result: CountResult; note: string | null; countedBy: string | null; countedAt: string | null }[]
}

export interface Site { code: string; name: string; plantCode: string; city: string; coordinator: string | null }
export const PLANTS = ['AKL', 'HLZ', 'CHC', 'WLG']

/** Spring Security puts its CSRF token in this cookie; every change must send it back as a header. */
function csrfToken() {
  return document.cookie.split('; ').find(c => c.startsWith('XSRF-TOKEN='))?.split('=')[1] ?? ''
}

async function call<T>(path: string, init?: RequestInit): Promise<T> {
  const method = (init?.method ?? 'GET').toUpperCase()
  const res = await fetch(path, {
    ...init,
    credentials: 'same-origin',
    headers: {
      'Content-Type': 'application/json',
      ...(method === 'GET' ? {} : { 'X-XSRF-TOKEN': decodeURIComponent(csrfToken()) }),
      ...(init?.headers ?? {}),
    },
  })
  if (!res.ok) {
    const problem = await res.json().catch(() => null)
    const fallback = res.status === 401 ? 'Please sign in first.' : `${res.status} ${res.statusText}`
    throw new Error(problem?.detail ?? fallback)
  }
  if (res.status === 204) return undefined as T
  return res.json() as Promise<T>
}

export type WorkItem = { reference: string; title: string; detail: string; amount: number | null; link: string }
export type WorkSection = { key: 'approvals' | 'mine' | 'counts' | 'finance'; title: string; hint: string; items: WorkItem[] }
export type MyWork = { username: string; displayName: string; sections: WorkSection[] }

export type UserRole = 'REQUESTER' | 'PLANT_MANAGER' | 'FINANCE_CONTROLLER' | 'CFO' | 'SITE_COORDINATOR' | 'ADMIN'
export interface User {
  username: string; displayName: string; title: string; plantCode: string | null; roles: UserRole[]
}
export const DEMO_PASSWORD = 'demo'

export type PlantSlice = { key: string | null; label: string; assetCount: number; originalValue: number; netBookValue: number }
export type PlantArea = {
  area: string; label: string; assetCount: number; originalValue: number; netBookValue: number
  topAssets: { tag: string; name: string; category: string; siteCode: string; netBookValue: number }[]
}
export type PlantView = {
  plantCode: string; plantName: string; city: string; asOf: string; siteCount: number; assetCount: number
  originalValue: number; accumulatedDepreciation: number; netBookValue: number
  areas: PlantArea[]; programmes: PlantSlice[]; categories: PlantSlice[]
}
export const PLANT_CITIES: Record<string, string> = { AKL: 'Auckland', HLZ: 'Hamilton', CHC: 'Christchurch', WLG: 'Wellington' }

export const api = {
  dashboard: () => call<Dashboard>('/api/dashboard/overview'),
  plant: (code: string, upTo?: string) =>
    call<PlantView>(`/api/dashboard/plants/${code}` + (upTo ? `?upTo=${upTo}` : '')),
  sites: () => call<Site[]>('/api/sites'),
  assets: (q: Record<string, string>) =>
    call<Page<Asset>>('/api/assets?' + new URLSearchParams(Object.entries(q).filter(([, v]) => v !== ''))),
  requests: (status?: RequestStatus) =>
    call<InvestmentRequest[]>('/api/requests' + (status ? `?status=${status}` : '')),
  request: (ref: string) => call<InvestmentRequest>(`/api/requests/${ref}`),
  create: (body: object) => call<InvestmentRequest>('/api/requests', { method: 'POST', body: JSON.stringify(body) }),
  submit: (ref: string) => call<InvestmentRequest>(`/api/requests/${ref}/submit`, { method: 'POST' }),
  decide: (ref: string, approve: boolean, comment: string) =>
    call<InvestmentRequest>(`/api/requests/${ref}/${approve ? 'approve' : 'reject'}`, {
      method: 'POST', body: JSON.stringify({ comment }),
    }),
  changes: (status?: ChangeStatus) => call<AssetChange[]>('/api/changes' + (status ? `?status=${status}` : '')),
  change: (ref: string) => call<AssetChange>(`/api/changes/${ref}`),
  createChange: (body: object) => call<AssetChange>('/api/changes', { method: 'POST', body: JSON.stringify(body) }),
  submitChange: (ref: string) => call<AssetChange>(`/api/changes/${ref}/submit`, { method: 'POST' }),
  decideChange: (ref: string, approve: boolean, comment: string) =>
    call<AssetChange>(`/api/changes/${ref}/${approve ? 'approve' : 'reject'}`, {
      method: 'POST', body: JSON.stringify({ comment }),
    }),
  cancelChange: (ref: string) => call<AssetChange>(`/api/changes/${ref}/cancel`, { method: 'POST', body: '{}' }),
  stocktakes: () => call<Campaign[]>('/api/stocktakes'),
  stocktake: (ref: string) => call<Campaign>(`/api/stocktakes/${ref}`),
  planStocktake: (body: object) => call<Campaign>('/api/stocktakes', { method: 'POST', body: JSON.stringify(body) }),
  startStocktake: (ref: string) => call<Campaign>(`/api/stocktakes/${ref}/start`, { method: 'POST' }),
  completeStocktake: (ref: string) =>
    call<{ campaign: Campaign; verified: number; draftDisposals: string[] }>(`/api/stocktakes/${ref}/complete`, { method: 'POST' }),
  cancelStocktake: (ref: string) => call<Campaign>(`/api/stocktakes/${ref}/cancel`, { method: 'POST' }),
  stocktakeSite: (ref: string, site: string) => call<SiteTask>(`/api/stocktakes/${ref}/sites/${site}`),
  recordCount: (ref: string, site: string, assetTag: string, result: CountResult) =>
    call<SiteProgress>(`/api/stocktakes/${ref}/sites/${site}/count`, {
      method: 'POST', body: JSON.stringify({ assetTag, result }),
    }),
  submitSite: (ref: string, site: string) =>
    call<SiteProgress>(`/api/stocktakes/${ref}/sites/${site}/submit`, { method: 'POST', body: '{}' }),
  capitalise: (ref: string) => call<InvestmentRequest>(`/api/requests/${ref}/capitalise`, { method: 'POST' }),
  myWork: () => call<MyWork>('/api/my-work'),
  me: () => call<User | undefined>('/api/auth/me'),
  personas: () => call<User[]>('/api/auth/personas'),
  login: (username: string, password: string) =>
    call<User>('/api/auth/login', { method: 'POST', body: JSON.stringify({ username, password }) }),
  logout: () => call<void>('/api/auth/logout', { method: 'POST' }),
  users: () => call<User[]>('/api/users'),
  roles: () => call<{ role: UserRole; description: string }[]>('/api/users/roles'),
  updateRoles: (username: string, roles: UserRole[]) =>
    call<User>(`/api/users/${username}/roles`, { method: 'PUT', body: JSON.stringify({ roles }) }),
}

const nzd = new Intl.NumberFormat('en-NZ', { style: 'currency', currency: 'NZD', maximumFractionDigits: 0 })
export const money = (n: number) => nzd.format(n)
export const millions = (n: number) =>
  `NZ$${(n / 1_000_000).toLocaleString('en-NZ', { maximumFractionDigits: 1 })}m`
export const label = (s: string) => {
  const words = s.toLowerCase().split('_').join(' ')
  return (words.charAt(0).toUpperCase() + words.slice(1)).replace(/^It /, 'IT ').replace(/^Cfo$/, 'CFO')
}
