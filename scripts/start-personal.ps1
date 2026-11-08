param([ValidateRange(1024,65535)][int]$Port = 8080, [string]$Username = 'admin')
$ErrorActionPreference = 'Stop'
$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
Set-Location -LiteralPath $projectRoot
if (-not (Test-Path -LiteralPath '.local/current-release.txt')) { throw 'Run .\scripts\build.ps1 first.' }
$release = (Get-Content -LiteralPath '.local/current-release.txt' -Raw).Trim()
if ($release -notmatch '^\d{17}$') { throw 'Invalid release pointer. Rebuild the application.' }
$jar = Join-Path $projectRoot ".local/releases/$release/billvault.jar"
if (-not (Test-Path -LiteralPath $jar)) { throw 'Release JAR missing. Rebuild the application.' }
if (Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue) { throw "Port $Port is already in use. Stop the existing app or select another port." }
$securePassword = Read-Host 'Administrator password (12+ characters; not saved)' -AsSecureString
$originalUser = $env:ADMIN_USERNAME
$originalPassword = $env:ADMIN_PASSWORD
try {
    $env:ADMIN_USERNAME = $Username
    $env:ADMIN_PASSWORD = [System.Net.NetworkCredential]::new('', $securePassword).Password
    if ($env:ADMIN_PASSWORD.Length -lt 12 -or [Text.Encoding]::UTF8.GetByteCount($env:ADMIN_PASSWORD) -gt 72 -or $env:ADMIN_PASSWORD.StartsWith('replace-with-')) { throw 'Choose a non-example password with at least 12 characters and at most 72 UTF-8 bytes.' }
    Write-Host "Starting your personal vault at http://127.0.0.1:$Port. Press Ctrl+C to stop."
    & java -Xms128m -Xmx512m -jar $jar --spring.profiles.active=personal "--server.port=$Port"
    if ($LASTEXITCODE -ne 0) { throw 'BillVault did not exit successfully. Review the console output.' }
} finally { $env:ADMIN_USERNAME = $originalUser; $env:ADMIN_PASSWORD = $originalPassword; $securePassword.Dispose() }
