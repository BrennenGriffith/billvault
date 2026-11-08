param(
    [Parameter(Mandatory=$true)][string]$BackupFile,
    [string]$DestinationDirectory = (Join-Path $PSScriptRoot ("../.local/restore-check/" + (Get-Date -Format 'yyyyMMdd-HHmmss-fff')))
)
$ErrorActionPreference = 'Stop'
$backup = (Resolve-Path -LiteralPath $BackupFile).Path
$checksumPath = $backup + '.sha256'
if (-not (Test-Path -LiteralPath $checksumPath)) { throw 'Missing SHA256 file. Refusing an unverified restore.' }
$expected = (Get-Content -LiteralPath $checksumPath -Raw).Trim()
if ($expected -notmatch '^[0-9A-Fa-f]{64}$' -or (Get-FileHash -LiteralPath $backup -Algorithm SHA256).Hash -ne $expected) { throw 'Backup checksum mismatch. Refusing to restore.' }
New-Item -ItemType Directory -Path $DestinationDirectory -Force | Out-Null
$target = Join-Path $DestinationDirectory 'billvault.mv.db'
$source = $null
$destination = $null
try {
    $source = [IO.File]::OpenRead($backup)
    # Never overwrite a personal database or a previous restored copy.
    $destination = [IO.File]::Open($target, [IO.FileMode]::CreateNew, [IO.FileAccess]::Write, [IO.FileShare]::None)
    $source.CopyTo($destination)
} finally { if ($destination) { $destination.Dispose() }; if ($source) { $source.Dispose() } }
if ((Get-FileHash -LiteralPath $target -Algorithm SHA256).Hash -ne $expected) { throw 'Restored file failed checksum verification.' }
Write-Output (Resolve-Path -LiteralPath $target).Path
Write-Host 'Restored into a separate directory. Your live database was not replaced. See docs/LOCAL_OPERATIONS.md to open and inspect this copy.'
