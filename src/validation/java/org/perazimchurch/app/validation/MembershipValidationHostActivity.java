package org.perazimchurch.app.validation;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;

import org.perazimchurch.app.presentation.ui.MembershipDialog;

/** Test-only host. It is packaged into the isolated validation application only. */
public final class MembershipValidationHostActivity extends Activity {
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout root = new LinearLayout(this);
        Button open = new Button(this);
        open.setText("Open membership screen");
        open.setOnClickListener(v -> MembershipDialog.show(this));
        root.addView(open);
        setContentView(root);
    }
}
