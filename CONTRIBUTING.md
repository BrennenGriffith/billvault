# Contributing to BillVault

Focused fixes, documentation, accessibility improvements, and well-tested features are welcome.
BillVault includes a self-hosted web app, a native JavaFX desktop app, and an optional MCP import
adapter/skill. Android remains planned. Read the [architecture](docs/ARCHITECTURE.md) and
[billing decisions](docs/DECISIONS.md) before changing shared behavior.

## Pick a starting point

- Reproduce an issue with fictional data and suggest a focused fix.
- Improve keyboard navigation, large-text layouts, or screen-reader labels.
- Clarify installation, Docker, backup, or assistant setup instructions.
- Add a regression case for recurrence, duplicate payments, import retries, or migrations.
- Propose query improvements with before/after measurements on generated data.

Discuss broad redesigns or substantial new dependencies in an issue before implementing them.
A contribution does not guarantee acceptance or a response deadline.

## Fork and branch

1. Fork the repository and clone your fork.
2. Add the original repository as `upstream`, then branch from its current `main`.
3. Use a descriptive branch such as `fix/month-end-billing` or `feature/import-preview`.
4. Keep unrelated changes in separate pull requests.

```sh
git remote add upstream https://github.com/BrennenGriffith/billvault.git
git fetch upstream
git switch -c fix/describe-the-change upstream/main
```

Use JDK 25 and Node.js 24 for the checks below. Docker is optional for ordinary development.
Never run tests against a personal financial database. Use temporary data directories and fictional
records; never commit credentials, databases, exports, signing keys, or private screenshots.

## Choose the component

| Component | Source and setup | Checks from the repository root |
| --- | --- | --- |
| Web frontend/backend | `frontend/`, `src/`; [local setup](README.md#self-host-the-web-app) | Frontend tests/build, then normal Maven verification |
| JavaFX desktop | `desktop/`, shared `src/`; [desktop guide](desktop/README.md) | Maven verification with the `desktop` profile |
| MCP adapter and import skill | `integrations/`; [MCP guide](integrations/mcp/README.md) | Adapter tests/audit; both Java profiles when shared imports change |
| Documentation | README and `docs/` | Verify links, examples, version claims, and any commands you change |

For local web development on Windows, run `scripts/start-local.ps1` and
`scripts/start-frontend.ps1` in separate PowerShell terminals. They use the documented local
configuration. On Linux/macOS, supply a development ADMIN_PASSWORD in your session environment,
run `./mvnw spring-boot:run -Dspring-boot.run.profiles=local`, then run `npm ci` and `npm run dev`
in `frontend/`. See the README for configuration and data locations.

## Run relevant checks

Web:

```sh
npm ci --prefix frontend
npm test --prefix frontend
npm run build --prefix frontend
./mvnw clean verify
```

Desktop:

```sh
./mvnw -B -Pdesktop clean verify
```

MCP:

```sh
npm ci --prefix integrations/mcp
npm test --prefix integrations/mcp
npm audit --prefix integrations/mcp --omit=dev --audit-level=high
```

On Windows, replace `./mvnw` with `.\mvnw.cmd`; use `npm.cmd` if your PowerShell policy blocks
`npm.ps1`. Run the two Maven profiles sequentially or in separate checkouts because they share
`target/`. A frontend build must exist before normal web verification. CI builds it first.

The automatic [contribution checks](.github/workflows/contribution-checks.yml) run on pull requests
and pushes to `main`. The [additional verification template](docs/ci/README.md) covers PostgreSQL
and Compose separately. CI checks do not deploy the app or use contributor-supplied secrets.

## Protect behavior and records

- Keep recurrence, money, and payment rules in shared services/calculators rather than UI handlers.
- Use decimal arithmetic and report currencies separately. Preserve transactional payment recording,
  occurrence uniqueness, optimistic versions, and retry-safe import receipts.
- Add the next Flyway migration for schema changes; never rewrite an applied migration.
- Test an actual failure case when changing billing, authentication, imports, backups, or recovery.
- Keep database/file work off the JavaFX UI thread and preserve usable native window controls.
- For UI evidence, use fictional records and cursor-free screenshots. Distinguish desktop from web.
- Keep private development context outside shipped source and documentation. Ignore rules are not
  protection for secrets already committed; inspect your diff before pushing.
- Format Java with Google Java Format and follow the existing TypeScript/CSS conventions.

## Submit and review

Use the pull-request template. Explain the concrete problem, resulting behavior, verification,
and any API, migration, backup, or compatibility impact. State what you did not verify.
Link the relevant issue and include screenshots for visible changes.

`main` requires a pull request, verified signatures, and an independent approving review.
Automatic web, desktop, and MCP checks form the contribution baseline. Address review threads
and rerun checks after revisions; new commits can invalidate an earlier approval. Maintainers
merge through the repository's protected-branch workflow. Do not request force-pushes to `main`
or disabling protections to get a change accepted.

Use signed commits when your workflow supports them. GitHub's protected branch still requires
the resulting commit to have a verified signature. Do not share a signing key with the project.
See [GitHub commit-signing documentation](https://docs.github.com/en/authentication/managing-commit-signature-verification).

Report vulnerabilities privately according to [SECURITY.md](SECURITY.md), rather than including
an exploit or private records in a public issue. Be respectful and keep feedback specific.
By contributing, you license your contribution under the repository's MIT license and remain
responsible for correctness, dependency licenses, and the privacy of submitted material.
