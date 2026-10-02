import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { Navbar } from './components/Navbar.tsx';

// TODO: implement lazy loading to prevent the client from downloading every
//       page at once.
import { Home } from './pages/home';
import { TrialBalancePage } from './pages/ledger/trial-balance';
import { PaymentDetailsPage } from './pages/payments/payment-details.tsx';
import './App.css';

export function App() {
  return (
    <BrowserRouter>
      <div className='dashboard-container'>
        <Navbar />

        <Routes>
          <Route path='/' element={<Navigate to='/home' replace />} />

          <Route path='/home' element={<Home />} />

          <Route path='/ledger/trial-balance' element={<TrialBalancePage />} />

          <Route
            path='/payments/payment-details/:paymentId'
            element={<PaymentDetailsPage />}
          />

          <Route
            path='/payments/payment-details'
            element={<PaymentDetailsPage />}
          />
        </Routes>
      </div>
    </BrowserRouter>
  );
}
