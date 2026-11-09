---
name: spring-boot-feature-builder
description: >-
  Use this skill when implementing a new backend feature, REST API endpoint, service, or database entity in the Struyen Spring Boot project.
---

# Spring Boot Feature Builder Skill

This skill provides the standard step-by-step workflow for implementing clean, production-grade features in `struyen_be`.

## Feature Development Workflow

### Step 1: Database & Entity Layer
1. If adding new tables or columns, add the DDL script to `docs/database/schema.sql`.
2. Define or update the entity in `src/main/java/com/struyen/struyen/entities/`:
   - Annotate with `@Entity`, `@Table(indexes = {...})`.
   - Use `@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@Builder`.
   - Never use `@Data` when relationships exist.
   - For child relations, use `@ManyToOne(fetch = FetchType.LAZY)`.
   - For JSON columns, use `@JdbcTypeCode(SqlTypes.JSON)`.

### Step 2: Repository Layer
1. Create or update the repository interface in `src/main/java/com/struyen/struyen/repositories/`.
2. Extend `JpaRepository<Entity, ID>`.
3. Implement index-backed queries or pagination methods returning `Page<Entity>`.

### Step 3: DTO Layer
1. Create request/response DTOs in `src/main/java/com/struyen/struyen/dtos/`.
2. Add a static factory method `fromEntity(Entity entity)` to map persistence models to responses.
3. For paginated responses, use `PageResponse<T>`.

### Step 4: Service Layer
1. Create or update the service in `src/main/java/com/struyen/struyen/services/`.
2. Annotate with `@Service` and `@RequiredArgsConstructor`.
3. Use `@Transactional(readOnly = true)` for query methods, `@Transactional` for state mutations.
4. Ensure all business validations and exceptions are thrown here.

### Step 5: Controller Layer
1. Create or update the controller in `src/main/java/com/struyen/struyen/controllers/`.
2. Annotate with `@RestController`, `@RequestMapping("/api/v1/...")`, and `@RequiredArgsConstructor`.
3. Return `ResponseEntity<T>` or `ResponseEntity<PageResponse<T>>`.

### Step 6: Verify & Document
1. Run `./mvnw compile` to verify compilation.
2. Run `./mvnw test` to ensure existing tests pass.
3. Use the `feature-logger` skill to document the feature in `docs/features/`.

