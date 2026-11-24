# Games tab: design concepts and mechanics (v1, 2026-11-21)

Owner: design thread. Code thread: feature/games-depth (PR 51). This file is design only.
Every name, character, level and piece of art here is original. The genres are
familiar (match, sort, bubble shooter, runner, daily word, card collection). No
existing game's name, characters, art, music or look is copied.

## 1. Design rules (apply to every game)

These come from project decisions and must not be broken for engagement numbers.

- **Works offline.** No network call inside a game. Content ships in the app.
- **No money.** No ads, no loot boxes, no paid energy, no pay-to-skip.
- **No engagement tricks.** No forced losses, no fake near-misses, no streak guilt,
  no "come back or lose it" messages. A missed day costs nothing.
- **No upload.** Scores stay on the phone. Leaderboards wait for the privacy decision.
- **Child-safe.** No gore, no fear scares, no villains shown as people. Enemies are
  thorns, storms, walls and doubt. Bible violence is never shown on screen.
- **Scripture stays accurate.** KJV text for quotes and verses (public domain).
  NASB only if it is in the user's own file. No ESV.
- **One card layout.** Icon, Bible character, title, one line, best score, Play.
  Titles and lines use the same size (the Games tab rule).
- **Gentle help.** Every game has a hint or a mercy option (retry the same puzzle,
  a lifeline, or a "read the verse" card). Help never costs points.
- **Calm pace.** Timers are optional and off by default. Sounds are short and soft.

## 2. Recommended first wave (6 games)

Ranked by fit with the app: how well it runs offline, how little art it needs, and
how well it teaches Scripture.

| # | Game (working title) | Genre it borrows from | Scripture hook | Effort |
|---|---|---|---|---|
| 1 | Manna Match | Match-3 tile swap | Manna in the wilderness, gathering daily | Medium |
| 2 | Fruit of the Spirit | Colour sort puzzle | Galatians 5:22-23, nine fruits | Medium |
| 3 | Seven Trumpets | Brick breaker | Jericho's walls fall (Joshua 6) | Medium |
| 4 | Dove Pop | Bubble shooter | Doves carry the olive branch (Genesis 8) | Low |
| 5 | Shepherd's Path | Endless lane runner | Psalm 23, the good shepherd guides | Medium |
| 6 | Daily Name | Daily word guessing | One Bible name each day, from a bundled list | Low |

Keep the existing eight games as they are. The code thread finishes Who Said It?.

## 3. Concepts

### 3.1 Manna Match (match-3)

- **Board:** 7 by 7. Tiles are bread loaves, grape clusters, fish, olives, and
  a golden manna jar (a joker tile).
- **Play:** Swap two neighbouring tiles. Three or more in a row clears and drops
  new tiles. Each clear fills a basket on the side.
- **Goal per level:** Fill the basket for the level (for example, 12 manna, 3 fish).
  Levels are grouped into "journeys" of the wilderness: Sinai, the Red Sea shore,
  the desert road.
- **Twist:** A manna jar placed in a 4-match (a "gathering jar") clears its whole
  row. A "gather only what you need" rule: tiles beyond the goal do nothing, so
  there is no reward for hoarding.
- **Gentle help:** A "Rest" button shuffles the board once per level. A verse card
  shows Exodus 16:4 at each journey start. No timer. Moves run out, and the player
  can retry the same level at once.
- **Why it fits:** Match play is calm and very well known. It works fully offline.
- **Art:** 6 tile icons, 3 backgrounds (desert, sea, road), 1 gathering jar.

### 3.2 Fruit of the Spirit (colour sort)

- **Board:** Up to 9 jars. Each filled with layers of nine fruit colours. Two empty
  jars are kept as spares.
- **Play:** Tap a jar, then tap another jar to pour its top layer. A layer can only
  go on top of the same fruit or an empty jar.
- **Goal:** Every jar holds one fruit type. Each fruit has a name and its verse
  (love, joy, peace, patience, kindness, goodness, faithfulness, gentleness,
  self-control).
- **Twist:** Levels add one "stillness" move per puzzle as a free undo. Finishing a
  fruit shows a short card of what that fruit looks like in daily life.
- **Gentle help:** Undo is free and unlimited. A "show me one move" hint is free.
- **Art:** 9 fruit icons (grapes, a peace dove, an olive branch, a golden fig,
  a wheat sheaf, and so on). Each is a flat cartoon, not a food ad.
- **Levels:** 60 hand-made starts plus a random generator with a seed, so the same
  seed gives the same board.

### 3.3 Seven Trumpets (brick breaker)

- **Board:** A stone wall of Jericho bricks. A paddle moves at the bottom.
- **Play:** Slide the paddle to bounce a trumpet-ball into the wall. The ball
  breaks bricks. Bricks show a verse when broken.
- **Twist:** Seven "circuits" around the walls: a brick shake (a hopeful faith
  moment) happens on the seventh pass. A wall falls with soft dust, never with
  people shown.
- **Lives:** Three balls. A lost ball does not end the level if the player taps
  "Try again" at once (one free retry per level).
- **Gentle help:** The paddle is wider on the first three levels. A slow-motion
  option is in settings.
- **Art:** A ram's horn, a stone brick, a wall, a dust cloud, a soft golden glow.

### 3.4 Dove Pop (bubble shooter)

- **Board:** A hanging cluster of coloured doves, lamps and olive leaves.
- **Play:** Aim and launch a bubble. Three of a colour that touch pop. Floating
  bits fall when they lose support.
- **Twist:** A "lamp" tile lights the whole cluster around it. The player ends a
  level by clearing the board, not by running out of time.
- **Gentle help:** A "peek" shows the next bubble. Hanging clusters never drop to
  the floor with a penalty.
- **Art:** 5 colours of dove or lamp, 1 olive leaf wildcard, a cloud backdrop.

### 3.5 Shepherd's Path (endless lane runner, calm)

- **Play:** The shepherd walks a path with three lanes. Tap left or right to switch
  lanes. Collect lambs and coins of grace. Dodge thorn bushes and rain clouds.
- **Twist:** There is no death. A thorn stops the shepherd for a short moment and
  drops a lamb. The run keeps going. The goal is distance and lambs collected.
- **Pace:** Speeds up slowly, never sharply. The first minute is always easy.
- **Psalm card:** Every 100 metres a short line from Psalm 23 appears (KJV).
- **Art:** Shepherd, lamb, thorn bush, cloud, a painted hill backdrop with a
  sunrise. A soft parallax layer.

### 3.6 Daily Name (daily word guessing)

- **Play:** One Bible name each day, from a list bundled in the app. Six guesses.
  Each guess is coloured: right letter and place, right letter wrong place, not in
  the name.
- **Twist:** After each guess the app shows one line from the name's story
  (a short KJV line). The puzzle is a teaching card, not a trick.
- **No streaks.** The daily name is always there, any day. A completed day shows
  a small mark. Missing days shows nothing.
- **Offline:** The list ships in the app. The day is set on the phone's calendar.
- **Art:** A small cartoon scroll with a letter tile. No new character needed.

## 4. Improvements to existing games (code thread's call on order)

- **Ladder of Faith:** Add one "Ask the shepherd" lifeline per run (removes two
  wrong answers). Keep three lives. Never reduce XP.
- **Bible Quiz:** Show the verse after each answer, even when correct.
- **Word Search and Crossword:** Add a "reveal one letter" hint that costs nothing.
- **Road to Jerusalem:** Replace the blank squares with a short verse on each
  square, so every turn teaches something.

## 5. Art direction (from project memory)

- **Theme:** Frosted-glass purple (`#2A0F33`, `#681A7D`, `#C06BE0`) and orange
  (`#E8772E`). Bright, clear images. No whitewash over the art, no blur over words.
- **Style:** Flat, friendly cartoon with rounded shapes. Soft light. No harsh
  outlines. Readable at 40 px.
- **Characters:** Small, kind, respectful. No faces in close-up. No text inside
  the image. Existing: shepherd, dove, explorer. New candidates below.
- **Logo:** Keep the existing Perazim logo as it is.

### New Bible characters (proposed, originals)

- **Miriam's Lamp-bearer:** A girl with a small lamp and a tambourine (Exodus 15).
- **The Builder:** A kind old man with a plumb line (Nehemiah-era wall builder).
- **The Little Boy with Loaves:** A child with five small loaves and two fish
  (John 6). Friendly and bright, never a crowd shot.

## 6. Build plan

1. Design (this file) and first mockups: artifact with 6 game cards, 2 art
   directions (done in the next message).
2. Levi picks the art direction and the first wave (multiple choice).
3. Art: generate one tile set and one background per game with the creative tools.
   Save each as a PNG under `src/main/res/drawable-nodpi/` and record prompts in
   this file.
4. Code: the code thread (feature/games-depth) builds the logic. Design hands it
   a spec per game: rules, numbers, levels, copy. No overlap in Java files.
5. Each step is a small commit and push. Nothing is installed on the phone from
   this thread.

## 7. Open questions for Levi

See the decision cards in the thread. Answers will be written here.
