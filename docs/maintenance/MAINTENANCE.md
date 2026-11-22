# Maintenance log

BillVault is actively maintained. Every dependency update is test built in a clean environment before it is merged, and the test results are compared against a baseline taken before the update. Nothing gets merged on a green checkmark alone.

## November 22, 2026 — dependency update sweep

Eight Dependabot updates reviewed, test built, and merged:

| Update | Change | Verification |
| --- | --- | --- |
| JavaFX controls (#16) | 25.0.1 → 27.0.1.1 | Built and test run on JDK 25 in a clean VM before merging; results matched the pre-update baseline exactly |
| Vite (#2) | 7.3.7 → 8.3.6 | Frontend install, production build, and test suite re-run |
| lucide-react (#11) | 0.468.0 → 1.70.0 | Frontend build and tests re-run |
| @testing-library/jest-dom (#3) | 6.9.1 → 7.0.1 | Test suite re-run, 4 of 4 passing |
| actions/setup-node (#12) | 4.4.0 → 7.1.0 | Workflow change, reviewed |
| actions/upload-artifact (#13) | 4.6.2 → 7.0.2 | Workflow change, reviewed |
| build-helper-maven-plugin (#15) | 3.6.1 → 3.8.0 | Backend build and tests re-run |
| Maven (#17) | 3.10.0 → 3.10.1 | Backend build and tests re-run |

After the last merge, `main` was rebuilt and retested end to end one more time.

### Test evidence

Frontend, after the sweep. The production build succeeds and the full test suite passes:

![Frontend build and test output](test-evidence-frontend.png)

Backend, before and after. Two test classes error in the sandbox because they expect a local profile and database setup. They error identically on the untouched baseline, so the updates introduced zero new failures:

![Backend test output, baseline versus after updates](test-evidence-backend.png)

### Still open, on purpose

- **TypeScript 5.9.3 → 7.0.2 (#4): held.** The new compiler is stricter and fails on the CSS import in `main.tsx` because the project has no `vite-env.d.ts` type reference file. It is a one file fix and will land separately so the change stays reviewable.
- **@vitejs/plugin-react 5.2.0 → 6.3.0 (#5): in review** against the new Vite 8 build before merging.
- **actions/download-artifact (#9) and actions/setup-java (#10): verified in testing.** Merging is waiting on a repository permission for workflow files.
