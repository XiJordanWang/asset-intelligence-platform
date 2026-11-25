import { useCallback, useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { api, label, money, type AssetChange } from '../api'
import { canDecide, hasRole, plantOf, useAuth, whoDecides } from '../auth'
import SignInHint from '../components/SignInHint'

export default function ChangeDetail() {
  const { reference = '' } = useParams()
  const { user } = useAuth()
  const [change, setChange] = useState<AssetChange | null>(null)
  const [comment, setComment] = useState('')
  const [error, setError] = useState<string | null>(null)

  const load = useCallback(() => {
    api.change(reference).then(setChange).catch(e => setError(e.message))
  }, [reference])

  useEffect(load, [load])

  const run = async (action: () => Promise<AssetChange>) => {
    try {
      const updated = await action()
      setChange(updated)
      setError(null)
      setComment('')
    } catch (e) {
      setError((e as Error).message)
    }
  }

  if (!change) return error ? <p className="error">{error}</p> : <p className="muted">Loading…</p>
  const open = change.status === 'DRAFT' || change.status === 'PENDING_APPROVAL'

  const stepPlant = change.waitingOn === 'RECEIVING_PLANT_MANAGER' ? plantOf(change.targetSiteCode) : plantOf(change.fromSiteCode)

  return (
    <section>
      <p><Link to="/changes">← All asset changes</Link></p>
      <h1>{change.type === 'MODIFICATION' ? 'Master-data change' : label(change.type)} of {change.assetName} <span className="mono muted small">{change.reference}</span></h1>
      <div className="grid-2">
        <div className="panel">
          <dl className="facts">
            <dt>Status</dt><dd><span className={`pill r-${change.status.toLowerCase()}`}>{label(change.status)}</span></dd>
            <dt>Asset</dt><dd className="mono">{change.assetTag}</dd>
            <dt>Current site</dt><dd>{change.fromSiteCode}</dd>
            {change.type === 'TRANSFER' && <><dt>Move to</dt><dd>{change.targetSiteCode}</dd></>}
            {change.type === 'DISPOSAL' && <><dt>Reason</dt><dd>{label(change.disposalReason ?? '')}</dd></>}
            {change.netBookValue != null && <><dt>Net book value</dt><dd>{money(change.netBookValue)}</dd></>}
            <dt>Requested by</dt><dd>{change.requestedBy}</dd>
          </dl>
          {change.reason && <p>{change.reason}</p>}
          {change.fieldChanges.length > 0 && (
            <table>
              <thead><tr><th>Field</th><th>Current value</th><th>New value</th></tr></thead>
              <tbody>
                {change.fieldChanges.map(f => (
                  <tr key={f.field}><td>{label(f.field)}</td><td className="muted">{f.oldValue ?? '–'}</td><td><strong>{f.newValue}</strong></td></tr>
                ))}
              </tbody>
            </table>
          )}
          {change.type === 'DISPOSAL' && (
            <p className="muted small">
              Approval route by net book value: up to NZ$10k the plant manager; up to NZ$250k adds the finance
              controller; above that adds the CFO. Lost assets always need finance.
            </p>
          )}
        </div>
        <div className="panel">
          <h2>Approval chain and audit trail</h2>
          {change.steps.length === 0 && <p className="muted">The route is worked out when the form is submitted.</p>}
          <ol className="steps">
            {change.steps.map(s => (
              <li key={s.order} className={`step ${s.decision.toLowerCase()}`}>
                <strong>{label(s.role)}</strong> · {label(s.decision)}
                {s.decidedBy && <> by {s.decidedBy}</>}
                {s.decidedAt && <span className="muted small"> · {new Date(s.decidedAt).toLocaleString('en-NZ')}</span>}
                {s.comment && <div className="muted">“{s.comment}”</div>}
              </li>
            ))}
          </ol>

          {change.status === 'DRAFT' && (
            hasRole(user, 'REQUESTER', 'PLANT_MANAGER', 'ADMIN')
              ? <button className="primary" onClick={() => run(() => api.submitChange(change.reference))}>Submit for approval</button>
              : <SignInHint who="a requester" action="submit this form" />
          )}
          {change.status === 'PENDING_APPROVAL' && change.waitingOn && (
            canDecide(user, change.waitingOn, stepPlant) ? (
              <div className="form">
                <p className="muted">Deciding as <strong>{user!.displayName}</strong> ({label(change.waitingOn)}).</p>
                <label>Comment<input value={comment} onChange={e => setComment(e.target.value)} /></label>
                <div className="actions">
                  <button className="primary" onClick={() => run(() => api.decideChange(change.reference, true, comment))}>Approve</button>
                  <button className="danger" onClick={() => run(() => api.decideChange(change.reference, false, comment))}>Reject</button>
                </div>
              </div>
            ) : <SignInHint who={whoDecides(change.waitingOn, stepPlant)} action="decide this step" />
          )}
          {open && (user?.username === change.requestedBy || hasRole(user, 'ADMIN')) && (
            <p>
              <button onClick={() => run(() => api.cancelChange(change.reference))}>Cancel this form</button>
            </p>
          )}
          {change.status === 'APPROVED' && (
            <p className="muted">Applied to the asset register. <Link to="/assets">View the register</Link></p>
          )}
          {error && <p className="error">{error}</p>}
        </div>
      </div>
    </section>
  )
}
