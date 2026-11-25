import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import {
  api, CATEGORIES, DISPOSAL_REASONS, label, money,
  type Asset, type ChangeType, type DisposalReason, type Page, type Site,
} from '../api'
import { hasRole, useAuth } from '../auth'

const STATUSES = ['PENDING', 'IN_SERVICE', 'UNDER_MAINTENANCE', 'DISPOSED']

export default function Assets() {
  const [sites, setSites] = useState<Site[]>([])
  const [filters, setFilters] = useState({ site: '', category: '', status: '' })
  const [page, setPage] = useState(0)
  const [result, setResult] = useState<Page<Asset> | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [action, setAction] = useState<{ asset: Asset; type: ChangeType } | null>(null)
  const { user } = useAuth()
  const canChange = hasRole(user, 'REQUESTER', 'PLANT_MANAGER', 'ADMIN')

  useEffect(() => {
    api.sites().then(setSites).catch(() => setSites([]))
  }, [])

  useEffect(() => {
    api.assets({ ...filters, page: String(page), size: '25' })
      .then(r => { setResult(r); setError(null) })
      .catch(e => setError(e.message))
  }, [filters, page])

  const update = (key: keyof typeof filters) => (e: React.ChangeEvent<HTMLSelectElement>) => {
    setPage(0)
    setFilters(f => ({ ...f, [key]: e.target.value }))
  }

  return (
    <section>
      <h1>Asset register</h1>
      <div className="filters">
        <select value={filters.site} onChange={update('site')} aria-label="Site">
          <option value="">All sites</option>
          {sites.map(s => <option key={s.code} value={s.code}>{s.code} · {s.name}</option>)}
        </select>
        <select value={filters.category} onChange={update('category')} aria-label="Category">
          <option value="">All classes</option>
          {CATEGORIES.map(c => <option key={c} value={c}>{label(c)}</option>)}
        </select>
        <select value={filters.status} onChange={update('status')} aria-label="Status">
          <option value="">All statuses</option>
          {STATUSES.map(s => <option key={s} value={s}>{label(s)}</option>)}
        </select>
        {result && <span className="muted">{result.page.totalElements.toLocaleString('en-NZ')} assets</span>}
        <a className="button" href={'/api/export/assets.xlsx?' + new URLSearchParams(Object.entries(filters).filter(([, v]) => v !== ''))}>
          Export to Excel
        </a>
      </div>
      {!canChange && <p className="muted small">Sign in as a requester to transfer, dispose of or edit assets.</p>}
      {error && <p className="error">{error}</p>}
      {action && <ChangeForm {...action} sites={sites} onClose={() => setAction(null)} />}
      <div className="panel table-wrap">
        <table>
          <thead>
            <tr>
              <th>Tag</th><th>Name</th><th>Class</th><th>Status</th><th>Site</th><th>Custodian</th>
              <th className="num">Original</th><th className="num">Net book value</th><th>Last verified</th><th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {result?.content.map(a => (
              <tr key={a.tag}>
                <td className="mono">{a.tag}</td>
                <td>{a.name}</td>
                <td>{label(a.category)}</td>
                <td><span className={`pill s-${a.status.toLowerCase()}`}>{label(a.status)}</span></td>
                <td>{a.siteCode}</td>
                <td>{a.custodian ?? '–'}</td>
                <td className="num">{money(a.originalValue)}</td>
                <td className="num">{money(a.netBookValue)}</td>
                <td>{a.lastVerifiedDate ?? <span className="warn">Never</span>}</td>
                <td>
                  {a.lockedByChange ? (
                    <Link to={`/changes/${a.lockedByChange}`} className="pill r-pending_approval">🔒 {a.lockedByChange}</Link>
                  ) : a.status !== 'DISPOSED' && canChange && (
                    <span className="row-actions">
                      <button onClick={() => setAction({ asset: a, type: 'TRANSFER' })}>Transfer</button>
                      <button onClick={() => setAction({ asset: a, type: 'DISPOSAL' })}>Dispose</button>
                      <button onClick={() => setAction({ asset: a, type: 'MODIFICATION' })}>Edit</button>
                    </span>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      {result && result.page.totalPages > 1 && (
        <div className="pager">
          <button disabled={page === 0} onClick={() => setPage(p => p - 1)}>Previous</button>
          <span>Page {page + 1} of {result.page.totalPages}</span>
          <button disabled={page + 1 >= result.page.totalPages} onClick={() => setPage(p => p + 1)}>Next</button>
        </div>
      )}
    </section>
  )
}

function ChangeForm({ asset, type, sites, onClose }: { asset: Asset; type: ChangeType; sites: Site[]; onClose: () => void }) {
  const navigate = useNavigate()
  const [targetSiteCode, setTarget] = useState(sites.find(s => s.code !== asset.siteCode)?.code ?? '')
  const [disposalReason, setReason] = useState<DisposalReason>('OBSOLETE')
  const [reason, setText] = useState('')
  const [master, setMaster] = useState({ newName: asset.name, newCustodian: asset.custodian ?? '', newSupplier: asset.supplier ?? '' })
  const [error, setError] = useState<string | null>(null)

  const submit = async (e: React.FormEvent) => {
    e.preventDefault()
    try {
      const created = await api.createChange({
        type, assetTag: asset.tag, reason,
        targetSiteCode: type === 'TRANSFER' ? targetSiteCode : null,
        disposalReason: type === 'DISPOSAL' ? disposalReason : null,
        ...(type === 'MODIFICATION' ? {
          newName: master.newName !== asset.name ? master.newName : null,
          newCustodian: master.newCustodian !== (asset.custodian ?? '') ? master.newCustodian : null,
          newSupplier: master.newSupplier !== (asset.supplier ?? '') ? master.newSupplier : null,
        } : {}),
      })
      navigate(`/changes/${created.reference}`)
    } catch (err) {
      setError((err as Error).message)
    }
  }

  return (
    <form className="panel form" onSubmit={submit}>
      <h2>{type === 'TRANSFER' ? 'Transfer' : type === 'DISPOSAL' ? 'Dispose of' : 'Change master data of'} {asset.tag} · {asset.name} (at {asset.siteCode}, NBV {money(asset.netBookValue)})</h2>
      {type === 'TRANSFER' ? (
        <label>Move to site
          <select value={targetSiteCode} onChange={e => setTarget(e.target.value)}>
            {sites.filter(s => s.code !== asset.siteCode).map(s => <option key={s.code} value={s.code}>{s.code} · {s.name}</option>)}
          </select>
        </label>
      ) : type === 'MODIFICATION' ? (
        <div className="form-row">
          <label>Name<input value={master.newName} onChange={e => setMaster(m => ({ ...m, newName: e.target.value }))} /></label>
          <label>Custodian<input value={master.newCustodian} onChange={e => setMaster(m => ({ ...m, newCustodian: e.target.value }))} /></label>
          <label>Supplier<input value={master.newSupplier} onChange={e => setMaster(m => ({ ...m, newSupplier: e.target.value }))} /></label>
        </div>
      ) : (
        <label>Disposal reason
          <select value={disposalReason} onChange={e => setReason(e.target.value as DisposalReason)}>
            {DISPOSAL_REASONS.map(r => <option key={r} value={r}>{label(r)}</option>)}
          </select>
        </label>
      )}
      <label>Notes<input value={reason} onChange={e => setText(e.target.value)} /></label>
      {error && <p className="error">{error}</p>}
      <div className="actions">
        <button className="primary" type="submit">Save draft</button>
        <button type="button" onClick={onClose}>Close</button>
      </div>
    </form>
  )
}
