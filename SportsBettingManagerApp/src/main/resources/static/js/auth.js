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
        
        if (!response.ok) {
            throw new Error('Błąd logowania. Sprawdź dane i spróbuj ponownie.');
        }
        
        const data = await response.json();
        setState({ token: data.token });
        
        // Zapisz token w localStorage
        localStorage.setItem('token', data.token);
        
        // Dekoduj token JWT aby uzyskać ID użytkownika
        const payload = parseJwt(data.token);
        setState({ userId: payload.sub });
        localStorage.setItem('userId', payload.sub);
        
        showNotification('Zalogowano pomyślnie!', 'success');
        return true;
    } catch (error) {
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
        
        if (!response.ok) {
            const errorData = await response.json();
            throw new Error(Array.isArray(errorData) ? errorData.join(', ') : errorData);
        }
        
        showNotification('Konto zostało utworzone. Możesz się teraz zalogować.', 'success');
        return true;
    } catch (error) {
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
        
        if (!response.ok) {
            throw new Error('Nie udało się zresetować hasła. Sprawdź token i spróbuj ponownie.');
        }
        
        showNotification('Hasło zostało zresetowane pomyślnie. Możesz się teraz zalogować.', 'success');
        return true;
    } catch (error) {
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