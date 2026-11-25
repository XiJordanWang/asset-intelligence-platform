import { Link, useLocation } from 'react-router-dom'

/** A short line telling the visitor who can act here, with a link to sign in as them. */
export default function SignInHint({ who, action }: { who: string; action: string }) {
  const location = useLocation()
  return (
    <p className="hint">
      🔒 Only {who} can {action}.{' '}
      <Link to="/login" state={{ from: location.pathname }}>Sign in as them</Link> to try it.
    </p>
  )
}
