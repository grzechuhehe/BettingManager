import React from 'react';
import { Routes, Route, Link, Navigate, useLocation } from 'react-router-dom';
import Register from './components/Register';
import Login from './components/Login';
import Dashboard from './components/Dashboard';
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
      {isAuthenticated ? (
        <>
          <Link to="/dashboard" className="text-blue-500 hover:text-blue-700">Dashboard</Link>
          <button onClick={logout} className="text-blue-500 hover:text-blue-700">Logout</button>
        </>
      ) : (
        <>
          <Link to="/register" className="text-blue-500 hover:text-blue-700">Register</Link>
          <Link to="/login" className="text-blue-500 hover:text-blue-700">Login</Link>
        </>
      )}
    </nav>
  );
};

// Główny layout aplikacji
const AppLayout = () => {
  return (
    <div className="min-h-screen bg-gray-100 flex flex-col items-center p-4">
      <header className="w-full max-w-4xl bg-white shadow-md rounded-lg p-6 mb-6">
        <div className="flex justify-between items-center">
          <h1 className="text-3xl font-bold text-gray-800">Sports Betting Manager</h1>
          <Navigation />
        </div>
      </header>
      <main className="w-full max-w-4xl bg-white shadow-md rounded-lg p-6">
        <Routes>
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
          <Route path="*" element={<Navigate to="/dashboard" />} />
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