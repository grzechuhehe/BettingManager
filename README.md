# 📊 Sports Betting Manager

![Java](https://img.shields.io/badge/Java-21-orange?style=for-the-badge&logo=java)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.3-green?style=for-the-badge&logo=springboot)
![React](https://img.shields.io/badge/React-19-blue?style=for-the-badge&logo=react)
![Tailwind CSS](https://img.shields.io/badge/Tailwind_CSS-4.0-38B2AC?style=for-the-badge&logo=tailwind-css)
![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?style=for-the-badge&logo=mysql)
![Docker](https://img.shields.io/badge/Docker-Enabled-2496ED?style=for-the-badge&logo=docker)

A full-stack platform for sports bettors and traders: track your own portfolio, analyze performance with Bloomberg-style metrics, discover +EV opportunities, and monitor public picks from X (Twitter) profiles — with AI-assisted bet extraction and automated settlement against live match results.

Built with **Java 21**, **Spring Boot 3.3**, **React 19**, and **MySQL 8**.

---

## 🚀 Key Features

### 💼 Your Bets
- Add **single bets** and **accumulators (parlays)** with stake, odds, and bookmaker.
- See **total odds and potential winnings** calculated automatically.
- **Import a bet from a screenshot** — paste or upload a slip and let the app read the event, pick, and odds.
- **Attach a slip photo** as proof when entering a bet manually.
- **Settle bets** as won, lost, or void and see profit/loss update immediately.
- **Edit bet details** after placing without re-entering the whole coupon.

### 📡 Social Betting Radar
- **Search X (Twitter) profiles** from the app and see a quick preview before you follow someone.
- **Track public tipsters** and sync their new posts in the background.
- **Extract picks from tweets and images** — singles, parlays, and combined (bet builder) coupons.
- When you start tracking someone, the app can **load their recent history** so you are not starting from zero.
- Open a **public profile page** with win rate, yield, streaks, and full bet history — including slips with **proof images**.
- See when a pick was **posted retroactively** (already settled when it was imported).

### ⚖️ Automatic Settlement
- **Settle pending bets against real match results** without checking every leg yourself.
- Works for **singles, parlays, and multi-leg builders** — each leg is resolved on its own.
- Runs **in the background on a schedule**; you can also **trigger a run manually** when needed.
- Handles tricky cases: **void legs** in a parlay, tennis doubles, national teams, player props after the match.

### 📈 Analytics & Edge
- **Dashboard** with ROI, yield, net profit, and open positions.
- **Equity curve** — how your bankroll evolved over time.
- **Profit breakdown by sport** — see where you perform best.
- **Advanced stats** — win rate, streaks, Sharpe ratio, rolling performance.
- **Activity heatmap** — which days you won or lost.
- **Live +EV feed** — spots where bookmaker odds look mispriced vs. prediction markets; filter by your minimum edge.
- **EV calculator** — check expected value for a pick before you place it.

### 🔐 Account
- **Register and log in** securely.
- **Reset password** via email if you forget it.
- Adjust **+EV alert threshold** to match how selective you want to be.

---

## 🗺️ Application Screens

| Route | Description |
|-------|-------------|
| `/dashboard` | Main analytics hub, +EV feed, quick actions |
| `/add-bet` | Manual bet form + AI screenshot import |
| `/bets` | Full bet list with settlement controls |
| `/live-odds` | Browse live markets from TheOddsAPI |
| `/social` | Social Betting Radar — search & track X profiles |
| `/profile/:username` | Public betting profile & statistics |
| `/profile` | Account settings (email, password reset, snapshot stats) |
| `/ev-calculator` | Standalone EV calculator |

---

## 🛠️ Tech Stack

### Backend
- Java 21, Spring Boot 3.3.6, Spring Data JPA, Spring Security (JWT)
- MySQL 8.0
- Google Gemini API, SocialData (X), TheOddsAPI, Apify SofaScore actor
- Thymeleaf (email templates), Springdoc OpenAPI

### Frontend
- React 19, Vite, Tailwind CSS v4, Recharts, Axios
- Nginx (production Docker image)

### Infrastructure
- Docker & Docker Compose
- GitHub Actions CI

---

## ⚙️ Getting Started (Docker — Recommended)

### Prerequisites
- Docker and Docker Compose
- SSL keystore `sportsbetting.p12` in the project root

### 1. Environment setup

Create `.env` in the project root (never commit it):

```env
# Database
DB_USERNAME=user
DB_PASSWORD=your_db_password
DB_ROOTPASSWORD=your_db_root_password

# Application secrets
JWT_SECRET=YourSuperSecretKeyForJwtTokens
KEYSTORE_PASSWORD=your_p12_certificate_password

# External APIs
ODDS_API_KEY=your_the_odds_api_key
SOCIALDATA_API_KEY=your_socialdata_tools_key
GEMINI_API_KEY=your_google_gemini_key
APIFY_API_TOKEN=your_apify_token

# Email (Mailtrap for local dev)
MAILTRAP_SMTP_USERNAME=your_mailtrap_user
MAILTRAP_SMTP_PASSWORD=your_mailtrap_password

# Optional
GEMINI_SYSTEM_PROMPT=
FRONTEND_URL=http://localhost:3000
```

### 2. Build and run

```bash
docker compose up --build -d
```

### 3. Access

| Service | URL |
|---------|-----|
| Frontend | http://localhost:3000 |
| Backend API | https://localhost:8443 |
| Swagger UI | https://localhost:8443/swagger-ui/index.html |

Bet slip proof images are served from `uploads/profiles/` (mounted in the backend container).

---

## 🛠️ Local Development (Without Docker)

### Prerequisites
- **JDK 21** (Maven enforcer rejects other versions)
- Maven 3.8+
- MySQL 8 with database `sportsbetting`
- Node.js 18+ for the frontend
- `sportsbetting.p12` in the project root

### Backend

Set environment variables (same names as `.env` above), then:

```bash
mvn clean install
mvn spring-boot:run
```

### Frontend

```bash
cd frontend
npm install
npm run dev
```

Frontend dev server: http://localhost:5173 (CORS configured for this origin).

---

## 🧪 Tests

```bash
mvn test
```

CI runs the full suite (~200 tests) on push/PR to `main` with Java 21 and a MySQL service container.

---

## 📁 Notable API Endpoints

| Area | Examples |
|------|----------|
| Auth | `POST /api/auth/signin`, `/signup`, `/forgot-password` |
| Bets | `POST /api/bets/add-bet`, `/add-bet-with-proof`, `GET /api/bets/dashboard-stats` |
| Import | `POST /api/bets/import-from-image` |
| Resolution | `POST /api/bets/run-auto-resolution` |
| Social | `GET /api/profiles/preview`, `POST /api/profiles/track`, `GET /api/profiles/{user}/picks` |
| EV | `GET /api/ev/opportunities`, `POST /api/ev/calculate` |
| Odds | `GET /api/odds/sports`, `/markets/{sportKey}` |
| User | `GET /api/user/profile`, `POST /api/user/settings` |

Full interactive documentation: **Swagger UI**.
