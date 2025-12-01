#!/bin/bash

# Helper script to create a minimal google-services.json for CI/CD
# This generates a valid JSON structure that satisfies the Google Services plugin
# but uses dummy/test values that won't work for actual Firebase features.

cat > google-services-ci-template.json << 'EOF'
{
  "project_info": {
    "project_number": "000000000000",
    "project_id": "sonic-switcher-ci-test",
    "storage_bucket": "sonic-switcher-ci-test.appspot.com"
  },
  "client": [
    {
      "client_info": {
        "mobilesdk_app_id": "1:000000000000:android:0000000000000000000000",
        "android_client_info": {
          "package_name": "io.github.mdpearce.sonicswitcher"
        }
      },
      "oauth_client": [],
      "api_key": [
        {
          "current_key": "AIzaSyDummyKeyForCITestingOnly-NotReal"
        }
      ],
      "services": {
        "appinvite_service": {
          "other_platform_oauth_client": []
        }
      }
    }
  ],
  "configuration_version": "1"
}
EOF

echo "✅ Created google-services-ci-template.json"
echo ""
echo "To use this as a GitHub Actions secret:"
echo "1. Copy the contents: cat google-services-ci-template.json | pbcopy"
echo "2. Go to: https://github.com/mdpearce/sonic-switcher/settings/secrets/actions"
echo "3. Create new secret named: GOOGLE_SERVICES_JSON"
echo "4. Paste the JSON content"
echo ""
echo "⚠️  This is a dummy config for CI - Firebase features won't work"
echo "   For production builds, use your real google-services.json"
