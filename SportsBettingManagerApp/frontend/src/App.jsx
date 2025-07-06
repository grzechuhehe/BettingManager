import React from 'react';
import Register from './components/Register';
import Login from './components/Login';

function App() {
  return (
    <div className="App">
      <header className="App-header">
        <h1>Sports Betting Manager</h1>
      </header>
      <main>
        <Register />
        <hr /> {/* Separator for clarity */}
        <Login />
      </main>
    </div>
  );
}

export default App;
