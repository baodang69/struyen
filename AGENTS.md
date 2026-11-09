# AGENTS.md - Struyen Backend Instructions

Struyen (`struyen_be`) is a Spring Boot 3 & Java 21 backend aggregator platform for reading webtoons/manga using the Otruyen API with local caching in PostgreSQL (JSONB) and image proxying.

---

## 1. Tech Stack & Key Conventions

- **Language & Runtime:** Java 21 LTS, Spring Boot 3.5.x
- **Database & ORM:** PostgreSQL, Spring Data JPA, Hibernate (JSONB for chapter images)
- **HTTP Client:** `RestTemplate` (migrating to `RestClient` for modern non-blocking calls)
- **Security:** Spring Security (Permit all public reader endpoints, CSRF disabled, CORS enabled)
- **Scheduling:** Spring `@EnableScheduling` (`@Scheduled` cron jobs)

### Layering Rules
- **Controllers (`controllers/`)**: HTTP endpoints only. Accept/return DTOs (`PageResponse<T>`). Never accept or return raw JPA Entities. No business logic.
- **Services (`services/`)**: Core business logic, transaction management (`@Transactional`).
- **Repositories (`repositories/`)**: Spring Data `JpaRepository` interfaces.
- **Entities (`entities/`)**: Hibernate entities. Avoid `@Data` on relationships; use `@Getter`, `@Setter`, `@NoArgsConstructor`. Always use `FetchType.LAZY` for relations.
- **DTOs (`dtos/`)**: Request/Response models with static factory methods (e.g. `fromEntity(...)`).

### Clean Code & Professional Standards (STRICT)
- **NO EMOJIS / ICONS:** Tuyệt đối KHÔNG sử dụng icon hoặc emoji (ví dụ: 🚨, ⚡, 🔥, 🚀, 💡, ✨, v.v.) trong mã nguồn, comments, log statements, và commit messages. Mọi nội dung code và git log phải hoàn toàn sạch sẽ, súc tích và chuyên nghiệp.


---

## 2. Essential Commands

```bash
# Compile and build
./mvnw clean compile

# Run tests
./mvnw test

# Start local server (port 8080)
./mvnw spring-boot:run

# Package executable JAR
./mvnw clean package -DskipTests
```

---

## 3. Feature Tracking Protocol (MANDATORY)

Every newly added or modified feature **MUST** be recorded:
1. Create a dedicated markdown file in `docs/features/<XX-feature-name>.md` following [`FEATURE_TEMPLATE.md`](file:///d:/Code/struyen_be/docs/features/FEATURE_TEMPLATE.md).
2. Register the feature in the index table in [`docs/features/README.md`](file:///d:/Code/struyen_be/docs/features/README.md).
3. Keep feature notes focused on: purpose, modified/added files, API endpoints, schema changes, and verification steps.

---

## 4. Agent Customizations & Standards

- **Rules (`.agents/rules/`):**
  - [`spring-boot-standards.md`](file:///d:/Code/struyen_be/.agents/rules/spring-boot-standards.md): Code style, layer boundaries, REST conventions.
  - [`jpa-database.md`](file:///d:/Code/struyen_be/.agents/rules/jpa-database.md): PostgreSQL, JSONB, indexing, transaction boundaries.
  - [`git-workflow.md`](file:///d:/Code/struyen_be/.agents/rules/git-workflow.md): Conventional commits and branching strategy.
  - [`feature-documentation.md`](file:///d:/Code/struyen_be/.agents/rules/feature-documentation.md): Strict feature logging requirements.

- **Skills (`.agents/skills/`):**
  - `spring-boot-feature-builder`: End-to-end guide to add new APIs/features cleanly.
  - `otruyen-sync-manager`: Guide for debugging and expanding Otruyen sync logic.
  - `feature-logger`: Standard guide to log new features.

