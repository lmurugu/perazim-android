# Device verification checkpoint

Phone was disconnected before the remaining checks could finish. The owner
will reconnect it later. Do not mark these checks passed or the app complete.

Verified on the phone: distinct verse text for all five Bible translations;
hymn search and lyric display; correct-answer feedback in Bible Quiz and
Who am I; verse tile selection in Verse Builder. Fresh Bible screenshots are
saved alongside this document.

The initial audio import failed because the phone has no OPEN_DOCUMENT audio
handler. The new player now tries GET_CONTENT and a permission-gated, book-filtered
MediaStore fallback for files up to 25 MB. Rebuilt debug and separate validation
APKs succeeded; 64 Android unit tests passed. The fallback build has not yet
been installed and exercised on the phone.

Relevant existing phone recordings found by metadata only: Jude (4,593,383 bytes),
Philemon (3,130,246), 1 Peter (16,878,119), and 2 Peter (10,305,293). No recordings
were copied to the repository or uploaded.

Remaining: install and hash-check latest APK; import/play/pause/seek/stop Jude
and 1 Peter; check saved entries after reopening/backgrounding; clean-input
two-turn on-phone and online AI conversations; Profile/campus and sermon lifecycle
checks; complete the isolated validation app's community walkthrough and
phone-to-browser classroom media check. Preserve the real account and its data.

The separate validation build uses application ID org.perazimchurch.app.validation.
Test fixtures and credentials stay in ignored server/.runtime. Revalidate or
recreate temporary fixtures before resuming. Browser-to-browser video and
backend results are documented in test_validation_2026-11-06.md; they do not
substitute for unfinished phone checks. Public deployment is outside scope.
