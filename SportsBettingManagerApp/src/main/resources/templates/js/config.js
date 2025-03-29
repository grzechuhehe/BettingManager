// Konfiguracja API
const API_URL = 'http://localhost:8080';
const API_ENDPOINTS = {
    auth: {
        login: '/api/auth/signin',
        register: '/api/auth/signup',
        resetPassword: '/api/auth/reset-password'
    },
    bets: {
        create: '/bets',
        getStats: '/bets/stats',
        getAdvancedStats: '/bets/advanced-stats',
        getHeatmapData: '/bets/heatmap'
    }
};

export { API_URL, API_ENDPOINTS }; 