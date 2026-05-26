# 📊 Sports Betting Manager

![Java](https://img.shields.io/badge/Java-21-orange?style=for-the-badge&logo=java)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.3-green?style=for-the-badge&logo=springboot)
![React](https://img.shields.io/badge/React-19-blue?style=for-the-badge&logo=react)
![Tailwind CSS](https://img.shields.io/badge/Tailwind_CSS-4.0-38B2AC?style=for-the-badge&logo=tailwind-css)
![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?style=for-the-badge&logo=mysql)
![Docker](https://img.shields.io/badge/Docker-Enabled-2496ED?style=for-the-badge&logo=docker)

A professional-grade analytics platform for sports traders. This full-stack application goes beyond simple tracking, offering **Bloomberg-style analytics**, performance metrics, live odds integration, and portfolio visualization. Built with performance and scalability in mind using **Java Spring Boot 3** and **React 19**.

---

## 🚀 Key Features

### 📡 Social Betting Intelligence & AI Extraction
- **Shadow Profile Tracking:** Track external sports traders' performance via X (formerly Twitter) shadow profiles without requiring them to register.
- **AI-Powered Bet Extraction:** Automatic extraction of **Single** and **Parlay (AKO)** bets from tweets and images using **Google Gemini Pro Vision** for advanced OCR.
- **Incremental Scraping Engine:** Efficient background sync using `since_id` pagination and image-presence heuristics to minimize API overhead and costs.
- **Historical Backfilling:** Automatically fetches and analyzes the last 100 posts for any newly tracked profile to provide immediate performance history.
- **Retroactive Analysis:** Intelligent detection of post-settlement bets with visual "Retroactive" badges and tooltips for transparency.

### 📈 Professional Analytics Dashboard
- **Live +EV Feed:** Expandable, deduplicated feed of currently available positive Expected Value opportunities filtered by user-defined profitability threshold (default >2%).
- **Equity Curve:** Interactive line chart visualizing cumulative profit over time (Lifetime Performance).
- **Profit by Sport:** Breakdown of net profit across different sports disciplines to identify strengths.
- **Key Metrics:** Real-time calculation of **ROI (Return on Investment)** and **Yield**.
- **Activity Heatmap:** Calendar view of trading activity and daily P/L.

### 💼 Portfolio Management
- **Bet Tracking:** Support for **Single** and **Parlay** (Accumulator) bets.
- **Auto-Calculation:** Automatic potential winnings and parlay odds aggregation.
- **Settlement Engine:** Streamlined workflow for settling bets (WON/LOST/VOID) with instant balance updates and safeguards against duplicate or invalid settlements.

### 📡 External Integrations & EV Engine
- **Automated EV Scanner:** Background engine running 24/7 that cross-references traditional bookmaker odds against predictive markets (**Polymarket**, **Kalshi**) to find mathematically profitable (+EV) bets.
- **Predictive Probability:** Integration with **Polymarket Gamma API** and **Kalshi** to calculate "True Probability" based on market liquidity and wisdom of the crowd.
- **Live Odds:** Real-time sports data and market odds powered by **TheOddsAPI** integration.


### 🔐 Security & Architecture
- **User Authentication:** Secure registration and login system based on **JWT**.
- **Password Recovery:** Comprehensive email-based password reset flow with anti-enumeration protection and Thymeleaf HTML templates.
- **API Documentation:** Interactive **Swagger UI (OpenAPI 3.0)** integration with Bearer Auth support.
- **Global Error Handling:** Unified JSON error responses driven by a centralized `GlobalExceptionHandler` and strict DTO validation rules.
- **Containerization & CI/CD:** Fully containerized environment using **Docker** and automated testing pipelines via **GitHub Actions**.

---

## 🛠️ Tech Stack

### Backend
- **Language:** Java 21
- **Framework:** Spring Boot 3.3.6
- **Database:** MySQL 8.0
- **AI/ML:** Google Gemini API (Vision Pro)
- **Data Providers:** SocialData (X API), TheOddsAPI, Polymarket, Kalshi
- **Security:** Spring Security + JWT
- **Templating:** Thymeleaf (Email templates)
- **Documentation:** Springdoc OpenAPI (Swagger)
- **CI/CD:** GitHub Actions
- **Containerization:** Docker, Docker Compose

### Frontend
- **Framework:** React 19
- **Build Tool:** Vite
- **Styling:** Tailwind CSS v4
- **Visualization:** Recharts
- **Web Server:** Nginx (in Docker)

---

## ⚙️ Getting Started (Docker - Recommended)

The easiest and recommended way to run the application is using Docker. This ensures a consistent environment matching the production setup.

### Prerequisites
- Docker and Docker Compose installed
- SSL Keystore (`sportsbetting.p12`) generated and placed in the project root.

### 1. Environment Setup
Create a `.env` file in the root directory based on the following template:

```env
# Database Credentials
DB_USERNAME=user
DB_PASSWORD=your_db_password
DB_ROOTPASSWORD=your_db_root_password

# Application Secrets
JWT_SECRET=YourSuperSecretKeyForJwtTokens
KEYSTORE_PASSWORD=your_p12_certificate_password

# External APIs
ODDS_API_KEY=your_the_odds_api_key
SOCIALDATA_API_KEY=your_socialdata_tools_key
GEMINI_API_KEY=your_google_gemini_key

# Email Configuration (Mailtrap - for testing password reset)
SMTP_MAILTRAP_USER=your_mailtrap_user
SMTP_MAILTRAP_PASSWORD=your_mailtrap_password
```

### 2. Build and Run
Run the following command to build the multi-stage images and start the containers (Database, Backend, Frontend):

```bash
docker compose up --build -d
```

### 3. Access the Application
- **Frontend App:** [http://localhost:3000](http://localhost:3000)
- **Backend API:** `https://localhost:8443`
- **Swagger UI (API Docs):** [https://localhost:8443/swagger-ui/index.html](https://localhost:8443/swagger-ui/index.html)

---

## 🛠️ Local Development (Without Docker)

If you prefer to run the application locally for active development:

### 1. Database
Ensure you have a local MySQL instance running with a database named `sportsbetting`.

### 2. Backend
Set the required environment variables (matching the `.env` file) in your IDE or terminal, and run:
```bash
mvn clean install
mvn spring-boot:run
```

### 3. Frontend
```bash
cd frontend
npm install
npm run dev
```
Local frontend will be available at `http://localhost:5173`.
