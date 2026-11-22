#!/usr/bin/env bash
# The YouTube thumbnails and the Play feature graphic of one locale's store video (docs/store-video.md), from its
# recording's stills:
#
#   scripts/store-video/thumbnail.sh <locale>
#
# writes build/store-video/<locale>/thumb-<locale>-16x9.{png,jpg} (1280x720) and -9x16 (1080x1920, the one YouTube
# shows for a vertical video), and feature-<locale>.{png,jpg} (1024x500, Play's feature graphic, which
# `fastlane upload_feature_graphics` uploads as the JPEG: Play refuses a PNG with an alpha channel). The words and
# stills are the locale JSON's `thumbnail`; the name, and the chips unless `thumbnail.chips` gives others, are its
# end card's.
set -euo pipefail

HERE="$(cd "$(dirname "$0")" && pwd)"
ROOT="$(cd "$HERE/../.." && pwd)"
LOCALE=${1:?usage: thumbnail.sh <locale>}
JSON="$HERE/locales/$LOCALE.json"
OUT="$ROOT/build/store-video/$LOCALE"
PUBLIC="$OUT/public"
[[ -f "$JSON" ]] || { echo "No scripts/store-video/locales/$LOCALE.json" >&2; exit 1; }
for tool in ffmpeg node npm; do
  command -v "$tool" >/dev/null || { echo "$tool is missing (docs/store-video.md)" >&2; exit 1; }
done

node -e '
  const fs = require("fs");
  const [json, publicDir, out] = process.argv.slice(1);
  const { endCard, thumbnail } = JSON.parse(fs.readFileSync(json, "utf8"));
  if (!thumbnail) throw new Error(`${json} has no thumbnail`);
  for (const still of [thumbnail.back, thumbnail.front]) {
    if (!fs.existsSync(`${publicDir}/${still}`)) throw new Error(`${still} is not recorded yet: run generate.sh first`);
  }
  const props = { ...thumbnail, name: endCard.name, chips: thumbnail.chips ?? endCard.chips };
  fs.writeFileSync(out, JSON.stringify(props));
' "$JSON" "$PUBLIC" "$OUT/thumbnail-props.json"

cp "$ROOT/iosApp/Resources/Assets.xcassets/AppIcon.appiconset/icon-1024.png" "$PUBLIC/icon.png"
cd "$HERE/remotion"
[[ -d node_modules ]] || npm ci --no-audit --no-fund
# One bundle for the three stills, instead of each still bundling the project again
BUNDLE="$OUT/thumbnail-bundle"
rm -rf "$BUNDLE"
npx remotion bundle src/thumbnail-index.ts --out-dir="$BUNDLE" --public-dir="$PUBLIC" --log=error
for size in Thumbnail:thumb-$LOCALE-16x9 ThumbnailVertical:thumb-$LOCALE-9x16 FeatureGraphic:feature-$LOCALE; do
  png="$OUT/${size#*:}.png"
  npx remotion still "$BUNDLE" "${size%%:*}" "$png" --props="$OUT/thumbnail-props.json" --log=error
  ffmpeg -y -v error -i "$png" -q:v 2 "${png%.png}.jpg"
  echo "$png"
done
