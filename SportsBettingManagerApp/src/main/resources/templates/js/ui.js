import { getState, setState } from './state.js';
import { showNotification } from './utils.js';

// Funkcje do zarządzania widokami
export function showView(viewId) {
    // Ukryj wszystkie widoki
    document.querySelectorAll('.view').forEach(view => {
        view.style.display = 'none';
    });
    
    // Pokaż wybrany widok
    const selectedView = document.getElementById(viewId);
    if (selectedView) {
        selectedView.style.display = 'block';
        setState({ currentView: viewId });
    }
}

export function updateNavigation() {
    const token = getState().token;
    const authLinks = document.querySelectorAll('.auth-link');
    const protectedLinks = document.querySelectorAll('.protected-link');
    
    authLinks.forEach(link => {
        link.style.display = token ? 'none' : 'block';
    });
    
    protectedLinks.forEach(link => {
        link.style.display = token ? 'block' : 'none';
    });
}

// Funkcje do obsługi formularzy
export function setupFormHandlers() {
    // Formularz logowania
    const loginForm = document.getElementById('login-form');
    if (loginForm) {
        loginForm.addEventListener('submit', (e) => {
            e.preventDefault();
            const username = document.getElementById('login-username').value;
            const password = document.getElementById('login-password').value;
            
            // Wywołaj funkcję logowania z auth.js
            import('./auth.js').then(module => {
                module.handleLogin(username, password);
            });
        });
    }
    
    // Formularz rejestracji
    const registerForm = document.getElementById('register-form');
    if (registerForm) {
        registerForm.addEventListener('submit', (e) => {
            e.preventDefault();
            const username = document.getElementById('register-username').value;
            const email = document.getElementById('register-email').value;
            const password = document.getElementById('register-password').value;
            
            // Wywołaj funkcję rejestracji z auth.js
            import('./auth.js').then(module => {
                module.handleRegister(username, email, password);
            });
        });
    }
    
    // Formularz dodawania zakładu
    const addBetForm = document.getElementById('add-bet-form');
    if (addBetForm) {
        addBetForm.addEventListener('submit', (e) => {
            e.preventDefault();
            
            // Wywołaj funkcję dodawania zakładu z bets.js
            import('./bets.js').then(module => {
                module.handleAddBet(e);
            });
        });
    }
}

// Funkcje do obsługi nawigacji
export function setupNavigationHandlers() {
    // Obsługa linków nawigacyjnych
    document.querySelectorAll('a[data-view]').forEach(link => {
        link.addEventListener('click', (e) => {
            e.preventDefault();
            const viewId = link.getAttribute('data-view');
            showView(viewId);
            
            // Załaduj dane dla danego widoku
            loadViewData(viewId);
        });
    });
    
    // Obsługa przycisku wylogowania
    const logoutButton = document.getElementById('logout-button');
    if (logoutButton) {
        logoutButton.addEventListener('click', () => {
            import('./auth.js').then(module => {
                module.handleLogout();
            });
        });
    }
}

// Funkcja do ładowania danych dla danego widoku
async function loadViewData(viewId) {
    if (!getState().token) return;
    
    try {
        switch (viewId) {
            case 'dashboard-view':
                await import('./bets.js').then(module => {
                    module.loadDashboardData();
                });
                break;
                
            case 'stats-view':
                await import('./bets.js').then(module => {
                    module.loadStatsData();
                });
                break;
                
            case 'advanced-stats-view':
                await import('./bets.js').then(module => {
                    module.loadAdvancedStatsData();
                });
                break;
        }
    } catch (error) {
        showNotification('Wystąpił błąd podczas ładowania danych.', 'error');
    }
}

// Funkcja inicjalizująca aplikację
export function initializeApp() {
    // Sprawdź, czy użytkownik jest zalogowany
    const token = localStorage.getItem('token');
    const userId = localStorage.getItem('userId');
    
    if (token && userId) {
        setState({ token, userId });
        updateNavigation();
        
        // Pokaż domyślny widok (dashboard)
        showView('dashboard-view');
        loadViewData('dashboard-view');
    } else {
        // Pokaż widok logowania
        showView('login-view');
    }
    
    // Ustaw obsługę formularzy i nawigacji
    setupFormHandlers();
    setupNavigationHandlers();
} 