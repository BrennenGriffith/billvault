param(
    [string]$DatabasePath = (Join-Path $PSScriptRoot '../data/billvault.mv.db'),
    [string]$DestinationDirectory = (Join-Path $env:LOCALAPPDATA 'BillVault/backups')
)
$ErrorActionPreference = 'Stop'
$database = (Resolve-Path -LiteralPath $DatabasePath).Path
New-Item -ItemType Directory -Path $DestinationDirectory -Force | Out-Null
$backup = Join-Path $DestinationDirectory ("billvault-" + (Get-Date -Format 'yyyyMMdd-HHmmss-fff') + '.mv.db')
$source = $null
$destination = $null
try {
    # An exclusive handle refuses a copy while H2 is using the database.
    $source = [IO.File]::Open($database, [IO.FileMode]::Open, [IO.FileAccess]::Read, [IO.FileShare]::None)
    $destination = [IO.File]::Open($backup, [IO.FileMode]::CreateNew, [IO.FileAccess]::Write, [IO.FileShare]::None)
    $source.CopyTo($destination)
} catch { throw "Backup failed. Stop BillVault before copying its H2 database. $($_.Exception.Message)" }
finally { if ($destination) { $destination.Dispose() }; if ($source) { $source.Dispose() } }
$checksum = (Get-FileHash -LiteralPath $backup -Algorithm SHA256).Hash
Set-Content -LiteralPath ($backup + '.sha256') -Value $checksum -Encoding ascii
Write-Output $backup
Write-Host 'Backup complete. Keep the .mv.db and .sha256 files together, outside Git. Copy backups to another device for disk-failure protection.'
