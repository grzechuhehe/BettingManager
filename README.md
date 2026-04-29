# 📊 Sports Betting Manager

![Java](https://img.shields.io/badge/Java-21-orange?style=for-the-badge&logo=java)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.3-green?style=for-the-badge&logo=springboot)
![React](https://img.shields.io/badge/React-19-blue?style=for-the-badge&logo=react)
![Tailwind CSS](https://img.shields.io/badge/Tailwind_CSS-4.0-38B2AC?style=for-the-badge&logo=tailwind-css)
![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?style=for-the-badge&logo=mysql)
![Docker](https://img.shields.io/badge/Docker-Enabled-2496ED?style=for-the-badge&logo=docker)

A professional-grade analytics platform for sports traders. This full-stack application goes beyond simple tracking, offering **Bloomberg-style analytics**, performance metrics, and portfolio visualization. Built with performance and scalability in mind using **Java Spring Boot 3** and **React 19**.

---

## 🚀 Key Features

### 📈 Professional Analytics Dashboard
- **Equity Curve:** Interactive line chart visualizing cumulative profit over time (Lifetime Performance).
- **Profit by Sport:** Breakdown of net profit across different sports disciplines to identify strengths.
- **Key Metrics:** Real-time calculation of **ROI (Return on Investment)** and **Yield**.
- **Activity Heatmap:** Calendar view of trading activity and daily P/L.

### 💼 Portfolio Management
- **Bet Tracking:** Support for **Single** and **Parlay** (Accumulator) bets.
- **Auto-Calculation:** Automatic potential winnings and parlay odds aggregation.
- **Settlement Engine:** Streamlined workflow for settling bets (WON/LOST/VOID) with instant balance updates.

### 🔐 Security & Architecture
- **User Authentication:** Secure registration and login system based on **JWT**.
- **API Documentation:** Interactive **Swagger UI (OpenAPI 3.0)** integration with Bearer Auth support.
- **Error Handling:** Unified, professional JSON error response format across the entire API.
- **Containerization:** Fully containerized environment using **Docker** and multi-stage builds.

---

## 🛠️ Tech Stack

### Backend
- **Language:** Java 21
- **Framework:** Spring Boot 3.3.6
- **Database:** MySQL 8.0
- **Security:** Spring Security + JWT
- **Documentation:** Springdoc OpenAPI (Swagger)
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
DB_USERNAME=user
DB_PASSWORD=your_db_password
DB_ROOTPASSWORD=your_db_root_password
JWT_SECRET=YourSuperSecretKeyForJwtTokens
KEYSTORE_PASSWORD=your_p12_certificate_password
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
