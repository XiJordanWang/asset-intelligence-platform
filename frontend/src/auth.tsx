import { createContext, useCallback, useContext, useEffect, useState, type ReactNode } from 'react'
import { api, type ApproverRole, type User, type UserRole } from './api'

interface Auth {
  user: User | null
  ready: boolean
  signIn: (username: string, password: string) => Promise<User>
  signOut: () => Promise<void>
}

const AuthContext = createContext<Auth | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null)
  const [ready, setReady] = useState(false)

  useEffect(() => {
    api.me().then(u => setUser(u ?? null)).catch(() => setUser(null)).finally(() => setReady(true))
  }, [])

  const signIn = useCallback(async (username: string, password: string) => {
    const u = await api.login(username, password)
    setUser(u)
    return u
  }, [])

  const signOut = useCallback(async () => {
    await api.logout().catch(() => undefined)
    setUser(null)
  }, [])

  return <AuthContext.Provider value={{ user, ready, signIn, signOut }}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used inside AuthProvider')
  return ctx
}

export const hasRole = (user: User | null, ...roles: UserRole[]) =>
  !!user && roles.some(r => user.roles.includes(r))

/**
 * Which roles can open each area. The server enforces the same split (SecurityConfig); this only decides
 * which menu entries and pages a person sees.
 */
export const AREAS = {
  reports: ['PLANT_MANAGER', 'FINANCE_CONTROLLER', 'CFO', 'ADMIN'],
  assets: ['REQUESTER', 'PLANT_MANAGER', 'FINANCE_CONTROLLER', 'CFO', 'SITE_COORDINATOR', 'ADMIN'],
  workflows: ['REQUESTER', 'PLANT_MANAGER', 'FINANCE_CONTROLLER', 'CFO', 'ADMIN'],
  stocktake: ['SITE_COORDINATOR', 'PLANT_MANAGER', 'FINANCE_CONTROLLER', 'ADMIN'],
  admin: ['ADMIN'],
} satisfies Record<string, UserRole[]>
export type Area = keyof typeof AREAS

export const canOpen = (user: User | null, area: Area) => hasRole(user, ...AREAS[area])

export const plantOf = (siteCode: string | null | undefined) => siteCode?.split('-')[0] ?? null

/** Mirrors the server rule: plant-manager steps belong to the manager of that plant. */
export function canDecide(user: User | null, step: ApproverRole | null, plant: string | null) {
  if (!user || !step) return false
  switch (step) {
    case 'PLANT_MANAGER':
    case 'RECEIVING_PLANT_MANAGER':
      return hasRole(user, 'PLANT_MANAGER') && user.plantCode === plant
    case 'FINANCE_CONTROLLER':
      return hasRole(user, 'FINANCE_CONTROLLER')
    case 'CFO':
      return hasRole(user, 'CFO')
  }
}

export function whoDecides(step: ApproverRole, plant: string | null) {
  switch (step) {
    case 'PLANT_MANAGER': return `the ${plant} plant manager`
    case 'RECEIVING_PLANT_MANAGER': return `the receiving plant manager (${plant})`
    case 'FINANCE_CONTROLLER': return 'a finance controller'
    case 'CFO': return 'the CFO'
  }
}
