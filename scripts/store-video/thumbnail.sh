#!/usr/bin/env bash
# The YouTube thumbnails of one locale's store video (docs/store-video.md), from its recording's stills:
#
#   scripts/store-video/thumbnail.sh <locale>
#
# writes build/store-video/<locale>/thumb-<locale>-16x9.{png,jpg} (1280x720) and -9x16 (1080x1920, the one YouTube
# shows for a vertical video). The words and stills are the locale JSON's `thumbnail`; the name, and the chips unless
# `thumbnail.chips` gives others, are its end card's.
set -euo pipefail

HERE="$(cd "$(dirname "$0")" && pwd)"
ROOT="$(cd "$HERE/../.." && pwd)"
LOCALE=${1:?usage: thumbnail.sh <locale>}
JSON="$HERE/locales/$LOCALE.json"
OUT="$ROOT/build/store-video/$LOCALE"
PUBLIC="$OUT/public"
[[ -f "$JSON" ]] || { echo "No scripts/store-video/locales/$LOCALE.json" >&2; exit 1; }

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
for size in Thumbnail:16x9 ThumbnailVertical:9x16; do
  png="$OUT/thumb-$LOCALE-${size#*:}.png"
  npx remotion still src/Thumbnail.tsx "${size%%:*}" "$png" --props="$OUT/thumbnail-props.json" \
    --public-dir="$PUBLIC" --log=error
  ffmpeg -y -v error -i "$png" -q:v 2 "${png%.png}.jpg"
  echo "$png"
done
