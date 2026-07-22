# Charity Management System

A full-featured charity management platform with backend API and frontend SPA for managing charitable organizations, cases, and donations.

## Architecture

```
charity/
├── backend/          Spring Boot 3 REST API (Java 21)
└── frontend/         Vue 3 SPA (Vite + TypeScript)
```

## Tech Stack

### Backend
- **Java 21** + **Spring Boot 3.2**
- **Spring Data JPA** + Hibernate
- **Spring Security** + JWT authentication
- **PostgreSQL**
- **Maven**

### Frontend
- **Vue 3** (Composition API + `<script setup>`)
- **Vite** build tool
- **TypeScript**
- **Pinia** state management
- **Vue Router**
- **Axios** HTTP client

## Features

- User registration & login (JWT-based)
- Center (charity organization) management
- Charity case management with image uploads
- Category, Province, City browsing
- Public listing pages with pagination & search
- Admin panel for content moderation
- Notice/Banner management
- File storage for uploaded assets

## Getting Started

### Prerequisites

- Java 21+
- Node.js 18+
- PostgreSQL
- Maven

### Backend Setup

```bash
cd backend
cp src/main/resources/application.yml.example src/main/resources/application.yml
# Edit application.yml with your database credentials
mvn clean install
mvn spring-boot:run
```

The API server starts at `http://localhost:8080`.

### Frontend Setup

```bash
cd frontend
npm install
npm run dev
```

The development server starts at `http://localhost:5173`.

### Authentication

The API uses JWT tokens. Public endpoints do not require authentication. Admin endpoints require a valid JWT token sent as `Authorization: Bearer <token>`.

## Project Structure

```
backend/
├── src/main/java/com/charity/app/
│   ├── config/           Security & app configuration
│   ├── controller/       REST controllers
│   ├── model/            JPA entities
│   ├── payload/          Request/Response DTOs
│   ├── repository/       Spring Data repositories
│   ├── security/         JWT & auth utilities
│   └── service/          Business logic layer
└── src/main/resources/
    └── application.yml   Application config

frontend/
├── src/
│   ├── api/              API client modules
│   ├── assets/           Static assets & styles
│   ├── components/       Reusable Vue components
│   ├── layouts/          Page layout components
│   ├── router/           Vue Router configuration
│   ├── stores/           Pinia stores
│   ├── utils/            Utility functions
│   └── views/            Page components
└── public/               Public static assets
```

## API Endpoints

| Method | Path                    | Description              |
|--------|-------------------------|--------------------------|
| GET    | /api/public/centers     | List approved centers    |
| GET    | /api/public/centers/{id}| Center profile detail    |
| GET    | /api/public/cases       | List charity cases       |
| GET    | /api/public/cases/{id}  | Case detail              |
| GET    | /api/public/categories  | Active categories        |
| GET    | /api/public/provinces   | Provinces                |
| GET    | /api/public/cities      | Cities by province       |
| POST   | /api/auth/register      | Register new user        |
| POST   | /api/auth/login         | Login                    |
| ...    | /api/admin/*            | Admin endpoints          |

## Branches

- `master` — Production-ready code
- `development` — Active development

## License

MIT
