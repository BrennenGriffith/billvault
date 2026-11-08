param([switch]$Postgres)
$ErrorActionPreference = 'Stop'
$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
Set-Location -LiteralPath $projectRoot
if (-not $env:ADMIN_PASSWORD) {
    $securePassword = Read-Host 'Choose your administrator password (at least 12 characters)' -AsSecureString
    $env:ADMIN_PASSWORD = [System.Net.NetworkCredential]::new('', $securePassword).Password
}
if ($env:ADMIN_PASSWORD.Length -lt 12) { throw 'Administrator password must contain at least 12 characters.' }
if ($Postgres) {
    if (-not $env:DB_PASSWORD) { throw 'Set DB_PASSWORD before starting with -Postgres.' }
    & .\mvnw.cmd spring-boot:run
} else {
    & .\mvnw.cmd spring-boot:run '-Dspring-boot.run.profiles=local'
}
exit $LASTEXITCODE
