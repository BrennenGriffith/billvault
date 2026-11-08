# BillVault Android: free app, optional support

Status: planning only; Android implementation has not started. Updated November 8, 2026.

## Product promise

**Know which bills are due before payday. No account or bank connection required.**

Build a free, open-source Android app for people who want a simple bill/subscription planner with local storage.
Optional Buy Me a Coffee support funds maintenance. Paying does not unlock features, remove limits, or change access.
The growth goal is real Google Play downloads and repeat usefulness, with honest milestones to show on the portfolio.

Default assumptions: English-first launch, Android phones, USD examples, one local owner, no hosted service.
Launch country and supported currency list remain decisions for implementation. No ads are displayed inside the app;
the proposed $50 advertising budget is for promoting the app externally. Nothing in this plan starts a campaign or spends money.

## First-release scope

| Priority | Deliverable | Acceptance condition |
| --- | --- | --- |
| P0 | Fast onboarding | Open an empty vault and add a first expense without signup or a support prompt. |
| P0 | Bills/subscriptions | Create, edit, search, pause, cancel, categorize, and view upcoming/overdue expenses offline. |
| P0 | Billing rules | Monthly/yearly/weekly/quarterly/custom schedules match the existing recurrence fixtures, including month ends and leap years. |
| P0 | Actual payments | Record amount and date; advance one occurrence; reject duplicate occurrences transactionally. Canceling preserves history. |
| P0 | Before-payday view | Enter next payday and weekly/fortnightly/monthly cadence; show bills due through that date, grouped by currency. Include overdue amounts explicitly. |
| P0 | Reminders | Explain and request notification permission when enabled; schedule local due-date reminders with duplicate protection and reboot/update/timezone handling. |
| P0 | Recovery | Full versioned backup/import through the Android file picker, plus CSV payment reports. Validate an import before replacing data. |
| P0 | Trust | Clear storage/backup explanation, optional biometric/device-credential app lock, no financial records in logs, no marketing SDKs. |
| P0 | About/support | Author, source, privacy policy, help, and optional support text; payment-link behavior depends on the policy decision below. |
| P1 | Home-screen widget | Compact upcoming bills/next due total, after the main app is reliable. |
| Later | Renewal tools | Trial end dates, cancellation deadlines/links, price-change history. |
| Later | Optional self-hosted sync | Explicit opt-in; preserve offline use. Requires a separate secure sync design. |

The before-payday number means **scheduled obligations**, not available bank balance, a prediction of income, or financial advice.
Manual entry is the tradeoff: make repeat entries quick with sensible defaults and duplication before adding complex import automation.

Do not include bank linking, automatic subscription discovery/cancellation, money transfers, household accounts,
cloud backups, in-app ad SDKs, or paid feature tiers in version 1. No automatic synchronization with the current desktop/web databases.

## Android architecture

- Add an independent Gradle project under `android/` in this repository; keep Maven web/desktop builds working.
- Kotlin, Jetpack Compose/Material 3, ViewModels, coroutines/Flow, repository interfaces, and Room/SQLite.
- Choose supported Android versions and pin stable SDK/toolchain/dependency versions during the foundation milestone.
  Proposed minimum: Android 8/API 26, subject to dependency compatibility and intended audience. Target the Play-required API at submission.
- Port platform-independent recurrence/calculation rules with shared golden test fixtures. Do not attempt to run Spring Boot,
  Hibernate/JPA, H2, or JavaFX on the phone. The existing Java 25 source is not a drop-in Android library.
- Keep amounts in integer minor units with currency metadata; use decimal arithmetic for normalization, respect currency precision,
  and never combine currencies or use floating-point amounts. Dates use local calendar dates; audit timestamps are UTC.
- Room entities: Expense, Category, Payment, ReminderRule, ReminderDelivery, AppSettings, and PaydaySchedule.
  Preserve expense versions, audit timestamps, foreign keys, occurrence uniqueness, and transactional payment/date updates.
- Prefer best-effort daily reminder scheduling suitable for due-date reminders, rather than promising exact-to-the-minute delivery.
  Verify Android background restrictions and document behavior if permission is denied, the app is force-stopped, or battery restrictions apply.
- Separate demo fixtures from the production database. Provide an explicit demo/preview route only if needed; never seed actual users' vaults.
- Core use should not need the INTERNET permission. External browser support/source links are deliberate user actions.
- Store data in app-private storage. Do not claim database encryption merely because a biometric screen lock exists.
  Decide backup encryption and explicitly control Android automatic/cloud backup so the local-storage promise is accurate.
- Never commit upload-signing keys, Play service credentials, user exports, or keystore passwords. Keep key recovery instructions private.

## Milestones and exit gates

Work in this order. These are completion gates, not promised calendar dates. Store testing/review adds external waiting time.

### 0. Product and publishing decisions

- [ ] Confirm Buy Me a Coffee URL and ownership; do not guess an account handle.
- [ ] Confirm Google Play developer-account status/type and whether the closed-test requirement applies.
- [ ] Confirm package identifier, public developer name/contact, launch region, minimum Android version, currencies, and privacy wording.
- [ ] Confirm full backup format/encryption and reminder timing expectations.
- [ ] Resolve in-app tipping policy before implementing a payment link. This does not block the local expense-tracking foundation.

### 1. Foundation and shared rules

- [ ] Create the Android project and build a debug APK on CI; document Android Studio setup.
- [ ] Port recurrence and money rules; run common fixture cases alongside existing Java tests.
- [ ] Add Room schema, migrations, isolated test fixtures, and transaction/duplicate-payment tests.
- [ ] Verify a fresh install and database migration on emulator and at least one real phone.

Exit: offline persistence, correct money/date behavior, and repeatable builds, without changing web/desktop behavior.

### 2. A complete useful app

- [ ] Overview, expense editor/list, categories, payment history, and before-payday summary.
- [ ] Notification onboarding/reminders and platform lifecycle tests.
- [ ] Versioned full backup/restore and CSV export, using system file dialogs.
- [ ] Local app lock, accessibility labels, large fonts, light/dark themes, empty/error states.
- [ ] About/help/privacy/support screens, with optional support kept out of onboarding and primary tasks.

Exit: a tester can add three bills, record a payment, receive a reminder, export a backup, and recover into a clean install.

### 3. Closed beta and store preparation

- [ ] Create a signed Android App Bundle for Play and a separately signed APK for GitHub distribution; plan upgrade/signing compatibility.
- [ ] Produce screenshots from the actual Android build with fictional data. Existing desktop screenshots are not Android store screenshots.
- [ ] Prepare icon, feature graphic, description, support contact, public privacy policy, Data safety, content rating, target audience,
  app-access information, and financial-features declaration as applicable to the actual app.
- [ ] Inspect every dependency and all network behavior before making privacy claims.
- [ ] Recruit 15–20 willing testers as a buffer. Where required, at least 12 must remain opted in continuously for 14 days;
  collect genuine usage feedback and apply for production access afterward. Passing the time requirement does not guarantee approval.
- [ ] Test notification denial, reboot, timezone/DST changes, restore corruption, duplicate taps, app upgrade, large fonts, airplane mode,
  screen reader navigation, and activity/process recreation. Do not promise reminders after an Android force-stop.
- [ ] Fix data-loss, calculation, startup, reminder, and restore defects before marketing.

### 4. Public release

- [ ] Publish the free app on Google Play after approval; add the real store URL to GitHub and the portfolio.
- [ ] Publish versioned Android release artifacts/checksums and a changelog in the same repository, clearly separate from desktop releases.
- [ ] Add confirmed Buy Me a Coffee links to the project website/README and GitHub funding configuration.
- [ ] Establish an organic baseline and handle support before the paid experiment.

### 5. First $50 acquisition experiment

Run only after a public store listing works and beta users can complete the main flow without assistance.
Treat this as buying learning, not buying a guaranteed download count or expecting immediate tip revenue.

## Optional support model

Suggested copy: **“BillVault is free. If it helps you stay on top of your bills, you can support its development with a coffee.”**

- Put a persistent, modest Support link in About/Settings and on the project website/README.
- At most one dismissible invitation after several successful uses; remember dismissal. No blocking dialogs, recurring nags,
  paid-only export/reminders, rewarded reviews, supporter badges, or feature unlocks in return for money.
- Default to voluntary one-time support. Do not require a Buy Me a Coffee membership to use the app.
- Open support pages in the external browser; do not handle card data or attach bill information, amounts, or user identifiers to links.

**In-app link is a verification gate, not an assumed permission.** Google's published tipping exception requires the contribution
to go fully to the creator and provide no digital benefits. Buy Me a Coffee publishes a 5% platform fee. These facts alone do not
establish whether the particular integration qualifies; confirm with Play support/current applicable rules before shipping it.
Do not mislabel creator support as tax-exempt charitable giving. If the direct link is not permitted, launch with support on the
website/GitHub and evaluate a permitted in-app support mechanism separately. Keep the app fully free either way.

## Store presentation and download milestones

Working title: **BillVault: Bills & Reminders**. Working short description:
**“Plan recurring bills, track payments, and get reminders. No bank login.”**

Screenshot story: (1) bills before payday, (2) add a subscription, (3) upcoming reminders, (4) payment history,
(5) export/restore, (6) local storage and optional support. Use real UI and demonstrate one clear benefit per frame.
Do not claim automatic bank discovery, exact reminder timing, encrypted storage, or sync unless implemented and verified.

Download metrics use Play Console's actual acquisition/statistics reports and public store listing information.
Publish dated, clearly named milestones on the portfolio/README: for example, “100 Google Play first-time acquisitions as of [date]”
only when that exact metric supports it. Do not combine GitHub artifact downloads, reinstall events, and unique Play users into one number.
Do not scrape a fake live counter or expose developer-console credentials in the app/site. A manual monthly update is sufficient initially.

Use Play Console aggregate acquisition, listing performance, and stability reports first. Do not add intrusive analytics to inflate
measurement precision. Whether someone created three bills or returned to use them needs voluntary tester feedback or a separately
designed, disclosed opt-in measurement system; downloads alone do not establish activation or retention.

## Marketing and the $50 budget

### First, validate organically

Use the existing GitHub project and portfolio, a short real-app demo, and relevant communities that allow self-promotion.
No unsolicited messages or posts are sent as part of this plan. Ask beta users which task helped them and where setup was confusing.
Request honest feedback, not five-star reviews or paid installs.

Draft message: **“I built a free bill reminder app that works without linking your bank. It stores your records locally,
shows what's due before payday, and supports exports. Optional coffee support keeps development going.”**

### Then, one bounded experiment

| Item | Proposed budget | Notes |
| --- | --- | --- |
| Play registration, if needed | $25 once | Separate from advertising; verify account requirements before paying. |
| First advertising test | Up to $50 total | Proposed ceiling, not authorization to launch a campaign. Include any fees/taxes within the ceiling. |
| Server hosting for core Android app | $0 | No backend required for the planned local app. This excludes developer labor, equipment, and external support-provider fees. |
| Creative/store assets | Use existing tools | Real Android screenshots and a short demo; no purchased install/review packages. |

If a new Play account is needed, the proposed cash outlay is approximately **$75**, before any other optional purchases.

Select **one** channel and audience after the store listing is live; do not scatter $50 across platforms. A small community placement
or tightly targeted traffic test is a candidate, subject to its current minimums and a real total spend cap. Verify that the chosen
channel can support a $50 total experiment before selecting it. No channel or result is guaranteed in advance.

Google recommends an App campaign daily budget of at least 50 times target CPI, so a $50 total budget is a poor fit for drawing strong
conclusions from an automated install campaign. If the budget is below a channel's practical minimum, keep it unspent and improve organic
distribution rather than forcing a campaign. Daily budgets can overdeliver; do not treat $10/day for five days as a guaranteed $50 cap.
Use a verified total/lifetime spending limit and end date; if the channel cannot enforce the ceiling, do not run it under this budget.

Prepare two benefit messages but run one at a time: **“Bills due before payday, in one place”** and **“Track subscriptions without bank linking.”**
Use distinct campaign/referrer tags without identifying users. Compare tagged store traffic against an organic baseline where reporting allows it.

Record spend, listing visitors/clicks, attributed acquisitions when available, cost per acquisition, and feedback.
Do not infer installs from ad clicks. Sparse data is directional, not statistically conclusive. At $1, $2, or $5 per acquired user,
$50 buys 50, 25, or 10 acquisitions respectively—**arithmetic scenarios, not forecasts**. Tip income is not an assumed return on ad spend.

Pause when the cap is reached, attribution is broken, ads attract the wrong audience, or users encounter critical problems.
Review at the first $10 spent; spend the remainder only if the store funnel is working and the campaign remains within the approved cap.
Do not automatically increase the budget. The decision after the test is fix the listing/product, repeat a bounded test, or stop paid acquisition.

## Immediate next work

1. Confirm the support URL and Play-account details.
2. Turn milestone 1 into the first Android implementation change: project skeleton, persistence, and shared recurrence fixtures.
3. Recruit genuine testers during development so closed testing is not discovered at the end.
4. Keep a short weekly progress/demo update for the portfolio; publish download milestones after there are real numbers.

## Primary references

Checked November 8, 2026. Recheck policy, target API, SDK versions, and campaign settings at implementation/submission time.

- [Android architecture recommendations](https://developer.android.com/topic/architecture/recommendations)
- [Android alarm scheduling and constraints](https://developer.android.com/develop/background-work/services/alarms)
- [Play developer registration](https://support.google.com/googleplay/android-developer/answer/6112435)
- [Play closed testing requirements](https://support.google.com/googleplay/android-developer/answer/14151465)
- [Play Data safety](https://support.google.com/googleplay/android-developer/answer/10787469)
- [Play Payments policy explanation](https://support.google.com/googleplay/android-developer/answer/10281818)
- [Buy Me a Coffee fees](https://buymeacoffee.com/faq)
- [Play acquisition reporting](https://google.play/business/acquisitionreporting/)
- [Google App campaign budget guidance](https://support.google.com/google-ads/answer/6167156)
- [Google Ads spending limits](https://support.google.com/google-ads/answer/10486637)
