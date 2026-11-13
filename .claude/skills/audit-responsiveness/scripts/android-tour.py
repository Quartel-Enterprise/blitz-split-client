#!/usr/bin/env python3
"""Walk the Android app's main screens on the task's emulator and screenshot each one.

  ANDROID_ADB_SERVER_PORT=<port + 1000> android-tour.py <serial> <output directory> <prefix>

The app must already hold a profile, one group and one bill (see the skill for the data to create), with the
recovery code sheet dismissed through "I've saved it". Elements are found by their English text through
`uiautomator dump`, so the tour survives any window size; set the device language to English first. The group and the
bill are found by the start of their names: TOUR_GROUP (default "Trip to Rio") and TOUR_BILL (default "Dinner").
Prints "!! not found" for a step it could not take: on a list and detail layout the tabs it leaves out are expected
(the rail has no "Activity" on a pane), anything else is worth a look. Ends by calling contact-sheet.py.
"""
import os
import re
import subprocess
import sys
import time

SERIAL, OUT, PREFIX = sys.argv[1], sys.argv[2], sys.argv[3]
GROUP = os.environ.get("TOUR_GROUP", "Trip to Rio")
BILL = os.environ.get("TOUR_BILL", "Dinner")
PACKAGE = "com.quare.blitzsplit"
HERE = os.path.dirname(os.path.abspath(__file__))
count = 0


def adb(*args):
    return subprocess.run(["adb", "-s", SERIAL, *args], capture_output=True).stdout


def nodes():
    adb("shell", "uiautomator", "dump", "/sdcard/tour.xml")
    xml = adb("shell", "cat", "/sdcard/tour.xml").decode("utf8", "replace")
    found = []
    for node in re.findall(r"<node [^>]*>", xml):
        def attribute(name):
            match = re.search(name + r'="([^"]*)"', node)
            return match.group(1) if match else ""
        bounds = re.match(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", attribute("bounds"))
        if bounds:
            found.append((attribute("text") or attribute("content-desc"), tuple(map(int, bounds.groups()))))
    return found


def tap(text, starts=False, wait=2.5):
    matches = [n for n in nodes() if (n[0].startswith(text) if starts else n[0] == text)]
    if not matches:
        print("  !! not found:", text)
        return False
    x1, y1, x2, y2 = matches[0][1]
    adb("shell", "input", "tap", str((x1 + x2) // 2), str((y1 + y2) // 2))
    time.sleep(wait)
    return True


def back(wait=1.5):
    adb("shell", "input", "keyevent", "4")
    time.sleep(wait)


def shot(name):
    global count
    count += 1
    path = os.path.join(OUT, f"{PREFIX}-{count:02d}-{name}.png")
    with open(path, "wb") as file:
        file.write(adb("exec-out", "screencap", "-p"))


def window():
    size = adb("shell", "wm", "size").decode().strip().splitlines()[-1]
    return tuple(map(int, re.search(r"(\d+)x(\d+)\s*$", size).groups()))


def swipe(from_fraction, to_fraction):
    width, height = window()
    adb("shell", "input", "swipe", str(width // 2), str(int(height * from_fraction)), str(width // 2),
        str(int(height * to_fraction)), "300")
    time.sleep(1.5)


def launch():
    adb("shell", "am", "force-stop", PACKAGE)
    adb("shell", "am", "start", "-n", f"{PACKAGE}/.MainActivity")
    time.sleep(6)


launch()
shot("home")
swipe(0.7, 0.3)
shot("home-scrolled")
swipe(0.3, 0.8)
for tab in ["Groups", "Balances", "Contacts"]:
    if tap(tab):
        shot(tab.lower())
tap("Home")
if tap("Activity"):
    shot("activity")
    back()
if tap("Profile"):
    shot("profile")
    swipe(0.7, 0.3)
    shot("profile-scrolled")
    back()
tap("Groups")
if tap(GROUP, starts=True):
    shot("group")
    for tab in ["Balances", "Members"]:
        if tap(tab):
            shot("group-" + tab.lower())
    tap("Expenses")
    if tap("Statistics"):
        shot("statistics")
        swipe(0.7, 0.3)
        shot("statistics-scrolled")
        back()
    if tap(BILL, starts=True):
        shot("bill")
        swipe(0.7, 0.3)
        shot("bill-scrolled")
        back()
    if tap("More group options"):
        shot("group-menu")
        back()
    if tap("Invite people"):
        shot("invite")
        back()
launch()
if tap("Search"):
    shot("search")
    adb("shell", "input", "text", BILL[:3].lower())
    time.sleep(2)
    shot("search-results")
print("screens:", count)
subprocess.run([sys.executable, os.path.join(HERE, "contact-sheet.py"), OUT, PREFIX])
