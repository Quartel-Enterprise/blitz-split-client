"""Stages the listing art (store_listings/listing-art) where the App Store upload reads its screenshots: fastlane/screenshots/<locale>/
and fastlane/screenshots-mac/<locale>/, each image named <screen>_<device>.png, so local-set.mjs checks and sends the set
as it does the framed one (docs/store-listing-screenshots.md#sending-a-set-made-on-a-mac).

The art's ten images of a device take the ten names of screens.tsv in order: the stores sort a shelf by file name, so
the art's order is kept, and the screen in a name is only a slot. The iPhone Duo's open images go as iphone_duo_open.

    python3 scripts/store-screenshots/listing-art/stage-app-store.py
    ART_DEVICES=iphone_6_7,mac python3 scripts/store-screenshots/listing-art/stage-app-store.py   # some devices only
    ART_LOCALES=en-US,pt-BR python3 scripts/store-screenshots/listing-art/stage-app-store.py      # some languages only

The Apple Watch's four images (store_listings/watch/app-store-ultra-3/<tag>/0N_<screen>.png, made by watch/generate.sh) go
into the iPhone's folder as <screen>_watch_ultra_3.png, 01_balances to 04_recorded: its shelf goes up with the iPhone's and
the iPad's, since deliver replaces every screenshot of a locale. App Store Connect takes one watch size for all of them.

A device left out, by ART_DEVICES or because the art was drawn for none of it (the watch's images made for no language), is not staged: a set without it is
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
DEVICES = {"iphone_6_7": IOS, "iphone_6_3": IOS, "ipad_13": IOS, "ipad_11": IOS, "iphone_duo": IOS, "mac": MAC, "watch_ultra_3": IOS}
DUO_OPEN_WIDTH = 2853
WATCH = ROOT / "store_listings/watch/app-store-ultra-3"
WATCH_SIZE = (422, 514)
WATCH_SCREENS = ["balances", "group", "confirm", "recorded"]


def screen_names():
    rows = (ROOT / "scripts/store-screenshots/screens.tsv").read_text().splitlines()
    return [row.split("\t")[1] for row in rows if row.strip() and not row.startswith("#")]


def app_store_locales():
    script = "import('./scripts/release/store-listing.mjs').then((m) => console.log(JSON.stringify(m.APP_STORE_LOCALES)))"
    output = subprocess.run(["node", "-e", script], cwd=ROOT, check=True, capture_output=True, text=True).stdout
    return json.loads(output)


def unstage_watch(store_locales, folder):
    """Removes a locale's staged watch images, and only those: the iPhone's and the iPad's share the folder."""
    for store_locale in store_locales:
        for image in (folder / store_locale).glob("*_watch_ultra_3.png"):
            image.unlink()


def stage_watch(tag, store_locales, folder):
    """The watch's images are 0N_<screen>.png, which already are their shelf's order and the screens' names."""
    unstage_watch(store_locales, folder)
    for number, screen in enumerate(WATCH_SCREENS, start=1):
        name = f"0{number}_{screen}"
        image = WATCH / tag / f"{name}.png"
        if not image.is_file():
            raise SystemExit(f"{image} is missing: watch/generate.sh draws it (WATCH_STORES=apple)")
        with Image.open(image) as opened:
            if opened.size != WATCH_SIZE:
                raise SystemExit(f"{image} is {opened.width}x{opened.height}, the Apple Watch's shelf takes 422x514")
        for store_locale in store_locales:
            target = folder / store_locale
            target.mkdir(parents=True, exist_ok=True)
            shutil.copy2(image, target / f"{name}_watch_ultra_3.png")


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
    # Only the folders of the devices being staged are replaced: a Mac left out keeps the Mac set staged before. The
    # watch replaces its own images alone, so ART_DEVICES=watch_ultra_3 keeps the iPhone's and the iPad's it joins.
    staged_folders = {folder for device, folder in devices.items() if device != "watch_ultra_3"}
    if not only:
        for folder in staged_folders:
            shutil.rmtree(folder, ignore_errors=True)
    staged = 0
    for language, store_locales in app_store_locales().items():
        tag = tags[language]
        if only and tag not in only:
            continue
        # Why: a language with no App Store listing locale (Filipino) has nothing to stage, art or not.
        if not store_locales:
            continue
        for store_locale in store_locales:
            for folder in staged_folders:
                if only:
                    shutil.rmtree(folder / store_locale, ignore_errors=True)
        for device, folder in devices.items():
            if device == "watch_ultra_3":
                if not (WATCH / tag).is_dir():
                    print(f"No Apple Watch images for {tag} (watch/generate.sh, WATCH_STORES=apple): its shelf is left out")
                    unstage_watch(store_locales, folder)
                    continue
                stage_watch(tag, store_locales, folder)
                staged += len(WATCH_SCREENS) * len(store_locales)
                continue
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
