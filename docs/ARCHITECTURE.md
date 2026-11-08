# Architecture

## Native desktop edition

The optional Maven `desktop` profile adds `desktop/src/main/java` and JavaFX dependencies. `DesktopStore`
starts a narrowly configured Spring context with `WebApplicationType.NONE`, shared services/repositories,
Flyway, and a separate H2 file in the OS user-data directory. It does not scan the web security/controllers
or start an HTTP listener. JavaFX invokes storage work on a single background executor and updates controls
on the JavaFX thread. DTO validation precedes mutations; shared transactions, row locks, optimistic versions,
and duplicate occurrence constraints apply to both editions. A file lock prevents multiple desktop writers.
There is no cloud synchronization, application login, or desktop database encryption. See [desktop guide](../desktop/README.md).

## Web edition

```text
React components → frontend/src/api.ts → same-origin /api
  → Spring Security filter chain (session + CSRF)
  → SubscriptionController / AuthController
  → SubscriptionService (@Transactional)
  → Spring Data repositories
  → PostgreSQL (or explicit local H2 profile)
```

`Expense` replaces the original in-memory Subscription object as the shared persisted domain model.
`ExpenseKind` distinguishes bills and subscriptions. The existing controller/service class names are retained
so the evolution from the original project is easy to follow. `/api/subscriptions` remains an alias, with a
new paginated response contract and richer request fields; old response consumers must migrate.

## Backend packages
- `model`: JPA field-access entities and enums. Associations are eager for this small personal dataset.
- `dto/Api`: immutable request/response records; entities never leave the service as public API responses.
- `repository`: generated JPA queries plus a pessimistic row lock for recording payments.
- `service/BillingCalculator`: deterministic recurrence and normalization rules.
- `service/SubscriptionService`: expense, payment, reporting and preference use cases.
- `config`: Spring Security, centralized error handling and hourly reminder scheduling.

Search, status/kind/category filters, sorting and pagination run in SQL via JPA Specifications and Pageable.
Dashboard calculations and calendar recurrence expansion operate over persisted records inside a transaction.
An independent payment row stores currency/amount at recording time, preserving those values when an expense changes.
The full payment ledger is loaded for the current personal-scale frontend; history pagination is a future optimization.

## Frontend
`App.tsx` owns navigation, authentication state and API loading. `ExpenseForm.tsx` owns expense input state.
`components.tsx` contains shared tables, dialogs, summary cards and charts. `types.ts` describes API contracts.
`api.ts` obtains a CSRF token, submits a form-encoded login, and makes authenticated JSON requests.
No financial records or session credentials are persisted to browser localStorage.
All totals come from API records; empty state is a supported first-run experience.

## Deployment
The daily `personal` profile serves the built React files from the Java JAR on loopback, disables API docs,
and reuses the selected local H2 data directory. Personal startup prompts each run without saving credentials;
the build helper publishes verified builds to versioned `.local/releases/` folders. Offline backup/restore tools
operate on separate files and refuse a live H2 copy or overwriting an existing restored database.

Vite proxies requests in development. In Docker, nginx serves the static React build and proxies requests
to the internal Java service. PostgreSQL runs on an internal network with a persistent volume.
A PostgreSQL health check gates Java startup; Java health gates nginx startup.
The supplied Compose stack binds to loopback and does not publish externally.
