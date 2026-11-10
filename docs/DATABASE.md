# Database

Flyway applies versioned migrations from `src/main/resources/db/migration/`.
V1 creates the initial schema; V2 adds durable batch-import receipts; V3 adds mobile command receipts and paired-device token hashes.
Hibernate uses `ddl-auto=validate`; it does not modify the deployed schema.
Never edit an applied migration. Add the next numbered migration for future changes.

| Model / table | Purpose | Relationships / invariants |
| --- | --- | --- |
| mobile_command_receipts | Retry-safe Android commands | Unique UUID, canonical request hash, atomic result receipt |
| mobile_devices | Paired desktop phones | Token hash, pairing time, revocation flag; no plaintext token |
| Category / categories | Display name and color | One category has many expenses; unique name |
| Expense / expenses | Recurring bill/subscription and schedule | Required category; positive decimal amount; status/kind/frequency checks; version and audit timestamps |
| Payment / payments | Actual payment ledger | Many payments per expense; unique (expense_id, occurrence_date) |
| ReminderRule / reminder_rules | Lead time and enabled flag | One rule per expense, 0–90 days |
| Notification / notifications | Delivered in-app reminder and read state | Unique (expense_id, occurrence_date) |
| expense_import_receipts | Retry-safe import results via JDBC | Unique preview UUID; created IDs/skipped count stored atomically with inserted expenses |
| AppSettings / app_settings | Installation currency view, theme and reminder default | One seeded settings row |

Money uses NUMERIC(12,2)/BigDecimal. Currency codes are validated ISO codes. Dates use DATE/LocalDate;
audit fields use TIMESTAMP WITH TIME ZONE/Instant. No real expense/payment data is seeded.
Deleting an expense cascades to its payment history, reminder and notifications. Canceling preserves all records.
Public entity fields use JPA field access for a compact initial model; API validation and transaction boundaries live
outside the entities. Multi-user ownership would require an AppUser table, owner foreign keys and authorization
checks in every expense/payment query; it is intentionally not represented as an incomplete security feature.

## Backup with Docker (PowerShell)

Use container-side files and docker cp to avoid PowerShell binary redirection corruption:

```powershell
docker compose exec -T db pg_dump -U billvault -d billvault -Fc -f /tmp/billvault.dump
docker compose cp db:/tmp/billvault.dump ./billvault-backup.dump
```

Store backups outside the repository and protect them as financial data.
Restore into a separate, newly created empty database first to verify the backup:

```powershell
docker compose cp ./billvault-backup.dump db:/tmp/billvault-restore.dump
docker compose exec -T db createdb -U billvault billvault_restore_check
docker compose exec -T db pg_restore -U billvault -d billvault_restore_check /tmp/billvault-restore.dump
```

These instructions do not drop or overwrite the live database. Replacing live data is a separate destructive
operation that should be reviewed and backed up first. Keep the PostgreSQL major version compatible with the dump tools.

For local H2, stop the backend before copying `data/billvault.mv.db`. H2 and PostgreSQL storage files are not interchangeable;
use application-level export/import or an explicit migration to transfer records between them.
