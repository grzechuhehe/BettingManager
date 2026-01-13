# 📊 Sports Betting Manager

![Java](https://img.shields.io/badge/Java-21-orange?style=for-the-badge&logo=java)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.3-green?style=for-the-badge&logo=springboot)
![React](https://img.shields.io/badge/React-19-blue?style=for-the-badge&logo=react)
![Tailwind CSS](https://img.shields.io/badge/Tailwind_CSS-4.0-38B2AC?style=for-the-badge&logo=tailwind-css)
![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?style=for-the-badge&logo=mysql)

A full-stack web application designed to track, manage, and analyze sports betting activities. Built with performance and scalability in mind, using the latest **Java Spring Boot** ecosystem for the backend and **React (Vite)** for a modern, responsive frontend.

---

## 🚀 Features

- **User Authentication:** Secure registration and login system based on **JWT (JSON Web Tokens)**.
- **Dashboard & Analytics:** Real-time overview of performance including:
    - Total Profit/Loss
    - Win Rate (%)
    - Active Bets count
    - Heatmaps & Advanced Charts (Soon)
- **Bet Management (CRUD):**
    - Place **Single** and **Parlay** (Accumulator) bets.
    - Automatic odds calculation for Parlays.
    - Edit and Delete existing bets.
- **Settlement System:** easy status updates for bets (WON, LOST, VOID) with automatic profit calculation.
- **Responsive UI:** Modern interface built with **Tailwind CSS**, fully responsive for desktop and mobile.

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
- **HTTP Client:** Axios
- **Routing:** React Router DOM

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

---

