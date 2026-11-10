#!/usr/bin/env python3
"""Fail-closed, redacted inspection of an Android release APK."""

import os
import re
import shutil
import sqlite3
import subprocess
import sys
import tempfile
import zipfile
import hashlib
from pathlib import Path


DENIED_MARKERS = (
    b"AI_APP_TOKEN", b"X-App-Token", b"AI_RELAY_URL",
    b"perazim-ai-relay.perazim-mission.workers.dev",
    b"docs.google.com/spreadsheets", b"spreadsheets.google.com",
    b"10.0.2.2:3000", b"http://127.0.0.1:3000", b"http://localhost:3000",
)
SECRET_PATTERNS = (
    re.compile(rb"AIza[0-9A-Za-z_-]{35}"),
    re.compile(rb"\bsk-[A-Za-z0-9]{20,}\b"),
    re.compile(rb"(?i)\bBearer\s+[A-Za-z0-9._~-]{16,}"),
    re.compile(rb"(?i)(?:api[_-]?key|access[_-]?token|client[_-]?secret)\s*[:=]\s*[\"'][^\"']{12,}[\"']"),
)
SENSITIVE_NAME = re.compile(r"(?i)(member.?contacts|membership.?register|member.?register|notebook.?photo|real.?member|test.?credentials|totp.?seed)")
KENYAN_PHONE = re.compile(rb"(?<![0-9])(?:\+?254|0)(?:7|1)[0-9]{8}(?![0-9])")
SENSITIVE_TABLE = re.compile(r"(?i)(member|membership|claim|contact|prayer|message|credential|session|admin|user|token)")
EXPECTED_ASSET_IMAGES = {
    "bishop_portrait.jpg",
    *(f"default_avatars/avatar_{i:02d}.png" for i in range(1, 21)),
    *(f"backgrounds/{name}.png" for name in (
        "bg_about_sanctuary", "bg_ai_guide_flame", "bg_give_harvest", "bg_home_dawn_cross",
        "bg_hymn_lyre", "bg_jokes_dove", "bg_me_path", "bg_offline_ark", "bg_prayer_moon",
        "bg_reader_scroll", "bg_riddles_lantern", "bg_sermons_lamp", "bg_sermons_lamp_portrait",
        "bg_splash_breakthrough", "bg_worship_dusk")),
    *(f"vignettes/{name}.png" for name in (
        "vignette_campus_embu", "vignette_campus_mwea", "vignette_campus_rombo",
        "vignette_empty_candle", "vignette_empty_dove", "vignette_empty_hill", "vignette_offline_dove")),
}


def tool_path(name):
    candidates = [shutil.which(name)]
    sdk = os.environ.get("ANDROID_HOME") or os.environ.get("ANDROID_SDK_ROOT")
    if sdk:
        if name == "apkanalyzer":
            candidates += [str(p) for p in Path(sdk).glob("cmdline-tools/*/bin/apkanalyzer")]
        elif name == "apksigner":
            candidates += [str(p) for p in Path(sdk).glob("build-tools/*/apksigner")]
        elif name == "aapt2":
            candidates += [str(p) for p in Path(sdk).glob("build-tools/*/aapt2")]
    for candidate in candidates:
        if candidate and Path(candidate).is_file():
            return candidate
    return None


def run(args):
    return subprocess.run(args, check=False, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True)


def inspect_database(zf, entry, tempdir):
    target = Path(tempdir) / "bundled.db"
    target.write_bytes(zf.read(entry))
    try:
        db = sqlite3.connect(f"file:{target}?mode=ro", uri=True)
        tables = [row[0] for row in db.execute("SELECT name FROM sqlite_master WHERE type='table'")]
        prohibited = []
        for table in tables:
            if SENSITIVE_TABLE.search(table):
                quoted = '"' + table.replace('"', '""') + '"'
                count = db.execute(f"SELECT COUNT(*) FROM {quoted}").fetchone()[0]
                if count:
                    prohibited.append(table)
        db.close()
        return prohibited
    except sqlite3.Error:
        return ["unreadable bundled database"]


def main():
    ci_unsigned = len(sys.argv) == 3 and sys.argv[1] == "--ci-unsigned"
    if ci_unsigned:
        apk = Path(sys.argv[2])
        if apk.name != "app-release-unsigned.apk":
            print("FAIL: CI unsigned mode requires the explicitly unsigned release artifact")
            return 2
    elif len(sys.argv) == 2:
        apk = Path(sys.argv[1])
    else:
        print("Usage: inspect_release_apk.py [--ci-unsigned] <apk>")
        return 2
    if not apk.is_file():
        print("FAIL: release APK is missing")
        return 1

    failures = []
    avatar_count = 0
    image_entries = []
    db_entries = []
    privacy_entries = []
    names = []
    with zipfile.ZipFile(apk) as zf:
        names = zf.namelist()
        for name in names:
            low = name.lower()
            if SENSITIVE_NAME.search(Path(name).name):
                failures.append("sensitive or prohibited asset filename present")
                # Never read a sensitive-named APK entry, even if a build
                # accidentally packages one. Filename detection is sufficient
                # to fail closed.
                continue
            if low.startswith("assets/default_avatars/") and low.endswith(".png"):
                avatar_count += 1
            if low.endswith((".png", ".jpg", ".jpeg", ".webp", ".bmp")):
                image_entries.append(name)
            if low.startswith("assets/privacy/") and ("notice" in low or "policy" in low):
                privacy_entries.append(name)
            if low.endswith((".db", ".sqlite", ".sqlite3")):
                db_entries.append(name)
            try:
                content = zf.read(name)
            except Exception:
                failures.append("unreadable APK entry")
                continue
            if any(marker in content for marker in DENIED_MARKERS):
                failures.append("prohibited credential, endpoint, or debug marker present")
            if any(pattern.search(content) for pattern in SECRET_PATTERNS):
                failures.append("credential-shaped value present")
            if name.startswith("assets/") and low.endswith((".csv", ".json", ".txt")) and KENYAN_PHONE.search(content):
                failures.append("phone-number-shaped data present")
            if name.endswith(".dex") and any(marker in content for marker in (b"SHEET_URL_", b"AI_APP_TOKEN", b"AI_RELAY_URL")):
                failures.append("unapproved remote configuration compiled into DEX")
        if avatar_count != 20:
            failures.append("expected set of 20 default avatar PNGs is incomplete")
        for name in image_entries:
            low = name.lower()
            if re.search(r"(?i)(notebook|journal|camera|img[_-]?\d|dsc[_-]?\d|pxl[_-]?\d)", name):
                failures.append("personal-capture or notebook image name present")
            if low.startswith("assets/") and name[len("assets/"):] not in EXPECTED_ASSET_IMAGES:
                failures.append("unreviewed image asset present")
        for index in range(1, 21):
            entry = f"assets/default_avatars/avatar_{index:02d}.png"
            source = Path("src/main/assets/default_avatars") / f"avatar_{index:02d}.png"
            if entry not in names or not source.is_file() or hashlib.sha256(zf.read(entry)).digest() != hashlib.sha256(source.read_bytes()).digest():
                failures.append("packaged avatar differs from the approved source asset")
        if not privacy_entries:
            failures.append("privacy notice and policy assets are missing")
        for name in privacy_entries:
            if b"DRAFT" not in zf.read(name).upper():
                failures.append("privacy document is not visibly marked as a draft")
        with tempfile.TemporaryDirectory(prefix="perazim-apk-check-") as tempdir:
            for entry in db_entries:
                if inspect_database(zf, entry, tempdir):
                    failures.append("bundled database contains restricted rows or is unreadable")

    analyzer = tool_path("apkanalyzer")
    if not analyzer:
        failures.append("Android apkanalyzer is unavailable; manifest check could not run")
    else:
        manifest = run([analyzer, "manifest", "print", str(apk)])
        if manifest.returncode != 0:
            failures.append("APK manifest could not be decoded")
        else:
            text = manifest.stdout
            if not re.search(r'usesCleartextTraffic="false"', text):
                failures.append("manifest does not explicitly disable cleartext traffic")
            if "networkSecurityConfig" not in text and "networkSecurityConfig" not in manifest.stdout:
                failures.append("manifest does not reference a network security configuration")

    signer = tool_path("apksigner")
    if not signer:
        failures.append("apksigner is unavailable; signature check could not run")
    else:
        verified = run([signer, "verify", str(apk)])
        if ci_unsigned:
            if verified.returncode == 0:
                failures.append("CI unsigned artifact is unexpectedly signed")
        elif verified.returncode != 0:
            failures.append("APK signature verification failed")

    if failures:
        for failure in sorted(set(failures)):
            print("FAIL:", failure)
        print("APK inspection failed; no detected values are displayed.")
        return 1
    if ci_unsigned:
        print("PASS: explicitly unsigned CI release APK inspected; signature absence confirmed.")
        print("NOT FOR DISTRIBUTION: unsigned CI artifact is not a signed release.")
    else:
        print("PASS: signed release APK inspection; no prohibited values detected or displayed.")
        print("PASS: manifest transport policy and APK signature verified.")
    print("PASS: 20 default avatar files match their source SHA-256; privacy documents remain marked DRAFT.")
    print(f"PASS: {len(db_entries)} bundled SQLite databases checked for restricted records.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
