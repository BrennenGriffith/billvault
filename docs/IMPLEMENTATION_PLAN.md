# Implementation status

## Available editions

| Edition | Interface | Storage | Status |
| --- | --- | --- | --- |
| Web | React/TypeScript and Spring Boot API | PostgreSQL or local H2 | Available |
| Desktop | Native JavaFX and embedded services | Separate local H2 database | 0.3.0 preview; source adds Android pairing |
| Android | Kotlin/Compose interface | App-private SQLite cache/local vault | [Initial source preview](../android/README.md) |

## Implemented capabilities

- Shared bill/subscription expense model, categories, payments, reminders, notifications, and settings.
- Flyway schema migrations, JPA repositories, request validation, and database constraints.
- Expense filtering/pagination, recurrence calculations, actual-payment history, and currency-separated reports.
- Transactional payment recording with row locks, duplicate-occurrence protection, and stale-edit conflicts.
- Web administrator sessions, BCrypt hashing, CSRF protection, and consistent API errors.
- React dashboard, expense management, calendar, reports, CSV exports, themes, and local fonts.
- JavaFX overview, expense management, payment history, reports, reminders, and preferences.
- Personal web packaging with loopback binding, bundled frontend assets, and password entry at startup without credential files.
- Offline H2 backup/checksum tools and restore checks into a separate directory.
- Docker Compose configuration with PostgreSQL persistence and internal backend/database networking.
- Windows desktop installer/portable packages and Linux AppImage release builds.

## Verification

Desktop 0.1.0 release verification included 13 shared/desktop tests on Windows and Linux, packaged startup/database smoke tests,
and a public Windows ZIP checksum/extraction check. The web edition passed 12 backend tests in a clean checkout.
Earlier frontend verification covered four tests and a TypeScript/Vite production build; PostgreSQL integration checks covered
Flyway, Hibernate schema validation, authentication, and database workflows. These results describe those revisions, not future builds.

The active [desktop workflow](../.github/workflows/desktop-release.yml) builds and smoke-tests Linux packages from an existing desktop tag.
The broader web/PostgreSQL/Compose [verification template](ci/README.md) is opt-in. Docker configuration does not imply a locally verified
container deployment. Public screenshots use isolated fictional records; no production expenses are seeded.

## Current limitations

- Web installations have one configured administrator; no multi-user ownership isolation.
- Desktop access relies on the OS account; its database is not encrypted.
- Source preview supports manual Android/host sync; no automatic cross-vault sync or complete H2-to-PostgreSQL migration utility.
- Web sessions are in memory, and reminder scheduling targets one backend instance.
- Payment history and reporting need further pagination/query optimization for large datasets.
- No bank connections, real payment processing, email delivery, managed cloud deployment, or Terraform provisioning.
- Windows desktop packages are unsigned; the desktop release is a preview.

See [architecture](ARCHITECTURE.md), [decisions](DECISIONS.md), and the [Android roadmap](ANDROID_PLAN.md) for implementation details and planned work.
