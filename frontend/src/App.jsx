import React, { useState } from 'react';
import { Routes, Route, Link, Navigate, useLocation, useNavigate } from 'react-router-dom';
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
import SocialBettingDashboard from './components/social/SocialBettingDashboard';
import TrackedProfileView from './components/social/TrackedProfileView';
import { AuthProvider, useAuth } from './context/AuthContext';

const ProtectedRoute = ({ children }) => {
  const { isAuthenticated, isLoading } = useAuth();
  const location = useLocation();

  if (isLoading) {
    return <div className="text-center p-4 text-body animate-fade-in">Loading...</div>;
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  return children;
};

const Navigation = () => {
  const { isAuthenticated, logout } = useAuth();
  const navigate = useNavigate();
  const [searchInput, setSearchInput] = useState('');

  const handleSearchSubmit = (e) => {
    e.preventDefault();
    if (searchInput.trim()) {
      const cleanUsername = searchInput.replace('@', '').trim();
      navigate(`/social?q=${encodeURIComponent(cleanUsername)}`);
      setSearchInput('');
    }
  };

  if (!isAuthenticated) {
    return (
      <nav className="flex items-center gap-4">
        <Link to="/login" className="button-text-link">Sign In</Link>
        <Link to="/register" className="button-primary">Get Started</Link>
      </nav>
    );
  }

  return (
    <nav className="flex items-center gap-6">
      <Link to="/dashboard" className="nav-link">Dashboard</Link>
      <Link to="/add-bet" className="nav-link">Add Bet</Link>
      <Link to="/bets" className="nav-link">My Bets</Link>
      <Link to="/live-odds" className="nav-link">Open Odds</Link>
      <form onSubmit={handleSearchSubmit} className="relative flex items-center">
        <input
          type="text"
          value={searchInput}
          onChange={(e) => setSearchInput(e.target.value)}
          placeholder="Search X profiles..."
          className="input-field w-48"
        />
      </form>
      <Link to="/profile" className="nav-link">Profile</Link>
      <button type="button" onClick={logout} className="button-primary">Logout</button>
    </nav>
  );
};

const AppLayout = () => {
  const location = useLocation();

  return (
    <div className="min-h-screen w-full bg-canvas flex flex-col items-center">
      <header className="top-nav flex justify-center">
        <div className="w-full max-w-7xl px-6 flex justify-between items-center h-16">
          <Link to="/" className="text-xl font-bold tracking-tight text-on-dark flex items-center">
            <span className="text-primary mr-2">●</span> SportsBettingManager
          </Link>
          <Navigation />
        </div>
      </header>
      <main className="w-full max-w-7xl px-6 py-section">
        <div key={location.pathname} className="animate-page-enter">
          <Routes>
            <Route path="/" element={<HomeOrDashboard />} />
            <Route path="/register" element={<Register />} />
            <Route path="/login" element={<Login />} />
            <Route path="/forgot-password" element={<ForgotPassword />} />
            <Route path="/reset-password" element={<ResetPassword />} />
            <Route path="/dashboard" element={<ProtectedRoute><Dashboard /></ProtectedRoute>} />
            <Route path="/add-bet" element={<ProtectedRoute><AddBetForm /></ProtectedRoute>} />
            <Route path="/bets" element={<ProtectedRoute><BetList /></ProtectedRoute>} />
            <Route path="/profile" element={<ProtectedRoute><UserProfile /></ProtectedRoute>} />
            <Route path="/live-odds" element={<ProtectedRoute><LiveMarkets /></ProtectedRoute>} />
            <Route path="/ev-calculator" element={<ProtectedRoute><EvCalculator /></ProtectedRoute>} />
            <Route path="/social" element={<ProtectedRoute><SocialBettingDashboard /></ProtectedRoute>} />
            <Route path="/profile/:username" element={<ProtectedRoute><TrackedProfileView /></ProtectedRoute>} />
            <Route path="*" element={<Navigate to="/" />} />
          </Routes>
        </div>
      </main>
    </div>
  );
};

const HomeOrDashboard = () => {
  const { isAuthenticated } = useAuth();
  return !isAuthenticated ? <Home /> : <Navigate to="/dashboard" />;
};

function App() {
  return (
    <AuthProvider>
      <AppLayout />
    </AuthProvider>
  );
}

export default App;
