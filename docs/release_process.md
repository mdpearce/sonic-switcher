# Release Process

## Overview

Sonic Switcher uses an automated GitHub Actions workflow to build and release the app to Google Play Store. The workflow handles version bumping, signing, uploading, tagging, and creating a PR to track the version change.

## Prerequisites

Before you can run releases, you need to set up the following GitHub Secrets:

### Required Secrets

1. **`RELEASE_KEYSTORE_BASE64`** - Your upload keystore encoded as base64
2. **`RELEASE_KEYSTORE_PASSWORD`** - Keystore password
3. **`RELEASE_KEY_ALIAS`** - Key alias in the keystore
4. **`RELEASE_KEY_PASSWORD`** - Key password
5. **`PLAY_STORE_SERVICE_ACCOUNT_JSON`** - Google Play Console service account JSON
6. **`GOOGLE_SERVICES_JSON`** - Firebase configuration (already set up for CI)

See [GitHub Secrets Setup Guide](github_secrets_setup.md) for detailed instructions on creating these secrets.

---

## How to Create a Release

### Step 1: Prepare the Release

1. **Ensure main branch is clean** - All PRs should be merged
2. **Update versionName** (if needed) - Create a PR to bump `versionName` in `app/build.gradle.kts`
3. **Test thoroughly** - All tests should pass on main

### Step 2: Trigger the Release Workflow

1. Go to [GitHub Actions](https://github.com/mdpearce/sonic-switcher/actions)
2. Click on **"Release to Play Store"** workflow
3. Click **"Run workflow"**
4. Select options:
   - **Branch**: `main` (always release from main)
   - **Play Store track**: Choose where to publish
     - `internal` - Internal testing (recommended for first releases)
     - `alpha` - Alpha testing
     - `beta` - Beta testing
     - `production` - Production release
5. Click **"Run workflow"**

### Step 3: What Happens Automatically

The workflow will:

1. ✅ **Bump versionCode** - Increments by 1 (e.g., 12 → 13)
2. ✅ **Decode signing key** - Extracts keystore from secrets
3. ✅ **Build signed bundle** - Creates release `.aab` file
4. ✅ **Generate release notes** - From git commits since last tag
5. ✅ **Upload to Play Store** - Publishes to selected track
6. ✅ **Create git tag** - Tags the release (e.g., `v0.0.1-SNAPSHOT-build.13`)
7. ✅ **Create PR** - Opens PR with versionCode bump
8. ✅ **Upload artifact** - Saves bundle to GitHub Actions artifacts

### Step 4: Review and Merge

1. **Check workflow status** - Ensure all steps completed successfully
2. **Review the PR** - Automated PR with versionCode bump
3. **Merge the PR** - Keeps version in sync with Play Store

---

## Release Tracks

### Internal Testing
- **Purpose**: Team testing, QA validation
- **Rollout**: Immediate to internal testers
- **Use when**: Testing new features before wider release

### Alpha Testing
- **Purpose**: Early adopters, closed testing group
- **Rollout**: Managed rollout to alpha testers
- **Use when**: Feature complete, needs user feedback

### Beta Testing
- **Purpose**: Open or closed beta program
- **Rollout**: Wider audience for pre-release testing
- **Use when**: Nearly production-ready, final validation

### Production
- **Purpose**: Public release
- **Rollout**: Staged rollout to all users
- **Use when**: Fully tested and ready for public

---

## Version Numbering

### versionName (Manual)
- **Format**: `MAJOR.MINOR.PATCH[-SUFFIX]`
- **Example**: `0.0.1-SNAPSHOT`, `1.0.0`, `1.2.3-beta`
- **Update manually** via PR before release
- **Follows**: Semantic versioning

### versionCode (Automatic)
- **Format**: Integer that increments
- **Example**: `12`, `13`, `14`
- **Auto-incremented** by release workflow
- **Never decreases** - Play Store requirement

### Git Tags
- **Format**: `v{versionName}-build.{versionCode}`
- **Example**: `v0.0.1-SNAPSHOT-build.13`
- **Created automatically** by release workflow
- **Marks release commits** for easy reference

---

## Release Notes

Release notes are **automatically generated** from git commit messages since the last tag.

### Best Practices for Commit Messages

Use conventional commits for better release notes:

```bash
feat: Add dark mode support
fix: Resolve crash when selecting large files
perf: Improve conversion speed by 20%
docs: Update README with new features
```

The workflow will:
- Extract commits since last release
- Format as bullet list
- Include in Play Store release notes
- Show in PR description

### Manual Release Notes (Optional)

To add custom release notes:
1. Edit the workflow run after it completes
2. Go to Play Console → Release management
3. Edit the release notes manually

---

## Troubleshooting

### Workflow fails at "Decode keystore"

**Problem**: Invalid base64 encoding or missing secret

**Solution**:
```bash
# Re-encode your keystore
base64 -i your-upload-key.jks | pbcopy
# Update RELEASE_KEYSTORE_BASE64 secret in GitHub
```

### Workflow fails at "Build release bundle"

**Problem**: Signing configuration error

**Solution**:
- Verify all 4 release secrets are set correctly
- Check keystore alias matches your actual keystore
- Ensure passwords are correct

### Workflow fails at "Upload to Play Store"

**Problem**: Service account permissions or invalid JSON

**Solution**:
1. Go to Play Console → Setup → API access
2. Verify service account has "Internal testing" access
3. Re-download service account JSON
4. Update `PLAY_STORE_SERVICE_ACCOUNT_JSON` secret

### Release uploaded but not visible

**Problem**: Wrong track or release status

**Solution**:
- Check Play Console → Release management → [Your Track]
- Releases on internal track require tester list to be configured
- Check workflow logs for actual track used

### Version already exists

**Problem**: versionCode already used in Play Store

**Solution**:
- Manually bump versionCode in `app/build.gradle.kts`
- Commit to main
- Re-run release workflow

---

## Manual Release (Fallback)

If the workflow fails, you can release manually:

### Option 1: Local Build & Upload

```bash
# 1. Bump version
./scripts/bump-version-code.sh

# 2. Build bundle
./gradlew bundleRelease

# 3. Upload manually via Play Console
# Go to Play Console → Release management → Create new release
# Upload: app/build/outputs/bundle/release/app-release.aab

# 4. Tag the release
git tag v0.0.1-SNAPSHOT-build.13
git push origin v0.0.1-SNAPSHOT-build.13

# 5. Create PR with version bump
git checkout -b release/version-bump-13
git add app/build.gradle.kts
git commit -m "chore: Bump versionCode to 13"
git push origin release/version-bump-13
# Create PR manually on GitHub
```

### Option 2: Gradle Play Publisher

```bash
# One command to publish (requires play-store-credentials.json)
./gradlew publishBundle --track=internal
```

---

## Security Best Practices

### Do's ✅

- ✅ Always release from `main` branch
- ✅ Keep signing keys in GitHub Secrets (never in repo)
- ✅ Use separate keystores for debug and release
- ✅ Back up your release keystore securely
- ✅ Rotate service account keys annually
- ✅ Review release notes before publishing

### Don'ts ❌

- ❌ Never commit keystores to the repository
- ❌ Never share keystore passwords in Slack/email
- ❌ Never release from feature branches
- ❌ Never skip version bumps
- ❌ Never reuse version codes
- ❌ Never delete release tags

---

## Monitoring Releases

### GitHub Actions
- View workflow runs: `https://github.com/mdpearce/sonic-switcher/actions`
- Download build artifacts (AAB files)
- Check release summaries

### Google Play Console
- View release status: Play Console → Release management
- Monitor rollout progress
- Check crash reports and ANRs
- Review user feedback

### Git Tags
```bash
# List all releases
git tag -l "v*"

# Show release details
git show v0.0.1-SNAPSHOT-build.13

# Checkout a specific release
git checkout v0.0.1-SNAPSHOT-build.13
```

---

## Quick Reference

### Workflow File
`.github/workflows/release.yml`

### Scripts
- `scripts/bump-version-code.sh` - Auto-increment versionCode

### Version Files
- `app/build.gradle.kts` - versionCode and versionName

### Play Console
- Internal testing: https://play.google.com/console → Testing → Internal testing
- Release management: https://play.google.com/console → Release management

### Useful Commands
```bash
# Check current version
grep -E '(versionCode|versionName)' app/build.gradle.kts

# List releases
git tag -l "v*" | sort -V

# Manual version bump
./scripts/bump-version-code.sh
```

---

## Changelog

The project maintains version history through:
- **Git tags** - Every release is tagged
- **Git commit messages** - Automatic release notes
- **PRs** - Version bump PRs track each release
- **Play Store** - Release notes visible to users

Future enhancement: Consider maintaining a `CHANGELOG.md` file.
