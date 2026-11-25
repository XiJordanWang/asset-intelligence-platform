import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { api, CATEGORIES, label, PLANTS, type Campaign } from '../api'
import { hasRole, useAuth } from '../auth'
import SignInHint from '../components/SignInHint'

export default function Stocktakes() {
  const [items, setItems] = useState<Campaign[]>([])
  const [showForm, setShowForm] = useState(false)
  const { user } = useAuth()
  const canPlan = hasRole(user, 'FINANCE_CONTROLLER', 'ADMIN')

  useEffect(() => {
    api.stocktakes().then(setItems).catch(() => setItems([]))
  }, [])

  return (
    <section>
      <div className="row-between">
        <h1>Stocktake</h1>
        {canPlan && <button className="primary" onClick={() => setShowForm(s => !s)}>{showForm ? 'Close' : 'Plan a stocktake'}</button>}
      </div>
      <p className="muted">
        A stocktake sends each site's coordinator a list of assets to find. Results flow back into the register:
        found assets get today's verification date, damaged ones go to maintenance, and missing ones become draft
        disposals for finance to review.
      </p>
      {!canPlan && <SignInHint who="finance" action="plan a stocktake" />}
      {showForm && canPlan && <PlanForm />}
      <div className="panel table-wrap">
        <table>
          <thead>
            <tr><th>Reference</th><th>Title</th><th>Scope</th><th>Sample</th><th>Due</th><th>Progress</th><th>Status</th></tr>
          </thead>
          <tbody>
            {items.map(c => (
              <tr key={c.reference}>
                <td className="mono"><Link to={`/stocktakes/${c.reference}`}>{c.reference}</Link></td>
                <td>{c.title}</td>
                <td>{c.scopePlant ?? 'All plants'} · {c.scopeCategory ? label(c.scopeCategory) : 'All classes'}</td>
                <td>{c.samplePercent === 100 ? 'Full count' : `${c.samplePercent}%`}</td>
                <td>{c.dueDate}</td>
                <td>{c.total > 0 ? <Progress done={c.counted} total={c.total} /> : <span className="muted">Not started</span>}</td>
                <td><span className={`pill st-${c.status.toLowerCase()}`}>{label(c.status)}</span></td>
              </tr>
            ))}
            {items.length === 0 && <tr><td colSpan={7} className="muted">No stocktakes yet.</td></tr>}
          </tbody>
        </table>
      </div>
    </section>
  )
}

export function Progress({ done, total }: { done: number; total: number }) {
  const pct = total === 0 ? 0 : Math.round((done / total) * 100)
  return (
    <span style={{ display: 'inline-flex', alignItems: 'center', gap: '.5rem' }}>
      <span className="progress"><span style={{ width: `${pct}%` }} /></span>
      <span className="muted small">{done}/{total}</span>
    </span>
  )
}

function PlanForm() {
  const navigate = useNavigate()
  const due = new Date(Date.now() + 21 * 864e5).toISOString().slice(0, 10)
  const [form, setForm] = useState({ title: 'Quarterly asset count', scopePlant: 'HLZ', scopeCategory: '', samplePercent: '20', dueDate: due })
  const [error, setError] = useState<string | null>(null)
  const set = (k: keyof typeof form) => (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) =>
    setForm(f => ({ ...f, [k]: e.target.value }))

  const submit = async (e: React.FormEvent) => {
    e.preventDefault()
    try {
      const created = await api.planStocktake({
        ...form,
        scopeCategory: form.scopeCategory || null,
        samplePercent: Number(form.samplePercent),
        photoRequired: false,
      })
      navigate(`/stocktakes/${created.reference}`)
    } catch (err) {
      setError((err as Error).message)
    }
  }

  return (
    <form className="panel form" onSubmit={submit}>
      <label>Title<input required value={form.title} onChange={set('title')} /></label>
      <div className="form-row">
        <label>Plant
          <select value={form.scopePlant} onChange={set('scopePlant')}>
            <option value="">All plants</option>
            {PLANTS.map(p => <option key={p} value={p}>{p}</option>)}
          </select>
        </label>
        <label>Asset class
          <select value={form.scopeCategory} onChange={set('scopeCategory')}>
            <option value="">All classes</option>
            {CATEGORIES.filter(c => c !== 'LAND').map(c => <option key={c} value={c}>{label(c)}</option>)}
          </select>
        </label>
        <label>Sample (% of assets per site)<input type="number" min={1} max={100} value={form.samplePercent} onChange={set('samplePercent')} /></label>
        <label>Due date<input type="date" value={form.dueDate} onChange={set('dueDate')} /></label>
      </div>
      {error && <p className="error">{error}</p>}
      <button className="primary" type="submit">Save plan</button>
    </form>
  )
}
