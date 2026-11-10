# BillVault MCP and import skill

Connect Codex, Claude Code, or another stdio MCP client to a running BillVault desktop
vault or self-hosted web installation. BillVault does not call a model provider and needs
no OpenAI/Anthropic API key. Use your existing assistant client and its own account/plan;
its access, connected sources, and usage limits still apply.

**Available with Desktop 0.3.0 and the current self-hosted source.** Download the desktop
package and the separate `BillVault-MCP-0.3.0.zip` adapter from
[the release](https://github.com/BrennenGriffith/billvault/releases/tag/desktop-v0.3.0).
The adapter ZIP contains this directory, its companion skill, and dependency lockfile;
extract it, then install dependencies as below. Desktop 0.2.0 lacks the bridge.

## What works

- List categories and a bounded expense page (notes, descriptions, and payment labels are omitted).
- Preview up to 50 structured recurring expenses with CREATE/SKIP rows.
- Commit the reviewed preview atomically and retry its ID without adding the batch twice.
- Import into desktop or self-hosted storage using the same validation and billing service.

No provider API key, BillVault subscription, or model SDK is required. Node.js 22+ is
required for this first adapter; Java is required only when building BillVault from source.
The local stdio adapter is not a remotely hosted MCP URL for ChatGPT's web interface.

An assistant can prepare expenses from a statement, pasted table, or an authorized tool
available in that assistant session. BillVault cannot automatically access a bank connector
or chat history attached to another product. Share only recurring expense summaries;
account/routing/card numbers and bank credentials have no place in the import schema.
This version imports expenses, not transaction histories or payment records.

## Install adapter dependencies

From the repository root:

```powershell
npm ci --prefix integrations/mcp
```

Use an absolute path to `integrations/mcp/server.mjs` in client configuration. Do not
register `npm start` as the stdio command: npm's banner can interfere with protocol output.
The server command is `node /absolute/path/to/billvault/integrations/mcp/server.mjs`.

## Desktop connection

Use the Desktop 0.3.0 native launcher, or build the JavaFX app with
`./mvnw -B -Pdesktop package` (Windows: `./mvnw.cmd`).
For packaged downloads, replace the Java command below with the installed
`BillVaultDesktop.exe --mcp-port=8765` or `./BillVaultDesktop-0.3.0-x86_64.AppImage --mcp-port=8765`.
In PowerShell 7+, create a session-only token, then start the built application:

```powershell
$env:BILLVAULT_MODE = 'desktop'
$env:BILLVAULT_URL = 'http://127.0.0.1:8765'
$env:BILLVAULT_MCP_TOKEN = [Convert]::ToHexString([Security.Cryptography.RandomNumberGenerator]::GetBytes(32))
java --enable-native-access=ALL-UNNAMED --sun-misc-unsafe-memory-access=allow -jar target/desktop-input/billvault-desktop.jar --mcp-port=8765
```

On Linux/macOS, use your shell's session environment with a URL-safe random token of
32–256 characters, then the same Java command. A newly packaged native launcher accepts
`--mcp-port=8765` too. Version 0.2.0 launchers do not implement it.

The bridge binds only to **127.0.0.1**, authenticates with the session token, rejects browser
Origin requests, and shares the running JavaFX app's store. It does not open a second H2
writer. Keep the desktop open. Ordinary startup without `--mcp-port` has no bridge listener.
Close/restart without the flag to revoke access; rotate the token for a new session.

The Java command occupies that shell. To launch your assistant from the same environment,
use a second terminal with the same environment set, or launch Java as a background process.
For example, from PowerShell in the repository root:

```powershell
Start-Process javaw -ArgumentList '--enable-native-access=ALL-UNNAMED --sun-misc-unsafe-memory-access=allow -jar target/desktop-input/billvault-desktop.jar --mcp-port=8765' -WindowStyle Normal
```

Do not launch both examples against the same vault. The native window remains the local
application UI. Keep tokens out of command arguments, committed config files, and chat.

## Self-hosted connection

Run the updated web server, then set these variables only in the environment used to
launch your assistant:

```powershell
$env:BILLVAULT_MODE = 'web'
$env:BILLVAULT_URL = 'https://your-billvault.example'
$env:BILLVAULT_USERNAME = 'your-admin-name'
$credential = Get-Credential -UserName $env:BILLVAULT_USERNAME -Message 'BillVault sign-in'
$env:BILLVAULT_PASSWORD = $credential.GetNetworkCredential().Password
```

For local development, `http://127.0.0.1:8080` is accepted. Non-loopback connections require
HTTPS; certificate verification stays enabled. A reverse-proxy base path is supported.
The adapter uses the existing administrator session login with cookies and CSRF, retained
in memory. It does not disable browser protections, persist credentials, or introduce a
provider API credential. This first version has the same single-administrator access as
the web app; there are no separate integration scopes. Stop the adapter and remove its
session environment variables when finished. Credentials may still be visible to other
processes running under your OS account; use only trusted local clients.

## Register with Codex or Claude Code

Launch the client with the session environment from above. Replace the paths below.
Registration saves a command, not credentials. Do not pass secrets through CLI `--env` options.

```text
codex mcp add billvault -- node "/absolute/path/to/billvault/integrations/mcp/server.mjs"
claude mcp add --transport stdio --scope user billvault -- node "/absolute/path/to/billvault/integrations/mcp/server.mjs"
```

For Codex, add the explicit environment pass-through list to the generated
`[mcp_servers.billvault]` entry in your user `~/.codex/config.toml`:

```toml
env_vars = ["BILLVAULT_MODE", "BILLVAULT_URL", "BILLVAULT_MCP_TOKEN", "BILLVAULT_USERNAME", "BILLVAULT_PASSWORD"]
```

Restart clients after changing their environment. Claude Code's `/mcp` and Codex's MCP
status can verify tool discovery. Enable host approval for `billvault_commit_import`;
tool annotations describe behavior but do not enforce user consent. The server requires
an exact preview ID and confirmed=true, not an independent proof that a human approved it.

Install the companion skill by copying **only** `integrations/skills/billvault-import/`
into your personal `~/.agents/skills/` (Codex) or `~/.claude/skills/` (Claude Code) directory.
Do not overwrite an existing skill with local modifications. The skill is optional and
does not install the MCP server itself.

Client references: [Codex MCP](https://developers.openai.com/codex/mcp),
[Claude Code MCP](https://code.claude.com/docs/en/mcp),
[MCP SDK stdio transport](https://ts.sdk.modelcontextprotocol.io/server),
[Codex skills](https://developers.openai.com/codex/skills), and
[Claude Code skills](https://code.claude.com/docs/en/skills).

## Example workflow

Ask: “Review these recurring expenses and prepare them for BillVault. Ask me about any
missing dates or recurrence. Show the import preview before saving.”

Fictional example after selecting a real category ID:

```json
{
  "source": "User-reviewed statement summary",
  "expenses": [{
    "name": "Streambox (demo)", "kind": "SUBSCRIPTION", "categoryId": 1,
    "amount": "14.99", "currency": "USD", "frequency": "MONTHLY",
    "startDate": "2026-11-01", "nextDueDate": "2026-12-01",
    "status": "ACTIVE", "variableAmount": false
  }]
}
```

The assistant calls `billvault_preview_import`, displays the rows, then calls
`billvault_commit_import` with the returned previewId after approval. No payment is
recorded. Open/refresh the web view to see changes; the desktop refreshes after a commit.

## Import behavior and limits

- Previews expire after ten minutes and disappear on server restart. Committed receipts
  survive restarts; retry the same preview ID after a lost response.
- A name/currency/kind match skips a row, including repeated rows in a batch. Whitespace
  and case are normalized. This is conservative detection, not bank transaction matching;
  distinct same-named expenses may need manual entry.
- Expense changes between preview and commit normally reject the preview as stale.
  This is not a lock on all concurrent manual edits; avoid importing while others edit.
- Inserts and their receipt share one transaction. Any failed row rolls back the batch.
- Import receipts store created IDs and skipped counts, not raw source statements.
- At most 100 active previews are retained per process. Financial data is not written to
  adapter logs. Text inside imported fields is untrusted data, not assistant instructions.
- Money follows the current two-decimal BillVault model; no exchange-rate conversion.
- The tool schema omits payment labels and bank fields. Free-text fields are not a
  sensitive-data detector; review summaries before sending them.
- This slice does not offer raw SQL, deletion, expense edits, provider bank linking,
  background sync, or historical-payment import.

## Verify from source

```powershell
./mvnw.cmd -B -Pdesktop test
npm test --prefix integrations/mcp
npm audit --prefix integrations/mcp --omit=dev
```

The Node tests exercise a real SDK stdio client/server handshake and a controlled HTTP
fixture. Java tests exercise authenticated web imports and the real desktop H2 bridge,
including retry receipts after reopening the vault. Use fictional data for manual checks.
