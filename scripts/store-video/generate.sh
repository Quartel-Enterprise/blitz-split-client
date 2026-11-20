#!/bin/zsh
# The store preview video of one locale, from scratch (docs/store-video.md):
#
#   scripts/store-video/generate.sh <locale> [--skip-record] [scene ...]
#
# 1. records the app's scenes on the running Android emulator (a debug build installed), unless --skip-record; naming
#    scenes records only those;
# 2. asks ElevenLabs for the narration's takes and the music, only those not generated yet (they cost credits);
# 3. composes the video with Remotion into build/store-video/<locale>/store-video-<locale>.mp4.
set -euo pipefail

HERE=${0:A:h}
ROOT=${HERE:h:h}
LOCALE=${1:?usage: generate.sh <locale> [--skip-record] [scene ...]}
shift
RECORD=1
if [[ ${1:-} == --skip-record ]]; then
  RECORD=0
  shift
  (( $# == 0 )) || { echo "--skip-record records nothing, so it takes no scenes: $*" >&2; exit 1; }
fi
OUT=$ROOT/build/store-video/$LOCALE
PUBLIC=$OUT/public
[[ -f $HERE/locales/$LOCALE.json ]] || { echo "No scripts/store-video/locales/$LOCALE.json" >&2; exit 1; }
for tool in adb ffmpeg ffprobe node npm python3; do
  command -v $tool >/dev/null || { echo "$tool is missing (docs/store-video.md)" >&2; exit 1; }
done
mkdir -p $PUBLIC

# The re-timing needs numpy and Pillow, in an environment of their own.
VENV=$ROOT/build/store-video/.venv
if [[ ! -x $VENV/bin/python ]]; then
  python3 -m venv $VENV
  $VENV/bin/pip install --quiet --disable-pip-version-check -r $HERE/requirements.txt
fi

if (( RECORD )); then
  adb get-state >/dev/null 2>&1 || { echo "No emulator is running" >&2; exit 1; }
  $VENV/bin/python -I $HERE/record.py $LOCALE "$@"
fi

$VENV/bin/python -I $HERE/elevenlabs.py narration $LOCALE
$VENV/bin/python -I $HERE/elevenlabs.py music $LOCALE
$VENV/bin/python -I $HERE/elevenlabs.py prepare $LOCALE

# The end card's icon and the two sound effects, made here so no binary lives in the repository.
cp $ROOT/iosApp/Resources/Assets.xcassets/AppIcon.appiconset/icon-1024.png $PUBLIC/icon.png
ffmpeg -y -v error -f lavfi -i "anoisesrc=d=0.55:c=pink:a=0.6:seed=7" \
  -af "highpass=f=250,lowpass=f=3500,afade=t=in:d=0.3:curve=exp,afade=t=out:st=0.3:d=0.25,volume=0.5,aformat=channel_layouts=stereo" \
  $PUBLIC/whoosh.wav
ffmpeg -y -v error -f lavfi -i "sine=f=660:d=0.18" -f lavfi -i "sine=f=1320:d=0.18" \
  -filter_complex "[0][1]amix=2,afade=t=out:d=0.17:curve=exp,volume=0.35,aformat=channel_layouts=stereo" $PUBLIC/pop.wav

cd $HERE/remotion
[[ -d node_modules ]] || npm ci --no-audit --no-fund
npx remotion render src/index.ts StoreVideo $OUT/raw.mp4 --props=$OUT/props.json --public-dir=$PUBLIC \
  --codec=h264 --crf=16 --log=error
# -14 LUFS, YouTube's loudness, where the Play listing's video lives.
ffmpeg -y -v error -i $OUT/raw.mp4 -c:v copy -af "loudnorm=I=-14:TP=-1.5:LRA=9" -c:a aac -b:a 192k -ar 48000 \
  $OUT/store-video-$LOCALE.mp4
rm $OUT/raw.mp4
echo "$OUT/store-video-$LOCALE.mp4"
