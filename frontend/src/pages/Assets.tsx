import { useEffect, useState } from 'react'
import { api, CATEGORIES, label, money, type Asset, type Page, type Site } from '../api'

const STATUSES = ['PENDING', 'IN_SERVICE', 'UNDER_MAINTENANCE', 'DISPOSED']

export default function Assets() {
  const [sites, setSites] = useState<Site[]>([])
  const [filters, setFilters] = useState({ site: '', category: '', status: '' })
  const [page, setPage] = useState(0)
  const [result, setResult] = useState<Page<Asset> | null>(null)
  const [error, setError] = useState<string | null>(null)

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
      </div>
      {error && <p className="error">{error}</p>}
      <div className="panel table-wrap">
        <table>
          <thead>
            <tr>
              <th>Tag</th><th>Name</th><th>Class</th><th>Status</th><th>Site</th>
              <th className="num">Original</th><th className="num">Net book value</th><th>Last verified</th>
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
                <td className="num">{money(a.originalValue)}</td>
                <td className="num">{money(a.netBookValue)}</td>
                <td>{a.lastVerifiedDate ?? <span className="warn">Never</span>}</td>
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
