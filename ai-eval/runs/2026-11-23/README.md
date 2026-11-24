# Perazim Guide laptop run, 2026-11-23

Mode: option 2. Routing follows OnDeviceAi.ask order (crisis, sensitive, scope). Model answers use the
x86 llama.cpp `llama-completion` in raw mode with the app's ChatML prompt (no chat template on top).
KJV evidence is exact single-verse only, read from the bundled kjv_verses.json. The signed knowledge-package
check is skipped for this run only; the model answers therefore lack package-only excerpts.
Automatic term checks only (must_include_any, must_not_include, ESV/NIV/NKJV). Rubric scoring is separate.
682 cases: 532 framework and extension cases, plus 150 stress cases. Route counts: model 514, routed 168.
