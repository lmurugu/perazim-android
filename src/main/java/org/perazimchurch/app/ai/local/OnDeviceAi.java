package org.perazimchurch.app.ai.local;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import org.perazimchurch.app.BuildConfig;
import org.perazimchurch.app.ai.PerazimAiEngine;
import org.perazimchurch.app.ai.remote.ConversationHistory;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** Executes the packaged official llama.cpp ARM64 binary locally. Inference never opens a socket. */
public final class OnDeviceAi {
    public static final String MODEL_NAME = "Qwen2.5 0.5B Instruct · Q4_K_M";
    public static final long MODEL_BYTES = 491400032L;
    private static final String MODEL_SHA = "74a4da8c9fdbcd15bd1f6d01d621410d31c6fc00986f5eb687824e7b93d7a9db";
    private static final String MODEL_URL = "https://huggingface.co/Qwen/Qwen2.5-0.5B-Instruct-GGUF/resolve/main/qwen2.5-0.5b-instruct-q4_k_m.gguf?download=true";
    private static final ExecutorService WORK = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static volatile Process active;
    private static volatile HttpURLConnection activeDownload;
    private static volatile boolean cancelled;
    private static final AtomicBoolean busy = new AtomicBoolean(false);
    private static final Object MODEL_STATUS_LOCK = new Object();
    private static String verifiedModelPath;
    private static long verifiedModelLength = -1L;
    private static long verifiedModelModified = -1L;
    private static boolean verifiedModelStatus;
    private OnDeviceAi() {}
    public interface Callback { void onSuccess(String answer); void onError(String error); }
    public interface DownloadCallback { void onProgress(int percent); void onComplete(); void onError(String error); }
    public interface ModelStatusCallback { void onComplete(boolean verified); }
    public static File modelFile(Context c) { return new File(c.getFilesDir(), "ai/qwen2.5-0.5b-instruct-q4_k_m.gguf"); }
    public static boolean isInstalled(Context c) { return modelFile(c).isFile() && modelFile(c).length() == MODEL_BYTES; }
    public static boolean runtimeAvailable(Context c) { return new File(c.getApplicationInfo().nativeLibraryDir, "libperazim_llama.so").isFile(); }

    /** Checks the full model digest on the worker pool; never call this from the UI thread. */
    public static boolean isModelVerified(Context context) throws Exception {
        File model = modelFile(context.getApplicationContext());
        if (!model.isFile() || model.length() != MODEL_BYTES) {
            cacheModelStatus(model, false);
            return false;
        }
        long modified = model.lastModified();
        synchronized (MODEL_STATUS_LOCK) {
            if (model.getAbsolutePath().equals(verifiedModelPath)
                    && verifiedModelLength == MODEL_BYTES && verifiedModelModified == modified) {
                return verifiedModelStatus;
            }
        }
        boolean verified = MODEL_SHA.equals(sha256(model));
        synchronized (MODEL_STATUS_LOCK) {
            if (model.isFile() && model.length() == MODEL_BYTES && model.lastModified() == modified) {
                verifiedModelPath = model.getAbsolutePath();
                verifiedModelLength = MODEL_BYTES;
                verifiedModelModified = modified;
                verifiedModelStatus = verified;
            }
        }
        return verified;
    }

    private static void cacheModelStatus(File model, boolean verified) {
        synchronized (MODEL_STATUS_LOCK) {
            verifiedModelPath = model.getAbsolutePath();
            verifiedModelLength = model.isFile() ? model.length() : -1L;
            verifiedModelModified = model.isFile() ? model.lastModified() : -1L;
            verifiedModelStatus = verified;
        }
    }

    static void invalidateModelVerification() {
        synchronized (MODEL_STATUS_LOCK) {
            verifiedModelPath = null;
            verifiedModelLength = -1L;
            verifiedModelModified = -1L;
            verifiedModelStatus = false;
        }
    }

    public static void checkModel(Context context, ModelStatusCallback callback) {
        Context app = context.getApplicationContext();
        WORK.execute(() -> {
            boolean verified;
            try { verified = isModelVerified(app); }
            catch (Exception ignored) { verified = false; }
            final boolean result = verified;
            MAIN.post(() -> callback.onComplete(result));
        });
    }
    private static void terminate(Process process) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) process.destroyForcibly();
        else process.destroy();
    }

    public static void cancel() {
        cancelled = true;
        Process p = active;
        if (p != null) terminate(p);
        HttpURLConnection connection = activeDownload;
        if (connection != null) connection.disconnect();
    }
    public static void ask(Context context, String question, List<ConversationHistory.Turn> history, Callback callback) {
        ask(context, question, history, OfflineKnowledgeStore.trustedKeys(context), callback);
    }

    public static void ask(Context context, String question, List<ConversationHistory.Turn> history,
                           Map<String, String> trustedKeys, Callback callback) {
        Context c = context.getApplicationContext();
        if (PerazimAiEngine.isCrisisQuery(question)) {
            MAIN.post(() -> callback.onError(PerazimAiEngine.crisisResponse().getMessageText()));
            return;
        }
        if (PerazimAiEngine.isSensitivePastoralDisclosure(question)) {
            MAIN.post(() -> callback.onError(PerazimAiEngine.sensitivePastoralResponse().getMessageText()));
            return;
        }
        if (PerazimAiEngine.isClearlyOutOfScopeQuery(question)) {
            MAIN.post(() -> callback.onError(PerazimAiEngine.outOfScopeResponse().getMessageText()));
            return;
        }
        if (!BuildConfig.OFFLINE_QWEN_ENABLED) {
            MAIN.post(() -> callback.onError("On-device AI is unavailable until approved-source controls are implemented and verified."));
            return;
        }
        if (!busy.compareAndSet(false, true)) {
            MAIN.post(() -> callback.onError("An on-phone AI operation is already running."));
            return;
        }
        cancelled = false;
        WORK.execute(() -> {
            Process process = null;
            try {
                if ("validation".equals(BuildConfig.BUILD_TYPE)
                        && OfflineKnowledgeStore.read(c, trustedKeys, BuildConfig.VERSION_NAME,
                        System.currentTimeMillis()) == null) {
                    OfflineKnowledgeStore.installBundledValidationPackage(c, BuildConfig.VERSION_NAME,
                            System.currentTimeMillis());
                }
                String evidenceQuery = evidenceQuery(question, history);
                JSONArray evidence = OfflineKnowledgeStore.retrieve(c, trustedKeys, BuildConfig.VERSION_NAME,
                        System.currentTimeMillis(), evidenceQuery);
                if (evidence.length() == 0) throw new IOException("No verified approved source supports this question.");
                if (!runtimeAvailable(c)) throw new IOException("This build does not include the ARM64 model runtime.");
                String prompt = LocalPrompt.build(question, history, evidence);
                if (!isInstalled(c)) throw new IOException("Download the on-device model first.");
                if (!MODEL_SHA.equals(sha256(modelFile(c)))) {
                    cacheModelStatus(modelFile(c), false);
                    throw new IOException("The installed model failed its integrity check. Use Model download to repair it.");
                }
                cacheModelStatus(modelFile(c), true);
                if (cancelled) throw new IOException("Generation cancelled.");
                String dir = c.getApplicationInfo().nativeLibraryDir;
                int answerLimit = LocalPrompt.requiresStructuredBiblicalAnswer(question) ? 280 : 120;
                ProcessBuilder builder = new ProcessBuilder(new File(dir, "libperazim_llama.so").getAbsolutePath(),
                        "-m", modelFile(c).getAbsolutePath(), "-p", prompt,
                        "-c", "2048", "-n", "128", "-t", "3", "--seed", "42", "--temp", "0.2",
                        "--grammar", CitationGrammar.build(evidence, answerLimit),
                        "--no-display-prompt", "--no-conversation", "--simple-io", "--no-escape", "--no-warmup");
                builder.environment().put("LD_LIBRARY_PATH", dir);
                process = builder.start(); active = process;
                if (cancelled) throw new IOException("Generation cancelled.");
                final Process running = process;
                Thread stderrDrain = new Thread(() -> {
                    try (InputStream errors = running.getErrorStream()) {
                        byte[] discard = new byte[4096];
                        while (errors.read(discard) != -1) { /* discard diagnostics; never persist prompt-adjacent logs */ }
                    } catch (IOException ignored) { }
                }, "perazim-ai-stderr");
                stderrDrain.setDaemon(true);
                stderrDrain.start();
                ScheduledExecutorService timer = Executors.newSingleThreadScheduledExecutor();
                java.util.concurrent.atomic.AtomicBoolean timedOut = new java.util.concurrent.atomic.AtomicBoolean(false);
                ScheduledFuture<?> deadline = timer.schedule(() -> { timedOut.set(true); terminate(running); }, 150, TimeUnit.SECONDS);
                StringBuilder answer = new StringBuilder();
                try (Reader reader = new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8)) {
                    char[] buffer = new char[512]; int count;
                    while ((count = reader.read(buffer)) != -1) {
                        int available = 16000 - answer.length();
                        if (available > 0) answer.append(buffer, 0, Math.min(available, count));
                    }
                    process.waitFor();
                } finally { deadline.cancel(false); timer.shutdownNow(); }
                int exit = process.exitValue();
                if (cancelled) throw new IOException("Generation cancelled.");
                if (timedOut.get()) throw new IOException("The local model timed out. Try a shorter question.");
                if (exit != 0) throw new IOException("The local model stopped (code " + exit + "). Try again with a shorter question.");
                String result = answer.toString().replace("<|im_end|>", "").replace("<|endoftext|>", "").trim();
                if (result.isEmpty()) throw new IOException("The local model did not produce an answer.");
                if (!hasVerifiedCitations(result, evidence)) {
                    throw new IOException("The local model did not return a verified source citation.");
                }
                String citedResult = appendVerifiedSourceTitles(result, evidence);
                MAIN.post(() -> callback.onSuccess(citedResult));
            } catch (Exception e) {
                String message = cancelled ? "Generation cancelled." : e.getMessage();
                MAIN.post(() -> callback.onError(message == null ? "Local model could not run." : message));
            }
            finally { if (process != null) terminate(process); active = null; busy.set(false); }
        });
    }

    private static String evidenceQuery(String question, List<ConversationHistory.Turn> history) {
        if (!OfflineKnowledgeStore.isUnderspecifiedReferenceQuestion(question) || history == null) return question;
        for (int i = history.size() - 1; i >= 0; i--) {
            ConversationHistory.Turn turn = history.get(i);
            if (turn != null && "user".equals(turn.role)
                    && LocalScriptureRetriever.hasExplicitReference(turn.text)) return turn.text;
        }
        return question;
    }

    private static boolean hasVerifiedCitations(String answer, JSONArray evidence) {
        java.util.regex.Matcher citations = java.util.regex.Pattern
                .compile("\\[([^@\\]]+)@([^:]+): ([^\\]]+)\\]").matcher(answer);
        boolean found = false;
        while (citations.find()) {
            found = true;
            boolean matched = false;
            for (int i = 0; i < evidence.length(); i++) {
                org.json.JSONObject source = evidence.optJSONObject(i);
                if (source != null && citations.group(1).equals(source.optString("sourceId"))
                        && citations.group(2).equals(source.optString("version"))
                        && citations.group(3).equals(source.optString("location"))
                        && (!source.has("citationEligible") || source.optBoolean("citationEligible", false))) matched = true;
            }
            if (!matched) return false;
        }
        return found;
    }

    private static String appendVerifiedSourceTitles(String answer, JSONArray evidence) {
        java.util.regex.Matcher citations = java.util.regex.Pattern
                .compile("\\[([^@\\]]+)@([^:]+): ([^\\]]+)\\]").matcher(answer);
        StringBuffer formatted = new StringBuffer();
        while (citations.find()) {
            String title = "";
            for (int i = 0; i < evidence.length(); i++) {
                org.json.JSONObject source = evidence.optJSONObject(i);
                if (source != null && citations.group(1).equals(source.optString("sourceId"))
                        && citations.group(2).equals(source.optString("version"))
                        && citations.group(3).equals(source.optString("location"))) {
                    title = source.optString("title", "");
                    break;
                }
            }
            title = title.replaceAll("[\\p{Cntrl}\\[\\]{}<>|*`_]+", " ")
                    .replaceAll("\\s+", " ").trim();
            if (title.length() > 120) title = title.substring(0, 120).trim();
            String replacement = citations.group();
            if (!title.isEmpty()) replacement += " — source: " + title;
            citations.appendReplacement(formatted, java.util.regex.Matcher.quoteReplacement(replacement));
        }
        citations.appendTail(formatted);
        return formatted.toString();
    }

    public static void download(Context context, DownloadCallback callback) {
        Context c = context.getApplicationContext();
        if (!BuildConfig.OFFLINE_QWEN_ENABLED) {
            MAIN.post(() -> callback.onError("On-device AI downloads are disabled until approved-source controls are implemented and verified."));
            return;
        }
        if (!busy.compareAndSet(false, true)) {
            MAIN.post(() -> callback.onError("An on-phone AI operation is already running."));
            return;
        }
        cancelled = false;
        WORK.execute(() -> {
            HttpURLConnection connection = null;
            File target = modelFile(c), part = new File(target.getPath() + ".part");
            try {
                if (!runtimeAvailable(c)) throw new IOException("This phone needs an ARM64 build for on-device AI.");
                File parent = target.getParentFile();
                if (parent != null && !parent.exists() && !parent.mkdirs()) throw new IOException("Cannot create model storage.");
                if (isModelVerified(c)) {
                    MAIN.post(() -> callback.onProgress(100));
                    MAIN.post(callback::onComplete);
                    return;
                }
                if (installStagedValidationModel(c, target, part)) {
                    MAIN.post(() -> callback.onProgress(100));
                    MAIN.post(callback::onComplete);
                    return;
                }
                long offset = part.exists() ? part.length() : 0;
                if (offset >= MODEL_BYTES) {
                    if (offset == MODEL_BYTES && MODEL_SHA.equals(sha256(part))) {
                        if (cancelled) throw new IOException("Download paused.");
                        if (!part.renameTo(target)) throw new IOException("Cannot finish model installation.");
                        cacheModelStatus(target, true);
                        MAIN.post(callback::onComplete);
                        return;
                    }
                    if (!part.delete()) throw new IOException("Cannot remove an invalid partial download.");
                    offset = 0;
                }
                if (cancelled) throw new IOException("Download paused.");
                if (c.getFilesDir().getUsableSpace() < MODEL_BYTES - offset + 64L * 1024 * 1024) throw new IOException("Free at least 560 MB before downloading the model.");
                connection = (HttpURLConnection) new URL(MODEL_URL).openConnection();
                activeDownload = connection;
                if (cancelled) throw new IOException("Download paused.");
                connection.setConnectTimeout(20000); connection.setReadTimeout(30000);
                if (offset > 0) connection.setRequestProperty("Range", "bytes=" + offset + "-");
                int code = connection.getResponseCode();
                if (code != 200 && code != 206) throw new IOException("Model download returned HTTP " + code);
                if (code == 206) {
                    String contentRange = connection.getHeaderField("Content-Range");
                    if (contentRange == null || !contentRange.equals("bytes " + offset + "-" + (MODEL_BYTES - 1) + "/" + MODEL_BYTES)) {
                        throw new IOException("The model server returned an invalid resume range.");
                    }
                }
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
                if (cancelled) throw new IOException("Download paused.");
                if (!part.renameTo(target)) throw new IOException("Cannot finish model installation.");
                cacheModelStatus(target, true);
                MAIN.post(callback::onComplete);
            } catch (Exception e) { String error = e.getMessage(); MAIN.post(() -> callback.onError(error == null ? "Model download failed." : error)); }
            finally { if (connection != null) connection.disconnect(); activeDownload = null; busy.set(false); }
        });
    }
    private static String sha256(File f) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream in = new FileInputStream(f)) { byte[] b = new byte[65536]; int n; while ((n = in.read(b)) != -1) digest.update(b, 0, n); }
        StringBuilder s = new StringBuilder(); for (byte b : digest.digest()) s.append(String.format(java.util.Locale.ROOT, "%02x", b & 255)); return s.toString();
    }

    private static boolean installStagedValidationModel(Context context, File target, File partial) throws Exception {
        if (!"validation".equals(BuildConfig.BUILD_TYPE)) return false;
        if (target.isFile() && target.length() == MODEL_BYTES && MODEL_SHA.equals(sha256(target))) {
            cacheModelStatus(target, true);
            return true;
        }
        File external = context.getExternalFilesDir(null);
        File staged = external == null ? null : new File(external, "qwen-validation.gguf");
        if (staged == null || !staged.isFile()) return false;
        if (staged.length() != MODEL_BYTES || !MODEL_SHA.equals(sha256(staged))) {
            throw new IOException("The staged validation model failed its integrity check.");
        }
        try (InputStream input = new FileInputStream(staged); OutputStream output = new FileOutputStream(partial)) {
            byte[] buffer = new byte[65536];
            int count;
            while ((count = input.read(buffer)) != -1) {
                if (cancelled) throw new IOException("Model installation cancelled.");
                output.write(buffer, 0, count);
            }
        }
        if (partial.length() != MODEL_BYTES || !MODEL_SHA.equals(sha256(partial))) {
            partial.delete();
            throw new IOException("The staged validation model failed its integrity check.");
        }
        if (target.exists() && !target.delete()) throw new IOException("Cannot replace the current validation model.");
        if (!partial.renameTo(target)) throw new IOException("Cannot install the staged validation model.");
        cacheModelStatus(target, true);
        return true;
    }
}
