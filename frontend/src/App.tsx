import { BrowserRouter, Routes, Route, Navigate} from 'react-router-dom';
import NavBar from './components/Navbar.tsx';

// TODO: implement lazy loading to prevent the client from downloading every
//       page at once.
import Home from './pages/home';
import TrialBalancePage from './pages/ledger/trial-balance';
import './App.css';

function App() {
  return (
    <BrowserRouter>
      <div className='dashboard-container'>
        <NavBar />

        <Routes>
          <Route path='/' element={<Navigate to='/home' replace />} />

          <Route path='/home' element={<Home />} />

          <Route path='/ledger/trial-balance' element={<TrialBalancePage />} />
        </Routes>
      </div>
    </BrowserRouter>
  );
}

export default App;
