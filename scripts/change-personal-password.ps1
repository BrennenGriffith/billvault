param([string]$Username = 'admin')
$ErrorActionPreference = 'Stop'
if ([Environment]::OSVersion.Platform -ne 'Win32NT') { throw 'This helper requires Windows DPAPI.' }
$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$credentialPath = Join-Path $projectRoot '.local/admin.credential.xml'
if (-not (Test-Path -LiteralPath $credentialPath)) { throw 'Run setup-personal.ps1 first.' }
# Replacing the saved credential takes effect after the application restarts.
$password = Read-Host 'New administrator password (12+ characters, at most 72 UTF-8 bytes)' -AsSecureString
$confirmation = Read-Host 'Confirm your new password' -AsSecureString
$plain = [Net.NetworkCredential]::new('', $password).Password
$confirmed = [Net.NetworkCredential]::new('', $confirmation).Password
try {
    if ([string]::IsNullOrWhiteSpace($Username) -or $plain -cne $confirmed -or $plain.Length -lt 12 -or [Text.Encoding]::UTF8.GetByteCount($plain) -gt 72 -or $plain.StartsWith('replace-with-')) { throw 'Check the username, password length, and confirmation.' }
    [PSCredential]::new($Username, $password) | Export-Clixml -LiteralPath $credentialPath
    Write-Host 'Saved password changed. Restart BillVault to invalidate existing sessions and apply it.'
} finally { $plain = $null; $confirmed = $null }
