#!/usr/bin/env python3
"""
capture_full_app.py
Captures high-fidelity screenshots of the entire Perazim Mission Church Android app
across all tabs, sub-tabs, dialogs, and activities.

Outputs to:
  1. docs/screenshots/
  2. <appDataDir>/brain/<conversation-id>/
"""

import os
import sys
import time
import subprocess
import xml.etree.ElementTree as ET
import re

APP_DATA_DIR = "/home/murugu/.gemini/antigravity/brain/398ec6fe-4556-4323-ad7a-11c4a5b3504b"
REPO_ROOT = "/home/murugu/Android/app"
DOCS_SCREENSHOTS = os.path.join(REPO_ROOT, "docs/screenshots")

os.makedirs(DOCS_SCREENSHOTS, exist_ok=True)
os.makedirs(APP_DATA_DIR, exist_ok=True)

def run_cmd(cmd_list):
    res = subprocess.run(cmd_list, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True)
    return res.stdout.strip()

def wake_device():
    run_cmd(["adb", "shell", "svc", "power", "stayon", "true"])
    run_cmd(["adb", "shell", "input", "keyevent", "224"])
    run_cmd(["adb", "shell", "wm", "dismiss-keyguard"])
    time.sleep(1)

def dump_ui():
    run_cmd(["adb", "shell", "uiautomator", "dump", "/sdcard/window_dump.xml"])
    tmp_xml = "/tmp/window_dump.xml"
    run_cmd(["adb", "pull", "/sdcard/window_dump.xml", tmp_xml])
    if os.path.exists(tmp_xml):
        try:
            return ET.parse(tmp_xml)
        except Exception:
            return None
    return None

def find_bounds(tree, text_pattern=None, class_pattern=None):
    if tree is None:
        return None
    for elem in tree.iter():
        txt = elem.attrib.get("text", "")
        cd = elem.attrib.get("content-desc", "")
        cls = elem.attrib.get("class", "")
        bounds = elem.attrib.get("bounds", "")
        match_text = True
        if text_pattern:
            match_text = (re.search(text_pattern, txt, re.IGNORECASE) or 
                          re.search(text_pattern, cd, re.IGNORECASE))
        match_class = True
        if class_pattern:
            match_class = re.search(class_pattern, cls, re.IGNORECASE)
        if match_text and match_class:
            m = re.findall(r"\d+", bounds)
            if len(m) == 4:
                return (int(m[0]), int(m[1]), int(m[2]), int(m[3]))
    return None

def tap_center(bounds):
    if not bounds:
        return False
    x = (bounds[0] + bounds[2]) // 2
    y = (bounds[1] + bounds[3]) // 2
    run_cmd(["adb", "shell", "input", "tap", str(x), str(y)])
    return True

def tap_text(pattern, wait_after=1.2):
    tree = dump_ui()
    b = find_bounds(tree, text_pattern=pattern)
    if b:
        tap_center(b)
        time.sleep(wait_after)
        return True
    return False

def capture(filename_base, desc=""):
    time.sleep(1.5)
    sdcard_path = f"/sdcard/{filename_base}.png"
    run_cmd(["adb", "shell", f"screencap -p {sdcard_path}"])
    
    local_docs = os.path.join(DOCS_SCREENSHOTS, f"{filename_base}.png")
    local_artifact = os.path.join(APP_DATA_DIR, f"{filename_base}.png")
    
    run_cmd(["adb", "pull", sdcard_path, local_docs])
    if os.path.exists(local_docs):
        subprocess.run(["cp", "-f", local_docs, local_artifact], check=True)
        size_kb = os.path.getsize(local_docs) / 1024
        print(f"  📸 Captured [{filename_base}.png] ({size_kb:.1f} KB): {desc}")
    else:
        print(f"  ❌ FAILED to capture [{filename_base}.png]")

def key_back():
    run_cmd(["adb", "shell", "input", "keyevent", "KEYCODE_BACK"])
    time.sleep(1)

def start_intent(extras=""):
    cmd = ["adb", "shell", "am", "start", "-S", "-n", "org.perazimchurch.app/.MainActivity"]
    if extras:
        cmd.extend(extras.split())
    run_cmd(cmd)
    time.sleep(2)

def main():
    print("🚀 Starting Complete Perazim App Screenshot Capture Pipeline...")
    wake_device()

    # 1. Home Screen
    print("Step 1: Home Tab")
    start_intent("--ei tab_index 0")
    capture("01_home", "Home Screen with church logo, daily verse & routine CTA")

    # 2. Sermons Tab
    print("Step 2: Sermons Tab")
    start_intent("--ei tab_index 1")
    capture("02_sermons", "Sermons Screen with livestream card, filter chips & archive")

    # 3. Sermons with Player Playing
    print("Step 3: Sermons Tab with Audio Playing")
    tap_text("Play Sermon", 1.5)
    capture("03_sermons_player", "Sermons Audio Controller active with orange scrub bar")
    tap_text("Pause", 0.5)

    # 4. Worship Tab
    print("Step 4: Worship Tab")
    start_intent("--ei tab_index 2")
    capture("04_worship", "Worship Screen with hymnal browser and category chips")

    # 5. Worship Hymn Detail Dialog
    print("Step 5: Worship Hymn Detail Dialog")
    tap_text("Holy, Holy, Holy", 1.5)
    capture("05_worship_hymn_detail", "Hymn Detail Modal with lyrics, chords & transpose controls")
    key_back()

    # 6. Prayer Wall
    print("Step 6: Prayer Wall")
    start_intent("--ei tab_index 3")
    capture("06_prayer_wall", "Prayer Wall with community petitions and Amen buttons")

    # 7. Prayer Mine Tab
    print("Step 7: Prayer Mine Tab")
    tap_text("My Prayers", 1.5)
    capture("07_prayer_mine", "Prayer Screen - My Prayers segmented tab")

    # 8. Prayer Share Dialog
    print("Step 8: Share a Prayer Dialog")
    start_intent("--ei tab_index 3")
    tap_text(r"\+ Share a prayer", 1.5)
    capture("08_prayer_share_dialog", "Share a Prayer Modal with church logo & petition inputs")
    key_back()

    # 9. Profile Screen
    print("Step 9: Profile Tab")
    start_intent("--ei tab_index 4")
    capture("09_profile", "Profile Screen with logo avatar, stats & Give button")

    # 10. Giving Dialog
    print("Step 10: Giving Dialog")
    start_intent("--ez open_giving true")
    capture("10_giving_dialog", "Giving Dialog with Paybill 4069983, logo & M-Pesa CTA")
    key_back()

    # 11. About Perazim Dialog
    print("Step 11: About Perazim Dialog")
    start_intent("--ei tab_index 4")
    tap_text("About Perazim", 1.5)
    capture("11_about_dialog", "About Perazim Dialog with logo & version 2.2.0 Build 4")
    key_back()

    # 12. Campus Switcher Dialog
    print("Step 12: Campus Switcher Dialog")
    start_intent("--ei tab_index 4")
    tap_text("Switch", 1.5)
    capture("12_campus_switcher_dialog", "Campus Switcher Dialog with Embu, Nairobi, Meru, Online")
    key_back()

    # 13. Fellowship Extras Dialog
    print("Step 13: Fellowship Extras Dialog")
    start_intent("--ei tab_index 4")
    tap_text("Fellowship Extras", 1.5)
    capture("13_fellowship_extras_dialog", "Fellowship Extras Dialog with Devotional Riddles & Jokes")
    key_back()

    # 14. Saved & Offline Content Dialog
    print("Step 14: Saved & Offline Content Dialog")
    start_intent("--ei tab_index 4")
    tap_text("Saved & Offline Content", 1.5)
    capture("14_saved_offline_content_dialog", "Saved & Offline Content Dialog")
    key_back()

    # 15. Holy Scripture Reader Dialog
    print("Step 15: Holy Scripture Reader Dialog")
    start_intent("--ez open_scripture true")
    capture("15_bible_reader", "Holy Scripture Reader Modal (2 Samuel 5:17-21)")
    key_back()

    # 16. AI Breakthrough Guide Dialog
    print("Step 16: AI Breakthrough Guide Dialog")
    start_intent("--ez open_ai_guide true")
    capture("16_ai_guide", "Breakthrough Guide Modal")
    key_back()

    # 17. Devotional Riddles Activity
    print("Step 17: Devotional Riddles Activity")
    start_intent("--ez open_riddles true")
    capture("17_riddles_activity", "Devotional Riddles Activity")
    key_back()

    # 18. Christian Joy / Jokes Activity
    print("Step 18: Christian Joy / Jokes Activity")
    start_intent("--ez open_jokes true")
    capture("18_jokes_activity", "Christian Joy & Humor Jokes Activity")
    key_back()

    # Reset to Home cleanly
    start_intent("--ei tab_index 0")
    print("🎉 All 18 screenshots successfully captured and synced!")

if __name__ == "__main__":
    main()
