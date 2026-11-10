# Android roadmap

BillVault Android has an initial native source preview under [android/](../android/README.md).
It is not on Google Play and does not yet have complete desktop/web feature parity.

## Available in the source preview

- First-run choices: self-hosted BillVault, paired standalone desktop, or phone-only vault.
- Separate profiles/caches, with no silent cross-vault merges.
- Expense creation/editing, recurrence fields, upcoming/overdue review, payments, actual totals, and CSV.
- App-private SQLite storage, explicit JSON backup/restore, and encrypted connection secrets.
- Manual Sync with an outbox, version conflicts, and atomic server receipts for safe retries.
- Optional desktop TLS listener, one-time pairing, exact certificate pinning, and device revocation.

The app uses Kotlin/Compose, ViewModels, coroutines, SQLiteOpenHelper, and OkHttp. It does not run
Spring Boot, JavaFX, or H2 on Android. Desktop and self-hosted services share the mobile sync contract.
Money uses the existing decimal/two-decimal model; currencies stay separate.

## Next milestones

- [ ] Complete emulator/physical-device checks: onboarding, rotation, large text, airplane mode, conflicts, and recovery.
- [ ] Add persistent form drafts and per-command conflict review.
- [ ] Add category management, projected calendar, and notification/reminder scheduling.
- [ ] Verify permission-aware reminders, reboot/timezone handling, and realistic delivery expectations.
- [ ] Add optional local lock and review backup/privacy behavior on target devices.
- [ ] Validate sync against real HTTPS self-hosted installations and Windows/Linux packages.
- [ ] Complete compatibility/security review before signed beta distribution.
- [ ] Configure signing/recovery outside Git, verify current Play requirements, and prepare actual Android screenshots.
- [ ] Establish Android release artifacts, beta testing, store listing, and aggregate acquisition reporting.

No cloud account, paid relay, or developer-hosted server is required for local/desktop use. Self-hosted
users provide their own reachable installation. Background synchronization, Android agent controls,
bank linking, automatic transaction discovery, and direct ChatGPT web connections are not included.
The in-progress desktop/web agent-refresh work is separate from this Android milestone.

Core tracking is intended to stay free. Optional support must not unlock core functionality or receive
financial records; review current store rules before adding support payment links.
