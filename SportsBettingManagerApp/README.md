# Menedżer Zakładów Sportowych - Interfejs Użytkownika

Interfejs użytkownika dla aplikacji Menedżer Zakładów Sportowych, umożliwiający zarządzanie zakładami sportowymi, śledzenie statystyk i analizę wyników.

## Funkcjonalności

- **Autoryzacja**
  - Logowanie użytkowników
  - Rejestracja nowych użytkowników
  - Wylogowanie

- **Zarządzanie zakładami**
  - Dodawanie nowych zakładów sportowych
  - Przeglądanie historii zakładów
  - Aktualizacja statusu zakładów

- **Statystyki**
  - Podstawowe statystyki na dashboardzie
  - Szczegółowe statystyki z wykresami
  - Zaawansowane statystyki z heatmapą aktywności

## Technologie

- HTML5
- CSS3
- JavaScript (ES6+)
- Moduły ES6

## Wymagania

- Działający backend Spring Boot na porcie 8443 z obsługą HTTPS
- Nowoczesna przeglądarka internetowa z obsługą modułów ES6

## Konfiguracja

1. Upewnij się, że backend jest uruchomiony na `https://localhost:8443`
2. Jeśli backend jest uruchomiony na innym porcie, zaktualizuj stałą `API_URL` w pliku `js/config.js`

## Uruchomienie

### Metoda 1: Bezpośrednio z przeglądarki

1. Otwórz plik `index.html` w przeglądarce internetowej
2. Zaloguj się lub zarejestruj nowe konto
3. Rozpocznij korzystanie z aplikacji

### Metoda 2: Używając lokalnego serwera (zalecane)

Aby uniknąć problemów z CORS, zaleca się uruchomienie aplikacji przez lokalny serwer HTTP.

#### Używając Node.js:

1. Zainstaluj `http-server` globalnie:
```bash
npm install -g http-server
```

2. Uruchom serwer w katalogu projektu:
```bash
http-server
```

3. Otwórz przeglądarkę i przejdź do `https://localhost:8080` (pamiętaj, że backend będzie nadal działał na `https://localhost:8443`)

#### Używając Pythona:

1. Uruchom serwer HTTP w katalogu projektu:
```bash
# Python 3
python -m http.server 8000

# Python 2
python -m SimpleHTTPServer 8000
```

2. Otwórz przeglądarkę i przejdź do `http://localhost:8000`

## Bezpieczeństwo

- Token JWT jest przechowywany w localStorage
- Wszystkie żądania do API są zabezpieczone tokenem JWT
- Dla aplikacji produkcyjnych zaleca się:
  - Użycie HTTPS
  - Implementację dodatkowych mechanizmów bezpieczeństwa
  - Bezpieczne przechowywanie tokenu JWT

## Rozwój

1. Modyfikuj pliki HTML, CSS i JavaScript według potrzeb
2. Testuj zmiany w przeglądarce
3. Upewnij się, że backend jest uruchomiony i dostępny
4. Sprawdź konsolę przeglądarki pod kątem błędów

## Struktura projektu

```
├── index.html          # Główny plik HTML
├── styles.css          # Style CSS
├── script.js           # Główny plik JavaScript
├── js/
│   ├── config.js       # Konfiguracja API
│   ├── state.js        # Zarządzanie stanem aplikacji
│   ├── utils.js        # Funkcje pomocnicze
│   ├── auth.js         # Logika autoryzacji
│   ├── bets.js         # Logika zakładów
│   └── ui.js           # Logika interfejsu użytkownika
└── README.md           # Dokumentacja
```

## Licencja

MIT 