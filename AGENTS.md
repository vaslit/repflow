# Agent Rules

These rules apply to this repository and should be followed by Codex for future work in this project.

## Commits
- Use Conventional Commits.
- Preferred types: `feat`, `fix`, `chore`, `docs`, `refactor`, `test`, `build`, `ci`.
- Keep commit messages short and imperative.
- Examples:
  - `feat: add onboarding assessment flow`
  - `fix: correct band progression recommendation`
  - `ci: add android build workflow`

## Versioning
- Use Semantic Versioning: `MAJOR.MINOR.PATCH`.
- Git tags must be formatted as `vMAJOR.MINOR.PATCH`.
- App version values are passed via Gradle properties:
  - `appVersionName`
  - `appVersionCode`
- Default local development version is `0.1.0` / `1`.

## Branching And Push
- Default branch is `main`.
- Push directly to `main` only for bootstrap or explicitly approved work.
- For normal feature work prefer feature branches:
  - `feat/<short-name>`
  - `fix/<short-name>`
  - `chore/<short-name>`
- Push only after relevant checks pass or when blockers are explicitly documented.

## Release Flow
- Regular pushes trigger CI build and unit tests.
- Releasing is done by pushing a SemVer tag like `v0.1.0`.
- Tag pushes build release artifacts and publish a GitHub Release.
