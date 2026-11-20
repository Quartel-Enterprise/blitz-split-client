"""Turns a slow, unevenly captured drag into a smooth eased scroll at 60 fps (docs/store-video.md).

The emulator's recorder captures a fast scroll at an uneven ~15 fps, so each scene is a slow drag (24 to 30 s) that
gives hundreds of frames. Each frame's scroll offset is measured by phase correlation against the one before; every
output frame then takes the captured frame whose offset is nearest to an eased curve, and the few pixels left are
closed by shifting the scrolling band, with the rows it uncovers taken from the next (or previous) frame.
"""
import json
import os
import subprocess

import numpy as np
from PIL import Image

FPS = 60
WIDTH, HEIGHT = 1080, 2400
# The status bar's notification icons, between the clock and the Wi-Fi: demo mode doesn't hide every app's, so this
# stretch is painted over, frame by frame, with the bar's own top rows, above the icons (it follows a sheet's scrim
# too). Not the rows under the bar: a list scrolling there would be smeared up into it.
ICONS_X0, ICONS_X1, BAR_BOTTOM, BAR_TOP_ROW = 120, 860, 62, 2
CLEAN_BAR_FILTER = (f"split[a][b];[b]crop={ICONS_X1 - ICONS_X0}:2:{ICONS_X0}:{BAR_TOP_ROW},"
                    f"scale={ICONS_X1 - ICONS_X0}:{BAR_BOTTOM}:flags=neighbor[fill];[a][fill]overlay={ICONS_X0}:0")


def clean_bar(image):
    image[0:BAR_BOTTOM, ICONS_X0:ICONS_X1] = image[BAR_TOP_ROW, ICONS_X0:ICONS_X1]
    return image


def _small(path, band0, band1):
    image = Image.open(path).convert("L").resize((WIDTH // 2, HEIGHT // 2))
    return np.asarray(image, dtype=np.float32)[band0 // 2:band1 // 2, 40:500]


def _shift(a, b):
    """How many pixels b scrolled up from a, to a fraction of a pixel."""
    fa = np.fft.fft2(a - a.mean())
    fb = np.fft.fft2(b - b.mean())
    cross = fa * np.conj(fb)
    cross /= np.abs(cross) + 1e-6
    r = np.fft.ifft2(cross).real
    row, column = np.unravel_index(np.argmax(r), r.shape)
    y = row - r.shape[0] if row > r.shape[0] // 2 else row
    c = r[:, column]
    n = len(c)
    i = int(np.argmax(c))
    low, mid, high = c[(i - 1) % n], c[i], c[(i + 1) % n]
    denominator = low - 2 * mid + high
    fraction = 0.5 * (low - high) / denominator if denominator != 0 else 0
    return (y + fraction) * 2


def _bezier(p1x, p1y, p2x, p2y):
    ts = np.linspace(0, 1, 2001)
    bx = 3 * (1 - ts) ** 2 * ts * p1x + 3 * (1 - ts) * ts ** 2 * p2x + ts ** 3
    by = 3 * (1 - ts) ** 2 * ts * p1y + 3 * (1 - ts) * ts ** 2 * p2y + ts ** 3
    return lambda x: np.interp(x, bx, by)


def retime(frames_dir, out, hold0, duration, hold1, band0, band1):
    """frames_dir holds the drag's frames, named by their time; writes a 60 fps H.264 clip: hold0 seconds still,
    the scroll eased over duration seconds, hold1 seconds still. band0..band1 is the rows that scroll."""
    files = sorted(os.listdir(frames_dir))
    paths = [os.path.join(frames_dir, f) for f in files]
    offsets = [0.0]
    previous = _small(paths[0], band0, band1)
    for path in paths[1:]:
        current = _small(path, band0, band1)
        offsets.append(offsets[-1] + max(0.0, _shift(previous, current)))
        previous = current
    offsets = np.array(offsets)
    total = offsets[-1]
    start = max(0, int(np.argmax(offsets > 1.0)) - 1)
    end = int(np.argmax(offsets >= total - 0.5))
    ease = _bezier(0.4, 0.0, 0.15, 1.0)

    n0, n, n1 = round(hold0 * FPS), round(duration * FPS), round(hold1 * FPS)
    plan = [(start, 0.0)] * n0
    i = start
    for k in range(n):
        target = ease(k / (n - 1)) * total
        while i < end and abs(offsets[i + 1] - target) <= abs(offsets[i] - target):
            i += 1
        plan.append((i, target - offsets[i]))
    plan += [(end, 0.0)] * n1
    residual = np.array([abs(r) for _, r in plan])

    encoder = subprocess.Popen(
        ["ffmpeg", "-y", "-v", "error", "-f", "rawvideo", "-pix_fmt", "rgb24", "-s", f"{WIDTH}x{HEIGHT}", "-r", str(FPS),
         "-i", "-", "-c:v", "libx264", "-crf", "14", "-preset", "slow", "-pix_fmt", "yuv420p", out],
        stdin=subprocess.PIPE,
    )
    cache = {}

    def frame(j):
        j = min(max(j, 0), len(paths) - 1)
        if j not in cache:
            if len(cache) > 6:
                cache.pop(next(iter(cache)))
            cache[j] = np.asarray(Image.open(paths[j]).convert("RGB"))
        return cache[j]

    for index, rest in plan:
        image = frame(index)
        d = int(round(rest))
        if d > 0:
            original, image = image, image.copy()
            image[band0:band1 - d] = original[band0 + d:band1]
            following = frame(index + 1)
            gap = offsets[min(index + 1, len(offsets) - 1)] - offsets[index]
            rows = np.arange(band1 - d, band1)
            image[band1 - d:band1] = following[np.clip(rows + int(round(d - gap)), 0, HEIGHT - 1)]
        elif d < 0:
            d = -d
            original, image = image, image.copy()
            image[band0 + d:band1] = original[band0:band1 - d]
            preceding = frame(index - 1)
            gap = offsets[index] - offsets[max(index - 1, 0)]
            rows = np.arange(band0, band0 + d)
            image[band0:band0 + d] = preceding[np.clip(rows + int(round(gap - d)), 0, HEIGHT - 1)]
        encoder.stdin.write(clean_bar(image.copy()).tobytes())
    encoder.stdin.close()
    encoder.wait()
    if encoder.returncode:
        raise SystemExit(f"ffmpeg failed writing {out}")
    with open(out + ".plan.json", "w") as plan_file:
        json.dump({"start": start, "end": end, "scroll": total, "files": [files[i] for i, _ in plan]}, plan_file)
    return total, float(residual.max()), float(residual.mean())
