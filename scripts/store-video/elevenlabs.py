"""The store preview video's narration and music, from ElevenLabs (docs/store-video.md).

    python elevenlabs.py narration <locale>   # every take of every line, into build/store-video/<locale>/voice/
    python elevenlabs.py music <locale>       # the background track, into build/store-video/<locale>/music-<hash>.mp3
    python elevenlabs.py prepare <locale>     # the chosen take and the music into public/, and the render's props

Spends credits of the ElevenLabs account the key belongs to, so it never asks twice for the same audio: each file is
named after a hash of its request (voice, model, settings, text), and a changed request makes a new one. The key comes from ELEVENLABS_API_KEY or ~/.elevenlabs_key and is never printed.
Audio is licensed for commercial use only while generated under a paid plan.
"""
import hashlib
import json
import os
import subprocess
import sys
import urllib.error
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
API = "https://api.elevenlabs.io/v1"


def key():
    value = os.environ.get("ELEVENLABS_API_KEY")
    if not value:
        path = Path.home() / ".elevenlabs_key"
        if not path.exists():
            raise SystemExit("Set ELEVENLABS_API_KEY or write the key to ~/.elevenlabs_key (chmod 600).")
        value = path.read_text().strip()
    return value


def post(path, body):
    request = urllib.request.Request(f"{API}/{path}", data=json.dumps(body).encode(),
                                     headers={"xi-api-key": key(), "Content-Type": "application/json"})
    try:
        with urllib.request.urlopen(request, timeout=300) as response:
            return response.read()
    except urllib.error.HTTPError as error:
        raise SystemExit(f"ElevenLabs refused {path}: {error.code} {error.read()[:300].decode(errors='replace')}")


def locale_paths(locale):
    config = json.loads((Path(__file__).parent / "locales" / f"{locale}.json").read_text())
    base = ROOT / "build/store-video" / locale
    return config, base


def line_request(voice, lines, i):
    """What ElevenLabs is asked for line i (1-based): with the lines around it, so its intonation follows the script,
    as if read in one go."""
    return {
        "text": lines[i - 1],
        "model_id": voice["model"],
        "language_code": voice["languageCode"],
        "voice_settings": {"stability": voice["stability"], "similarity_boost": voice["similarity"]},
        "previous_text": " ".join(lines[:i - 1])[-300:] or None,
        "next_text": " ".join(lines[i:])[:300] or None,
    }


def digest(*parts):
    return hashlib.sha1(json.dumps(parts, sort_keys=True).encode()).hexdigest()[:10]


def take_file(base, voice, lines, take, i):
    return base / "voice" / f"take{take}" / f"{i}-{digest(voice['id'], line_request(voice, lines, i))}.mp3"


def narration(locale):
    config, base = locale_paths(locale)
    voice, lines = config["voice"], config["lines"]
    for take in range(1, voice["takes"] + 1):
        for i in range(1, len(lines) + 1):
            out = take_file(base, voice, lines, take, i)
            if out.exists():
                continue
            out.parent.mkdir(parents=True, exist_ok=True)
            body = line_request(voice, lines, i)
            out.write_bytes(post(f"text-to-speech/{voice['id']}?output_format=mp3_44100_192", body))
            print(f"take {take} line {i}")


def music_request(track):
    return {
        "prompt": track["prompt"],
        "music_length_ms": round(track["seconds"] * 1000),
        "model_id": "music_v2",
        "force_instrumental": True,
    }


def music_file(base, track):
    return base / f"music-{digest(music_request(track))}.mp3"


def music(locale):
    config, base = locale_paths(locale)
    out = music_file(base, config["music"])
    if out.exists():
        print(f"{out.name} is already there")
        return
    base.mkdir(parents=True, exist_ok=True)
    out.write_bytes(post("music?output_format=mp3_44100_192", music_request(config["music"])))
    print(out)


def seconds(path):
    out = subprocess.run(["ffprobe", "-v", "error", "-show_entries", "format=duration", "-of", "csv=p=0", str(path)],
                         capture_output=True, text=True, check=True).stdout
    return round(float(out), 3)


def check_lines_fit(locale, config, durations):
    """The shots' lengths come from their steps, not from the lines, and StoreVideo.tsx starts each line 0.25 s into its
    shot (the end card's 0.5 s in): a longer line would still be playing when the next one starts."""
    lengths = [sum(step["to"] - step["from"] if step["kind"] == "play" else step["seconds"] for step in shot["steps"])
               for shot in config["shots"]]
    room = [length - 0.25 for length in lengths] + [config["endSeconds"] - 0.5]
    late = [f"line {i + 1}: {spoken:.2f}s in {fits:.2f}s" for i, (spoken, fits) in enumerate(zip(durations, room))
            if spoken > fits]
    if late:
        raise SystemExit(f"{locale}.json: lines longer than their shots ({'; '.join(late)}); shorten them, lengthen "
                         "the shots' steps, or pick another take")


def prepare(locale):
    """Each line of the chosen take, at -16 LUFS, as public/voice<n>.wav; the music as public/music.wav; and the props
    the render reads (the locale, how long each line and the music last)."""
    config, base = locale_paths(locale)
    voice, lines = config["voice"], config["lines"]
    if len(lines) != len(config["shots"]) + 1:
        raise SystemExit(f"{locale}.json needs one line per shot plus the end card's: "
                         f"{len(config['shots'])} shots, {len(lines)} lines")
    public = base / "public"
    public.mkdir(parents=True, exist_ok=True)
    durations = []
    for i in range(1, len(lines) + 1):
        out = public / f"voice{i}.wav"
        source = take_file(base, voice, lines, voice["take"], i)
        subprocess.run(["ffmpeg", "-y", "-v", "error", "-i", str(source), "-af",
                        "loudnorm=I=-16:TP=-1.5:LRA=7,aresample=48000", "-ac", "1", str(out)], check=True)
        durations.append(seconds(out))
    check_lines_fit(locale, config, durations)
    subprocess.run(["ffmpeg", "-y", "-v", "error", "-i", str(music_file(base, config["music"])), "-ar", "48000",
                    str(public / "music.wav")],
                   check=True)
    props = {"locale": locale, "voiceSeconds": durations, "musicSeconds": seconds(public / "music.wav")}
    (base / "props.json").write_text(json.dumps(props))
    print(json.dumps(props))


if __name__ == "__main__":
    if len(sys.argv) != 3 or sys.argv[1] not in ("narration", "music", "prepare"):
        raise SystemExit(__doc__)
    {"narration": narration, "music": music, "prepare": prepare}[sys.argv[1]](sys.argv[2])
