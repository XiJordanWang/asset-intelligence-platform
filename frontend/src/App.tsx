import { NavLink, Route, Routes } from 'react-router-dom'
import Home from './pages/Home'
import Cockpit from './pages/Cockpit'
import Assets from './pages/Assets'
import Approvals from './pages/Approvals'
import RequestDetail from './pages/RequestDetail'

const NAV = [
  { to: '/', label: 'Home', end: true },
  { to: '/cockpit', label: 'Management report' },
  { to: '/assets', label: 'Asset register' },
  { to: '/requests', label: 'Lifecycle approval' },
]

export default function App() {
  return (
    <div className="shell">
      <header className="topbar">
        <div className="brand">
          <span className="logo" aria-hidden>◆</span>
          <span>Asset Intelligence</span>
          <span className="demo-badge">Demo · fictional data</span>
        </div>
        <nav>
          {NAV.map(n => (
            <NavLink key={n.to} to={n.to} end={n.end} className={({ isActive }) => (isActive ? 'active' : '')}>
              {n.label}
            </NavLink>
          ))}
        </nav>
      </header>
      <main className="content">
        <Routes>
          <Route path="/" element={<Home />} />
          <Route path="/cockpit" element={<Cockpit />} />
          <Route path="/assets" element={<Assets />} />
          <Route path="/requests" element={<Approvals />} />
          <Route path="/requests/:reference" element={<RequestDetail />} />
        </Routes>
      </main>
      <footer className="footer">
        Kowhai Components Ltd is a fictional company. All figures are generated mock data.
      </footer>
    </div>
  )
}
