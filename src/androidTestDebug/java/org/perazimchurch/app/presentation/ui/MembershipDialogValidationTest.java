package org.perazimchurch.app.presentation.ui;

import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.perazimchurch.app.data.local.session.MembershipSnapshotCache;
import org.perazimchurch.app.validation.MembershipValidationHostActivity;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Synthetic validation-package UI smoke checks. No account or network fixture is used. */
@RunWith(AndroidJUnit4.class)
public class MembershipDialogValidationTest {
    @Test public void guestSeesNeutralSelfServiceScreenAndNoAdministratorOrCandidateDetails() throws Exception {
        android.app.Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        Intent intent = new Intent(instrumentation.getTargetContext(), MembershipValidationHostActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        Activity activity = instrumentation.startActivitySync(intent);
        try {
            Set<String> labels = new HashSet<>();
            instrumentation.runOnMainSync(() -> {
                android.app.AlertDialog dialog = MembershipDialog.open(activity);
                try {
                    collect(dialog.getWindow().getDecorView(), labels);
                } finally {
                    dialog.dismiss();
                }
            });
            assertTrue(labels.contains("Your membership status"));
            assertTrue(labels.contains("Sign in to check your church membership status."));
            assertTrue(labels.contains("Submit a membership request"));
            assertFalse(labels.contains("Administrator tools"));
            for (String label : labels) {
                assertFalse(label.toLowerCase(java.util.Locale.ROOT).contains("candidate count"));
                assertFalse(label.toLowerCase(java.util.Locale.ROOT).contains("matching candidates"));
            }
        } finally {
            instrumentation.runOnMainSync(activity::finish);
        }
    }

    @Test public void deviceSnapshotCacheSeparatesSyntheticAccountsAndServers() {
        MembershipSnapshotCache.clear();
        MembershipSnapshotCache.put("validation-account-a", "HTTPS://EXAMPLE.invalid:443/", "VERIFICATION_PENDING", 1234);
        assertTrue(MembershipSnapshotCache.get("validation-account-a", "https://example.invalid").status.equals("VERIFICATION_PENDING"));
        assertFalse(MembershipSnapshotCache.get("validation-account-b", "https://example.invalid") != null);
        assertFalse(MembershipSnapshotCache.get("validation-account-a", "https://other.invalid") != null);
        MembershipSnapshotCache.clear();
    }

    private static void collect(View view, Set<String> labels) {
        if (view.getVisibility() != View.VISIBLE) return;
        if (view instanceof TextView) labels.add(((TextView) view).getText().toString());
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) collect(group.getChildAt(i), labels);
        }
    }
}
