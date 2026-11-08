param([string]$Username = 'admin')
$ErrorActionPreference = 'Stop'
Set-Location -LiteralPath (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
if (-not (Get-Command docker -ErrorAction SilentlyContinue)) { throw 'Install Docker Desktop or Docker Engine with Compose first. See docs/DOCKER.md.' }
$originalUser = $env:ADMIN_USERNAME
$originalAdmin = $env:ADMIN_PASSWORD
$originalDb = $env:DB_PASSWORD
$admin = $null
$database = $null
try {
    $admin = Read-Host 'Administrator password (12+ characters; no credential file)' -AsSecureString
    $database = Read-Host 'Database password (reuse the original for an existing volume)' -AsSecureString
    $env:ADMIN_USERNAME = $Username
    $env:ADMIN_PASSWORD = [System.Net.NetworkCredential]::new('', $admin).Password
    $env:DB_PASSWORD = [System.Net.NetworkCredential]::new('', $database).Password
    if ($env:ADMIN_PASSWORD.Length -lt 12 -or [Text.Encoding]::UTF8.GetByteCount($env:ADMIN_PASSWORD) -gt 72 -or $env:ADMIN_PASSWORD.StartsWith('replace-with-')) { throw 'Administrator password must be non-example, at least 12 characters and at most 72 UTF-8 bytes.' }
    if ($env:DB_PASSWORD.Length -lt 12) { throw 'Use a unique database password of at least 12 characters.' }
    Write-Host 'No credential file is written. Docker retains environment values in local container metadata. See docs/DOCKER.md.'
    & docker compose up --build -d --wait --wait-timeout 240
    if ($LASTEXITCODE -ne 0) { throw 'Docker startup failed. Check Docker Desktop and docker compose logs.' }
    Write-Host 'BillVault: http://127.0.0.1:8088 (or your APP_PORT override).'
} finally {
    $env:ADMIN_USERNAME = $originalUser
    $env:ADMIN_PASSWORD = $originalAdmin
    $env:DB_PASSWORD = $originalDb
    if ($admin) { $admin.Dispose() }
    if ($database) { $database.Dispose() }
}
