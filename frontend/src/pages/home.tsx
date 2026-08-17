import { Link } from 'react-router-dom'

export default function Home() {
    return (
        <div>
            <p>Welcome to payments dashboard!</p>
            <Link to="/ledger/trial-balance">Trial Balance</Link>
        </div>
    )
}