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
