# Contributing to BillVault

For the native JavaFX edition, see [desktop source/build instructions](desktop/README.md).
Run `./mvnw -Pdesktop clean verify` (Windows: `mvnw.cmd`) to check desktop persistence and shared logic.
Keep web and desktop behavior consistent by implementing billing rules in the shared service/calculator.
Use isolated temporary data directories for desktop testing; never package personal databases in releases.

Thanks for helping make recurring expenses easier to understand. Small, focused contributions are welcome.
BillVault is a single-user bill and subscription management application. Review the [architecture](docs/ARCHITECTURE.md)
and [decisions](docs/DECISIONS.md) before proposing broad redesigns.

## Development setup
1. Fork this repository and clone your fork.
2. Install JDK 25 and Node.js 24 (Docker Desktop is optional).
3. Run `scripts/start-local.ps1` and `scripts/start-frontend.ps1` in separate PowerShell terminals.
4. Use fictional data only. Never point tests at your own financial database.
5. Create a feature branch such as `fix/month-end-billing` and keep the change focused.

On Linux/macOS, set ADMIN_PASSWORD in your shell, run `./mvnw spring-boot:run -Dspring-boot.run.profiles=local`,
and run `npm ci && npm run dev` inside frontend. Never paste a real password into an issue or pull request.

## Before submitting

```powershell
npm.cmd --prefix frontend ci
npm.cmd --prefix frontend test
npm.cmd --prefix frontend run build
.\mvnw.cmd verify
```

The opt-in CI configuration also runs the backend suite with PostgreSQL and smoke-tests Docker Compose. If you change money, billing dates,
payment deduplication, authentication, or backup behavior, add a test for the actual failure case.
For an existing database, add a new Flyway migration; do not rewrite an already-applied migration.
Format TypeScript/CSS consistently with the existing files and Java using Google Java Format.

## Opening a pull request
Explain the requirement, final behavior, tradeoffs and verification. Include screenshots for visible UI changes,
using fictional data and no private records. Ask in an issue before large dependency additions or architecture changes.
Maintainers may request revisions; a submitted contribution is not a promise of acceptance or a response deadline.

## Good first contributions
- Accessible keyboard navigation and responsive layout improvements.
- Focused edge-case tests for recurrence and payments.
- Payment-history pagination and measured query optimizations.
- Clearer setup/troubleshooting documentation across operating systems.

Use the issue templates for bugs and features. Report security vulnerabilities privately as described in SECURITY.md.
By contributing, you agree to license your contribution under this repository's MIT license. Be respectful,
assume good intent, and keep technical feedback specific and constructive.

## Contribution quality

Review submitted code, verify its behavior, and document relevant design decisions and test results.
Contributors are responsible for correctness, dependency licensing, and keeping private data out of changes.
