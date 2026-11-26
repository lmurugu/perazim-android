# Games engine: how to use it

This is the short guide for wiring a game into the shared engine. It covers what each piece
does and the one call a game makes when a run ends.

## Pieces

| Piece | Where | What it does |
|---|---|---|
| `GameDefinition` | `domain/games/engine` | Describes a game: id, title, description, icon, level count, `createSession(level, seed)`. |
| `GameSession` | `domain/games/engine` | One run: `size`, `position`, `finished`, `score`, `lives`, `hint`, `prompt`, `answer(String)`. |
| `GameSession.Outcome` | same | `CORRECT`, `WRONG`, `IGNORED`, `MOVED` (a legal move that neither solves nor fails). |
| `StarRating` | `domain/games/engine` | 3 stars when perfect, 2 at 60% or more, 1 otherwise. Capped at 2 when lives reach 0. |
| `LevelProgression` | same | Saves the best stars per game and level on this phone. Two stars passes and unlocks the next level. |
| `GameResults.finishRun` | `presentation/ui/games/common` | Rates a finished session, saves its stars for the level and returns them. Called by `GameChrome`. |
| `GameResults.starLine`, `loadVerseOfDay` | same | Star glyphs and today's verse for the results screen. |
| `GameChrome` | `presentation/ui/games/common` | **The shared results screen and pause overlay.** Draws through a small `GameChrome.Host` interface. |

## Player-facing progression

Home shortcuts and Games cards enter through `GamesHubDialog.launchGame`. The main dialog keeps the shipped question, ladder, and dice mechanics. It opens their campaign picker before creating a run. Vine Words, Lamp Slide, and Story Mosaic keep their independent dialogs. When changing a mechanic, check the launcher used by the hub as well as any prototype or catalog session.

The current campaigns use these progression rules:

- Knowledge rounds use the bundled easy, medium, and hard banks. Two stars pass a round. Who am I? reduces the number of clues instead.
- Word Search adds target words, Memory Match adds pairs, and Story Timeline adds events. Solving the board clears the level. Stars also reflect moves or mistakes where those are part of the game.
- Ladder of Faith keeps its lives and lifelines. Each successive climb adds a hard rung. Reaching the top clears the level. Road to Jerusalem clears on a win, with longer roads and a phone head start in the hardest band.
- Completing a Vine Words crossword or Lamp Slide painting unlocks the next stage even if the player needed help. Story Mosaic keeps all chapters open and remembers the next unfinished scene in each chapter.

The active screens use `GameResults.saveLevel` as soon as the final answer or move is accepted. It writes the last result, best stars, clear status, and any unlock together in account-prefixed local preferences. `GameChrome.showLevelResult` displays an already saved result. Any animation or Continue button runs after the save. The game picker shows the current unlocked level, cleared count, and saved result. The earlier personal best and solve counters remain available on the hub.

## Adding a game

1. Implement `GameSession` for the game's rules. Keep it free of Android types so it can be unit tested.
   If the game advances after each answer, extend `AdvancingSession` in `domain/games/catalog`.
2. Implement `GameDefinition` and return the session from `createSession(level, seed)`.
   Use `RoundDefinition` or a `*Definition` in `domain/games/catalog` as a model.
3. In the play screen, answer through `session.answer(String)`. Show `session.prompt()` when it is
   not null, otherwise keep the screen's own text.
4. When `session.finished()` is true, make one call. It saves the stars once and shows the results:
   ```java
   new GameChrome(host).finishedRun(gameId, title, session, level, levelCount, replay);
   ```
   `host` is a `GameChrome.Host` (the games tab implements it in `GamesHubDialog.chrome()`).
   `gameId` is the catalog id, and the stars are saved to `perazim_game_scores_v1`.
   Games that still use the score-and-size form call `finishedRun(gameId, title, score, size, replay)`,
   which saves level 1 only.
5. For pause, call `new GameChrome(host).pauseButton(resume)`, where `resume` re-renders the same question.
6. Add a unit test for the session rules. `StarRating` and `LevelProgression` already have tests.

## Rules

- Campaign play and progress work offline. The optional church leaderboard requires a compatible server and the existing account eligibility checks.
- Keep the adapters thin. The rules live in the session, not in the screen.
- Do not hard-code `lives` as `NO_LIVES` when the game has lives; pass `session.lives()`.
- Art follows `docs/games/ART_BRIEFS.md`. A game uses `game_<id>` art when it exists, else its icon.

## Registering a game screen (GameContext)

A game with its own dialog registers a launcher once, from `GameLaunchers.registerAll()`
(called from `PerazimApplication.onCreate`). The hub passes a `GameContext` when the card is tapped:

```java
GameRegistry.register("story_mosaic", ctx -> StoryMosaicDialog.show(ctx.activity(), ctx.ui()));
```

- `ctx.activity()` is the activity to open a dialog on.
- `ctx.hub()` is the back action that returns to the Games list.
- `ctx.scores()` is the games preferences file. `ctx.scorePrefix()` is the owner key from
  `GamesHubDialog.ownerKey(ownerId)`, so stars and progress belong to the account, not the device.
  Store a game's stars under `ctx.scorePrefix() + gameId`, which is what `GameChrome` and
  `LevelProgression` already do.
- `ctx.ui()` returns a `GameUi`, already configured with the context. Its `page(title, subtitle, playing)`
  builds the scrolling page with the back button, and `text`, `button` and `gap` draw on it.
- `ctx.ownerCheck()` and `ctx.seedReset()` cover the account check and the seed reset for Replay.

## Star rules and hints

- `GameDefinition.starRule()` returns a `StarRule`, which takes the whole finished `GameSession`.
  The default uses `StarRating` (score, size and lives). A game overrides it to use hints, for example
  `session.hintsUsed()`.
- `GameSession.hintsUsed()` defaults to 0. A session that shows hints overrides it.

## Next level

`GameChrome.finishedRun(game, title, session, level, replay, next)` enables Next level when the level
after this one is unlocked (`LevelProgression`, two or more stars). Pass `null` to keep it off.

## Existing register lines

Story Mosaic and Lamp Slide register their own launchers on their feature branches. Those lines use the
earlier `activity -> ...` form and move to `ctx -> ...` (with `ctx.activity()` and `ctx.ui()`) when those
branches merge onto this one. The deepen threads adapt their own lines.

## Motion and feel (GameFx)

`presentation/ui/games/common/GameFx` is the motion kit. It uses only ValueAnimator, ViewPropertyAnimator,
ObjectAnimator and Canvas, so it runs on small phones. When the system's "Remove animations" setting is on
(animator duration scale 0), every effect jumps to its end state.

| Call | What it does |
|---|---|
| `GameFx.attachPress(button)` | Press bounce to 0.94 and a spring back. `GameUi.button` calls it for every button. |
| `GameFx.enter(screen)` / `GameFx.exit(screen, end)` | Slide up and fade in on enter, slide down and fade out on exit. `GameUi.page` calls enter. |
| `GameFx.correct(view)` / `GameFx.wrong(view)` | Correct: glow pulse, tick and light haptic. Wrong: horizontal shake, buzz and haptic. |
| `GameFx.pulse(view, glowColor)` / `GameFx.shake(view)` | The parts of the two calls above, for use on their own. |
| `GameFx.starRow(context, stars)` + `GameFx.popStars(frame, stars)` | Three stars that pop in one by one with a particle burst. `GameChrome` results use these. Add the frame with `GameChrome.Host.addRow`. |
| `GameFx.countUp(textView, target, prefix, suffix)` | Counts a score up. |
| `GameFx.shimmer(view)` | One light sweep across a view, used for Next level when the level unlocks. |
| `GameFx.dim(root)` | Dims the screen behind a pause overlay. Returns the shade view; remove it on resume. Blur is not used (it needs Android 12 and is costly on small phones). |
| `GameAudioSettings` | Gives the player separate Music, Voice, and Sound effects controls. |

Game audio uses bundled recordings and effects, routed through `GameAudio`. The mixer controls playback independently of the animations.

`GameChrome` already uses the results effects. Game screens call `GameFx.correct` and `GameFx.wrong` on each answer.

## Game-feel art (GameArt)

The art sheets live in `src/main/assets/games/ui/` (from 333e174). `GameArt` loads each one once, at half size.

- `ctx.ui().button(...)` draws the orange or purple pill frame. `styleButton` does this for every button.
- `ctx.ui().panel()` adds a card-style panel to the page, framed with the purple panel from the UI sheet.
- Particle bursts (`GameFx.starRow` / `popStars`) draw star, sparkle and flare sprites from the particle sheet.
  Without the sheet they fall back to drawn dots.
- Level pickers: `GameArt.levelMap(context)` returns the level-map scene. `GameArt.levelNodesFor(width, height)`
  gives the level node positions in pixels, from the first level to the last, along the path.

The sprite and node coordinates are approximate, read from the sheets by eye. Adjust `GameArt`'s constants if a
crop or node looks off on a device.
