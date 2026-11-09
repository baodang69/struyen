# Rule: Git & Conventional Commits Workflow

Rules for commit hygiene, branch etiquette, and pull requests.

## 1. Conventional Commits Format
Every commit message must follow the Conventional Commits specification:
```text
<type>(<scope>): <short description>

[optional body]

[optional footer(s)]
```

### Types:
- `feat`: A new feature (corresponds with a new note in `docs/features/`)
- `fix`: A bug fix
- `docs`: Documentation changes only (e.g., updating `AGENTS.md`, `docs/features/`)
- `refactor`: A code change that neither fixes a bug nor adds a feature
- `perf`: A code change that improves performance
- `test`: Adding missing tests or correcting existing tests
- `chore`: Build process, dependencies, or tool configurations

### Examples:
- `feat(story): add public endpoint to search stories by category`
- `fix(sync): handle network timeout when calling otruyen api`
- `docs(features): document image proxy cache headers`

## 2. Branching Strategy
- `main` / `master`: Production-ready code.
- `develop`: Integration branch for active features.
- `feature/<feature-name>`: Dedicated branch for developing new capabilities before merging to `develop`.

