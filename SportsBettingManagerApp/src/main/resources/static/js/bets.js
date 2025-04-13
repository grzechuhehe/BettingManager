import { API_URL, API_ENDPOINTS } from './config.js';
import { getState } from './state.js';
import { showNotification, formatStatus, formatBetType } from './utils.js';

export async function handleAddBet(e) {
    e.preventDefault();
    
    // Tworzymy obiekt wydarzenia sportowego
    const sportEvent = {
        teamHome: document.getElementById('event-team-home').value,
        teamAway: document.getElementById('event-team-away').value,
        date: document.getElementById('event-date').value,
        sportType: document.getElementById('event-sport-type').value
    };
    
    // Sprawdź, czy token i userId są dostępne
    const userId = getState().userId;
    const token = getState().token;
    
    if (!userId || !token) {
        showNotification('Sesja wygasła. Zaloguj się ponownie.', 'error');
        return false;
    }
    
    console.log('Aktualne userId przed utworzeniem zakładu:', userId);
    console.log('Aktualny token:', token.substring(0, 10) + '...');
    
    // Sprawdź poprawność ID użytkownika
    let userIdNumber;
    try {
        userIdNumber = parseInt(userId);
        if (isNaN(userIdNumber) || userIdNumber <= 0) {
            throw new Error('Nieprawidłowy identyfikator użytkownika');
        }
    } catch (error) {
        console.error('Błąd parsowania ID użytkownika:', error);
        showNotification('Błąd identyfikatora użytkownika. Zaloguj się ponownie.', 'error');
        return false;
    }
    
    // Tworzymy obiekt zakładu z poprawnie skonwertowanymi wartościami liczbowymi
    const bet = {
        amount: parseFloat(document.getElementById('bet-amount').value),
        odds: parseFloat(document.getElementById('bet-odds').value),
        type: document.getElementById('bet-type').value,
        status: document.getElementById('bet-status').value,
        event: sportEvent,
        user: { id: userIdNumber }
    };
    
    try {
        // Upewnij się, że data jest w formacie ISO string
        if (bet.event && bet.event.date) {
            console.log('Data przed konwersją:', bet.event.date);
            // Konwertuj datę na format ISO string jeśli to nie jest już string
            if (!(typeof bet.event.date === 'string')) {
                bet.event.date = new Date(bet.event.date).toISOString();
            }
            console.log('Data po konwersji:', bet.event.date);
        }
        
        console.log('Wysyłam zakład do API:', JSON.stringify(bet));
        console.log('Token używany do autoryzacji:', getState().token);
        
        const response = await fetch(`${API_URL}${API_ENDPOINTS.bets.create}`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${getState().token}`
            },
            body: JSON.stringify(bet)
        });
        
        if (!response.ok) {
            try {
                const errorData = await response.json();
                throw new Error(Array.isArray(errorData) ? errorData.map(err => `${err.field}: ${err.defaultMessage}`).join(', ') : 'Nie udało się dodać zakładu.');
            } catch (jsonError) {
                // Handle case when response is not valid JSON
                if (response.status === 401) {
                    throw new Error('Sesja wygasła. Zaloguj się ponownie.');
                } else {
                    throw new Error(`Nie udało się dodać zakładu: ${response.status} ${response.statusText}`);
                }
            }
        }
        
        // Resetuj formularz
        document.getElementById('add-bet-form').reset();
        
        showNotification('Zakład został dodany pomyślnie!', 'success');
        return true;
    } catch (error) {
        showNotification(error.message, 'error');
        return false;
    }
}

export async function loadDashboardData() {
    if (!getState().token || !getState().userId) return;
    
    try {
        // Pobierz statystyki użytkownika
        const statsResponse = await fetch(`${API_URL}${API_ENDPOINTS.bets.getStats}?userId=${getState().userId}`, {
            headers: {
                'Authorization': `Bearer ${getState().token}`
            }
        });
        
        if (!statsResponse.ok) {
            if (statsResponse.status === 401) {
                throw new Error('Sesja wygasła. Zaloguj się ponownie.');
            }
            throw new Error('Nie udało się pobrać danych statystycznych.');
        }
        
        const statsData = await statsResponse.json();
        
        // Aktualizuj dane na dashboardzie
        document.getElementById('total-amount').textContent = `${statsData.totalAmount.toFixed(2)} zł`;
        document.getElementById('profit-loss').textContent = `${statsData.profitLoss.toFixed(2)} zł`;
        document.getElementById('roi').textContent = `${(statsData.roi * 100).toFixed(2)}%`;
        
        // Pobierz ostatnie zakłady
        const recentBets = statsData.recentBets || [];
        renderRecentBets(recentBets);
        
    } catch (error) {
        showNotification(error.message, 'error');
    }
}

export async function loadStatsData() {
    if (!getState().token || !getState().userId) return;
    
    try {
        const response = await fetch(`${API_URL}${API_ENDPOINTS.bets.getAdvancedStats}/${getState().userId}`, {
            headers: {
                'Authorization': `Bearer ${getState().token}`
            }
        });
        
        if (!response.ok) {
            if (response.status === 401) {
                throw new Error('Sesja wygasła. Zaloguj się ponownie.');
            }
            throw new Error('Nie udało się pobrać danych statystycznych.');
        }
        
        const statsData = await response.json();
        
        // Aktualizuj wartości w widoku statystyk
        document.getElementById('total-bets').textContent = statsData.totalBets;
        document.getElementById('successful-bets').textContent = statsData.successfulBets;
        document.getElementById('stats-profit-loss').textContent = `${statsData.profitLoss.toFixed(2)} zł`;
        document.getElementById('stats-roi').textContent = `${statsData.roiPercentage.toFixed(2)}%`;
        document.getElementById('rolling-average').textContent = `${statsData.rollingAverage30d.toFixed(2)} zł`;
        document.getElementById('current-streak').textContent = statsData.currentStreak;
        
    } catch (error) {
        showNotification(error.message, 'error');
    }
}

export async function loadAdvancedStatsData() {
    if (!getState().token || !getState().userId) return;
    
    try {
        // Pobierz zaawansowane statystyki
        const statsResponse = await fetch(`${API_URL}${API_ENDPOINTS.bets.getAdvancedStats}/${getState().userId}`, {
            headers: {
                'Authorization': `Bearer ${getState().token}`
            }
        });
        
        if (!statsResponse.ok) {
            if (statsResponse.status === 401) {
                throw new Error('Sesja wygasła. Zaloguj się ponownie.');
            }
            throw new Error('Nie udało się pobrać zaawansowanych statystyk.');
        }
        
        const statsData = await statsResponse.json();
        
        // Aktualizuj wartość wskaźnika Sharpe'a
        document.getElementById('sharpe-ratio').textContent = statsData.sharpeRatio.toFixed(2);
        
        // Renderuj wykres współczynników wygranych wg typu zakładu
        renderWinRatesChart(statsData.winRatesByType);
        
        // Pobierz dane do heatmapy
        const heatmapResponse = await fetch(`${API_URL}${API_ENDPOINTS.bets.getHeatmapData}/${getState().userId}`, {
            headers: {
                'Authorization': `Bearer ${getState().token}`
            }
        });
        
        if (!heatmapResponse.ok) {
            if (heatmapResponse.status === 401) {
                throw new Error('Sesja wygasła. Zaloguj się ponownie.');
            }
            throw new Error('Nie udało się pobrać danych heatmapy.');
        }
        
        const heatmapData = await heatmapResponse.json();
        
        // Renderuj heatmapę
        renderHeatmap(heatmapData);
        
    } catch (error) {
        showNotification(error.message, 'error');
    }
}

// Funkcje renderujące
function renderRecentBets(bets) {
    const tbody = document.getElementById('recent-bets-body');
    tbody.innerHTML = '';
    
    if (!bets || bets.length === 0) {
        const row = document.createElement('tr');
        row.innerHTML = '<td colspan="6" style="text-align: center;">Brak zakładów do wyświetlenia</td>';
        tbody.appendChild(row);
        return;
    }
    
    bets.forEach(bet => {
        const row = document.createElement('tr');
        
        // Formatuj datę
        const date = new Date(bet.placedAt);
        const formattedDate = `${date.toLocaleDateString()} ${date.toLocaleTimeString()}`;
        
        // Formatuj wydarzenie
        const event = bet.event ? `${bet.event.teamHome} vs ${bet.event.teamAway}` : 'Brak danych';
        
        // Formatuj status
        let statusClass = '';
        switch (bet.status) {
            case 'PENDING':
                statusClass = 'bet-status-pending';
                break;
            case 'WON':
                statusClass = 'bet-status-won';
                break;
            case 'LOST':
                statusClass = 'bet-status-lost';
                break;
        }
        
        row.innerHTML = `
            <td>${formattedDate}</td>
            <td>${event}</td>
            <td>${formatBetType(bet.type)}</td>
            <td>${bet.amount.toFixed(2)} zł</td>
            <td>${bet.odds.toFixed(2)}</td>
            <td class="${statusClass}">${formatStatus(bet.status)}</td>
        `;
        
        tbody.appendChild(row);
    });
}

function renderWinRatesChart(winRatesByType) {
    const container = document.getElementById('win-rates-by-type');
    container.innerHTML = '';
    
    if (!winRatesByType || Object.keys(winRatesByType).length === 0) {
        container.textContent = 'Brak danych do wyświetlenia';
        return;
    }
    
    const chartContainer = document.createElement('div');
    chartContainer.style.display = 'flex';
    chartContainer.style.height = '200px';
    chartContainer.style.alignItems = 'flex-end';
    chartContainer.style.gap = '20px';
    chartContainer.style.padding = '10px 0';
    
    Object.entries(winRatesByType).forEach(([type, rate]) => {
        const barContainer = document.createElement('div');
        barContainer.style.display = 'flex';
        barContainer.style.flexDirection = 'column';
        barContainer.style.alignItems = 'center';
        barContainer.style.flex = '1';
        
        const percentage = (rate * 100).toFixed(0);
        
        const bar = document.createElement('div');
        bar.style.width = '100%';
        bar.style.height = `${percentage}%`;
        bar.style.backgroundColor = '#3498db';
        bar.style.marginBottom = '10px';
        bar.style.borderRadius = '4px';
        bar.style.minHeight = '10px';
        
        const label = document.createElement('div');
        label.textContent = formatBetType(type);
        label.style.fontSize = '12px';
        label.style.fontWeight = 'bold';
        
        const value = document.createElement('div');
        value.textContent = `${percentage}%`;
        value.style.fontSize = '14px';
        
        barContainer.appendChild(bar);
        barContainer.appendChild(label);
        barContainer.appendChild(value);
        
        chartContainer.appendChild(barContainer);
    });
    
    container.appendChild(chartContainer);
}

function renderHeatmap(heatmapData) {
    const container = document.getElementById('heatmap-container');
    container.innerHTML = '';
    
    if (!heatmapData || Object.keys(heatmapData).length === 0) {
        container.textContent = 'Brak danych do wyświetlenia';
        return;
    }
    
    const table = document.createElement('table');
    table.className = 'heatmap-table';
    table.style.width = '100%';
    table.style.borderCollapse = 'collapse';
    
    // Nagłówek tabeli - dni tygodnia
    const header = document.createElement('thead');
    const headerRow = document.createElement('tr');
    
    // Pusty narożnik
    headerRow.appendChild(document.createElement('th'));
    
    // Dodaj nagłówki dla dni tygodnia
    const daysOfWeek = ['Poniedziałek', 'Wtorek', 'Środa', 'Czwartek', 'Piątek', 'Sobota', 'Niedziela'];
    daysOfWeek.forEach(day => {
        const th = document.createElement('th');
        th.textContent = day;
        th.style.padding = '8px';
        headerRow.appendChild(th);
    });
    
    header.appendChild(headerRow);
    table.appendChild(header);
    
    // Zawartość tabeli - godziny i komórki heatmapy
    const tbody = document.createElement('tbody');
    
    // Dodaj wiersze dla godzin (zakładamy dane co godzinę)
    for (let hour = 0; hour < 24; hour++) {
        const row = document.createElement('tr');
        
        // Etykieta godziny
        const hourLabel = document.createElement('td');
        hourLabel.textContent = `${hour}:00`;
        hourLabel.style.fontWeight = 'bold';
        hourLabel.style.padding = '8px';
        row.appendChild(hourLabel);
        
        // Dodaj komórki dla dni tygodnia
        for (let day = 0; day < 7; day++) {
            const cell = document.createElement('td');
            
            // Pobierz wartość z danych heatmapy (jeśli istnieje)
            const dayKey = daysOfWeek[day];
            const hourKey = `${hour}`;
            const value = heatmapData[dayKey] && heatmapData[dayKey][hourKey] ? heatmapData[dayKey][hourKey] : 0;
            
            // Oblicz kolor tła na podstawie wartości
            const intensity = Math.min(value * 100, 100); // Wartość procentowa intensywności
            cell.style.backgroundColor = `rgba(52, 152, 219, ${intensity / 100})`;
            cell.style.textAlign = 'center';
            cell.style.padding = '8px';
            
            // Pokaż wartość jako tekst tylko jeśli jest większa od zera
            if (value > 0) {
                cell.textContent = value.toFixed(2);
            }
            
            row.appendChild(cell);
        }
        
        tbody.appendChild(row);
    }
    
    table.appendChild(tbody);
    container.appendChild(table);
} 