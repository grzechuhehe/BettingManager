// Funkcje pomocnicze
export function showNotification(message, type = 'success') {
    const notification = document.getElementById('notification');
    notification.textContent = message;
    notification.className = `notification ${type} visible`;
    
    setTimeout(() => {
        notification.classList.remove('visible');
    }, 5000);
}

export function parseJwt(token) {
    try {
        return JSON.parse(atob(token.split('.')[1]));
    } catch (e) {
        return null;
    }
}

export function formatStatus(status) {
    switch (status) {
        case 'PENDING':
            return 'Oczekujący';
        case 'WON':
            return 'Wygrany';
        case 'LOST':
            return 'Przegrany';
        default:
            return status;
    }
}

export function formatBetType(type) {
    switch (type) {
        case 'WIN':
            return 'Wygrana';
        case 'DRAW':
            return 'Remis';
        case 'OVER':
            return 'Powyżej';
        case 'UNDER':
            return 'Poniżej';
        default:
            return type;
    }
} 