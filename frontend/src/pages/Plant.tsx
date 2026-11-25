import { useEffect, useMemo, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import type { EChartsOption } from 'echarts'
import { api, label, millions, money, PLANT_CITIES, PLANTS, type PlantView } from '../api'
import EChart from '../components/EChart'
import SitePlan from '../components/SitePlan'
import { useAuth } from '../auth'

const PROGRAMME_COLOURS = ['#2dd4bf', '#60a5fa', '#facc15', '#f472b6', '#a78bfa', '#fb923c', '#4ade80', '#94a3b8', '#475569']

export default function PlantPage() {
  const { user } = useAuth()
  const { plantCode = user?.plantCode ?? 'AKL' } = useParams()
  const navigate = useNavigate()
  const [upTo, setUpTo] = useState('')
  const [data, setData] = useState<PlantView | null>(null)
  const [selected, setSelected] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    api.plant(plantCode, upTo || undefined)
      .then(d => {
        setData(d)
        setError(null)
        setSelected(s => s ?? [...d.areas].sort((a, b) => b.netBookValue - a.netBookValue)[0]?.area ?? null)
      })
      .catch(e => setError(e.message))
  }, [plantCode, upTo])

  const areaChart = useMemo<EChartsOption | null>(() => data && {
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: 150, right: 30, top: 10, bottom: 30 },
    xAxis: { type: 'value', splitLine: { lineStyle: { color: '#23314f' } } },
    yAxis: { type: 'category', inverse: true, data: data.areas.map(a => a.label) },
    series: [{
      name: 'Assets', type: 'bar', barWidth: 10,
      data: data.areas.map(a => ({ value: a.assetCount, itemStyle: { color: a.area === selected ? '#facc15' : '#2dd4bf' } })),
      label: { show: true, position: 'right', color: '#cbd5e1' },
    }],
  }, [data, selected])

  const programmeChart = useMemo<EChartsOption | null>(() => data && {
    tooltip: { trigger: 'item', valueFormatter: v => millions(Number(v)) },
    legend: { orient: 'vertical', right: 0, top: 'middle', textStyle: { color: '#cbd5e1' } },
    color: PROGRAMME_COLOURS,
    series: [{
      name: 'Net book value', type: 'pie', radius: ['52%', '70%'], center: ['30%', '50%'],
      label: { show: false },
      data: data.programmes.map(p => ({ name: p.label, value: Math.round(p.netBookValue) })),
    }],
  }, [data])

  const area = data?.areas.find(a => a.area === selected)
  const thisMonth = new Date().toISOString().slice(0, 7)

  return (
    <section>
      <div className="row-between">
        <div className="tabs">
          {PLANTS.map(p => (
            <button key={p} className={`tab ${p === plantCode ? 'active' : ''}`} onClick={() => navigate(`/plants/${p}`)}>
              {PLANT_CITIES[p]} plant
            </button>
          ))}
        </div>
        <label className="muted small" style={{ display: 'flex', alignItems: 'center', gap: '.5rem' }}>
          Up to
          <input type="month" value={upTo || thisMonth} max={thisMonth} onChange={e => setUpTo(e.target.value)} />
        </label>
      </div>
      {error && <p className="error">{error}</p>}
      {data && (
        <>
          <h1>{data.plantName} <span className="muted small">{data.siteCount} sites · as of {data.asOf}</span></h1>
          <div className="kpis">
            <div className="kpi"><div className="kpi-label">Net book value</div><div className="kpi-value">{millions(data.netBookValue)}</div></div>
            <div className="kpi"><div className="kpi-label">Original value</div><div className="kpi-value">{millions(data.originalValue)}</div></div>
            <div className="kpi"><div className="kpi-label">Accumulated depreciation</div><div className="kpi-value">{millions(data.accumulatedDepreciation)}</div></div>
            <div className="kpi"><div className="kpi-label">Assets on the books</div><div className="kpi-value">{data.assetCount.toLocaleString('en-NZ')}</div></div>
          </div>
          <div className="plant-grid">
            <div className="panel">
              <h2>Site plan <span className="small">(schematic, click an area)</span></h2>
              <SitePlan areas={data.areas} selected={selected} onSelect={setSelected} />
            </div>
            <div className="plant-side">
              <div className="panel">
                <h2>Assets by area</h2>
                {areaChart && <EChart option={areaChart} height={260} />}
              </div>
              <div className="panel">
                <h2>Net book value by capex programme</h2>
                {programmeChart && <EChart option={programmeChart} height={230} />}
              </div>
            </div>
          </div>
          {area && (
            <div className="panel table-wrap">
              <h2>{area.label}: biggest assets by net book value</h2>
              <table>
                <thead><tr><th>Tag</th><th>Asset</th><th>Class</th><th>Site</th><th className="num">Net book value</th></tr></thead>
                <tbody>
                  {area.topAssets.map(a => (
                    <tr key={a.tag}>
                      <td className="mono">{a.tag}</td>
                      <td>{a.name}</td>
                      <td>{label(a.category)}</td>
                      <td>{a.siteCode}</td>
                      <td className="num">{money(a.netBookValue)}</td>
                    </tr>
                  ))}
                  {area.topAssets.length === 0 && <tr><td colSpan={5} className="muted">No assets in this area at this date.</td></tr>}
                </tbody>
              </table>
            </div>
          )}
        </>
      )}
    </section>
  )
}
