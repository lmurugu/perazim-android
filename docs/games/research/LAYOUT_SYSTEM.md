# Games layout system: one theme, six layouts (v1, 2026-11-21)

Companion to [MECHANICS.md](MECHANICS.md). Design only. Every game in the Games tab
should look like it belongs to one family and still play differently.

The rule in one line: **the frame is shared, the board is not.**

---

## 1. What is shared across all games

### 1.1 Palette (frosted-glass purple and orange)

Source of truth is the app's colour resources. The concept doc uses slightly
different tints; this table uses the app values and marks the concept values as
alternates. Code threads should use the app values and update the concept doc.

| Token | Value | Use |
|---|---|---|
| `games.bg.deep` | `#2D0938` | Deepest background, top of the screen |
| `games.bg.mid` | `#681A7D` | Primary purple, buttons, bars |
| `games.bg.light` | `#C06BE0` | Glow, highlights, selected states (concept value) |
| `games.accent` | `#E17D2F` | Orange: primary action, stars, progress |
| `games.accent.glow` | `#E8772E` | Orange glow edge (concept value) |
| `games.surface` | `#FFFFFF` at 14% over the background | Frosted glass panel fill |
| `games.surface.stroke` | `#FFFFFF` at 28% | 1 dp panel edge |
| `games.text.primary` | `#FFFFFF` | Titles, numbers |
| `games.text.secondary` | `#F5ECF7` | Body lines (app `purple_tint`) |
| `games.success` | `#7BD389` | Correct, placed, solved (used sparingly) |
| `games.soft.warn` | `#F2C94C` | Hint and star highlight only |

Rules:
- Frosted panels are a 14% white fill with a blur behind them where the device
  supports it. On Android 8 and 9 without blur, use a flat 14% fill; the look is
  still correct.
- No pure black. No pure red. A wrong answer is a soft shake and a warm amber
  flash, never red.
- Text on any image must pass WCAG AA (4.5:1). Put words on a glass panel, never
  straight on a busy picture.

### 1.2 Top bar (identical in every game)

```
+--------------------------------------------------------------+
| [<]   Level 12        [★ 7]              [ II ]            |
+--------------------------------------------------------------+
```

- Left: back button (48 dp tap target).
- Centre: game title small, level name below it.
- Right: stars earned in this run, and a pause button.
- Height 56 dp. Sits on the frosted glass strip.
- Pause opens a card: Resume, Retry this level, Home. No timer keeps running
  behind it, and no ads.

### 1.3 Results screen (identical structure)

```
+--------------------------------------------+
|           [ scene or character art ]       |
|                                            |
|             ★  ★  ☆   (stars)              |
|          Level 12 complete                 |
|   "KJV line"  — Reference                  |
|                                            |
|   [ Read verse ]     [ Play again ]        |
|           [ Next level  →  ]               |
+--------------------------------------------+
```

- Stars use `games.accent` for earned and a 28% white outline for unearned.
- The verse line is always KJV with reference.
- Buttons: primary (orange gradient with glow) is "Next level"; secondary
  (purple glass) for the rest.
- Nothing about rank, leaderboard or upload. Nothing that says "come back
  tomorrow or lose it".

### 1.4 Typography

- Titles: a bundled, open-licence serif (SIL OFL) at 22 sp, medium weight. Use a
  single font file, under 100 KB. Chosen for a book-like warmth. Name the chosen
  font in the code thread's PR, not here.
- Body and buttons: the system sans (Roboto on Android) at 16 sp, 14 sp for
  secondary lines.
- Numbers (levels, stars, moves): the system sans, tabular figures.
- Verse text: the serif at 18 sp, 1.4 line height, always in quotes with a
  reference beneath in 14 sp.
- Minimum touch target 48 dp. Minimum text size 14 sp.

### 1.5 Character cast (shared, small)

Six characters appear across the games. They are small, kind and respectful.
Faces are seen from the side or from a distance, never in close-up. No text in
the art.

| Character | Role in games | Where they appear |
|---|---|---|
| Young shepherd | Guide, hints | Hint bubble, Galilee Walk start |
| Prophet (with a scroll) | Verse keeper | Results "Read verse" card, Journey Path stops |
| Fisher disciple (with a net) | Companion, encourages | Lamp Slide completion, Story Mosaic |
| Woman with a water jar | Question giver | Galilee Walk encounters |
| Child with loaves | Warm reward | Story Mosaic captions, Ladder pray beat |
| Olive the dove | Mascot, progress | Top-bar star counter icon, results screen corner |

The dove is the only character allowed as a small badge in the top bar. Everyone
else appears in art or on cards.

### 1.6 Motion (shared rules)

- Standard ease: 180 ms out for panels, 120 ms for taps.
- Success: a warm light pulse, 300-600 ms. No confetti, no screen shake.
- Wrong: a 120 ms soft shake and an amber flash. Nothing punitive.
- Reduced motion: honour the system setting; replace moves with fades.

---

## 2. What is different per game

Each game is defined by four things: a layout archetype, a background family, an
interaction style, and a motion signature.

| Game | Layout archetype | Background family | Interaction | Motion signature |
|---|---|---|---|---|
| Vine Words | Wheel plus grid | Night sky, aurora, dawn over hills | Swipe on a circular wheel | Trail glow; word slides into the grid |
| Story Mosaic | Scene grid | Warm painted scenes, parchment margins | Drag and drop to slots | Snap and a warm-light pulse when a scene completes |
| Lamp Slide | Tile grid | One painting at a time, framed like a gallery | Slide tiles into the gap | 140 ms slide; painting fades into full colour |
| Ladder of Faith (upgraded) | Ladder of levels | Dark purple with gold rungs | Tap one of four answers | Rung lifts; pray beat is still (no motion) |
| Galilee Walk | Walking map (tile grid) | Painted village and lake, top-down | Tap a destination; the character walks | 60 fps tween along a path; encounter bubble |
| Journey Path | Winding route | Parchment map, sea and desert bands | Tap a stop | Route line draws in; stop pin bounces once |

Each background family stays inside the palette. A background may use its own
hues, but it always has a deep purple vignette at the edges so the frame holds.

---

## 3. Wireframes

Each wireframe shows the game screen only. The top bar and results screen follow
section 1.

### 3.1 Vine Words (wheel plus grid)

```
+-----------------------------------------+
| [<]  Level 12  ★ 2 of 3       [ II ]    |   top bar
|                                         |
|   crossing grid (top half)              |
|                                         |
|     V . . I N E                         |
|     . . . . . R                         |
|     B R A N C H                         |
|         . . . . T                      |
|                                         |
|   [ Reveal ]          [ Shuffle ]       |   gentle help
|                                         |
|          .--- wheel ----.              |
|         /  (E)      (R)   \             |   letters in a ring
|        |  (A)   ·    (N)  |             |   swipe trail
|         \  (B)      (C)   /              |
|          '-- (H) ---(T)--'              |
+-----------------------------------------+
```

- Grid is top, wheel is bottom (thumb reach).
- Wheel letters are circles 44 dp, arranged in a ring; the order is shuffled by
  seed.

### 3.2 Story Mosaic (scene grid)

```
+-----------------------------------------+
| [<]  Ark, Genesis 6   ★ 1       [ II ]   |
|                                         |
|  +---------+---------+---------+        |
|  |  scene  |  scene  |  scene  |        |   4 by 4 grid,
|  |  faint  |  faint  |  faint  |        |   faint outline
|  +---------+---------+---------+        |   in Guided mode
|  |         |         |         |        |
|  +---------+---------+---------+        |
|                                         |
|   Tray:  [ p3 ] [ p7 ] [ p12 ] [ p1 ]    |   pieces to drag
|                                         |
|   [ Show one spot ]                     |   hint, 2 s
+-----------------------------------------+
```

- Pieces cut from one scene; the tray shows them in a row, scrolls sideways.
- Quiet mode: no faint outline, no hint button.

### 3.3 Lamp Slide (tile grid)

```
+-----------------------------------------+
| [<]  Level 14  moves 23 / par 28 [ II ]  |
|                                         |
|   +-----+-----+-----+-----+             |
|   |  7  |  3  |  12 |  1  |             |   4 by 4
|   +-----+-----+-----+-----+             |   gap is the blank
|   |  4  |     |  9  |  6  |             |   cell
|   +-----+-----+-----+-----+             |
|   | ... | ... | ... | ... |             |
|   +-----+-----+-----+-----+             |
|                                         |
|   [ Peek ]   [ Undo ]   [ One move ]     |
+-----------------------------------------+
```

- Board is square, centred, 80% of the width.
- Gap has a soft inner shadow; tiles have a 2 dp frosted edge.

### 3.4 Ladder of Faith, upgraded (ladder of levels)

```
+-----------------------------------------+
| [<]  Ladder  rung 6        ★ 3   [ II ]  |
|                                         |
|        ___                              |
|       |   |  <- rung 7 (locked)         |
|       |___|                             |
|       |   |  <- rung 6 (current)        |
|       |___|                             |
|                                         |
|   "Who led Israel out of Egypt?"        |
|  [ Moses ]     [ Joseph ]               |
|  [ Abraham ]   [ David  ]               |
|                                         |
|   [ Ask the shepherd ]    lifeline x1   |
+-----------------------------------------+
```

- Pray beat after every fifth question replaces this screen with a still card:
  one KJV line, reference, "Amen" button.

### 3.5 Galilee Walk (walking map, wave 2)

```
+-----------------------------------------+
| [<]  Map 3: Lake shore   ♥ ♥ ♡   [ II ]  |
|                                         |
|   . . . . ~ ~ ~ ~ . . . .               |
|   . [W] . . ~ ~ ~ ~ . . .                |   12 by 12 tiles,
|   . . . . . ~ ~ ~ ~ . . .                |   top-down
|   . . ● . . . . . . . . .                |   ● player
|   . . . . . . [F] . . . .                |   [F] character
|                                         |
|   Bubble: "Who was called from his nets?"|
|   [ Peter ]  [ Andrew ]  [ James ]       |
+-----------------------------------------+
```

- Hearts are a patience count: filled hearts remaining, outlined hearts lost.

### 3.6 Journey Path (winding route)

```
+-----------------------------------------+
| [<]  Exodus 1 of 4   stop 4/10   [ II ]  |
|                                         |
|         (5)                             |
|        /   \                            |
|     (4)     (6)                         |
|       \     /                           |
|        (3)                              |
|        /                                |
|     (2)    (7)  <- stops, drawn route    |
|       \  /                              |
|       (1)  start  (Red Sea shore)       |
|                                         |
|   Stop 4: "Where did the people cross?"  |
|   [ The Red Sea ]   [ The Nile ]         |
+-----------------------------------------+
```

- Route is a single line; completed stops are filled, the current stop has a
  soft pulse, locked stops are dim.

---

## 4. Checks

- **Contrast.** Every text colour above passes 4.5:1 on its panel. Confirm with
  the accessibility review skill before the code threads ship.
- **Colour blind.** Stars and states use shape as well as colour (filled, outlined,
  dim).
- **Size.** Every tap target is 48 dp or larger, including wheel letters and tiles.
- **Phone width.** Layouts are tested at 360 dp width and 411 dp width.
- **Art budget.** Shared assets (frame, cast, sound) stay under 3 MB in total.
  Per-game art follows MECHANICS.md section 6.

---

## 5. Mockup

An interactive artifact shows six game screens side by side in this system. It
is linked from the draft PR once published. The artifact is a design reference,
not shipped code.

Artifact link: https://claude.ai/artifact/368417eRufp3wQHd8TjjDt (private to the project owner until shared from the page's Share menu)

---

## 6. Open points for the design thread (PR 52)

1. Confirm the serif font choice and licence before any code thread imports it.
2. Confirm the character names and roles in 1.5 match the art thread's cast.
3. Decide whether the palette lives in one shared colour file for games (code
   thread's call); this doc uses the app tokens.
