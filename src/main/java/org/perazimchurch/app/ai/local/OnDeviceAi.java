package org.perazimchurch.app.ai.local;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import org.perazimchurch.app.ai.remote.ConversationHistory;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.concurrent.*;

/** Executes the packaged official llama.cpp ARM64 binary locally. Inference never opens a socket. */
public final class OnDeviceAi {
    public static final String MODEL_NAME = "Qwen2.5 0.5B Instruct · Q4_K_M";
    public static final long MODEL_BYTES = 491400032L;
    private static final String MODEL_SHA = "74a4da8c9fdbcd15bd1f6d01d621410d31c6fc00986f5eb687824e7b93d7a9db";
    private static final String MODEL_URL = "https://huggingface.co/Qwen/Qwen2.5-0.5B-Instruct-GGUF/resolve/main/qwen2.5-0.5b-instruct-q4_k_m.gguf?download=true";
    private static final ExecutorService WORK = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static volatile Process active;
    private static volatile boolean cancelled;
    private OnDeviceAi() {}
    public interface Callback { void onSuccess(String answer); void onError(String error); }
    public interface DownloadCallback { void onProgress(int percent); void onComplete(); void onError(String error); }
    public static File modelFile(Context c) { return new File(c.getFilesDir(), "ai/qwen2.5-0.5b-instruct-q4_k_m.gguf"); }
    public static boolean isInstalled(Context c) { return modelFile(c).isFile() && modelFile(c).length() == MODEL_BYTES; }
    public static boolean runtimeAvailable(Context c) { return new File(c.getApplicationInfo().nativeLibraryDir, "libperazim_llama.so").isFile(); }
    public static void cancel() { cancelled = true; Process p = active; if (p != null) p.destroy(); }
    public static void ask(Context context, String question, List<ConversationHistory.Turn> history, Callback callback) {
        Context c = context.getApplicationContext();
        String prompt = LocalPrompt.build(question, history);
        WORK.execute(() -> {
            cancelled = false;
            Process process = null;
            try {
                if (!runtimeAvailable(c)) throw new IOException("This build does not include the ARM64 model runtime.");
                if (!isInstalled(c)) throw new IOException("Download the on-device model first.");
                String dir = c.getApplicationInfo().nativeLibraryDir;
                ProcessBuilder builder = new ProcessBuilder(new File(dir, "libperazim_llama.so").getAbsolutePath(),
                        "-m", modelFile(c).getAbsolutePath(), "-p", prompt,
                        "-c", "2048", "-n", "192", "-t", "3",
                        "--no-display-prompt", "--no-conversation", "--simple-io", "--no-escape", "--no-warmup");
                builder.environment().put("LD_LIBRARY_PATH", dir);
                builder.redirectError(new File(c.getCacheDir(), "local-ai-runtime.log"));
                process = builder.start(); active = process;
                final Process running = process;
                ScheduledExecutorService timer = Executors.newSingleThreadScheduledExecutor();
                ScheduledFuture<?> deadline = timer.schedule(running::destroy, 150, TimeUnit.SECONDS);
                StringBuilder answer = new StringBuilder();
                try (Reader reader = new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8)) {
                    char[] buffer = new char[512]; int count;
                    while ((count = reader.read(buffer)) != -1) {
                        if (answer.length() < 16000) answer.append(buffer, 0, count);
                    }
                } finally { deadline.cancel(false); timer.shutdownNow(); }
                int exit = process.waitFor();
                if (cancelled) throw new IOException("Generation cancelled.");
                if (exit != 0) throw new IOException("The local model stopped (code " + exit + "). Try again with a shorter question.");
                String result = answer.toString().replace("<|im_end|>", "").replace("<|endoftext|>", "").trim();
                if (result.isEmpty()) throw new IOException("The local model did not produce an answer.");
                MAIN.post(() -> callback.onSuccess(result));
            } catch (Exception e) { String message = e.getMessage(); MAIN.post(() -> callback.onError(message == null ? "Local model could not run." : message)); }
            finally { if (process != null) process.destroy(); active = null; }
        });
    }
    public static void download(Context context, DownloadCallback callback) {
        Context c = context.getApplicationContext();
        WORK.execute(() -> {
            cancelled = false;
            HttpURLConnection connection = null;
            File target = modelFile(c), part = new File(target.getPath() + ".part");
            try {
                if (!runtimeAvailable(c)) throw new IOException("This phone needs an ARM64 build for on-device AI.");
                File parent = target.getParentFile();
                if (parent != null && !parent.exists() && !parent.mkdirs()) throw new IOException("Cannot create model storage.");
                long offset = part.exists() ? part.length() : 0;
                if (c.getFilesDir().getUsableSpace() < MODEL_BYTES - offset + 64L * 1024 * 1024) throw new IOException("Free at least 560 MB before downloading the model.");
                connection = (HttpURLConnection) new URL(MODEL_URL).openConnection();
                connection.setConnectTimeout(20000); connection.setReadTimeout(30000);
                if (offset > 0) connection.setRequestProperty("Range", "bytes=" + offset + "-");
                int code = connection.getResponseCode();
                if (code != 200 && code != 206) throw new IOException("Model download returned HTTP " + code);
                if (code != 206) offset = 0;
                long total = offset;
                try (InputStream input = connection.getInputStream(); OutputStream output = new FileOutputStream(part, offset > 0)) {
                    byte[] buffer = new byte[65536]; int read, previousPercent = -1;
                    while ((read = input.read(buffer)) != -1) {
                        if (cancelled) throw new IOException("Download paused. Tap Download to resume.");
                        total += read;
                        if (total > MODEL_BYTES) throw new IOException("Unexpected model size.");
                        output.write(buffer, 0, read);
                        int percent = (int) (total * 100 / MODEL_BYTES);
                        if (percent != previousPercent) { previousPercent = percent; MAIN.post(() -> callback.onProgress(percent)); }
                    }
                }
                if (part.length() != MODEL_BYTES || !MODEL_SHA.equals(sha256(part))) { part.delete(); throw new IOException("Model integrity check failed; please download again."); }
                if (!part.renameTo(target)) throw new IOException("Cannot finish model installation.");
                MAIN.post(callback::onComplete);
            } catch (Exception e) { String error = e.getMessage(); MAIN.post(() -> callback.onError(error == null ? "Model download failed." : error)); }
            finally { if (connection != null) connection.disconnect(); }
        });
    }
    private static String sha256(File f) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream in = new FileInputStream(f)) { byte[] b = new byte[65536]; int n; while ((n = in.read(b)) != -1) digest.update(b, 0, n); }
        StringBuilder s = new StringBuilder(); for (byte b : digest.digest()) s.append(String.format(java.util.Locale.ROOT, "%02x", b & 255)); return s.toString();
    }
}
