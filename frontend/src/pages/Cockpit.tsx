import { useEffect, useMemo, useState } from 'react'
import type { EChartsOption } from 'echarts'
import { api, label, millions, type Dashboard } from '../api'
import EChart from '../components/EChart'
import SiteMap from '../components/SiteMap'

export default function Cockpit() {
  const [data, setData] = useState<Dashboard | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    api.dashboard().then(setData).catch(e => setError(e.message))
  }, [])

  const plantChart = useMemo<EChartsOption | null>(() => {
    if (!data) return null
    return {
      tooltip: { trigger: 'axis' },
      legend: { top: 0 },
      grid: { left: 60, right: 16, top: 36, bottom: 30 },
      xAxis: { type: 'category', data: data.plants.map(p => p.plantCode) },
      yAxis: { type: 'value', axisLabel: { formatter: (v: number) => `${v / 1e6}m` } },
      series: [
        { name: 'Original value', type: 'bar', data: data.plants.map(p => p.originalValue), itemStyle: { color: '#334155' } },
        { name: 'Net book value', type: 'bar', data: data.plants.map(p => p.netBookValue), itemStyle: { color: '#2dd4bf' } },
      ],
    }
  }, [data])

  const categoryChart = useMemo<EChartsOption | null>(() => {
    if (!data) return null
    const totals = new Map<string, number>()
    for (const p of data.plants) {
      for (const [cat, v] of Object.entries(p.byCategory)) totals.set(cat, (totals.get(cat) ?? 0) + (v?.netBookValue ?? 0))
    }
    return {
      tooltip: { trigger: 'item', valueFormatter: v => millions(Number(v)) },
      series: [{
        type: 'pie', radius: ['45%', '72%'],
        data: [...totals.entries()].map(([name, value]) => ({ name: label(name), value })),
        label: { color: '#cbd5e1' },
      }],
    }
  }, [data])

  if (error) return <p className="error">Could not load the dashboard: {error}</p>
  if (!data) return <p className="muted">Loading…</p>

  const t = data.totals
  const kpis = [
    { label: 'Original value', value: millions(t.originalValue) },
    { label: 'Net book value', value: millions(t.netBookValue) },
    { label: 'Accumulated depreciation', value: millions(t.accumulatedDepreciation) },
    { label: 'Impairment', value: millions(t.impairment) },
    { label: 'Active assets', value: t.assetCount.toLocaleString('en-NZ') },
    { label: 'Sites / suppliers', value: `${t.siteCount} / ${t.supplierCount}` },
  ]

  return (
    <section>
      <h1>Management report <span className="muted small">as of {data.asOf}</span></h1>
      <div className="kpis">
        {kpis.map(k => (
          <div key={k.label} className="kpi">
            <div className="kpi-label">{k.label}</div>
            <div className="kpi-value">{k.value}</div>
          </div>
        ))}
      </div>
      <div className="grid-2">
        <div className="panel">
          <h2>Sites across New Zealand</h2>
          <SiteMap sites={data.sites} />
        </div>
        <div className="panel">
          <h2>Value by plant</h2>
          {plantChart && <EChart option={plantChart} />}
          <h2>Net book value by asset class</h2>
          {categoryChart && <EChart option={categoryChart} height={240} />}
        </div>
      </div>
      <div className="panel">
        <h2>Plant breakdown</h2>
        <table>
          <thead>
            <tr><th>Plant</th><th className="num">Original value</th><th className="num">Net book value</th><th className="num">Depreciation this year</th></tr>
          </thead>
          <tbody>
            {data.plants.map(p => (
              <tr key={p.plantCode}>
                <td>{p.plantCode}</td>
                <td className="num">{millions(p.originalValue)}</td>
                <td className="num">{millions(p.netBookValue)}</td>
                <td className="num">{millions(p.yearToDateDepreciation)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </section>
  )
}
