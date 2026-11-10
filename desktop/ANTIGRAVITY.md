# BillVault Desktop development guide

This file describes the native JavaFX application, its shared code, and the workflow for desktop changes.
Keep it current when changing architecture, packaging, or supported behavior.

## Project root and entry point

Open the **repository root**, not only the `desktop/` directory:

```text
billvault/
  pom.xml
  mvnw / mvnw.cmd
  src/main/java/com/brennensubs/billvault/     Shared services, models, DTOs, repositories
  src/main/resources/db/migration/           Flyway schema migrations
  desktop/src/main/java/com/brennensubs/billvault/desktop/
  desktop/src/main/resources/com/brennensubs/billvault/desktop/
  desktop/src/test/java/com/brennensubs/billvault/desktop/
  desktop/packaging/
```

The desktop application is an optional Maven profile in the root `pom.xml`; there is no separate desktop POM.
Use JDK 25+ and activate Maven profile `desktop`. Node.js, PostgreSQL, and Docker are not required for desktop builds.

Main class: `com.brennensubs.billvault.desktop.DesktopLauncher`.

In Eclipse, import the repository as an existing Maven project and enable the `desktop` profile before running the launcher.

## Components

| Component | Responsibility |
| --- | --- |
| `DesktopLauncher` | Starts JavaFX through a plain Java main class. |
| `DesktopApplication` | Native controls, dialogs, navigation, UI state, and background storage jobs. |
| `DesktopWindowSizing` | Centers the complete native frame inside the screen's usable bounds, including small/DPI-scaled screens. |
| `DesktopStore` | Non-web Spring context, H2 configuration, validation, and process lock. |
| Shared `SubscriptionService` | Expense, payment, report, reminder, and settings transactions. |
| Shared `BillingCalculator` | Recurrence and normalized-cost calculations. |
| `desktop.css` | JavaFX styling; React styling does not affect this application. |
| `packaging/billvault.svg`, `.png`, `.ico` | Canonical vector design and native packaging exports. |

The desktop uses shared domain services directly. It does not start Tomcat, expose HTTP endpoints, load React, or require an application login.
Desktop, web, and planned Android databases are separate; there is no automatic synchronization.

## Editing rules

- Keep billing behavior in shared services/calculators rather than duplicating it in JavaFX event handlers.
- Validate DTOs before mutations and retain transaction, duplicate-occurrence, and optimistic-version protections.
- Use decimal arithmetic for money and keep currencies separate.
- Run database/file work on the background executor; update JavaFX controls on its application thread.
- Preserve native minimize, maximize, close, and resize controls. Use JavaFX logical coordinates and screen **visual** bounds;
  do not hard-code a content size that pushes the decorated window outside the desktop or force a global DPI scale.
- Allow minimum sizes to shrink for small screens; wrap or scroll controls rather than making the frame larger than the available desktop.
- Keep data outside installation folders. Packaging and application updates must not delete or reset financial records.
- Add new Flyway migrations for schema changes. Do not edit an already-applied migration.
- Keep artwork exports synchronized and bundle the window icon as a classpath resource.
- Format Java using Google Java Format. Keep changes focused and document user-visible behavior.

## Data safety

Default database locations:

- Windows: `%LOCALAPPDATA%/BillVaultDesktop/billvault.mv.db`
- Linux: `${XDG_DATA_HOME:-$HOME/.local/share}/billvault-desktop/billvault.mv.db`

Use a fresh, isolated directory for testing. Never seed, reset, copy while open, or run destructive tests against a personal vault.
The app accepts `--data-dir=<directory>` for a separate vault. Close the app before copying an H2 database or restoring a backup.
The process lock prevents multiple desktop writers. Storage is not encrypted; access relies on the OS account.
Do not commit databases, exports, passwords, signing keys, or local tool artifacts.

## Build and test

From the repository root:

```powershell
.\mvnw.cmd -B -Pdesktop clean verify
java --enable-native-access=ALL-UNNAMED --sun-misc-unsafe-memory-access=allow -jar target/desktop-input/billvault-desktop.jar
```

On Linux/macOS use `./mvnw` instead of `mvnw.cmd`. The JAR uses neighboring runtime dependencies in
`target/desktop-input/lib/`; keep that directory together with the JAR.

The desktop profile runs shared billing/API tests and desktop tests. It excludes the web frontend-serving test because React assets
are not part of this build. Changes to shared code also need the normal web verification described in the root README.

For a packaged startup smoke test, use an isolated data directory:

```powershell
.\BillVaultDesktop.exe "--data-dir=C:/path/to/temporary-vault" --smoke-test=true
```

Smoke mode loads the native UI/database, verifies visible decorated window bounds, and exits automatically. It is for automated verification,
not ordinary launches. The Linux release workflow also checks startup on 1024×600 and 800×600 virtual displays.

Manual window checks: launch at normal OS scaling, confirm the entire title bar is visible, minimize/restore, maximize/restore,
resize the window, and close cleanly. Verify that expense actions remain reachable when the toolbar wraps.

## Native packages

Windows (redistributable JDK such as Temurin; WiX required for the installer):

```powershell
.\scripts\build-desktop.ps1 -Version 0.1.2 -Installer
```

Linux x86_64 (JDK, GTK libraries, and verified appimagetool):

```sh
APPIMAGETOOL=/absolute/path/to/appimagetool ./scripts/build-desktop-linux.sh 0.1.2
```

Output: `target/desktop-release/`. Windows produces a portable ZIP and optional EXE installer; Linux produces an AppImage.
Packages include Java, application/dependency licenses, and checksums. Build on the target OS. Windows packages are currently unsigned.
Close running packaged apps before rebuilding; Windows may lock their executables/runtime files. An isolated checkout can avoid disturbing a running installation.

## Release workflow

1. Verify the fix, regression tests, and native startup behavior with isolated data.
2. Select a new desktop version; update packaging defaults and current documentation consistently.
3. Commit the source and create a `desktop-v<version>` tag. Keep published release tags stable.
4. Build Windows packages and create a draft GitHub release with the artifacts and checksum files.
5. Dispatch `.github/workflows/desktop-release.yml` with the same version. It checks out the existing tag, tests/builds the Linux package,
   smoke-tests it, and uploads Linux artifacts to that release.
6. Verify all artifacts and publish the release. Update the root README download links after the packages are available.

Use release notes to describe the trigger, corrected behavior, and verification. Keep web/desktop release labels distinct from Android work.
Do not publish personal data or local test artifacts as release assets.

## Related documentation

- [Desktop user guide](README.md)
- [Repository setup](../README.md)
- [Architecture](../docs/ARCHITECTURE.md)
- [Billing decisions](../docs/DECISIONS.md)
- [Database schema](../docs/DATABASE.md)
- [Contribution workflow](../CONTRIBUTING.md)
