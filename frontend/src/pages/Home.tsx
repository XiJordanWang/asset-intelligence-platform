import { Link } from 'react-router-dom'

const MODULES = [
  { title: 'Asset', text: 'Asset register, depreciation and verification across every plant and site.', to: '/assets', ready: true },
  { title: 'Lifecycle approval', text: 'Investment requests routed by amount to the right approvers, then capitalised.', to: '/requests', ready: true },
  { title: 'Management report', text: 'Cockpit with book values, depreciation and a map of all sites.', to: '/cockpit', ready: true },
  { title: 'Invoice', text: 'AI reads supplier invoices and matches them to approved requests.', ready: false },
  { title: 'Self-service desk', text: 'Ask questions about capex policy and get answers with sources (RAG).', ready: false },
]

export default function Home() {
  return (
    <section>
      <div className="hero">
        <h1>One place for every asset, from request to retirement</h1>
        <p>
          A demo platform for <strong>Kowhai Components Ltd</strong>, a fictional New Zealand manufacturer with four
          plants and 48 sites. Built with Java 21, Spring Boot and React; AI features are being added next.
        </p>
      </div>
      <div className="modules">
        {MODULES.map(m => {
          const body = (
            <>
              <h2>{m.title}</h2>
              <p>{m.text}</p>
              <span className={m.ready ? 'pill ok' : 'pill soon'}>{m.ready ? 'Open' : 'Coming soon'}</span>
            </>
          )
          return m.ready && m.to ? (
            <Link key={m.title} to={m.to} className="module">{body}</Link>
          ) : (
            <div key={m.title} className="module disabled">{body}</div>
          )
        })}
      </div>
    </section>
  )
}
