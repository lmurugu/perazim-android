# Perazim Assistant and exact-topic reading research

This work uses the original app checkout at `/home/murugu/Android/app`.
Branch `codex/assistant-dictation` builds on `codex/daily-reading` and the
available Bible Search baseline `91ae4a9`. The installed validation update
uses versionCode 34 with the existing debug signer.

## Assistant

- The **Perazim Assistant** button on Home sits between the reading
  resume button and **Reading plans**. It opens the full-screen
  `AiBreakthroughGuideDialog`. The chat shell and messages use a monochrome
  light or dark palette. `AssistantMark` draws the existing Perazim globe
  and cross logo as black or white ink, and draws the listening orb.
- **Dictate a message** opens the microphone flow. A listening screen
  shows the speech state, animated orb, and partial transcript. Stop
  returns recognised text to the editable composer. Cancel discards the
  voice turn. Only Send submits text through the existing Guide router.
- `GuideDictation` uses Android on-device speech recognition when present.
  On Android versions with only the configured speech service, the app
  explains that Android may use the internet and asks the user to continue
  each time. Perazim does not store audio. The active recognizer is released
  on completion, cancellation, or leaving the foreground.
- Existing Guide modes, consent, safety responses, knowledge verification,
  AI flags, and release gates are unchanged.

## Exact-topic Daily Reading

In **Home → Reading plans**, type a topic and select **Research this exact
topic**. `ReadingResearch` searches the project's bundled KJV asset at
`assets/bible/text/KJV.tsv.gz` by words or phrases. `BibleAssets.openText` handles the APK packager’s
plain TSV representation and the source tree’s gzip file. It shows verbatim
passages and references before the user starts a plan. Related terms used
by the search do not rename the user's topic. Plans are capped by the
number of passages found and the existing 30-day limit. A query with too
little evidence shows a no-match message. The reflection is an authored
reading prompt linked to the passage.

The search runs locally and does not send the query to a provider. A saved
custom plan contains the exact topic and the selected KJV readings in the
same account-private preferences as existing reading plans. Existing topic
plans and daily controls retain their saved state.

## Build and verification

Run the validation build with the existing pilot API configuration:

```bash
export ANDROID_HOME=/home/murugu/Android/sdk
./gradlew --offline --no-daemon assembleValidation testValidationUnitTest \
  --tests 'org.perazimchurch.app.reading.*' --tests 'org.perazimchurch.app.ai.*' \
  --tests 'org.perazimchurch.app.presentation.ui.GuideDictationTest' \
  --tests 'org.perazimchurch.app.domain.games.*' --tests 'org.perazimchurch.app.games.*'
```

Version 33 installed in place and launched on the Infinix X689C (Android 11).
The phone opened the Assistant from the logo button, returned the KJV text of
John 3:16, and displayed the Android speech-service consent before the
microphone started. The exact-topic search preserved the label `loneliness`
and showed KJV references plus the plan action. No existing plan was replaced
during this check. 657 targeted tests passed and 4 were skipped.
The simulated speech test verified listening, partial results, and the final
editable draft. Live microphone transcription on this device still needs a
spoken check.

[Android SpeechRecognizer reference](https://developer.android.com/reference/android/speech/SpeechRecognizer)

Version 34 moves the Assistant entry to the Home reading card. The phone check
confirmed it sits below the reading button and opens the Assistant. The
bottom bar returns to its five existing navigation entries.
