param([ValidateRange(1024,65535)][int]$Port = 8080)
$ErrorActionPreference = 'Stop'
if ([Environment]::OSVersion.Platform -ne 'Win32NT') { throw 'This helper requires Windows DPAPI. Use the environment-based JAR instructions on Linux/macOS.' }
$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
Set-Location -LiteralPath $projectRoot
$credentialPath = Join-Path $projectRoot '.local/admin.credential.xml'
if (-not (Test-Path -LiteralPath $credentialPath)) { throw 'Run .\scripts\setup-personal.ps1 first.' }
if (-not (Test-Path -LiteralPath '.local/current-release.txt')) { throw 'Run .\scripts\build.ps1 first.' }
$release = (Get-Content -LiteralPath '.local/current-release.txt' -Raw).Trim()
if ($release -notmatch '^\d{17}$') { throw 'Invalid release pointer. Rebuild the application.' }
$jar = Join-Path $projectRoot ".local/releases/$release/billvault.jar"
if (-not (Test-Path -LiteralPath $jar)) { throw 'Release JAR missing. Rebuild the application.' }
if (Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue) { throw "Port $Port is already in use. Stop the existing app or select another port." }
$credential = Import-Clixml -LiteralPath $credentialPath
$originalUser = $env:ADMIN_USERNAME
$originalPassword = $env:ADMIN_PASSWORD
try {
    $env:ADMIN_USERNAME = $credential.UserName
    $env:ADMIN_PASSWORD = $credential.GetNetworkCredential().Password
    Write-Host "Starting your personal vault at http://127.0.0.1:$Port. Press Ctrl+C to stop."
    & java -Xms128m -Xmx512m -jar $jar --spring.profiles.active=personal "--server.port=$Port"
    if ($LASTEXITCODE -ne 0) { throw 'BillVault did not exit successfully. Review the console output.' }
} finally { $env:ADMIN_USERNAME = $originalUser; $env:ADMIN_PASSWORD = $originalPassword }
