import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { api, label, money, type AssetChange, type ChangeStatus } from '../api'
import { canDecide, plantOf, useAuth } from '../auth'

const TABS: (ChangeStatus | 'ALL')[] = ['ALL', 'DRAFT', 'PENDING_APPROVAL', 'APPROVED', 'REJECTED', 'CANCELLED']

export default function Changes() {
  const { user } = useAuth()
  const [tab, setTab] = useState<ChangeStatus | 'ALL'>('PENDING_APPROVAL')
  const [items, setItems] = useState<AssetChange[]>([])

  useEffect(() => {
    api.changes(tab === 'ALL' ? undefined : tab).then(setItems).catch(() => setItems([]))
  }, [tab])

  return (
    <section>
      <div className="row-between">
        <h1>Asset changes</h1>
        <a className="button" href="/api/export/changes.xlsx">Export to Excel</a>
      </div>
      <p className="muted">
        Start a transfer, disposal or master-data change from the <Link to="/assets">asset register</Link>. While a form is open the asset is
        locked, so no other form can change it.
      </p>
      <div className="tabs">
        {TABS.map(t => (
          <button key={t} className={t === tab ? 'tab active' : 'tab'} onClick={() => setTab(t)}>{label(t)}</button>
        ))}
      </div>
      <div className="panel table-wrap">
        <table>
          <thead>
            <tr><th>Reference</th><th>Type</th><th>Asset</th><th>From</th><th>To / reason</th><th className="num">Net book value</th><th>Status</th><th>Requested by</th></tr>
          </thead>
          <tbody>
            {items.map(c => (
              <tr key={c.reference}>
                <td className="mono"><Link to={`/changes/${c.reference}`}>{c.reference}</Link></td>
                <td>{label(c.type)}</td>
                <td><span className="mono">{c.assetTag}</span> {c.assetName}</td>
                <td>{c.fromSiteCode}</td>
                <td>{c.type === 'TRANSFER' ? c.targetSiteCode : c.type === 'DISPOSAL' ? label(c.disposalReason ?? '') : 'Master data'}</td>
                <td className="num">{c.netBookValue != null ? money(c.netBookValue) : '–'}</td>
                <td>
                  <span className={`pill r-${c.status.toLowerCase()}`}>{label(c.status)}</span>
                  {canDecide(user, c.waitingOn, plantOf(c.waitingOn === 'RECEIVING_PLANT_MANAGER' ? c.targetSiteCode : c.fromSiteCode))
                    && <span className="pill your-turn">Your turn</span>}
                </td>
                <td>{c.requestedBy}</td>
              </tr>
            ))}
            {items.length === 0 && <tr><td colSpan={8} className="muted">No forms here.</td></tr>}
          </tbody>
        </table>
      </div>
    </section>
  )
}
