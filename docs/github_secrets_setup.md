# GitHub Actions Secrets Setup Guide

## Overview

This guide walks you through setting up all required secrets for the Sonic Switcher CI/CD pipeline.

## Required Secrets

### 1. GOOGLE_SERVICES_JSON

**Purpose**: Firebase configuration for Google Services plugin

**When needed**: Every build (PR checks, releases)

**How to set up**:

#### Step 1: Generate the JSON content

Choose **Option A** for CI/testing or **Option B** for production builds.

**Option A: Minimal CI Config (Recommended)**

This satisfies the build requirements without exposing real Firebase credentials:

```bash
# Run the helper script from project root
./scripts/generate-ci-firebase-config.sh

# The script creates: google-services-ci-template.json
# Copy to clipboard
cat google-services-ci-template.json | pbcopy  # macOS
cat google-services-ci-template.json | xclip -selection clipboard  # Linux
```

**Option B: Real Firebase Config**

Use this if you need actual Firebase features in CI (not needed for unit tests):

```bash
# Copy your real google-services.json
cat app/google-services.json | pbcopy  # macOS
cat app/google-services.json | xclip -selection clipboard  # Linux
```

#### Step 2: Add to GitHub

1. Go to your repository on GitHub
2. Navigate to: **Settings** → **Secrets and variables** → **Actions**
3. Click **"New repository secret"**
4. Enter details:
   - **Name**: `GOOGLE_SERVICES_JSON`
   - **Secret**: Paste the JSON content from Step 1
5. Click **"Add secret"**

#### Verification

The secret should appear in your secrets list (the value will be hidden).

---

## Future Secrets (Not Yet Required)

These are not currently needed but may be added for future workflows:

### RELEASE_KEYSTORE_BASE64

**Purpose**: Android app signing keystore (upload key) encoded as base64

**When needed**: Release workflow for building signed bundles

**How to set up**:

#### Step 1: Encode your keystore

```bash
# Encode your existing keystore to base64
cat /path/to/your-upload-key.jks | base64 | pbcopy  # macOS
cat /path/to/your-upload-key.jks | base64 -w 0 | xclip -selection clipboard  # Linux
```

**Note**: `pbcopy` (macOS) and `xclip` (Linux) are clipboard utilities. On Linux, install `xclip` via your package manager if not already installed (`sudo apt install xclip` or `sudo yum install xclip`).

#### Step 2: Add to GitHub

1. Go to: **Settings** → **Secrets and variables** → **Actions**
2. Click **"New repository secret"**
3. Name: `RELEASE_KEYSTORE_BASE64`
4. Value: Paste the base64 string
5. Click **"Add secret"**

### RELEASE_KEYSTORE_PASSWORD

**Purpose**: Password for the release keystore

**When needed**: Release workflow

**Setup**:
1. Name: `RELEASE_KEYSTORE_PASSWORD`
2. Value: Your keystore password

### RELEASE_KEY_ALIAS

**Purpose**: Alias of the key in the keystore

**When needed**: Release workflow

**Setup**:
1. Name: `RELEASE_KEY_ALIAS`
2. Value: Your key alias (e.g., `upload`)

### RELEASE_KEY_PASSWORD

**Purpose**: Password for the specific key

**When needed**: Release workflow

**Setup**:
1. Name: `RELEASE_KEY_PASSWORD`
2. Value: Your key password (often same as keystore password)

### PLAY_STORE_SERVICE_ACCOUNT_JSON

**Purpose**: Google Play Console API access for automated publishing

**When needed**: Release workflow for Play Store uploads

**How to set up**:

#### Step 1: Create service account

1. Go to [Google Play Console](https://play.google.com/console)
2. Navigate to: **Setup → API access**
3. Click **"Create new service account"**
4. Follow the link to Google Cloud Console
5. Create a new service account with **"Service Account User"** role
6. Create a JSON key for the service account
7. Download the JSON file

#### Step 2: Grant permissions

1. Return to Play Console → API access
2. Find your service account and click **"Grant access"**
3. Add to **"Internal testing"** track with **"Release manager"** role
4. Click **"Apply"**

#### Step 3: Add to GitHub

```bash
# Copy the JSON file content
cat /path/to/service-account.json | pbcopy  # macOS
cat /path/to/service-account.json | xclip   # Linux
```

1. Go to: **Settings** → **Secrets and variables** → **Actions**
2. Click **"New repository secret"**
3. Name: `PLAY_STORE_SERVICE_ACCOUNT_JSON`
4. Value: Paste the entire JSON content
5. Click **"Add secret"**

**Note**: The play-publisher plugin can also use a file, but for CI/CD it's better to use secrets.

---

## Security Best Practices

### Do's ✅

- ✅ Use GitHub Secrets for all sensitive data
- ✅ Use minimal/dummy configs for CI when possible
- ✅ Rotate secrets periodically (every 6-12 months)
- ✅ Use different Firebase projects for dev/staging/prod
- ✅ Review secret usage in workflow files before approving PRs

### Don'ts ❌

- ❌ Never commit `google-services.json` to the repository
- ❌ Never print secrets in workflow logs (`echo ${{ secrets.* }}`)
- ❌ Never use production Firebase credentials for CI
- ❌ Never share secrets via Slack, email, etc.
- ❌ Don't give more access than needed (principle of least privilege)

---

## Troubleshooting

### Secret not found error

**Error**: `Context access might be invalid: GOOGLE_SERVICES_JSON`

**Solution**: 
1. Check secret name is exactly `GOOGLE_SERVICES_JSON` (case-sensitive)
2. Verify secret is set in **Actions** secrets (not Dependabot or Codespaces)
3. Re-run workflow after adding secret

### Invalid JSON error

**Error**: `Could not find google-services.json` or JSON parsing error

**Solution**:
1. Verify the secret contains valid JSON (use JSONLint.com)
2. Ensure entire file content is copied (including `{` and `}`)
3. Check for hidden characters or encoding issues
4. Use the helper script to generate known-good JSON

### Workflow can't read secret

**Error**: Secret appears empty in workflow

**Solution**:
1. Secrets are only available to workflows in the same repository
2. Forked repositories don't have access to upstream secrets
3. Secret names are case-sensitive
4. Check repository settings → Actions → General → "Fork pull request workflows"

### Secret needs updating

**To update an existing secret**:
1. Go to Settings → Secrets and variables → Actions
2. Click the secret name
3. Click "Update secret"
4. Paste new value
5. Click "Update secret"

**Note**: Updating a secret doesn't re-run old workflows. Re-run manually if needed.

---

## Verification Checklist

Before pushing a PR, verify:

- [ ] `GOOGLE_SERVICES_JSON` secret is set in GitHub
- [ ] Secret contains valid JSON (starts with `{`, ends with `}`)
- [ ] You can see the secret in Settings → Secrets and variables → Actions
- [ ] Test workflow runs successfully

---

## Quick Reference

### Secret URLs

- **Actions Secrets**: `https://github.com/mdpearce/sonic-switcher/settings/secrets/actions`
- **Workflow Runs**: `https://github.com/mdpearce/sonic-switcher/actions`

### Helper Scripts

- Generate CI Firebase config: `./scripts/generate-ci-firebase-config.sh`
- Validate CI locally: `./scripts/validate-ci.sh`

### Workflow Files

- PR Checks: `.github/workflows/pr-checks.yml`

---

## Additional Resources

- [GitHub Actions: Encrypted Secrets](https://docs.github.com/en/actions/security-guides/encrypted-secrets)
- [Firebase: google-services.json](https://firebase.google.com/docs/android/setup#add-config-file)
- [Android: App Signing](https://developer.android.com/studio/publish/app-signing)
