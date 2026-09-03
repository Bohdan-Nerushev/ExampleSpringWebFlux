# WebFlux Reactive Order Service

Reactive order management microservice built on **Spring Boot 3.x (WebFlux)**, **Spring Data R2DBC**, **PostgreSQL**, **Flyway**, **WebClient**, **Resilience4j**, **BlockHound**, and **OpenAPI / Swagger**.

---

## 🚀 Key Features & Architecture

- **Fully Non-Blocking I/O Stack (Reactive WebFlux & Netty)**: Zero `.block()` calls, JDBC, or blocking threads in production code.
- **Reactive Database Access (R2DBC)**: Utilizes `r2dbc-postgresql` for asynchronous interactions with PostgreSQL.
- **Automated Database Migrations (Flyway)**: Manages database schema versioning.
- **External Integration via WebClient**: Asynchronous discount retrieval and notification dispatch guarded by `timeout` and `retryWhen` (backoff).
- **Parallel Data Aggregation (`Mono.zip`)**: Executes independent queries concurrently to calculate aggregate order analytics.
- **Non-Blocking Environment Audit (BlockHound)**: Automatically detects accidental blocking calls (`Thread.sleep`, I/O) at runtime and during testing.
- **API Documentation (Swagger UI / OpenAPI 3.0)**: Interactive specification for REST and SSE endpoints.

---

## 📋 REST API Specification

| Method | Path | Description | Response Type |
| --- | --- | --- | --- |
| `POST` | `/api/v1/orders` | Create a new order | `201 Created` (`OrderResponse`) |
| `GET` | `/api/v1/orders/{id}` | Retrieve order details by ID | `200 OK` (`OrderResponse`) / `404 Not Found` |
| `GET` | `/api/v1/orders` | Retrieve list of all orders | `200 OK` (`Flux<OrderResponse>`) |
| `GET` | `/api/v1/orders/analytics` | Aggregated order analytics (`Mono.zip`) | `200 OK` (`OrderAnalyticsResponse`) |
| `GET` | `/api/v1/orders/stream` | Server-Sent Events (SSE) order status stream | `200 OK` (`text/event-stream`) |

---

## ⚙️ Environment Configuration (.env)

Before running the application, copy `.env.example` to `.env` and adjust the variables if needed:

```dotenv
POSTGRES_DB=orderdb
POSTGRES_USER=postgres
POSTGRES_PASSWORD=postgres
POSTGRES_PORT=5432
WIREMOCK_PORT=8081
APP_PORT=8080
```

---

## 🐳 Running with Docker Compose

To launch the full system (PostgreSQL, WireMock, and Order Service) in Docker containers, run:

```bash
docker compose up -d --build
```

- **Swagger UI**: Available at `http://localhost:8080/swagger-ui.html`
- **OpenAPI JSON**: `http://localhost:8080/v3/api-docs`
- **WireMock Admin**: `http://localhost:8081/__admin`

---

## 🛠️ Running Locally (Development)

### 1. Start Infrastructure (PostgreSQL & WireMock):

```bash
docker compose up -d postgres wiremock
```

### 2. Launch Spring Boot Application:

```bash
mvn spring-boot:run
```

---

## 🧪 Testing

Execute the complete test suite (Unit tests, BlockHound, WireMock, and Testcontainers integration tests):

```bash
mvn clean test
```

### Test Suite Overview:
- **`OrderServiceTest`**: Reactive unit testing of business logic using `StepVerifier` and Mockito.
- **`OrderControllerTest`**: Web layer integration tests for REST endpoints and JSON serialization via `WebTestClient`.
- **`ExternalDiscountClientTest`**: WebClient integration tests with WireMock / MockWebServer (verifying 200, 404, 500 status handling).
- **`BlockHoundTest`**: Detects accidental blocking operations in Event Loop threads.
- **`OrderRepositoryIT`**: Spring Data R2DBC integration tests executing real SQL against a PostgreSQL `Testcontainers` instance.

---

## 📂 Project Structure

```text
.
├── Dockerfile                  # Multi-stage OCI Docker build
├── docker-compose.yml          # Docker Compose setup (Postgres, WireMock, Order Service)
├── pom.xml                     # Maven dependencies & plugins configuration
├── src
│   ├── main
│   │   ├── java/com/example/webflux
│   │   │   ├── client/          # WebClient integrations (ExternalDiscountClient, ExternalNotificationClient)
│   │   │   ├── config/          # WebClient & Swagger UI configurations
│   │   │   ├── controller/      # WebFlux REST & SSE controllers (OrderController)
│   │   │   ├── domain/          # R2DBC entities and Enums (Order, OrderItem, OrderStatus)
│   │   │   ├── dto/             # Request, Response, and Internal DTOs
│   │   │   ├── exception/       # Global exception handling (GlobalExceptionHandler)
│   │   │   ├── mapper/          # DTO <-> Entity mappers (@Component OrderMapper)
│   │   │   ├── repository/      # Spring Data R2DBC repositories
│   │   │   └── service/         # Reactive services (OrderService, OrderServiceImpl)
│   │   └── resources
│   │       ├── db/migration/    # Flyway SQL migrations (V1__init_schema.sql)
│   │       └── application.yml  # Primary Spring Boot configuration
│   └── test/                    # Reactive unit and integration tests
└── wiremock/mappings/           # Stub mappings for WireMock
```
