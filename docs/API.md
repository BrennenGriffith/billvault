# API

Base path `/api`. Protected endpoints require a JSESSIONID session cookie. JSON mutation bodies are validated;
DTO definitions in `dto/Api.java` and authenticated `/v3/api-docs` describe the complete field contracts.
Application errors use ProblemDetail (`status`, `title`, `detail`). Security-filter rejections use Spring's security response.

| Method | Path | Purpose |
| --- | --- | --- |
| GET | /auth/csrf | Public token + header name; send cookies on subsequent requests |
| POST | /auth/login | Form encoded username/password + CSRF header; 204 success / 401 invalid |
| GET | /auth/me | Current username |
| POST | /auth/logout | Invalidate session; CSRF required |
| GET | /expenses | SQL-filtered paginated expenses |
| GET | /expenses/{id} | One expense or 404 |
| POST | /expenses | Create expense; 201 |
| PUT | /expenses/{id} | Replace editable fields |
| DELETE | /expenses/{id} | Permanently delete expense and associated history; 204 |
| POST | /imports/preview | Validate 1–50 expenses and return expiring CREATE/SKIP rows; no expenses saved |
| POST | /imports/{id}/commit | Commit reviewed preview with `{"confirmed":true}`; replay returns original receipt |
| GET / POST | /categories | List / create a category |
| GET | /payments | Payment history, newest payment date first |
| POST | /expenses/{id}/payments | Record current occurrence and advance date; 201 / 409 duplicate |
| GET | /dashboard | Per-currency estimates, actual totals, upcoming, overdue, category totals, trends |
| GET | /calendar?from=YYYY-MM-DD&to=YYYY-MM-DD | Expanded projected active occurrences; max one year |
| GET / PUT | /expenses/{id}/reminder | Read / configure daysBefore and enabled |
| GET | /notifications | Notification inbox |
| POST | /notifications/refresh | Generate due reminders; 204 |
| PATCH | /notifications/{id}/read | Mark read; 204 |
| GET / PUT | /settings | Currency view, theme, default reminderDays |
| GET | /reports/summary?year=2026&month=11 | Per-currency actual monthly/yearly payment totals |
| GET | /reports/payments.csv?from=YYYY-MM-DD&to=YYYY-MM-DD | Actual payment CSV for inclusive dates |

List parameters: `search`, `kind=SUBSCRIPTION|BILL`, `status=ACTIVE|PAUSED|CANCELED`, `categoryId`,
`sort=name|amount|nextDueDate`, zero-based `page` (default 0), `size` (default 20, max 100).
Expense PUT requests must include the current response `version`; stale or missing versions return 409 to prevent lost edits.
Amount sorts group by currency first. Response: `{content,page,size,totalElements,totalPages}`.
`/subscriptions` and `/subscriptions/{id}` alias the expense endpoints for migration; they do not automatically
filter kind. The original list's bare-array contract changed to a page object.

Example expense request (replace categoryId with a real category ID):

```json
{
  "name": "Internet", "description": "Home connection", "kind": "BILL", "categoryId": 6,
  "amount": 79.99, "currency": "USD", "frequency": "MONTHLY",
  "customInterval": null, "customUnit": null,
  "startDate": "2026-01-31", "nextDueDate": "2026-11-30", "endDate": null,
  "paymentMethod": "Personal card", "websiteUrl": "https://example.com",
  "status": "ACTIVE", "variableAmount": false, "notes": ""
}
```

Payment body: `{"occurrenceDate":"2026-11-30","paymentDate":"2026-11-08","amount":79.99,"notes":"Paid early"}`.
The occurrence must be the current nextDueDate. Actual paymentDate cannot be in the future. Custom frequency requires
customInterval (1–3650) and customUnit (DAYS/WEEKS/MONTHS/YEARS). Dates and amounts are validated on the server.
CSV quotes text and guards formula-leading cells. No endpoint initiates real payments.

## Batch expense imports

`POST /imports/preview` accepts `{"source":"reviewed summary","expenses":[ExpenseRequest,...]}`.
Source is a short label, not a raw statement. All rows must pass bean and semantic validation.
The result contains `previewId`, `expiresAt`, `source`, and indexed rows with the original expense,
`action` (`CREATE` or `SKIP`), and `reason`. A same-name/currency/kind match is skipped after
case/whitespace normalization. Previews last ten minutes in process memory.

`POST /imports/{previewId}/commit` accepts `{"confirmed":true}` and returns
`{previewId,createdIds,skipped}`. Inserts and their durable receipt commit atomically. Retry
the same ID after a lost response; an already committed ID replays its receipt after restart.
Uncommitted expired, missing, or stale previews return 409 and require a new preview.
Confirmation is a caller assertion; configure the assistant host to obtain user approval.
Both web operations require the existing authenticated session and CSRF header.
See [MCP setup](../integrations/mcp/README.md) for the separate opt-in desktop bridge.
