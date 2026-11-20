# Store preview video

Google Play shows one preview video above a listing's screenshots, and autoplays its first 30 seconds muted on
home, search and the listing. Ours is **generated, not edited by hand**: the real Android app recorded on an emulator,
on the store screenshots' sample trip, inside a 3D phone with camera moves, cards lifted out of the screen, the words
of the narration on screen, a narrator and a music bed. Today only **pt-BR** exists; each other language is a new
`scripts/store-video/locales/<locale>.json` (the `store-video` skill walks through one). Nothing the scripts produce
is versioned: recordings, audio and the video live in `build/store-video/<locale>/`, and the video goes to YouTube.

```bash
./gradlew :androidApp:installDebug            # on the emulator the recording uses
scripts/store-video/generate.sh pt-BR          # records, voices, composes: build/store-video/pt-BR/store-video-pt-BR.mp4
scripts/store-video/generate.sh pt-BR --skip-record        # compose again from the recordings already made
scripts/store-video/generate.sh pt-BR group bal            # record only these scenes again, then compose
```

## Play's rules the video follows

From [Add preview assets](https://support.google.com/googleplay/android-developer/answer/9866151) (Preview video):

- **At most 30 seconds** (only those autoplay) and the core features in the first 10: the pt-BR one runs 27.7 s.
- **At least 80% of it is the app's real experience**: every screen is a recording of the app, never a mockup or
  an AI-generated video (those redraw text and amounts frame by frame); only the end card is ours.
- **No fingers on screen** (`show_touches` is off), **no black bars**, a portrait video.
- **Works muted**: the narration's words are on screen.
- **No call to action, ranking or price**: no "Download now", "Best", "#1", "New", "Free" or "Sale". "Funciona
  offline" and "Sem precisar de conta" are features, not prices.
- **Ads off and embeddable**: upload it to YouTube as unlisted, monetization off, then paste its URL into Play
  Console's Main store listing (Grow users > Store presence > Main store listing > Preview video), per language.

## What runs

| Piece | What it does |
|---|---|
| [`locales/<locale>.json`](../scripts/store-video/locales/pt-BR.json) | Everything a language changes: the app language and theme to record, the narration's lines, the voice, the music's prompt, the scenes to record (taps by their visible text) and the shots the video plays (titles, steps, the cards' rectangles) |
| [`record.py`](../scripts/store-video/record.py) | Starts the debug app on the sample trip in that language, cleans the status bar, walks the scenes and records each one |
| [`retime.py`](../scripts/store-video/retime.py) | Turns each slow recorded drag into a smooth eased 60 fps scroll |
| [`elevenlabs.py`](../scripts/store-video/elevenlabs.py) | The narration's takes and the music from ElevenLabs, then the chosen take and the music for the render |
| [`remotion/`](../scripts/store-video/remotion/src/StoreVideo.tsx) | The composition, with [Remotion](https://www.remotion.dev): the phone, the camera, the pushes, the freezes and their cards, the titles, the end card, the sound |
| [`generate.sh`](../scripts/store-video/generate.sh) | All of it in order, then the loudness YouTube expects (-14 LUFS) |

### Recording on the emulator

- **The sample trip.** A debug build reads `files/store-video` at start (`StoreVideoAndroid.kt`, written by
  `record.py` with `run-as`): it holds the language tag and `dark` or `light`, and the app starts on the store
  screenshots' sample trip in that language and its currency, in a Room database of its own, so the developer's data
  survives. The first launch seeds the trip in the background and writes `files/store-video.ready` when it is done;
  `record.py` waits for that file and starts the app again, so every scene starts on the finished trip (a seed that
  fails logs why under the `StoreVideo` tag of logcat). `record.py` deletes both files when it ends, and a release
  build never reads them. The trip's photos (the
  faces, the groups' pictures, the receipt) are the files of `scripts/store-screenshots/assets.tsv`, which
  `record.py` copies into the app's `files/store-video-assets/` first.
- **The emulator's recorder, on the host** (`adb emu screenrecord`). The device's `screenrecord` made a loaded Mac's
  emulator kill the app for not answering in time. The host recorder captures at an uneven frame rate, and records
  Android's cross-fades between screens as grey flashes, so:
  - **Scrolls are slow drags** (24 to 30 s) that `retime.py` speeds up: it measures each frame's offset by phase
    correlation and picks, for every output frame, the captured one nearest to an eased curve, then closes the few
    pixels left by shifting the scrolling band. A drag must **start on a section header**: one that starts on a list
    item holds still long enough to become a long press, and opens the item's menu.
  - **Screens change with our own push**, never the app's transition: each scene starts on its screen already open.
- **A clean status bar.** System UI demo mode gives 9:41, a full battery and Wi-Fi only, and Gmail and the Google app
  are suspended while recording; both are undone at the end. Demo mode still leaves some apps' notification icons
  beside the clock (snoozing them didn't hide them either), so every frame and screenshot has the stretch between the
  clock and the Wi-Fi painted over with the bar's own top rows (`clean_bar` in `retime.py`), which follows
  a sheet's scrim as it darkens.
- **A busy emulator answers late.** Its "isn't responding" dialog is answered with Wait; every tap waits for its
  target to show, and every scene for its text (`waitFor`); a tap recorded in a scene waits for the screen it opens
  (`shows`), and the clip is cut from the moment the screen starts to change, not from a fixed time.
- **Each scene starts where it was measured.** A screen keeps its scroll when the app comes back to it, so the
  scenes say where to start: `backUntil` a text, `scrollUpUntil` a text at the top. Taps are found by their text,
  their content description, or its start (`descStarts`, for a card that merges its words, as the group's
  "Viagem a Lisboa, fixado: …").

### The video

- **Shots.** Each line of narration is a shot: one or more steps, `play` (a stretch of a recording), `freeze` (the
  frame the step before ended on, held while cards lift out of it) and `still` (a screenshot). The cards are
  rectangles in the recording's 1080x2400 pixels, cut from that same frame, so what lifts always matches the screen.
- **Say only what the recording shows.** The trip's groups keep Simplify debts off, the app's default, so Balances
  shows each pair's net and the line says "quem deve quanto a quem", never "the fewest transfers". Turning it on
  would leave this device's user owing nobody, and the Pix scene (Pay Carla) with nothing to pay.
- **Titles** are the line's words, shortened where needed, the key words in the brand blue. Each line fades in and
  rises a little; no per-word bounce or blur (Pierre's call: it read as a tremble).
- **Sound.** Each line starts 0.25 s into its shot. The music plays under everything, dips while a line plays, and is
  started so its own ending lands on the last frame. A whoosh marks each whip, a soft pop each lifted card; both are
  synthesized by `generate.sh`, as the end card's icon is copied from the Apple app, so no binary lives here.

## ElevenLabs

The narration and the music come from [ElevenLabs](https://elevenlabs.io), on Pierre's **Creator** plan (annual, with
pay-as-you-go off, so it never bills past the plan's credits). What it costs and allows:

- **Credits.** One take of the six pt-BR lines is about 300 credits; `elevenlabs.py` makes `voice.takes` of them
  (three while a voice is being picked). A re-render is free only on the machine that holds the chosen take's files
  in `build/store-video/<locale>/voice/`, which are not versioned: on a fresh checkout, or after a line changes,
  `narration` asks for new takes, whose audio differs from the one Pierre picked, so they have to be picked again. A
  32-second track is about 450. The plan has 121k a month. `elevenlabs.py` never asks twice for the same audio: each
  file is named after a hash of its request (voice, model, settings, the line and its neighbours, or the music's
  prompt and length), so changing any of them makes a new file and the old one stays unused.
- **The free plan can't make these**: its audio isn't licensed for commercial use, and its API refuses the voice
  library's voices ("Free users cannot use library voices via the API"). Audio made under a paid plan stays licensed
  after the plan ends.
- **The key** is read from `ELEVENLABS_API_KEY` or `~/.elevenlabs_key` (mode 600), never from the repository, and
  never printed. A restricted key needs Text to Speech, Voices (read) and Music Generation; the
  [ElevenLabs MCP](https://github.com/elevenlabs/elevenlabs-mcp) uses the same key.
- **The voice** is a native speaker from the voice library, chosen by ear by Pierre: for pt-BR, Amanda Kelly (Sweet,
  Warm and Clear), take 1. Line 2 was reworded after take 1 was picked and only it was voiced again; the other five
  lines are take 1's files, voiced with the old line 2 around them and renamed by hand to the new requests' hashes.
  So their cache names don't match what they were voiced with, and a fresh `narration` would voice them all again.
  Each line is generated with the lines around it (`previous_text`, `next_text`), so the question in "Viagem com os
  amigos?" rises like one. Takes differ from run to run: a new run means picking again.
- **The music** is instrumental (ElevenLabs Music, `music_v2`), prompted to stay out of the voice's way.

## Things that bite

- **The emulator is put back as it was**: the animation scales, Show taps, demo mode, the suspended apps and the
  app's own language (which the seed sets to the video's) return to what they were before the run, even when it fails.
- **Right-to-left languages mirror the status bar**: the clock moves to the right, so the stretch `clean_bar` paints
  over (x 120 to 860) would cover the Wi-Fi and leave the notification icons. Mirror it before recording Arabic or
  Hebrew.
- **The emulator needs Android 13 (API 33) or newer.** `record.py` puts the app's language back with
  `cmd locale set-app-locales`, which older versions lack; there the seed's language stays in the app's legacy
  locale setting after the run.
- **Coordinates are the emulator's.** The swipes and the cards' rectangles are pixels of a 1080x2400 screen at 420
  dpi (a Pixel-sized emulator); another size moves every one of them. Taps are found by their text instead.
- **Another language moves the layout.** Longer words wrap, so a card's rectangle and a drag's start can move: check
  the frames (`build/store-video/<locale>/public/*.png`) before trusting a locale's numbers.
- **The device recorder is not an option.** `adb shell screenrecord` records smoothly but loads the emulator enough
  for Android to kill the app on a busy Mac.
- **Pix is pt-BR's alone.** Only the pt-BR video has the Pix shot ("E pra acertar, Pix copia e cola…"): the app
  offers Pix only to people likely to use it ([the Pix rule](product/general.md)), and the sample trip has a Pix key
  only in reais. Every other locale, pt-PT included, drops that shot, its line and its `pix` scene.
- **Copying the Pix code shows the system's clipboard bubble**, so the Pix scene (Settle up, then Pay Carla) stops
  before tapping Copy code; the lifted "Pix copy and paste" card does the pointing.
- **Remotion's license** is free for individuals and companies of up to three people; past that it needs a company
  license ([remotion.pro](https://www.remotion.pro/license)).
