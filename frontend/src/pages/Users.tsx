import { Fragment, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { api, label, type User, type UserRole } from '../api'
import { hasRole, useAuth } from '../auth'

/** Permission management: which roles each person holds. Admins only. */
export default function Users() {
  const { user } = useAuth()
  const [users, setUsers] = useState<User[]>([])
  const [roles, setRoles] = useState<{ role: UserRole; description: string }[]>([])
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!hasRole(user, 'ADMIN')) return
    Promise.all([api.users(), api.roles()])
      .then(([u, r]) => { setUsers(u); setRoles(r) })
      .catch(e => setError(e.message))
  }, [user])

  if (!hasRole(user, 'ADMIN')) {
    return (
      <section>
        <h1>Users and roles</h1>
        <p className="muted">Only an administrator can manage roles. <Link to="/login">Sign in as Sam King (admin)</Link> to try it.</p>
      </section>
    )
  }

  const toggle = async (u: User, role: UserRole) => {
    const next = u.roles.includes(role) ? u.roles.filter(r => r !== role) : [...u.roles, role]
    try {
      const saved = await api.updateRoles(u.username, next)
      setUsers(list => list.map(x => (x.username === saved.username ? saved : x)))
      setError(null)
    } catch (e) {
      setError((e as Error).message)
    }
  }

  return (
    <section>
      <h1>Users and roles</h1>
      <p className="muted">Tick a box to grant a role. Changes apply the next time that person signs in.</p>
      {error && <p className="error">{error}</p>}
      <div className="panel table-wrap">
        <table>
          <thead>
            <tr>
              <th>Person</th><th>Plant</th>
              {roles.map(r => <th key={r.role} title={r.description}>{label(r.role)}</th>)}
            </tr>
          </thead>
          <tbody>
            {users.map(u => (
              <tr key={u.username}>
                <td><strong>{u.displayName}</strong> <span className="muted small block">{u.username} · {u.title}</span></td>
                <td>{u.plantCode ?? 'All'}</td>
                {roles.map(r => (
                  <td key={r.role}>
                    <input type="checkbox" aria-label={`${u.displayName}: ${label(r.role)}`}
                           checked={u.roles.includes(r.role)} onChange={() => toggle(u, r.role)} />
                  </td>
                ))}
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <div className="panel">
        <h2>What each role can do</h2>
        <dl className="facts">
          {roles.map(r => <Fragment key={r.role}><dt>{label(r.role)}</dt><dd>{r.description}</dd></Fragment>)}
        </dl>
      </div>
    </section>
  )
}
