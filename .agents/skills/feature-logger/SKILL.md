---
name: feature-logger
description: >-
  Use this skill whenever a feature is implemented, updated, or refactored to document it into the docs/features registry.
---

# Feature Logger Skill

This skill guides the agent in documenting completed features following the project's standard convention.

## Execution Steps

### 1. Identify Feature Details
- Feature code: e.g. `F-06 / USER-AUTH`
- Feature title: e.g. `Hệ thống xác thực người dùng JWT`
- Affected files: Entities, DTOs, Controllers, Services, Repositories, Configurations.
- API Endpoints: Methods, paths, parameters, response schema.

### 2. Create the Feature File
1. Copy format from `docs/features/FEATURE_TEMPLATE.md`.
2. Save to `docs/features/<next_number>-<feature_slug>.md` (e.g. `docs/features/06-user-auth.md`).
3. Fill in all sections:
   - Overview & Objectives
   - Mermaid sequence/architecture diagram
   - Database changes
   - Key source files
   - Endpoints & cURL examples
   - Verification steps

### 3. Update the Master Registry
1. Open `docs/features/README.md`.
2. Add a new row to the table in Section 1 with the link to the newly created file.

