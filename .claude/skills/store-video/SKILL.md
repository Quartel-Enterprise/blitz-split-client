---
name: store-video
description: "Make or redo the store preview video of a language (build/store-video/<locale>/store-video-<locale>.mp4): the real Android app recorded on the emulator, narrated by an ElevenLabs voice over a music bed, composed with Remotion by scripts/store-video/generate.sh, under Google Play's preview video rules. Adds a language's scripts/store-video/locales/<locale>.json (script, voice, scenes, cards), records, has Pierre pick the voice and the take by ear, renders and checks the result. Use whenever someone asks for the Play (or App Store) preview video, its narration or music, or to redo it in a language (\"faz o vídeo da loja em espanhol\", \"refaz o vídeo de prévia\", \"a narração do vídeo\")."
---

# Make the store preview video of a language

[docs/store-video.md](../../../docs/store-video.md) explains the pipeline, Play's rules and what each piece does: read
it first. `scripts/store-video/generate.sh <locale>` does the work; this skill decides what a language needs, asks
Pierre what only he can pick, and checks every output before he sees it. Talk to Pierre in his language; the JSON, the
code and the docs stay in English, the narration and titles in the video's language.

## 0. What it needs

- An Android emulator running, 1080x2400 at 420 dpi (Pixel-sized, the coordinates assume it), with a debug build:
  `./scripts/heavy-check.sh ./gradlew :androidApp:installDebug`.
- `adb`, `ffmpeg`, `node`/`npm`, `python3`.
- The ElevenLabs key in `ELEVENLABS_API_KEY` or `~/.elevenlabs_key`. Never print it, paste it, or write it anywhere
  else. If it is missing, ask Pierre to save it there; don't create one yourself unless he asks.
- Check the plan before spending (`curl` `GET /v1/user/subscription` with the key, or the ElevenLabs MCP's
  `check_subscription`): a paid tier, `allowed_to_extend_character_limit` false (no pay-as-you-go), credits enough
  (a language takes about 1,500). On the free plan stop: its audio can't go in a store video.

## 1. The language's JSON

For a language that has none, copy `scripts/store-video/locales/en-US.json` to `<locale>.json`, named with the store
screenshots' locale codes (`scripts/store-screenshots/locales.tsv`). Not pt-BR's: its Pix shot, `pix` scene and Pix
thumbnail are no other locale's. The render reads it through `props.json`, so nothing else needs registering. Then:

- **`lines`**: the narration, one per shot plus the end card's. Translate the meaning, not the words, in the tone of
  that language's store listing (`docs/store-listing.md`); keep each line about as long as pt-BR's, since the shots'
  lengths follow them.
- **Pix is pt-BR's alone**: every other locale, pt-PT included, drops the Pix shot, its line and its `pix` scene, as
  [docs/store-video.md](../../../docs/store-video.md) explains under "Pix is pt-BR's alone". Showing instead how that
  market pays back is Pierre's call, and only if the app does it: ask before adding a shot.
- **Titles and the end card**: the line's key words (`title`, `subtitle`, `accent`), the app's name in that language
  ("Bora Rachar" in Portuguese, "Blitz Split" elsewhere) and the two chips. Nothing Play forbids: no call to action,
  ranking, "new", "free" or price.
- **`appLanguage`** is the tag `files/store-video` takes (the store screenshots' language), **`launchWaitFor`** a text
  of the home screen in that language.
- **`scenes`**: the `tap` texts and `waitFor` texts are the app's words in that language (look them up in the
  Android `values-<lang>/strings.xml`, or dump the screen: `adb shell uiautomator dump`). Keep the swipes, then check
  them in step 3.

## 2. The voice, picked by Pierre

- List the voice library's native voices for the language and accent (`GET /v1/shared-voices?language=<code>&accent=<accent>&gender=female`,
  or the MCP's `search_voice_library`), sorted by use, and keep a handful whose descriptions fit (warm, friendly,
  clear; not "announcer" or "urgent"). Ask Pierre's preference of gender if it isn't settled.
- Generate one take of all the lines with each (`elevenlabs.py narration` with a temporary JSON, or the MCP's
  `text_to_speech`), check each with Whisper (the words, the language, the question rising), and build a page that
  plays them one after another, the current video's voice first for reference. Send it with SendUserFile and let
  Pierre choose by ear: the voice is his call.
- Put the chosen voice in the JSON with `"takes": 3`, run `elevenlabs.py narration <locale>` for its three takes,
  check them the same way, and have Pierre pick the take (`voice.take`); then set `"takes"` to that take's number
  (never below it, or a fresh checkout's `narration` never makes the take `prepare` reads), so a later render on this
  machine asks for nothing new.
- **Music**: reuse a locale's prompt unless Pierre asks for another; a new one gets three options in a page, mixed
  under the narration, for him to pick.

## 3. Record and check the frames

`scripts/store-video/generate.sh <locale>` records, then composes. While it records, nothing else should load the
Mac (a busy emulator stalls the app and the scrolls stutter). Afterwards, before anyone watches:

- **Look at each scene's frames** (`ffmpeg -i build/store-video/<locale>/public/<scene>.mp4 -vf fps=2,scale=240:-1,tile=8x2`)
  and the stills (`public/*.png`): the right screen, a full status bar of 9:41, Wi-Fi and battery only, no menu
  opened by a long press, no "isn't responding" dialog.
- **Check each card's rectangle** on its still: crop the rectangle with a 100 px grid and confirm it frames the whole
  card, in this language's layout. Fix `cards` in the JSON and render again with `--skip-record`.
- A scene that went wrong records again alone: `generate.sh <locale> <scene>`.

## 4. Check the video

- **No flashes**: per-frame average brightness (`signalstats` YAVG on a small scale) has no jump above 8 between two
  frames.
- **The words**: Whisper on the video's audio transcribes every line as written, each inside its shot.
- **Under 30 s**, and the titles never touch the phone (a contact sheet at 1 fps).
- **The thumbnails**: fill the JSON's `thumbnail` (a short headline in the listing's tone, its accent words, a
  Balances still behind a payment still), run `scripts/store-video/thumbnail.sh <locale>` and look at both sizes:
  the vertical one's words never touch its edges.
- Send the video and the thumbnails with SendUserFile and wait for Pierre's approval; what he dislikes goes back to the JSON or to
  `StoreVideo.tsx`, and what he decided goes into docs/store-video.md.

## 5. Publish

Uploading is Pierre's: YouTube, unlisted, monetization off, then the URL in Play Console's listing of that language
(docs/store-video.md). Never upload or paste it for him unless he asks in this conversation.
