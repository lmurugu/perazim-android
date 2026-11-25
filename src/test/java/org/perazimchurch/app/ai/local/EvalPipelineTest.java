package org.perazimchurch.app.ai.local;

import static org.junit.Assume.assumeTrue;

import androidx.test.core.app.ApplicationProvider;

import org.perazimchurch.app.ai.PerazimAiEngine;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLooper;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Evaluation harness, skipped unless the environment provides:
 * PERAZIM_EVAL_CASES (cases.jsonl), PERAZIM_EVAL_MODEL (GGUF), PERAZIM_EVAL_LLAMA (x86 llama-cli),
 * PERAZIM_EVAL_OUT (answers JSONL). It runs each case through the real OnDeviceAi.ask routing.
 * Only the native model call is replaced, via OnDeviceAi.testRunner. Package verification is not touched.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class EvalPipelineTest {

    @Test
    public void runAllCases() throws Exception {
        String casesPath = System.getenv("PERAZIM_EVAL_CASES");
        String modelPath = System.getenv("PERAZIM_EVAL_MODEL");
        String llama = System.getenv("PERAZIM_EVAL_LLAMA");
        String outPath = System.getenv("PERAZIM_EVAL_OUT");
        assumeTrue(casesPath != null && modelPath != null && llama != null && outPath != null);

        Context c = ApplicationProvider.getApplicationContext();
        SoftAndroidKeyStore.install();

        // Install the model file where the app expects it (same bytes, checked by the app's own SHA).
        File model = OnDeviceAi.modelFile(c);
        model.getParentFile().mkdirs();
        if (!model.isFile() || model.length() != OnDeviceAi.MODEL_BYTES) {
            Files.copy(new File(modelPath).toPath(), model.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
        // Presence marker for the native runtime check; the process itself is replaced by the seam below.
        File marker = new File(c.getApplicationInfo().nativeLibraryDir, "libperazim_llama.so");
        boolean createdMarker = false;
        if (marker.getParentFile() != null) marker.getParentFile().mkdirs();
        if (!marker.isFile()) { marker.createNewFile(); createdMarker = true; }

        OnDeviceAi.testRunner = (prompt, grammar, limit) -> {
            try {
                return runLlama(llama, model.getAbsolutePath(), prompt, limit);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new java.io.IOException(e);
            } catch (java.io.IOException e) {
                throw e;
            } catch (Exception e) {
                throw new java.io.IOException(e);
            }
        };

        final boolean removeMarker = createdMarker;
        try {
        List<String> lines = Files.readAllLines(new File(casesPath).toPath(), StandardCharsets.UTF_8);
        try (OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream(outPath), StandardCharsets.UTF_8)) {
            for (String line : lines) {
                if (line.trim().isEmpty()) continue;
                JSONObject kase = new JSONObject(line);
                List<org.perazimchurch.app.ai.remote.ConversationHistory.Turn> history = new ArrayList<>();
                JSONArray turns = kase.optJSONArray("history");
                if (turns != null) {
                    for (int i = 0; i < turns.length(); i++) {
                        JSONArray pair = turns.getJSONArray(i);
                        history.add(new org.perazimchurch.app.ai.remote.ConversationHistory.Turn(
                                pair.getString(0), pair.getString(1)));
                    }
                }
                AtomicReference<String> status = new AtomicReference<>("error");
                AtomicReference<String> text = new AtomicReference<>("");
                CountDownLatch done = new CountDownLatch(1);
                OnDeviceAi.ask(c, kase.getString("prompt"), history, new OnDeviceAi.Callback() {
                    @Override public void onSuccess(String answer) { status.set("answer"); text.set(answer); done.countDown(); }
                    @Override public void onError(String error) { status.set("route-or-error"); text.set(error); done.countDown(); }
                });
                long deadline = System.currentTimeMillis() + 200_000L;
                while (!done.await(50, TimeUnit.MILLISECONDS) && System.currentTimeMillis() < deadline) {
                    ShadowLooper.idleMainLooper();
                }
                JSONObject row = new JSONObject();
                row.put("id", kase.getString("id"));
                row.put("category", kase.getString("category"));
                row.put("severity", kase.getString("severity"));
                row.put("prompt", kase.getString("prompt"));
                row.put("status", done.getCount() == 0 ? status.get() : "timeout");
                row.put("answer", text.get());
                row.put("mode", "full-pipeline-model-seam");
                out.write(row.toString() + "\n");
                out.flush();
            }
        }
        } finally {
            OnDeviceAi.testRunner = null;
            if (removeMarker) marker.delete();
            SoftAndroidKeyStore.uninstall();
        }
    }

    /** Runs the x86 llama-cli, as the app's native runtime would, and returns the reply text only. */
    static String runLlama(String llama, String model, String prompt, int limit) throws Exception {
        // Raw completion, the same as the app's runtime (no chat template applied on top of the ChatML prompt).
        ProcessBuilder pb = new ProcessBuilder(llama, "-m", model, "-p", prompt, "-n", String.valueOf(limit),
                "--temp", "0.2", "--seed", "42", "--no-display-prompt", "--simple-io");
        pb.redirectErrorStream(false);
        pb.redirectInput(new File("/dev/null"));
        Process p = pb.start();
        StringBuilder raw = new StringBuilder();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
            String l;
            while ((l = r.readLine()) != null) raw.append(l).append('\n');
        }
        p.waitFor(150, TimeUnit.SECONDS);
        String out = raw.toString();
        int cut = out.indexOf("\n> EOF");
        if (cut >= 0) out = out.substring(0, cut);
        int cut2 = out.indexOf("[ Prompt:");
        if (cut2 >= 0) out = out.substring(0, cut2);
        return out.trim();
    }

    /**
     * Option 2 (laptop, labelled): routing in the same order as OnDeviceAi.ask, KJV passages from the
     * Bible retriever, the real prompt builder, and the x86 model. The signed knowledge-package check is
     * skipped for this run only, because the test provider cannot satisfy Java's crypto authentication.
     */
    @Test
    public void runOption2() throws Exception {
        String casesPath = System.getenv("PERAZIM_EVAL_CASES");
        String modelPath = System.getenv("PERAZIM_EVAL_MODEL");
        String llama = System.getenv("PERAZIM_EVAL_LLAMA");
        String outPath = System.getenv("PERAZIM_EVAL_OUT2");
        assumeTrue(casesPath != null && modelPath != null && llama != null && outPath != null);
        Context c = ApplicationProvider.getApplicationContext();
        List<String> lines = Files.readAllLines(new File(casesPath).toPath(), StandardCharsets.UTF_8);
        try (OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream(outPath), StandardCharsets.UTF_8)) {
            for (String line : lines) {
                if (line.trim().isEmpty()) continue;
                JSONObject kase = new JSONObject(line);
                String q = kase.getString("prompt");
                String status;
                String answer;
                PreModel.Decision decided = PreModel.decide(c, q);
                if (decided != null) {
                    status = decided.status;
                    answer = decided.answer;
                } else {
                    // PRE-SIGNING ESTIMATE: evidence from the unsigned package built from the approved library files.
                    JSONArray evidence = PreSigningEvidence.retrieve(q);
                    if (evidence.length() == 0) {
                        // Production OnDeviceAi.ask throws here before the model runs, so the model is never called without evidence.
                        status = "refused:no-verified-source";
                        answer = "No verified approved source supports this question.";
                    } else {
                        String prompt = LocalPrompt.build(q, new ArrayList<>(), evidence);
                        long t0 = System.currentTimeMillis();
                        String raw = runLlama(llama, modelPath, prompt, 128);
                        String modelText = LocalPrompt.stripStockPhrases(raw);
                        String grounded = GroundedAnswer.finalAnswer(q, modelText, evidence);
                        String body = GroundedAnswer.body(evidence);
                        status = (grounded != null && grounded.equals(body) ? "grounded:evidence-only" : "grounded:model-framing")
                                + ":" + (System.currentTimeMillis() - t0) + "ms";
                        answer = grounded == null ? body : grounded;
                        // Keep the raw model text for the record, separate from the answer.
                        status = status + "|raw:" + modelText.replace('\n', ' ').substring(0, Math.min(160, modelText.length()));
                    }
                }
                JSONObject row = new JSONObject();
                row.put("id", kase.getString("id"));
                row.put("category", kase.getString("category"));
                row.put("severity", kase.getString("severity"));
                row.put("prompt", q);
                row.put("status", status);
                row.put("answer", answer);
                row.put("mode", "option2-no-package-verification");
                out.write(row.toString() + "\n");
                out.flush();
            }
        }
    }

    /** Exact single-verse KJV evidence from the bundled JSON (SQLCipher is unavailable on the JVM). */
    static JSONArray kjvEvidence(String q) throws Exception {
        JSONArray out = new JSONArray();
        ScriptureReferenceNormalizer.Result ref = ScriptureReferenceNormalizer.normalize(q);
        if (ref == null || ref.normalized == null || !ref.confident) return out;
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("^(.+) (\\d+):(\\d+)$").matcher(ref.normalized);
        if (!m.find()) return out;
        String book = m.group(1);
        int chapter = Integer.parseInt(m.group(2));
        int verse = Integer.parseInt(m.group(3));
        File base = new File("src/main/assets/bible");
        JSONArray manifest = new JSONArray(new String(Files.readAllBytes(new File(base, "kjv_manifest.json").toPath()), StandardCharsets.UTF_8));
        int bookNumber = -1;
        for (int i = 0; i < manifest.length(); i++) {
            if (manifest.getJSONObject(i).getString("name").equalsIgnoreCase(book)) bookNumber = manifest.getJSONObject(i).getInt("bookNumber");
        }
        if (bookNumber < 0) return out;
        JSONArray verses = new JSONArray(new String(Files.readAllBytes(new File(base, "kjv_verses.json").toPath()), StandardCharsets.UTF_8));
        for (int i = 0; i < verses.length(); i++) {
            JSONObject v = verses.getJSONObject(i);
            if (v.getInt("bookNumber") == bookNumber && v.getInt("chapterNumber") == chapter && v.getInt("verseNumber") == verse) {
                JSONObject item = new JSONObject();
                item.put("sourceId", "Bible");
                item.put("title", "King James Version");
                item.put("authorityClass", "SCRIPTURE");
                item.put("version", "KJV");
                item.put("location", book + " " + chapter + ":" + verse);
                item.put("text", v.getString("text"));
                out.put(item);
                break;
            }
        }
        return out;
    }
}
