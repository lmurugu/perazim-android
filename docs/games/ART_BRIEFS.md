# Games tab: art briefs

Every game card on the Games tab uses one layout: a game icon, a Bible character,
a title, one line, the best score and a Play button. These briefs describe the art
each game needs. The ElevenLabs prompts below are the source for generated assets.

## Shared style

- Palette: deep purple `#2A0F33`, royal purple `#681A7D`, soft lilac `#C06BE0`,
  orange `#E8772E` for accents. Frosted-glass look: soft light, no harsh outlines.
- Flat, friendly, cartoon-style illustration with rounded shapes. Readable at 40 px.
- Characters are small, kind and respectful. No faces shown in close-up, no artist
  likeness, no text inside the image.
- Transparent or plain purple background, so the card colour shows through.

## Games

| Game | What the player does | Art needed | Character |
|---|---|---|---|
| Bible Quiz | Answers four-choice questions about Bible stories | Scroll icon, open scroll with a star | Shepherd |
| Who am I? | Guesses a Bible character from clues | Magnifying glass over a question mark, shown as a lantern | Explorer |
| Verse Builder | Rebuilds a KJV verse from word tiles | Puzzle tiles with a scroll | Dove |
| Ladder of Faith | Climbs 20 rungs with three lives | Ladder of light climbing to a dove | Shepherd |
| Word Search | Finds Bible names in a letter grid | Letter grid with a highlighted word | Shepherd |
| Memory Match | Matches pairs of Bible symbols | Two face-down cards, one showing a dove | Dove |
| Story Timeline | Puts events in story order | Timeline with three small scenes, left to right | Explorer |
| Mini Crossword | Fills a five-by-five crossword | Crossword grid with a pencil | Shepherd |
| Road to Jerusalem | Rolls a die and races to the city | Winding road with a small city on a hill | Explorer |
| Old or New? | Sorts Bible books into Old or New Testament | Two scrolls, one aged and one bright, with a book between them | Explorer |

## Character cards (small cartoon, about 36 px)

- Shepherd: a cartoon shepherd with a staff and a lamb. Warm orange robe.
- Dove: a white dove with an olive branch. Lilac background accent.
- Explorer: a cartoon traveller with a walking stick and a small bag. Purple cloak.

## ElevenLabs prompts

Generate one image per icon and one per character. Use a transparent or plain
background. Keep each prompt to the subject and the style above.

- `game-quiz`: "Flat cartoon icon of an open scroll with a small gold star, rounded shapes, deep purple and orange palette, plain background."
- `game-who`: "Flat cartoon icon of a magnifying glass over a question mark, rounded shapes, deep purple and orange palette, plain background."
- `game-verse`: "Flat cartoon icon of three wooden word tiles with a scroll, rounded shapes, purple and orange palette, plain background."
- `game-ladder`: "Flat cartoon icon of a golden ladder of light rising to a small white dove, purple and orange palette, plain background."
- `game-wordsearch`: "Flat cartoon icon of a grid of letters with one row highlighted in orange, purple palette, plain background."
- `game-memory`: "Flat cartoon icon of two face-down playing cards, one showing a white dove, purple and orange palette, plain background."
- `game-timeline`: "Flat cartoon icon of a horizontal timeline with three small scenes: a boat, a tent, a city, purple and orange palette, plain background."
- `game-crossword`: "Flat cartoon icon of a small crossword grid with a pencil, purple and orange palette, plain background."
- `game-road`: "Flat cartoon icon of a winding road leading to a small city on a hill, purple and orange palette, plain background."
- `game-testament`: "Flat cartoon icon of two scrolls, one aged and one bright, with a book between them, purple and orange palette, plain background."
- `char-shepherd`: "Small flat cartoon shepherd with a staff and a lamb, warm orange robe, plain background, no text."
- `char-dove`: "Small flat cartoon white dove holding an olive branch, lilac accent, plain background, no text."
- `char-explorer`: "Small flat cartoon traveller with a walking stick and a small bag, purple cloak, plain background, no text."

## Status

- Briefs written: yes (this file).
- Assets generated: yes. Each card uses a 256 px square PNG in
  `src/main/res/drawable-nodpi/` (`game_art_*` and `game_char_*`).
- Generator returned 16:9 frames. Images were centred-cropped to square where
  the subject fit. Word search was regenerated at 1:1. Timeline does not fit a
  centred square, so it is padded to square on its own lavender background.
- Road to Jerusalem keeps its vector icon (`ic_game_road.xml`). Its generated
  art was not produced, so no new asset exists for it.

## Who Said It? (added 2026-11-21)

- Art: `game_art_who_said_it` (256 px square). Generated at 16:9 with
  gpt-image-2.5-sunburst, one take, centre-cropped to square. The shepherd,
  lamb, dove and lamp all stay in frame.
- Prompt: "Flat cartoon illustration, centred square composition, of a friendly
  Bible-era shepherd with a speech bubble that contains no words, a small dove and
  a lamp nearby, warm orange and lilac purple palette, soft gradient plain
  background, clean vector style, no text, no letters."
