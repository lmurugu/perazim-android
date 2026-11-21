#!/usr/bin/env python3
"""Procedural sound design for the Perazim games.

Every sound is synthesised here from sine partials, envelopes and filtered noise, so the
assets are original work with no licence to track. Output: OGG Vorbis, mono, 22050 Hz.
Run:  python3 tools/game_audio/synth.py <out_dir>   (needs numpy and ffmpeg with libvorbis)
"""
import os
import subprocess
import sys
import tempfile
import wave

import numpy as np

SR = 22050
rng = np.random.default_rng(7)


def midi(n):
    return 440.0 * 2 ** ((n - 69) / 12.0)


def env(n, attack, decay):
    t = np.arange(n) / SR
    a = np.minimum(1.0, t / max(attack, 1e-4))
    return a * np.exp(-t / max(decay, 1e-3))


def harp(f, dur, amp=0.5):
    n = int(SR * dur)
    t = np.arange(n) / SR
    tone = np.sin(2 * np.pi * f * t) + 0.35 * np.sin(4 * np.pi * f * t) + 0.12 * np.sin(6 * np.pi * f * t)
    return amp * tone * env(n, 0.003, dur * 0.45)


def lyre(f, dur, amp=0.45):
    n = int(SR * dur)
    t = np.arange(n) / SR
    tone = np.sin(2 * np.pi * f * t) + 0.5 * np.sin(4 * np.pi * f * t + 0.3) + 0.2 * np.sin(8 * np.pi * f * t)
    return amp * tone * env(n, 0.004, dur * 0.35)


def flute(f, dur, amp=0.35):
    n = int(SR * dur)
    t = np.arange(n) / SR
    vib = 1 + 0.004 * np.sin(2 * np.pi * 5.2 * t) * np.minimum(1, t / 0.4)
    tone = np.sin(2 * np.pi * f * t * vib) + 0.08 * np.sin(4 * np.pi * f * t)
    breath = 0.03 * rng.standard_normal(n) * np.exp(-t * 3)
    return amp * (tone + breath) * env(n, 0.08, dur * 0.6)


def chime(f, dur, amp=0.35):
    n = int(SR * dur)
    t = np.arange(n) / SR
    tone = np.sin(2 * np.pi * f * t) + 0.5 * np.sin(2 * np.pi * f * 2.76 * t) * np.exp(-t * 4) \
        + 0.25 * np.sin(2 * np.pi * f * 5.4 * t) * np.exp(-t * 7)
    return amp * tone * env(n, 0.002, dur * 0.5)


def pad(f, dur, amp=0.12):
    n = int(SR * dur)
    t = np.arange(n) / SR
    tone = sum(np.sin(2 * np.pi * f * d * t + k) for k, d in enumerate([1.0, 1.003, 0.997, 2.0]))
    swell = np.minimum(1, t / (dur * 0.35)) * np.minimum(1, (dur - t) / (dur * 0.3))
    return amp * tone * np.clip(swell, 0, 1)


def hand_drum(dur=0.22, amp=0.55, pitch=110):
    n = int(SR * dur)
    t = np.arange(n) / SR
    sweep = pitch * (1 + 2.5 * np.exp(-t * 30))
    body = np.sin(2 * np.pi * np.cumsum(sweep) / SR) * np.exp(-t * 14)
    slap = 0.25 * rng.standard_normal(n) * np.exp(-t * 90)
    return amp * (body + slap)


def shaker(dur=0.08, amp=0.12):
    n = int(SR * dur)
    t = np.arange(n) / SR
    noise = rng.standard_normal(n)
    hp = np.diff(noise, prepend=0)  # crude high-pass
    return amp * hp * np.exp(-t * 60)


def place(buf, start_s, sig):
    i = int(start_s * SR)
    if i >= len(buf):
        return
    end = min(len(buf), i + len(sig))
    buf[i:end] += sig[: end - i]


def loop_place(buf, start_s, sig):
    """Places a note into a loop buffer, wrapping its tail to the start so the loop stays seamless."""
    i = int(start_s * SR) % len(buf)
    for k in range(0, len(sig), len(buf)):
        seg = sig[k: k + len(buf)]
        j = (i + k) % len(buf)
        end = j + len(seg)
        if end <= len(buf):
            buf[j:end] += seg
        else:
            cut = len(buf) - j
            buf[j:] += seg[:cut]
            buf[: len(seg) - cut] += seg[cut:]


def normalize(x, peak=0.8):
    m = np.max(np.abs(x)) or 1
    return x / m * peak


# ---- Shared sound effects -------------------------------------------------

def sfx_tap():
    return normalize(chime(midi(84), 0.09, 0.5), 0.5)


def sfx_correct():
    b = np.zeros(int(SR * 0.45))
    place(b, 0.0, chime(midi(76), 0.35))
    place(b, 0.07, chime(midi(83), 0.35))
    return normalize(b, 0.7)


def sfx_wrong():
    # Gentle, low and soft: a falling flute pair. Never a buzz or harsh tone.
    b = np.zeros(int(SR * 0.38))
    place(b, 0.0, flute(midi(64), 0.22, 0.4))
    place(b, 0.13, flute(midi(60), 0.25, 0.35))
    return normalize(b, 0.55)


def sfx_star():
    b = np.zeros(int(SR * 0.7))
    for k, n in enumerate([76, 80, 83, 88]):
        place(b, k * 0.07, harp(midi(n), 0.5, 0.45))
    return normalize(b, 0.75)


def sfx_complete():
    b = np.zeros(int(SR * 1.8))
    seq = [(0, 60), (0.12, 64), (0.24, 67), (0.36, 72), (0.6, 71), (0.72, 72), (0.9, 76)]
    for t, n in seq:
        place(b, t, harp(midi(n), 0.9, 0.5))
    place(b, 0.9, chime(midi(84), 1.0, 0.3))
    place(b, 0.9, pad(midi(72), 0.9, 0.08))
    return normalize(b, 0.8)


def sfx_combo(step):
    # Pitch rises one semitone per step, so the tone climbs as a streak builds (step 1..3).
    b = np.zeros(int(SR * 0.3))
    place(b, 0.0, chime(midi(79 + step), 0.25, 0.45))
    place(b, 0.05, harp(midi(91 + step), 0.2, 0.25))
    return normalize(b, 0.6)


def sfx_hint():
    b = np.zeros(int(SR * 0.6))
    place(b, 0.0, flute(midi(79), 0.5, 0.35))
    place(b, 0.18, chime(midi(86), 0.4, 0.2))
    return normalize(b, 0.6)


# ---- Music: one identity per game ------------------------------------------

# root MIDI note, bpm, chord progression (semitone offsets of chord roots from root, minor flag),
# lead instrument, drum pattern, pad on/off. Warm, worshipful, joyful; each game has its own colour.
GAMES = {
    "bible_quiz":     dict(root=62, bpm=76, chords=[(0, 0), (7, 0), (9, 1), (5, 0)], lead="harp", drums="light", pad=True),
    "who_am_i":       dict(root=57, bpm=70, chords=[(0, 1), (8, 0), (5, 0), (7, 0)], lead="flute", drums="none", pad=True),
    "verse_builder":  dict(root=64, bpm=80, chords=[(0, 0), (5, 0), (7, 0), (0, 0)], lead="chime", drums="light", pad=False),
    "ladder_of_faith": dict(root=60, bpm=66, chords=[(0, 0), (9, 1), (5, 0), (7, 0)], lead="lyre", drums="hand", pad=True),
    "word_search":    dict(root=67, bpm=84, chords=[(0, 0), (4, 0), (9, 1), (5, 0)], lead="harp", drums="shaker", pad=False),
    "memory_match":   dict(root=65, bpm=78, chords=[(0, 0), (9, 1), (2, 1), (7, 0)], lead="chime", drums="none", pad=True),
    "story_timeline": dict(root=59, bpm=72, chords=[(0, 0), (5, 0), (7, 0), (0, 0)], lead="lyre", drums="light", pad=True),
    "road_to_jerusalem": dict(root=62, bpm=92, chords=[(0, 0), (7, 0), (5, 0), (7, 0)], lead="flute", drums="hand", pad=False),
    "old_or_new":     dict(root=64, bpm=74, chords=[(0, 0), (9, 1), (5, 0), (7, 0)], lead="harp", drums="light", pad=True),
    "who_said_it":    dict(root=60, bpm=70, chords=[(0, 0), (5, 0), (9, 1), (7, 0)], lead="flute", drums="none", pad=True),
    "fill_the_gap":   dict(root=62, bpm=82, chords=[(0, 0), (7, 0), (9, 1), (7, 0)], lead="chime", drums="shaker", pad=False),
    "vine_words":     dict(root=67, bpm=88, chords=[(0, 0), (5, 0), (9, 1), (7, 0)], lead="lyre", drums="shaker", pad=False),
    "lamp_slide":     dict(root=65, bpm=86, chords=[(0, 0), (4, 0), (9, 1), (5, 0)], lead="chime", drums="light", pad=True),
    "story_mosaic":   dict(root=60, bpm=76, chords=[(0, 0), (8, 0), (5, 0), (7, 0)], lead="harp", drums="light", pad=True),
    "hub":            dict(root=62, bpm=74, chords=[(0, 0), (5, 0), (7, 0), (0, 0)], lead="harp", drums="none", pad=True),
}

QUALITY_CHORD = [0, 4, 7]
MINOR_CHORD = [0, 3, 7]
LEAD_FN = {"harp": harp, "lyre": lyre, "flute": flute, "chime": chime}


def render_music(cfg):
    beat = 60.0 / cfg["bpm"]
    bars = 8
    dur = beat * 4 * bars
    buf = np.zeros(int(SR * dur))
    root = cfg["root"]
    lead = LEAD_FN[cfg["lead"]]
    for bar, (offset, minor) in enumerate(cfg["chords"] * 2):
        t0 = bar * 4 * beat
        chord = MINOR_CHORD if minor else QUALITY_CHORD
        base = root + offset
        if cfg["pad"]:
            loop_place(buf, t0, pad(midi(base - 12), 4 * beat, 0.07))
        # Bass pulse on beats 1 and 3.
        for b in (0, 2):
            loop_place(buf, t0 + b * beat, harp(midi(base - 12), beat * 1.6, 0.22))
        # Arpeggio through chord tones on eighth notes, lead line on the top voice.
        for step in range(8):
            note = base + chord[step % 3] + 12 * ((step // 3) % 2)
            loop_place(buf, t0 + step * beat / 2, lead(midi(note), beat * 0.9, 0.22))
        drums = cfg["drums"]
        for b in range(4):
            tb = t0 + b * beat
            if drums == "light" and b in (0, 2):
                loop_place(buf, tb, hand_drum(amp=0.25, pitch=90))
            elif drums == "hand":
                loop_place(buf, tb, hand_drum(amp=0.3 if b != 3 else 0.2, pitch=120))
                loop_place(buf, tb + beat / 2, hand_drum(amp=0.15, pitch=140))
            elif drums == "shaker":
                loop_place(buf, tb + beat / 2, shaker())
                loop_place(buf, tb, shaker(amp=0.08))
    return normalize(buf, 0.6)


def write_wav(path, samples):
    pcm = (np.clip(samples, -1, 1) * 32767).astype("<i2")
    with wave.open(path, "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(SR)
        w.writeframes(pcm.tobytes())


def to_ogg(wav_path, ogg_path, quality):
    subprocess.run(["ffmpeg", "-hide_banner", "-loglevel", "error", "-y", "-i", wav_path,
                    "-c:a", "libvorbis", "-q:a", str(quality), "-ac", "1", "-ar", str(SR), ogg_path], check=True)


def main():
    out = sys.argv[1]
    os.makedirs(out, exist_ok=True)
    tmp = tempfile.mkdtemp()
    effects = {
        "sfx_tap": sfx_tap(), "sfx_correct": sfx_correct(), "sfx_wrong": sfx_wrong(),
        "sfx_star": sfx_star(), "sfx_complete": sfx_complete(), "sfx_hint": sfx_hint(),
        "sfx_combo1": sfx_combo(1), "sfx_combo2": sfx_combo(2), "sfx_combo3": sfx_combo(3),
    }
    for name, data in effects.items():
        wav = os.path.join(tmp, name + ".wav")
        write_wav(wav, data)
        to_ogg(wav, os.path.join(out, name + ".ogg"), 2)
    for game, cfg in GAMES.items():
        wav = os.path.join(tmp, "music_" + game + ".wav")
        write_wav(wav, render_music(cfg))
        to_ogg(wav, os.path.join(out, "music_" + game + ".ogg"), 1)
    print("wrote", len(effects), "effects and", len(GAMES), "music loops to", out)


if __name__ == "__main__":
    main()
