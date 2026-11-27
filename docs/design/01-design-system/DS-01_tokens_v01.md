# DS-01 tokens, v01

Status: under review (Levi). Nothing here is approved. Agents do not mark it approved.
Made in thread: Build the design system reference.
Sources: prompt v0.1 section 5 (values), codex/community-integration PerazimTheme.java, GlassKit.java, Glass.java (code values).

## Colour, dark mode

| Token | Value (prompt) | Use |
|---|---|---|
| Canvas | #2A0F33 to #160520 vertical, with glows | Page background |
| Glass card | white 15% (#26FFFFFF), rim 18% | Cards, panels |
| Glass control | white 20% (#33FFFFFF), rim 20% | Chips, small round buttons |
| Solid surface | #3D1749 | Opaque dialogs and popups |
| Solid surface 2 | #4F1E5C | Inputs, chips on opaque layers |
| Purple muted | #8B5A9A | Inactive icons, secondary outlines |
| Purple soft | #A67BB3 | Eyebrow labels |
| Purple brand | #681A7D (gradient #8A2BA3 to #681A7D) | Selected pill, secondary button |
| Orange primary | #E17D2F (gradient #E88636 to #C06119) | The one primary button |
| Orange soft | #E8A86A | Pressed or focused orange |
| Text primary | #F5F0F7 | Headings |
| Text secondary | #B9A8C0 | Body, metadata |
| Text body | #E6D8EE | Body copy inside cards |
| Text disabled | #6E5C78 | Disabled text, placeholders |
| Stroke | white 20% (#33FFFFFF) | Hairlines, rims |

## Colour, light mode

| Token | Value (prompt) | Use |
|---|---|---|
| Canvas | #F8F1FB to #EFE0F6 to #E6D2F0 | Page background |
| Glass card | white 60% (#99FFFFFF), purple rim 40% | Cards |
| Glass control | white 80% (#CCFFFFFF) | Chips, round buttons |
| Solid surface | #FFFFFF | Dialogs |
| Solid surface 2 | #F1E4F6 | Inputs |
| Purple muted | #7B5A88 | Inactive icons |
| Purple soft | #7A3E8F | Eyebrow labels |
| Text primary | #2A0F33 | Headings |
| Text secondary | #5E4A68 | Body |
| Text body | #5A3A66 | Body in cards |
| Text disabled | #A793B0 | Disabled |
| Stroke | purple 20% (#33681A7D) | Hairlines |
| Orange primary | same as dark | The one primary button |

## Status colours (both modes)

Success #10B981, error #EF4444. Status messages only.

## Shape, type, spacing

- Radii (dp): SM 6, MD 10, LG 22, XL 28, PILL 999.
- Type (sp): Micro 9, Caption 11 bold uppercase, Body small 12, Body 14, Subtitle 16 bold, Title 18 bold, Headline 22 bold, Hero 26 bold, Wordmark 20 bold (+0.04 letter spacing). System font.
- Spacing (dp): gutter 16, bottom 32, card padding 16 (dense 12 to 14), gap between cards 16, small tiles 10, icon to text 10 to 12, touch target 48, primary button 56.

## Prompt against code (differences, not resolved)

| Token | Prompt | Code | Status |
|---|---|---|---|
| Glass card, dark | white 15% | COLOR_SURFACE #26FFFFFF; GlassKit.glass solid #3D1749, rim #4A3554; Glass frosted #38FFFFFF to #12FFFFFF | Differs |
| Glass card, light | white 60% | COLOR_SURFACE #99FFFFFF; GlassKit.glass #C7FFFFFF to #B3FFFFFF; Glass frosted #E6FFFFFF to #A6FFFFFF | Differs |
| Glass control, dark | white 20% | GlassKit.glassControl #1AFFFFFF (COLOR_SURFACE_2 #33FFFFFF) | Differs |
| Glass control, light | white 80% | COLOR_SURFACE_2 #CCFFFFFF; GlassKit.glassControl #66FFFFFF to #40FFFFFF | Differs |
| Orange gradient | #E88636 to #C06119 | GlassKit and createPrimaryOrangeGradient match; Glass ORANGE uses #F5A05A, #E17D2F, #C8611B | Differs |
| Step text | #E8A86A (dark) | GlassKit.accent #F8C894 dark, #8A4A12 light | Differs |
| Tab bar | Home, Sermons, Worship, Prayer, Give; Give centre | MainActivity: Home, Sermons, Worship, Community, Give (Give last) | Differs |
| Give button | centre circle | ic_give plain tab; createGiveOrb unused | Differs |

## Contrast (WCAG ratio, measured)

| Pair | Ratio | Note |
|---|---|---|
| Text primary on dark canvas | 15.4 to 17.4 | Pass |
| Text secondary #B9A8C0 on dark canvas bottom | 8.8 | Pass |
| Text body #E6D8EE on dark surface | 10.9 | Pass |
| Text disabled #6E5C78 on dark canvas | 2.85 | Disabled exemption; label should still read |
| White on orange top #E88636 | 2.66 | Fails 3:1 |
| White on orange flat #E17D2F | 2.93 | Fails 3:1 |
| White on orange bottom #C06119 | 4.23 | Fails 4.5:1 body; passes 3:1 large text |
| White on purple #681A7D | 10.3 | Pass |
| Text primary #2A0F33 on light canvas | 13.7 | Pass |
| Text secondary #5E4A68 on light canvas | 5.6 | Pass |
| Text disabled #A793B0 on light canvas | 2.24 | Disabled exemption |
| Success #10B981 on white | 2.54 | Fails for text |
| Error #EF4444 on white | 3.76 | Large text only |

## Undecided (prompt section 11, unchanged)

1. Radius scale: code 22 and 28 (recommended) or draft 14 and 16.
2. Light and dark colour aliases: semantic names for new work (recommended); rename in a separate approved change (PR 96 draft).
3. Give centre colour: purple orb as built (recommended).
4. Sermons canvas #1A0A20: media screens only (recommended), decide later.
5. Light variant for other games: dark until Levi approves (recommended).
6. Card elevation 4 dp: leave unused (recommended).
7. Mode switch: design Light and Dark for every screen (recommended).

Found while building (for Levi, no decision taken): white on orange contrast; disabled text; success and error text on light; glass values (prompt vs code); Assistant monochrome logo vs rule 1; tab order and label; focus ring colour (not set; proposed purple soft); Assistant black and white exact values (proposed #000000 and #FFFFFF).
