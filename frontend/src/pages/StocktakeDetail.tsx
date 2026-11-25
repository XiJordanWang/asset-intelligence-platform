import { useCallback, useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { api, label, type Campaign } from '../api'
import { Progress } from './Stocktakes'
import { hasRole, useAuth } from '../auth'
import SignInHint from '../components/SignInHint'

export default function StocktakeDetail() {
  const { reference = '' } = useParams()
  const [c, setC] = useState<Campaign | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [outcome, setOutcome] = useState<string | null>(null)
  const { user } = useAuth()
  const canRun = hasRole(user, 'FINANCE_CONTROLLER', 'ADMIN')

  const load = useCallback(() => {
    api.stocktake(reference).then(setC).catch(e => setError(e.message))
  }, [reference])
  useEffect(load, [load])

  const run = async (action: () => Promise<Campaign>) => {
    try {
      setC(await action())
      setError(null)
    } catch (e) {
      setError((e as Error).message)
    }
  }

  const complete = async () => {
    try {
      const r = await api.completeStocktake(reference)
      setC(r.campaign)
      setOutcome(`${r.verified} assets verified. ${r.draftDisposals.length} draft disposals raised for missing assets${r.draftDisposals.length ? ': ' + r.draftDisposals.join(', ') : ''}.`)
      setError(null)
    } catch (e) {
      setError((e as Error).message)
    }
  }

  if (!c) return error ? <p className="error">{error}</p> : <p className="muted">Loading…</p>
  const allSubmitted = c.sites.length > 0 && c.sites.every(s => s.status === 'SUBMITTED')

  return (
    <section>
      <p><Link to="/stocktakes">← All stocktakes</Link></p>
      <h1>{c.title} <span className="mono muted small">{c.reference}</span></h1>
      <div className="kpis">
        <div className="kpi"><div className="kpi-label">Status</div><div className="kpi-value">{label(c.status)}</div></div>
        <div className="kpi"><div className="kpi-label">Scope</div><div className="kpi-value small-kpi">{c.scopePlant ?? 'All plants'} · {c.scopeCategory ? label(c.scopeCategory) : 'All classes'}</div></div>
        <div className="kpi"><div className="kpi-label">Sample</div><div className="kpi-value">{c.samplePercent === 100 ? 'Full' : `${c.samplePercent}%`}</div></div>
        <div className="kpi"><div className="kpi-label">Counted</div><div className="kpi-value">{c.counted} / {c.total}</div></div>
        <div className="kpi"><div className="kpi-label">Missing so far</div><div className="kpi-value">{c.missing}</div></div>
        <div className="kpi"><div className="kpi-label">Due</div><div className="kpi-value small-kpi">{c.dueDate}</div></div>
      </div>

      {!canRun && (c.status === 'PLANNED' || c.status === 'IN_PROGRESS') && <SignInHint who="finance" action="start, complete or cancel a stocktake" />}
      <div className="actions" hidden={!canRun}>
        {c.status === 'PLANNED' && <button className="primary" onClick={() => run(() => api.startStocktake(reference))}>Start and send count lists</button>}
        {c.status === 'IN_PROGRESS' && <button className="primary" disabled={!allSubmitted} onClick={complete}>Complete and update the register</button>}
        {(c.status === 'PLANNED' || c.status === 'IN_PROGRESS') && <button className="danger" onClick={() => run(() => api.cancelStocktake(reference))}>Cancel</button>}
      </div>
      {c.status === 'IN_PROGRESS' && !allSubmitted && <p className="muted small">Every site must submit its count before the stocktake can be completed.</p>}
      {outcome && <p className="ok-text">{outcome} <Link to="/changes">See asset changes</Link></p>}
      {error && <p className="error">{error}</p>}

      {c.sites.length > 0 && (
        <div className="panel table-wrap">
          <table>
            <thead>
              <tr><th>Site</th><th>Coordinator</th><th>Progress</th><th className="num">Found</th><th className="num">Damaged</th><th className="num">Missing</th><th>Status</th></tr>
            </thead>
            <tbody>
              {c.sites.map(s => (
                <tr key={s.siteCode}>
                  <td><Link to={`/stocktakes/${reference}/sites/${s.siteCode}`}>{s.siteCode}</Link> <span className="muted">{s.siteName}</span></td>
                  <td>{s.coordinator}</td>
                  <td><Progress done={s.counted} total={s.total} /></td>
                  <td className="num c-found">{s.found}</td>
                  <td className="num c-damaged">{s.damaged}</td>
                  <td className="num c-missing">{s.missing}</td>
                  <td><span className={`pill ${s.status === 'SUBMITTED' ? 'ok' : 'soon'}`}>{label(s.status)}</span></td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  )
}
