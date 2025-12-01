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

### RELEASE_KEYSTORE

**Purpose**: Android app signing keystore for release builds

**When needed**: Only for release/production builds (not PR checks)

**Setup**: Will be documented when release workflow is implemented

### PLAY_STORE_SERVICE_ACCOUNT

**Purpose**: Google Play Console API access for automated publishing

**When needed**: Only for automated Play Store deployments

**Setup**: Will be documented when deployment workflow is implemented

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
