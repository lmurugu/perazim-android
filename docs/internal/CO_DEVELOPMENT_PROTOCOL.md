# Claude and Codex co-development protocol (Perazim Android)

Status: DRAFT for Levi's approval, 2026-11-27. Shared copy: /mnt/project-files/codex-handover/co-development-protocol.md. Build register: /mnt/project-files/codex-handover/build-register.md.

## 1. Source of truth

- **Integration branch:** `codex/community-integration`. It is the base for every build the phone runs until Levi chooses otherwise.
- **HEAD at time of writing:** `19c2b1757abffdb854d61713ceca5ef32afcabdc`.
- **How to discover HEAD:** `git ls-remote origin refs/heads/codex/community-integration`. Record the HEAD built from in the build register.
- **Who writes to it:** Codex only. Claude never pushes to it and never merges into it.
- **Never force-push** the integration branch or any branch the other agent is using.

## 2. Ownership and checkouts

| Area | Owner | Where it lives |
|---|---|---|
| Games, Daily Reading, Community, AI Assistant, Codex's integration work | Codex | Codex's working tree on codex/community-integration |
| Bible search, Sermons fix, AI engine and safety, design rules, theme rename | Claude | Separate clones under the laptop's `Android/worktrees/<name>` |
| Build, unit tests, APK, phone check | The agent that made the change | That agent's own clone |

Rules:
- Neither agent runs `git reset`, `git checkout`, `git stash`, `git clean` or `git stash pop` in the other agent's checkout.
- Untracked files and stashes stay where they are.
- Claude's work stays in clones of origin. Claude does not add worktrees to Codex's repository.
- Read-only commands in the other agent's checkout are fine. Record `git -C <path> status --short | md5sum` before and after reading.

## 3. Branches and handovers

- Claude's work: `claude/*` branches, one workstream per branch. Each opens a **draft** PR against `codex/community-integration` only after its phone check passes.
- Codex's work: `codex/*` branches, created by Codex.
- Handover notes: `/mnt/project-files/codex-handover/` (shared) and `docs/internal/` on the branch that owns the work.
- Design rules: `docs/design/DESIGN_RULES.md` (draft PR #92). Each agent updates the rules on its own branch.
- **Correction to the design rules:** rule 6 ("do not follow the system night mode") does not hold for `codex/community-integration`. `PerazimTheme.applyMode(light)` supports Light, Dark and System, and the mode swaps the colour tokens. The rules must describe that mode switch before they are merged onto that base.

## 4. Consistent builds and versionCode

- Every build includes the latest work from **both** agents on the integration base. Before a build, the author integrates Codex's current HEAD and reports any conflict.
- Every build gets a versionCode above the newest one installed on the phone. The phone is on **37**, so the next build is **38 or higher**.
- Stress test on the PC emulator first. Only a passing build is offered for install.
- **App-first:** a fix is built, installed and seen working in the app before it is committed or pushed.
- AI pull requests stay draft. Claude never merges. Codex merges integration work after review. Levi approves every merge that changes theology, product or release decisions, and every release.

## 5. Integration flow

1. The author starts from the integration HEAD on its own branch.
2. The author builds and runs unit tests. Results go in the build register.
3. The APK is written to `~/perazim-phone-backups/perazim-validation-vNN.apk`.
4. Levi installs it himself and checks the screens.
5. Only after the phone check passes does the author open a draft PR against `codex/community-integration`. The PR records base commit, versionCode, APK path, tests and phone result.
6. Codex reviews and merges. Claude does not merge.
7. Conflicts are resolved on the author's branch. Nothing is reset, rebased or force-pushed on the other agent's branch.

## 6. Build register

See the shared file `build-register.md`. One row per APK with versionCode, base commit, changes, tests and phone check. A build is "phone verified" only when Levi confirms it.

| versionCode | base commit | status |
|---|---|---|
| 36 | codex/community-integration | installed on Levi's phone |
| 37 | 19c2b175 | installed by Levi |
| 38 | 19c2b175 + theme alias rename (uncommitted in Claude's clone) | 789 tests, 0 failed; pending Levi's phone check |

## 7. Status: implemented vs pending

**Implemented**
- Integration base at 19c2b175, with Bible search overviews and the sermons fallback.
- Community: pinned-event area, Inbox, new-post action, My groups and Latest posts (checked on the phone in build 36).
- Theme alias rename: tested and built as v38, pending Levi's phone check.
- Design rules draft (PR #92, docs only).
- Light, Dark and System modes exist in the integration base.

**Pending**
- `GlassPolish.isPageColor` compares `COLOR_BG_DEEP` twice after the rename. Simplify to one comparison in the reconcile commit, after the phone check.
- Light-border fix: light surfaces still use dark strokes. Not started. Needs before and after phone checks.
- Reconcile the rename and design rules onto the current integration HEAD after Levi's phone check. Nothing is pushed to the integration branch before that check.
- Community: full mockup, group permissions, Discover section, prayer audiences, and the Cloudflare API connection for online features.
- Remaining Community privacy and Bible Search acceptance checks.

## 8. Known state in Codex's checkout (preserved, do not touch)

- Untracked files include build logs, `docs/internal/resource-catalog-pilot.md`, `server/migrations/0022_resource_catalog.sql` (must be renumbered before it lands; migrations run to 0027), and `server/src/routes/resources.js`.
- `stash@{0}` on feature/daily-reading-plans holds Community edits. Codex decides when to apply it.

## 9. Limits

- A laptop Remote Control session only receives messages Levi types himself. Claude cannot message it directly, and it stops when the laptop sleeps.
- Claude cannot push to Codex's branch, and cannot see Codex's uncommitted files.
- The laptop cannot reach github.com over SSH (proxy allow-list), so remote checks go through GitHub.
