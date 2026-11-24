import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { api, CATEGORIES, label, money, type InvestmentRequest, type RequestStatus, type Site } from '../api'

const TABS: (RequestStatus | 'ALL')[] = ['ALL', 'DRAFT', 'PENDING_APPROVAL', 'APPROVED', 'CAPITALISED', 'REJECTED']

export default function Approvals() {
  const [tab, setTab] = useState<RequestStatus | 'ALL'>('PENDING_APPROVAL')
  const [items, setItems] = useState<InvestmentRequest[]>([])
  const [showForm, setShowForm] = useState(false)

  useEffect(() => {
    api.requests(tab === 'ALL' ? undefined : tab).then(setItems).catch(() => setItems([]))
  }, [tab])

  return (
    <section>
      <div className="row-between">
        <h1>Lifecycle approval</h1>
        <button className="primary" onClick={() => setShowForm(s => !s)}>{showForm ? 'Close' : 'New investment request'}</button>
      </div>
      {showForm && <NewRequestForm />}
      <div className="tabs">
        {TABS.map(t => (
          <button key={t} className={t === tab ? 'tab active' : 'tab'} onClick={() => setTab(t)}>{label(t)}</button>
        ))}
      </div>
      <div className="panel table-wrap">
        <table>
          <thead>
            <tr><th>Reference</th><th>Title</th><th>Class</th><th>Site</th><th className="num">Amount</th><th>Status</th><th>Requested by</th></tr>
          </thead>
          <tbody>
            {items.map(r => (
              <tr key={r.reference}>
                <td className="mono"><Link to={`/requests/${r.reference}`}>{r.reference}</Link></td>
                <td>{r.title}</td>
                <td>{label(r.category)}</td>
                <td>{r.siteCode}</td>
                <td className="num">{money(r.amount)}</td>
                <td><span className={`pill r-${r.status.toLowerCase()}`}>{label(r.status)}</span></td>
                <td>{r.requestedBy}</td>
              </tr>
            ))}
            {items.length === 0 && <tr><td colSpan={7} className="muted">No requests here.</td></tr>}
          </tbody>
        </table>
      </div>
    </section>
  )
}

function NewRequestForm() {
  const navigate = useNavigate()
  const [sites, setSites] = useState<Site[]>([])
  const [error, setError] = useState<string | null>(null)
  const [form, setForm] = useState({
    title: '', justification: '', category: 'PLANT_MACHINERY', amount: '25000', siteCode: 'HLZ-01', requestedBy: 'demo.requester',
  })

  useEffect(() => {
    api.sites().then(setSites).catch(() => setSites([]))
  }, [])

  const set = (k: keyof typeof form) =>
    (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement>) => setForm(f => ({ ...f, [k]: e.target.value }))

  const submit = async (e: React.FormEvent) => {
    e.preventDefault()
    try {
      const created = await api.create({ ...form, amount: Number(form.amount) })
      navigate(`/requests/${created.reference}`)
    } catch (err) {
      setError((err as Error).message)
    }
  }

  return (
    <form className="panel form" onSubmit={submit}>
      <label>Title<input required value={form.title} onChange={set('title')} placeholder="e.g. Laser cutter for prototype workshop" /></label>
      <label>Justification<textarea value={form.justification} onChange={set('justification')} rows={3} /></label>
      <div className="form-row">
        <label>Asset class
          <select value={form.category} onChange={set('category')}>
            {CATEGORIES.map(c => <option key={c} value={c}>{label(c)}</option>)}
          </select>
        </label>
        <label>Amount (NZD)<input type="number" min={1} required value={form.amount} onChange={set('amount')} /></label>
        <label>Site
          <select value={form.siteCode} onChange={set('siteCode')}>
            {sites.map(s => <option key={s.code} value={s.code}>{s.code} · {s.name}</option>)}
          </select>
        </label>
      </div>
      {error && <p className="error">{error}</p>}
      <button className="primary" type="submit">Save draft</button>
    </form>
  )
}
