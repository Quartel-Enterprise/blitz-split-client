# Accessibility

Splitting a bill is something everyone does, so Blitz Split (**Bora Rachar** in Portuguese) aims to work for everyone,
whatever the device, the screen size or the way they use it.

## Supported environments

| Platform | Minimum | UI |
| --- | --- | --- |
| Android | Android 8.0 (API 26): phones, tablets and foldables | Jetpack Compose, Material 3 Expressive |
| iPhone and iPad | iOS and iPadOS 26 | SwiftUI |
| Mac | macOS 26 | SwiftUI |
| Web | A current version of Chrome, Edge, Firefox or Safari, with OPFS | React |

Each platform is drawn by its own native UI, so it works with the platform's own assistive technologies: TalkBack and
Switch Access on Android, VoiceOver, Voice Control and Switch Control on Apple devices, and screen readers and the
keyboard in the browser.

## What we commit to

- **Contrast.** Every text reads at 4.5:1 or more on what is behind it, and every icon, dot or field edge that carries
  meaning at 3:1 (WCAG 2.2 AA), in light and dark mode. Money and status never rely on color alone: amounts carry their
  sign and words such as "owes" or "is owed".
- **Text size.** The apps follow the system's font size (Android's font scale, Apple's Dynamic Type, the browser's
  zoom), and layouts reflow to make room; an amount stays on one line and shrinks to fit.
- **Screen readers.** Buttons, icons and photos have labels; decorative images are hidden from them; a row reads as
  one sentence instead of scattered pieces.
- **Motion.** Animations follow Android's Remove animations, Reduce Motion on Apple devices and
  `prefers-reduced-motion` on the web.
- **Every screen size.** Phones upright and lying down, tablets, foldables in book and tabletop postures, the Mac's
  windows and the web's breakpoints, as [Responsive layouts](docs/architecture/responsive-layouts.md) describes.
- **Keyboard and pointer.** On the Mac and the web, controls are reachable with the keyboard and show a focus ring;
  tablets work with a keyboard and a mouse or trackpad too.
- **Language.** The app is translated into English, Portuguese (Brazil and Portugal), Spanish, French, German,
  Italian, Polish, Dutch, Japanese, Turkish, Indonesian, Russian, Ukrainian, Korean, Hindi, Arabic, Chinese (Simplified), Hebrew, Thai, Vietnamese, Chinese (Traditional), Swedish, Czech, Romanian, Bengali, Malay, Greek, Croatian, Hungarian, Urdu, Filipino, Serbian, Slovak, Persian, Tamil, Norwegian, Telugu, Bulgarian, Marathi, Swahili, Catalan, Danish, Finnish, Latvian, Gujarati and Gujarati, and follows the device's language and number format.

## Known limitations

- Reading a receipt from a photo relies on the camera and on how legible the receipt is; every field it fills in can
  be typed instead.
- Scanning an invite's QR code needs the camera; the same invite works as a link.
- A group's statistics are partly visual. "By category" lists each category's amount as text. Screen readers read
  the chart over time as the list of its periods and amounts (TalkBack and the web) or as an Audio Graph (VoiceOver);
  on screen, a period's amount shows only when its bar or point is picked (with the arrow keys too on the web, the
  Mac and Android, read aloud on the web).

## Reporting a barrier

If something in the app is hard or impossible to use with your device, setting or assistive technology, please tell
us: open an issue with the **Bug report** template, say which platform and version, and fill in its assistive technology field, and
what got in the way. Barriers are treated as bugs, and fixed with the same priority.
