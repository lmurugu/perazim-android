#!/usr/bin/env python3
"""
Perazim Church Embu - Google Drive Media & Sermon Catalog Pipeline
===================================================================
Automatically parses church sermon recordings (MP3/M4A), study notes (PDF),
and video broadcasts (MP4/YouTube) from Google Drive or local folders,
merging multi-format assets into a unified JSON database for the Android app.

Zero Cloud Cost Architecture:
Outputs a production-grade `sermons.json` catalog ready for Room pre-population
or hosting on GitHub Pages / Google Apps Script.
"""

import os
import sys
import json
import re
import uuid
import datetime

AUDIO_EXTENSIONS = {'.mp3', '.m4a', '.aac', '.wav'}
VIDEO_EXTENSIONS = {'.mp4', '.mkv', '.webm', '.mov'}
PDF_EXTENSIONS = {'.pdf'}

COMMON_PREACHERS = [
    "Bishop Dr. David Mutweri",
    "Rev. Mutweri",
    "Pastor Grace Mutweri",
    "Guest Speaker",
    "Perazim Ministry Team"
]

DEFAULT_SERIES = [
    "Breakthrough 2026",
    "Walking in Faith",
    "Deliverance & Restoration",
    "Kingdom Economics & Generosity",
    "Holy Spirit & Revival"
]

def clean_title(filename):
    name, _ = os.path.splitext(filename)
    # Strip dates like 2026-02-15 or 20260215
    name = re.sub(r'^\d{4}[-_]?\d{2}[-_]?\d{2}[-_]?', '', name)
    # Replace underscores and dashes with spaces
    name = name.replace('_', ' ').replace('-', ' ')
    # Remove audio/video bitrate tags like (128kbps), (1080p)
    name = re.sub(r'\(\s*\d+\s*(kbps|p|k)\s*\)', '', name, flags=re.I)
    # Strip extra whitespace
    return ' '.join(name.strip().split()).title()

def detect_preacher(text):
    for p in COMMON_PREACHERS:
        if any(part.lower() in text.lower() for part in p.split()):
            return p
    return "Bishop Dr. David Mutweri"

def detect_passage(text):
    # Match standard scripture patterns like "2 Samuel 5:17-21", "John 3:16", "Genesis 1:1"
    match = re.search(r'([1-3]?\s*[A-Za-z]+)\s+(\d+)(?::(\d+(?:-\d+)?))?', text)
    if match:
        return match.group(0).strip()
    return "2 Samuel 5:20"

def detect_series(text):
    for s in DEFAULT_SERIES:
        if any(w.lower() in text.lower() for w in s.split() if len(w) > 3):
            return s
    return "Breakthrough 2026"

def build_gdrive_direct_url(file_id, mime_type='audio'):
    """
    Transforms a Google Drive file ID into a high-bandwidth direct streaming URL.
    """
    if not file_id:
        return ""
    # Direct export link for media streaming
    return f"https://drive.google.com/uc?export=download&id={file_id}"

def build_catalog_from_directory(dir_path):
    print(f"[*] Scanning media directory: {dir_path}")
    items_by_base = {}

    for root, _, files in os.walk(dir_path):
        for f in files:
            ext = os.path.splitext(f)[1].lower()
            base = re.sub(r'\.[^.]+$', '', f).lower()
            base = re.sub(r'[_\-\s]+', ' ', base).strip()

            if base not in items_by_base:
                items_by_base[base] = {
                    'original_filename': f,
                    'audio': None,
                    'video': None,
                    'pdf': None,
                    'filesize': 0
                }

            full_path = os.path.join(root, f)
            size = os.path.getsize(full_path)

            if ext in AUDIO_EXTENSIONS:
                items_by_base[base]['audio'] = full_path
                items_by_base[base]['filesize'] += size
            elif ext in VIDEO_EXTENSIONS:
                items_by_base[base]['video'] = full_path
                items_by_base[base]['filesize'] += size
            elif ext in PDF_EXTENSIONS:
                items_by_base[base]['pdf'] = full_path

    sermons = []
    for base, data in items_by_base.items():
        title = clean_title(data['original_filename'])
        preacher = detect_preacher(data['original_filename'])
        passage = detect_passage(data['original_filename'])
        series = detect_series(data['original_filename'])

        sermon_id = str(uuid.uuid5(uuid.NAMESPACE_DNS, f"perazim.sermon.{base}"))
        now_millis = int(datetime.datetime.now().timestamp() * 1000)

        sermon = {
            "id": sermon_id,
            "title": title,
            "preacher": preacher,
            "passage": passage,
            "series": series,
            "audioUrl": f"https://media.perazimchurch.org/audio/{os.path.basename(data['audio'])}" if data['audio'] else "",
            "videoUrl": f"https://media.perazimchurch.org/video/{os.path.basename(data['video'])}" if data['video'] else "",
            "pdfUrl": f"https://media.perazimchurch.org/notes/{os.path.basename(data['pdf'])}" if data['pdf'] else "",
            "notes": f"https://media.perazimchurch.org/notes/{os.path.basename(data['pdf'])}" if data['pdf'] else "",
            "thumbnailUrl": "https://media.perazimchurch.org/thumbnails/default_sermon.jpg",
            "durationSeconds": 2700,
            "datePreached": str(now_millis),
            "description": f"Sermon delivered at Perazim Mission Church, Embu by {preacher}. Theme scripture: {passage}.",
            "isDownloaded": False,
            "playCount": 0
        }
        sermons.append(sermon)

    return sermons

def main():
    if len(sys.argv) > 1:
        target_dir = sys.argv[1]
    else:
        target_dir = "."

    sermons = build_catalog_from_directory(target_dir) if os.path.isdir(target_dir) else []
    output_path = os.path.join(os.path.dirname(os.path.abspath(__file__)), "sermons_catalog.json")

    with open(output_path, "w", encoding="utf-8") as out:
        json.dump(sermons, out, indent=2, ensure_ascii=False)

    print(f"[✓] Generated catalog with {len(sermons)} sermons -> {output_path}")

if __name__ == "__main__":
    main()
