
import React from 'react';
import { Link } from 'react-router-dom';

const Home = () => {
  return (
    <div className="text-center">
      <h2 className="text-4xl font-bold text-gray-800 mb-4">Witaj w Sports Betting Manager!</h2>
      <p className="text-lg text-gray-600 mb-8">
        Twoje centralne miejsce do zarządzania zakładami sportowymi. Śledź swoje postępy, analizuj statystyki i podejmuj lepsze decyzje.
      </p>
      <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
        <Link to="/login" className="bg-white shadow-lg rounded-lg p-8 hover:shadow-xl transition-shadow duration-300">
          <h3 className="text-2xl font-semibold text-gray-800 mb-2">Zaloguj się</h3>
          <p className="text-gray-600">Masz już konto? Zaloguj się, aby kontynuować.</p>
        </Link>
        <Link to="/register" className="bg-white shadow-lg rounded-lg p-8 hover:shadow-xl transition-shadow duration-300">
          <h3 className="text-2xl font-semibold text-gray-800 mb-2">Zarejestruj się</h3>
          <p className="text-gray-600">Nie masz jeszcze konta? Dołącz do nas i zacznij zarządzać swoimi zakładami.</p>
        </Link>
      </div>
    </div>
  );
};

export default Home;
