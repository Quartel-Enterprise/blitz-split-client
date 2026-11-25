#!/usr/bin/env bash
#
# Show the signing certificate. Run by .github/workflows/release-beta.yml.
#
set -eo pipefail

apksigner="$(find "$ANDROID_HOME/build-tools" -name apksigner -type f | sort -V | tail -1)"
shopt -s nullglob
apks=(androidApp/build/outputs/apk/release/*.apk)
if [ "${#apks[@]}" -ne 1 ]; then
  echo "::error::Expected one release APK, found ${#apks[@]}."
  exit 1
fi
apk="${apks[0]}"
if ! keystore="$(keytool -list -v -keystore "$RUNNER_TEMP/release.jks" -alias "$ANDROID_KEY_ALIAS" \
  -storepass "$ANDROID_KEYSTORE_PASSWORD" 2>&1)"; then
  echo "$keystore"
  echo "::error::keytool could not read the release keystore."
  exit 1
fi
hex() { sed -n "s/^[[:space:]]*$1: *//p" <<< "$keystore" | head -1 | tr -d ':' | tr 'A-F' 'a-f'; }
sha1="$(hex SHA1)"
sha256="$(hex SHA256)"
if [ -z "$sha1" ] || [ -z "$sha256" ]; then
  echo "$keystore"
  echo "::error::keytool printed no SHA-1 or SHA-256 for the release key."
  exit 1
fi
if ! certs="$("$apksigner" verify --print-certs "$apk" 2>&1)" || ! grep -qiF "$sha256" <<< "$certs"; then
  echo "apksigner ${apksigner} printed:"
  echo "$certs"
  echo "::error::The release APK is not signed with the release keystore's certificate (SHA-256 $sha256)."
  exit 1
fi
# assetlinks.json lists AB:CD:…
fingerprint="$(sed 's/../&:/g; s/:$//' <<< "${sha256^^}")"
# The package the APK declares, so the check follows the applicationId.
package="$("$(dirname "$apksigner")/aapt2" dump packagename "$apk")"
if ! app_links="$(./scripts/release/app-links-status.sh "$package" "$fingerprint")"; then
  echo "::warning title=App Links::$app_links"
fi
echo "::notice title=APK signing SHA-1::$sha1"
echo "::notice title=APK signing SHA-256::$fingerprint"
{
  echo "### APK signing certificate"
  echo ""
  echo "SHA-1: \`$sha1\`"
  echo ""
  echo "SHA-256: \`$fingerprint\`"
  echo ""
  echo "Google sign in needs the SHA-1 on the Firebase Android app. $app_links"
} >> "$GITHUB_STEP_SUMMARY"
