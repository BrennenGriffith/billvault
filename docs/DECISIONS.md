# Decisions

## Shared recurring expense
Bills and subscriptions share dates, money, category, status and payment history. One Expense entity with
an ExpenseKind enum prevents duplicate services and schemas. A variableAmount flag expresses estimates;
every payment records its own actual amount, including fixed subscriptions if an adjustment is needed.

## Recurrence
The start date supplies the original day/month anchor. A January 31 monthly expense becomes February 28/29,
then March 31. A yearly February 29 expense becomes February 28 in non-leap years and returns to February 29
in a leap year. Quarterly/custom-month schedules retain the original day. Weekly/day schedules use real calendar
arithmetic. Paused/canceled expenses are excluded from forecasts and cannot be marked paid. Pausing does not
silently move the due date; edit it when resuming if needed. Recording an overdue payment advances exactly one
occurrence, preserving unpaid intervening occurrences. A final payment whose next occurrence exceeds endDate
marks the expense canceled. The calendar expands projected occurrences; the dashboard's upcoming lists show
the next unpaid occurrence per expense. Calendar queries are limited to one year and 20,000 recurrence steps.

## Estimates vs actual payments
Monthly normalized costs: weekly ×52/12, monthly ×1, quarterly /3, yearly /12. Custom days use 365.2425/12/days;
custom weeks use 52/12/weeks; custom months and years divide by their interval. These are documented estimates,
not predictions of how many charges fall in a particular calendar month. Annual estimates equal the unrounded
monthly estimate ×12. Calculate in BigDecimal and round displayed totals HALF_UP to two decimal places.
Actual cash flow groups Payment.paymentDate by calendar month/year. Report totals are calculated on the server in BigDecimal; the frontend only formats them. Ended expenses with unpaid occurrences remain visible as overdue but are excluded from ongoing projected costs. Currencies stay separate; there is no FX conversion.
Changing a category/name affects current presentation of historical payments; the payment's amount/currency/date remain stored.
The installation currently uses the server's local calendar date, with UTC audit timestamps; per-user timezones are future work.

## Payment concurrency
A database row lock serializes payment submissions for an expense. A database unique constraint on expense+occurrence
is the final protection. A repeat returns 409 Conflict, with no second payment or date advance. Expense changes and payment
recording share a transaction. A version column also enables Hibernate optimistic locking. PUT requests must include the version read by the client; stale edits return 409 instead of silently replacing another tab's changes.

## Authentication
One administrator is configured through environment variables. The password is hashed with BCrypt at startup and is
never returned through the API. Sessions avoid storing bearer tokens in the browser; JSESSIONID is HttpOnly/SameSite=Strict.
All non-read requests, including login/logout, require Spring's session CSRF token. The frontend obtains an encoded token
from `/api/auth/csrf`, submits it in the advertised header, and refreshes it after login. See the official
[Spring Security CSRF reference](https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html).
Default binding is loopback; Docker exposes only its frontend on loopback. Configure TLS and COOKIE_SECURE before a network deployment.
No permissive CORS is needed because both dev and Docker use a same-origin proxy. Login throttling/recovery and durable sessions
are future hardening work. Restarting the app invalidates sessions. Environment secrets are excluded from source control.

## Database and migrations
PostgreSQL is the deployment database; local H2 is an explicit convenience profile. Flyway owns schema changes, with
Hibernate validation to detect mismatches. Boot 4 uses its Flyway starter and the PostgreSQL database module; see
[Spring Boot database initialization](https://docs.spring.io/spring-boot/how-to/data-initialization.html).
The schema intentionally uses portable SQL, but H2 tests alone are not proof of PostgreSQL compatibility. CI runs the integration
suite against PostgreSQL. Docker/PostgreSQL runtime verification must be reported separately from H2 success.

## Reminders
One reminder event is delivered per expense occurrence. Editing its lead time does not create a second event for that occurrence.
An hourly scheduler and manual refresh populate the notification inbox. Read state is persisted. The current implementation is
single-instance; multiple backend replicas need a distributed lock/outbox and retry handling before email delivery is added.


## Local personal packaging and credentials
The built React assets are included in the executable Java JAR, so daily use needs one local Java process.
The personal profile reuses the existing H2 data directory, forces loopback binding, and disables Swagger.
Personal startup prompts each run and temporarily supplies the password to the Java child process.
No credential file is written; the original shell environment is restored when Java exits.
Passwords longer than BCrypt's 72-byte input limit and unchanged example passwords are rejected.
Fonts are bundled locally instead of fetching Google Fonts. The served application sets a same-origin CSP.
Offline backups acquire an exclusive H2 file handle and include a SHA256 checksum; restore checks create a separate copy.
This is a personal local deployment target, not a claim of an audited or highly available Internet service.
