# Enable GitHub Actions

The reviewed `github-actions.yml` template runs frontend tests/build/audit, backend verification with H2 and
PostgreSQL, and a disposable Docker Compose smoke test. It never deploys the application or provisions AWS.

It is opt-in. To enable it on your fork or installation:

```powershell
New-Item -ItemType Directory -Path .github/workflows -Force
Copy-Item docs/ci/github-actions.yml .github/workflows/ci.yml
git add .github/workflows/ci.yml
git commit -m "Enable GitHub Actions verification"
git push
```

Your GitHub credentials must allow creating workflow files. For the GitHub CLI, complete the browser flow
from `gh auth refresh --hostname github.com --scopes workflow`, then push again. Repository owners can also
add the file using GitHub's web editor. Review the template's permissions and action versions before enabling it.
Docker checks use only disposable CI data. Do not substitute your real database credentials.

## Desktop Linux releases

The desktop Linux workflow is active in this repository under `.github/workflows/desktop-release.yml`.
The original web/Docker verification template above remains opt-in. Forks can activate the desktop template as follows.

`desktop-release.yml` builds the JavaFX edition on Ubuntu 22.04 with Temurin 25, runs the shared/desktop
tests, packages an x86_64 AppImage, and launches that package under Xvfb against a temporary database.
Additional 1024×600 and 800×600 runs verify that the native frame stays within usable screen bounds.
Only after that succeeds does it upload the AppImage and checksum to `desktop-v<version>` in Releases.
It checks out the existing `desktop-v<version>` tag and appends Linux assets to that Windows desktop release,
so both platform packages come from the same application source. Create the tagged desktop release first.
It uses GitHub's temporary job token, with write permission only in the publishing job, and no personal secrets.

Activate by copying it to `.github/workflows/desktop-release.yml`, committing and pushing with workflow-capable
credentials, then selecting **Actions → Desktop Linux release → Run workflow**. Enter the same `x.y.z`
version used for the Windows package. Re-running an already uploaded version refuses duplicate assets;
choose a new version for changed binaries. The pinned AppImage tool checksum fails closed if upstream replaces
the continuous release; review the new upstream version and checksum before deliberately updating the template.

Windows packages are built with `scripts/build-desktop.ps1 -Installer` on Windows with a redistributable
JDK 25+ and a JDK-compatible WiX installation. See [desktop guide](../../desktop/README.md).
