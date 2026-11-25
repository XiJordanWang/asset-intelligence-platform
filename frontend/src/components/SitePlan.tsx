import type { PlantArea } from '../api'
import { millions } from '../api'

/**
 * A schematic, top-down plan of a typical Kowhai Components plant. Each production area is one building zone,
 * shaded by its share of the plant's net book value. Click a zone to see its biggest assets.
 */
type Zone = { area: string; x: number; y: number; w: number; h: number }

const ZONES: Zone[] = [
  { area: 'MACHINING', x: 70, y: 70, w: 270, h: 150 },
  { area: 'MOULDING', x: 350, y: 70, w: 190, h: 150 },
  { area: 'QUALITY_LAB', x: 550, y: 70, w: 120, h: 90 },
  { area: 'ASSEMBLY', x: 70, y: 230, w: 360, h: 150 },
  { area: 'FINISHING', x: 440, y: 230, w: 150, h: 150 },
  { area: 'WAREHOUSE', x: 600, y: 170, w: 170, h: 210 },
  { area: 'OFFICES', x: 70, y: 420, w: 210, h: 80 },
  { area: 'SITE_SERVICES', x: 300, y: 420, w: 290, h: 80 },
]

const TREES = [[30, 40], [30, 120], [30, 200], [30, 300], [30, 380], [30, 470], [700, 450], [740, 470], [770, 430], [650, 480]]

export default function SitePlan({ areas, selected, onSelect }: {
  areas: PlantArea[]
  selected: string | null
  onSelect: (area: string) => void
}) {
  const byArea = new Map(areas.map(a => [a.area, a]))
  const maxNbv = Math.max(1, ...areas.map(a => a.netBookValue))

  return (
    <svg viewBox="0 0 800 530" className="site-plan" role="img" aria-label="Schematic site plan of the plant">
      <defs>
        <pattern id="roof" width="12" height="12" patternUnits="userSpaceOnUse">
          <path d="M0 6 H12" stroke="rgba(148,163,184,0.12)" strokeWidth="1" />
        </pattern>
        <pattern id="parking" width="18" height="30" patternUnits="userSpaceOnUse">
          <path d="M0 0 V30" stroke="rgba(148,163,184,0.25)" strokeWidth="1" />
        </pattern>
      </defs>

      {/* site boundary and ring road */}
      <rect x="10" y="10" width="780" height="510" rx="18" fill="#0d1628" stroke="#23314f" />
      <rect x="50" y="50" width="740" height="470" fill="none" stroke="#1e2a44" strokeWidth="14" />
      <path d="M50 400 H790" stroke="#1e2a44" strokeWidth="14" />
      <path d="M50 400 H790" stroke="#334155" strokeWidth="1" strokeDasharray="10 10" />
      <text x="790" y="40" textAnchor="end" className="plan-note">N ↑</text>
      <text x="420" y="514" textAnchor="middle" className="plan-note">Main gate</text>

      {/* truck yard by the warehouse */}
      {[0, 1, 2, 3, 4, 5].map(i => (
        <rect key={i} x={778} y={185 + i * 32} width="10" height="18" fill="#334155" />
      ))}

      {/* car park */}
      <rect x="600" y="420" width="70" height="80" fill="url(#parking)" stroke="#23314f" />

      {TREES.map(([cx, cy], i) => (
        <circle key={i} cx={cx} cy={cy} r="8" fill="#14532d" opacity="0.7" />
      ))}

      {ZONES.map(z => {
        const a = byArea.get(z.area)
        const share = a ? a.netBookValue / maxNbv : 0
        const isSelected = selected === z.area
        return (
          <g key={z.area} className="plan-zone" onClick={() => onSelect(z.area)} tabIndex={0}
             onKeyDown={e => (e.key === 'Enter' || e.key === ' ') && onSelect(z.area)}>
            <rect x={z.x} y={z.y} width={z.w} height={z.h} rx="4"
                  fill={`rgba(45, 212, 191, ${0.08 + 0.5 * share})`} />
            <rect x={z.x} y={z.y} width={z.w} height={z.h} rx="4" fill="url(#roof)"
                  stroke={isSelected ? '#facc15' : '#2dd4bf'} strokeWidth={isSelected ? 3 : 1} strokeOpacity={isSelected ? 1 : 0.6} />
            <text x={z.x + 12} y={z.y + 24} className="plan-label">{a?.label ?? z.area}</text>
            {a && (
              <text x={z.x + 12} y={z.y + 44} className="plan-value">
                {millions(a.netBookValue)} · {a.assetCount} assets
              </text>
            )}
          </g>
        )
      })}
    </svg>
  )
}
