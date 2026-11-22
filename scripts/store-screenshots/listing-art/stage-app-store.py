"""Stages the listing art (store_listings/listing-art) where the App Store upload reads its screenshots: fastlane/screenshots/<locale>/
and fastlane/screenshots-mac/<locale>/, each image named <screen>_<device>.png, so local-set.mjs checks and sends the set
as it does the framed one (docs/store-listing-screenshots.md#sending-a-set-made-on-a-mac).

The art's ten images of a device take the ten names of screens.tsv in order: the stores sort a shelf by file name, so
the art's order is kept, and the screen in a name is only a slot. The iPhone Duo's open images go as iphone_duo_open.

    python3 scripts/store-screenshots/listing-art/stage-app-store.py
    ART_DEVICES=iphone_6_7,mac python3 scripts/store-screenshots/listing-art/stage-app-store.py   # some devices only
    ART_LOCALES=en-US,pt-BR python3 scripts/store-screenshots/listing-art/stage-app-store.py      # some languages only

A device left out, by ART_DEVICES or because the art was drawn for none of it, is not staged: a set without it is
refused by local-set.mjs unless it says so, and a release uploads what its runners captured.
"""
import json
import os
import shutil
import subprocess
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[3]
ART = ROOT / "store_listings/listing-art"
IOS = ROOT / "fastlane/screenshots"
MAC = ROOT / "fastlane/screenshots-mac"
DEVICES = {"iphone_6_7": IOS, "iphone_6_3": IOS, "ipad_13": IOS, "ipad_11": IOS, "iphone_duo": IOS, "mac": MAC}
DUO_OPEN_WIDTH = 2853


def screen_names():
    rows = (ROOT / "scripts/store-screenshots/screens.tsv").read_text().splitlines()
    return [row.split("\t")[1] for row in rows if row.strip() and not row.startswith("#")]


def app_store_locales():
    script = "import('./scripts/release/store-listing.mjs').then((m) => console.log(JSON.stringify(m.APP_STORE_LOCALES)))"
    output = subprocess.run(["node", "-e", script], cwd=ROOT, check=True, capture_output=True, text=True).stdout
    return json.loads(output)


def main():
    names = screen_names()
    chosen = [device for device in os.environ.get("ART_DEVICES", "").split(",") if device]
    unknown = [device for device in chosen if device not in DEVICES]
    if unknown:
        raise SystemExit(f"No device {', '.join(unknown)}: one of {', '.join(DEVICES)}")
    devices = {device: folder for device, folder in DEVICES.items() if not chosen or device in chosen}
    tags = {line.split("\t")[1]: line.split("\t")[0] for line in (ROOT / "scripts/store-screenshots/locales.tsv").read_text().splitlines()
            if line.strip() and not line.startswith("#")}
    # ART_LOCALES narrows it to those listing tags, and then only their folders are replaced: the others stay as staged.
    only = [tag for tag in os.environ.get("ART_LOCALES", "").split(",") if tag]
    unknown_tags = [tag for tag in only if tag not in tags.values()]
    if unknown_tags:
        raise SystemExit(f"locales.tsv has no {', '.join(unknown_tags)}")
    if not only:
        for folder in (IOS, MAC):
            shutil.rmtree(folder, ignore_errors=True)
    staged = 0
    for language, store_locales in app_store_locales().items():
        tag = tags[language]
        if only and tag not in only:
            continue
        for store_locale in store_locales:
            for folder in (IOS, MAC):
                if only:
                    shutil.rmtree(folder / store_locale, ignore_errors=True)
        for device, folder in devices.items():
            images = sorted((ART / device / tag).glob("*.png"))
            if len(images) != len(names):
                raise SystemExit(f"{device} {tag} has {len(images)} images, the shelf takes {len(names)}")
            for store_locale in store_locales:
                target = folder / store_locale
                target.mkdir(parents=True, exist_ok=True)
                for image, name in zip(images, names):
                    suffix = device
                    if device == "iphone_duo" and Image.open(image).width == DUO_OPEN_WIDTH:
                        suffix = "iphone_duo_open"
                    shutil.copy2(image, target / f"{name}_{suffix}.png")
                    staged += 1
    print(f"Staged {staged} images into {IOS} and {MAC}")


main()
