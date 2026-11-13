#!/usr/bin/env python3
"""WCAG 2 contrast ratio of colors, alpha composited first.

  contrast.py '#266489' '#FFFFFF'                 one pair: foreground on background
  contrast.py '#3C3C43@0.6' '#FFFFFF'             a foreground at 60% alpha
  contrast.py '#3C3C43@0.6' '#266489@0.12' '#FFF' a foreground on a tint over a base
  contrast.py --min 4.5 pairs.txt                 a file of "label | fg | bg [| base]" lines; exits 1 if any is under

Colors are #RGB, #RRGGBB or #RRGGBBAA, with an optional @alpha. Text needs 4.5:1, large text (18 pt, or 14 pt
bold) and a mark that carries meaning (an icon, a dot, a field's edge, a focus ring) 3:1.
"""
import sys


def parse(color):
    alpha = 1.0
    if "@" in color:
        color, a = color.split("@")
        alpha = float(a)
    h = color.strip().lstrip("#")
    if len(h) == 3:
        h = "".join(c * 2 for c in h)
    if len(h) == 8:
        alpha *= int(h[6:8], 16) / 255
    return tuple(int(h[i:i + 2], 16) / 255 for i in (0, 2, 4)), alpha


def over(top, alpha, bottom):
    return tuple(t * alpha + b * (1 - alpha) for t, b in zip(top, bottom))


def luminance(rgb):
    lin = [c / 12.92 if c <= 0.04045 else ((c + 0.055) / 1.055) ** 2.4 for c in rgb]
    return 0.2126 * lin[0] + 0.7152 * lin[1] + 0.0722 * lin[2]


def ratio(fg, bg, base="#FFFFFF"):
    base_rgb, _ = parse(base)
    bg_rgb, bg_alpha = parse(bg)
    behind = over(bg_rgb, bg_alpha, base_rgb)
    fg_rgb, fg_alpha = parse(fg)
    front = over(fg_rgb, fg_alpha, behind)
    hi, lo = sorted((luminance(front), luminance(behind)), reverse=True)
    return (hi + 0.05) / (lo + 0.05)


def main(argv):
    minimum = 4.5
    if argv and argv[0] == "--min":
        minimum = float(argv[1])
        argv = argv[2:]
    if len(argv) == 1:
        failed = False
        for line in open(argv[0]):
            parts = [p.strip() for p in line.split("|")]
            if len(parts) < 3 or line.lstrip().startswith("//"):
                continue
            r = ratio(*parts[1:4])
            failed |= r < minimum
            print(f"{'FAIL' if r < minimum else 'ok  '} {r:5.2f}  {parts[0]}")
        return 1 if failed else 0
    if len(argv) in (2, 3):
        print(f"{ratio(*argv):.2f}")
        return 0
    print(__doc__)
    return 2


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
