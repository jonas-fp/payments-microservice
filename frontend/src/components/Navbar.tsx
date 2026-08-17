import { NavLink } from 'react-router-dom';

export default function Navbar() {
  return (
    <nav>
      <ul>
        <li>
          <NavLink to='/home'>Home</NavLink>
        </li>
        <li>
          <NavLink to='/ledger/trial-balance'>Trial Balance</NavLink>
        </li>
      </ul>
    </nav>
  );
}
