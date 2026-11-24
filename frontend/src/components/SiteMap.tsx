import { useEffect, useRef } from 'react'
import L from 'leaflet'
import type { Dashboard } from '../api'
import { millions } from '../api'

const PLANT_COLOURS: Record<string, string> = { AKL: '#2dd4bf', HLZ: '#facc15', CHC: '#60a5fa', WLG: '#f472b6' }

/** Map of New Zealand with one circle per site, sized by net book value. */
export default function SiteMap({ sites }: { sites: Dashboard['sites'] }) {
  const ref = useRef<HTMLDivElement>(null)

  useEffect(() => {
    if (!ref.current) return
    const map = L.map(ref.current, { scrollWheelZoom: false }).setView([-41.0, 173.5], 5)
    L.tileLayer('https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png', {
      attribution: '&copy; OpenStreetMap contributors &copy; CARTO',
      maxZoom: 12,
    }).addTo(map)
    const maxNbv = Math.max(1, ...sites.map(s => s.netBookValue))
    for (const s of sites) {
      L.circleMarker([s.latitude, s.longitude], {
        radius: 4 + 14 * Math.sqrt(s.netBookValue / maxNbv),
        color: PLANT_COLOURS[s.plantCode] ?? '#94a3b8',
        weight: 1,
        fillOpacity: 0.55,
      })
        .bindTooltip(`<strong>${s.code}</strong> ${s.name}<br/>${s.assetCount} assets · ${millions(s.netBookValue)} NBV`)
        .addTo(map)
    }
    return () => {
      map.remove()
    }
  }, [sites])

  return (
    <div>
      <div ref={ref} className="map" />
      <div className="legend">
        {Object.entries(PLANT_COLOURS).map(([code, colour]) => (
          <span key={code}><i style={{ background: colour }} /> {code} plant</span>
        ))}
      </div>
    </div>
  )
}
