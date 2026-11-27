# Perazim design folder (v01)

Status: **under review.** Nothing here is approved. Levi is the only approver for design (prompt section 0 and 8.4). Every file is listed in `INDEX.md`.

## What is here

| Path | What |
|---|---|
| `00-prompt/perazim-design-prompt.md` | Reusable design prompt v0.1 (tokens, layout, naming, checklist) |
| `02-screens/NN-slug/NN-slug_MOCK_{dark,light}_v01.html` | Interactive mock-ups, 28 screens, both modes. State switcher (default, loading, empty, error, offline, signed out). Tabs and links open their screens. |
| `02-screens/NN-slug/NN-slug_WIRE_v01.png` | Greyscale wireframe at 360 dp with each element named |
| `assets/ic_perazim_logo.png` | The logo, byte for byte from `codex/community-integration` (`src/main/res/drawable/ic_perazim_logo.png`). Not recoloured. Not used on the orange button. |
| `tools/` | The generator (`lib.js` screen engine, `screens-a.js` to `screens-d.js` screen data, `build.js`, `make-index.js`) |
| `INDEX.md` | One line per file |

REF PNGs (1080 by 2340, dark and light, 140 files in total) are **not in git**. Each is about 1.1 MB and the set would add roughly 60 MB. They are in the project folder at `/mnt/project-files/design/02-screens/NN-slug/`. Regenerate them with `node tools/build.js`.

The upscaled x2 copies are in the project folder at `/mnt/project-files/design/03-upscaled/` (56 screen frames at 2160 by 4680 px, 2 DS-01 sheets at 2160 px wide, and one overview contact sheet per mode). The PNGs are **not in git**. Regenerate them with `node tools/render-upscaled.js`, which renders each REF frame and the DS-01 sheet from the same HTML at 2x the REF pixel size, and rewrites the upscaled rows of `INDEX.md`.

## How they were built

- Tokens are from prompt section 5 (dark and light), glass kinds from 6.1, radii from 7.1, type from 7.2. Light and dark use the same orange and purple brand values.
- Frame: 360 by 780 dp, status bar, 16 dp gutters, floating five-item tab bar with the Give orb in the centre.
- Copy comes from the integration branch where a real string exists (for example "Copy Paybill & account", "Perazim Member Account", "Sign in to see your groups"). Anything else is marked "sample" in the screen notes.
- Scripture is KJV only, shown as Book Chapter:Verse. Every quote was written from the KJV text.
- The Assistant screens (17, 18) are black and white in both modes (prompt 6.11). Screen 18 is treated as part of the Assistant. That reading is mine, so Levi should confirm it.
- No emoji. Icons are inline line icons on a 24 dp grid.
- Checked: no horizontal overflow at 320, 360 and 412 dp on every screen (`build.js` reports it).

## Decisions and findings for Levi (undecided, not settled here)

1. **Contrast, primary button.** White bold 17 sp on the orange `#E17D2F` is 2.93:1, and on the gradient end `#C06119` it is 4.23:1. Both are below AA for 17 sp text (4.5:1). Options: a darker orange stop, 18 sp or larger bold, or keep as is. Recommended: darken the orange stop or raise the label size. A new colour needs Levi's approval (prompt 4.4).
2. **Contrast, inactive tab labels.** Muted purple `#8B5A9A` on the dark canvas is 3.32:1 at 11 sp. Light mode is 5.18:1 and passes. Recommended: use the soft purple `#A67BB3` (5.03:1) for inactive labels on dark.
3. **Fourth tab name.** Plan section 5.1 says Community. The prompt lists Prayer. The mock uses Community.
4. **Give position.** The prompt puts Give in the centre orb, not last. The mock follows the prompt, and the orb stays purple (prompt open item 3).
5. **Light set for games.** Only Lamp Slide has an approved light variant (prompt 6.10). Light files for screens 20, 22 and 23 are drawn so the set is complete, and are marked undecided.
6. **Light game canvas.** Uses the three frozen stops `#FBEFF8`, `#F8DBD7`, `#E0B1EB` as a 160 degree gradient. The exact stop positions were not in the code I read, so check against the approved mock.
7. **Tithe label.** The Give screen keeps the fund name Tithe as built. The doctrine on tithing is undecided (plan 14.10), so the screen does not explain it.
8. **Code conflict, not designed around.** The integration branch has the string "Verse Peek (costs 3 points)", which contradicts the plan rule that help never costs points. Flagged for Codex and Levi.
9. **Reader translations.** The reader mock shows KJV, WEB, ASV and BSB only. ESV, NIV, NKJV and SUV are left out until Levi decides (plan 4.2 and 14.2).
10. **Privacy notice and sign-in.** Shown as a draft, with accept disabled and registration blocked, because the notice is not published (plan 10.4). The privacy contact is a placeholder for Levi to fill.
11. **Paybill number.** A placeholder. The real number comes from app settings and is not written into the design.
12. **Radius scale.** Cards use 22 dp and hero 28 dp (code scale). Game cards use 14 dp (prompt 6.10). Open item 1 in the prompt.

## Not done in this batch

- The design-system sheet DS-01 is in `01-design-system/` as HTML, tokens and SVG. Its PNG references are in the project folder, not git.
- Upscaled x2 copies are produced (see above). They are not in git.
- SVG wireframes are not produced (prompt says "when possible").
- Contrast checks are the ratios above, not a full audit of every text pair on every screen. The accessibility audit should follow.
- Screen 09 (Groups) is not verified on a phone build, as the plan says.

## Rebuild

```
cd docs/design/tools
node build.js            # all 28 screens (REF PNGs go to /mnt/project-files/design)
node build.js 01-home    # one screen
node make-index.js       # refresh INDEX.md rows
```
