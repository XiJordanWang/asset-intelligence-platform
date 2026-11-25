import { useCallback, useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { api, label, type CountResult, type SiteTask } from '../api'
import { Progress } from './Stocktakes'
import { hasRole, plantOf, useAuth } from '../auth'
import SignInHint from '../components/SignInHint'

const RESULTS: CountResult[] = ['FOUND', 'DAMAGED', 'MISSING']

export default function StocktakeSite() {
  const { reference = '', siteCode = '' } = useParams()
  const [task, setTask] = useState<SiteTask | null>(null)
  const [error, setError] = useState<string | null>(null)
  const { user } = useAuth()

  const load = useCallback(() => {
    api.stocktakeSite(reference, siteCode).then(setTask).catch(e => setError(e.message))
  }, [reference, siteCode])
  useEffect(load, [load])

  if (!task) return error ? <p className="error">{error}</p> : <p className="muted">Loading…</p>
  const coordinator = task.progress.coordinator
  const isCoordinator = (hasRole(user, 'SITE_COORDINATOR') && user?.plantCode === plantOf(siteCode)) || hasRole(user, 'ADMIN')
  const open = task.campaignStatus === 'IN_PROGRESS' && task.progress.status === 'OPEN'
  const editable = open && isCoordinator

  const record = async (tag: string, result: CountResult) => {
    try {
      await api.recordCount(reference, siteCode, tag, result)
      load()
      setError(null)
    } catch (e) {
      setError((e as Error).message)
    }
  }

  const submit = async () => {
    try {
      await api.submitSite(reference, siteCode)
      load()
      setError(null)
    } catch (e) {
      setError((e as Error).message)
    }
  }

  return (
    <section>
      <p><Link to={`/stocktakes/${reference}`}>← {reference}</Link></p>
      <div className="row-between">
        <h1>{siteCode} · {task.progress.siteName}</h1>
        <Progress done={task.progress.counted} total={task.progress.total} />
      </div>
      <p className="muted">
        Site coordinator: <strong>{coordinator}</strong>.{' '}
        {task.progress.status === 'SUBMITTED' && 'This count has been submitted.'}
      </p>
      {open && !isCoordinator && <SignInHint who={`the ${plantOf(siteCode)} site coordinator`} action="record counts here" />}
      {error && <p className="error">{error}</p>}
      <div className="panel table-wrap">
        <table>
          <thead><tr><th>Tag</th><th>Asset</th><th>Class</th><th>Custodian</th><th>Result</th></tr></thead>
          <tbody>
            {task.lines.map(l => (
              <tr key={l.assetTag}>
                <td className="mono">{l.assetTag}</td>
                <td>{l.assetName}</td>
                <td>{label(l.category)}</td>
                <td>{l.custodian ?? '–'}</td>
                <td>
                  {editable ? (
                    <span className="count-buttons">
                      {RESULTS.map(r => (
                        <button key={r} className={l.result === r ? 'on' : ''} onClick={() => record(l.assetTag, r)}>{label(r)}</button>
                      ))}
                    </span>
                  ) : (
                    <span className={`c-${l.result.toLowerCase()}`}>{label(l.result)}</span>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      {editable && (
        <button className="primary" disabled={task.progress.counted < task.progress.total} onClick={submit}>
          Submit count for {siteCode}
        </button>
      )}
    </section>
  )
}
