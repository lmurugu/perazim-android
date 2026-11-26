#!/usr/bin/env python3
"""Render Perazim's original game-show cues and fast gameplay loops."""
import argparse
from pathlib import Path
from tempfile import TemporaryDirectory

import numpy as np

from synth import GAMES, SR, chime, flute, hand_drum, loop_place, midi, seeded, shaker, to_ogg, write_wav

# BPM, lead instrument, bass/kick pattern, notes in the game's four-note hook.
ARRANGEMENTS = {
    "bible_quiz":       (144, "brass", (0, 1.5, 2, 3.5), (0, 7, 9, 12)),
    "who_am_i":         (132, "pluck", (0, 1.75, 2.5, 3.5), (0, 3, 7, 5)),
    "verse_builder":    (134, "bell", (0, 1, 2.5, 3.5), (0, 4, 7, 9)),
    "ladder_of_faith":  (152, "brass", (0, 1, 2, 3), (0, 4, 9, 12)),
    "word_search":      (146, "pluck", (0, 1.5, 2.5, 3), (7, 4, 2, 7)),
    "memory_match":     (138, "bell", (0, 1.5, 2.5, 3.5), (0, 4, 0, 7)),
    "story_timeline":   (128, "flute", (0, 1, 2, 3.5), (0, 2, 7, 4)),
    "road_to_jerusalem":(154, "flute", (0, 0.75, 2, 2.75), (7, 9, 12, 14)),
    "old_or_new":       (130, "brass", (0, 1.5, 2, 3.25), (4, 2, 0, 7)),
    "who_said_it":      (140, "pluck", (0, 0.75, 2.5, 3.5), (4, 7, 12, 9)),
    "fill_the_gap":     (148, "bell", (0, 1, 2, 3.5), (4, 7, 9, 4)),
    "vine_words":      (142, "flute", (0, 1.5, 2.5, 3.5), (0, 4, 9, 7)),
    "lamp_slide":      (136, "pluck", (0, 1.5, 2, 3.25), (0, 7, 4, 2)),
    "story_mosaic":    (126, "bell", (0, 0.75, 2, 3), (0, 7, 3, 12)),
    "hub":             (122, "pluck", (0, 1.5, 2, 3.5), (0, 2, 4, 7)),
}


def synth_note(note, seconds, kind, gain):
    frequency = midi(note)
    if kind == "bell":
        return chime(frequency, seconds, gain)
    if kind == "flute":
        return flute(frequency, seconds, gain)
    t = np.arange(int(SR * seconds)) / SR
    phase = 2 * np.pi * frequency * t
    fundamental = np.sin(phase)
    if kind == "brass":
        tone = fundamental + 0.42 * np.sin(2 * phase) + 0.23 * np.sin(3 * phase)
    elif kind == "pluck":
        tone = fundamental + np.exp(-t * 14) * (0.45 * np.sin(2 * phase) + 0.3 * np.sin(3 * phase))
    else:
        tone = fundamental + 0.18 * np.sin(2 * phase)
    envelope = np.minimum(1, t / 0.009) * np.clip((seconds - t) / 0.04, 0, 1) * np.exp(-t / seconds)
    return gain * tone * envelope


def render_loop(game):
    bpm, lead, syncopation, hook = ARRANGEMENTS[game]
    beat = 60 / bpm
    buf = np.zeros(round(32 * beat * SR))
    rng = seeded("groove-" + game)
    root = GAMES[game]["root"]
    for bar, (offset, minor) in enumerate(GAMES[game]["chords"] * 2):
        start = 4 * bar * beat
        chord = (0, 3 if minor else 4, 7)
        note = root + offset
        for i, pulse in enumerate(syncopation):
            time = start + pulse * beat
            bass = note - 24 + (7 if i == 3 else 0)
            loop_place(buf, time, synth_note(bass, 0.54 * beat, "bass", 0.22))
            loop_place(buf, time, hand_drum(0.17, 0.18 if i == 0 else 0.10, 82))
        for pulse in (1, 3):
            loop_place(buf, start + beat * pulse, shaker(amp=0.12))
            loop_place(buf, start + beat * pulse, hand_drum(0.13, 0.10, 172))
        for pulse in (0.5, 1.5, 2.5, 3.5):
            loop_place(buf, start + beat * pulse, shaker(amp=float(rng.uniform(0.07, 0.11))))
        for pulse in (0.75, 2.75):
            for step in chord:
                loop_place(buf, start + beat * pulse, synth_note(note + step, 0.36 * beat, "pluck", 0.053))
        melody = hook if bar in (0, 3, 4, 7) else (chord[0], chord[2], chord[1], chord[2])
        for pulse, step in zip((0, 1.25, 2.25, 3.5), melody):
            loop_place(buf, start + pulse * beat, synth_note(note + step, 0.65 * beat, lead, 0.15))
    # A short wrapped reflection keeps the boundary smooth.
    buf += 0.12 * np.roll(buf, round(0.087 * SR))
    return buf * (0.49 / max(abs(buf)))


def render_cue(notes, length, lead):
    buf = np.zeros(round(length * SR))
    for start, note, duration, gain in notes:
        loop_place(buf, start, synth_note(note, duration, lead, gain))
    fade = round(0.04 * SR)
    buf[-fade:] *= np.linspace(1, 0, fade)
    return buf * (0.74 / max(abs(buf)))


CUES = {
    "sfx_show_start": ([(0.00, 55, .14, .24), (.14, 62, .15, .28), (.28, 67, .16, .30),
                        (.43, 72, .56, .34), (.43, 76, .56, .22), (.43, 79, .56, .14)], 1.04, "brass"),
    "sfx_answer_correct": ([(0, 79, .22, .31), (.10, 86, .40, .34), (.10, 91, .36, .14)], .53, "bell"),
    "sfx_answer_incorrect": ([(0, 55, .14, .27), (.13, 49, .28, .31), (.13, 56, .21, .12)], .46, "brass"),
}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("output", type=Path)
    folder = parser.parse_args().output
    folder.mkdir(parents=True, exist_ok=True)
    with TemporaryDirectory() as scratch:
        wave = Path(scratch) / "render.wav"
        for name, cfg in CUES.items():
            write_wav(str(wave), render_cue(*cfg))
            to_ogg(wave, folder / f"{name}.ogg", 3)
        for game in ARRANGEMENTS:
            write_wav(str(wave), render_loop(game))
            to_ogg(wave, folder / f"music_{game}.ogg", 3)


if __name__ == "__main__":
    main()
