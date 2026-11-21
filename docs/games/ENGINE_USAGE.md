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

- Everything works offline. No score upload, no ads.
- Keep the adapters thin. The rules live in the session, not in the screen.
- Do not hard-code `lives` as `NO_LIVES` when the game has lives; pass `session.lives()`.
- Art follows `docs/games/ART_BRIEFS.md`. A game uses `game_<id>` art when it exists, else its icon.
