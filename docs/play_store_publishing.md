# Play Store Publishing Setup

This project uses the [Gradle Play Publisher plugin](https://github.com/Triple-T/gradle-play-publisher) to automate publishing to the Google Play Store.

## Initial Setup

### 1. Create a Service Account

1. Go to [Google Play Console](https://play.google.com/console/)
2. Navigate to **Setup → API access**
3. Click **Create new service account**
4. Follow the link to Google Cloud Console
5. Create a new service account with these details:
   - Name: `sonic-switcher-publisher` (or your preferred name)
   - Role: **Service Account User**
6. Create a JSON key for the service account
7. Download the JSON key file

### 2. Grant Permissions

Back in the Google Play Console:

1. Go to **Setup → API access**
2. Find your newly created service account
3. Click **Grant access**
4. Set permissions:
   - **Account permissions**: None needed
   - **App permissions**: Select "Sonic Switcher"
   - Grant these permissions:
     - View app information and download bulk reports (read-only)
     - **Release to internal testing track** ✓
     - Release to production, exclude devices, and use Play App Signing ✓
     - Manage testing tracks and edit tester lists ✓
5. Click **Invite user** (or **Apply**)

### 3. Add Credentials to Project

1. Rename the downloaded JSON file to `play-store-credentials.json`
2. Place it in the `app/` directory: `app/play-store-credentials.json`
3. **IMPORTANT**: This file contains sensitive credentials. It's already in `.gitignore` to prevent accidental commits.

⚠️ **Never commit the credentials file to version control!**

## Publishing Releases

### Quick Publish to Internal Track

```bash
./gradlew publishReleaseBundle
```

This will:
- Build the release AAB (Android App Bundle)
- Upload to the **internal testing track** (default)
- Set release status to **completed**

### Publish to Different Tracks

Override the default track with the `--track` parameter:

```bash
# Internal testing (default)
./gradlew publishReleaseBundle --track internal

# Alpha testing
./gradlew publishReleaseBundle --track alpha

# Beta testing
./gradlew publishReleaseBundle --track beta

# Production
./gradlew publishReleaseBundle --track production
```

### Publish APK Instead of Bundle

```bash
./gradlew publishReleaseApk
```

### Promote Between Tracks

```bash
# Promote from internal to alpha
./gradlew promoteArtifact --from-track internal --promote-track alpha

# Promote from alpha to beta
./gradlew promoteArtifact --from-track alpha --promote-track beta

# Promote from beta to production
./gradlew promoteArtifact --from-track beta --promote-track production
```

## Useful Commands

### Publish as Draft

```bash
./gradlew publishReleaseBundle --release-status draft
```

### Bootstrap Metadata

Download existing metadata from Play Console:

```bash
./gradlew bootstrap
```

This creates a `app/src/main/play/` directory with:
- Store listing (descriptions, screenshots)
- Release notes
- Graphics assets

You can edit these files locally and they'll be uploaded with the next publish.

### Validate Without Publishing

```bash
./gradlew validateReleaseBundle
```

## Release Notes

### Option 1: Per-Release Notes

Create `app/src/main/play/release-notes/en-US/default.txt`:

```
- Bug fixes and performance improvements
- Added new audio format support
- Improved conversion speed
```

### Option 2: Inline Release Notes

In `app/build.gradle.kts`, add to the `play` block:

```kotlin
play {
    // ... existing config ...
    releaseNotes.set(mapOf(
        "en-US" to "Bug fixes and performance improvements"
    ))
}
```

## Troubleshooting

### "Service account not found" Error

Ensure the service account has been granted access in Play Console (Setup → API access).

### "Version code X has already been used"

Increment `versionCode` in `app/build.gradle.kts` before publishing.

### "The bundle is not signed"

Verify your signing configuration in `local.properties`:
- `RELEASE_KEYSTORE_PATH`
- `RELEASE_KEYSTORE_PASSWORD`
- `RELEASE_KEY_ALIAS`
- `RELEASE_KEY_PASSWORD`

### "Track not found"

Create the track in Play Console first:
- Go to **Testing → Internal testing** (or Alpha/Beta)
- Create the track if it doesn't exist

## Advanced Configuration

### Custom Track Names

You can create custom tracks (e.g., "qa", "staging"):

```kotlin
play {
    track.set("staging")
}
```

### Update Priority

Set in-app update priority (0-5):

```kotlin
play {
    updatePriority.set(3)  // Medium priority
}
```

### Staged Rollouts

Gradually release to production:

```bash
./gradlew publishReleaseBundle --track production --user-fraction 0.1
```

Then increase gradually:

```bash
./gradlew publishReleaseBundle --track production --user-fraction 0.5
./gradlew publishReleaseBundle --track production --user-fraction 1.0
```

## CI/CD Integration

For automated publishing from CI:

1. Store `play-store-credentials.json` as a secret/encrypted file
2. Set up a secure CI pipeline
3. Run publishing commands in your release workflow

Example GitHub Actions:

```yaml
- name: Publish to Play Store
  env:
    PLAY_CREDENTIALS: ${{ secrets.PLAY_STORE_CREDENTIALS }}
  run: |
    echo "$PLAY_CREDENTIALS" > app/play-store-credentials.json
    ./gradlew publishReleaseBundle
```

## Reference

- [Plugin Documentation](https://github.com/Triple-T/gradle-play-publisher)
- [Google Play Console](https://play.google.com/console/)
- [Play Developer API](https://developers.google.com/android-publisher)
