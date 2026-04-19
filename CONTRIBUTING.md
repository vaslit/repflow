# Contributing

## Commit Format
- Use Conventional Commits.
- Format: `<type>: <summary>`
- Allowed types: `feat`, `fix`, `docs`, `refactor`, `test`, `build`, `ci`, `chore`

## Versioning
- Versioning follows SemVer.
- Release tags use `vX.Y.Z`.
- Examples:
  - `v0.1.0`
  - `v0.2.3`
  - `v1.0.0`

## Push Rules
- `main` is the default branch.
- Prefer working in feature branches unless the change is an initial bootstrap or explicitly approved for direct push.
- Before push, ensure CI-relevant checks pass or describe the blocker in the commit/PR context.

## Releases
- CI runs on pushes and pull requests.
- GitHub Release artifacts are produced from SemVer tags.
