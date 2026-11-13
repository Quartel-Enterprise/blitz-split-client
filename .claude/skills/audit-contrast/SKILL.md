---
name: audit-contrast
description: "Sweep the app for color contrast problems on Android, iOS, macOS and the web, in light and dark: text under 4.5:1, icons, dots, field edges and focus rings under 3:1 (WCAG AA), dimmed text that still informs, money and status colors on a tint they were not chosen for. Computes every ratio from the real token values, never by eye, and fixes what fails. Use whenever someone asks to check or fix contrast, legibility, colors that are hard to read, accessibility of colors, or after a change that adds colors, tints, opacity or a new surface (\"confere o contraste\", \"esse texto está apagado\")."
argument-hint: "[screens or platforms to limit the sweep to]"
---

# Audit contrast

The rule is one bullet of [docs/product/general.md](../../../docs/product/general.md): every text reads at 4.5:1 or
more on what is behind it, and a dot, an icon or a field's edge that carries meaning at 3:1, in light and dark. The
same doc holds the colors that were chosen on purpose (the green of what comes back, the orange of what you owe, the
pending amber, the three tones of a category or a person's color): read its color bullets first, so a fix never
undoes one.

A ratio is computed, never judged by eye: `scripts/contrast.py` takes a foreground, what is behind it and, when
that is a tint, the surface under the tint, and composites the alphas first.

```bash
.claude/skills/audit-contrast/scripts/contrast.py '#266489' '#FFFFFF'                    # 6.43
.claude/skills/audit-contrast/scripts/contrast.py '#3C3C43@0.6' '#266489@0.12' '#FFFFFF' # text on a tint over white
.claude/skills/audit-contrast/scripts/contrast.py --min 4.5 pairs.txt                    # "label | fg | bg [| base]" per line
```

## 1. Scope

With no argument, sweep everything, weighing what changed since the last sweep: `git log --oneline | grep -i
contrast` finds it, and `git diff <that commit>..HEAD --stat -- android iosApp webApp` lists what was added since.
New screens, new tints and new uses of opacity are where failures come from: the tokens themselves were already
checked.

## 2. One subagent per platform, read-only

Launch three in one message. Each computes and reports only the pairs that fail, with both themes' numbers, the
`path:line`, and the fix with an existing token. What each looks at:

**The tokens, both themes.** Every on-color against its color, and every color that is drawn on more than one
surface against each of them: the money green and orange and the pending amber on each container level and each tint
they are drawn on, the data tone of every category and avatar color on a card, the page and a muted surface.

- Android: the scheme in `:android:designsystem`'s theme, `pendingColor()`, `toContainerColor` / `toContentColor` /
  `toDataColor`. Dynamic color schemes cannot be computed from the repo: say so.
- Apple: `Palette` and the asset catalog. System colors take Apple's documented values: `secondaryLabel` is
  `#3C3C43` at 60% in light (3.44:1 on white: a known gap, Pierre's to decide, not to fix silently) and `#EBEBF5` at
  60% in dark; `tertiaryLabel` is 30%, under 3:1 on anything.
- Web: every token of `webApp/src/index.css`, converted from oklch, and `--avatar-bg` / `--avatar-fg` /
  `--avatar-data` for every color.

**The usages.** Grep for what lowers a pair below its tokens:

- Opacity on something that informs: Compose's `alpha(…)` / `.copy(alpha = …)`, SwiftUI's `.opacity(…)`, Tailwind's
  `opacity-*` and `text-*/NN`. A disabled control is exempt; the line that says why it is disabled ("Needs internet")
  is not, and neither is a status drawn dimmed ("Full debt" with its check).
- A muted color on a tinted surface: `onSurfaceVariant` / `outline` as text on a container, `.secondary` /
  `.tertiary` on a tint, `text-muted-foreground` on `bg-muted` or `bg-accent`.
- A label on a filled button: every prominent button on Apple sets `.prominentLabel()` (a build phase checks it), and
  a selected chip takes `Palette.onAccent`, never `.white` (1.7:1 on the dark accent).
- Text or icons over a photo or a scrim: compute it over the lightest thing that can be behind (a white page in the
  light theme), not over black.
- A selected row on the Mac: a custom color keeps its value on the selection's highlight, so a money color there
  reads at about 1:1; switch to `.primary` / `.secondary` when `backgroundProminence` is `.increased`.
- Marks: a focus ring, an unchecked switch or checkbox, a field's edge, a dot that stands for a group or a status,
  against 3:1.
- Hard-coded colors in feature code (`Color(0x…)`, `Color(red:…)`, `text-red-500`): each one is outside the tokens,
  so compute it and move it to a token.

## 3. See it

Numbers decide, but a look catches what grep misses: run the app in light and dark on one device per platform (the
`audit-responsiveness` skill says how to seed data and take contact sheets) and look for anything faint. Check a
suspect by sampling its two colors from the screenshot and running them through `contrast.py`.

## 4. Fix

In this order: use the token made for that surface; if none passes, drop the opacity or the tint that lowered it; only
then change a token's value, keeping its hue, on the three platforms at once, and update the color bullets of
`docs/product/general.md` in the same pull request. Never fix one theme by breaking the other: compute both. A fix
that changes how something looks at a glance (a darker scrim, a stronger focus ring) is listed in the pull request as
a decision taken; one that would replace a platform's own color across the app (Apple's secondary label) is Pierre's
to decide, so report it and leave it.

## 5. Report

In the pull request: each pair fixed with its ratio before and after in both themes, what passes with a thin margin
(under 5:1), and what was left for Pierre with the numbers.
