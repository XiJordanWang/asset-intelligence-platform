import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { api, money, type MyWork, type WorkSection } from '../api'
import { canOpen, useAuth, type Area } from '../auth'

const MODULES: { title: string; text: string; to?: string; area?: Area }[] = [
  { title: 'Asset register', text: 'Every asset across the plants and sites, with depreciation and verification.', to: '/assets', area: 'assets' },
  { title: 'Investment requests', text: 'Capex requests routed by amount to the right approvers, then capitalised.', to: '/requests', area: 'workflows' },
  { title: 'Asset changes', text: 'Transfers, disposals and corrections. Write-offs escalate by net book value.', to: '/changes', area: 'workflows' },
  { title: 'Stocktake', text: 'Physical counts with a coordinator per site. Missing assets become draft disposals.', to: '/stocktakes', area: 'stocktake' },
  { title: 'Management report', text: 'Book values, depreciation and a map of all sites.', to: '/cockpit', area: 'reports' },
  { title: 'Plant view', text: 'Each plant on a site plan, by production area and capex programme.', to: '/plants', area: 'reports' },
  { title: 'Users & roles', text: 'Give people roles; each role sees its own menu and tasks.', to: '/admin/users', area: 'admin' },
  { title: 'Invoice', text: 'AI reads supplier invoices and matches them to approved requests.' },
  { title: 'Self-service desk', text: 'Ask questions about capex policy and get answers with sources (RAG).' },
]

const EMPTY: Record<WorkSection['key'], string> = {
  approvals: 'Nothing is waiting for you. New requests for your plant or approval level will appear here.',
  mine: 'You have no open requests or forms.',
  counts: 'No sites to count right now.',
  finance: 'Nothing to capitalise or run.',
}

export default function Home() {
  const { user } = useAuth()
  const [work, setWork] = useState<MyWork | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    api.myWork().then(setWork).catch(e => setError(e.message))
  }, [user?.username])

  const total = work?.sections.reduce((n, s) => n + s.items.length, 0) ?? 0
  const modules = MODULES.filter(m => !m.area || canOpen(user, m.area))

  return (
    <section>
      <div className="hero">
        <h1>Kia ora, {user?.displayName.split(' ')[0]}</h1>
        <p>
          {user?.title}.{' '}
          {work && (total === 0 ? 'You are all caught up.' : `You have ${total} item${total === 1 ? '' : 's'} to look at.`)}
        </p>
      </div>
      {error && <p className="error">{error}</p>}
      {!work && !error && <p className="muted">Loading your work…</p>}
      <div className="work-grid">
        {work?.sections.map(s => (
          <div key={s.key} className="panel work-section">
            <div className="row-between">
              <h2>{s.title}</h2>
              <span className={`pill ${s.items.length ? 'soon' : 'ok'}`}>{s.items.length}</span>
            </div>
            <p className="muted small">{s.items.length ? s.hint : EMPTY[s.key]}</p>
            <ul className="work-list">
              {s.items.slice(0, 8).map(i => (
                <li key={i.reference}>
                  <Link to={i.link}>
                    <span className="mono small">{i.reference}</span>
                    <strong>{i.title}</strong>
                    <span className="muted small">{i.detail}</span>
                  </Link>
                  {!!i.amount && <span className="num">{money(i.amount)}</span>}
                </li>
              ))}
            </ul>
            {s.items.length > 8 && <p className="muted small">and {s.items.length - 8} more</p>}
          </div>
        ))}
      </div>
      <h2 className="section-title">What your role can open</h2>
      <div className="modules">
        {modules.map(m => {
          const body = (
            <>
              <h2>{m.title}</h2>
              <p>{m.text}</p>
              <span className={m.to ? 'pill ok' : 'pill soon'}>{m.to ? 'Open' : 'Coming soon'}</span>
            </>
          )
          return m.to ? (
            <Link key={m.title} to={m.to} className="module">{body}</Link>
          ) : (
            <div key={m.title} className="module disabled">{body}</div>
          )
        })}
      </div>
    </section>
  )
}
