#!/usr/bin/env python3
"""Put the screenshots of one run side by side, so a whole size is read in a few images.

  contact-sheet.py <directory> <prefix> [per-sheet=5] [height=1000]

Reads <directory>/<prefix>-NN-*.png and writes <directory>/sheet-<prefix>-<n>.jpg, printing which screens each
holds. Needs Pillow (python3 -m pip install pillow). Use 3 per sheet for landscape sizes.
"""
import glob
import os
import sys

from PIL import Image

directory, prefix = sys.argv[1], sys.argv[2]
per_sheet = int(sys.argv[3]) if len(sys.argv) > 3 else 5
height = int(sys.argv[4]) if len(sys.argv) > 4 else 1000
files = sorted(glob.glob(os.path.join(directory, f"{prefix}-[0-9]*.png")))
for start in range(0, len(files), per_sheet):
    images = [Image.open(f).convert("RGB") for f in files[start:start + per_sheet]]
    images = [i.resize((max(1, i.width * height // i.height), height)) for i in images]
    sheet = Image.new("RGB", (sum(i.width for i in images) + 12 * (len(images) - 1), height), (255, 0, 255))
    x = 0
    for image in images:
        sheet.paste(image, (x, 0))
        x += image.width + 12
    out = os.path.join(directory, f"sheet-{prefix}-{start // per_sheet + 1}.jpg")
    sheet.save(out, quality=82)
    print(out, [os.path.basename(f)[len(prefix) + 1:-4] for f in files[start:start + per_sheet]])
