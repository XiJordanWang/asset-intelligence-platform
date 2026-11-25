import { useMemo } from 'react'
import * as echarts from 'echarts'
import type { Dashboard } from '../api'
import { millions } from '../api'
import EChart from './EChart'
// New Zealand outline from Natural Earth (public domain), bundled so the map needs no API key or tile server.
import nzOutline from './nz.geo.json'

const PLANT_COLOURS: Record<string, string> = { AKL: '#2dd4bf', HLZ: '#facc15', CHC: '#60a5fa', WLG: '#f472b6' }

echarts.registerMap('nz', nzOutline as Parameters<typeof echarts.registerMap>[1])

type SitePoint = Dashboard['sites'][number]

/** Map of New Zealand with one circle per site, sized by net book value. Zoom with the mouse wheel, drag to pan. */
export default function SiteMap({ sites }: { sites: Dashboard['sites'] }) {
  const option = useMemo<echarts.EChartsOption>(() => {
    const maxNbv = Math.max(1, ...sites.map(s => s.netBookValue))
    return {
      tooltip: {
        trigger: 'item',
        formatter: p => {
          const s = (p as unknown as { data: { site: SitePoint } }).data?.site
          return s ? `<strong>${s.code}</strong> ${s.name}<br/>${s.assetCount} assets · ${millions(s.netBookValue)} NBV` : ''
        },
      },
      geo: {
        map: 'nz',
        roam: true,
        layoutCenter: ['50%', '50%'],
        layoutSize: '100%',
        scaleLimit: { min: 1, max: 12 },
        silent: true,
        itemStyle: { areaColor: '#16223b', borderColor: '#2b3d63', borderWidth: 1 },
        label: { show: false },
      },
      series: Object.keys(PLANT_COLOURS).map(plant => ({
        name: `${plant} plant`,
        type: 'scatter',
        coordinateSystem: 'geo',
        itemStyle: { color: PLANT_COLOURS[plant], opacity: 0.75, borderColor: '#0b1220', borderWidth: 1 },
        emphasis: { scale: 1.4 },
        data: sites
          .filter(s => s.plantCode === plant)
          .map(s => ({
            value: [s.longitude, s.latitude],
            symbolSize: 8 + 26 * Math.sqrt(s.netBookValue / maxNbv),
            site: s,
          })),
      })),
    }
  }, [sites])

  return (
    <div>
      <EChart option={option} height={460} />
      <div className="legend">
        {Object.entries(PLANT_COLOURS).map(([code, colour]) => (
          <span key={code}><i style={{ background: colour }} /> {code} plant</span>
        ))}
        <span>Circle size = net book value · scroll to zoom, drag to move</span>
      </div>
    </div>
  )
}
