#!/usr/bin/env bash
# Reproduce the checked-in CPU-only ARM64 runtime from the official llama.cpp release.
set -euo pipefail
cd "$(dirname "$0")/.."
cache=.local-ai-build
archive="$cache/llama-android.tar.gz"
mkdir -p "$cache" src/main/jniLibs/arm64-v8a src/main/assets/licenses
if [[ ! -f "$archive" ]]; then
  curl --fail --location --retry 3 'https://github.com/ggml-org/llama.cpp/releases/download/b12144/llama-b12144-bin-android-arm64.tar.gz' --output "$archive"
fi
printf '%s  %s\n' '8fdb5bb59c66f3f1eecb87804554d801c25227b5d62f7a118ca38cc2a6650704' "$archive" | sha256sum --check -
tar xzf "$archive" -C "$cache"
command -v eu-strip >/dev/null || { echo 'Install elfutils (eu-strip) to strip ARM64 debug symbols.' >&2; exit 1; }
for library in libggml-base.so libggml.so libggml-cpu-android_armv8.0_1.so libllama.so libllama-common.so libllama-completion-impl.so; do
  cp "$cache/llama-b12144/$library" "src/main/jniLibs/arm64-v8a/$library"
  eu-strip "src/main/jniLibs/arm64-v8a/$library"
done
# Android permits executable code only from its installed native-library directory.
cp "$cache/llama-b12144/llama-completion" src/main/jniLibs/arm64-v8a/libperazim_llama.so
eu-strip src/main/jniLibs/arm64-v8a/libperazim_llama.so
cp "$cache/llama-b12144/LICENSE" src/main/assets/licenses/llama.cpp-LICENSE.txt
