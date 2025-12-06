# Repository Guidelines

## Project Structure & Module Organization
- Spring Boot 3.5.6 / Java 17 project driven by Gradle (`build.gradle`, `settings.gradle`).
- Source lives in `src/main/java/pproject/once_upon_a_time`; DDD-style domains (`domain/<bounded-context>/controller|service|repository|domain|dto`), auth helpers in `auth`, shared utilities in `global`, and app-wide configs in `config`.
- API configuration and properties sit in `src/main/resources` (`application.yml` with `application-dev.yml` and `application-prod.yml` profiles).
- Tests mirror the main packages under `src/test/java/pproject/once_upon_a_time`.

## Build, Test, and Development Commands
- `./gradlew bootRun` — start the API locally using the active profile (set `SPRING_PROFILES_ACTIVE=dev` for local).
- `./gradlew build` — compile, run tests, and package the app.
- `./gradlew test` — execute the JUnit/Spring test suite.
- `./gradlew clean` — remove build artifacts before a fresh build.

## Coding Style & Naming Conventions
- Java 17, 4-space indentation, Lombok for boilerplate; prefer constructor injection.
- Follow existing package layout; new domain code belongs under `domain/<context>/...` to keep DDD boundaries clear.
- Class names are `PascalCase`, fields and methods `camelCase`; REST endpoints use kebab-case paths where applicable.
- Database naming (if adding entities): tables `lower_snake_case`, PK `id`, timestamps `created_at/updated_at/deleted_at`.
- Use Spring annotations consistently; keep controller DTOs in `dto` and entity logic in `domain`.

## Testing Guidelines
- Framework: JUnit 5 with Spring Boot test support; security helpers via `spring-security-test`.
- Name test classes `*Test` and mirror package paths; focus on service logic and controller slices.
- For new features, add happy-path and failure-path tests; prefer mocking external integrations and keeping tests profile-isolated.
- Run `./gradlew test` before any PR; keep tests fast and deterministic.

## Commit & Pull Request Guidelines
- Branching: GitHub Flow (`develop` for integration, feature branches like `feat/#issue-number` or `fix/#issue-number`).
- Commit format: `<tag>: <title>` with optional bullet details and issue reference; tags align with Gitmoji (e.g., `feat`, `fix`, `refactor`, `docs`, `test`, `chore`, `hotfix`).
- Examples: `feat: 회원가입 API 구현`, `fix: 회원가입 시 닉네임 중복 오류 수정`.
- PRs: use the template, describe scope and testing, link issues, and add screenshots for user-facing changes when relevant.

## Configuration & Security Notes
- Use profile-specific YAMLs for secrets/URLs; never commit credentials. Prefer environment variables for DB and external keys.
- Swagger/OpenAPI is wired via Springdoc; keep new endpoints documented and secured by existing auth filters/config.
