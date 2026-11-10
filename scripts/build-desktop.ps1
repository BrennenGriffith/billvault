param([ValidatePattern('^\d+\.\d+\.\d+$')][string]$Version = '0.1.1', [switch]$Installer)
$ErrorActionPreference = 'Stop'
if ([Runtime.InteropServices.RuntimeInformation]::OSArchitecture -ne [Runtime.InteropServices.Architecture]::X64) { throw 'This package script currently targets Windows x64.' }
Set-Location -LiteralPath (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
if (-not (Get-Command jpackage -ErrorAction SilentlyContinue)) { throw 'Install a JDK 25+ containing jpackage.' }
& .\mvnw.cmd -B -Pdesktop clean verify
if ($LASTEXITCODE -ne 0) { throw 'Desktop verification failed.' }
$destination = Join-Path (Get-Location) "target/desktop-package-$Version"
if (Test-Path -LiteralPath $destination) { throw 'Packaging directory already exists; use a clean build directory.' }
& jpackage --type app-image --name BillVaultDesktop --app-version $Version --vendor 'Brennen Griffith' --icon desktop/packaging/billvault.ico --input target/desktop-input --main-jar billvault-desktop.jar --main-class com.brennensubs.billvault.desktop.DesktopLauncher --dest $destination --java-options '--enable-native-access=ALL-UNNAMED' --java-options '--sun-misc-unsafe-memory-access=allow'
if ($LASTEXITCODE -ne 0) { throw 'jpackage app image failed.' }
$releaseDirectory = Join-Path (Get-Location) 'target/desktop-release'
New-Item -ItemType Directory -Path $releaseDirectory -Force | Out-Null
Copy-Item LICENSE,THIRD_PARTY_NOTICES.md -Destination "$destination/BillVaultDesktop"
Copy-Item desktop/README.md -Destination "$destination/BillVaultDesktop/README.txt"
Copy-Item desktop/licenses -Destination "$destination/BillVaultDesktop/licenses" -Recurse
Compress-Archive -Path "$destination/BillVaultDesktop" -DestinationPath "$releaseDirectory/BillVaultDesktop-$Version-windows-x64.zip"
if ($Installer) {
    & jpackage --type exe --name BillVaultDesktop --app-version $Version --vendor 'Brennen Griffith' --icon desktop/packaging/billvault.ico --app-image "$destination/BillVaultDesktop" --dest $releaseDirectory --license-file LICENSE --win-per-user-install --win-menu --win-shortcut --win-upgrade-uuid '2c885dd0-28fb-4d24-9e53-1ac6c3e97520'
    if ($LASTEXITCODE -ne 0) { throw 'Installer packaging failed. Install WiX compatible with your JDK; the portable ZIP is already built.' }
}
Get-ChildItem $releaseDirectory -File | Where-Object Extension -in '.zip','.exe' | ForEach-Object {
    $hash = (Get-FileHash -LiteralPath $_.FullName -Algorithm SHA256).Hash.ToLowerInvariant()
    [IO.File]::WriteAllText("$($_.FullName).sha256", "$hash  $($_.Name)`n", [Text.UTF8Encoding]::new($false))
}
Write-Host "Desktop packages: $releaseDirectory"
