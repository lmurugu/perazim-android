# Perazim Android Design Rules

**Audience:** Claude Code and Codex. Both agents follow this file so every tab, card, button, dialog and game looks like one app.
**Status:** Draft for Levi's review. Extracted from the code on `main` on 2026-11-27 and from `/mnt/project-files/dark_*.png` and `game-mockups/`.
**Rule zero:** Levi is the only approver for design decisions. Agents may follow these rules. Agents may not invent new brand colours, fonts or logo treatments.

---

## 0. Ground rules

1. **Keep the logo.** `ic_perazim_logo.png` (`src/main/res/drawable/`) is used as-is. Never recolour, redraw, crop or replace it.
2. **Never change the soft pink light gradient.** It is the light canvas of the light game variant (section 3.3). Its exact stops are in section 3.3.
3. **Completed features are frozen.** Do not restyle a finished screen to match a new idea. Non-contradicting enhancements are fine (for example, a new card on a finished tab).
4. **Frosted purple with orange accents** is the house look: deep purple canvas, purple frosted cards with a hairline border, and orange reserved for one primary action per screen.
5. **Use tokens, not hex literals.** New code takes colours from `PerazimTheme` (section 2). Do not add a new `Color.parseColor("#…")` in a screen file.
6. **Do not follow the Android system night mode.** The app has no `values-night` folder and no `uiMode` handling. Light and dark are chosen by which token set a surface uses (section 3). Do not add a system-dark switch without Levi's approval.
7. **Do not touch Codex's working tree.** Codex's Games and Daily Reading work is in progress. Read it, do not rewrite it, and keep changes to the shared files small and separate.

---

## 1. Where the rules live (source of truth)

| Concern | File | Notes |
|---|---|---|
| Dark tokens, gradients, spacing, type sizes, radii | `src/main/java/org/perazimchurch/app/ui/theme/PerazimTheme.java` | **The single source of truth.** Use this first. |
| Legacy string-free resources | `src/main/res/values/colors.xml`, `dimens.xml`, `styles.xml` | Partly duplicated by `PerazimTheme`. Do not add new values here without need. |
| Light tokens (legacy constants) | `MainActivity.java` lines ~76–95, `HomeUiBinder.java` lines ~76–97, `BibleReaderDialog.java` lines ~63–75, `AiBreakthroughGuideDialog.java` lines ~63–73, `JokesActivity.java`, `SermonsActivity.java`, `HymnsActivity.java`, `RiddlesActivity.java` | **See drift warning in section 2.4.** |
| Tab shell, top bar, bottom bar, mini-player | `MainActivity.java` (`createTopAppBar` ~405, `createMiniPlayer` ~470, `createBottomTabBar` ~581) | |
| Home tab | `presentation/ui/HomeUiBinder.java` | Live tab. Hero card, routine, shelves. |
| Sermons tab | `presentation/ui/SermonsUiBinder.java` | Live tab. Background `#1A0A20` (section 2.2). |
| Worship tab | `presentation/ui/WorshipUiBinder.java` | Live tab. |
| Fellowship / Prayer tab | `presentation/ui/FellowshipUiBinder.java` | Live tab. |
| Giving tab | `MainActivity.buildGivingScreen()` (~2210) and `GivingDialog.java` | |
| Me / Profile tab | `presentation/ui/ProfileUiBinder.java` | Live tab, largest surface. |
| Bible reader | `presentation/ui/BibleReaderDialog.java` | Mostly dark tokens. Some light legacy constants. |
| Games hub | `presentation/ui/games/GamesHubDialog.java` | Codex-owned on the games branch. Read only. |
| Launcher and app icons | `src/main/res/drawable/ic_launcher.png`, `ic_perazim_logo.png` | |
| Vector icons | `src/main/res/drawable/ic_*.xml` | Section 6. |
| Mockups (design only) | `/mnt/project-files/game-mockups/`, `/mnt/project-files/dark_*.png` | Look at these to see intent. Code is the truth when they differ. |

---

## 2. Colour tokens (dark system, the default)

Defined in `PerazimTheme.java`. Hex values are exact.

### 2.1 Core tokens

| Token | Hex | Use |
|---|---|---|
| `COLOR_BG_DEEP` | `#2A0F33` | App canvas, dialog window background, game screens |
| `COLOR_SURFACE` | `#3D1749` | Cards, content containers, game answer boxes |
| `COLOR_SURFACE_2` | `#4F1E5C` | Elevated surfaces, inputs, search bars, chips, secondary buttons |
| `COLOR_PURPLE_MUTED` | `#8B5A9A` | Inactive nav, secondary icons, outline borders |
| `COLOR_PURPLE_BRAND` | `#681A7D` | Active nav, links, selected chips, play buttons |
| `COLOR_PURPLE_SOFT` | `#A67BB3` | Eyebrow labels, soft labels, section kickers, icon tint |
| `COLOR_PURPLE_GHOST` | `#33681A7D` | 20% selection wash (translucent) |
| `COLOR_ORANGE_BRAND` | `#E17D2F` | **Only** the single primary CTA on a screen, game primary button |
| `COLOR_ORANGE_MUTED` | `#C4894F` | Rare secondary accent, orange outlines |
| `COLOR_ORANGE_SOFT` | `#E8A86A` | Pressed or focus on an orange CTA, "continue step" text on hero |
| `COLOR_ORANGE_GHOST` | `#3DE17D2F` | 24% orange selection wash (translucent) |
| `COLOR_TEXT_PRIMARY` | `#F5F0F7` | Headings and high-contrast text on dark |
| `COLOR_TEXT_SECONDARY` | `#B9A8C0` | Subtitles, metadata, body on dark |
| `COLOR_TEXT_DISABLED` | `#6E5C78` | Disabled text and placeholders |
| `COLOR_STROKE` | `#4A3554` | Card borders, dividers, bottom and top bar hairlines |
| `COLOR_WHITE` | `#FFFFFF` | Text on orange and purple CTAs |
| `COLOR_SUCCESS` | `#10B981` | Success states |
| `COLOR_ERROR` | `#EF4444` | Error states |

Other colours in use that are not in `PerazimTheme` yet, and should be added only with Levi's approval:

| Hex | Where | Meaning |
|---|---|---|
| `#1A0A20` | Sermons tab background, sermon player | Darkest canvas for media |
| `#1A1A1A` | `COLOR_TEXT_DARK` (`PerazimTheme`) | Dark text on light surfaces |
| `#E62A0F33`, `#B32A0F33` | Scrim gradients on photo tiles | 90% and 70% canvas scrim over images |
| `#99000000` | `AiBreakthroughGuideDialog` | Modal scrim |
| `#E88636` / `#C06119` | Primary CTA gradient | See 3.2 |
| `#8E30A8` | Legacy brightened purple | Do not use for new work |

### 2.2 Dark surfaces: where each is used

- Tab canvas: `COLOR_BG_DEEP` gradient (`createBackgroundGradient`), top to bottom.
- Top bar, bottom bar: their own gradients (section 3.2).
- Sermons tab: flat `#1A0A20`, set in `SermonsUiBinder` (also its status bar).
- Player dialogs: `PerazimPlayerDialog` style (`styles.xml`) with `#1A0A20` / `#FF1A0A20` window.

### 2.3 Light tokens (legacy light surfaces, still used)

Used on the light cards in legacy screens and some dialogs. Hex from the code constants:

| Name | Hex | Notes |
|---|---|---|
| Light canvas | `#FAF7FB` | Scroll background on light screens |
| Light card | `#FFFFFF` | Card surface |
| Light purple tint | `#F5ECF7` | Pill and button fill |
| Light border | `#E7D5EC` | Card and pill border |
| Light orange tint | `#FDF5EF` | Orange pill fill |
| Light orange border | `#F9DFCC` | Orange pill border |
| Light text primary | `#1A1225` | Headings on light |
| Light text muted | `#725B78` | Body on light |
| Dark heading on light | `#2D0938` | `COLOR_PURPLE_DARK` (headings and bars) |
| Light shadow | `#450E53` / `#98450B` | 3D bevel shadow for purple / orange |

### 2.4 Drift warning (must read before using a constant)

Several classes define constants named like light tokens but **inherit dark values from `PerazimTheme`**. The comments beside them describe light hex that the code does not produce.

| Constant | Comment says | Actual value |
|---|---|---|
| `PerazimTheme.COLOR_PURPLE_TINT` | `#F5ECF7` | `#33681A7D` (translucent purple) |
| `PerazimTheme.COLOR_ORANGE_TINT` | `#FDF5EF` | `#3DE17D2F` (translucent orange) |
| `PerazimTheme.COLOR_BG_NEUTRAL` | `#FAF7FB` | `#2A0F33` (dark canvas) |
| `PerazimTheme.COLOR_BORDER_GREY` | `#E7D5EC` | `#4A3554` (dark stroke) |
| `PerazimTheme.COLOR_ORANGE_BORDER` | `#F9DFCC` | `#C4894F` (dark orange) |
| `HomeUiBinder.COLOR_*` (copies of the above) | as above | as above |
| `BibleReaderDialog.COLOR_*`, `MainActivity.COLOR_*`, `JokesActivity` etc. (**own** literals) | light hex | light hex (correct) |

**Rule:** If a screen is dark, use `PerazimTheme` tokens. If a screen is light, use the **literal light hex** from section 2.3 in that screen's own constants. Never mix a `PerazimTheme` "light-named" constant into a light screen. Fixing the names is a separate task for Levi to approve, because it changes live screens.

---

## 3. Gradients, canvases and surfaces

### 3.1 Dark gradients (`PerazimTheme` factories)

| Factory | Stops (hex) | Orientation | Radius / stroke | Use |
|---|---|---|---|---|
| `createBackgroundGradient()` | `#2E1038` → `#190720` | top → bottom | none | Tab and screen canvas |
| `createTopBarGradient(hairline)` | `#32123D` → `#220A2B` | top → bottom | bottom hairline `COLOR_STROKE` | Top app bar |
| `createBottomNavGradient(hairline)` | `#260C2E` → `#18061F` | top → bottom | top hairline `COLOR_STROKE` | Bottom tab bar |
| `createCardGradient(r, s)` | `#421950` → `#31113E` | TL → BR | r, stroke `COLOR_STROKE` | Standard card, mini-player, home header |
| `createSurface2Gradient(r, s)` | `#541F63` → `#3E164A` | TL → BR | r, stroke `COLOR_STROKE` | Inner containers, routine box, inputs |
| `createHeroGradient(r, s)` | `#4A1B59` → `#2F0D39` | TL → BR | r, stroke `#583B65` | Hero: Today card, Daily Verse, member identity |
| `createDialogGradient(r, s)` | `#3D154A` → `#24092C` | top → bottom | r, stroke `COLOR_STROKE` | Dialog panels |
| `createActivePillGradient(r)` | `#751E8D` → `#521264` | TL → BR | r, no stroke | Active nav pill, secondary filled button |
| `createMutedPillGradient(r, col, s)` | `#361241` → `#280C31` | top → bottom | r, stroke given | Outline or muted pill |
| `createPrimaryOrangeGradient(r)` | `#E88636` → `#C06119` | TL → BR | r, no stroke | **The one primary CTA** |

### 3.2 Chrome

- **Top app bar:** `createTopBarGradient(dp(1))`. Height is content-driven, padding `16dp` horizontal, `10dp` vertical. Logo `28dp` square with `10dp` end margin. Wordmark "Perazim" `20sp` bold, letter spacing `0.04`, `COLOR_TEXT_PRIMARY`. Right-side profile icon `40dp` box, `8dp` padding, tinted `COLOR_TEXT_PRIMARY`.
- **Bottom tab bar:** `createBottomNavGradient(dp(1))`. Height `58dp` in code (`dimens.xml` says `56dp`; code wins). Five items: Home, Sermons, Worship, Prayer, Give. The Give item is the orange circle.
  - Active tab: `createActivePillGradient(dp(12))`, label and icon `COLOR_TEXT_PRIMARY`.
  - Inactive tab: no background, label and icon `COLOR_PURPLE_MUTED` (`#8B5A9A`).
  - Icon sizes: `18sp` (`bottom_nav_icon_size`), label `11sp` (`bottom_nav_label_size`).
- **Mini-player:** `createCardGradient(dp(12), dp(1))`, sits directly above the bottom bar.

### 3.3 Light canvas (game variant, do not change)

From the approved mockup `game-mockups/lamp_slide/05-play-light.png`, sampled at the corners:

- Top-left `#FBEFF8`, top-right `#F8DBD7` (soft pink), bottom `#E0B1EB` (soft lavender).
- Direction: top to bottom, with the pink wash at the top right.
- **Never change these stops.** Light game screens use this canvas. Light cards on it use `#FFFFFF` with the `#E7D5EC` border.

In the dark screenshots (`dark_01_home.png`) the canvas is the deep purple gradient in 3.1, with a faint warm glow at the lower right. That glow is part of the mockup background, not a token.

---

## 4. Spacing, type, shape and elevation

### 4.1 Spacing (dp)

`PerazimTheme` constants: `SPACING_2XS 2`, `XS 4`, `SM 8`, `MD 12`, `LG 16`, `XL 20`, `2XL 24`, `3XL 32`.

Common layout values in the code:

- Screen gutter: **16dp** each side (`contentContainer.setPadding(dp(16), dp(16), dp(16), dp(32))`).
- Card inner padding: **16dp** (`card_padding`). Dense cards use `12dp`–`14dp`.
- Between cards: **16dp** bottom margin (`createTodayCard`), **10dp** between tiles.
- Icon-to-text gap: **10–12dp**.

### 4.2 Type scale (sp)

`PerazimTheme` constants: `MICRO 9`, `CAPTION 11`, `BODY_SM 12`, `BODY 14`, `SUBTITLE 16`, `TITLE 18`, `HEADLINE 22`, `HERO 26`.

- Wordmark: `20sp` bold.
- Screen heading: `22`–`26sp` bold, `COLOR_TEXT_PRIMARY`.
- Card title: `16`–`18sp` bold.
- Body: `14sp`, line spacing `1.15`.
- Eyebrow label (uppercase, small, letter spacing): `11sp` bold, `COLOR_PURPLE_SOFT`.
- Button text: `14–17sp` bold, all caps for the primary CTA.
- Typeface: system default (`Typeface.DEFAULT` / `DEFAULT_BOLD`). Do not add a custom font without approval.
- Game screens use slightly smaller text than the default (per `game-mockups/index.md`). Answer text may use `19sp`.

### 4.3 Radii (dp)

`PerazimTheme`: `SM 6`, `MD 10`, `LG 14`, `XL 20`, `PILL 999`.

- Cards: **16dp** (`card_corner_radius`) for the main cards; **14dp** for light cards and pills.
- Inner containers and inputs: **10dp**.
- Buttons: **10dp** (primary and 3D), **8dp** (small secondary).
- Pills and chips: **999dp** (`RADIUS_PILL`) or **14dp** in `createPillBg`.

### 4.4 Elevation and bevel

- Cards on dark do **not** use Android elevation. They rely on the gradient and the `COLOR_STROKE` hairline (`card_elevation 4dp` exists in `dimens.xml` but is not the house look).
- 3D buttons (`create3dButtonDrawable`): a solid shadow layer is drawn under the face, offset down by `2–4dp`.
  - Purple: face `#681A7D`, shadow `#450E53`.
  - Orange: face `#E17D2F`, shadow `#98450B`.
- Dark CTAs use the gradient in 3.1 rather than the 3D bevel (for example the Home "Begin 3-minute routine" button).

---

## 5. Components

Each component lists the approved look. Code paths are where to copy from.

### 5.1 Hero card (Home "Today")

- Background: `createHeroGradient(dp(16), dp(1))`, padding `16dp`, bottom margin `16dp`.
- Eyebrow date: `11sp` bold uppercase, `COLOR_PURPLE_SOFT`.
- Step label: `COLOR_ORANGE_SOFT`.
- Title: `COLOR_TEXT_PRIMARY`, bold. Body: `COLOR_TEXT_SECONDARY`, line spacing `1.15`.
- Inner routine box: `createSurface2Gradient(dp(10), dp(1))`, padding `12dp × 10dp`.
- Source: `HomeUiBinder.createTodayCard()` (~401).

### 5.2 Standard card

- Background: `createCardGradient(dp(16), dp(1))`. Padding `16dp`.
- Title `COLOR_TEXT_PRIMARY`, subtitle `COLOR_TEXT_SECONDARY`.
- Tiles with photo: `128dp` high, `14dp` radius, bottom scrim `#B32A0F33`, labels padded `16dp × 14dp`.
- Source: `HomeUiBinder.createPhotoTiles()` (~504), `SermonsUiBinder`.

### 5.3 Light card (legacy and some dialogs)

- Background `#FFFFFF`, border `1dp #E7D5EC`, radius `14dp`, padding `16dp`.
- Title `#1A1225`, body `#725B78`.
- Source: `MainActivity.createCard()` (~3063).

### 5.4 Buttons

| Role | Background | Text | Where |
|---|---|---|---|
| **Primary CTA** (one per screen) | `createPrimaryOrangeGradient(dp(10))` | `COLOR_TEXT_PRIMARY`, bold, `14sp`, min height `56dp` | Home "Begin routine" |
| Secondary filled | `createActivePillGradient(dp(8))` | `COLOR_TEXT_PRIMARY`, bold | Routine "Next" |
| Outline or muted | `createMutedPillGradient(dp(10), #4A3554, dp(1))` | `COLOR_TEXT_PRIMARY` | Filters, quieter actions |
| Icon pill (dark) | `createPillBg` on `COLOR_SURFACE_2`, stroke `COLOR_STROKE` | `COLOR_TEXT_PRIMARY` | Top-bar and bible-reader icon buttons |
| Light 3D button | `create3dButtonDrawable(#681A7D, #450E53, 10, 2–3)` | `#FFFFFF` | Light screens |
| Light 3D orange | `create3dButtonDrawable(#E17D2F, #98450B, 10, 3)` | `#FFFFFF` | Light screens, the single CTA |

Rules:
- **One orange primary per screen.** Everything else is purple.
- Minimum touch target `48dp`. Primary CTAs are `56dp` tall.
- Disabled: `alpha 0.5` (as in `AiBreakthroughGuideDialog`), or text `COLOR_TEXT_DISABLED`.

### 5.5 Pills, chips and tags

- Dark chip: `createPillBg(COLOR_SURFACE_2, COLOR_STROKE)`, text `COLOR_TEXT_PRIMARY`, `RADIUS_PILL`.
- Selected chip: `createActivePillGradient`.
- Light pills: `#F5ECF7` fill, `#E7D5EC` border (purple). `#FDF5EF` fill, `#F9DFCC` border (orange).

### 5.6 Inputs and search

- Background `COLOR_SURFACE_2` (or `createSurface2Gradient`), stroke `COLOR_STROKE`, radius `10dp`, padding `14dp`.
- Text `COLOR_TEXT_PRIMARY` `19sp` in game answer boxes, `16sp` elsewhere. Hint `COLOR_TEXT_SECONDARY`.
- Source: `GamesHubDialog.java` (~165–172).

### 5.7 Dialogs and sheets

- Window background `COLOR_BG_DEEP` (`#2A0F33`), status and nav bar the same colour.
- Panel: `createDialogGradient(dp(16), dp(1))`.
- Scrim: `#99000000` for light modals, `#E62A0F33` for image overlays.
- Title `COLOR_TEXT_PRIMARY`, body `COLOR_TEXT_SECONDARY`.
- Dialog style: `PerazimPlayerDialog` in `styles.xml` for players.

### 5.8 Top bar, bottom bar and mini-player

See 3.2. Do not add a third bar style.

### 5.9 Game screens (Games hub, design mockups)

Source: `presentation/ui/games/GamesHubDialog.java`, and the mockups in `game-mockups/`.

- Canvas: `COLOR_BG_DEEP`, full screen, scroll view on top.
- Page padding `20dp` horizontal, `16dp` top, `28dp` bottom.
- Game card: `COLOR_SURFACE`, padding `16dp × 12dp`, bottom margin `16dp`, radius `14dp`.
- Tile grid: `2dp × 3dp` margins per tile, selected tile alpha `0.4`.
- Primary button: `COLOR_ORANGE_BRAND`, text `COLOR_BG_DEEP`, `17sp`, min height `52dp`, padding `12dp`, radius `14dp`.
- Secondary button: `COLOR_SURFACE_2`, text `COLOR_TEXT_PRIMARY`.
- Light variant (Lamp Slide only, shown in the mockup `05-play-light.png`): canvas from 3.3, controls use the light cards from 5.3. Whether other games get a light variant is open (section 8).
- Verses on game screens are KJV only.

### 5.10 Icons

- Vector icons in `src/main/res/drawable/ic_*.xml`. Nav icons: `ic_nav_home`, `ic_nav_sermons`, `ic_nav_worship`, `ic_nav_prayer`, `ic_nav_me`. Give: `ic_give`. Others: `ic_search`, `ic_plus`, `ic_refresh`, `ic_chevron_right`, `ic_phone`, `ic_location`, `ic_message`, `ic_video_camera`, `ic_smiley`, `ic_flame`, `ic_music_note`.
- Themed Christian icons: `ic_christian_*.xml` (bible, cross, dove, candle, etc.) for doors and games.
- Tint with `setColorFilter(..., SRC_IN)`:
  - Active or on dark: `COLOR_TEXT_PRIMARY`.
  - Inactive or secondary: `COLOR_PURPLE_MUTED` or `COLOR_PURPLE_SOFT`.
  - On orange CTA: `COLOR_TEXT_PRIMARY`.
- Icon size: nav `18sp`, card icons `24–44dp`.

### 5.11 Logo

- `ic_perazim_logo.png` at `28dp` (top bar), `44dp` (Home header). Used on Giving, Community, Profile avatar fallback, Sermons and Fellowship.
- Keep the logo full colour. Do not place it on the orange CTA.

---

## 6. Recipes

### 6.1 Add a new screen (dark)

1. Root: `FrameLayout` or `ScrollView` with `setBackground(PerazimTheme.createBackgroundGradient())`.
2. Column: `LinearLayout` vertical, padding `16dp` sides, `32dp` bottom.
3. Eyebrow → heading → cards. Use `COLOR_PURPLE_SOFT` for eyebrow, `COLOR_TEXT_PRIMARY` for heading.
4. Each card: `createCardGradient(dp(16), dp(1))`, padding `16dp`, margin bottom `16dp`.
5. One orange CTA at most. Use `createPrimaryOrangeGradient(dp(10))`.
6. Check the screen at phone width (`360dp`) with no horizontal scroll.

### 6.2 Add a new card

1. Pick hero (`createHeroGradient`) only for the key card of the screen. Otherwise use `createCardGradient`.
2. Title `16–18sp` bold `COLOR_TEXT_PRIMARY`. Body `14sp` `COLOR_TEXT_SECONDARY`.
3. Inner box: `createSurface2Gradient(dp(10), dp(1))`.

### 6.3 Add a new button

1. Primary (one per screen): `createPrimaryOrangeGradient(dp(10))`, text `COLOR_TEXT_PRIMARY`, min height `56dp`.
2. Secondary: `createActivePillGradient(dp(8))`.
3. Quiet: `createMutedPillGradient(dp(10), COLOR_STROKE, dp(1))`.
4. Add `setTypeface(Typeface.DEFAULT_BOLD)`, padding `12–14dp`.

### 6.4 Add a light screen (only when Levi asks for light)

1. Canvas: `#FAF7FB` (or the 3.3 gradient for game screens).
2. Cards: `#FFFFFF`, stroke `#E7D5EC`, radius `14dp`, padding `16dp`.
3. Text: heading `#2D0938` or `#1A1225`, body `#725B78`.
4. Pills: `#F5ECF7` / `#E7D5EC` for purple, `#FDF5EF` / `#F9DFCC` for orange.
5. Declare the constants in the screen's own class with literal hex. Do not reuse `PerazimTheme` constants (section 2.4).

### 6.5 Add a new icon

1. Put a 24dp vector XML in `src/main/res/drawable/` named `ic_<name>.xml`.
2. Stroke or fill with white (`#FFFFFF`) so the tint decides the colour.
3. Tint in code with `setColorFilter(token, SRC_IN)`.

---

## 7. Do and don't

**Do**
- Use `PerazimTheme` tokens and factories for dark surfaces.
- Keep one orange CTA per screen.
- Use `COLOR_STROKE` hairlines to separate cards, not shadows.
- Keep the logo unchanged in size range and colour.
- Use `16dp` gutters and `16dp` card padding.
- Test on a phone-sized emulator or the phone before shipping (per the app-first rule in project memory).
- Mention in the PR which screens were touched and whether light or dark tokens were used.

**Don't**
- Don't add a new hex literal to a screen file. Add the token first, with Levi's approval.
- Don't use `#8E30A8` or other legacy brights for new work.
- Don't use `COLOR_PURPLE_TINT`, `COLOR_ORANGE_TINT`, `COLOR_BG_NEUTRAL` or `COLOR_BORDER_GREY` from `PerazimTheme` on a light screen (drift warning, section 2.4).
- Don't add a second orange button to a screen.
- Don't change the pink-to-lavender light canvas (3.3).
- Don't change the logo or put it on the orange CTA.
- Don't restyle a finished screen to match a new idea.
- Don't add a custom font, a system dark switch, or new elevation shadows without approval.
- Don't use emoji as icons in new UI. Use the vector set.
- Don't mix Sheng or Swahili copy into UI text without Levi's native check.

---

## 8. Known gaps (for Levi's decision)

1. `PerazimTheme` light-named aliases hold dark values (section 2.4). Fixing them changes live screens, so it needs approval.
2. Light and dark are not tokenised. Legacy classes each define their own light constants.
3. `dimens.xml` says bottom bar `56dp`, code uses `58dp`.
4. There is no `values-night` folder. A system-dark mode would be a new decision.
5. Games: the mockups show a light variant only for Lamp Slide. Whether other games get a light variant is open.
6. `card_elevation 4dp` is defined but not the house look. Confirm whether to remove it.

---

## 9. Sign-off

- Source of truth for code: `PerazimTheme.java`.
- Mockups: `/mnt/project-files/game-mockups/`, `/mnt/project-files/dark_*.png`.
- Approval: **Levi** (sole approver). This document is marked draft until Levi approves it.
