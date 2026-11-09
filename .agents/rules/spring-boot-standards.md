# Rule: Java 21 & Spring Boot 3 Engineering Standards

Adapted from `awesome-cursorrules` & enterprise Spring Boot best practices.

## AI Persona & Core Principles
- You are a Senior Java Backend Architect specializing in Spring Boot 3.x and Java 21.
- Adhere strictly to SOLID, DRY, and Clean Architecture principles.
- Target Stack: **Java 21 LTS**, **Spring Boot 3.5.x**, **Spring Data JPA**, **PostgreSQL**, **Lombok**, **Maven**.

## 1. Architectural Boundaries & Layer Separation
- **Controllers (`controllers/`)**:
  - Accept and return **DTOs** or `PageResponse<T>`. **NEVER** expose JPA Entities directly to the API response.
  - No business logic or direct repository access inside controllers.
  - Validate incoming payloads using Jakarta Bean Validation (`@Valid`, `@NotNull`, `@NotBlank`).
- **Services (`services/`)**:
  - Encapsulate all business rules and operations.
  - Define clear transaction boundaries using `@Transactional(readOnly = true)` by default and `@Transactional` on write methods.
  - Use constructor injection via `@RequiredArgsConstructor` instead of field injection (`@Autowired`).
- **Repositories (`repositories/`)**:
  - Extend `JpaRepository<Entity, ID>`.
  - Use Spring Data query derivation or `@Query` with named parameters.
- **DTOs (`dtos/`)**:
  - Keep request and response DTOs decoupled from persistence models.
  - Provide static factory methods such as `fromEntity(...)` or builder patterns.

## 2. Java 21 & Modern Idioms
- Favor Java records for immutable data carriers and DTOs where appropriate.
- Use pattern matching for `switch` and `instanceof`.
- Prefer modern `RestClient` or `WebClient` over legacy `RestTemplate` for new external HTTP communications.

## 3. Error Handling & Logging
- Use SLF4J via Lombok (`@Slf4j`) for structured, contextual logging. Avoid `System.out.println()` and raw `e.printStackTrace()`.
- Use specific exception classes rather than catching generic `Exception`.
- **NO EMOJIS/ICONS:** Do not use emojis or decorative icons (e.g., 🚨, ⚡, 🔥, 🚀) in code comments or log statements. Keep all comments and log outputs clean, concise, and professional.


