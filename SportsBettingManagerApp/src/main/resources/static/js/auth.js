import { API_URL, API_ENDPOINTS } from './config.js';
import { getState, setState, clearState } from './state.js';
import { showNotification, parseJwt } from './utils.js';

export async function handleLogin(e) {
    e.preventDefault();
    
    const username = document.getElementById('username').value;
    const password = document.getElementById('password').value;
    
    try {
        const response = await fetch(`${API_URL}${API_ENDPOINTS.auth.login}`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ username, password })
        });
        
        // Sprawdź najpierw typ odpowiedzi
        const contentType = response.headers.get("content-type");
        
        if (!response.ok) {
            // Obsługa błędów w zależności od typu odpowiedzi
            if (contentType && contentType.includes("application/json")) {
                // Jeśli to JSON, pobierz obiekt błędu
                const errorData = await response.json();
                throw new Error(errorData.error || 'Błąd logowania. Sprawdź dane i spróbuj ponownie.');
            } else {
                // Jeśli to tekst, pobierz wiadomość tekstową
                const errorText = await response.text();
                throw new Error(errorText || 'Błąd logowania. Sprawdź dane i spróbuj ponownie.');
            }
        }
        
        // Tylko dla poprawnych odpowiedzi parsujemy JSON
        const data = await response.json();
        console.log('Token otrzymany:', data);
        setState({ token: data.token });
        
        // Zapisz token w localStorage
        localStorage.setItem('token', data.token);
        
        // Dekoduj token JWT aby uzyskać ID użytkownika
        const payload = parseJwt(data.token);
        console.log('JWT payload:', payload);
        // Zapisujemy userId z odpowiedzi zamiast z payloadu
        setState({ userId: data.id });
        localStorage.setItem('userId', data.id);
        console.log('Zapisane userId:', data.id);
        
        showNotification('Zalogowano pomyślnie!', 'success');
        
        // Zaktualizuj nawigację i pokaż dashboard
        import('./ui.js').then(ui => {
            ui.updateNavigation();
            // Opóźnij wczytanie danych dashboardu, aby dać czas na inicjalizację
            setTimeout(() => {
                import('./bets.js').then(bets => {
                    if (bets.loadDashboardData) {
                        bets.loadDashboardData();
                    }
                });
            }, 100);
        });
        
        return true;
    } catch (error) {
        console.error('Błąd logowania:', error);
        showNotification(error.message, 'error');
        return false;
    }
}

export async function handleRegister(e) {
    e.preventDefault();
    
    const username = document.getElementById('register-username').value;
    const email = document.getElementById('register-email').value;
    const password = document.getElementById('register-password').value;
    
    try {
        const response = await fetch(`${API_URL}${API_ENDPOINTS.auth.register}`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ username, email, password })
        });
        
        // Sprawdź typ odpowiedzi
        const contentType = response.headers.get("content-type");
        
        if (!response.ok) {
            if (contentType && contentType.includes("application/json")) {
                const errorData = await response.json();
                if (errorData.error) {
                    throw new Error(errorData.error);
                } else if (Array.isArray(errorData)) {
                    throw new Error(errorData.join(', '));
                } else {
                    throw new Error("Błąd rejestracji");
                }
            } else {
                const errorText = await response.text();
                throw new Error(errorText || "Błąd rejestracji");
            }
        }
        
        const data = await response.json();
        const message = data.message || 'Konto zostało utworzone. Możesz się teraz zalogować.';
        
        showNotification(message, 'success');
        return true;
    } catch (error) {
        console.error('Błąd logowania:', error);
        showNotification(error.message, 'error');
        return false;
    }
}

export async function handlePasswordReset(e) {
    e.preventDefault();
    
    const token = document.getElementById('reset-token').value;
    const password = document.getElementById('reset-password').value;
    
    try {
        const response = await fetch(`${API_URL}${API_ENDPOINTS.auth.resetPassword}`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ token, password })
        });
        
        // Sprawdź typ odpowiedzi
        const contentType = response.headers.get("content-type");
        
        if (!response.ok) {
            if (contentType && contentType.includes("application/json")) {
                const errorData = await response.json();
                if (errorData.error) {
                    throw new Error(errorData.error);
                } else {
                    throw new Error('Nie udało się zresetować hasła. Sprawdź token i spróbuj ponownie.');
                }
            } else {
                const errorText = await response.text();
                throw new Error(errorText || 'Nie udało się zresetować hasła. Sprawdź token i spróbuj ponownie.');
            }
        }
        
        const data = await response.json();
        const message = data.message || 'Hasło zostało zresetowane pomyślnie. Możesz się teraz zalogować.';
        
        showNotification(message, 'success');
        return true;
    } catch (error) {
        console.error('Błąd logowania:', error);
        showNotification(error.message, 'error');
        return false;
    }
}

export function handleLogout() {
    // Wyczyść stan i localStorage
    localStorage.removeItem('token');
    localStorage.removeItem('userId');
    setState({ token: null, userId: null });
    showNotification('Wylogowano pomyślnie!', 'success');
} 