export type AssetCategory =
  | 'BUILDING' | 'PLANT_MACHINERY' | 'IT_EQUIPMENT' | 'VEHICLE' | 'FURNITURE' | 'LAND' | 'CONSTRUCTION_IN_PROGRESS'
export type AssetStatus = 'PENDING' | 'IN_SERVICE' | 'UNDER_MAINTENANCE' | 'DISPOSED'
export type RequestStatus = 'DRAFT' | 'PENDING_APPROVAL' | 'APPROVED' | 'REJECTED' | 'CAPITALISED'
export type ApproverRole = 'PLANT_MANAGER' | 'FINANCE_CONTROLLER' | 'CFO'

export const CATEGORIES: AssetCategory[] = [
  'BUILDING', 'PLANT_MACHINERY', 'IT_EQUIPMENT', 'VEHICLE', 'FURNITURE', 'LAND', 'CONSTRUCTION_IN_PROGRESS',
]
export const ROLES: ApproverRole[] = ['PLANT_MANAGER', 'FINANCE_CONTROLLER', 'CFO']

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
}

export interface Asset {
  tag: string; name: string; category: AssetCategory; status: AssetStatus
  siteCode: string; plantCode: string; supplier: string | null
  originalValue: number; accumulatedDepreciation: number; impairment: number; netBookValue: number
  inServiceDate: string | null; lastVerifiedDate: string | null
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

export interface Site { code: string; name: string; plantCode: string; city: string }

async function call<T>(path: string, init?: RequestInit): Promise<T> {
  const res = await fetch(path, {
    ...init,
    headers: { 'Content-Type': 'application/json', ...(init?.headers ?? {}) },
  })
  if (!res.ok) {
    const problem = await res.json().catch(() => null)
    throw new Error(problem?.detail ?? `${res.status} ${res.statusText}`)
  }
  return res.json() as Promise<T>
}

export const api = {
  dashboard: () => call<Dashboard>('/api/dashboard/overview'),
  sites: () => call<Site[]>('/api/sites'),
  assets: (q: Record<string, string>) =>
    call<Page<Asset>>('/api/assets?' + new URLSearchParams(Object.entries(q).filter(([, v]) => v !== ''))),
  requests: (status?: RequestStatus) =>
    call<InvestmentRequest[]>('/api/requests' + (status ? `?status=${status}` : '')),
  request: (ref: string) => call<InvestmentRequest>(`/api/requests/${ref}`),
  create: (body: object) => call<InvestmentRequest>('/api/requests', { method: 'POST', body: JSON.stringify(body) }),
  submit: (ref: string) => call<InvestmentRequest>(`/api/requests/${ref}/submit`, { method: 'POST' }),
  decide: (ref: string, approve: boolean, role: ApproverRole, decidedBy: string, comment: string) =>
    call<InvestmentRequest>(`/api/requests/${ref}/${approve ? 'approve' : 'reject'}`, {
      method: 'POST', body: JSON.stringify({ role, decidedBy, comment }),
    }),
  capitalise: (ref: string) => call<InvestmentRequest>(`/api/requests/${ref}/capitalise`, { method: 'POST' }),
}

const nzd = new Intl.NumberFormat('en-NZ', { style: 'currency', currency: 'NZD', maximumFractionDigits: 0 })
export const money = (n: number) => nzd.format(n)
export const millions = (n: number) =>
  `NZ$${(n / 1_000_000).toLocaleString('en-NZ', { maximumFractionDigits: 1 })}m`
export const label = (s: string) => {
  const words = s.toLowerCase().split('_').join(' ')
  return words.charAt(0).toUpperCase() + words.slice(1)
}
