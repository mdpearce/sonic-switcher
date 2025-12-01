# CI/CD Setup

## Overview

This project uses **GitHub Actions** for continuous integration on pull requests. The CI/CD pipeline is optimized for cost (free public runners) and speed (aggressive caching).

## Initial Setup

### Required GitHub Secrets

Before the workflow can run, you need to configure the following secrets in your GitHub repository:

**Go to**: `Settings → Secrets and variables → Actions → New repository secret`

#### 1. `GOOGLE_SERVICES_JSON`

The Firebase configuration file content. For CI/CD, you can use a minimal dummy config since unit tests don't need actual Firebase services.

**Option A: Generate minimal config (recommended for CI)**
```bash
# Run the helper script
./scripts/generate-ci-firebase-config.sh

# Copy the generated JSON
cat google-services-ci-template.json | pbcopy  # macOS
cat google-services-ci-template.json | xclip   # Linux
```

**Option B: Use your real google-services.json (if you want Firebase to work in CI)**
```bash
cat app/google-services.json | pbcopy
```

Then:
1. Go to [Repository Secrets](https://github.com/mdpearce/sonic-switcher/settings/secrets/actions)
2. Click "New repository secret"
3. Name: `GOOGLE_SERVICES_JSON`
4. Value: Paste the JSON content
5. Click "Add secret"

**⚠️ Important Notes:**
- This should be valid JSON (the entire file content)
- For CI, a dummy config is fine since tests don't need Firebase
- For production/release workflows, use your real Firebase config
- Never commit `google-services.json` to the repository

---

## Workflow: PR Checks

**File**: `.github/workflows/pr-checks.yml`

**Triggers**: Pull requests to `main` branch

**Jobs**:

### 1. Lint Check
- Runs `ktlintCheck` on all modules
- Fails fast if code style violations detected
- Uploads lint reports as artifacts on failure

### 2. Unit Tests
- Executes all JVM unit tests (`./gradlew test --continue`)
- Continues testing all modules even if one fails
- Uploads test reports and results as artifacts

### 3. Coverage
- Generates Kover coverage reports (XML + HTML)
- Posts coverage summary as PR comment
- Enforces minimum coverage thresholds:
  - Overall: 60%
  - Changed files: 70%
- Only runs if unit tests pass

### 4. PR Check Summary
- Consolidates all job results
- Single required status check for branch protection
- Fails if any upstream job fails

## Branch Protection

**Recommended Settings** (Settings → Branches → main → Add rule):

- ✅ Require a pull request before merging
- ✅ Require status checks to pass before merging
  - Required check: `PR Check Summary`
- ✅ Require branches to be up to date before merging
- ⬜ Do not require reviews for testing purposes (or enable for production)

## Coverage Reporting

### Kover (Kotlin Coverage)

**Why Kover over JaCoCo?**
- Native Kotlin support (better understanding of inline functions, coroutines)
- Faster instrumentation
- Modern Gradle plugin with better caching
- First-class support for multi-module Android projects

**Configuration**: `build.gradle.kts` (root)

**Exclusions**:
- Generated code (Hilt, Room, KSP, Compose)
- Android framework (R classes, BuildConfig)
- Firebase modules

### Local Coverage Reports

```bash
# Generate all reports
./gradlew test koverXmlReport koverHtmlReport

# View HTML report
open build/reports/kover/html/index.html

# Print coverage to console
./gradlew koverLog

# Verify minimum thresholds
./gradlew koverVerify
```

## Cost Optimization

### Free Tier Usage (Public Repos)

GitHub provides **unlimited free minutes** for public repositories on public runners.

**Runner**: `ubuntu-latest`
- Cost: FREE for public repos
- Specs: 2-core CPU, 7 GB RAM, 14 GB SSD
- Good enough for unit tests and ktlint

**Not Used in PR Workflow**:
- macOS runners (needed for instrumentation tests, but expensive)
- Emulators (slow, resource-intensive)

### Caching Strategy

**Gradle Cache**:
```yaml
- uses: gradle/actions/setup-gradle@v4
  with:
    cache-read-only: false
```

This caches:
- Gradle wrapper
- Dependencies (Maven artifacts)
- Build cache (compiled classes, resources)

**Performance Improvement**:
- First run: ~5-8 minutes
- Cached runs: ~2-3 minutes (60-70% faster)

### Concurrency Control

```yaml
concurrency:
  group: ${{ github.workflow }}-${{ github.event.pull_request.number }}
  cancel-in-progress: true
```

Cancels old workflow runs when you push new commits to a PR. Saves wasted compute on outdated code.

## Artifacts

Test reports and coverage reports are uploaded as artifacts and retained for **7-30 days**.

**Access**: GitHub Actions → Workflow Run → Artifacts section

| Artifact | Retention | When Uploaded |
|----------|-----------|---------------|
| ktlint-reports | 7 days | Lint failures |
| test-reports | 7 days | Always |
| test-results | 7 days | Always |
| coverage-reports | 30 days | Coverage job |

## Firebase Configuration for CI

The workflow uses GitHub Secrets to inject the Firebase configuration at build time:

```yaml
- name: Create google-services.json from secrets
  run: echo '${{ secrets.GOOGLE_SERVICES_JSON }}' > app/google-services.json
```

**Why use secrets?**
- ✅ Keeps sensitive data out of repository
- ✅ Different configs for CI vs production
- ✅ Easy to update without code changes
- ✅ Standard security practice

**What gets injected:**
- `google-services.json` file in the `app/` directory
- Created fresh for each workflow run
- Deleted automatically when workflow completes

**For unit tests:** A minimal/dummy Firebase config works fine since:
- Tests don't make actual Firebase API calls
- The Google Services plugin just needs valid JSON structure
- Analytics and Crashlytics are not initialized in tests

---

## Local Validation

**Before pushing a PR**, run the same checks locally:

```bash
# Full CI simulation
./gradlew ktlintCheck test koverXmlReport koverHtmlReport

# Individual checks
./gradlew ktlintCheck          # Lint only
./gradlew test                 # Tests only
./gradlew koverLog             # Coverage summary
```

## Future Enhancements

**Not implemented** (to keep costs low and focus on PR gates):

- ❌ Instrumentation tests (requires emulator)
- ❌ Release builds in CI
- ❌ Automatic deployment to Play Store
- ❌ Performance benchmarking
- ❌ Screenshot testing

These can be added later in separate workflows or manual processes.

## Debugging CI Failures

### Lint Failures

```bash
# Run ktlint locally
./gradlew ktlintCheck

# Auto-fix most issues
./gradlew ktlintFormat
```

### Test Failures

```bash
# Run tests with detailed output
./gradlew test --info --stacktrace

# Run specific test
./gradlew :app:testDebugUnitTest --tests "MainScreenViewModelTest"

# View test report
open app/build/reports/tests/testDebugUnitTest/index.html
```

### Coverage Failures

If coverage is below threshold:

```bash
# Check current coverage
./gradlew koverLog

# See detailed HTML report
./gradlew koverHtmlReport
open build/reports/kover/html/index.html
```

Add tests for uncovered code, then verify:

```bash
./gradlew test koverVerify
```

## Troubleshooting

### "google-services.json not found"

The CI workflow auto-creates this file. Locally, you need a real one (see `local.properties.sample`).

### Gradle daemon issues

CI runs use `--no-daemon` implicitly. If you see issues locally:

```bash
./gradlew --stop  # Kill daemon
./gradlew clean   # Clean build
```

### Timeout issues

Default timeout: 15 minutes for tests, 10 minutes for lint

If exceeded, check for:
- Infinite loops in tests
- Missing `runTest` wrapper for coroutines
- Deadlocks in concurrent code

## Monitoring

**GitHub Insights → Actions**:
- View workflow run times
- Identify slow jobs
- Track success/failure rates

**Branch protection insights**:
- See how often checks fail
- Identify flaky tests

## References

- [GitHub Actions Documentation](https://docs.github.com/en/actions)
- [Gradle Actions Setup](https://github.com/gradle/actions)
- [Kover Documentation](https://kotlin.github.io/kotlinx-kover/)
- [JaCoCo Report Action](https://github.com/madrapps/jacoco-report) (works with Kover XML)
