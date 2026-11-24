import { useCallback, useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { api, label, money, ROLES, type ApproverRole, type InvestmentRequest } from '../api'

const DEMO_USERS: Record<ApproverRole, string> = {
  PLANT_MANAGER: 'tama.plantmanager',
  FINANCE_CONTROLLER: 'grace.finance',
  CFO: 'hemi.cfo',
}

export default function RequestDetail() {
  const { reference = '' } = useParams()
  const [req, setReq] = useState<InvestmentRequest | null>(null)
  const [role, setRole] = useState<ApproverRole>('PLANT_MANAGER')
  const [comment, setComment] = useState('')
  const [error, setError] = useState<string | null>(null)

  const load = useCallback(() => {
    api.request(reference).then(r => {
      setReq(r)
      if (r.waitingOn) setRole(r.waitingOn)
    }).catch(e => setError(e.message))
  }, [reference])

  useEffect(load, [load])

  const run = async (action: () => Promise<InvestmentRequest>) => {
    try {
      setReq(await action())
      setError(null)
      setComment('')
    } catch (e) {
      setError((e as Error).message)
    }
  }

  if (!req) return error ? <p className="error">{error}</p> : <p className="muted">Loading…</p>

  return (
    <section>
      <p><Link to="/requests">← All requests</Link></p>
      <h1>{req.title} <span className="mono muted small">{req.reference}</span></h1>
      <div className="grid-2">
        <div className="panel">
          <dl className="facts">
            <dt>Status</dt><dd><span className={`pill r-${req.status.toLowerCase()}`}>{label(req.status)}</span></dd>
            <dt>Amount</dt><dd>{money(req.amount)}</dd>
            <dt>Asset class</dt><dd>{label(req.category)}</dd>
            <dt>Site</dt><dd>{req.siteCode}</dd>
            <dt>Requested by</dt><dd>{req.requestedBy}</dd>
            {req.capitalisedAssetTag && <><dt>Asset</dt><dd className="mono">{req.capitalisedAssetTag}</dd></>}
          </dl>
          {req.justification && <p>{req.justification}</p>}
        </div>
        <div className="panel">
          <h2>Approval chain</h2>
          {req.steps.length === 0 && <p className="muted">The approval chain is set when the request is submitted.</p>}
          <ol className="steps">
            {req.steps.map(s => (
              <li key={s.order} className={`step ${s.decision.toLowerCase()}`}>
                <strong>{label(s.role)}</strong> · {label(s.decision)}
                {s.decidedBy && <> by {s.decidedBy}</>}
                {s.comment && <div className="muted">“{s.comment}”</div>}
              </li>
            ))}
          </ol>

          {req.status === 'DRAFT' && (
            <button className="primary" onClick={() => run(() => api.submit(req.reference))}>Submit for approval</button>
          )}
          {req.status === 'PENDING_APPROVAL' && (
            <div className="form">
              <label>Act as (demo)
                <select value={role} onChange={e => setRole(e.target.value as ApproverRole)}>
                  {ROLES.map(r => <option key={r} value={r}>{label(r)} ({DEMO_USERS[r]})</option>)}
                </select>
              </label>
              <label>Comment<input value={comment} onChange={e => setComment(e.target.value)} /></label>
              <div className="actions">
                <button className="primary" onClick={() => run(() => api.decide(req.reference, true, role, DEMO_USERS[role], comment))}>Approve</button>
                <button className="danger" onClick={() => run(() => api.decide(req.reference, false, role, DEMO_USERS[role], comment))}>Reject</button>
              </div>
            </div>
          )}
          {req.status === 'APPROVED' && (
            <button className="primary" onClick={() => run(() => api.capitalise(req.reference))}>Capitalise into asset register</button>
          )}
          {error && <p className="error">{error}</p>}
        </div>
      </div>
    </section>
  )
}
