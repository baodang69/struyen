# Rule: PostgreSQL & Spring Data JPA Best Practices

Guidelines for data modeling, queries, and PostgreSQL operations in Struyen.

## 1. Entity Guidelines
- **Lombok Usage:** Always use `@Getter`, `@Setter`, `@NoArgsConstructor`, and `@AllArgsConstructor`. **DO NOT** use `@Data` on JPA Entities with relations (`@ManyToOne`, `@OneToMany`) to avoid circular `equals()`, `hashCode()`, and `toString()` infinite loops.
- **Lazy Loading:** Always set `fetch = FetchType.LAZY` on `@ManyToOne` and `@ManyToMany` mappings.
- **Timestamps:** Use `@CreationTimestamp` and `@UpdateTimestamp` on audit columns (`createdAt`, `updatedAt`).

## 2. PostgreSQL & JSONB Handling
- Use Hibernate 6 native JSON mapping:
  ```java
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(columnDefinition = "jsonb")
  private List<Map<String, Object>> imagesJson;
  ```
- Store image lists in `chapter.images_json` to prevent row explosion in PostgreSQL.

## 3. Query Optimization & Indexing
- Always check that query filters (`slug`, `is_active`, `updated_at`, foreign keys) have corresponding indexes in the table schema.
- Prevent N+1 query issues by utilizing `JOIN FETCH` or `@EntityGraph` when eager fetching related entities is needed.
- Return `Page<T>` or `Slice<T>` for lists; never load unbounded datasets into memory.

