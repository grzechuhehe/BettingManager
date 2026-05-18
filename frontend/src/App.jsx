import React from 'react';
import { Routes, Route, Link, Navigate, useLocation } from 'react-router-dom';
import Register from './components/Register';
import Login from './components/Login';
import Dashboard from './components/Dashboard';
import AddBetForm from './components/AddBetForm';
import BetList from './components/BetList';
import Home from './components/Home';
import UserProfile from './components/UserProfile';
import LiveMarkets from './components/LiveMarkets';
import ForgotPassword from './components/ForgotPassword';
import ResetPassword from './components/ResetPassword';
import EvCalculator from './components/EvCalculator';
import { AuthProvider, useAuth } from './context/AuthContext';

// Komponent do ochrony ścieżek
const ProtectedRoute = ({ children }) => {
  const { isAuthenticated, isLoading } = useAuth();
  const location = useLocation();

  if (isLoading) {
    return <div className="text-center p-4">Loading...</div>;
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  return children;
};

// Komponent nawigacji
const Navigation = () => {
  const { isAuthenticated, logout } = useAuth();

  return (
    <nav className="flex items-center space-x-6">
      {isAuthenticated && (
        <>
          <Link to="/dashboard" className="text-sm text-body hover:text-on-dark font-medium transition-colors">Dashboard</Link>
          <Link to="/add-bet" className="text-sm text-body hover:text-on-dark font-medium transition-colors">Add Bet</Link>
          <Link to="/bets" className="text-sm text-body hover:text-on-dark font-medium transition-colors">My Bets</Link>
          <Link to="/live-odds" className="text-sm text-body hover:text-on-dark font-medium transition-colors">Open Odds</Link>
          <Link to="/ev-calculator" className="text-sm text-body hover:text-on-dark font-medium transition-colors">+EV Engine</Link>
          <Link to="/profile" className="text-sm text-body hover:text-on-dark font-medium transition-colors">Profile</Link>
          <button onClick={logout} className="button-primary text-sm">Logout</button>
        </>
      )}
    </nav>
  );
};

// Główny layout aplikacji
const AppLayout = () => {
  const { isAuthenticated } = useAuth();

  return (
    <div className="min-h-screen w-full bg-canvas flex flex-col items-center">
      <header className="w-full h-16 border-b border-hairline bg-canvas/80 backdrop-blur-md flex justify-center sticky top-0 z-50">
        <div className="w-full max-w-7xl px-6 flex justify-between items-center">
          <Link to="/" className="text-xl font-bold tracking-tight text-on-dark flex items-center">
             <span className="text-primary mr-2">●</span> SportsBettingManager
          </Link>
          <Navigation />
        </div>
      </header>
      <main className="w-full max-w-7xl px-6 py-24">

        <Routes>
          <Route path="/" element={!isAuthenticated ? <Home /> : <Navigate to="/dashboard" />} />
          <Route path="/register" element={<Register />} />
          <Route path="/login" element={<Login />} />
          <Route path="/forgot-password" element={<ForgotPassword />} />
          <Route path="/reset-password" element={<ResetPassword />} />
          <Route 
            path="/dashboard"
            element={
              <ProtectedRoute>
                <Dashboard />
              </ProtectedRoute>
            }
          />
          <Route 
            path="/add-bet"
            element={
              <ProtectedRoute>
                <AddBetForm />
              </ProtectedRoute>
            }
          />
          <Route 
            path="/bets"
            element={
              <ProtectedRoute>
                <BetList />
              </ProtectedRoute>
            }
          />
          <Route 
            path="/profile"
            element={
              <ProtectedRoute>
                <UserProfile />
              </ProtectedRoute>
            }
          />
          <Route 
            path="/live-odds"
            element={
              <ProtectedRoute>
                <LiveMarkets />
              </ProtectedRoute>
            }
          />
          <Route 
            path="/ev-calculator"
            element={
              <ProtectedRoute>
                <EvCalculator />
              </ProtectedRoute>
            }
          />
          <Route path="*" element={<Navigate to="/" />} />
        </Routes>
      </main>
    </div>
  );
}

function App() {
  return (
    <AuthProvider>
      <AppLayout />
    </AuthProvider>
  );
}

export default App;