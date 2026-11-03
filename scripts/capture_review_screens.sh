#!/usr/bin/env bash
# ==============================================================================
# capture_review_screens.sh
# Captures screenshots and UI text dumps of all main Perazim app screens
# for visual audit and honesty verification.
#
# Output:
#   docs/review/<commit-hash>-<screen>.png
#   docs/review/<commit-hash>-<screen>.txt
#
# PRIVACY SAFEGUARD:
#   Only navigates internal org.perazimchurch.app screens and dialogs.
#   Never launches third-party apps (WhatsApp, M-Pesa, SMS messages, Call log).
# ==============================================================================

set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$REPO_ROOT"

COMMIT_HASH="$(git rev-parse --short HEAD 2>/dev/null || echo "HEAD")"
OUT_DIR="docs/review"
mkdir -p "$OUT_DIR"

# Resolve ADB device
DEVICE="${ADB_DEVICE:-}"
if [ -z "$DEVICE" ]; then
    DEVICE="$(adb devices | grep -w "device" | head -n 1 | awk '{print $1}')"
fi

if [ -z "$DEVICE" ]; then
    echo "❌ Error: No authorized ADB device found."
    exit 1
fi

echo "📱 Using connected device: $DEVICE"
echo "📌 Commit Hash: $COMMIT_HASH"
echo "📂 Output Directory: $OUT_DIR"

# Ensure device display is awake and unlocked
adb -s "$DEVICE" shell svc power stayon true >/dev/null 2>&1 || true
adb -s "$DEVICE" shell input keyevent 224 >/dev/null 2>&1 || true
adb -s "$DEVICE" shell wm dismiss-keyguard >/dev/null 2>&1 || true
sleep 1

# Helper to dump text and capture screen
capture_screen() {
    local screen_name="$1"
    local png_path="${OUT_DIR}/${COMMIT_HASH}-${screen_name}.png"
    local txt_path="${OUT_DIR}/${COMMIT_HASH}-${screen_name}.txt"

    echo "  📸 Capturing: $screen_name..."
    sleep 1

    # 1. Screen capture
    adb -s "$DEVICE" shell screencap -p "/sdcard/rev_screen.png"
    adb -s "$DEVICE" pull "/sdcard/rev_screen.png" "$png_path" >/dev/null 2>&1

    # 2. UI hierarchy text dump
    adb -s "$DEVICE" shell uiautomator dump "/sdcard/rev_ui.xml" >/dev/null 2>&1 || true
    adb -s "$DEVICE" pull "/sdcard/rev_ui.xml" "/tmp/rev_ui.xml" >/dev/null 2>&1 || true

    if [ -f "/tmp/rev_ui.xml" ]; then
        python3 -c "
import xml.etree.ElementTree as ET
try:
    tree = ET.parse('/tmp/rev_ui.xml')
    texts = []
    for elem in tree.iter():
        t = elem.attrib.get('text', '').strip()
        cd = elem.attrib.get('content-desc', '').strip()
        cls = elem.attrib.get('class', '')
        if t:
            texts.append(f'[{cls}] {t}')
        elif cd:
            texts.append(f'[{cls}] (desc) {cd}')
    with open('$txt_path', 'w', encoding='utf-8') as f:
        f.write('\n'.join(texts) + '\n')
except Exception as e:
    with open('$txt_path', 'w') as f:
        f.write(f'UI dump error: {e}\n')
"
    else
        echo "No UI dump available" > "$txt_path"
    fi
}

# 1. Launch MainActivity at Home
echo "▶️ Launching Perazim app..."
adb -s "$DEVICE" shell am start -S -n org.perazimchurch.app/.MainActivity >/dev/null 2>&1
sleep 2

# Screen 01: Home Tab
adb -s "$DEVICE" shell input tap 72 1535 # Tab 0 Home
capture_screen "01-home"

# Screen 02: Sermons Tab
adb -s "$DEVICE" shell input tap 216 1535 # Tab 1 Sermons
capture_screen "02-sermons"

# Screen 03: Worship Tab
adb -s "$DEVICE" shell input tap 360 1535 # Tab 2 Worship
capture_screen "03-worship"

# Screen 04: Fellowship Tab
adb -s "$DEVICE" shell input tap 504 1535 # Tab 3 Fellowship
capture_screen "04-fellowship"

# Screen 05: Profile Tab
adb -s "$DEVICE" shell input tap 648 1535 # Tab 4 Profile
capture_screen "05-profile"

# Screen 06: Giving Dialog
adb -s "$DEVICE" shell am start -n org.perazimchurch.app/.MainActivity --ez open_giving true >/dev/null 2>&1
sleep 2
capture_screen "06-giving-dialog"
adb -s "$DEVICE" shell input keyevent KEYCODE_BACK
sleep 1

# Screen 07: Submit Prayer Dialog (From Fellowship)
adb -s "$DEVICE" shell am start -S -n org.perazimchurch.app/.MainActivity --ei tab_index 3 >/dev/null 2>&1
sleep 2
adb -s "$DEVICE" shell input tap 360 484 # Tap SUBMIT PRAYER button
sleep 1
capture_screen "07-prayer-dialog"
adb -s "$DEVICE" shell input keyevent KEYCODE_BACK # Dismiss Prayer dialog
sleep 1

# Screen 08: Holy Scripture Reader Dialog
adb -s "$DEVICE" shell am start -S -n org.perazimchurch.app/.MainActivity --ez open_scripture true >/dev/null 2>&1
sleep 2
capture_screen "08-bible-reader"
adb -s "$DEVICE" shell input keyevent KEYCODE_BACK
sleep 1

# Screen 09: AI Breakthrough Guide Dialog
adb -s "$DEVICE" shell am start -S -n org.perazimchurch.app/.MainActivity --ez open_ai_guide true >/dev/null 2>&1
sleep 2
capture_screen "09-ai-guide"
adb -s "$DEVICE" shell input keyevent KEYCODE_BACK
sleep 1

# Screen 10: Riddles Screen
adb -s "$DEVICE" shell am start -S -n org.perazimchurch.app/.MainActivity --ez open_riddles true >/dev/null 2>&1
sleep 2
capture_screen "10-riddles"
adb -s "$DEVICE" shell input keyevent KEYCODE_BACK
sleep 1

# Screen 11: Jokes Screen
adb -s "$DEVICE" shell am start -S -n org.perazimchurch.app/.MainActivity --ez open_jokes true >/dev/null 2>&1
sleep 2
capture_screen "11-jokes"
adb -s "$DEVICE" shell input keyevent KEYCODE_BACK
sleep 1

# Return to Home clean
adb -s "$DEVICE" shell am start -S -n org.perazimchurch.app/.MainActivity >/dev/null 2>&1

echo "✅ Capture complete! Review artifacts saved in $OUT_DIR/"
ls -lh "$OUT_DIR/${COMMIT_HASH}-"*
