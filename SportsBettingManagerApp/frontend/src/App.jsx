import React from 'react';
import { Routes, Route, Link } from 'react-router-dom';
import Register from './components/Register';
import Login from './components/Login';
import Dashboard from './components/Dashboard';
import { AuthProvider } from './context/AuthContext';

function App() {
  return (
    <div className="min-h-screen bg-gray-100 flex flex-col items-center justify-center p-4">
      <header className="w-full max-w-md bg-white shadow-md rounded-lg p-6 mb-6 text-center">
        <h1 className="text-3xl font-bold text-gray-800 mb-4">Sports Betting Manager</h1>
        <nav className="space-x-4">
          <Link to="/register" className="text-blue-500 hover:text-blue-700">Register</Link>
          <Link to="/login" className="text-blue-500 hover:text-blue-700">Login</Link>
          <Link to="/dashboard" className="text-blue-500 hover:text-blue-700">Dashboard</Link>
        </nav>
      </header>
      <main className="w-full max-w-md bg-white shadow-md rounded-lg p-6">
        <AuthProvider>
          <Routes>
            <Route path="/register" element={<Register />} />
            <Route path="/login" element={<Login />} />
            <Route path="/dashboard" element={<Dashboard />} />
            <Route path="/" element={<Login />} />
          </Routes>
        </AuthProvider>
      </main>
    </div>
  );
}

export default App;
