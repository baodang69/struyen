# Rule: Mandatory Feature Documentation

This rule ensures that the project history, features, and capabilities are always documented.

## Trigger & Requirement
Whenever you:
1. Implement a new API endpoint, service, or architectural module.
2. Complete a significant refactor or modify an existing feature.

You **MUST** perform the following actions before finishing your task:

### Action 1: Create or Update Feature Note
- Path: `docs/features/<XX-feature-name>.md`
- Use the structure defined in `docs/features/FEATURE_TEMPLATE.md`:
  - Feature Code & Status
  - Objectives & Scope
  - Sequence / Architecture diagram
  - Key modified files
  - API endpoints (URL, params, response)
  - Testing & curl instructions

### Action 2: Update Feature Registry
- Path: `docs/features/README.md`
- Add a new row to the table with:
  - ID / Code
  - Name
  - Status (`Hoàn thành` / `Đang phát triển`)
  - Date
  - Link to the feature file

