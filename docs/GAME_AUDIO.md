# Game audio: research, direction and licences

Owner: games engine. Engine code: `src/main/java/.../ui/games/common/GameAudio.java`.
Game-show music and cue generator: `tools/game_audio/show.py`. Assets: `src/main/assets/games/audio/`.

## Spoken game reactions

All 14 games have a short win line, a retry line and a help line. The British English takes use the same speaker at the original pitch. The clips and their caption text are bundled in `assets/games/audio/announcer/` and play offline.

- **Win:** Plays after the completed result has been saved. The line also appears with the result.
- **Retry:** Plays on an incorrect attempt or a failed run. Brief cooldowns keep several quick misses from speaking over one another.
- **Help:** Plays only when a player asks for a hint, tip or lifeline. The written clue remains available without audio.

The **Sound** button is available inside every game. Music, voice and effects have independent switches and volumes. Voice only, Effects only and Voice + effects are quick mixes that turn off music. The preview uses the current game without changing progress.

Wins use one of four jingles matched to the game. Retry and help have shorter cues. The illustrated reaction stickers reuse Perazim game art. Win confetti follows the celebrations and system motion settings. Voice follows phone silent mode and app lifecycle. One line plays at a time, with music and other game sounds lowered under speech.


## 1. What makes game audio work (research summary)

Notes from studying how top casual, word and puzzle games use sound. These are practices, not copied audio.

- **Reward juice.** Match-3 and word games pair each success with a short rising tone. The reward grows with the streak.
- **Layered combo pitch-ups.** Each step of a streak sits a semitone or so above the last. The player hears the streak build without reading the score.
- **Short UI ticks.** Taps are 50 to 100 ms soft transients, never long. They must not mask the music.
- **Gentle failure.** A wrong answer is a soft, low, falling sound, not a buzz. This matters most for children.
- **Adaptive music loops.** A seamless loop of 20 to 30 seconds per game. Music sits quiet under play and is the emotional identity of each game.
- **Ducking.** Music dips briefly while an effect plays, so the cue is heard clearly.
- **Respect the context.** Music pauses in the background, stops on audio focus loss, and stays off in silent or vibrate mode.

## 2. Audio direction

The faster arrangements use game-show brass, arcade-style plucks, bells, and
light percussion. The 14 games each have an original hook and rhythm. The hub
has its own separate track.

| Game | BPM | Direction |
|---|---:|---|
| Bible Quiz | 144 | Bright brass quiz-show groove |
| Who am I? | 132 | Syncopated detective plucks |
| Verse Builder | 134 | Bell arpeggios over a steady beat |
| Ladder of Faith | 152 | Rising brass and a driving beat |
| Word Search | 146 | Quick arcade plucks and shakers |
| Memory Match | 138 | Playful repeating bells |
| Story Timeline | 128 | Flute over an adventure rhythm |
| Road to Jerusalem | 154 | Quick travelling flute and drums |
| Old or New? | 130 | Bouncy brass and offbeat bass |
| Who Said It? | 140 | Playful call-and-response plucks |
| Fill the Gap | 148 | Quick bells with a syncopated rhythm |
| Vine Words | 142 | Bright flute and light percussion |
| Lamp Slide | 136 | Syncopated plucks over a puzzle beat |
| Story Mosaic | 126 | Bells over an adventure rhythm |
| Games hub | 122 | Light plucks and a steady beat |

Each round opens with an original game-show-style stinger. Correct submissions
play a rising bell and one of four positive voice clips. Incorrect submissions
play a lower two-note cue and one of four encouraging voice clips. The judgment
cue takes priority over incidental taps. On the last answer, the level-result
reaction follows the answer cue with no delay to saving or the UI. Voices use
Microsoft Ryan, British English, with the original pitch. Silent mode and the
independent Voice and Sound effects switches still apply.

## 3. How the audio is made

The base effects are synthesised in `tools/game_audio/synth.py` from Karplus-Strong plucked strings (harp and lyre),
breathy vibrato flute, chimes, soft pads, filtered noise and a short reverb tail. Each game has a four-note
motif drawn from its own pentatonic scale, played on the last bar of each phrase. Combo sounds rise one
semitone per step. Loops are wrapped through the reverb so they repeat seamlessly.

Three effects were generated with ElevenLabs (Sound Effects) and are kept as they are:
`sfx_tap`, `sfx_wrong` (soft falling flute) and `sfx_combo1`. Each was generated once, with a plain prompt,
and converted to OGG Vorbis, mono, 22050 Hz with a loudness normalisation.

ElevenLabs music generation was not used: the account ran out of credits and the calls failed with "Insufficient funds".

Run the synth in a scratch output folder to regenerate the base music and effects. Keep the three existing ElevenLabs cues, the six reaction cues, and the announcer recordings when copying results back:

    python3 tools/game_audio/synth.py /tmp/perazim-base-game-audio

Output is OGG Vorbis, mono, 22050 Hz. Effects are about 4 to 13 KB each. Music loops are about 55 to 115 KB
each. The bundled audio folder contains base effects, game tracks, and voice clips.

Render the active soundtrack and stingers with NumPy and FFmpeg:

    python3 tools/game_audio/show.py src/main/assets/games/audio

The show script writes the 15 tracks and the three round/answer cues only.
The eight short answer clips and the 42 earlier game-specific lines are
separate OGG files under `announcer/`. The **Sound** panel previews the
intro, correct cue, incorrect cue, and current game's win mix.

## 4. Licences

- Synthesised music and effects (except the three ElevenLabs effects) are original Perazim project work,
  generated by the script above. They are licensed with the project. No copyrighted music, samples or recordings are included.
- `sfx_tap`, `sfx_wrong`, `sfx_combo1`: generated with ElevenLabs Sound Effects on the project's account.
  Use is covered by ElevenLabs' terms for generated audio on the plan in use. Check those terms before a
  store release, and replace these files if the plan does not cover commercial app use.

- The announcer takes in `announcer/` were generated with Microsoft Ryan, British English, for the local validation build.

## 5. Engine behaviour

- Effects play through `SoundPool`. Music loops through `MediaPlayer`.
- Music, voice, and effects volumes (0 to 100) and on/off switches are stored per account under the owner's score prefix.
- Music ducks to about 35% for a short time while an effect plays.
- Audio focus: loss stops the music, transient loss pauses it, and regained focus resumes it.
- Background: music pauses when no app activity is started, and resumes on return.
- Silent or vibrate mode: all game sounds are off.
- If a sound cannot be loaded, the game continues with visual feedback and captions.
- Everything works offline; no network is used.

## 6. Open choices for Levi

- Taste check on the polished synthesised set (see the PR and sample files).
- Top up ElevenLabs credits later if richer music is wanted; that needs a licence check first.
- Whether to replace the synthesised tracks with recorded or licensed instruments later (costs and licence work).
