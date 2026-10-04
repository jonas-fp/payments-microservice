import { NavLink } from 'react-router-dom';

import './Navbar.css';

export function Navbar() {
  return (
    <nav>
      <ul className='navbar'>
        <li>
          <img
            className='navbar-logo'
            src='../../public/favicon.svg'
            alt='Company logo'
          />
        </li>
        <li>
          <NavLink className='navbar-link' to='/home'>
            Home
          </NavLink>
        </li>
        <li>
          <NavLink className='navbar-link' to='/ledger/trial-balance'>
            Trial Balance
          </NavLink>
        </li>
        <li>
          <NavLink className='navbar-link' to='/payments/payment-details'>
            Payment Details
          </NavLink>
        </li>
        <li>
          <NavLink className='navbar-link' to='/reconciliation/breaks'>
            Reconciliation Breaks
          </NavLink>
        </li>
      </ul>
    </nav>
  );
}
