package org.perazimchurch.app.ai;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.perazimchurch.app.MainActivity;
import org.perazimchurch.app.ai.local.OfflineKnowledgeStore;
import org.perazimchurch.app.ai.local.OnDeviceAi;
import org.perazimchurch.app.ai.remote.ConversationHistory;
import org.perazimchurch.app.presentation.ui.AiBreakthroughGuideDialog;

import java.io.File;
import java.lang.reflect.Field;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public final class AiBreakthroughGuideDeviceTest {
    @Test public void guideDialogShowsOfflineQwenAnswerForNamedKjvReference() throws Exception {
        Assume.assumeTrue("Run the interactive generative AI check only in the synthetic validation build",
                "validation".equals(org.perazimchurch.app.BuildConfig.BUILD_TYPE)
                        && org.perazimchurch.app.BuildConfig.OFFLINE_QWEN_ENABLED);
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        File external = context.getExternalFilesDir(null);
        File stagedModel = external == null ? null : new File(external, "qwen-validation.gguf");
        Assume.assumeTrue("The validation Qwen model must be staged on the device",
                stagedModel != null && stagedModel.isFile() && stagedModel.length() == OnDeviceAi.MODEL_BYTES);

        CountDownLatch modelReady = new CountDownLatch(1);
        AtomicReference<String> modelError = new AtomicReference<>();
        OnDeviceAi.download(context, new OnDeviceAi.DownloadCallback() {
            @Override public void onProgress(int percent) { }
            @Override public void onComplete() { modelReady.countDown(); }
            @Override public void onError(String error) { modelError.set(error); modelReady.countDown(); }
        });
        assertTrue("Validation model setup timed out", modelReady.await(60, TimeUnit.SECONDS));
        assertTrue("Validation model setup failed: " + modelError.get(), OnDeviceAi.isInstalled(context));

        Context prefsContext = context;
        android.content.SharedPreferences prefs = prefsContext.getSharedPreferences("perazim_guide_mode", Context.MODE_PRIVATE);
        boolean hadMode = prefs.contains("mode");
        String oldMode = prefs.getString("mode", "guide");
        prefs.edit().putString("mode", "local").commit();
        clearInstalledPackage(context);

        Activity activity = null;
        AiBreakthroughGuideDialog dialog = null;
        try {
            Intent launch = new Intent(context, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            activity = InstrumentationRegistry.getInstrumentation().startActivitySync(launch);
            assertNotNull("Main activity did not launch", activity);
            AtomicReference<AiBreakthroughGuideDialog> dialogRef = new AtomicReference<>();
            Activity host = activity;
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
                AiBreakthroughGuideDialog opened = new AiBreakthroughGuideDialog(host,
                        new AiBreakthroughGuideDialog.AiActionListener() {
                            @Override public void onNavigateTab(int tabIndex) { }
                            @Override public void onOpenGiving() { }
                            @Override public void onOpenLiveSermons() { }
                            @Override public void onOpenBible(String book) { }
                            @Override public void onLaunchRiddles() { }
                            @Override public void onLaunchJokes() { }
                            @Override public void onStartReflections() { }
                        });
                opened.show();
                dialogRef.set(opened);
            });
            dialog = dialogRef.get();
            assertNotNull("Breakthrough Guide dialog did not open", dialog);
            EditText input = field(dialog, "etInput", EditText.class);
            Button send = field(dialog, "btnSend", Button.class);
            LinearLayout messages = field(dialog, "messagesContainer", LinearLayout.class);
            assertNotNull(input);
            assertNotNull(send);
            assertNotNull(messages);

            InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
                input.setText("Explain John 3:16");
                send.performClick();
            });
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(170);
            String visibleText = "";
            while (System.nanoTime() < deadline) {
                AtomicReference<String> text = new AtomicReference<>("");
                InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> appendText(messages, text));
                visibleText = text.get();
                if (visibleText.contains("[Bible@KJV: John 3:16]")) break;
                Thread.sleep(250L);
            }
            assertTrue("Guide dialog did not show a cited Qwen answer. Visible text: " + visibleText,
                    visibleText.contains("[Bible@KJV: John 3:16]"));
            assertTrue("The dialog omitted the Scripture translation title: " + visibleText,
                    visibleText.contains("source: King James Version"));
            assertTrue("The answer was not labeled as local Qwen: " + visibleText,
                    visibleText.contains("On-phone AI · Qwen · Offline"));
        } finally {
            AiBreakthroughGuideDialog opened = dialog;
            Activity host = activity;
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
                if (opened != null && opened.isShowing()) opened.dismiss();
                if (host != null && !host.isFinishing()) host.finish();
            });
            if (hadMode) prefs.edit().putString("mode", oldMode).commit();
            else prefs.edit().remove("mode").commit();
            clearInstalledPackage(context);
        }
    }

    private static <T> T field(Object target, String name, Class<T> type) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return type.cast(field.get(target));
    }

    private static void appendText(View view, StringBuilder text) {
        if (view instanceof TextView) text.append(((TextView) view).getText()).append('\n');
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) appendText(group.getChildAt(i), text);
        }
    }

    private static void appendText(View view, AtomicReference<String> output) {
        StringBuilder text = new StringBuilder(output.get());
        appendText(view, text);
        output.set(text.toString());
    }

    private static void clearInstalledPackage(Context context) {
        new android.util.AtomicFile(new File(context.getFilesDir(), "ai/knowledge/current.json")).delete();
        context.getSharedPreferences("perazim_ai_package_state_v1", Context.MODE_PRIVATE)
                .edit().clear().commit();
        context.getSharedPreferences("perazim_ai_package_update_status_v1", Context.MODE_PRIVATE)
                .edit().clear().commit();
    }
}
