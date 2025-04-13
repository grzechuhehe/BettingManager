// Konfiguracja API
const API_URL = ''; // Pusty, żeby używać relatywnych URLi (ten sam host)
const API_ENDPOINTS = {
    auth: {
        login: '/api/auth/signin',
        register: '/api/auth/signup',
        resetPassword: '/api/auth/reset-password'
    },
    bets: {
        create: '/api/bets',
        getStats: '/api/bets/stats',
        getAdvancedStats: '/api/bets/advanced-stats',
        getHeatmapData: '/api/bets/heatmap'
    }
};

export { API_URL, API_ENDPOINTS }; 