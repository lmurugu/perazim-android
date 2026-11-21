# Game mockups (design only)

Phone portrait screens, 1080x2340 (360x780 at 3x). Each game has four screens:
`01-start` (start or level select), `02-play` (mid-play), `03-hint` (hint or wrong-answer state), `04-results` (stars and a KJV verse).

Theme: frosted deep purple with orange accents, text slightly smaller than default. Light and dark variants are shown for Lamp Slide (`05-play-light.png`). All verses are KJV.

Placeholder art: scenes, paintings, tiles and game boards are simple SVG or CSS illustrations. Character images are the three approved PNGs (shepherd, dove, explorer) from PR 52. Final art is not included.

| Folder | Game | Notes |
|---|---|---|
| vine_words | Vine Words | Replaces Mini Crossword (clue mode), wheel and grid |
| lamp_slide | Lamp Slide | Sliding puzzle, par-based stars, light variant |
| story_mosaic | Story Mosaic | Scene jigsaw, Guided and Quiet modes |
| manna | Manna Match | Match-3, planned |
| fruit | Fruit of the Spirit | Colour sort, planned |
| trumpet | Seven Trumpets | Brick breaker, planned |
| dove | Dove Pop | Bubble shooter, planned |
| path | Shepherd's Path | Lane runner, planned |
| name | Daily Name | Daily word guess, planned |
| trivia | Bible Quiz | Existing |
| who_am_i | Who am I? | Existing |
| verse_builder | Verse Builder | Existing |
| ladder | Ladder of Faith | Existing, upgraded |
| word_search | Word Search | Existing |
| memory_match | Memory Match | Existing |
| timeline | Story Timeline | Existing |
| road | Road to Jerusalem | Existing |
| old_or_new | Old or New? | Existing |
| who_said_it | Who Said It? | Existing |
| fill_gap | Fill the Gap | Existing |

`contact-sheet.png` shows all 20 sets. `build.cjs` renders the PNGs from HTML with Playwright.

Count note: drawable-nodpi has 18 game tiles. Mini Crossword is merged into Vine Words, so 17 games plus the 3 new games gives 20 sets.
