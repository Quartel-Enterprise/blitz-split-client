#!/usr/bin/env bash
#
# Show the signing certificate. Run by .github/workflows/release-production.yml.
#
set -eo pipefail

shopt -s nullglob
aabs=()
for app in androidApp wearApp; do
  found=("$app"/build/outputs/bundle/release/*.aab)
  if [ "${#found[@]}" -ne 1 ]; then
    echo "::error::Expected one release AAB of $app, found ${#found[@]}."
    exit 1
  fi
  aabs+=("${found[0]}")
done
if ! keystore="$(keytool -list -v -keystore "$RUNNER_TEMP/release.jks" -alias "$ANDROID_KEY_ALIAS" \
  -storepass "$ANDROID_KEYSTORE_PASSWORD" 2>&1)"; then
  echo "$keystore"
  echo "::error::keytool could not read the release keystore."
  exit 1
fi
digest() { sed -n "s/^[[:space:]]*$1: *//p" <<< "$2" | head -1; }
sha1="$(digest SHA1 "$keystore")"
sha256="$(digest SHA256 "$keystore")"
if [ -z "$sha1" ] || [ -z "$sha256" ]; then
  echo "$keystore"
  echo "::error::keytool printed no SHA-1 or SHA-256 for the release key."
  exit 1
fi
# The watch's too: the Data Layer only links the phone's and the watch's apps signed by one key.
for aab in "${aabs[@]}"; do
  if ! bundle="$(keytool -printcert -jarfile "$aab" 2>&1)" || [ "$(digest SHA256 "$bundle")" != "$sha256" ]; then
    echo "$bundle"
    echo "::error::$aab is not signed with the release keystore's certificate (SHA-256 $sha256)."
    exit 1
  fi
done
echo "::notice title=AAB signing SHA-1::$sha1"
{
  echo "### AAB signing certificate"
  echo ""
  echo "SHA-1: \`$sha1\`"
  echo ""
  echo "SHA-256: \`$sha256\`"
  echo ""
  echo "Google Play takes the bundles, the phone's and the watch's, only when this is their upload key certificate (Play Console > Test and release > App integrity)."
} >> "$GITHUB_STEP_SUMMARY"
