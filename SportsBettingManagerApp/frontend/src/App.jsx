import React from 'react';
import { Routes, Route, Link, Navigate, useLocation } from 'react-router-dom';
import Register from './components/Register';
import Login from './components/Login';
import Dashboard from './components/Dashboard';
import AddBetForm from './components/AddBetForm';
import Home from './components/Home';
import { AuthProvider, useAuth } from './context/AuthContext';

// Komponent do ochrony ścieżek
const ProtectedRoute = ({ children }) => {
  const { isAuthenticated } = useAuth();
  const location = useLocation();

  if (!isAuthenticated) {
    // Przekieruj na stronę logowania, zapamiętując, skąd przyszedł użytkownik
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  return children;
};

// Komponent nawigacji, który zmienia się w zależności od stanu logowania
const Navigation = () => {
  const { isAuthenticated, logout } = useAuth();

  return (
    <nav className="space-x-4">
      {isAuthenticated && (
        <>
          <Link to="/dashboard" className="text-blue-500 hover:text-blue-700">Dashboard</Link>
          <Link to="/add-bet" className="text-blue-500 hover:text-blue-700">Add Bet</Link>
          <button onClick={logout} className="text-blue-500 hover:text-blue-700">Logout</button>
        </>
      )}
    </nav>
  );
};

// Główny layout aplikacji
const AppLayout = () => {
  const { isAuthenticated } = useAuth();

  return (
    <div className="min-h-screen bg-gray-100 flex flex-col items-center p-4">
      <header className="w-full max-w-4xl bg-white shadow-md rounded-lg p-6 mb-6">
        <div className="flex justify-between items-center">
          <h1 className="text-3xl font-bold text-gray-800">Sports Betting Manager</h1>
          <Navigation />
        </div>
      </header>
      <main className="w-full max-w-4xl">
        <Routes>
          <Route path="/" element={!isAuthenticated ? <Home /> : <Navigate to="/dashboard" />} />
          <Route path="/register" element={<Register />} />
          <Route path="/login" element={<Login />} />
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
          <Route path="*" element={<Navigate to="/" />} />
        </Routes>
      </main>
    </div>
  );
}

// Główny komponent App, który dostarcza kontekst autoryzacji
function App() {
  return (
    <AuthProvider>
      <AppLayout />
    </AuthProvider>
  );
}

export default App;