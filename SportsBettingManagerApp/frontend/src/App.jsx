import React from 'react';
import { Routes, Route, Link } from 'react-router-dom';
import Register from './components/Register';
import Login from './components/Login';
import Dashboard from './components/Dashboard'; // We will create this soon
import { AuthProvider } from './context/AuthContext';

function App() {
  return (
    <div className="App">
      <header className="App-header">
        <h1>Sports Betting Manager</h1>
        <nav>
          <Link to="/register">Register</Link> |
          <Link to="/login">Login</Link> |
          <Link to="/dashboard">Dashboard</Link> {/* New link for testing */}
        </nav>
      </header>
      <main>
        <AuthProvider> {/* Wrap the entire application with AuthProvider */}
          <Routes>
            <Route path="/register" element={<Register />} />
            <Route path="/login" element={<Login />} />
            <Route path="/dashboard" element={<Dashboard />} />
            <Route path="/" element={<Login />} /> {/* Default route */}
          </Routes>
        </AuthProvider>
      </main>
    </div>
  );
}

export default App;