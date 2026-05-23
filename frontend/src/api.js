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

export const forgotPassword = (email) => {
  return apiClient.post('/auth/forgot-password', { email });
};

export const resetPassword = (token, newPassword) => {
  return apiClient.post('/auth/reset-password', { token, newPassword });
};

// --- Funkcje API dla zakładów (Bets) ---
export const getBets = () => {
  return apiClient.get('/bets');
};

export const getDashboardStats = () => {
  return apiClient.get('/bets/dashboard-stats');
};

export const getAdvancedStats = () => {
  return apiClient.get('/bets/advanced-stats');
};

export const getHeatmapData = () => {
  return apiClient.get('/bets/heatmap');
};

// --- Funkcje API dla Użytkownika ---
export const getUserProfile = () => {
  return apiClient.get('/user/profile');
};

export const changePassword = (passwordData) => {
  return apiClient.post('/user/change-password', passwordData);
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

// --- Funkcje API dla kursów (Odds) ---
export const getAvailableSports = () => {
  return apiClient.get('/odds/sports');
};

export const getLiveOdds = (sportKey) => {
  return apiClient.get(`/odds/markets/${sportKey}`);
};

// --- Funkcje API dla EV ---
export const calculateExpectedValue = (eventQuery, bookmakerOdds) => {
    return apiClient.post('/ev/calculate', { eventQuery, bookmakerOdds });
};

export const getEvOpportunities = () => {
    return apiClient.get('/ev/opportunities');
};

// --- Funkcje API dla Social Betting Profiles ---
export const getTrackedProfiles = () => apiClient.get('/profiles/tracked');
export const trackNewProfile = (xUsername) => apiClient.post('/profiles/track', { xUsername });
export const triggerManualScan = (xUsername) => apiClient.post(`/profiles/${xUsername}/scan`);
export const getTrackedProfilePicks = (xUsername, page = 0, size = 10) => apiClient.get(`/profiles/${xUsername}/picks?page=${page}&size=${size}`);
export const getTrackedProfileStats = (xUsername) => apiClient.get(`/profiles/${xUsername}/stats`);
export const getTrackedProfileAdvancedStats = (xUsername) => apiClient.get(`/profiles/${xUsername}/advanced-stats`);

export default apiClient;
