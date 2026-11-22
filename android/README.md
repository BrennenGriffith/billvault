# BillVault Android preview

Open this **android/** directory in Android Studio, not the parent Maven project.
This is a native Kotlin/Jetpack Compose application with app-private SQLite storage.
It is an initial source preview, not a Google Play release or complete feature-parity release.
No managed server, paid relay, model-provider API key, or bank connection is required.

## First launch

Choose one of three separate vault modes:

| Mode | Setup | Sync |
| --- | --- | --- |
| Self-hosted web app | Your updated BillVault HTTPS URL, username, and password | Tap **Sync** to send queued edits and reload host records |
| Standalone desktop | Enable **Storage & preferences → Android sync** in the updated desktop app, then pair | Tap **Sync** while both devices are on a reachable local network |
| Only on this phone | No sign-in or network needed | Records stay local; Sync reloads the local vault |

Connected modes cache records on the phone. They do not merge with another vault or with phone-only
records. Switching vaults preserves each cache and its pending edits; saved vaults appear on onboarding.
There is no automatic background synchronization in this first preview.

## Build and run

The initial project uses the compatible installed toolchain: AGP 8.7.3, Gradle 8.9, Kotlin 2.0.21,
JDK 21, and Android SDK 35. Minimum device version is Android 8 / API 26. Dependencies are pinned;
these are development targets, not a statement of current Google Play submission eligibility.

1. In Android Studio, choose **Open** and select this directory.
2. Use the IDE's bundled JDK 21 as the Gradle JDK. Let Gradle sync.
3. Install Android SDK 35 if prompted. `local.properties` is machine-local and ignored by Git.
4. Select a physical device or create an emulator in Device Manager, then run **app**.

Windows command-line checks from this directory:

```powershell
$env:JAVA_HOME = 'C:/Program Files/Android/Android Studio/jbr'
.\gradlew.bat assembleDebug testDebugUnitTest lintDebug
```

On Linux/macOS, select JDK 21 and run `./gradlew` with the same tasks.
The debug APK is `app/build/outputs/apk/debug/app-debug.apk`. It uses a local debug signing key;
production signing and Play distribution are not configured. Do not commit signing keys.

## Connect to a self-hosted installation

Build and run the backend from the same source revision as this Android preview. It adds authenticated
`/api/mobile/snapshot` and `/api/mobile/commands` endpoints and Flyway migration V3. Existing Desktop
0.3.0 downloads and older self-hosted builds do not have this mobile API.

Enter the installation's HTTPS base URL, including any reverse-proxy base path, and administrator
credentials. Android uses the existing cookie session and CSRF protection. Passwords stay in memory
unless **Remember sign-in** is selected; remembered credentials are encrypted with Android Keystore.
Cookie sessions are not saved. If sign-in expires, enter the password in Settings before syncing.

For an emulator and a local development web server only, a debug build accepts
`http://10.0.2.2:8080`. Release builds require HTTPS. A physical phone needs a reachable HTTPS server;
`localhost` on the phone refers to the phone itself. The supplied loopback-only Docker setup is not
made remotely reachable automatically. Configure your own HTTPS access or use desktop pairing.

## Pair with the desktop

Use the desktop app built from this revision. In **Storage & preferences**, find **Android sync**:

1. Choose **Enable / show pairing**. It starts a separate TLS listener on port 8766; normal desktop
   startup leaves it off. This does not expose the loopback-only MCP bridge.
2. Pick the correct Wi-Fi/Ethernet LAN address from the desktop's list. If Windows asks, allow access
   on private networks only. Do not forward this port on your router.
3. Copy the pairing details, or enter the address, certificate SHA256 fingerprint, and one-time code
   on Android. Compare the fingerprint directly with the desktop. The code expires after five minutes,
   accepts at most five failed attempts, and is consumed after one successful pairing.
4. Tap **Pair and sync**. After that, use Android's **Sync** button. Desktop must remain running with
   Android sync enabled. Phone and desktop must be mutually reachable; Wi-Fi client isolation/VPNs
   may block the connection.

The phone pins the exact paired certificate and verifies its validity. Device access tokens are encrypted
with Android Keystore; desktop stores token hashes. The desktop TLS identity lives in its OS user-data
folder as `mobile-identity.p12`, protected by the OS account, and must not be shared or committed.
**Stop Android sync** closes the listener. **Forget paired devices** revokes every phone's access;
local cached data on those phones is not remotely erased. A changed desktop identity requires re-pairing.

## Editing and conflict behavior

- Add/edit expenses and record payments in any mode. Connected-mode mutations are queued locally
  until Sync; the UI shows the pending count. Do not mistake a queued payment for a host-confirmed one.
- Sync sends each command with a stable UUID. Host changes and their receipts commit in one
  transaction. If the response is lost, retrying does not create another expense or payment.
- Finish syncing an expense's pending change before editing or paying that same expense again.
  Newly created remote expenses must sync before recording their first payment.
- Stale edits, duplicate occurrences, expired pairing, and network errors remain explicit. Pending
  edits stay on the phone; conflicts do not silently overwrite host records.
- Settings offers a confirmed **Discard pending changes and reload** action. It intentionally drops
  unsent edits; it does not undo changes already accepted by the host.
- Snapshot schema is versioned. The initial snapshot endpoint is intended for personal-scale vaults
  (at most 10,000 expenses; Android rejects responses over 20 MB).

## Included and remaining work

Included: first-run mode selection; isolated saved profiles; overview with per-currency commitments;
searchable expense list and editor; next-due/overdue list; recurrence/date validation; payment recording
and history; actual monthly/yearly totals; light/dark display toggle; manual sync; payment CSV export;
versioned JSON backup and validated phone-only restore. Backups use Android's file picker. Sync pending
changes before exporting; financial exports contain sensitive data and belong outside the repository.

Not included yet: system notifications/reminder scheduling, category management UI, full projected
calendar, persistent form drafts, advanced conflict resolution, background sync, automatic cross-vault
migration, Android MCP/agent controls, or store publishing. Shared recurrence and money cases are tested
against existing Java behavior. Test the UI on an emulator and a physical phone before distributing it.

App data and connection secrets are excluded from automatic cloud/device-transfer backups; use explicit
exports. The financial SQLite cache itself is not separately encrypted. Uninstalling clears local app
storage; an exported backup is necessary to recover phone-only records. Android library notices are in
`app/src/main/assets/licenses/`.
