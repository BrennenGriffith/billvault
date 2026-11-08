# Security policy

BillVault currently targets a single administrator on a private, local installation. The personal startup profile
binds to 127.0.0.1 and disables OpenAPI/Swagger. Public source code does not mean your financial records should be public.

## Reporting a vulnerability
Use **Security → Report a vulnerability** on [the GitHub repository](https://github.com/BrennenGriffith/billvault/security/advisories/new)
for a private report. Do not post an exploit involving personal information in a public issue. Include the affected commit,
reproduction steps using fictional data, expected impact and a suggested fix if available. This is a community-maintained
personal project, without a guaranteed response SLA or security audit.

## Operating safely
- Keep real records, .env files, backups, credentials and local database files outside Git. Ignore rules are defense in depth;
  inspect `git diff --cached` before pushing.
- Use a unique password and keep your Windows account secure. The personal startup script decrypts credentials only for the app process.
- Stop the application before H2 file backups; verify a restored copy before relying on a backup.
- The provided HTTP local configuration is not an Internet deployment. Use TLS, Secure cookies, secrets handling,
  backup automation and abuse protection before public hosting.
- A single administrator can access every record. Multi-user isolation is not implemented.
- Sessions are in memory; restarting invalidates them. Changing your saved password takes effect after restart.

No bank connections or real payment processing are implemented. Please do not submit bank credentials.
