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
- **MySQL** (prod) / **H2** (local dev)
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
- Maven
- Docker (optional, for the containerized setup below)

### Backend Setup

```bash
cd backend
# Local dev uses H2 in-memory (no MySQL needed):
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

The API server starts at `http://localhost:8082` (default) / `8085` (local profile). The API is also exposed via docker as `81` (master) and `8081` (dev).

### Frontend Setup

```bash
cd frontend
npm install
npm run dev
```

The development server starts at `http://localhost:5173`.

### Docker (per-branch images)

The docker setup runs **two branches at the same time** — `master` on port 80 and `development` on port 8080 — each with its own prebuilt image. `compose.yaml` is image-based, so images are built per branch with a `git worktree` script:

```bash
./docker-build.sh all           # or: ./docker-build.sh dev|master
docker compose up -d
```

| Service         | Branch       | Port | DB |
|-----------------|--------------|------|----|
| frontend-master | master       | 80   | —  |
| frontend-dev    | development  | 8080 | —  |
| backend-master  | master       | 81   | MySQL |
| backend-dev     | development  | 8081 | H2  |
| mysql           | —            | 3307 | MySQL |

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
