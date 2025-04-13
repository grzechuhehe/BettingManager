import { handleLogin, handleRegister, handlePasswordReset, handleLogout } from './auth.js';
import { getState, setState } from './state.js';
import { showNotification } from './utils.js';
import { API_URL } from './config.js';

// Funkcje do zarządzania widokami
export function showView(viewId) {
    // Ukryj wszystkie kontenery treści
    document.querySelectorAll('.content-container').forEach(view => {
        view.classList.add('hidden');
    });
    
    // Pokaż wybrany widok
    const selectedView = document.getElementById(viewId);
    if (selectedView) {
        selectedView.classList.remove('hidden');
        setState({ currentView: viewId });
    }
}

// Funkcja do przełączania formularzy uwierzytelniania
export function showAuthForm(formId) {
    // Ukryj wszystkie formularze uwierzytelniania
    document.querySelectorAll('.form-container').forEach(form => {
        form.classList.add('hidden');
    });
    
    // Pokaż wybrany formularz
    const selectedForm = document.getElementById(formId);
    if (selectedForm) {
        selectedForm.classList.remove('hidden');
    }
}

export function updateNavigation() {
    const token = getState().token;
    
    // Pokaż/ukryj nawigację w zależności od stanu uwierzytelnienia
    const navigation = document.getElementById('navigation');
    if (navigation) {
        navigation.classList.toggle('hidden', !token);
    }
    
    // Pokaż odpowiedni kontener w zależności od stanu uwierzytelnienia
    const authForms = document.getElementById('auth-forms');
    if (authForms) {
        authForms.classList.toggle('hidden', !!token);
    }
    
    if (token) {
        // Jeśli zalogowany, pokaż dashboard
        showView('dashboard');
    } else {
        // Jeśli nie zalogowany, pokaż formularze uwierzytelniania
        document.querySelectorAll('.content-container').forEach(view => {
            view.classList.add('hidden');
        });
    }
}

// Inicjalizacja aplikacji
document.addEventListener('DOMContentLoaded', () => {
    // Ustaw obsługę formularzy
    setupFormHandlers();
    
    // Ustaw obsługę nawigacji
    setupNavigationHandlers();
    
    // Sprawdź, czy użytkownik jest zalogowany
    const token = localStorage.getItem('token');
    const userId = localStorage.getItem('userId');
    
    if (token && userId) {
        // Najpierw sprawdź, czy token jest ważny przed jego użyciem
        verifyToken(token)
          .then(valid => {
            if (valid) {
                setState({ token, userId });
                updateNavigation();
            } else {
                // Token jest nieprawidłowy, usuń dane z localStorage
                localStorage.removeItem('token');
                localStorage.removeItem('userId');
                showAuthForm('login-form');
            }
          })
          .catch(() => {
            // W przypadku błędu, dla bezpieczeństwa wyloguj
            localStorage.removeItem('token');
            localStorage.removeItem('userId');
            showAuthForm('login-form');
          });
    } else {
        // Domyślnie pokaż formularz logowania
        showAuthForm('login-form');
    }
});

// Funkcje do obsługi formularzy
function setupFormHandlers() {
    // Formularz logowania
    const loginForm = document.getElementById('login');
    if (loginForm) {
        loginForm.addEventListener('submit', handleLogin);
    }
    
    // Formularz rejestracji
    const registerForm = document.getElementById('register');
    if (registerForm) {
        registerForm.addEventListener('submit', handleRegister);
    }
    
    // Formularz resetowania hasła
    const resetForm = document.getElementById('reset');
    if (resetForm) {
        resetForm.addEventListener('submit', handlePasswordReset);
    }
    
    // Formularz dodawania zakładu
    const addBetForm = document.getElementById('add-bet-form');
    if (addBetForm) {
        addBetForm.addEventListener('submit', (e) => {
            e.preventDefault();
            import('./bets.js').then(module => {
                module.handleAddBet(e);
            });
        });
    }
    
    // Przełączanie między formularzami uwierzytelniania
    const showRegisterLink = document.getElementById('show-register');
    if (showRegisterLink) {
        showRegisterLink.addEventListener('click', (e) => {
            e.preventDefault();
            showAuthForm('register-form');
        });
    }
    
    const showLoginLink = document.getElementById('show-login');
    if (showLoginLink) {
        showLoginLink.addEventListener('click', (e) => {
            e.preventDefault();
            showAuthForm('login-form');
        });
    }
    
    const showResetLink = document.getElementById('show-reset');
    if (showResetLink) {
        showResetLink.addEventListener('click', (e) => {
            e.preventDefault();
            showAuthForm('reset-form');
        });
    }
    
    const backToLoginLink = document.getElementById('back-to-login');
    if (backToLoginLink) {
        backToLoginLink.addEventListener('click', (e) => {
            e.preventDefault();
            showAuthForm('login-form');
        });
    }
}

// Funkcje do obsługi nawigacji
function setupNavigationHandlers() {
    // Dashboard link
    const dashboardLink = document.getElementById('dashboard-link');
    if (dashboardLink) {
        dashboardLink.addEventListener('click', (e) => {
            e.preventDefault();
            showView('dashboard');
            loadDashboardData();
        });
    }
    
    // Add bet link
    const addBetLink = document.getElementById('add-bet-link');
    if (addBetLink) {
        addBetLink.addEventListener('click', (e) => {
            e.preventDefault();
            showView('add-bet');
        });
    }
    
    // Stats link
    const statsLink = document.getElementById('stats-link');
    if (statsLink) {
        statsLink.addEventListener('click', (e) => {
            e.preventDefault();
            showView('stats');
            loadStatsData();
        });
    }
    
    // Advanced stats link
    const advancedStatsLink = document.getElementById('advanced-stats-link');
    if (advancedStatsLink) {
        advancedStatsLink.addEventListener('click', (e) => {
            e.preventDefault();
            showView('advanced-stats');
            loadAdvancedStatsData();
        });
    }
    
    // Logout link
    const logoutLink = document.getElementById('logout-link');
    if (logoutLink) {
        logoutLink.addEventListener('click', (e) => {
            e.preventDefault();
            handleLogout();
            updateNavigation();
            showAuthForm('login-form');
        });
    }
}

// Funkcja do weryfikacji tokena JWT
async function verifyToken(token) {
    try {
        if (!token) {
            console.error('Brak tokena do weryfikacji');
            return false;
        }
        
        // Sprawdź format tokena JWT (powinien mieć 3 części oddzielone kropką)
        const parts = token.split('.');
        if (parts.length !== 3) {
            console.error('Token ma nieprawidłowy format. Powinien składać się z 3 części:', parts.length);
            return false;
        }
        
        let payload;
        try {
            // Zdekoduj część payloadu tokena (druga część)
            const base64Url = parts[1];
            const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
            const jsonPayload = decodeURIComponent(atob(base64).split('').map(function(c) {
                return '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2);
            }).join(''));
            
            payload = JSON.parse(jsonPayload);
            console.log('Zdekodowany payload tokena JWT:', payload);
        } catch (e) {
            console.error('Nie udało się zdekodować payloadu tokena:', e);
            return false;
        }
        
        // Sprawdź, czy token zawiera pole exp (czas wygaśnięcia)
        if (!payload.exp) {
            console.log('Token nie zawiera informacji o czasie wygaśnięcia (exp)');
            // Dla bezpieczeństwa zakładamy, że token bez czasu wygaśnięcia jest nieważny
            return false;
        }
        
        // Sprawdź czas wygaśnięcia
        const expirationTime = payload.exp * 1000; // Konwersja na milisekundy
        const currentTime = Date.now();
        
        console.log('Czas wygaśnięcia tokena:', new Date(expirationTime).toLocaleString());
        console.log('Aktualny czas:', new Date(currentTime).toLocaleString());
        
        if (currentTime >= expirationTime) {
            console.log('Token wygasł');
            return false;
        }
        
        // Dodatkowe logowanie dla innych ważnych pól w tokenie
        if (payload.sub) {
            console.log('Subject tokena (username):', payload.sub);
        }
        
        if (payload.iss) {
            console.log('Wydawca tokena:', payload.iss);
        }
        
        return true;
    } catch (error) {
        console.error('Nieoczekiwany błąd podczas weryfikacji tokena:', error);
        return false;
    }
}

// Funkcje do ładowania danych
async function loadDashboardData() {
    try {
        import('./bets.js').then(module => {
            if (module.loadDashboardData) {
                module.loadDashboardData();
            }
        });
    } catch (error) {
        showNotification('Błąd podczas ładowania danych dashboardu', 'error');
    }
}

async function loadStatsData() {
    try {
        import('./bets.js').then(module => {
            if (module.loadStatsData) {
                module.loadStatsData();
            }
        });
    } catch (error) {
        showNotification('Błąd podczas ładowania statystyk', 'error');
    }
}

async function loadAdvancedStatsData() {
    try {
        import('./bets.js').then(module => {
            if (module.loadAdvancedStatsData) {
                module.loadAdvancedStatsData();
            }
        });
    } catch (error) {
        showNotification('Błąd podczas ładowania zaawansowanych statystyk', 'error');
    }
}