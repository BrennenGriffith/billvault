# Daily personal use on Windows

This mode serves the built React frontend and REST API from one Java process. No Vite dev server, Docker,
AWS account, or cloud subscription is required. It is intended for one person on a local computer, on loopback only.

## First-time setup

Install JDK 25 and Node.js 24, clone the repository, and open PowerShell in the project folder:

```powershell
.\scripts\build.ps1
.\scripts\start-personal.ps1
```

Build performs npm install/tests/build and Maven clean verify, then stores a versioned runnable JAR under
`.local/releases/`. Each startup prompts for an administrator password; no credential file is created.
The password is supplied to the Java child process in memory and the shell environment is restored on exit.
Use a unique password with at least 12 characters and at most 72 UTF-8 bytes. If PowerShell blocks scripts, review your organization's policy before changing it;
you can also run the documented Java/Maven commands manually.

Open http://127.0.0.1:8080. The terminal stays open while the app runs. Press Ctrl+C and wait for shutdown
before backups or updates. A port-in-use message means another backend is already running; stop it first.
OpenAPI is disabled in this mode. The application runs with a 512 MB Java heap and a small connection pool.

H2 stores your records in `data/billvault.mv.db`. This is the same path used by the original development
profile: it is reused without deleting, reseeding, or migrating your existing records to a new database.
The `personal` profile always binds to 127.0.0.1. Do not expose this local HTTP configuration to the Internet.
Keep the database on a local disk, outside live cloud-sync folders. A local backup on the same disk does not
protect against disk failure; copy verified backups to another trusted device.

## Every day

```powershell
.\scripts\start-personal.ps1
```

No Node server or build is needed until you change the application. Browser sessions expire after inactivity
and are invalidated when the backend restarts. Enter the same password each time, or choose a new one
at the next startup to change it. Authentication is configured at startup; no administrator password is stored in H2.
Remember your password or manage it in your own password manager. BillVault does not save it for you.

## Backup

Stop BillVault first. Then:

```powershell
.\scripts\backup-local.ps1
```

The default destination is `%LOCALAPPDATA%/BillVault/backups`, outside the source repository. The command
prints the backup path and writes a matching `.sha256` file. Keep both together. It acquires an exclusive
read handle and fails if the database is in use. It never deletes old backups; manage retention deliberately.
These files contain financial information and are not encrypted by this backup script.

## Restore into a separate directory and inspect it

```powershell
.\scripts\restore-check.ps1 -BackupFile 'C:/path/to/billvault-YYYYMMDD-HHMMSS-fff.mv.db'
```

The command checks the checksum, creates a separate `.local/restore-check/.../billvault.mv.db`, and refuses
to overwrite any existing database. A matching checksum proves copy integrity, not application compatibility.
To inspect the restored records, stop the live app, set BILLVAULT_DATA_DIR to the printed file's **directory**,
and start the application. For example:

```powershell
$env:BILLVAULT_DATA_DIR = 'C:/absolute/path/to/.local/restore-check/20261108-120000-000'
.\scripts\start-personal.ps1
# Sign in and verify expenses and payment history. Then stop the app.
Remove-Item Env:BILLVAULT_DATA_DIR
```

Restore checking uses the current app's Flyway migrations on the restored copy only. The original backup
and live database remain separate. Replacing live data is intentionally not automated: retain a copy of the
live database and review the verified restored copy before choosing to use it permanently.

## Update

1. Stop BillVault and make a backup.
2. Review release notes/changes and pull the new source.
3. Run `scripts/build.ps1`, then `scripts/start-personal.ps1`.
4. Confirm sign-in, expenses and payment history.

Builds install into a new release directory only after verification passes. They do not overwrite your running
JAR or delete data. Do not downgrade an app against a database that has received newer migrations; recover
the matching pre-upgrade backup into a separate directory if a rollback is needed.

## PostgreSQL and future hosting

Docker Compose runs PostgreSQL with a persistent volume and only publishes the frontend on loopback. See
README and DATABASE.md for setup and pg_dump instructions. H2 backup files cannot be restored into PostgreSQL.
An application-level H2-to-PostgreSQL migration remains future work; CSV reports are not complete database backups.
Before AWS hosting, add HTTPS, server-side secret management, automated off-host backups, restore monitoring,
and login abuse protection. No paid resources are provisioned by this repository's setup scripts.
