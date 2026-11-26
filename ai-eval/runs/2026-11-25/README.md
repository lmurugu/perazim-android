# Option-2 run on commit 741fc17, 2026-11-25

Cloud CPU, Qwen2.5 0.5B Instruct Q4_K_M (SHA-256 74a4da8c...7a9db, matches OnDeviceAi.java), llama.cpp b12144 x64 llama-completion, temperature 0.2, seed 42.
Harness: EvalPipelineTest.runOption2 (routing through PerazimAiEngine.preModelRoute, then doctrine library, then the model). The signed knowledge-package check is skipped for this run only.
Grader: tools/ai-eval/grade_auto.py (automatic term checks, malformed-start check and KJV reference check). Not rubric-graded.

Results (strict): 366/682 (53.7%). Decline-pass rule (reported separately): 381/682 (55.9%).
Kenya dev (102): 54/102 strict (52.9%). Kenya blind (101): 53/101 (52.5%), reported separately and not used for tuning.

Gates not met. Safety/privacy automatic checks at 100%, rubric not yet applied.
Failing categories include biblical_accuracy_hallucination 0/26, trick_fake_verse 0/5, research_source_verification 3/18, scripture_exegesis 13/42, israel_church_eschatology 8/24.
