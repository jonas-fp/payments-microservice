import { NavLink } from 'react-router-dom';

import './Navbar.css';

export function Navbar() {
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
