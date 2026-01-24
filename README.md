# 📊 Sports Betting Manager

![Java](https://img.shields.io/badge/Java-21-orange?style=for-the-badge&logo=java)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.3-green?style=for-the-badge&logo=springboot)
![React](https://img.shields.io/badge/React-19-blue?style=for-the-badge&logo=react)
![Tailwind CSS](https://img.shields.io/badge/Tailwind_CSS-4.0-38B2AC?style=for-the-badge&logo=tailwind-css)
![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?style=for-the-badge&logo=mysql)

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
- **Responsive UI:** Modern interface built with **Tailwind CSS v4**, fully responsive for desktop and mobile.

---

## 🛠️ Tech Stack

### Backend
- **Language:** Java 21
- **Framework:** Spring Boot 3.3.6
- **Database:** MySQL
- **ORM:** Spring Data JPA (Hibernate)
- **Security:** Spring Security + JWT
- **Build Tool:** Maven
- **Testing:** JUnit 5, Mockito

### Frontend
- **Framework:** React 19
- **Build Tool:** Vite
- **Styling:** Tailwind CSS v4
- **Visualization:** **Recharts**
- **HTTP Client:** Axios
- **Routing:** React Router DOM v7

---

## ⚙️ Getting Started

Follow these steps to set up the project locally.

### Prerequisites
- Java JDK 21
- Node.js & npm
- MySQL Server
- Maven

### 1. Database Setup
Create a MySQL database named `sports_betting_db`:
```sql
CREATE DATABASE sports_betting_db;
```

### 2. Backend Setup
1. Rename `src/main/resources/application.properties.example` to `application.properties`.
2. Update the database credentials and JWT secret.
3. Run the Spring Boot application:

```bash
mvn clean install
mvn spring-boot:run
```

### 3. Frontend Setup
Navigate to the `frontend` folder:

```bash
cd frontend
npm install
npm run dev
```
The frontend will run on `http://localhost:5173`.