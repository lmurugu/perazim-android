# Bible game mechanics: research and Perazim specs (v1, 2026-11-21)

Owner: research thread (PR for branch `research/game-mechanics`).
Status: design only. No app code changes. Specs here are what the code threads
(engine PR 53, games PR 51, art PR 52) will implement.

Companion file: [LAYOUT_SYSTEM.md](LAYOUT_SYSTEM.md) (one theme, six layouts).
Design rules this file follows: [../GAME_DESIGN_CONCEPTS.md](../GAME_DESIGN_CONCEPTS.md), section 1.

---

## 0. Scope, sources and limits

- **What was studied.** Six popular Bible games on Google Play, from the store
  listing screenshots, store descriptions, tags, and the review summaries shown
  on the listing pages. Gameplay was not played end to end.
- **Confidence.** Each analysis below says where a fact comes from. "Listing"
  means it is visible on the store page. "Inferred" means it is a reasonable
  reading of the screenshots, not something seen in play. Treat inferred points
  as hypotheses to check in a short playtest with children before we copy any
  pattern.
- **Names.** The six titles are used here only as reference labels. No name,
  character, art, level design, copy or music from those games is used. Every
  Perazim spec below has its own original name.
- **Store facts recorded for the decision** (from the listings, as of 2026-11-21):

| Reference game (label used here) | Download size on listing | Ads / purchases on listing | Listing rating and review signal |
|---|---|---|---|
| Word-connect crossword ("Holyscapes") | 78 MB | Contains ads, in-app purchases | 4.7, 28K reviews; "Ad Experience" tag |
| Story jigsaw ("Bible Puzzle") | 575 MB | In-app purchases; "Teacher Approved" badge; age tags up to 9-12 | 50K+ downloads; some chapters marked "Coming soon" |
| Slide jigsaw solitaire ("Bible Jigsolitaire") | 126 MB | Contains ads, in-app purchases | 4.9, 19K reviews; a review reports the game restarting |
| Trivia ladder ("Bible Trivia") | 116 MB | Contains ads, in-app purchases | 4.7, 134K reviews; reviews complain of ad pop-ups after levels and ads that cannot be closed |
| Top-down RPG ("Didactic Jesus Game") | 67 MB | None shown (demo listing) | 10K+ downloads; no reviews yet |
| Map quiz ("Bible Quiz Quest") | 42 MB | None shown in the listing | 4.1, 253 reviews; one review praises "no adds" and a 15-second timer |

The size column is the main lesson for us. The two games that look most like
ours by content are 575 MB and 126 MB. Our per-game art cap is 5 MB.

---

## 1. Summary

| # | Reference game | Layout archetype | Core loop in one line | Keep | Leave out | Perazim spec (original name) | Recommendation |
|---|---|---|---|---|---|---|---|
| 1 | Word-connect crossword | Wheel plus grid | Swipe letters on a wheel to fill a crossword | Wheel input, level numbers, free-feeling hints | Coin hints, coin packs, ads | **Vine Words** | Build first. Merge Mini Crossword into it. |
| 2 | Story jigsaw | Scene grid | Drag pieces of an illustrated Bible scene into place | Story captions, chapters, calm pace | Locked "coming soon" chapters, 575 MB art, Ranked score | **Story Mosaic** | Build first. Shares its chapter bank with Story Timeline. |
| 3 | Slide jigsaw solitaire | Tile grid | Slide tiles to rebuild a painting | Painting reveal, verse banner on completion | Ads, restart bugs, 126 MB art | **Lamp Slide** | Build first. New mechanic for the Games tab. |
| 4 | Trivia ladder | Ladder of levels | Four-option questions up a level ladder with a prayer beat | Daily set, prayer pause between levels | Ad pop-ups, unclosable ads | **Ladder of Faith, upgraded** (no new game) | Upgrade the existing Ladder of Faith. |
| 5 | Top-down RPG | Walking map | Walk a town and meet characters who ask questions | Meeting characters, health as hearts | Real-time combat, hearts that refill by timer or purchase | **Galilee Walk** | Wave 2. Shares one map engine with Journey Path. |
| 6 | Map quiz | Winding path | Answer a scripture question at each stop along a route | The route map, a scripture line on every question | Forced timer, ads, language-pack paywalls | **Journey Path** | Replaces Road to Jerusalem's board. Build before Galilee Walk. |

---

## 2. Game by game

Each section has: the reference game, what it does, the Perazim version, and what
to leave out. The Perazim version is the spec. Its rules, level generation,
engine mapping and asset budget are complete enough to implement.

### 2.1 Word-connect crossword (reference: Holyscapes)

**Core loop (listing).** A ring of letters sits at the bottom. The player drags
across letters to spell a word. A correct word fills a crossword grid above.
Each level has a fixed set of words that cross each other. Solving all words
completes the level.

**Progression (listing).** Levels are numbered (for example "Level 31" in the
store capture). The grid grows and the letter count rises over time. Backgrounds
change between sets of levels (aurora, sunset, sky, galaxy). The screens show a
coin balance in the top bar.

**Rewards.** Coins are earned by solving words. Coins buy hints. Some listings
also sell coin packs.

**Hint economy (listing).** Three hint buttons sit beside the wheel. Each shows a
coin price (80 or 240 coins in the captures). Hints are the only way to spend
coins, so coins become a currency players must watch. The listing review tag
"Ad Experience" shows that ads are part of the design too.

**Difficulty curve (inferred).** Early levels are three to four short words.
Later levels add longer words and more crossings. The wheel can carry more
letters than any one level needs, which adds choice.

**Session length (inferred).** One level takes one to four minutes.

**What makes it engaging.** Clear, instant feedback when a word lands. Satisfying
swipe. A visible grid that fills up. Short levels that end on a success.

**Leave out.** Coin hints, coin packs, ads between levels, any paid shuffle.

#### Perazim version: Vine Words

- **Idea.** Same wheel-and-grid loop. Words come from the verse the level is built
  on. Players are taught the verse before the words are solved, not after.
- **Rules.**
  1. A level is built from one passage of one to three verses (for example John
     15:1-5, "vine", "branch", "fruit", "abide").
  2. The grid holds N target words (N = 3 at level 1, up to 10 at level 40). Each
     target word crosses at least one other target word.
  3. The wheel shows exactly the letters of the target words, plus up to two
     extra letters from a bonus word. The wheel is never shown in alphabetical
     order. The seed decides the order.
  4. The player swipes letters on the wheel. Releasing a valid word places it on
     the grid if it matches a slot. A word that matches no slot is ignored, not
     penalised.
  5. Bonus words are optional. They add to the score but never block the level.
  6. No timer. No lives.
  7. Solving the last target word shows the passage. The player taps "Read" to
     see the verse text from the bundled KJV.
- **Gentle help.**
  - **Reveal a letter.** Free, once per word. Each reveal lowers the star cap for
    that level from 3 to 2. Coins do not exist.
  - **Shuffle.** Free and unlimited. It changes only the wheel order.
  - **Retry.** The same level can be retried at once.
- **Level generation from KJV.**
  1. Build-time script reads the bundled KJV text (public domain) and makes a word
     list of A-Z words, 3 to 8 letters, that occur in a curated set of about 40
     passages. Proper names are allowed only if they are in the child-safe list.
  2. A level picks a passage with seed S. Target words are drawn from words in
     that passage, filtered to the length band for the level.
  3. A crossword is built by backtracking. The builder tries crossings, rejects a
     grid where two slots touch without crossing, and stops after 200 attempts. If
     it fails, the seed is advanced and the level is rebuilt. The same seed always
     gives the same level.
  4. The bonus word and the wheel are built from the target letters.
  5. Test: every level in the level set is generated and checked on CI (no level may
     fail to build; each target word appears in the passage it came from).
- **Engine mapping** (`GameDefinition` / `GameSession`).
  - `id` = `vine_words`. `levelCount` = 40. `levelName(n)` = "Level n".
  - `createSession(level, seed)` builds the grid and wheel.
  - `size()` = number of target words. `position()` = words solved so far.
  - `answer(word)` returns `CORRECT` when the word fills a slot, `WRONG` when it
    matches no slot and is not a bonus word, and `IGNORED` for a word already
    placed. A bonus word returns `CORRECT` and adds points without counting toward
    `finished()`.
  - `hint()` returns "The first letter is V" or the next letter of a chosen slot.
  - `prompt()` returns the passage reference, for the header.
  - `lives()` = `NO_LIVES`.
  - Stars use `StarRating.stars(score, size, lives)`, with hints lowering the cap.
- **Assets.** Wheel and grid are drawn in code (Canvas). Background set: 8 scenic
  images, WebP, 1080 px wide, about 80 KB each. Total under 1 MB.
- **Motion.** A swipe leaves a soft trail. A solved word slides into its grid
  slots over 200 ms. No particle effects.
- **Relation to existing games.** Replaces **Mini Crossword** as the crossword
  card. Mini Crossword's clue mode becomes a variant of Vine Words for level
  1-10 players who want written clues. Word Search stays separate; it is a
  search grid, not a wheel.

---

### 2.2 Story jigsaw (reference: Bible Puzzle)

**Core loop (listing).** Pick a story chapter (Noah's ark, Moses, David, Daniel
are shown). Drag pieces of a painted scene into a grid. Each completed scene shows
a caption from the story.

**Progression (listing).** Chapters are unlocked in order. Later chapters show a
"Coming soon" label. Difficulty is set by a "Normal" selector with arrows. The
store lists a "Ranked" mode.

**Rewards.** Trophies and achievements (trophy icon in the top bar). The age
tags run from "ages up to 5" to "9-12", and the badge "Teacher Approved" is
shown.

**Difficulty curve (listing).** Grids go from small (3 by 3 looks) to larger
(5 by 5 and up) pieces. The "Ranked" mode adds a score.

**Session length (inferred).** Five to ten minutes per scene.

**What makes it engaging.** Story captions connect each piece to a Bible event. The
scene reveals bit by bit. Warm art and gentle sound.

**Leave out.** Locked "coming soon" chapters that look like a paywall. Ranked
score that has to be uploaded (we have no score upload). Art packs of 575 MB.

#### Perazim version: Story Mosaic

- **Idea.** Put one illustrated Bible scene together, piece by piece. When the
  scene is complete, one short KJV line for that scene is shown with its reference.
  Every chapter is open from the start. Nothing is locked behind coins or waiting.
- **Rules.**
  1. A level is one scene in a grid. Levels 1-5 are 3 by 3, 6-15 are 4 by 4,
     16-30 are 5 by 5, 31-40 are 6 by 6.
  2. The pieces are cut from the scene at run time (one image per scene, not one
     file per piece).
  3. A piece can be dragged to a slot. It snaps when it is within the slot's area.
     A wrong slot is shown in a soft shake and the piece returns.
  4. Two modes: **Guided** (a faint outline of the scene is shown, and one hint
     shows the piece's slot for 2 s) and **Quiet** (no outline, no hint; the piece
     tray is shown in a random order). The default is Guided for levels 1-10 and
     Quiet afterwards.
  5. No timer. No lives. No ranking.
  6. When the scene is complete, a short caption appears with the KJV line and
     reference. The player taps "Continue".
- **Level generation from KJV.**
  1. Each scene has a fixed passage in the chapter bank (shared with Story Timeline).
     Example: the ark scene uses Genesis 6:22 ("Thus did Noah; according to all that
     God commanded him, so did he.").
  2. The chapter bank lists scenes in story order. Level n = scene `n mod scenes`,
     and the grid size comes from the level band.
  3. The seed chooses the shuffle of the tray and the piece cut (a small random
     rotation is never used; pieces are only moved, never turned).
  4. Test: every scene's caption reference is checked against the bundled KJV text
     at build time.
- **Engine mapping.**
  - `id` = `story_mosaic`. `levelCount` = 40.
  - `size()` = number of pieces. `position()` = pieces placed.
  - `answer("pieceId:slot")` returns `CORRECT` when the piece lands in its slot,
    `WRONG` when it does not, and `IGNORED` for a piece already placed.
  - `hint()` returns the slot of one unplaced piece in Guided mode, or `null` in
    Quiet mode.
  - `prompt()` returns the scene caption, shown before play and after the last
    piece.
  - `lives()` = `NO_LIVES`. Stars: 3 for no wrong drops, 2 for at most one piece
    in three wrong, 1 for the rest, via `StarRating` with `score` = correct drops.
- **Assets.** 12 scenes at 1024 px, WebP, about 90 KB each, about 1.1 MB. Shared
  character sprites come from the core cast (see LAYOUT_SYSTEM.md). Total under 2 MB.
- **Motion.** A drop snaps with a 120 ms ease. A complete scene does a gentle
  warm-light pulse over 600 ms. No confetti.
- **Relation to existing games.** Keeps **Story Timeline** separate: timeline is
  about order, mosaic is about a picture. They share the chapter bank and the
  scene captions, so a child who knows the timeline sees the same story in
  the mosaic.

---

### 2.3 Slide jigsaw solitaire (reference: Bible Jigsolitaire)

**Core loop (listing).** A painting is cut into tiles, one tile is missing, and
the player slides tiles into the gap until the picture is right.

**Progression (listing).** Levels are numbered (LEVEL 11 and LEVEL 64 appear
in the captures). An "Easy to play" mode is advertised. The store has a
"Bible journey" theme.

**Rewards.** Level completion. The capture shows a verse banner after a level.

**Hint economy (inferred).** The listing says "contains ads" and "in-app
purchases". Hints are not shown in the captures.

**Difficulty curve (inferred).** Small grids at the start, larger grids later.
Reviews report a restart problem, which is a reminder to test saved state.

**Session length (inferred).** Two to five minutes per level.

**What makes it engaging.** Slide puzzles are calm and easy to start. The
picture gives a reason to finish. A short verse at the end gives a moment of
reflection.

**Leave out.** Ads, paid hints, restarts after a crash, 126 MB of art.

#### Perazim version: Lamp Slide

- **Idea.** A classic sliding puzzle with one painting per set. When the picture is
  right, a verse banner reads the KJV line with its reference, and a short prayer
  line is shown. The painting is the reward.
- **Rules.**
  1. Board is 3 by 3 for levels 1-10, 4 by 4 for levels 11-25, 5 by 5 for levels
     26-40.
  2. One tile is the gap. The player taps a tile next to the gap, or swipes
     toward it, to slide it into the gap. Only neighbours of the gap can move.
  3. Undo is unlimited.
  4. Hint: "Show the picture" (2 s peek, free) and "One move" (free, shows the next
     tile to move). Each use lowers the star cap by one.
  5. Moves are counted. The star rule uses par (see below).
  6. No timer by default. An optional calm timer can be switched on in settings
     (default off).
  7. Completion shows the verse banner with reference and one line from the prayer
     card, then "Continue".
- **Level generation.**
  1. Each level draws its painting from a bundled set of 12 paintings (4 per
     band), chosen by the seed.
  2. The start position is built by making random legal slides from the solved
     state (400 moves for 3 by 3, 800 for 4 by 4, 1200 for 5 by 5). This
     guarantees the puzzle is solvable and avoids the parity problem of random
     shuffles.
  3. Par = the moves an A* solver takes on the start position, computed at build
     time and stored with the level (for 3 by 3 and 4 by 4). For 5 by 5, par is
     the number of slides in the generation walk.
  4. Verse for each painting comes from the chapter bank. Verses are checked against
     the bundled KJV text at build time.
- **Star rule.** Stars = 3 when moves are at most par, 2 when at most 1.5 times
  par, 1 otherwise. Hints lower the cap as above. This is a per-game rule, not
  `StarRating`, because `StarRating` counts correct answers.
- **Engine mapping.**
  - `id` = `lamp_slide`. `levelCount` = 40.
  - `size()` = par for the level. `position()` = moves made.
  - `answer("up" | "down" | "left" | "right")` returns `CORRECT` for a legal slide
    and `IGNORED` for an illegal one. **Gap:** the engine's `Outcome` has no "moved"
    value. The engine thread should add `Outcome.MOVED` or a separate
    `ActionSession` interface. This is the one engine change this spec needs.
  - `finished()` = the board matches the solved state.
  - `lives()` = `NO_LIVES`. `score()` = moves within par.
- **Assets.** 12 paintings at 1024 px, WebP, about 90 KB each, about 1.1 MB.
  Tiles are cut at run time, one image per painting, not one file per tile.
- **Motion.** A tile slides over 140 ms with a soft ease. The painting fades in
  over 300 ms when complete.
- **Relation to existing games.** New mechanic. It shares the verse banner and the
  results screen with other games. It does not replace anything.

---

### 2.4 Trivia ladder (reference: Bible Trivia)

**Core loop (listing).** Four-option questions. A level has a set of questions.
Passing a level moves the player up the ladder.

**Progression (listing).** Levels are numbered (LEVEL 1 and LEVEL 11 appear in
the captures). The listing claims over 6,500 questions from the Old and New
Testament, and a "Bible study tool" section.

**Rewards.** Level progress on a bar. A daily quiz is shown in the capture.

**Prayer moment (listing).** Between levels, a screen says "Pray to God sincerely"
with a prayer line, a progress bar, and an "Amen" button. This is one of the most
distinctive moments and is worth keeping.

**Hint economy (inferred).** Listing says "contains ads, in-app purchases". Reviews
complain about ad pop-ups after levels and ads that cannot be closed.

**Difficulty curve (listing).** The review summary mentions progressive difficulty.

**Session length (inferred).** Three to six minutes per level.

**What makes it engaging.** Clear four-option format. Quick feedback. The daily
quiz gives a reason to return. The prayer moment gives the session a close.

**Leave out.** Ad pop-ups, unclosable ads, paid hints.

#### Perazim version: Ladder of Faith, upgraded (no new game)

- **Idea.** Keep the existing Ladder of Faith. Add the prayer moment and a daily
  set. Remove any ad path.
- **Rules (additions to the existing game).**
  1. **Pray beat.** After every fifth question, a card shows one KJV prayer
     verse or short line with its reference and an "Amen" button. No timer. The
     player taps when ready.
  2. **Daily Climb.** One fixed 10-question set per date, built from the bundled
     bank with a date seed. Missing a day costs nothing. Results are local.
  3. **Lifeline (from the concepts doc).** "Ask the shepherd" removes two wrong
     answers, once per run.
  4. Keep the existing lives and XP. No change to scoring.
- **Level generation.** Existing `LadderBank` is reused. Daily Climb uses the date
  as the seed. Each daily set draws two questions from each of five books, so one
  day never repeats a book.
- **Engine mapping.** Already present. Pray beat is a UI step between `answer()`
  calls. Daily Climb uses the same `GameDefinition` with a fixed level id.
- **Assets.** None new. Prayer cards are text.
- **Relation to existing games.** This is the Ladder of Faith card. Bible Quiz stays
  as the quick quiz. Do not add a second trivia ladder; the upgrade covers the
  reference game's best parts.

---

### 2.5 Top-down RPG (reference: Didactic Jesus Game)

**Core loop (listing).** The player walks a town and meets biblical characters
(the capture names Mary, the apostles and the wise men). Stops with characters
lead to short lessons.

**Progression (listing).** Scenes change between towns. Story is delivered in the
captions ("Walk in Jesus' footsteps", "Meet biblical characters").

**Health (listing).** A row of hearts sits in the top bar. The hearts are health,
not a timer.

**Difficulty curve (inferred).** Only a demo is listed, so the curve is unknown.

**Session length (inferred).** Ten to twenty minutes for a full town.

**What makes it engaging.** Walking, exploring and meeting people. A sense of
place.

**Leave out.** Real-time combat, hearts that refill by timer or purchase, a
demo that asks for purchase to continue.

#### Perazim version: Galilee Walk (wave 2)

- **Idea.** A calm, step-by-step walk across a small map. Players meet a character
  at each stop. The character asks one question with a KJV line for context. Hearts
  are a patience count, not a timer.
- **Rules.**
  1. A map is a 12 by 12 tile grid. The player taps a destination; the character
     walks there along a shortest path at a fixed pace (no real-time control, no
     combat, no falling).
  2. Each map has 3-5 stops. A stop is a character encounter with one question
     (three options) and one KJV line shown on answer.
  3. Hearts: 3 per map. A wrong answer at an encounter costs a heart. A map ends
     at 0 hearts with a "Walk again" card. Hearts refill when the next map starts;
     there is no timer and nothing to buy.
  4. Levels = maps. Ten maps in version 1 (Nazareth, the lake shore, the road to
     Jerusalem, and so on).
  5. No timer. No ranking.
- **Level generation.** Each map is authored (not random). Stop questions come
  from the question bank with the map's passage. Map layout is fixed, so testing
  is simple.
- **Engine mapping.**
  - `id` = `galilee_walk`. `levelCount` = 10.
  - `size()` = number of encounters. `position()` = current encounter.
  - `prompt()` = the encounter question. `hint()` = the chapter reference.
  - `answer(option)` returns `CORRECT`/`WRONG`. `lives()` = hearts left.
  - Stars: `StarRating.stars(score, size, lives)` (the lives rule already caps the
    stars when hearts reach zero).
- **Assets.** Tile atlas 256 by 256 WebP (about 60 KB). Six character sprites at
  48 px, four frames each (about 300 KB total). Background per map, 1080 px
  WebP (about 100 KB each, 1 MB total). Under 3 MB.
- **Performance.** Canvas or a tile view. Movement is a 60 fps tween on a fixed
  path. No physics. Tested on a 2 GB device.
- **Relation to existing games.** Shares the map engine with **Journey Path** (see
  2.6). Build Journey Path first. Galilee Walk adds walking and characters to the
  same map. When built, it replaces the RPG-style walk in Road to Jerusalem.

---

### 2.6 Map quiz (reference: Bible Quiz Quest)

**Core loop (listing).** The player follows a winding route with stops. At each
stop, a scripture-backed question is asked. Stop 1 is the start of a journey
(for example crossing the Red Sea).

**Progression (listing).** Stops along one route. Story captions ("Join the
Adventure", "Creating the Red Sea").

**Language (listing).** A language picker shows many flags. Language choice is a
setting, not a purchase.

**Scripture (listing).** "Every question backed by scripture". This is the
selling point in the captures.

**Timer (listing and review).** A review says "only 15 sec", so the questions are
timed. Timing is part of the challenge in this game.

**Hint economy.** None shown.

**Difficulty curve (inferred).** Questions get harder along the route.

**Session length (inferred).** Five to ten minutes per route.

**What makes it engaging.** A route to follow. Every stop is a clear task.
Scripture is shown with every answer.

**Leave out.** Forced timers for every question (make it an option, default off),
language packs behind a paywall, ads.

#### Perazim version: Journey Path

- **Idea.** A winding route in four journeys, each with ten stops. Each stop asks one
  question with a verse reference. The route is the level map. This becomes the
  shared map layout for Road to Jerusalem and Galilee Walk.
- **Rules.**
  1. Four journeys (Exodus, Ruth, Gospels, Acts). Ten stops per journey.
  2. A stop has three questions. Three options each. The player must get two
     right to pass the stop (the `PASS_STARS` rule from `LevelProgression`).
  3. Each answer shows the KJV line with its reference.
  4. Timer: off by default. Settings can turn on a 20-second timer per question.
  5. No ads, no paid languages. The interface language is a setting (English and
     Kiswahili in version 1). Scripture text stays KJV English in every language,
     so quotes always match the references.
- **Level generation.** Each journey is authored in a file with stop and question
  ids. The route is a fixed list of points. Questions come from the question bank.
  A seed chooses the option order only.
- **Engine mapping.**
  - `id` = `journey_path`. `levelCount` = 40 (ten stops times four journeys).
  - `createSession(stop, seed)` returns a three-question session.
  - `prompt()` = the question, `hint()` = the verse reference.
  - `answer(option)` returns `CORRECT`/`WRONG`. `lives()` = `NO_LIVES`.
  - Stars use `StarRating`.
- **Assets.** Route background per journey, 1080 px WebP (about 120 KB each, under
  0.5 MB). Route pins drawn in code.
- **Relation to existing games.** **Replaces Road to Jerusalem's board.** Road to
  Jerusalem's square-by-square board is the same idea with fewer stops. The
  question content in Road can move into Journey Path's stops. Bible Quiz keeps its
  quick mode and can offer Journey Path as an extra tab.

---

## 3. How the Perazim specs relate to the games we already have or plan

### 3.1 Existing (12 games, from the code and the concepts doc)

| Existing game | What happens to it | Why |
|---|---|---|
| Bible Quiz | Keep. Add Journey Path as an optional tab. | The quick quiz is still needed. |
| Who am I? | Keep. | Different format (riddle). |
| Verse Builder | Keep. Lamp Slide can reuse its verse banner. | Different mechanic. |
| Ladder of Faith | **Upgrade** (2.4): pray beat, Daily Climb, lifeline. | Best match for the reference game's strengths. |
| Word Search | Keep. | A search grid, not a wheel. |
| Memory Match | Keep. | Different mechanic. |
| Story Timeline | Keep. Shares the chapter bank with Story Mosaic. | Order is different from picture. |
| Mini Crossword | **Merge** into Vine Words (clue mode). The separate card goes away. | Same grid, better input. |
| Road to Jerusalem | **Replace** its board with Journey Path. | Same route idea, more stops and questions. |
| Old or New? | Keep. | Quick fact game. |
| Who Said It? | Keep (code thread finishes it, PR 51). | Quote game. |
| Fill the Gap | Keep (PR 51). | Verse game. |

### 3.2 Planned (six games, `docs/games/GAME_DESIGN_CONCEPTS.md`)

| Planned game | Relation to the new specs | Recommendation |
|---|---|---|
| Manna Match (match-3) | No overlap. Match-3 is a new genre for us. | Keep in the plan. |
| Fruit of the Spirit (colour sort) | No overlap. | Keep in the plan. |
| Seven Trumpets (brick breaker) | No overlap. | Keep in the plan. |
| Dove Pop (bubble shooter) | No overlap. | Keep in the plan. |
| Shepherd's Path (runner) | No overlap. | Keep in the plan. |
| Daily Name (daily word guess) | Vine Words also uses words from passages. Do not add daily words to Vine Words. | Keep as its own game. |

Recommended order for code threads: Vine Words, Lamp Slide, Story Mosaic first
(three archetypes, low art). Then Journey Path. Then Galilee Walk. Ladder upgrade
can ship any time because it adds no new game.

---

## 4. Engine changes this research needs

Only one change is needed, and it is small.

1. **Non-answer actions.** Lamp Slide and Story Mosaic do not produce a correct or
   wrong answer on each move. Add `Outcome.MOVED` (or a second interface, for
   example `ActionSession`, with `act(String)`). Recommend `Outcome.MOVED`, since
   `GameSession.answer(String)` already carries commands as strings.
2. **Stars per game.** Lamp Slide uses a par-based star rule. Add an optional
   `StarRule` parameter to `GameDefinition`, with `StarRating` as the default.
3. **Hints that cost stars.** Vine Words and Lamp Slide lower the star cap on
   hint use. Add `hintsUsed()` to `GameSession`, or let `StarRating` accept a
   `hintsUsed` count. Default 0 keeps today's behaviour.

No change is needed for the other specs.

---

## 5. Shared level pipeline (all Bible content)

1. **Bundle.** KJV text from the public-domain source, stored in the app's assets.
   NASB only from a user-supplied file (not in git). No ESV, per project decision.
2. **Build-time script** (runs in CI, not on the phone):
   - builds the word list for Vine Words (A-Z, 3-8 letters, from curated passages);
   - checks every verse reference used in any game against the KJV text;
   - builds the Lamp Slide start positions and par values;
   - writes one level file per game, with a version number.
3. **Runtime.** Only reads the level files. No network. Random only from the seed.
4. **Tests.** Every level builds. Every verse reference resolves. No target word
   appears twice in one grid. Par is reproducible from the seed.

---

## 6. Performance and size budget (all six specs)

- **Device target.** 2 GB RAM, Android 8.0 (API 26) or later, no network.
- **Engine.** Native Views or Canvas. No game engine, no physics engine.
- **Frame rate.** 60 fps target. Animations are 120-600 ms tweens.
- **Art per game.** Under 5 MB. Target under 3 MB. Sprites 48-256 px. Backgrounds
  1080 px WebP.
- **Sound.** Short, soft clips under 30 KB each, a limited set shared by all games.
- **Memory.** Load one game's art at a time. Release art when leaving a game.

---

## 7. Questions for Levi (none block the three first builds)

1. Do you want Galilee Walk (wave 2) in the first year, or should it wait until
   after Journey Path?
2. Should the language picker in Journey Path ship with English and Kiswahili only
   for version 1?
3. Is a 20-second optional timer acceptable in Journey Path, off by default?

---

## 8. Sources

- Play Store listing screenshots and store text from the project thread
  (2026-11-21), for the six reference games.
- Existing repo: `docs/games/GAME_DESIGN_CONCEPTS.md`, `domain/games/engine` on
  branch `feature/games-engine` (`GameDefinition`, `GameSession`, `LevelProgression`,
  `StarRating`).
- KJV (public domain) for every verse and quote in the specs.
