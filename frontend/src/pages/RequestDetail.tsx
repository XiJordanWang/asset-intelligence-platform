import { useCallback, useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { api, label, money, type InvestmentRequest } from '../api'
import { canDecide, hasRole, plantOf, useAuth, whoDecides } from '../auth'
import SignInHint from '../components/SignInHint'

export default function RequestDetail() {
  const { reference = '' } = useParams()
  const { user } = useAuth()
  const [req, setReq] = useState<InvestmentRequest | null>(null)
  const [comment, setComment] = useState('')
  const [error, setError] = useState<string | null>(null)

  const load = useCallback(() => {
    api.request(reference).then(setReq).catch(e => setError(e.message))
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
            hasRole(user, 'REQUESTER', 'PLANT_MANAGER', 'ADMIN')
              ? <button className="primary" onClick={() => run(() => api.submit(req.reference))}>Submit for approval</button>
              : <SignInHint who="a requester" action="submit this request" />
          )}
          {req.status === 'PENDING_APPROVAL' && req.waitingOn && (
            canDecide(user, req.waitingOn, plantOf(req.siteCode)) ? (
              <div className="form">
                <p className="muted">Deciding as <strong>{user!.displayName}</strong> ({label(req.waitingOn)}).</p>
                <label>Comment<input value={comment} onChange={e => setComment(e.target.value)} /></label>
                <div className="actions">
                  <button className="primary" onClick={() => run(() => api.decide(req.reference, true, comment))}>Approve</button>
                  <button className="danger" onClick={() => run(() => api.decide(req.reference, false, comment))}>Reject</button>
                </div>
              </div>
            ) : <SignInHint who={whoDecides(req.waitingOn, plantOf(req.siteCode))} action="decide this step" />
          )}
          {req.status === 'APPROVED' && (
            hasRole(user, 'FINANCE_CONTROLLER', 'ADMIN')
              ? <button className="primary" onClick={() => run(() => api.capitalise(req.reference))}>Capitalise into asset register</button>
              : <SignInHint who="finance" action="capitalise this request" />
          )}
          {error && <p className="error">{error}</p>}
        </div>
      </div>
    </section>
  )
}
