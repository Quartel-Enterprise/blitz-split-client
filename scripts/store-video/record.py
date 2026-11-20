"""Records the scenes of the store preview video on a running Android emulator (docs/store-video.md).

    python record.py <locale> [scene ...]

Starts the debug app on the store screenshots' sample trip in the locale's language (files/store-video, read by
StoreVideoAndroid.kt), cleans the status bar (demo mode, the apps that post notifications suspended), then walks the
locale's "scenes": taps found by their text, a slow drag recorded per scene and re-timed into a smooth 60 fps clip,
screenshots for screens shown still. Writes build/store-video/<locale>/public/<scene>.mp4 and .png, plus the stills the
video's freezes and pushes need. Puts the emulator back as it was, even when a step fails.
"""
import json
import os
import re
import shutil
import subprocess
import sys
import tempfile
import time
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))  # run with python -I, which leaves this folder out
from retime import CLEAN_BAR_FILTER, FPS, retime  # noqa: E402

ROOT = Path(__file__).resolve().parents[2]
PACKAGE = "com.quare.blitzsplit"
SUSPENDED = ["com.google.android.gm", "com.google.android.googlequicksearchbox"]
DEMO = [
    ["enter"],
    ["clock", "-e", "hhmm", "0941"],
    ["battery", "-e", "level", "100", "-e", "plugged", "false"],
    ["network", "-e", "mobile", "hide"],
    ["network", "-e", "wifi", "show", "-e", "level", "4", "-e", "fully", "true"],
    ["notifications", "-e", "visible", "false"],
]
# The dialog a busy emulator shows when the app is slow to answer; "Wait" keeps it running.
NOT_RESPONDING = re.compile(r"(isn.t responding|não está respondendo)")
WAIT_BUTTON = re.compile(r"^(Wait|Aguardar)$")


def adb(*args, check=True, capture=False):
    result = subprocess.run(["adb", *args], check=check, capture_output=True, text=True)
    return result.stdout if capture else None


def demo(*args):
    adb("shell", "am", "broadcast", "-a", "com.android.systemui.demo", "-e", "command", *args)


def nodes():
    """The labelled nodes on screen: (text, content-desc, (x, y) of the center); none while the screen can't be read
    (uiautomator answers "null root node" while an app is starting or animating)."""
    out = adb("shell", "rm -f /sdcard/store-video-ui.xml; uiautomator dump /sdcard/store-video-ui.xml", check=False,
              capture=True) or ""
    if "dumped" not in out:
        time.sleep(2)
        return []
    xml = adb("shell", "cat", "/sdcard/store-video-ui.xml", check=False, capture=True) or ""
    found = []
    for text, desc, bounds in re.findall(r'text="([^"]*)"[^>]*content-desc="([^"]*)"[^>]*bounds="([^"]*)"', xml):
        x0, y0, x1, y1 = map(int, re.findall(r"\d+", bounds))
        found.append((text.replace("\xa0", " "), desc, ((x0 + x1) // 2, (y0 + y1) // 2)))
    return found


def answer_not_responding(found):
    if any(NOT_RESPONDING.search(t) for t, _, _ in found):
        for text, _, (x, y) in found:
            if WAIT_BUTTON.match(text):
                adb("shell", "input", "tap", str(x), str(y))
                time.sleep(3)
                return True
    return False


def find(target, found):
    """The first node matching target: its whole text, its whole content description, or the start of the content
    description (a card that merges its words into one, as "Viagem a Lisboa, fixado: …")."""
    for text, desc, center in found:
        if (
            ("text" in target and text == target["text"])
            or ("desc" in target and desc == target["desc"])
            or ("descStarts" in target and desc.startswith(target["descStarts"]))
        ):
            return center
    return None


def scroll_up_until(text, flings=8):
    """Scrolls to the very top, then checks text shows: a screen keeps its scroll when the app comes back to it, and a
    scene must start where its swipes and cards were measured. Stopping as soon as text showed could leave the screen
    a few rows short of the top, so it always flings a few times first."""
    for i in range(flings):
        adb("shell", "input", "swipe", "540", "700", "540", "2000", "150")
        time.sleep(1.5)
        if i >= 2 and any(t == text for t, _, _ in nodes()):
            return
    if not any(t == text for t, _, _ in nodes()):
        raise SystemExit(f'scrolling up never reached "{text}"')


def back_until(text, presses=6):
    for _ in range(presses):
        if any(t == text for t, _, _ in nodes()):
            return
        adb("shell", "input", "keyevent", "KEYCODE_BACK")
        time.sleep(2)
    raise SystemExit(f'going back never reached "{text}"')


def wait_for(text, seconds=60):
    deadline = time.time() + seconds
    while time.time() < deadline:
        found = nodes()
        if answer_not_responding(found):
            continue
        if any(t == text for t, _, _ in found):
            time.sleep(1.5)  # the screen's own animations settle
            return
        time.sleep(1)
    raise SystemExit(f'"{text}" never showed on the emulator')


def tap(target):
    deadline = time.time() + 60
    while time.time() < deadline:
        found = nodes()
        if answer_not_responding(found):
            continue
        center = find(target, found)
        if center:
            adb("shell", "input", "tap", str(center[0]), str(center[1]))
            return
        time.sleep(1)
    raise SystemExit(f"nothing on the emulator matches {target}")


def frame_count(path):
    out = subprocess.run(["ffprobe", "-v", "error", "-select_streams", "v", "-show_entries", "frame=pts_time", "-of",
                          "csv=p=0", path], capture_output=True, text=True, check=True).stdout
    return len(out.split())


def first_change(path, after):
    """The time, in seconds, of the first frame after `after` that differs visibly from the one before it."""
    probe = subprocess.run(["ffprobe", "-v", "error", "-select_streams", "v", "-show_entries", "frame=pts_time", "-of",
                            "csv=p=0", str(path)], capture_output=True, text=True, check=True).stdout.split()
    times = [float(t) for t in probe]
    raw = subprocess.run(["ffmpeg", "-v", "error", "-i", str(path), "-vf", "scale=54:120,format=gray", "-fps_mode",
                          "passthrough", "-f", "rawvideo", "-"], capture_output=True, check=True).stdout
    frame_size = 54 * 120
    previous = None
    for i, t in enumerate(times):
        current = raw[i * frame_size:(i + 1) * frame_size]
        if len(current) < frame_size:
            break
        if previous is not None and t > after:
            diff = sum(abs(a - b) for a, b in zip(current, previous)) / frame_size
            if diff > 1.5:
                return t
        previous = current
    raise SystemExit(f"nothing changed on screen in {path} after {after}s")


def start_recording(path):
    # The emulator's own recorder, on the host: the device's screenrecord made a loaded Mac's emulator kill the app.
    adb("emu", "screenrecord", "start", "--time-limit", "170", "--fps", "60", "--bit-rate", "24000000", str(path))


def stop_recording():
    adb("emu", "screenrecord", "stop")
    time.sleep(2)


def record_scene(scene, out_dir, raw_dir):
    name = scene["record"]
    raw = raw_dir / f"{name}.webm"
    start_recording(raw.resolve())
    if "swipe" in scene:
        x, y0, y1, ms = scene["swipe"]
        time.sleep(3)
        adb("shell", "input", "swipe", str(x), str(y0), str(x), str(y1), str(ms))
        time.sleep(3)
        stop_recording()
        frames = Path(tempfile.mkdtemp(prefix=f"store-video-{name}-"))
        try:
            subprocess.run(["ffmpeg", "-v", "error", "-i", str(raw), "-map", "0:v", "-fps_mode", "passthrough",
                            "-frame_pts", "1", "-enc_time_base", "1/1000", "-q:v", "1", str(frames / "%08d.jpg")],
                           check=True)
            scroll, worst, mean = retime(str(frames), str(out_dir / f"{name}.mp4"), *scene["retime"])
        finally:
            shutil.rmtree(frames, ignore_errors=True)
        print(f"{name}: {frame_count(raw)} frames, scrolled {scroll:.0f}px, residual max {worst:.1f}px mean {mean:.2f}px")
    else:
        time.sleep(2.5)
        tap(scene["tap"])
        if "shows" in scene:
            wait_for(scene["shows"])  # a busy emulator answers the tap late: wait for the screen it opens
        time.sleep(scene.get("after", 3))
        stop_recording()
        lead, length = scene["trim"]
        # The tap lands whenever a busy emulator gets to it: the clip starts lead seconds before the screen changes.
        start = max(0.0, first_change(raw, after=2.0) - lead)
        # Seeking after -i: before it, the recorder's uneven frame times lose the clip's last frames.
        subprocess.run(["ffmpeg", "-y", "-v", "error", "-i", str(raw), "-ss", f"{start:.3f}", "-t", str(length), "-filter_complex",
                        f"fps={FPS},{CLEAN_BAR_FILTER},format=yuv420p", "-c:v", "libx264", "-crf", "14", "-preset", "slow", "-an",
                        str(out_dir / f"{name}.mp4")], check=True)
        if frame_count(out_dir / f"{name}.mp4") < round(length * FPS) - 1:
            raise SystemExit(f"{name}: the recording ends before the {length}s the clip needs; record it again")
        print(f"{name}: {frame_count(raw)} frames, trimmed to {length}s from {start:.2f}s")


def push_assets():
    """The sample trip's photos (the receipt, the faces, the groups' pictures), the files of assets.tsv, into
    files/store-video-assets/ at the path StoreVideoAndroid.kt looks them up by (bank/ dropped)."""
    table = ROOT / "scripts/store-screenshots/assets.tsv"
    staging = "/data/local/tmp/store-video-assets"
    adb("shell", f"rm -rf {staging}")
    for line in table.read_text().splitlines():
        if not line.strip() or line.startswith("#"):
            continue
        source = line.split("\t")[1]
        target = source.removeprefix("bank/")
        adb("shell", f"mkdir -p {staging}/{os.path.dirname(target) or '.'}")
        adb("push", str(ROOT / "scripts/store-screenshots" / source), f"{staging}/{target}")
    files = f"/data/user/0/{PACKAGE}/files"
    adb("shell", f"run-as {PACKAGE} sh -c 'rm -rf {files}/store-video-assets && cp -r {staging} {files}/store-video-assets'")
    adb("shell", f"rm -rf {staging}")


FILES = f"/data/user/0/{PACKAGE}/files"
SETTINGS = [("global", "sysui_demo_allowed", "1"), ("system", "show_touches", "0")] + [
    ("global", key, "1") for key in ("window_animation_scale", "transition_animation_scale", "animator_duration_scale")
]


def app_locales():
    """The app's own languages as Android keeps them (the seed sets the video's), e.g. "pt-BR", or "" for none."""
    out = adb("shell", "cmd", "locale", "get-app-locales", PACKAGE, check=False, capture=True) or ""
    match = re.search(r"\[(.*)\]", out)
    return match.group(1).replace(" ", "") if match else ""


def launch():
    adb("shell", "am", "force-stop", PACKAGE)
    adb("shell", "monkey", "-p", PACKAGE, "-c", "android.intent.category.LAUNCHER", "1")


def wait_for_seed(seconds=240):
    """The first launch seeds the trip in the background and writes files/store-video.ready when it is done."""
    deadline = time.time() + seconds
    while time.time() < deadline:
        if adb("shell", f"run-as {PACKAGE} ls {FILES}/store-video.ready", check=False, capture=True).strip():
            return
        answer_not_responding(nodes())
        time.sleep(2)
    raise SystemExit("the sample trip was never seeded: look for StoreVideo in adb logcat")


def snapshot():
    """What the recording changes on the emulator, for teardown to put back."""
    return {
        "settings": [(table, key, (adb("shell", "settings", "get", table, key, capture=True) or "").strip())
                     for table, key, _ in SETTINGS],
        "locales": app_locales(),
    }


def setup(config):
    """Seeds the trip on a first launch, then starts the app again on the finished trip."""
    language = config["appLanguage"]
    # A freshly installed app has no files folder until it first runs.
    adb("shell", f"run-as {PACKAGE} mkdir -p {FILES}")
    push_assets()
    adb("shell", f"run-as {PACKAGE} sh -c 'rm -f {FILES}/store-video.ready; "
                 f"echo {language} {config['theme']} > {FILES}/store-video'")
    for table, key, value in SETTINGS:
        adb("shell", "settings", "put", table, key, value)
    for command in DEMO:
        demo(*command)
    for package in SUSPENDED:
        adb("shell", "pm", "suspend", package, check=False)
    launch()
    wait_for_seed()
    launch()
    wait_for(config["launchWaitFor"], seconds=120)


def teardown(saved):
    for package in SUSPENDED:
        adb("shell", "pm", "unsuspend", package, check=False)
    demo("exit")
    adb("shell", f"run-as {PACKAGE} rm -f {FILES}/store-video {FILES}/store-video.ready", check=False)
    adb("shell", "am", "force-stop", PACKAGE, check=False)
    for table, key, value in saved["settings"]:
        if value in ("", "null"):
            adb("shell", "settings", "delete", table, key, check=False)
        else:
            adb("shell", "settings", "put", table, key, value, check=False)
    adb("shell", "cmd", "locale", "set-app-locales", PACKAGE, "--locales", saved["locales"] or "''", check=False)


def stills(config, out_dir):
    """The last frame of every played stretch, which the video holds for a freeze or slides away on a push."""
    for shot in config["shots"]:
        for step in shot["steps"]:
            if step["kind"] != "play":
                continue
            index = round(step["to"] * FPS) - 1
            target = out_dir / f"{step['clip']}_{index}.png"
            target.unlink(missing_ok=True)
            subprocess.run(["ffmpeg", "-y", "-v", "error", "-i", str(out_dir / f"{step['clip']}.mp4"), "-vf",
                            f"select=eq(n\\,{index})", "-frames:v", "1", "-update", "1", str(target)], check=True)
            if not target.exists():
                raise SystemExit(f"{step['clip']}.mp4 has no frame {index} ({step['to']}s); record it again")


def main():
    locale = sys.argv[1]
    only = set(sys.argv[2:])
    config = json.loads((Path(__file__).parent / "locales" / f"{locale}.json").read_text())
    out_dir = ROOT / "build/store-video" / locale / "public"
    raw_dir = ROOT / "build/store-video" / locale / "raw"
    out_dir.mkdir(parents=True, exist_ok=True)
    raw_dir.mkdir(parents=True, exist_ok=True)
    saved = snapshot()
    try:
        setup(config)
        for scene in config["scenes"]:
            name = scene.get("record") or scene.get("still")
            skip = bool(only) and name is not None and name not in only
            if "record" in scene and not skip:
                record_scene(scene, out_dir, raw_dir)
            elif "still" in scene and not skip:
                screen = subprocess.run(["adb", "exec-out", "screencap", "-p"], capture_output=True, check=True).stdout
                subprocess.run(["ffmpeg", "-y", "-v", "error", "-f", "png_pipe", "-i", "-", "-filter_complex",
                                CLEAN_BAR_FILTER, str(out_dir / f"{scene['still']}.png")], input=screen, check=True)
            elif "record" not in scene and "still" not in scene:
                if "tap" in scene:
                    tap(scene["tap"])
                if "backUntil" in scene:
                    back_until(scene["backUntil"])
                if "scrollUpUntil" in scene:
                    scroll_up_until(scene["scrollUpUntil"])
                if "swipe" in scene:
                    x, y0, y1, ms = scene["swipe"]
                    adb("shell", "input", "swipe", str(x), str(y0), str(x), str(y1), str(ms))
            if "waitFor" in scene:
                wait_for(scene["waitFor"])
    finally:
        teardown(saved)
    stills(config, out_dir)


if __name__ == "__main__":
    os.chdir(Path(__file__).parent)
    main()
