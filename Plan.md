# Навчальний план створення реактивного застосунку на Spring WebFlux

---

## 1. Опис навчальної цілі

Основна мета цього проєкту — **практичне засвоєння фундаментальних принципів реактивного та неблокуючого програмування** у екосистемі Java / Spring.

У результаті проходження цього плану розробник навчиться:
* Будувати неблокуючі REST API за допомогою **Spring WebFlux**.
* Працювати з реактивними типами **`Mono`** (0..1 елемент) та **`Flux`** (0..N елементів) бібліотеки Project Reactor.
* Реалізовувати неблокуючу взаємодію з реляційною базой даних PostgreSQL за допомогою **Spring Data R2DBC**.
* Виконувати асинхронні неблокуючі HTTP-запити до зовнішніх мікросервісів через **Spring WebClient**.
* Налаштовувати інтеграційне середовище за допомогою **Docker Compose** (PostgreSQL + WireMock).
* Правильно обробляти помилки в реактивних ланцюжках без розриву потоку даних.
* Вловлювати та запобігати появі блокуючих операцій в Event Loop (використовуючи **BlockHound**).
* Писати ефективні юніт- та інтеграційні тести за допомогою `StepVerifier`, `WebTestClient`, `WireMock` та `Testcontainers`.

---

## 2. Предметна область та Архітектура застосунку

### Предметна область: `Order Processing Service` (Сервіс обробки замовлень)

Для навчання обрано сервіс обробки замовлень інтернет-магазину. Застосунок виконує наступні функції:
1. Приймає запити на створення замовлень від клієнтів.
2. Зберігає замовлення та позиції замовлення в PostgreSQL через **R2DBC**.
3. Звертається до зовнішнього сервісу знижок/промокодів (зовнішній мікросервіс, зімітований через **WireMock**) для розрахунку підсумкової вартості.
4. Надсилає сповіщення про створення замовлення у відповіді на запит  (також зімітований у **WireMock**).
5. Надає реактивні CRUD-ендпоінти для отримання списку замовлень (стрімінг через `Flux` / Server-Sent Events) та інформації про конкретне замовлення (`Mono`).

### Реактивний потік даних (Data Flow)
1. **Request Phase**: Запит потрапляє у Netty Event Loop thread.
2. **Execution Phase**: Запит передається у `OrderController` -> `OrderService` без блокування потоку.
3. **External Calls**: `OrderService` паралельно або послідовно через `flatMap`/`zip` робить неблокуючий запит до PostgreSQL через R2DBC та HTTP-запит до WireMock через `WebClient`.
4. **Response Phase**: Коли дані готові, Netty повертає відповідь клієнту. Потік Event Loop не блокувався на жодному з етапів очікування I/O.

---

## 3. Технологічний стек

| Компонент | Технологія / Бібліотека | Призначення |
| :--- | :--- | :--- |
| **Language & JDK** | Java 25 | Мова програмування |
| **Framework** | Spring Boot 4.x | Базовий фреймворк |
| **Web Layer** | Spring WebFlux (Project Reactor / Netty) | Реактивний веб-сервер та REST API |
| **Database** | PostgreSQL 18 | Реляційна база даних |
| **Reactive Data Access** | Spring Data R2DBC (`r2dbc-postgresql`) | Неблокуючий доступ до БД |
| **Database Migrations** | Flyway (`flyway-core`, `flyway-database-postgresql`) | Автоматичні міграції схеми БД при старті |
| **HTTP Client** | Spring WebClient | Неблокуючий реактивний HTTP-клієнт |
| **Mocking External APIs** | WireMock (Docker Container) | Імітація зовнішнього мікросервісу |
| **Containerization** | Docker & Docker Compose | Інфраструктурне середовище |
| **Validation** | Jakarta Bean Validation (`spring-boot-starter-validation`) | Реактивна валідація DTO |
| **Testing** | JUnit 5, Reactor Test (`StepVerifier`), WebTestClient, Testcontainers, BlockHound | Комплексне тестування реактивного коду |

---

## 4. Повна файлова структура проєкту

```
ExsampleSpringWebFlux/
├── Plan.md                                 # Цей навчальний план
├── docker-compose.yml                      # Docker infrastructure (PostgreSQL + WireMock)
├── pom.xml                                 # Maven dependencies & build configuration
├── wiremock/
│   └── mappings/
│       ├── discount-service.json           # WireMock mock mapping for discounts
│       └── notification-service.json       # WireMock mock mapping for notifications
└── src/
    ├── main/
    │   ├── java/
    │   │   └── com/
    │   │       └── example/
    │   │           └── webflux/
    │   │               ├── WebFluxApplication.java              # Main Application Class & BlockHound init
    │   │               ├── config/
    │   │               │   ├── R2dbcConfig.java                 # R2DBC & Database Converters config
    │   │               │   └── WebClientConfig.java             # WebClient beans configuration
    │   │               ├── controller/
    │   │               │   └── OrderController.java             # WebFlux REST Controller (@RestController)
    │   │               ├── domain/
    │   │               │   ├── entity/
    │   │               │   │   ├── Order.java                   # R2DBC Entity (@Table)
    │   │               │   │   └── OrderItem.java               # R2DBC Entity (@Table)
    │   │               │   └── enums/
    │   │               │       └── OrderStatus.java             # Order Status Enum
    │   │               ├── dto/
    │   │               │   ├── request/
    │   │               │   │   └── CreateOrderRequest.java      # Input DTO with Bean Validation
    │   │               │   └── response/
    │   │               │       ├── DiscountResponse.java        # DTO for WireMock response
    │   │               │       ├── OrderResponse.java           # Output DTO for API
    │   │               │       └── ErrorResponse.java           # Standardized API Error DTO
    │   │               ├── exception/
    │   │               │   ├── GlobalExceptionHandler.java      # @RestControllerAdvice for WebFlux
    │   │               │   ├── OrderNotFoundException.java      # Custom Domain Exception
    │   │               │   └── ExternalServiceException.java    # Custom Integration Exception
    │   │               ├── client/
    │   │               │   ├── ExternalDiscountClient.java      # WebClient wrapper for Discount Service
    │   │               │   └── ExternalNotificationClient.java  # WebClient wrapper for Notification Service
    │   │               ├── repository/
    │   │               │   ├── OrderRepository.java             # ReactiveCrudRepository / R2dbcRepository
    │   │               │   └── OrderItemRepository.java         # ReactiveCrudRepository / R2dbcRepository
    │   │               └── service/
    │   │                   ├── OrderService.java                # Service Interface
    │   │                   └── impl/
    │   │                       └── OrderServiceImpl.java        # Reactive Business Logic implementation
    │   └── resources/
    │       ├── application.yml                                  # Spring configuration (R2DBC, Flyway, WireMock URL)
    │       └── db/
    │           └── migration/
    │               ├── V1__create_orders_table.sql              # Flyway Migration V1
    │               └── V2__create_order_items_table.sql        # Flyway Migration V2
    └── test/
        ├── java/
        │   └── com/
        │       └── example/
        │           └── webflux/
        │               ├── controller/
        │               │   └── OrderControllerTest.java         # WebTestClient Unit/Slice Test
        │               ├── service/
        │               │   └── OrderServiceTest.java            # StepVerifier Service Unit Test
        │               ├── client/
        │               │   └── ExternalDiscountClientTest.java  # WebClient + WireMock Integration Test
        │               └── integration/
        │                   ├── OrderRepositoryIT.java           # R2DBC + Testcontainers Integration Test
        │                   └── FullApplicationIT.java          # End-to-End Application Integration Test
        └── resources/
            └── application-test.yml                             # Test Profile Configuration
```

---

## 5. Золоті правила реактивного програмування

Під час розробки цього застосунку суворо дотримуємося наступних правил:

1. **Жодних блокуючих викликів в Event Loop!**
   * Суворо заборонено використовувати `.block()`, `.blockFirst()`, `.blockLast()`, `.toIterable()`, `Thread.sleep()`.
   * Заборонено використовувати класичне JDBC (`DriverManager`, `DataSource`, `JpaRepository`, `EntityManager`).
   * Заборонено використовувати блокуючий `RestTemplate` або `HttpURLConnection`.

2. **Правильне використання реактивних типів:**
   * `Mono<T>` — для операцій, що повертають **0 або 1** елемент (створення замовлення, пошук за ID, оновлення, видалення).
   * `Flux<T>` — для операцій, що повертають **0..N** елементів (список замовлень, стрімінг даних, пакетна обробка).

3. **Обробка блокуючого legacy-коду (якщо неминуче):**
   * Якщо необхідно викликати блокуючий метод (наприклад, старий SDK або роботу з файловою системою), його МУСИТЬ бути винесено в окремий пул потоків за допомогою `.publishOn(Schedulers.boundedElastic())`.

4. **Реактивний потік "лінивий" (Lazy):**
   * Нічого не відбувається, поки немає підписки (`Nothing happens until you subscribe`).
   * Spring WebFlux автоматично підписується на `Mono`/`Flux`, повернені з контролера. Ручна підписка через `.subscribe()` у production-коді є антипатерном (за винятком fire-and-forget задач з належною обробкою помилок).

5. **Обробка помилок усередині ланцюжка:**
   * Помилки є сигналами у реактивному потоці (`onError`).
   * Використовувати `.onErrorResume()`, `.onErrorMap()`, `.retryWhen()`, `.defaultIfEmpty()` для граційної обробки збоїв.

---

## 6. Детальний покроковий план реалізації

План складається з **21 послідовного етапу**. Кожен етап деталізовано за 7 навчальними пунктами.

---

### Етап 1. Визначення предметної області та архітектури застосунку
* **Що створюємо:** Концептуальну та архітектурну модель проєкту `Order Processing Service`.
* **Для чого це потрібно:** Щоб мати чітке розуміння меж системи, сутностей, DTO та зв'язків між мікросервісами до написання коду.
* **Яку проблему вирішує:** Запобігає хаотичному проектуванню та змішуванню блокуючих і неблокуючих паттернів.
* **Зв'язок із WebFlux:** Визначає, де виникають неблокуючі I/O межі (база даних R2DBC, зовнішній HTTP-сервіс WireMock).
* **На що звернути увагу:** Не проектувати сутності з JPA-аннотаціями на кшталт `@OneToMany` чи `@ManyToOne`, оскільки R2DBC не підтримує lazy loading та відносні мапінги ORM (тут немає Hibernate L1/L2 кшу).
* **Ключові класи/компоненти:** Схема архітектури, доменна модель замовлення.
* **Як перевірити:** Наявність чіткої Mermaid-діаграми та документації моделей у `Plan.md`.

---

### Етап 2. Створення Spring Boot проєкту та налаштування Maven залежностей
* **Що створюємо:** Файл `pom.xml` з необхідними залежностями Spring Boot 3.x.
* **Для чого це потрібно:** Підключення реактивних стартерів (`spring-boot-starter-webflux`, `spring-boot-starter-data-r2dbc`), драйвера `r2dbc-postgresql`, Flyway, Lombok, Validation та тестових бібліотек.
* **Яку проблему вирішує:** Формує білд-конфігурацію без конфліктів транзитивних залежностей та блокуючих стартерів (наприклад, без `spring-boot-starter-web` або `spring-boot-starter-data-jpa`).
* **Зв'язок із WebFlux:** `spring-boot-starter-webflux` автоматично підключає **Project Reactor** та **Netty** як вбудований неблокуючий веб-сервер.
* **На що звернути увагу:** **НЕ додавати** `spring-boot-starter-web` (Spring MVC / Tomcat) або `spring-boot-starter-data-jpa` (Hibernate / JDBC). Якщо вони присутні, Spring Boot за замовчуванням може підняти Tomcat замість Netty.
* **Ключові класи/компоненти:** `pom.xml`.
* **Як перевірити:** Виконати `mvn dependency:tree` і переконатися у відсутності `org.springframework:spring-webmvc` та `org.hibernate`.

---

### Етап 3. Створення Docker Compose для PostgreSQL та WireMock
* **Що створюємо:** Файл `docker-compose.yml` та мапінги WireMock у папці `wiremock/mappings/`.
* **Для чого це потрібно:** Локальний запуск повністю ізольованої інфраструктури: СУБД PostgreSQL на порту 5432 та зовнішній сервіс WireMock на порту 8080.
* **Яку проблему вирішує:** Забезпечує відтворюваність середовища розробки без необхідності локального встановлення PostgreSQL та розгортання реальних зовнішніх мікросервісів.
* **Зв'язок із WebFlux:** Дозволяє випробувати неблокуючу роботу з PostgreSQL через R2DBC та реалістичні HTTP-відповіді/затримки/помилки від WireMock.
* **На що звернути увагу:** У налаштуваннях PostgreSQL перевірити доступи (user, password, database). Для WireMock налаштувати JSON-файли імітації відповіді (наприклад, ендпоінти `/api/v1/discounts/{code}` та `/api/v1/notifications`).
* **Ключові класи/компоненти:** `docker-compose.yml`, `wiremock/mappings/discount-service.json`.
* **Як перевірити:** Запустити `docker compose up -d` та перевірити доступність Postgres (`docker compose exec postgres pg_isready`) і WireMock (`curl http://localhost:8080/__admin`).

---

### Етап 4. Налаштування схеми БД та міграцій через Flyway
* **Що створюємо:** SQL-скрипти міграцій `V1__create_orders_table.sql` та `V2__create_order_items_table.sql` у `src/main/resources/db/migration/`.
* **Для чого це потрібно:** Автоматичне створення та версіонування таблиць `orders` та `order_items` в PostgreSQL.
* **Яку проблему вирішує:** R2DBC не має інструменту ddl-auto (як Hibernate), тому схема бази даних повинна керовано створюватися міграціями.
* **Зв'язок із WebFlux:** Flyway виконує міграції при старті застосунку через JDBC драйвер (оскільки Flyway за своєю природою блокуючий інструмент стадії запуску), після чого застосунок працює виключно через неблокуючий R2DBC драйвер.
* **На що звернути увагу:** Підключити `flyway-core` та `flyway-database-postgresql`, а в `application.yml` вказати стандартні JDBC URL для Flyway (`jdbc:postgresql://...`) та R2DBC URL для самого застосунку (`r2dbc:postgresql://...`).
* **Ключові класи/компоненти:** `application.yml`, Flyway Auto-configuration.
* **Як перевірити:** При старті застосунку у логах має з'явитися `Flyway Community Edition ... Migration to version v1 ... successfully applied`. Таблиці мають з'явитися у БД.

---

### Етап 5. Налаштування реактивного доступу до PostgreSQL через R2DBC
* **Що створюємо:** Конфігураційний файл `application.yml` та `R2dbcConfig.java`.
* **Для чого це потрібно:** Налаштування `ConnectionFactory` для R2DBC, параметрів пулу з'єднань (R2DBC Pool) та конвертерів для Enum/Custom типів.
* **Яку проблему вирішує:** Забезпечує повністю неблокуюче з'єднання з базою даних через протокол R2DBC (Reactive Relational Database Connectivity).
* **Зв'язок із WebFlux:** Запити до бази даних виконуються асинхронно, повертаючи `Mono` та `Flux`, не блокуючи робочі потоки Netty.
* **На що звернути увагу:** URL бази даних повинен починатися з `r2dbc:postgresql://`, а не `jdbc:postgresql://`. Налаштувати розмір пулу `r2dbc.pool.initial-size` та `max-size`.
* **Ключові класи/компоненти:** `ConnectionFactory`, `R2dbcTransactionManager`, `@EnableR2dbcRepositories`.
* **Як перевірити:** Перевірити успішне підключення при старті та відсутність `HikariCP` / JDBC у стеку викликів.

---

### Етап 6. Створення Domain/Model класів та DTO
* **Що створюємо:** Сутності R2DBC (`Order`, `OrderItem`), Enums (`OrderStatus`), а також DTO запитів та відповідей (`CreateOrderRequest`, `OrderResponse`, `ErrorResponse`).
* **Для чого це потрібно:** Представлення даних у БД та ізоляція внутрішньої доменної моделі від зовнішнього REST API.
* **Яку проблему вирішує:** Запобігає витоку внутрішніх деталей БД у REST API та забезпечує незмінність (immutability) даних.
* **Зв'язок із WebFlux:** В R2DBC сутності є легкими POJO-класами з аннотаціями `@Table`, `@Id`, `@Column`.
* **На що звернути увагу:** Не використовувати JPA-аннотації `@Entity`, `@Table(name=...)` з `jakarta.persistence`. Використовувати аннотації з `org.springframework.data.annotation` та `org.springframework.data.relational.core.mapping`. Для генерованого первинного ключа в R2DBC поле `id` має бути `Long` (або `UUID`) і дорівнювати `null` перед першим збереженням.
* **Ключові класи/компоненти:** `Order`, `OrderItem`, `@Id`, `@Table`, `CreateOrderRequest`.
* **Як перевірити:** Коректність кореляції полів сутностей із SQL-схемою Flyway.

---

### Етап 7. Створення Reactive Repositories
* **Що створюємо:** Інтерфейси `OrderRepository` та `OrderItemRepository`.
* **Для чого це потрібно:** Отримання готових реактивних методів маніпуляції даними (`save`, `findById`, `findAll`, `deleteById`).
* **Яку проблему вирішує:** Виключає необхідність написання шаблонного коду для CRUD операцій з БД.
* **Зв'язок із WebFlux:** Інтерфейси успадковують `R2dbcRepository<Order, Long>` або `ReactiveCrudRepository<Order, Long>`. Усі методи повертають `Mono<T>` або `Flux<T>`.
* **На що звернути увагу:** Якщо додаються кастомні запити через `@Query`, вони повинні повертати `Mono` або `Flux`. Не використовувати синхронні методи.
* **Ключові класи/компоненти:** `R2dbcRepository`, `ReactiveCrudRepository`, `@Query`.
* **Як перевірити:** Написати простий тест збереження та пошуку сутності за допомогою `StepVerifier`.

---

### Етап 8. Реалізація неблокуючого HTTP-клієнта через `WebClient`
* **Що створюємо:** `WebClientConfig.java`, а також клієнти `ExternalDiscountClient` та `ExternalNotificationClient`.
* **Для чого це потрібно:** Виконання HTTP-запитів до WireMock для отримання знижок та надсилання нотифікацій.
* **Яку проблему вирішує:** Замінює застарілий та блокуючий `RestTemplate` на реактивний `WebClient`.
* **Зв'язок із WebFlux:** `WebClient` побудований на базе Reactor Netty HTTP client. Він виконує асинхронні I/O операції та повертає `Mono<T>` або `Flux<T>`.
* **На що звернути увагу:** Конфігурувати `WebClient.Builder` як Spring Bean. Використовувати `uriBuilder` для формування ш шляхів, `.retrieve()`, `.bodyToMono()` або `.bodyToFlux()`. Налаштувати таймаути підключення та читання на рівні `HttpClient`.
* **Ключові класи/компоненти:** `WebClient`, `WebClient.Builder`, `HttpClient` (Reactor Netty).
* **Як перевірити:** Перевірити виклики методів клієнта та переконатися, що вони повертають `Mono<DiscountResponse>`.

---

### Етап 9. Реалізація Service Layer (Реактивна бізнес-логіка)
* **Що створюємо:** Інтерфейс `OrderService` та його реалізацію `OrderServiceImpl`.
* **Для чого це потрібно:** Об'єднання роботи репозиторіїв R2DBC, HTTP-клієнтів WebClient та бізнес-правил в єдиний реактивний потік.
* **Яку проблему вирішує:** Ізолює бізнес-логіку від контролерів та інфраструктури.
* **Зв'язок із WebFlux:** Вся логіка будується як декларативний реактивний ланцюжок: `repository.save()` -> `flatMap(order -> discountClient.getDiscount().map(...))` -> `repository.save()`.
* **На що звернути увагу:** Не розривати ланцюжок reactive streams. Використовувати `flatMap` для асинхронних трансформацій, що повертають `Mono`/`Flux`, і `map` для синхронних чистих функцій. Уникати виклику `.subscribe()` всередині сервісу.
* **Ключові класи/компоненти:** `OrderService`, `OrderServiceImpl`, `Mono.flatMap()`, `Mono.zip()`, `Flux.flatMap()`.
* **Як перевірити:** Написати unit-тест з моками репозиторію та WebClient, перевіривши потік через `StepVerifier`.

---

### Етап 10. Реалізація Валідації вхідних даних
* **Що створюємо:** Аннотації валідації в `CreateOrderRequest` (`@NotNull`, `@NotBlank`, `@Positive`, `@Size`).
* **Для чого це потрібно:** Гарантія коректності даних до їх потрапляння в бізнес-логіку та БД.
* **Яку проблему вирішує:** Запобігає некоректному стану системи та дає змогу швидко повернути клієнту 400 Bad Request.
* **Зв'язок із WebFlux:** WebFlux інтегрований з Jakarta Bean Validation. При використанні `@Valid` у параметрах методу контролера помилки валідації повертають `WebExchangeBindException`.
* **На що звернути увагу:** Валідація відбувається до виконання реактивного ланцюжка контролера.
* **Ключові класи/компоненти:** `@Valid`, `Validator`, `WebExchangeBindException`.
* **Як перевірити:** Відправити некоректний JSON на контролер і переконатися у отриманні відповіді 400.

---

### Етап 11. Створення WebFlux REST API (Controller Layer)
* **Що створюємо:** `OrderController.java` з CRUD-ендпоінтами.
* **Для чого це потрібно:** Надання зовнішнього REST API для клієнтів.
* **Яку проблему вирішує:** Отримання HTTP-запитів та передача їх у реактивні сервіси з поверненням відповідних статусів HTTP.
* **Зв'язок із WebFlux:** Використання `@RestController` або функціональних роутерів (`RouterFunction` / `HandlerFunction`). Метод повертає `Mono<ResponseEntity<OrderResponse>>` або `Flux<OrderResponse>`. Для стрімінгу можна вказати `produces = MediaType.TEXT_EVENT_STREAM_VALUE`.
* **На що звернути увагу:** Контролер НЕ повинен розпаковувати `Mono`/`Flux` (ніяких `mono.block()`). Всі методи повертають реактивні типи напряму Spring WebFlux framework.
* **Ключові класи/компоненти:** `@RestController`, `@PostMapping`, `@GetMapping`, `Mono`, `Flux`, `ServerSentEvents`.
* **Як перевірити:** Виконати curl-запит та отримати JSON-відповідь або Event-Stream.

---

### Етап 12. Написання Глобального обробника помилок
* **Що створюємо:** `GlobalExceptionHandler.java` з аннотацією `@RestControllerAdvice`.
* **Для чого це потрібно:** Централізоване вловлювання винятків у реактивних потоках та перетворення їх у кастомний `ErrorResponse` (або `ProblemDetail` у Spring Boot 3).
* **Яку проблему вирішує:** Запобігає витоку внутрішніх stack trace клієнту та забезпечує уніфікований формат помилок API.
* **Зв'язок із WebFlux:** Перехоплює винятки, що виникли в реактивних потоках контролерів, і повертає `Mono<ResponseEntity<ErrorResponse>>`.
* **На що звернути увагу:** Обробляти `WebExchangeBindException` (валідція), `OrderNotFoundException` (404), `ExternalServiceException` (503/502) та загальний `Exception` (500).
* **Ключові класи/компоненти:** `@RestControllerAdvice`, `@ExceptionHandler`, `ServerWebExchange`, `Mono.just()`.
* **Як перевірити:** Викликати помилкові сценарії (наприклад, пошук неіснуючого ID) та перевірити структуру JSON відповіді помилки.

---

### Етап 13. Обробка помилок та стійкість у реактивних потоках
* **Що створюємо:** Реактивну логіку обробки збоїв у `ExternalDiscountClient` та `OrderServiceImpl`.
* **Для чого це потрібно:** Забезпечення стійкості (resilience) застосунку при відмові зовнішніх сервісів чи бази даних.
* **Яку проблему вирішує:** Запобігає повній зупинці обробки запиту у разі тимчасових мережевих збоїв.
* **Зв'язок із WebFlux:** Використання реактивних операторів `.onErrorResume()`, `.onErrorMap()`, `.retryWhen(Retry.backoff(...))`, `.timeout(Duration.ofSeconds(...))`.
* **На що звернути увагу:** Визначити fallback-поведінку (наприклад, якщо Discount Service недоступний — застосувати 0% знижки замість падіння всього замовлення, або повернути виразну помилку).
* **Ключові класи/компоненти:** `onErrorResume`, `onErrorReturn`, `retryWhen`, `Retry.backoff()`, `timeout`.
* **Як перевірити:** Налаштувати WireMock на повернення помилки 500 або затримку (delay) і перевірити спрацювання retry/fallback.

---

### Етап 14. Демонстрація складних операторів та парного використання `Mono` і `Flux`
* **Що створюємо:** Додатковий ендпоінт аналітики або зведеної інформації по замовленням у `OrderService`.
* **Для чого це потрібно:** Глибоке практичне розуміння можливостей Project Reactor (`zip`, `combineLatest`, `flatMapSequential`, `publishOn`, `subscribeOn`).
* **Яку проблему вирішує:** Демонструє, як паралельно виконувати незалежні асинхронні операції (наприклад, паралельний запит ціни, знижки та залишків на складі).
* **Зв'язок із WebFlux:** `Mono.zip(monoA, monoB)` виконує обидва запити одночасно у неблокуючому стилі та об'єднує результати, коли обидва готові.
* **На що звернути увагу:** Різниця між `flatMap` (не зберігає порядок/виконує паралельно) та `concatMap` (зберігає порядок/виконує послідовно).
* **Ключові класи/компоненти:** `Mono.zip()`, `Flux.merge()`, `Flux.flatMap()`, `Schedulers`.
* **Як перевірити:** Заміряти час виконання двох паралельних запитів по 100мс (сумарний час має бути ~100мс, а не 200мс).

---

### Етап 15. Аудит та перевірка відсутності блокуючих операцій (BlockHound)
* **Що створюємо:** Підключення та ініціалізація **BlockHound** у `WebFluxApplication.java` та у тесах.
* **Для чого це потрібно:** Автоматичне виявлення блокуючих викликів I/O чи `Thread.sleep()` у потоках Reactor Event Loop.
* **Яку проблему вирішує:** Гарантує, що розробник випадково не вніс блокуючу бібліотеку або виклик у неблокуючий потік.
* **Зв'язок із WebFlux:** BlockHound вбудовується у JVM через ByteBuddy і кидає `BlockingOperationError`, якщо в потоці `reactor-http-nio` робиться блокуючий виклик.
* **На що звернути увагу:** Ініціалізувати `BlockHound.install()` у `main` методі до старту Spring контейнера або у тестовому ранері.
* **Ключові класи/компоненти:** `BlockHound.install()`, `BlockingOperationError`.
* **Як перевірити:** Створити тимчасовий тестовий метод із `Thread.sleep(100)` усередині `Mono.just().map()` та переконатися, що BlockHound впадає з помилкою.

---

### Етап 16. Unit-тестування реактивних компонентів через `StepVerifier`
* **Що створюємо:** Тести `OrderServiceTest.java` з використанням Mockito та Reactor Test.
* **Для чого це потрібно:** Перевірка коректності реактивної бізнес-логіки в ізоляції.
* **Яку проблему вирішує:** Класичні `assertEquals` не працюють для асинхронних `Mono`/`Flux`, оскільки дані з'являються з часом.
* **Зв'язок із WebFlux:** `StepVerifier` підписується на реактивний потік і віртуально керує часом або перевіряє послідовність сигналів (`onNext`, `onError`, `onComplete`).
* **На що звернути увагу:** Використовувати `StepVerifier.create(mono)` -> `.expectNext(...)` -> `.verifyComplete()`.
* **Ключові класи/компоненти:** `StepVerifier`, `PublisherProbe`, `Mockito.when().thenReturn(Mono.just(...))`.
* **Як перевірити:** Успішне проходження `mvn test`.

---

### Етап 17. Інтеграційне тестування WebFlux API з `WebTestClient`
* **Що створюємо:** `OrderControllerTest.java`.
* **Для чого це потрібно:** Тестування HTTP REST ендпоінтів, мапінгу JSON, валідації та статусів відповідей.
* **Яку проблему вирішує:** Перевірка веблаєра WebFlux без запуск у повного сервера чи з легким WebTestClient binder.
* **Зв'язок із WebFlux:** `WebTestClient` — неблокуючий тестовий HTTP-клієнт, спеціально розроблений для Spring WebFlux.
* **На що звернути увагу:** Використовувати `@WebFluxTest(controllers = OrderController.class)` або підключати `WebTestClient.bindToController(...)`.
* **Ключові класи/компоненти:** `WebTestClient`, `@MockBean`, `expectStatus()`, `expectBody()`.
* **Як перевірити:** Запуск тесту контролера з перевіркою статусу 201 Created та JSON полів.

---

### Етап 18. Інтеграційне тестування взаємодії з WireMock
* **Що створюємо:** `ExternalDiscountClientTest.java`.
* **Для чого це потрібно:** Перевірка реальної реактивної HTTP-взаємодії `WebClient` із зовнішнім сервісом.
* **Яку проблему вирішує:** Перевіряє мапінг JSON від відповідей зовнішнього API, обробку помилок 4xx/5xx та таймаутів.
* **Зв'язок із WebFlux:** Використовує WireMockServer / WireMockExtension для перехоплення неблокуючих запитів `WebClient`.
* **На що звернути увагу:** Переконатися, що `WebClient` коректно розпарсує JSON від WireMock і що реактивний потік правильно обробляє помилкові статуси (наприклад, 404 Not Found від знижкового сервісу).
* **Ключові класи/компоненти:** `WireMockExtension`, `stubFor(get(...))`, `StepVerifier`.
* **Як перевірити:** Перевірити у тесті, що при поверненні 200 від WireMock `WebClient` повертає очікуваний `DiscountResponse`.

---

### Етап 19. Інтеграційне тестування PostgreSQL через Testcontainers & R2DBC
* **Що створюємо:** `OrderRepositoryIT.java`.
* **Для чого це потрібно:** Перевірка виконання реальних SQL-запитів до справжньої бази даних PostgreSQL під час автоматичної збірки.
* **Яку проблему вирішує:** Запобігає розходженням між H2/вбудованими БД та реальним PostgreSQL.
* **Зв'язок із WebFlux:** `Testcontainers` піднімає Docker-контейнер PostgreSQL, а Spring Data R2DBC підключається до нього через реактивний R2DBC URL.
* **На що звернути увагу:** Налаштувати `@DynamicPropertySource` для динамічної підстановки R2DBC та JDBC (для Flyway) URL тестового контейнера.
* **Ключові класи/компоненти:** `@Testcontainers`, `PostgreSQLContainer`, `StepVerifier`.
* **Як перевірити:** Викопати реальний `save` та `findById` у тесті та переконатися, що дані записуються та зчитуються з контейнера Postgres.

---

### Етап 20. Налаштування Dockerfile для Spring Boot застосунку
* **Що створюємо:** Багатоетапний (Multi-stage) `Dockerfile` у корені проєкту.
* **Для чого це потрібно:** Контейнеризація самого Spring Boot WebFlux застосунку для зручного розгортання.
* **Яку проблему вирішує:** Формування мінімального та безпечного OCI-образу без зайвого сміття початкового сирцевого коду.
* **Зв'язок із WebFlux:** Оптимізація параметрів JVM для неблокуючого середовища (пам'ять, Netty direct memory).
* **На що звернути увагу:** Перший етап (builder): `maven:3.9-eclipse-temurin-17` -> `mvn clean package -DskipTests`. Другий етап (runner): `eclipse-temurin:17-jre-alpine` -> запуск `java -jar app.jar`.
* **Ключові класи/компоненти:** `Dockerfile`.
* **Як перевірити:** Зібрати образ через `docker build -t order-service:latest .` та перевірити його запуск.

---

### Етап 21. Фінальний запуск та перевірка всієї системи у Docker Compose (E2E)
* **Що створюємо:** Оновлений `docker-compose.yml`, який включає 3 сервіси: `postgres`, `wiremock` та `order-service`.
* **Для чого це потрібно:** Повноцінний підсумковий запуск та перевірка всієї реактивної системи в єдиній Docker мережі.
* **Яку проблему вирішує:** Фінальне підтвердження того, що всі компоненти (WebFlux API, R2DBC, Flyway, WireMock, PostgreSQL) узгоджено працюють разом.
* **Зв'язок із WebFlux:** Повний End-to-End шлях реактивного запиту у реальному контейнеризованому середовищі.
* **На що звернути увагу:** Правильне налаштування `depends_on` з `condition: service_healthy` (щоб `order-service` чекав готовності Postgres та WireMock). Налаштування внутрішньої мережі Docker (`networks`).
* **Ключові класи/компоненти:** `docker-compose.yml`, `curl` / Postman скрипти перевірки.
* **Як перевірити:** Виконати `docker compose up --build`, відправити POST запит на створення замовлення, перевірити записи у Postgres та логи WireMock.

---

## 7. Критерії готовності застосунку (Definition of Done)

Проєкт вважається успішно виконаним, якщо:

1. [ ] **Код реалізовано згідно з плану**: Створено всі класи згідно з файловою структурою (Розділ 4).
2. [ ] **Всі endpoint-и неблокуючі**: Немає жодного виклику `.block()`, `RestTemplate` або JDBC у production-коді.
3. [ ] **BlockHound пройдено**: Тести з BlockHound підтверджують відсутність блокуючих операцій у реактивних потоках.
4. [ ] **База даних підключена через R2DBC**: Всі операції з PostgreSQL здійснюються через Spring Data R2DBC.
5. [ ] **Міграції Flyway працюють**: При старті застосунку схема БД успішно створюється Flyway-скриптами.
6. [ ] **HTTP-запити через WebClient**: Взаємодія з WireMock реалізована через `WebClient` з обробкою таймаутів та помилок.
7. [ ] **Обробка помилок уніфікована**: `@RestControllerAdvice` повертає зрозумілі JSON-структури помилок (400, 404, 500, 503).
8. [ ] **Покриття тестами**:
   * Написано unit-тести для сервісів з `StepVerifier`.
   * Написано тести контролерів з `WebTestClient`.
   * Написано інтеграційні тести з `WireMock` та `Testcontainers` (PostgreSQL).
9. [ ] **Docker Compose працює "з коробки"**: Команда `docker compose up --build` успішно піднімає весь стек (Postgres + WireMock + WebFlux App), міграції проходяться, і API відповідає на запити.
