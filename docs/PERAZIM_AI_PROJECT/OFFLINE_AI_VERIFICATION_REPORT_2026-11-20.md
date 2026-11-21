# Offline AI verification report (2026-11-20)

Clear separation of what is shown, and what is not.

| Level | Demonstrated? | Evidence |
|---|---|---|
| Code presence | Yes | `OnDeviceAi.java`, `LocalPrompt.java`, `PerazimAiEngine.java`, `libperazim_llama.so` (arm64 only) |
| Automated host tests | Yes | `org.perazimchurch.app.ai.*` unit tests pass on `feature/offline-qwen-phone` (latest AI commit 661ca7a) |
| Debug APK build | Yes | `assembleDebug` succeeds with `OFFLINE_QWEN_ENABLED=true` in debug only |
| Emulator launch (x86_64) | Yes (app launch only) | App displayed; on-device inference is not possible on x86 (runtime is arm64-only) |
| Offline inference on a device | **No** | Not run |
| Inference without internet | **No** | Not run |
| Correct system instructions supplied | Partly | Code reads: `LocalPrompt.build` output is the `-p` argument. Not checked in a model run |
| Model-load failure handling | Code present; not demonstrated | `IOException` paths return to the callback |
| Fails safely without evidence | Code present; partly tested | Generation requires evidence; citations checked |
| Online AI disabled | Verified by build config | `ONLINE_AI_ENABLED=false` in all variants |
| Resource fit | **Not measured** | Model 491 MB, 2048 context, 3 threads |

**Conclusion:** The offline AI is implemented and has passing host tests. It has not been shown to run offline on a phone. Do not describe it as verified until the runtime plan (`RUNTIME_TEST_PLAN_2026-11-20.md`) is run with owner authorisation.
