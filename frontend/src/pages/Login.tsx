import { useEffect, useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { api, DEMO_PASSWORD, label, type User } from '../api'
import { useAuth } from '../auth'

/** Pick a fictional colleague to sign in as. Every demo user has the same password, shown below. */
export default function Login() {
  const { signIn, user } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const from = (location.state as { from?: string } | null)?.from ?? '/'
  const [people, setPeople] = useState<User[]>([])
  const [form, setForm] = useState({ username: '', password: '' })
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    api.personas().then(setPeople).catch(() => setPeople([]))
  }, [])

  const go = async (username: string, password: string) => {
    try {
      await signIn(username, password)
      // On the sign-in gate the page you asked for simply appears; on /login (switching user) go back.
      if (location.pathname === '/login') navigate(from, { replace: true })
    } catch (e) {
      setError((e as Error).message)
    }
  }

  return (
    <section>
      <h1>{user ? 'Switch account' : 'Choose an account to sign in'}</h1>
      <p className="muted">
        This is a demo of an asset and capex platform for the fictional Kowhai Components Ltd. Pick a person to see
        it through their role. Approvals only accept the person each step belongs to, so try the same request as
        different people. Not sure? Start with <strong>Aroha</strong> (raises requests) or <strong>Grace</strong> (finance).
        {user && <> You are signed in as <strong>{user.displayName}</strong>.</>}
      </p>
      <div className="personas">
        {people.map(p => (
          <button key={p.username} className={`persona ${user?.username === p.username ? 'current' : ''}`}
                  onClick={() => go(p.username, DEMO_PASSWORD)}>
            <span className="avatar" aria-hidden>{p.displayName.split(' ').map(w => w[0]).join('')}</span>
            <span>
              <strong>{p.displayName}</strong>
              <span className="muted small block">{p.title}</span>
              <span className="role-pills">{p.roles.map(r => <span key={r} className="pill">{label(r)}</span>)}</span>
            </span>
          </button>
        ))}
      </div>
      <form className="panel form login-form" onSubmit={e => { e.preventDefault(); go(form.username, form.password) }}>
        <h2>Or type a username</h2>
        <div className="form-row">
          <label>Username<input autoComplete="username" value={form.username}
                                onChange={e => setForm(f => ({ ...f, username: e.target.value }))} /></label>
          <label>Password<input type="password" autoComplete="current-password" value={form.password}
                                onChange={e => setForm(f => ({ ...f, password: e.target.value }))} /></label>
        </div>
        <p className="muted small">Demo password for every user: <code>{DEMO_PASSWORD}</code></p>
        {error && <p className="error">{error}</p>}
        <button className="primary" type="submit">Sign in</button>
      </form>
    </section>
  )
}
