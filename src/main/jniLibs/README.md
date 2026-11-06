# On-phone inference runtime

The checked-in `arm64-v8a` libraries are the CPU-only subset of the official
llama.cpp **b12144** Android ARM64 release, stripped of debug symbols with
elfutils. `libperazim_llama.so` is the unmodified `llama-completion` executable
under an Android native-library name. It runs directly as a child process from
`ApplicationInfo.nativeLibraryDir`, with no HTTP server or network inference.
`useLegacyPackaging` and `android:extractNativeLibs="true"` are required.

Reproduce them with `scripts/prepare-local-ai-runtime.sh`. The script verifies the
upstream archive SHA-256 before extraction. Upstream release:
https://github.com/ggml-org/llama.cpp/releases/tag/b12144
Archive SHA-256: `8fdb5bb59c66f3f1eecb87804554d801c25227b5d62f7a118ca38cc2a6650704`.
The MIT license is included in `assets/licenses/llama.cpp-LICENSE.txt`.

The model is deliberately separate from the APK and Git. The app downloads the
official Qwen2.5-0.5B-Instruct Q4_K_M GGUF (Apache-2.0), 491,400,032 bytes,
and checks SHA-256 `74a4da8c9fdbcd15bd1f6d01d621410d31c6fc00986f5eb687824e7b93d7a9db`
before atomic installation into private app storage. Source:
https://huggingface.co/Qwen/Qwen2.5-0.5B-Instruct-GGUF

Only ARM64 phones support this runtime. Other architectures keep online AI and
built-in app help; the download UI explains when this runtime is unavailable.
