# BillVault implementation plan

## Audit
The original app has two GET endpoints backed by a service-owned ArrayList.
Creation is incorrectly annotated in the service and is not exposed by the controller.
Missing IDs throw through Optional.get(). No database, frontend, authentication, or validation exists.
The original Subscription class is a plain Java object, not a persisted entity.

## Build sequence
1. Persist recurring expenses, categories, payments, reminder rules, notifications, and settings using JPA and Flyway.
2. Add validated DTOs, CRUD, filtering, error responses, billing calculations and duplicate-safe payments.
3. Protect the API using administrator session authentication and CSRF tokens.
4. Build React/TypeScript dashboard, expense management, calendar, payment history, reports and settings.
5. Add Docker Compose, tests, CI and learning documentation. Verify builds and actual API workflows.

## Scope decisions
Bills and subscriptions share an Expense entity differentiated by kind. One configured administrator
owns this installation; multi-user account ownership is a future migration, not a claimed feature.
PostgreSQL is the deployment database. An explicit local profile uses persistent H2 for development
when PostgreSQL/Docker is unavailable. No production expenses are seeded.
No cloud deployment or payment processing is included.


## Completed implementation
- [x] Replace in-memory storage with JPA entities, repositories, Flyway migrations and PostgreSQL configuration.
- [x] Add SQL-backed expense filters/pagination, validated request/response DTOs and error handling.
- [x] Add recurrence rules, transactionally recorded payments, uniqueness constraints, calendar and reports.
- [x] Add single-administrator session authentication, BCrypt and CSRF protection.
- [x] Build the React/TypeScript dashboard and all seven main screens, responsive styling and themes.
- [x] Add in-app reminder rules, hourly scheduling, notifications, CSV export and category/preferences forms.
- [x] Add Dockerfiles, Compose with persistent PostgreSQL, dev override, health checks and GitLab pipeline.
- [x] Write setup, architecture, schema, API, decision and developer documentation.

## Verified on November 8, 2026
- Maven verify: 10 backend tests passed on H2, including live HTTP login/CSRF/logout and database-backed workflows.
- The same 10 backend tests passed against isolated PostgreSQL 17.11 on loopback port 55432, including Flyway migration and Hibernate validation.
- Four frontend tests passed; TypeScript and Vite production build passed.
- npm audit: zero reported vulnerabilities after upgrading Vitest.
- Browser on local Vite/Spring Boot: signed in, created a test expense, verified projected totals, recorded a different actual amount and verified due-date advance.
- Removed only the temporary browser QA record and its payment; the application starts with no expense data.
- Browser checks covered both the narrow panel layout and the final desktop dashboard. Final preview screenshot: `.local/billvault-preview.png`.

## Not verified / deferred
- Docker image builds and Compose runtime: Docker is not installed on this PC.
- GitLab CI pipeline execution and registry publishing: configuration is supplied, no remote repository/pipeline was created.
- Multi-user accounts, email notifications, cloud infrastructure and external deployment are not included.
- Larger datasets need payment-history pagination and database aggregate queries. Multi-instance reminders need coordination.


## Local operations and public-source preparation — November 8, 2026
- Added a one-process personal profile serving the bundled React production build, with loopback binding and disabled Swagger.
- Added Windows DPAPI credential setup/start/password-change helpers and versioned personal build artifacts.
- Added exclusive-handle offline H2 backups, SHA256 checksums, and separate-copy restore checks.
- Added API version checking to reject stale expense edits; consistent security-filter errors and a same-origin CSP.
- Bundled fonts locally, removing the application's external Google Fonts requests.
- Added public README artwork and fictional-data screenshots, MIT licensing, contribution/security guidelines, issue/PR templates, and an opt-in GitHub Actions workflow template.
- The workflow template in docs/ci covers frontend checks, H2 and PostgreSQL tests, and a disposable Docker Compose smoke test; it is not active until installed.
- Cloud hosting and paid AWS provisioning remain deferred. The personal profile is a private local deployment target.


### Local verification for this revision
- 12 backend tests passed, including real HTTP authentication/security errors, stale-edit protection and the personal profile's selected database directory/static frontend.
- Integration coverage passed against both H2 and PostgreSQL 17.11. Four frontend tests and TypeScript/Vite production builds passed; npm audit reported zero vulnerabilities.
- Backup while H2 was open was rejected. An offline backup and separate restored copy had matching SHA256 checksums.
- Existing restore targets and deliberately corrupted checksum files were rejected.
- The real personal startup helper successfully decrypted an isolated Windows DPAPI credential and opened a restored fictional database containing 8 expenses and 42 payments.
- Public screenshots were captured from the isolated demo. Personal data, credentials, JARs, dependencies, logs and local artifacts were checked out of the staged source.
- Docker runtime verification is still pending because this PC has no Docker installation; the opt-in GitHub workflow provides that check. Cloud deployment remains deferred.

## Credential handling update

Personal startup now prompts each run without saving credentials. The earlier DPAPI helpers and disposable credential test fixture were removed. Docker usage and its local metadata limitations are documented in DOCKER.md.
