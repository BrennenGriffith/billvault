# Android roadmap

BillVault Android is a planned native companion to the desktop and web editions. Android implementation has not started.

## Product goals

- Track recurring bills and subscriptions without an account or bank connection.
- Show upcoming obligations before a selected payday, with currencies reported separately.
- Store records locally and provide complete backup, restore, and CSV export.
- Keep core functionality free, with optional support for development.

## Delivery status

| Area | Status |
| --- | --- |
| Android application | Planned |
| Google Play distribution | Not configured |
| Optional funding page | Not configured |

An Android release will have its own store listing and artifacts. Existing Windows and Linux packages are desktop applications.

## First-release scope

| Feature | Acceptance criteria |
| --- | --- |
| Onboarding | Create the first expense offline without signup or a support request. |
| Expense management | Create, edit, search, categorize, pause, and cancel bills and subscriptions. |
| Recurrence | Weekly, monthly, quarterly, yearly, and custom schedules; month-end and leap-year behavior consistent with the existing billing rules. |
| Payments | Record actual amounts and dates, prevent duplicate occurrences, and advance the next due date in one transaction. |
| Payday planning | Configure a payday and cadence; summarize overdue and upcoming obligations through that date. |
| Local reminders | Permission-aware due-date notifications with duplicate protection and reboot, update, and timezone handling. |
| Reports | Payment history, actual monthly/yearly totals, and CSV export. |
| Recovery | Versioned full backup and validated restore through the Android file picker. |
| Accessibility | Screen-reader labels, large text, keyboard navigation where applicable, and light/dark themes. |
| Privacy | App-private storage, optional device-authentication lock, and no financial records in logs or marketing SDKs. |

Payday totals represent scheduled obligations, not bank balances or confirmed income. Payment recording does not transfer money.
Manual entry is the initial workflow. Templates and expense duplication can reduce setup time.

Bank linking, automatic discovery/cancellation, cloud sync, shared accounts, and paid feature tiers are outside the first release.
The Android database will be separate from existing desktop and web databases until an explicit import or sync feature is available.

## Architecture

- Independent Gradle project under `android/`; existing Maven web and desktop builds remain separate.
- Kotlin, Jetpack Compose/Material 3, ViewModels, coroutines/Flow, repository interfaces, and Room/SQLite.
- Minimum supported Android version, target SDK, and dependency versions selected during project setup and checked against submission requirements.
- Platform-independent billing rules validated using common recurrence and money fixtures. Spring Boot, JPA, H2, and JavaFX are not Android runtime dependencies.
- Money stored in integer minor units with currency metadata; decimal arithmetic for normalization and currency-aware precision. No floating-point monetary amounts or implicit currency conversion.
- Local calendar dates for billing schedules and UTC timestamps for audit fields.
- Room models: Expense, Category, Payment, ReminderRule, ReminderDelivery, AppSettings, and PaydaySchedule.
- Foreign keys, expense versions, occurrence uniqueness, schema migrations, and transactional payment/date updates.
- Due-date notification scheduling designed around Android background restrictions. Exact-to-the-minute delivery is not a release guarantee.
- App-private data storage with an explicit automatic-backup policy. Device-authentication locks and database encryption are separate capabilities.
- External source/support pages opened only through deliberate browser actions. Core tracking does not require network access.
- Signing keys and service credentials kept outside source control, with documented private recovery procedures.

## Milestones

### 1. Foundation

- [ ] Create the Android project, CI build, and Android Studio setup guide.
- [ ] Implement Room models, migrations, repositories, and isolated fixtures.
- [ ] Port billing behavior and verify parity with shared recurrence/money cases.
- [ ] Test persistence, duplicate-payment protection, and database upgrades.

Exit criteria: repeatable builds and correct offline data behavior on an emulator and a physical device.

### 2. Core application

- [ ] Build overview, expense editor/list, categories, payments, and payday summary.
- [ ] Implement local reminders and notification-permission onboarding.
- [ ] Add backup/restore, CSV export, local lock, and privacy/help screens.
- [ ] Verify accessibility, empty/error states, and activity/process recreation.

Exit criteria: a tester can add bills, record a payment, receive a reminder, export a backup, and restore it into a clean installation.

### 3. Beta and store preparation

- [ ] Configure the Play developer account, application identifier, public contact, launch regions, and signing strategy.
- [ ] Build a signed Android App Bundle and establish APK distribution/signing compatibility for GitHub releases.
- [ ] Create actual Android screenshots with fictional data, an icon, feature graphic, and store description.
- [ ] Complete privacy, Data safety, content-rating, app-access, audience, and applicable financial-feature declarations.
- [ ] Recruit testers and complete the closed-testing process required for the developer account.
- [ ] Test denied notifications, reboot, timezone/DST changes, battery restrictions, corrupted imports, duplicate taps, upgrades, airplane mode, and screen readers.
- [ ] Resolve data-loss, calculation, startup, reminder, and recovery defects before release.

### 4. Public release

- [ ] Publish the approved free app on Google Play and link the store listing from the project documentation.
- [ ] Publish separate Android release artifacts, checksums, and changelog.
- [ ] Configure the optional funding page and review the support integration against current store requirements.
- [ ] Establish acquisition and stability baselines before paid promotion.

### Later releases

- Home-screen widget for upcoming bills.
- Trial endings, cancellation deadlines/links, and price-change history.
- Optional self-hosted synchronization with an independent security and conflict-resolution design.

## Optional support

Development support is voluntary and does not unlock functionality, exports, reminders, or higher limits.
Support links belong in About/Settings and project documentation. Any invitation should be dismissible and outside the primary workflow.
Payment pages must not receive expense information or application identifiers.

Buy Me a Coffee is the proposed funding provider; no page is currently configured. Its integration requires review against Google Play's
current payments rules. The published tipping exception has conditions concerning recipient proceeds and digital benefits, while the
provider charges a platform fee. Eligibility must be established before an external payment link ships in the Android app.
Website/GitHub support and any permitted in-app support mechanism can be configured separately.

## Store presentation and measurement

Working title: **BillVault: Bills & Reminders**.

Working description: **Plan recurring bills, track payments, and get reminders. No bank login.**

Store screenshots should demonstrate payday planning, subscription entry, reminders, payment history, and recovery using the Android UI.
Privacy and functionality claims must match the released build.

Use Play Console's aggregate acquisition, listing-performance, and stability reports. Public milestones should identify the metric,
platform, and reporting date. GitHub artifact downloads, reinstall events, and unique Play acquisitions are separate measures.
Activation and retention require appropriate aggregate reporting or voluntary beta feedback; acquisition counts alone do not establish either.
Developer-console credentials must never be exposed through a public counter or client application.

## Acquisition experiments

Begin with the project website, source repository, product demos, and communities that permit project promotion.
Collect beta feedback on setup and repeat usefulness before paid campaigns.

Paid experiments require a defined audience, one channel, a total spending cap, an end date, and attribution checks.
Record spend, listing traffic, attributable acquisitions, cost per acquisition, stability, and feedback. Ad clicks are not installs.
Review channel minimums and budget-overdelivery rules before launch; daily budgets are not necessarily total spending caps.
Stop an experiment at its cap or when attribution, product stability, or audience fit is inadequate.

## References

Policy references were last reviewed November 8, 2026. Recheck requirements during implementation and store submission.

- [Android architecture](https://developer.android.com/topic/architecture/recommendations)
- [Android alarm scheduling](https://developer.android.com/develop/background-work/services/alarms)
- [Play developer registration](https://support.google.com/googleplay/android-developer/answer/6112435)
- [Play closed testing](https://support.google.com/googleplay/android-developer/answer/14151465)
- [Play Data safety](https://support.google.com/googleplay/android-developer/answer/10787469)
- [Play payments](https://support.google.com/googleplay/android-developer/answer/10281818)
- [Buy Me a Coffee fees](https://buymeacoffee.com/faq)
- [Play acquisition reporting](https://google.play/business/acquisitionreporting/)
- [App campaign budgets](https://support.google.com/google-ads/answer/6167156)
- [Advertising spending limits](https://support.google.com/google-ads/answer/10486637)
