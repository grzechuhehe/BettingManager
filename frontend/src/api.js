import axios from 'axios';

// Konfiguracja centralnej instancji klienta API
const apiClient = axios.create({
  baseURL: 'https://localhost:8443/api',
  headers: {
    'Content-Type': 'application/json',
  },
});

// Interceptor do automatycznego dodawania tokena JWT do nagłówków
apiClient.interceptors.request.use(
  (config) => {
    // Pobieramy dane użytkownika z localStorage
    const userStr = localStorage.getItem('user');
    if (userStr) {
      const user = JSON.parse(userStr);
      const token = user?.token || user?.accessToken; // Elastyczność na różne nazwy pola z tokenem
      if (token) {
        config.headers.Authorization = `Bearer ${token}`;
      }
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// --- Funkcje API dla autoryzacji ---
export const login = (credentials) => {
  return apiClient.post('/auth/signin', credentials);
};

export const register = (userData) => {
  return apiClient.post('/auth/signup', userData);
};

// --- Funkcje API dla zakładów (Bets) ---
export const getBets = () => {
  return apiClient.get('/bets');
};

export const getDashboardStats = () => {
  return apiClient.get('/bets/dashboard-stats');
};

export const addBet = (betData) => {
  return apiClient.post('/bets/add-bet', betData);
};

export const settleBet = (id, status) => {

  return apiClient.patch(`/bets/${id}/settle`, { status });

};



export const updateBet = (id, betData) => {

  return apiClient.put(`/bets/${id}`, betData);

};



export const deleteBet = (id) => {

  return apiClient.delete(`/bets/${id}`);

};



export default apiClient;
