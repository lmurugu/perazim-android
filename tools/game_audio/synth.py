#!/usr/bin/env python3
"""Procedural sound design for the Perazim games.

Everything here is synthesised from sine partials, Karplus-Strong plucked strings, filtered noise,
envelopes and a small reverb, so the assets are original work with no licence to track.
Output: OGG Vorbis, mono, 22050 Hz.

Three effects (sfx_tap, sfx_wrong, sfx_combo1) were generated with ElevenLabs and are kept as they are:
they are listed in KEEP and are skipped here. Run:
    python3 tools/game_audio/synth.py <out_dir>      (needs numpy and ffmpeg with libvorbis)
"""
import os
import subprocess
import sys
import tempfile
import wave
import zlib

import numpy as np

SR = 22050
KEEP = {"sfx_tap", "sfx_wrong", "sfx_combo1"}


def midi(n):
    return 440.0 * 2 ** ((n - 69) / 12.0)


def seeded(name):
    return np.random.default_rng(zlib.crc32(name.encode()))


RNG = np.random.default_rng(7)


def env(n, attack, decay):
    t = np.arange(n) / SR
    return np.minimum(1.0, t / max(attack, 1e-4)) * np.exp(-t / max(decay, 1e-3))


def pluck(f, dur, amp=0.5, bright=0.5):
    """Karplus-Strong string: a noise burst fed through a delay line with a damped average."""
    n = int(SR * dur)
    period = max(2, int(round(SR / f)))
    buf = RNG.uniform(-1, 1, period)
    out = np.zeros(n)
    damp = 0.996 - (1 - bright) * 0.01
    for i in range(n):
        out[i] = buf[0]
        buf[:-1] = buf[1:]
        buf[-1] = damp * 0.5 * (out[i] + buf[-2])
    return amp * out * env(n, 0.002, dur * 0.4)


def harp(f, dur, amp=0.5):
    # Plucked string plus a little body partial, so it reads as a harp rather than a beep.
    n = int(SR * dur)
    t = np.arange(n) / SR
    body = 0.15 * np.sin(2 * np.pi * f * 2 * t) * np.exp(-t * 6)
    return amp * (pluck(f, dur, 1.0, 0.7)[:n] + body) * 0.7


def lyre(f, dur, amp=0.45):
    return pluck(f, dur, amp, 0.4)


def flute(f, dur, amp=0.35):
    n = int(SR * dur)
    t = np.arange(n) / SR
    vib = 1 + 0.005 * np.sin(2 * np.pi * 5.0 * t) * np.clip((t - 0.15) / 0.3, 0, 1)
    tone = np.sin(2 * np.pi * f * t * vib) + 0.12 * np.sin(4 * np.pi * f * t) + 0.04 * np.sin(6 * np.pi * f * t)
    noise = seeded("breath").standard_normal(n)
    breath = np.convolve(noise, np.ones(9) / 9, mode="same") * 0.05
    return amp * (tone + breath) * env(n, 0.1, dur * 0.6)


def chime(f, dur, amp=0.35):
    n = int(SR * dur)
    t = np.arange(n) / SR
    tone = np.sin(2 * np.pi * f * t) + 0.45 * np.sin(2 * np.pi * f * 2.76 * t) * np.exp(-t * 4) \
        + 0.2 * np.sin(2 * np.pi * f * 5.4 * t) * np.exp(-t * 7)
    return amp * tone * env(n, 0.002, dur * 0.5)


def pad(f, dur, amp=0.1):
    n = int(SR * dur)
    t = np.arange(n) / SR
    tone = sum(np.sin(2 * np.pi * f * d * t + k) for k, d in enumerate([1.0, 1.004, 0.996, 2.0, 3.0]))
    swell = np.clip(np.minimum(1, t / (dur * 0.35)) * np.minimum(1, (dur - t) / (dur * 0.3)), 0, 1)
    return amp * tone * swell * 0.4


def hand_drum(dur=0.22, amp=0.55, pitch=110):
    n = int(SR * dur)
    t = np.arange(n) / SR
    sweep = pitch * (1 + 2.5 * np.exp(-t * 30))
    body = np.sin(2 * np.pi * np.cumsum(sweep) / SR) * np.exp(-t * 14)
    slap = 0.2 * RNG.standard_normal(n) * np.exp(-t * 90)
    return amp * (body + slap)


def shaker(dur=0.08, amp=0.12):
    n = int(SR * dur)
    t = np.arange(n) / SR
    hp = np.diff(RNG.standard_normal(n), prepend=0)
    return amp * hp * np.exp(-t * 60)


def reverb(x, wet=0.22, seconds=0.9):
    """A soft reverb tail: a decaying noise impulse, convolved and mixed back in."""
    t = np.arange(int(SR * seconds)) / SR
    ir = seeded("reverb").standard_normal(len(t)) * np.exp(-t * 5.5)
    ir[0] = 1.0
    ir /= np.sqrt(np.sum(ir ** 2))
    wetsig = np.convolve(x, ir)[: len(x)]
    return (1 - wet) * x + wet * wetsig


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


# ---- Shared sound effects (the ones not kept from ElevenLabs) ----------------

def sfx_correct():
    b = np.zeros(int(SR * 0.5))
    place(b, 0.0, harp(midi(76), 0.4, 0.5))
    place(b, 0.07, harp(midi(83), 0.4, 0.5))
    place(b, 0.07, chime(midi(88), 0.3, 0.12))
    return normalize(reverb(b, 0.2, 0.4), 0.7)


def sfx_star():
    b = np.zeros(int(SR * 0.8))
    for k, n in enumerate([76, 80, 83, 88]):
        place(b, k * 0.07, harp(midi(n), 0.55, 0.45))
    return normalize(reverb(b, 0.25, 0.6), 0.75)


def sfx_complete():
    b = np.zeros(int(SR * 2.0))
    for t, n in [(0, 60), (0.12, 64), (0.24, 67), (0.36, 72), (0.6, 71), (0.72, 72), (0.9, 76)]:
        place(b, t, harp(midi(n), 1.0, 0.5))
    place(b, 0.9, chime(midi(84), 1.1, 0.25))
    place(b, 0.9, pad(midi(72), 1.0, 0.12))
    return normalize(reverb(b, 0.3, 1.0), 0.8)


def sfx_combo(step):
    # Pitch rises one semitone per step, so the tone climbs as a streak builds (step 2..3).
    b = np.zeros(int(SR * 0.35))
    place(b, 0.0, chime(midi(79 + step), 0.25, 0.45))
    place(b, 0.05, harp(midi(91 + step), 0.25, 0.25))
    return normalize(reverb(b, 0.15, 0.4), 0.6)


def sfx_hint():
    b = np.zeros(int(SR * 0.7))
    place(b, 0.0, flute(midi(79), 0.55, 0.35))
    place(b, 0.18, chime(midi(86), 0.45, 0.2))
    return normalize(reverb(b, 0.2, 0.5), 0.6)


# ---- Music: one identity per game -----------------------------------------

# root MIDI note, bpm, chord progression (offsets of chord roots, minor flag), lead voice,
# drum pattern and pad. Warm, worshipful, joyful; each game has its own colour and motif.
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

LEAD_FN = {"harp": harp, "lyre": lyre, "flute": flute, "chime": chime}
QUALITY = [0, 4, 7]
MINOR = [0, 3, 7]


def motif_for(name, root):
    """A four-note motif per game, drawn from the game's own scale, so each track has a fingerprint."""
    rng = seeded("motif-" + name)
    scale = [0, 2, 4, 7, 9]  # major pentatonic: calm and open
    steps = rng.choice(len(scale) * 2, size=4)
    return [root + 12 + scale[s % 5] + 12 * (s // 5) for s in steps]


def render_music(name, cfg):
    beat = 60.0 / cfg["bpm"]
    bars = 8
    dur = beat * 4 * bars
    buf = np.zeros(int(SR * dur))
    root = cfg["root"]
    lead = LEAD_FN[cfg["lead"]]
    motif = motif_for(name, root)
    for bar, (offset, minor) in enumerate(cfg["chords"] * 2):
        t0 = bar * 4 * beat
        chord = MINOR if minor else QUALITY
        base = root + offset
        if cfg["pad"]:
            loop_place(buf, t0, pad(midi(base - 12), 4 * beat, 0.07))
        for b in (0, 2):
            loop_place(buf, t0 + b * beat, harp(midi(base - 12), beat * 1.6, 0.2))
        if bar % 4 == 3:
            # The motif, played on the last bar of each phrase, so the track has a memorable hook.
            for k, n in enumerate(motif):
                loop_place(buf, t0 + k * beat / 2, LEAD_FN[cfg["lead"]](midi(n), beat * 0.9, 0.3))
        else:
            for step in range(8):
                note = base + chord[step % 3] + 12 * ((step // 3) % 2)
                loop_place(buf, t0 + step * beat / 2, lead(midi(note), beat * 0.9, 0.2))
        drums = cfg["drums"]
        for b in range(4):
            tb = t0 + b * beat
            if drums == "light" and b in (0, 2):
                loop_place(buf, tb, hand_drum(amp=0.22, pitch=90))
            elif drums == "hand":
                loop_place(buf, tb, hand_drum(amp=0.3 if b != 3 else 0.2, pitch=120))
                loop_place(buf, tb + beat / 2, hand_drum(amp=0.14, pitch=140))
            elif drums == "shaker":
                loop_place(buf, tb + beat / 2, shaker())
                loop_place(buf, tb, shaker(amp=0.08))
    # A reverb tail on a loop would smear the seam, so the loop is wrapped through the reverb once.
    wrapped = np.concatenate([buf, buf])
    out = reverb(wrapped, 0.18, 0.9)[len(buf): 2 * len(buf)]
    return normalize(out, 0.6)


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
        "sfx_correct": sfx_correct(), "sfx_star": sfx_star(), "sfx_complete": sfx_complete(),
        "sfx_combo2": sfx_combo(2), "sfx_combo3": sfx_combo(3), "sfx_hint": sfx_hint(),
    }
    for name, data in effects.items():
        if name in KEEP:
            continue
        wav = os.path.join(tmp, name + ".wav")
        write_wav(wav, data)
        to_ogg(wav, os.path.join(out, name + ".ogg"), 2)
    for game, cfg in GAMES.items():
        wav = os.path.join(tmp, "music_" + game + ".wav")
        write_wav(wav, render_music(game, cfg))
        to_ogg(wav, os.path.join(out, "music_" + game + ".ogg"), 1)
    print("wrote effects and", len(GAMES), "music loops to", out)


if __name__ == "__main__":
    main()
