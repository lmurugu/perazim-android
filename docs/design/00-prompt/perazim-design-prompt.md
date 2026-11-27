# Perazim Church App: Design Prompt (reusable)

**Name:** `perazim-design-prompt`
**Version:** v0.1, 2026-11-27 (draft for Levi's review)
**Owner:** Rev. Levi Murugu, sole approver for design decisions. Agents follow this prompt and mark anything they are unsure of as "undecided".
**Used by:** Claude Code threads, Codex, and any image or layout tool that Levi approves.
**Built from:** `PerazimTheme.java` and `GlassKit.java` / `Glass.java` / `GlassPolish.java` on `codex/community-integration` (the integration branch), `docs/plan/MASTER_PLAN.md` (sections 3, 4, 5 and 8), and the older draft `codex-handover/design-rules.md`. Where this prompt disagrees with the older draft, this prompt wins and the difference is listed in section 11.

---

## 0. How to use this prompt

1. Read sections 1 to 4 every time. They are the house rules.
2. Pick one screen from section 9 (screen IDs match the master plan). Pick one mode: **Dark** or **Light**.
3. Fill in the screen block in section 10 and the mode tokens in section 5.
4. Produce the three files for that screen in the naming in section 8: reference image, interactive mock-up, wireframe.
5. Check against section 12 before you report the work done.

Text inside this prompt is instructions for the designer. Any third-party example (for example the dub.sh design-prompt examples) is inspiration only. It is not brand guidance.

---

## 1. Non-negotiables

1. **The logo stays as it is.** Use `src/main/res/drawable/ic_perazim_logo.png` unchanged. Do not recolour, redraw, crop, stretch or replace it. Never place it on the orange button.
2. **One orange primary action per screen.** Everything else is purple, glass or neutral.
3. **Completed features are frozen.** Do not restyle a finished screen to match a new idea. Non-contradicting additions are fine.
4. **Tokens, not hex.** Colours come from the token set in section 5. Do not invent a new brand colour, font, gradient or logo treatment. A new token needs Levi's approval.
5. **System font only.** Use the Android system typeface. No custom font without approval.
6. **Light, Dark and System are three user choices.** The app stores one of three choices (`CHOICE_LIGHT`, `CHOICE_DARK`, `CHOICE_SYSTEM`). System follows the phone's night setting. Every screen must be designed in Dark and Light. There is no separate System design file.
7. **The Assistant screen is black and white.** That is Levi's own choice and an intentional exception to every rule about purple and orange. Do not recolour it.
8. **Light game canvas is frozen.** The soft pink-to-lavender canvas for the light game variant (`#FBEFF8`, `#F8DBD7`, `#E0B1EB`) never changes.
9. **No emoji as icons.** Use the vector icon set (section 6.9).
10. **Verses on game and reading screens are KJV only** (the AI rule and the game rule). Other translations are shown only where the Bible reader already shows them.

---

## 2. Who the app is for

- A church app for **Perazim Mission Church (PMCI)**, Embu, Kenya, with campuses named in the master plan (Embu Headquarters, Nairobi Fellowship, Meru Outreach, Online Campus). Keep the campus list as the plan gives it. Do not invent campuses.
- Users are mostly adults in Kenya on Android phones. Children may read and watch public content. Accounts and the Assistant are adults only.
- Tone: calm, warm, reverent and plain. Short everyday words. Scripture first. No pressure, no streak guilt, no fake urgency, no money pressure (giving is a copy-the-Paybill action, never a sales screen).
- The app is Bible-centred and Protestant, Pentecostal and Evangelical in teaching. Design must not imply any other doctrinal position in images, icons or copy.
- Swahili or Sheng copy in a design needs Levi's native check before it goes in a reference image. Mark it "copy to check" in the file.

---

## 3. Visual character (the house look)

- **Frosted glass over a deep purple canvas.** Cards and buttons are translucent white glass with a light rim and a soft sheen across the top half. This is what `GlassKit` and `Glass` build.
- **Purple** is the brand family: canvas, glass, and the purple pill for selected states.
- **Orange** is the warm call to action. It appears once per screen. Its glow is a soft orange halo, not a hard shadow.
- **Soft light glows** sit behind the page: purple top-left, orange lower-right. They are part of the page background, not a token you can use elsewhere.
- **No heavy shadows, no Android elevation.** Separation comes from the glass rim and the hairline, not from drop shadows.
- Keep the layout quiet. Lots of breathing room. Few colours on one screen.

---

## 4. Layout rules

- Phone design width: **360 dp** (test also at 320 and 412). No horizontal scrolling at any width.
- Screen gutter: **16 dp** each side. Bottom padding **32 dp** so content clears the tab bar.
- Card inner padding: **16 dp** (dense cards 12 to 14 dp).
- Gap between cards: **16 dp**. Gap between small tiles: **10 dp**.
- Icon-to-text gap: **10 to 12 dp**.
- Minimum touch target **48 dp**. Primary button **56 dp** tall (the codex branch uses 52 dp inside games; use 56 dp elsewhere).
- Navigation: a floating glass pill tab bar at the bottom with five items. The Give item is the centre circle (see section 9 and section 11, open item 3).
- Top app bar: logo (28 dp), wordmark "Perazim" (20 sp bold, slight letter spacing), profile icon on the right.
- Mini-player (when audio plays) sits just above the tab bar as a glass card.

---

## 5. Tokens

### 5.1 Dark mode (default)

| Token | Hex or value | Use |
|---|---|---|
| Canvas | `#2A0F33` to `#160520` vertical, with glows | Page background |
| Glass card | white 15% (`#26FFFFFF`) with 18% rim | Cards, panels |
| Glass control | white 20% (`#33FFFFFF`) with 20% rim | Chips, small round buttons |
| Solid surface | `#3D1749` | Opaque fallback for dialogs and popups |
| Solid surface 2 | `#4F1E5C` | Inputs, chips on opaque layers |
| Purple muted | `#8B5A9A` | Inactive icons, secondary outlines |
| Purple soft | `#A67BB3` | Eyebrow labels |
| Purple brand | `#681A7D` (gradient `#8A2BA3` to `#681A7D`) | Selected pill, Give centre button |
| Orange primary | `#E17D2F` (gradient `#E88636` to `#C06119`) | The one primary button |
| Orange soft | `#E8A86A` | Pressed or focused orange, "step" text |
| Text primary | `#F5F0F7` | Headings |
| Text secondary | `#B9A8C0` (labels `#CDBBD6`, body in cards `#E6D8EE`) | Body, metadata |
| Text disabled | `#6E5C78` | Disabled text, placeholders |
| Stroke | white 20% (`#33FFFFFF`) | Hairlines, rims |

### 5.2 Light mode

| Token | Hex or value | Use |
|---|---|---|
| Canvas | `#F8F1FB` to `#EFE0F6` to `#E6D2F0` vertical, with soft glows | Page background |
| Glass card | white 60% (`#99FFFFFF`) with a purple 40% rim | Cards |
| Glass control | white 80% (`#CCFFFFFF`) | Chips, round buttons |
| Solid surface | `#FFFFFF` | Dialogs |
| Solid surface 2 | `#F1E4F6` | Inputs |
| Purple muted | `#7B5A88` | Inactive icons |
| Purple soft | `#7A3E8F` | Eyebrow labels |
| Text primary | `#2A0F33` | Headings |
| Text secondary | `#5E4A68` (`#5A3A66` for body in cards, `#8A4A12` for accent text) | Body |
| Text disabled | `#A793B0` | Disabled |
| Stroke | purple 20% (`#33681A7D`) | Hairlines |
| Orange primary | same as dark | The one primary button |

Light mode uses the same orange and purple brand tokens as dark. Do not use the light game canvas (section 1, rule 8) as the app light canvas.

### 5.3 Colours that are not tokens yet (do not reuse without approval)

| Hex | Where it appears | Status |
|---|---|---|
| `#1A0A20` | Sermons background and the sermon player | Undecided. Media-only darkest canvas. Ask Levi before reuse. |
| `#99000000` | Modal scrim in one dialog | Keep to that dialog. |
| `#E62A0F33`, `#B32A0F33` | Scrims over photo tiles | Keep to photo tiles. |
| `#8E30A8` | Old brightened purple | Retired. Do not use. |
| `#FFFFFF` on orange | Text on the primary button | Use the existing `readableText` rule. |

### 5.4 Status colours

- Success `#10B981`, error `#EF4444` (from `PerazimTheme`). Use only for status messages, never for decoration.

---

## 6. Components

### 6.1 Glass kinds (how a fill becomes a surface)

Each surface is one of three kinds. Choose by role, not by the old colour.

- **ORANGE:** the primary call to action. Solid enough for white text.
- **TINTED:** a saturated colour (brand purple, green, red) as see-through tinted glass.
- **FROSTED:** pale, white or near-black surfaces become clear frosted glass.

Text on glass uses `readableText`. Do not pick a text colour by eye when the surface is translucent; the contrast must meet WCAG AA (4.5:1 for body, 3:1 for large text).

### 6.2 Top app bar, tab bar and mini-player

- Top bar: logo, wordmark, profile icon in a 40 dp glass circle. No bar background; the canvas shows through.
- Tab bar: five items (Home, Sermons, Worship, Prayer, Give). Active item is a purple pill with white icon and label. Inactive is muted purple icon and label. Icons 18 sp, labels 11 sp.
- Mini-player: frosted glass card, 12 dp radius, with title, progress and play/pause.

### 6.3 Hero card

- The key card of a screen only (for example Home "Today"). Larger radius (XL), stronger glass, eyebrow date in purple soft uppercase 11 sp bold.
- Title text primary, body text secondary, one optional step label in orange soft.

### 6.4 Standard card

- Glass card, 16 dp padding, title 16 to 18 sp bold, body 14 sp secondary.
- Photo tile: 128 dp high, radius per the scale in 7.1, a bottom scrim (`#B32A0F33`), white caption padded 16 dp × 14 dp.

### 6.5 Buttons

| Role | Look | Where |
|---|---|---|
| Primary (one per screen) | Orange gradient, white bold text, 56 dp tall, soft orange glow | Main action |
| Secondary | Purple pill (`#8A2BA3` to `#681A7D`), white bold text | Next, start |
| Quiet | Glass pill with rim, text primary | Filters, cancel |
| Icon button | 40 dp glass circle, line icon | Top bar, reader toolbar |
| Disabled | Opacity 50%, or text disabled | Unavailable actions |

Text on buttons: 14 to 17 sp bold. Use sentence case except the primary button where all caps is allowed.

### 6.6 Chips and filters

- Selected: purple pill, white text. Others: glass pill, text primary. Padding 9 dp × 16 dp, 12 sp.

### 6.7 Inputs and search

- Background solid surface 2 (or glass control), rim stroke, radius 10 dp (or the scale in 7.1 for pill inputs), padding 14 dp.
- Text primary 16 sp (19 sp in game answer boxes). Hint text secondary.

### 6.8 Dialogs and sheets

- Dialog panel: solid surface or dialog gradient (`#3D154A` to `#24092C`), radius 16 dp (scale in 7.1), hairline rim.
- Title text primary, body text secondary. Actions at the bottom, one primary.

### 6.9 Icons

- Vector set in `src/main/res/drawable/ic_*.xml`. Line icons, 24 dp grid, white stroke so the tint decides colour.
- Nav icons: `ic_nav_home`, `ic_nav_sermons`, `ic_nav_worship`, `ic_nav_prayer`, `ic_nav_me`, `ic_give`. Themed Christian icons: `ic_christian_*`.
- Tint: active or on dark `#F5F0F7`; inactive `#8B5A9A`; on orange, white.
- Icon sizes: nav 18 sp; card icons 24 to 44 dp; icon badges in coloured squares (44 dp) on Home tiles.

### 6.10 Game screens

- Canvas as section 5, full screen. Page padding 20 dp sides, 16 dp top, 28 dp bottom.
- Game card: solid surface, radius 14 dp, padding 16 × 12 dp.
- Primary button 52 dp min height, 17 sp, orange.
- Light variant exists only for Lamp Slide in the approved mockup. Other games stay dark until Levi decides (section 11, open item 5).
- Game art and audio stay within the child-safe rules in the master plan (section 4.9).

### 6.11 Assistant (black and white)

- Black and white only. Solid black and white surfaces, no purple glass, no orange primary, no glows. This is Levi's choice and stays.
- Show sources and the crisis reply in the same layout as other screens, but in black and white. Emergency numbers come first (999 or 112), then the lines in the crisis library.
- Disclaimers and feedback rows follow the same black and white rules.

---

## 7. Shape and type

### 7.1 Radii (dp)

The token set in `PerazimTheme` on the integration branch is: `SM 6`, `MD 10`, `LG 22`, `XL 28`, `PILL 999`.

Working rule until Levi decides (section 11, open item 1):

- Hero card: XL (28).
- Main cards and glass panels: LG (22).
- Inner containers, inputs, photo tiles: MD (10).
- Small secondary buttons and chips: SM (6) or PILL (999) for chips.
- Pills, tab bar, mini-player: PILL (999) or LG (22).

Do not use the older draft's 14 or 16 dp radii for new work.

### 7.2 Type scale (sp)

| Step | Size | Weight | Colour |
|---|---|---|---|
| Micro | 9 | regular | secondary |
| Caption, eyebrow label | 11 | bold, uppercase, tracking 0.1 em | purple soft (dark) or purple soft (light) |
| Body small | 12 | regular | secondary |
| Body | 14 | regular, line height 1.15 | secondary |
| Subtitle | 16 | bold | primary |
| Title | 18 | bold | primary |
| Headline | 22 | bold | primary |
| Hero | 26 | bold | primary |
| Wordmark | 20 | bold, letter spacing 0.04 | primary |

Typeface: the Android system font. Ask Levi before any change.

---

## 8. Design folder and naming convention

The design folder lives in the repo at `docs/design/` and mirrors `/mnt/project-files/design/`.

### 8.1 Folder layout

```
docs/design/
  00-prompt/
    perazim-design-prompt.md          (this file)
  01-design-system/
    DS-01_design-system-reference_dark_v01.png
    DS-01_design-system-reference_light_v01.png
    DS-01_tokens_v01.md               (token table as text, copied from section 5)
  02-screens/
    01-home/
      01-home_REF_dark_v01.png        (design reference image)
      01-home_REF_light_v01.png
      01-home_MOCK_dark_v01.html      (interactive mock-up, self-contained)
      01-home_MOCK_light_v01.html
      01-home_WIRE_v01.png            (wireframe, greyscale, mode-neutral)
    02-bible-reader/
      ...
  03-upscaled/
    DS-01_design-system-reference_dark_x2_v01.png   (single upscaled image)
    01-home_REF_dark_x2_v01.png
    ...
  INDEX.md                            (one line per file: name, screen ID, mode, version, status)
```

### 8.2 File names

Pattern: `<screen-id>_<kind>_<mode>_<version>.<ext>`

- `<screen-id>`: `NN-slug` from section 9. Two digits, hyphen, lowercase slug. The design-system sheet uses `DS-01`.
- `<kind>`: `REF` (reference image), `MOCK` (interactive mock-up), `WIRE` (wireframe), `TOKENS` (token text), `UP` is not used; upscaled files add `x2` before the version.
- `<mode>`: `dark`, `light`. Wireframes have no mode and omit it.
- `<version>`: `v01`, `v02`, and so on. Never overwrite an earlier version. Keep the old file and add a new one.
- `<ext>`: `.png` for images, `.html` for mock-ups, `.md` for text.

Examples:

- `01-home_REF_dark_v01.png`
- `01-home_MOCK_light_v02.html`
- `01-home_WIRE_v01.png`
- `DS-01_design-system-reference_dark_x2_v01.png` (in `03-upscaled/`)

### 8.3 Standards for each kind

- **REF:** a single frame at 1080 × 2340 (phone portrait, 9:19.5), at 2x. Full screen, status bar included. The reference shows the approved look only; no notes on the image.
- **MOCK:** one HTML file, no build step, no external calls. Inline CSS and JS. Shows the screen and its states (default, loading, empty, error, offline, signed out). Uses only tokens from section 5. Opens in Chromium at 360 dp width.
- **WIRE:** a greyscale layout at 360 dp width. Boxes and labels only. Names each element as in section 10. Saved as PNG and, when possible, SVG.
- **DS-01:** the design-system sheet. Contains the token swatches, glass kinds, buttons, chips, inputs, the five-item tab bar, the type scale and the radii. Shows dark and light side by side.
- **UP (upscaled):** a 2x or 4x copy of a REF or DS file, kept as one image, with no added text.

### 8.4 Rules for the folder

- Each screen has its own folder. Do not put two screens in one folder.
- Never rename a published file. Add a new version.
- `INDEX.md` lists every file, its status ("under review", "approved by Levi", "undecided") and the thread that made it. Agents never mark a file "approved"; only Levi does.
- Copy any file Levi approves into the repo; the project-files copy is the working copy.

---

## 9. Screen list (IDs from the master plan)

IDs match `docs/plan/MASTER_PLAN.md` sections 4 and 5. Add a new ID only when a new screen is added to the plan.

| ID | Screen | Plan reference | Tab or dialog |
|---|---|---|---|
| 01-home | Home (Today card, routine, doors, daily verse) | 4.1, 5.1 | Tab 1 |
| 02-bible-reader | Bible reader (highlights, compare, share) | 4.2 | Dialog |
| 03-bible-search | Bible search and search overview | 4.2 | Dialog |
| 04-sermons | Sermons list (church feed, fallback) | 4.3 | Tab 2 |
| 05-sermon-player | Now Playing (full screen) | 4.3 | Dialog (dark media canvas, undecided) |
| 06-worship | Worship hymnal | 4.4 | Tab 3 |
| 07-fellowship | Community feed (pinned events, Inbox, new post) | 4.5 | Tab 4 |
| 08-prayer | Prayer requests and amen | 4.5 | Tab 4 |
| 09-groups | Groups and member lists | 4.5 | Tab 4 (not yet verified) |
| 10-give | Give (Paybill, M-Pesa steps, copy button) | 4.6 | Tab 5 |
| 11-me | Me (profile, account, privacy) | 4.7 | Top-bar profile |
| 12-auth | Sign in, register, PIN | 5.2 | Dialog |
| 13-onboarding | First run: guest mode, campus, privacy | 5.2, 6.1 | Full screen |
| 14-privacy-notice | Privacy notice | 5.2 | Dialog |
| 15-membership | Membership claim and status | 5.2, 4.7 | Dialog |
| 16-pin-upgrade | Move from the legacy PIN | 5.2 | Dialog |
| 17-assistant-chat | Perazim Assistant (black and white) | 4.8, 6.11 | Full screen |
| 18-assistant-memory | What the guide remembers (opt-in) | 5.2 | Full screen |
| 19-games-hub | Games hub (cards: icon, Bible character, title, one line, best score, Play) | 4.9 | Dialog |
| 20-game-vine-words | Vine Words | 4.9 | Game |
| 21-game-lamp-slide | Lamp Slide (has light variant) | 4.9 | Game |
| 22-game-ladder | Ladder of Faith | 4.9 | Game |
| 23-game-fill-gap | Fill the Gap | 4.9 | Game |
| 24-daily-reading | Daily Reading plan | 4.10 | Dialog or card |
| 25-notifications | Notifications | 5.2 | Dialog |
| 26-qr-connection | QR connection (family, classroom) | 5.2 | Dialog |
| 27-reflections | Reflections (journal) | 5.2 | Dialog |
| 28-local-bible-audio | Local Bible audio player | 5.2 | Dialog |
| DS-01 | Design-system reference | 8 | Sheet |

The other games (Who Am I, Word Search, Memory Match, Story Mosaic, Testament, Timeline, Road, Verse Builder, Mini Crosswords, Who Said It) get IDs `2x-game-<slug>` when their turn comes.

---

## 10. Screen block (fill in for each screen)

Copy this block, fill it in, and keep it with the screen folder.

```
Screen ID:        NN-slug
Purpose:          one sentence, plain words, what the person does here
Mode(s):          dark | light (both required before REF is final)
Layout (top to bottom):
  1. top bar (logo, wordmark, profile icon)
  2. ...
  n. tab bar (active item: ...)
Elements:         each element with its glass kind (ORANGE, TINTED, FROSTED) and token
Primary action:   the one orange button (or "none")
States:           default, loading, empty, error, offline, signed out, (others from the plan)
Copy:             headings and buttons in English; Swahili or Sheng marked "copy to check"
Scripture:        KJV only for quotes; reference shown as "Book Chapter:Verse"
Accessibility:    contrast checked; touch targets 48 dp or more; TalkBack labels listed
Do not:           the list of things this screen must avoid
Source:           plan section, branch, and file names this follows
```

---

## 11. Decisions Levi has not made yet (undecided, do not settle these alone)

Each open item has a recommended option. Keep the recommended option in drafts and mark it "undecided".

1. **Radius scale.** Code says LG 22 and XL 28. The older draft says 14 and 16. *Recommended:* use the code scale (section 7.1) and retire the draft numbers.
2. **Light and dark colour aliases.** `PerazimTheme` still has light-named constants that hold dark values (for example `COLOR_PURPLE_TINT`, `COLOR_BG_NEUTRAL`). About 183 references use them. *Recommended:* new work uses the semantic names in section 5; the rename happens in a separate approved change (PR 96 draft).
3. **Give centre button colour.** The master plan says orange orb. The codex branch draws a purple orb (`createGiveOrb`). *Recommended:* keep the purple orb as built, and make Give's primary action (the copy-Paybill button) the one orange button on that screen.
4. **Sermons canvas `#1A0A20`.** Not a token. *Recommended:* leave it for media screens only and decide later.
5. **Light variant for other games.** Only Lamp Slide has one. *Recommended:* other games stay dark until Levi approves a light set.
6. **Card elevation.** `card_elevation 4dp` exists in `dimens.xml` but is not the house look. *Recommended:* leave it unused and remove it in a later cleanup.
7. **Mode switch in the design.** The older draft says "do not follow system night mode". That is no longer true (section 1, rule 6). *Recommended:* design Light and Dark for every screen and treat System as the phone choice.

---

## 12. Quality check before a screen is reported done

- [ ] Logo unchanged, not on the orange button.
- [ ] One orange primary button, or none.
- [ ] Every colour is a token from section 5 or 5.3 (with Levi's approval for 5.3).
- [ ] Dark and light both done (REF and MOCK), WIRE present.
- [ ] Renders at 360 dp with no horizontal scroll; checked at 320 and 412 dp.
- [ ] Touch targets 48 dp or more; primary 56 dp.
- [ ] Body text meets 4.5:1 contrast on its glass or canvas; large text 3:1.
- [ ] States shown: default, loading, empty, error, offline, signed out.
- [ ] Copy is plain English; Swahili or Sheng marked "copy to check".
- [ ] Scripture quotes are KJV, except where the reader already shows another translation.
- [ ] No emoji used as icons; vector icons only.
- [ ] Assistant screen stays black and white.
- [ ] Files named per section 8, with a new version rather than overwriting, and listed in `INDEX.md`.
- [ ] Status written as "under review" or "undecided"; never "approved".

---

## 13. Image-generation prompt template (for a tool Levi has approved)

Use this only with a tool Levi has approved. Fill the brackets from section 10. Adapted from the structure of the public design-prompt examples on dub.sh (opening deliverable line, app summary, style, palette with hex, type, numbered screens, quality bar, and "NOT" exclusions). Treat the example prompts as inspiration only.

```
Generate an image of a complete modern mobile app UI design system and all screen designs for
"Perazim", a Bible-centred church app for Perazim Mission Church in Embu, Kenya.

App summary: [one sentence from section 2]. The app helps members [three plain tasks from the plan].

Design style: frosted glass over a deep purple canvas. Translucent white glass cards with a light
rim and a soft top sheen. One orange primary button per screen with a soft orange glow. Generous
16 px gutters, quiet layout, large soft radii (cards 22 px, hero 28 px, pills fully round).
NOT cluttered, NOT corporate, NOT neon, NOT dark-blue tech, NOT emoji icons, NOT heavy shadows.

Colour palette (dark mode): canvas #2A0F33 to #160520 with a purple glow top-left and an orange
glow lower-right; glass card white 15 percent; purple brand #681A7D (gradient #8A2BA3 to #681A7D);
orange primary #E17D2F (gradient #E88636 to #C06119); text primary #F5F0F7; text secondary #B9A8C0.
Light mode: canvas #F8F1FB to #E6D2F0; glass white 60 percent; text primary #2A0F33.

Typography: the Android system font, bold headings, 14 px body, 11 px uppercase eyebrow labels.

Logo: the existing Perazim logo, unchanged, on the top bar. Never on the orange button.

Screens (numbered, from section 9 of the design prompt):
1. [screen ID and name] — [elements in order]
2. ...

Presentation: one frame per screen at 1080 x 2340, dark and light side by side. A design-system
sheet with the tokens, glass kinds, buttons, chips, inputs, tab bar and type scale.

Quality bar: App Store product quality, consistent across all screens, every screen checked at
phone width, no text below contrast 4.5 to 1.
```

---

## 14. Source list and version history

- Tokens and glass: `src/main/java/org/perazimchurch/app/ui/theme/PerazimTheme.java`, `GlassKit.java`, `Glass.java`, `GlassPolish.java`, on `codex/community-integration`. Read these before any token change.
- Plan: `docs/plan/MASTER_PLAN.md` (PR 98) sections 3, 4, 5, 8.
- Older draft (superseded where it differs): `codex-handover/design-rules.md`, copied to `/mnt/project-files/codex-handover/design-rules.md`.
- Mockups: `/mnt/project-files/game-mockups/`, `/mnt/project-files/dark_*.png`, `/mnt/project-files/light_*.png`, `docs/review/ui-redesign-*.png`. Code is the truth when a mockup differs.
- v0.1 (2026-11-27): first version, drafted from the sources above. Pending Levi's review.
