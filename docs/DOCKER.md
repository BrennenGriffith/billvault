# Docker tutorial

Run React/nginx, Spring Boot, and PostgreSQL together on your own computer. This does not create an AWS account or paid resources. Only the frontend is published, at **http://127.0.0.1:8088**. This guide assumes a fresh Docker installation; existing H2 records are not automatically imported.

## 1. Install the prerequisites

- Install [Git](https://git-scm.com/downloads).
- On Windows/macOS, install [Docker Desktop](https://docs.docker.com/desktop/) and start it. On Windows, use Linux containers and follow Docker's WSL 2 setup requirements.
- On Linux, install [Docker Engine](https://docs.docker.com/engine/install/) and the [Compose plugin](https://docs.docker.com/compose/install/linux/).

You do not need host Java, Node.js, or PostgreSQL: the Dockerfiles build them in containers. Confirm the engine is running:

```sh
docker version
docker compose version
```

`docker version` must show both Client and Server. Allow several minutes for the first image download and build.

## 2. Get the source

```sh
git clone https://github.com/BrennenGriffith/billvault.git
cd billvault
```

### Windows / PowerShell

```powershell
.\scripts\start-docker.ps1
```

Enter a unique administrator password (12+ characters, at most 72 UTF-8 bytes) and a separate database password (12+ characters). Prompts hide input. The username defaults to `admin`; use `-Username yourname` to change it. The helper builds and starts the stack, waits for health checks, and restores your original shell variables afterward. It does not create `.env` or a credential file.

### Linux / macOS / Bash

Use hidden prompts so secrets do not become commands in shell history:

```bash
(
  read -r -s -p 'Administrator password (12+ characters, max 72 UTF-8 bytes): ' ADMIN_PASSWORD
  printf '\n'
  read -r -s -p 'Database password (12+ characters): ' DB_PASSWORD
  printf '\n'
  export ADMIN_PASSWORD DB_PASSWORD
  export ADMIN_USERNAME=admin
  docker compose up --build -d --wait --wait-timeout 240
)
```

The subshell discards its variables on exit. Use the same database password whenever recreating containers against the existing volume. Keep passwords in your memory or your own password manager; BillVault does not save them in a user credential file.

## Credential handling

**No real credentials belong in this repository, its README, screenshots, issues, or commits.** `.env.example` contains examples only; `.env` and credential files are ignored. The recommended prompts write no credential file and do not print passwords.

Docker passes these values through container environment variables. They remain in **local container metadata** and are accessible to users with Docker access, even after your shell variables are cleared. `docker compose stop` retains containers and that metadata. Removing containers with `docker compose down` removes their active configuration, but this is not a forensic secure-erasure guarantee. PostgreSQL stores an authentication verifier in its data volume. Passwords also exist in process memory while services run.

If your requirement is no saved Docker credential configuration, use the **personal JAR option in the README**, which prompts on every start without a credential file. A future network deployment should use managed secret delivery and HTTPS. Do not share `docker inspect`, `docker compose config`, environment dumps, or unreviewed logs: they may reveal secrets. Git ignore rules do not remove secrets already committed.

## 3. Open and verify

Open **http://127.0.0.1:8088** and sign in using your prompted administrator password.

1. Create a fictional subscription with an amount, category and due date.
2. Record a payment and check that its next due date advances.
3. Open reports and verify payment history.
4. Refresh the page to confirm records persist.

Health and targeted logs:

```sh
docker compose ps
docker compose logs --tail 80 backend
docker compose logs --tail 80 db
```

Run Compose commands from this checkout. The configuration requires both password variables even for some read-only commands. To run commands after the startup helper clears variables, use temporary **non-secret placeholders** for commands that do not create/reconfigure containers:

```powershell
$env:ADMIN_PASSWORD = 'unused-for-status-only'
$env:DB_PASSWORD = 'unused-for-status-only'
docker compose ps
Remove-Item Env:ADMIN_PASSWORD, Env:DB_PASSWORD
```

In Bash, prefix a command with `ADMIN_PASSWORD=unused DB_PASSWORD=unused`. Never run `up`, `create`, or a configuration-changing command with those placeholders. For convenience, keep a separate terminal with the real values supplied through hidden prompts if you need several operational commands, and close it afterward.

## 4. Understand what persists

| Component | Purpose | Storage / access |
| --- | --- | --- |
| frontend | nginx serves React and forwards API requests | Loopback port 8088 |
| backend | Java API, authentication, migrations, billing rules | Internal network only |
| db | PostgreSQL 17 | Internal network; named `postgres_data` volume |

Compose prefixes the volume name with its project name. Keep the same checkout/project name when updating, or specify the same `-p` consistently; changing it can create an apparently empty installation with a different volume. The development override publishes PostgreSQL separately; it is not needed for normal use.

- `docker compose stop`: stops containers; preserves containers and records.
- `docker compose start`: resumes existing containers without changing passwords or code.
- `docker compose down`: removes containers/network; preserves the database volume.
- **`docker compose down -v` deletes the database volume. Do not use it for your records.**

Changing `DB_PASSWORD` does not change an initialized PostgreSQL user's password. A mismatch prevents backend login. Reuse the original database password; deliberate rotation requires changing the PostgreSQL role and backend configuration together. The administrator password is configured at backend startup and can be changed by recreating that container with a new value. Sessions reset on restart.

## 5. Back up and check recovery

Back up before upgrades. Run with the temporary operational variables described above:

```powershell
docker compose exec -T db pg_dump -U billvault -d billvault -Fc -f /tmp/billvault.dump
docker compose cp db:/tmp/billvault.dump "$HOME/billvault-backup.dump"
```

Choose a new filename each time to preserve earlier backups. Store copies outside Git and on another trusted device; dumps contain financial information. Avoid PowerShell binary redirection for database dumps. See [database recovery instructions](DATABASE.md#backup-with-docker-powershell) to restore into a **separate empty database** and verify it before relying on the backup. H2 files and CSV reports are not PostgreSQL backups.

## 6. Update or contribute

1. Make and verify a backup; review the changes you are pulling.
2. Run `git pull --ff-only`.
3. Run the startup helper again (or the Bash prompt block) with the original database password. `--build` rebuilds changed images and Compose recreates affected containers.
4. Verify sign-in, your expenses and payment history. Keep the pre-update backup; migrations can make older versions incompatible with the updated database.

For changes, fork the repository, create a branch, and follow [CONTRIBUTING.md](../CONTRIBUTING.md). See the [architecture guide](ARCHITECTURE.md) for the request flow and application components. The optional Actions template includes a Compose smoke check; it is not active until installed.

## Troubleshooting

| Symptom | What to check |
| --- | --- |
| Cannot connect to Docker daemon | Start Docker Desktop/Engine; check Linux container mode and WSL setup. |
| Port 8088 already in use | Set `APP_PORT=8090` in your shell before startup and browse port 8090. |
| Missing password variable | Use the startup helper/hidden prompts; Compose does not remember shell variables. |
| Database authentication failed | Existing volume needs its original database password, not a new one. |
| Health wait times out | Inspect backend/db logs and available memory/disk; the first build can take time. |
| Empty database after changing folder/project | Check Compose project name and existing volumes; do not delete them. |
| API request returns 401 before login | Expected: sign in through the frontend. |

These settings use local HTTP and are not an Internet deployment configuration. Docker runtime verification requires a working engine; documentation or JAR tests alone do not prove the container stack works.
