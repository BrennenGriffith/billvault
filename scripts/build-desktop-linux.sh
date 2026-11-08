#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
version="${1:-0.1.0}"
[[ "$version" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]] || { echo 'Version must be x.y.z'; exit 1; }
[[ "$(uname -m)" == x86_64 ]] || { echo 'This packaging script currently targets Linux x86_64.'; exit 1; }
: "${APPIMAGETOOL:?Set APPIMAGETOOL to a verified appimagetool executable}"
./mvnw -B -Pdesktop clean verify
mkdir -p target/desktop-release
jpackage --type app-image --name BillVaultDesktop --app-version "$version" --vendor 'Brennen Griffith' \
  --input target/desktop-input --main-jar billvault-desktop.jar \
  --main-class com.brennensubs.billvault.desktop.DesktopLauncher --dest target/desktop-linux \
  --java-options '--enable-native-access=ALL-UNNAMED' --java-options '--sun-misc-unsafe-memory-access=allow'
appdir=target/BillVaultDesktop.AppDir
mkdir -p "$appdir/usr"
cp -a target/desktop-linux/BillVaultDesktop "$appdir/usr/"
cp LICENSE THIRD_PARTY_NOTICES.md desktop/README.md "$appdir/usr/BillVaultDesktop/"
cp -a desktop/licenses "$appdir/usr/BillVaultDesktop/licenses"
cp desktop/packaging/AppRun desktop/packaging/billvault.desktop desktop/packaging/billvault.svg "$appdir/"
chmod +x "$appdir/AppRun"
ARCH=x86_64 "$APPIMAGETOOL" "$appdir" "target/desktop-release/BillVaultDesktop-$version-x86_64.AppImage"
(cd target/desktop-release && sha256sum "BillVaultDesktop-$version-x86_64.AppImage" > "BillVaultDesktop-$version-x86_64.AppImage.sha256")
