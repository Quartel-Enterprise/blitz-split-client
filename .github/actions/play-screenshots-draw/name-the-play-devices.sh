#!/usr/bin/env bash
#
# Name the Play devices. Run by .github/actions/play-screenshots-draw/action.yml.
#
set -eo pipefail

devices="${DEVICES:-play_phone,play_tablet_7,play_tablet_10,wear}"
devices="${devices// /}"
# Refused here, before the drawing, as stage-play.mjs would refuse it after
if [[ ! "$devices" =~ ^(play_phone|play_tablet_7|play_tablet_10|wear)(,(play_phone|play_tablet_7|play_tablet_10|wear))*$ ]]; then
  echo "::error::devices names image types of play_phone, play_tablet_7, play_tablet_10 and wear, with commas: $DEVICES"
  exit 1
fi
art="$(tr ',' '\n' <<< "$devices" | grep '^play_' | paste -sd, - || true)"
{
  echo "ART_DEVICES=$art"
  echo "PLAY_DEVICES=$devices"
  echo "ART_LOCALES=$(tr -s ' ,' ',,' <<< "$LOCALES" | sed 's/^,//; s/,$//')"
} >> "$GITHUB_ENV"
echo "art=$art" >> "$GITHUB_OUTPUT"
if [[ ",$devices," == *",wear,"* ]]; then echo "wear=true" >> "$GITHUB_OUTPUT"; fi
