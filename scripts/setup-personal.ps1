param([string]$Username = 'admin')
$ErrorActionPreference = 'Stop'
if ([Environment]::OSVersion.Platform -ne 'Win32NT') { throw 'This script uses Windows DPAPI. See README for Linux environment configuration.' }
if ([string]::IsNullOrWhiteSpace($Username)) { throw 'Username must not be blank.' }
$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$credentialPath = Join-Path $projectRoot '.local/admin.credential.xml'
if (Test-Path -LiteralPath $credentialPath) { throw 'Personal credentials already exist. Use change-personal-password.ps1 to replace them explicitly.' }
$password = Read-Host 'Choose your personal administrator password (12+ characters, at most 72 UTF-8 bytes)' -AsSecureString
$confirmation = Read-Host 'Confirm your password' -AsSecureString
$plain = [Net.NetworkCredential]::new('', $password).Password
$confirmed = [Net.NetworkCredential]::new('', $confirmation).Password
try {
    if ($plain -cne $confirmed) { throw 'Passwords do not match.' }
    if ($plain.Length -lt 12 -or [Text.Encoding]::UTF8.GetByteCount($plain) -gt 72 -or $plain.StartsWith('replace-with-')) { throw 'Use a unique password of at least 12 characters and at most 72 UTF-8 bytes.' }
    New-Item -ItemType Directory -Path (Split-Path $credentialPath) -Force | Out-Null
    [PSCredential]::new($Username, $password) | Export-Clixml -LiteralPath $credentialPath -NoClobber
    Write-Host 'Credentials saved using Windows DPAPI. Only your Windows account on this PC can decrypt the saved password.'
} finally { $plain = $null; $confirmed = $null }
