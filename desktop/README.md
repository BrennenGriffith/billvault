# BillVault Desktop

A native JavaFX edition of BillVault. Runs entirely on your computer with an embedded H2 database. No hosting, Docker, browser, separate Java installation, or administrator password is required for packaged downloads.

## Download and run

Use the assets under the **Desktop** release at [GitHub Releases](https://github.com/BrennenGriffith/billvault/releases). Download only packages actually listed there; source archives are not runnable desktop packages.

- **Windows x64:** download the portable ZIP, extract the entire folder, and run `BillVaultDesktop.exe`. Keep its `app` and `runtime` folders together. An `.exe` installer may also be supplied; it installs for your Windows user.
- **Linux x86_64:** download the AppImage, run `chmod +x BillVaultDesktop-*.AppImage`, then launch it. A graphical desktop with GTK 3 and compatible system libraries is required. On distributions without FUSE support, use `./BillVaultDesktop-*.AppImage --appimage-extract` and run `squashfs-root/AppRun`. The 0.1.0 package passed a packaged-app smoke test on Ubuntu 22.04 under Xvfb; other distributions have not been individually verified.
- Packages bundle the Java runtime and JavaFX. Windows downloads are unsigned; operating systems may show an unknown-publisher warning. No automatic updater is included.

Compare the download's SHA256 with its adjacent `.sha256` asset (`Get-FileHash` on Windows, `sha256sum` on Linux). Checksums detect corruption; they are not a code signature.

## What it includes

Native overview, paginated/searchable expense tables, bills/subscriptions with all recurrence fields, categories, expense editing/cancellation/deletion, payment history, actual monthly/yearly totals, CSV export, reminders refreshed while using the app, and default currency/reminder preferences. It calls the same transactional Java service layer as the web edition. Currency totals remain separate. Reminders do not run after the app closes; there are no system-tray or email notifications. The desktop appearance is separate from React's theme preferences.

## Your data and credentials

There is no desktop login or stored administrator credential. Access is protected by your operating-system account. The database and exported files are **not encrypted**. Use a private OS account and disk encryption where appropriate.

- Windows: `%LOCALAPPDATA%/BillVaultDesktop/billvault.mv.db`
- Linux: `${XDG_DATA_HOME:-$HOME/.local/share}/billvault-desktop/billvault.mv.db`
- macOS source builds: `~/Library/Application Support/BillVaultDesktop/billvault.mv.db` (no macOS release package yet).

The web edition's database is separate. There is no automatic sync or complete cross-edition import. The desktop edition opens no HTTP port and needs no cloud credentials. Do not point it at a web database while that database is running. Only one desktop process may open a vault at a time.

## Back up, restore, and update

Close all BillVault Desktop windows and wait for the process to exit, then copy `billvault.mv.db` to a safe location outside the repository and preferably another device. A backup contains your financial records. Test restoration using a separate directory and `BillVaultDesktop.exe --data-dir=C:/path/to/restored-copy` (or the equivalent Linux executable argument). Never overwrite your current file without preserving a copy first.

To update, back up first, close the app, and replace the application folder/install the new version. Data stays in the OS data directory. Flyway applies migrations on startup. Older versions may not open a newer schema; keep the pre-update backup. Uninstalling the application does not automatically remove your data.

## Build from source

Install a redistributable JDK 25+ (such as Eclipse Temurin) and use the Maven wrapper. The desktop profile needs no Node.js or React build; it uses native controls and does not bundle the web frontend. It runs shared billing/API tests plus desktop persistence tests, excluding the web frontend serving test. For a full web-edition verification, follow the root README separately.

```powershell
.\scripts\build-desktop.ps1 -Version 0.1.0
# Optional installer: requires a WiX toolset supported by your JDK
.\scripts\build-desktop.ps1 -Version 0.1.0 -Installer
```

For Linux, use a Linux x86_64 build machine with JDK 25+, GTK 3, packaging tools, and a verified [appimagetool](https://github.com/AppImage/appimagetool):

```sh
chmod +x mvnw scripts/build-desktop-linux.sh
APPIMAGETOOL=/absolute/path/to/appimagetool ./scripts/build-desktop-linux.sh 0.1.0
```

Build packages on their target OS. Windows jpackage cannot produce a Linux runtime. Output is under `target/desktop-release/`, excluded from Git. The [Linux desktop release workflow](https://github.com/BrennenGriffith/billvault/actions/workflows/desktop-release.yml) is active in this repository; `docs/ci/desktop-release.yml` is the matching template for forks. It checks out the existing desktop release tag and adds an AppImage only after tests and a packaged-app smoke test pass. Never package databases, backups, `.env`, or credentials as application resources.

## Desktop architecture

`DesktopLauncher` → JavaFX `DesktopApplication` → non-web `DesktopStore` → shared `SubscriptionService` → repositories/H2. UI storage work runs on one worker thread; JavaFX changes run on its application thread. Request records are validated before writes, and the shared service retains recurrence, transaction, duplicate-payment, and stale-edit protection. The process lock prevents two desktop windows from writing the same vault.

Source and project artwork are MIT licensed. Java, JavaFX, and dependencies retain their own licenses; package notices accompany releases. This is a first desktop release, not full visual parity with the web app.
