$ErrorActionPreference = 'Stop'
Set-Location -LiteralPath (Join-Path $PSScriptRoot '..')
& npm.cmd --prefix frontend ci
if ($LASTEXITCODE -ne 0) { throw 'npm install failed.' }
& npm.cmd --prefix frontend test
if ($LASTEXITCODE -ne 0) { throw 'Frontend tests failed.' }
& npm.cmd --prefix frontend run build
if ($LASTEXITCODE -ne 0) { throw 'Frontend build failed.' }
& .\mvnw.cmd -B clean verify
if ($LASTEXITCODE -ne 0) { throw 'Backend verification failed.' }
$release = Get-Date -Format 'yyyyMMddHHmmssfff'
$releaseDirectory = Join-Path $PWD ".local/releases/$release"
New-Item -ItemType Directory -Path $releaseDirectory -Force | Out-Null
Copy-Item -LiteralPath 'target/billvault-0.0.1-SNAPSHOT.jar' -Destination (Join-Path $releaseDirectory 'billvault.jar')
Set-Content -LiteralPath '.local/current-release.txt' -Value $release -Encoding ascii
Write-Host 'Verified personal build ready. Run .\scripts\start-personal.ps1.'
