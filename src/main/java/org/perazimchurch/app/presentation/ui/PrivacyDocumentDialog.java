package org.perazimchurch.app.presentation.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.text.TextUtils;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

final class PrivacyDocumentDialog {
    private PrivacyDocumentDialog() {}

    static void show(@NonNull Context context, @NonNull String title, @NonNull String content) {
        TextView text = new TextView(context);
        text.setText(formatMarkdown(content));
        text.setTextSize(16);
        text.setPadding(36, 24, 36, 24);
        text.setTextIsSelectable(true);
        ScrollView scroll = new ScrollView(context);
        scroll.addView(text, new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        new AlertDialog.Builder(context).setTitle(title).setView(scroll).setPositiveButton("Close", null).show();
    }

    @NonNull static String readAsset(@NonNull Context context, @NonNull String path) throws Exception {
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(context.getAssets().open(path), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) content.append(line).append('\n');
        }
        return content.toString();
    }

    @NonNull private static String formatMarkdown(@NonNull String markdown) {
        String cleaned = markdown.replaceAll("(?m)^<!--.*-->\\s*", "")
                .replaceAll("(?m)^#{1,6}\\s*", "")
                .replaceAll("(?m)^[-*]\\s+", "• ")
                .replaceAll("(?m)^\\s*\\d+\\.\\s+", "• ")
                .replace("**", "").replace("`", "");
        return TextUtils.replace(cleaned, new String[]{"\r\n"}, new CharSequence[]{"\n"}).toString().trim();
    }
}
