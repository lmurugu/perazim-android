# Games integration on the Bible Search baseline

The work is in the original app checkout at `/home/murugu/Android/app`,
on `codex/games-rebase-on-tip`. The branch starts at `91ae4a9`. Keep the
Bible Search branch and its separate worktree read-only for this integration.
When `origin/claude/project-thread-iywd95` is published, replay these commits
on that tip and repeat verification before pushing or opening a draft PR.

## What is in the integration

- The existing Home and Community Games entry points continue to open the
  integrated hub. The 14 games retain their level pickers, difficulty bands,
  saved stars, progress, and result flow. SharedPreferences still uses the
  existing game identifiers and owner prefix.
- Gameplay has 14 individual music loops plus hub music. The actual start of a
  level plays an intro effect. Judged input plays a correct or incorrect cue and
  a matching short voice line. Final reactions are separate. The three sound
  channels remain independently configurable. See [the audio guide](../GAME_AUDIO.md)
  and [the engine guide](../games/ENGINE_USAGE.md).
- Lamp Slide keeps the lamp-lit background and drawn buttons alongside Sound,
  puzzle feedback, and saved level progress. The scene helper accepts a Context.
- Story Mosaic keeps the Bible Search baseline's KJV lookup through
  `BibleVerseReader`. Bible Search and its data layer are outside this diff.

## Branch ancestry checked against 91ae4a9

Ten of the thirteen Games feature tips are already ancestors of the baseline.
The other merge-bases are `9818521` for `feature/game-screens-redesign` and
`51a7952` for both `design/game-mockups` and `research/game-mechanics`.
The integration keeps the two runtime fixes from the redesigned screens branch.
The design and research branches remain reference material.

## Verification and release sequence

The original checkout compiled with zero Java errors on `91ae4a9`.
All 225 targeted game tests passed:

```bash
export ANDROID_HOME=/home/murugu/Android/sdk
./gradlew --offline --no-daemon --max-workers=2 compileValidationJavaWithJavac \
  testValidationUnitTest --tests 'org.perazimchurch.app.domain.games.*' \
  --tests 'org.perazimchurch.app.games.*'
```

Validation versionCode is set to 29, above the connected Infinix's observed 27.
Signing configuration is unchanged. Device acceptance for the combined build
is pending. Before pushing, check the signer against the installed app, update
in place, open all 14 Games screens, try audio, and verify an existing save and
a level completion without clearing the app's data.

The requested Claude branch returned no matching heads on GitHub during
integration. The local commits are ready for the follow-up rebase. No branch
push, draft PR, or phone installation has taken place for this baseline.

The pre-handover Daily Reading prototype is preserved locally at `c41b689`
on `feature/daily-reading-plans`. It will be applied to the separate
`codex/daily-reading` branch after Games is verified and pushed.
