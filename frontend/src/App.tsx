import { useState, type ReactNode } from 'react'
import { Link, NavLink, Route, Routes, useNavigate } from 'react-router-dom'
import { canOpen, useAuth, type Area } from './auth'
import { label } from './api'
import Login from './pages/Login'
import Users from './pages/Users'
import Home from './pages/Home'
import Cockpit from './pages/Cockpit'
import Assets from './pages/Assets'
import Approvals from './pages/Approvals'
import RequestDetail from './pages/RequestDetail'
import Changes from './pages/Changes'
import ChangeDetail from './pages/ChangeDetail'
import Stocktakes from './pages/Stocktakes'
import StocktakeDetail from './pages/StocktakeDetail'
import StocktakeSite from './pages/StocktakeSite'
import PlantPage from './pages/Plant'

const NAV: { to: string; label: string; end?: boolean; area?: Area }[] = [
  { to: '/', label: 'My work', end: true },
  { to: '/cockpit', label: 'Management report', area: 'reports' },
  { to: '/plants', label: 'Plant view', area: 'reports' },
  { to: '/assets', label: 'Asset register', area: 'assets' },
  { to: '/requests', label: 'Investment requests', area: 'workflows' },
  { to: '/changes', label: 'Asset changes', area: 'workflows' },
  { to: '/stocktakes', label: 'Stocktake', area: 'stocktake' },
  { to: '/admin/users', label: 'Users & roles', area: 'admin' },
]

/** Shows a page only to roles that can open it; the server refuses the data for everyone else anyway. */
function RequireRole({ area, children }: { area: Area; children: ReactNode }) {
  const { user } = useAuth()
  if (canOpen(user, area)) return <>{children}</>
  return (
    <section className="panel">
      <h1>Not part of your role</h1>
      <p className="muted">
        You are signed in as {user?.displayName} ({user?.roles.map(label).join(', ')}), and this area belongs to other roles.
        Use <Link to="/login">Switch account</Link> to try it as someone else.
      </p>
      <Link className="button" to="/">Back to my work</Link>
    </section>
  )
}

/** Compact account button for the top bar; the menu holds the name, role and the switch / sign out actions. */
function UserMenu() {
  const { user, signOut } = useAuth()
  const navigate = useNavigate()
  const [open, setOpen] = useState(false)
  if (!user) return null
  const initials = user.displayName.split(' ').map(w => w[0]).join('')
  return (
    <div className="user-menu" onBlur={e => { if (!e.currentTarget.contains(e.relatedTarget)) setOpen(false) }}>
      <button className="user-button" aria-expanded={open} onClick={() => setOpen(o => !o)}>
        <span className="avatar small-avatar" aria-hidden>{initials}</span>
        <span>{user.displayName.split(' ')[0]}</span>
        <span aria-hidden>▾</span>
      </button>
      {open && (
        <div className="user-dropdown" role="menu">
          <strong>{user.displayName}</strong>
          <span className="muted small">{user.title}</span>
          <Link className="button" to="/login" onClick={() => setOpen(false)}>Switch account</Link>
          <button onClick={async () => { setOpen(false); await signOut(); navigate('/') }}>Sign out</button>
        </div>
      )}
    </div>
  )
}

/** Before anything else, the visitor chooses who to be. Every page and API call needs a signed-in user. */
function SignInScreen() {
  return (
    <div className="shell">
      <header className="topbar">
        <div className="brand">
          <span className="logo" aria-hidden>◆</span>
          <span>Asset Intelligence</span>
          <span className="demo-badge">Demo · fictional data</span>
        </div>
      </header>
      <main className="content"><Login /></main>
      <footer className="footer">Kowhai Components Ltd is a fictional company. All figures are generated mock data.</footer>
    </div>
  )
}

export default function App() {
  const { user, ready } = useAuth()
  if (!ready) return <p className="muted" style={{ padding: '2rem' }}>Loading…</p>
  if (!user) return <SignInScreen />
  const nav = NAV.filter(n => !n.area || canOpen(user, n.area))
  return (
    <div className="shell">
      <header className="topbar">
        <div className="brand">
          <span className="logo" aria-hidden>◆</span>
          <span>Asset Intelligence</span>
          <span className="demo-badge">Demo · fictional data</span>
        </div>
        <nav>
          {nav.map(n => (
            <NavLink key={n.to} to={n.to} end={n.end} className={({ isActive }) => (isActive ? 'active' : '')}>
              {n.label}
            </NavLink>
          ))}
        </nav>
        <UserMenu />
      </header>
      <main className="content">
        <Routes>
          <Route path="/" element={<Home />} />
          <Route path="/login" element={<Login />} />
          <Route path="/admin/users" element={<RequireRole area="admin"><Users /></RequireRole>} />
          <Route path="/cockpit" element={<RequireRole area="reports"><Cockpit /></RequireRole>} />
          <Route path="/assets" element={<RequireRole area="assets"><Assets /></RequireRole>} />
          <Route path="/requests" element={<RequireRole area="workflows"><Approvals /></RequireRole>} />
          <Route path="/requests/:reference" element={<RequireRole area="workflows"><RequestDetail /></RequireRole>} />
          <Route path="/changes" element={<RequireRole area="workflows"><Changes /></RequireRole>} />
          <Route path="/changes/:reference" element={<RequireRole area="workflows"><ChangeDetail /></RequireRole>} />
          <Route path="/plants" element={<RequireRole area="reports"><PlantPage /></RequireRole>} />
          <Route path="/plants/:plantCode" element={<RequireRole area="reports"><PlantPage /></RequireRole>} />
          <Route path="/stocktakes" element={<RequireRole area="stocktake"><Stocktakes /></RequireRole>} />
          <Route path="/stocktakes/:reference" element={<RequireRole area="stocktake"><StocktakeDetail /></RequireRole>} />
          <Route path="/stocktakes/:reference/sites/:siteCode" element={<RequireRole area="stocktake"><StocktakeSite /></RequireRole>} />
        </Routes>
      </main>
      <footer className="footer">
        Kowhai Components Ltd is a fictional company. All figures are generated mock data.
      </footer>
    </div>
  )
}
