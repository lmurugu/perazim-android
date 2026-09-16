package com.example.app.presentation.ui;

import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.app.community.qr.PerazimQrHelper;
import com.example.app.data.local.entity.UserEntity;
import com.example.app.data.local.session.SessionManager;
import com.example.app.data.repository.RepositoryProvider;
import com.example.app.domain.model.Connection;
import com.example.app.domain.repository.ConnectionRepository;
import com.example.app.ui.theme.PerazimTheme;

import java.util.List;

/**
 * Interactive dialog allowing members to share and scan fellowship connection QR codes.
 * <p>
 * Displays the member's profile QR code ("Scan to Connect"), provides a copy connection code action,
 * enables pasting or entering connection tokens to send requests, and displays incoming pending
 * connection requests with "Accept" and "Decline" actions.
 */
public class QrConnectionDialog extends Dialog {

    private final ConnectionRepository connectionRepository;
    private final String userId;
    private final String userName;
    private final String campus;
    private final String payload;

    private LinearLayout pendingContainer;
    private TextView tvPendingEmpty;

    public QrConnectionDialog(@NonNull Context context) {
        this(context, RepositoryProvider.getInstance(context).getConnectionRepository());
    }

    public QrConnectionDialog(@NonNull Context context, @NonNull ConnectionRepository connectionRepository) {
        this(context, connectionRepository, null, null, null);
    }

    public QrConnectionDialog(@NonNull Context context,
                              @NonNull ConnectionRepository connectionRepository,
                              @Nullable String userId,
                              @Nullable String userName,
                              @Nullable String campus) {
        super(context);
        this.connectionRepository = connectionRepository;

        // Resolve user identity
        UserEntity currentUser = null;
        try {
            currentUser = SessionManager.getInstance(context).getCurrentUser();
        } catch (Throwable ignored) {}

        if (userId != null && !userId.trim().isEmpty()) {
            this.userId = userId.trim();
        } else if (currentUser != null && currentUser.getId() != null) {
            this.userId = currentUser.getId();
        } else {
            this.userId = "user_member";
        }

        if (userName != null && !userName.trim().isEmpty()) {
            this.userName = userName.trim();
        } else if (currentUser != null && currentUser.getName() != null && !currentUser.getName().trim().isEmpty()) {
            this.userName = currentUser.getName().trim();
        } else {
            this.userName = "Fellowship Saint";
        }

        if (campus != null && !campus.trim().isEmpty()) {
            this.campus = campus.trim();
        } else if (currentUser != null && currentUser.getCampusId() != null) {
            this.campus = currentUser.getCampusId();
        } else {
            this.campus = "Perazim Central";
        }

        this.payload = PerazimQrHelper.generateConnectionPayload(this.userId, this.userName, this.campus);
    }

    public static QrConnectionDialog show(@NonNull Context context) {
        QrConnectionDialog dialog = new QrConnectionDialog(context);
        dialog.show();
        return dialog;
    }

    public static QrConnectionDialog show(@NonNull Context context, @NonNull ConnectionRepository connectionRepository) {
        QrConnectionDialog dialog = new QrConnectionDialog(context, connectionRepository);
        dialog.show();
        return dialog;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        if (getWindow() != null) {
            getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }

        View contentView = buildLayout();
        setContentView(contentView);

        refreshPendingRequests();
    }

    private View buildLayout() {
        ScrollView scrollView = new ScrollView(getContext());
        scrollView.setFillViewport(true);

        LinearLayout root = new LinearLayout(getContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(16), dp(16), dp(20));

        // Card Container
        LinearLayout card = new LinearLayout(getContext());
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(18), dp(18), dp(18), dp(18));

        GradientDrawable cardBg = new GradientDrawable();
        cardBg.setColor(PerazimTheme.COLOR_WHITE);
        cardBg.setCornerRadius(dp(PerazimTheme.RADIUS_XL));
        cardBg.setStroke(dp(2), PerazimTheme.COLOR_BORDER_GREY);
        card.setBackground(cardBg);

        // 1. TOP HEADER & CLOSE BUTTON
        LinearLayout topHeader = new LinearLayout(getContext());
        topHeader.setOrientation(LinearLayout.HORIZONTAL);
        topHeader.setGravity(Gravity.CENTER_VERTICAL);

        TextView tvBadge = new TextView(getContext());
        tvBadge.setText("🤝 FELLOWSHIP CONNECT");
        tvBadge.setTextSize(TypedValue.COMPLEX_UNIT_SP, PerazimTheme.TEXT_SIZE_CAPTION);
        tvBadge.setTypeface(Typeface.DEFAULT_BOLD);
        tvBadge.setTextColor(PerazimTheme.COLOR_PRIMARY_PURPLE);
        tvBadge.setBackground(createPillDrawable(PerazimTheme.COLOR_PURPLE_TINT, PerazimTheme.COLOR_BORDER_GREY));
        tvBadge.setPadding(dp(10), dp(4), dp(10), dp(4));
        topHeader.addView(tvBadge);

        View spacer = new View(getContext());
        topHeader.addView(spacer, new LinearLayout.LayoutParams(0, 1, 1.0f));

        Button btnClose = new Button(getContext());
        btnClose.setText("✕");
        btnClose.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        btnClose.setTypeface(Typeface.DEFAULT_BOLD);
        btnClose.setTextColor(PerazimTheme.COLOR_PURPLE_DEEP);
        btnClose.setBackground(createPillDrawable(PerazimTheme.COLOR_BG_NEUTRAL, PerazimTheme.COLOR_BORDER_GREY));
        LinearLayout.LayoutParams lpClose = new LinearLayout.LayoutParams(dp(36), dp(34));
        btnClose.setLayoutParams(lpClose);
        btnClose.setOnClickListener(v -> dismiss());
        topHeader.addView(btnClose);

        card.addView(topHeader);

        // Subtitle
        TextView tvSub = new TextView(getContext());
        tvSub.setText("Exchange QR codes to connect in fellowship and 1-on-1 messaging.");
        tvSub.setTextSize(TypedValue.COMPLEX_UNIT_SP, PerazimTheme.TEXT_SIZE_CAPTION);
        tvSub.setTextColor(PerazimTheme.COLOR_TEXT_MUTED);
        tvSub.setPadding(0, dp(8), 0, dp(12));
        card.addView(tvSub);

        // Divider
        card.addView(createDivider());

        // 2. MEMBER PROFILE QR CODE SECTION
        LinearLayout qrSection = new LinearLayout(getContext());
        qrSection.setOrientation(LinearLayout.VERTICAL);
        qrSection.setGravity(Gravity.CENTER_HORIZONTAL);
        qrSection.setPadding(0, dp(10), 0, dp(10));

        TextView tvName = new TextView(getContext());
        tvName.setText("👤 " + userName);
        tvName.setTextSize(TypedValue.COMPLEX_UNIT_SP, PerazimTheme.TEXT_SIZE_TITLE);
        tvName.setTypeface(Typeface.DEFAULT_BOLD);
        tvName.setTextColor(PerazimTheme.COLOR_PURPLE_DEEP);
        tvName.setGravity(Gravity.CENTER);
        qrSection.addView(tvName);

        TextView tvCampus = new TextView(getContext());
        tvCampus.setText("🏛️ " + campus);
        tvCampus.setTextSize(TypedValue.COMPLEX_UNIT_SP, PerazimTheme.TEXT_SIZE_BODY_SM);
        tvCampus.setTextColor(PerazimTheme.COLOR_ACCENT_ORANGE);
        tvCampus.setTypeface(Typeface.DEFAULT_BOLD);
        tvCampus.setGravity(Gravity.CENTER);
        tvCampus.setPadding(0, dp(2), 0, dp(12));
        qrSection.addView(tvCampus);

        // QR Code Bitmap Display
        ImageView ivQrCode = new ImageView(getContext());
        int qrPixelSize = dp(200);
        Bitmap qrBitmap = PerazimQrHelper.generateQrBitmap(
                payload,
                qrPixelSize,
                PerazimTheme.COLOR_PURPLE_DEEP,
                PerazimTheme.COLOR_WHITE
        );
        ivQrCode.setImageBitmap(qrBitmap);

        GradientDrawable qrBorder = new GradientDrawable();
        qrBorder.setColor(PerazimTheme.COLOR_WHITE);
        qrBorder.setCornerRadius(dp(PerazimTheme.RADIUS_MD));
        qrBorder.setStroke(dp(2), PerazimTheme.COLOR_BORDER_GREY);
        ivQrCode.setBackground(qrBorder);
        ivQrCode.setPadding(dp(8), dp(8), dp(8), dp(8));

        LinearLayout.LayoutParams lpQr = new LinearLayout.LayoutParams(qrPixelSize, qrPixelSize);
        lpQr.setMargins(0, 0, 0, dp(8));
        ivQrCode.setLayoutParams(lpQr);
        qrSection.addView(ivQrCode);

        TextView tvScanHint = new TextView(getContext());
        tvScanHint.setText("📷 Scan to Connect");
        tvScanHint.setTextSize(TypedValue.COMPLEX_UNIT_SP, PerazimTheme.TEXT_SIZE_BODY_SM);
        tvScanHint.setTypeface(Typeface.DEFAULT_BOLD);
        tvScanHint.setTextColor(PerazimTheme.COLOR_PRIMARY_PURPLE);
        tvScanHint.setGravity(Gravity.CENTER);
        tvScanHint.setPadding(0, 0, 0, dp(12));
        qrSection.addView(tvScanHint);

        // Copy Connection Code Button
        Button btnCopyCode = new Button(getContext());
        btnCopyCode.setText("📋 Copy Connection Code");
        btnCopyCode.setTextSize(TypedValue.COMPLEX_UNIT_SP, PerazimTheme.TEXT_SIZE_BODY_SM);
        btnCopyCode.setTypeface(Typeface.DEFAULT_BOLD);
        btnCopyCode.setTextColor(PerazimTheme.COLOR_PRIMARY_PURPLE);
        btnCopyCode.setBackground(createPillDrawable(PerazimTheme.COLOR_PURPLE_TINT, PerazimTheme.COLOR_BORDER_GREY));
        btnCopyCode.setPadding(dp(16), dp(8), dp(16), dp(8));
        btnCopyCode.setOnClickListener(v -> copyPayloadToClipboard());

        LinearLayout.LayoutParams lpCopy = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        btnCopyCode.setLayoutParams(lpCopy);
        qrSection.addView(btnCopyCode);

        card.addView(qrSection);

        // Divider
        card.addView(createDivider());

        // 3. ENTER / PASTE MEMBER CODE SECTION
        TextView tvInputTitle = new TextView(getContext());
        tvInputTitle.setText("➕ ENTER / PASTE MEMBER CODE");
        tvInputTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, PerazimTheme.TEXT_SIZE_CAPTION);
        tvInputTitle.setTypeface(Typeface.DEFAULT_BOLD);
        tvInputTitle.setTextColor(PerazimTheme.COLOR_PRIMARY_PURPLE);
        tvInputTitle.setPadding(0, dp(8), 0, dp(6));
        card.addView(tvInputTitle);

        final EditText etCode = new EditText(getContext());
        etCode.setHint("Paste perazim://connect?... or member ID");
        etCode.setTextSize(TypedValue.COMPLEX_UNIT_SP, PerazimTheme.TEXT_SIZE_BODY_SM);
        etCode.setTextColor(PerazimTheme.COLOR_TEXT_DARK);
        etCode.setSingleLine(true);
        etCode.setBackground(createCardBackground(PerazimTheme.COLOR_BG_NEUTRAL, PerazimTheme.RADIUS_MD, PerazimTheme.COLOR_BORDER_GREY, 1));
        etCode.setPadding(dp(12), dp(10), dp(12), dp(10));
        LinearLayout.LayoutParams lpEt = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        lpEt.setMargins(0, 0, 0, dp(8));
        etCode.setLayoutParams(lpEt);
        card.addView(etCode);

        Button btnSendRequest = new Button(getContext());
        btnSendRequest.setText("🚀 Send Connection Request");
        btnSendRequest.setTextSize(TypedValue.COMPLEX_UNIT_SP, PerazimTheme.TEXT_SIZE_BODY_SM);
        btnSendRequest.setTypeface(Typeface.DEFAULT_BOLD);
        btnSendRequest.setTextColor(PerazimTheme.COLOR_WHITE);
        btnSendRequest.setBackground(create3dButtonDrawable(
                PerazimTheme.COLOR_PRIMARY_PURPLE,
                PerazimTheme.COLOR_PURPLE_SHADOW,
                PerazimTheme.RADIUS_MD,
                3
        ));
        btnSendRequest.setPadding(dp(14), dp(10), dp(14), dp(10));
        LinearLayout.LayoutParams lpSendBtn = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        lpSendBtn.setMargins(0, 0, 0, dp(12));
        btnSendRequest.setLayoutParams(lpSendBtn);
        btnSendRequest.setOnClickListener(v -> {
            String rawInput = etCode.getText().toString().trim();
            handleSendConnectionRequest(rawInput, etCode);
        });
        card.addView(btnSendRequest);

        // Divider
        card.addView(createDivider());

        // 4. PENDING INCOMING CONNECTION REQUESTS SECTION
        TextView tvPendingTitle = new TextView(getContext());
        tvPendingTitle.setText("📥 PENDING CONNECTION REQUESTS");
        tvPendingTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, PerazimTheme.TEXT_SIZE_CAPTION);
        tvPendingTitle.setTypeface(Typeface.DEFAULT_BOLD);
        tvPendingTitle.setTextColor(PerazimTheme.COLOR_PRIMARY_PURPLE);
        tvPendingTitle.setPadding(0, dp(8), 0, dp(6));
        card.addView(tvPendingTitle);

        pendingContainer = new LinearLayout(getContext());
        pendingContainer.setOrientation(LinearLayout.VERTICAL);
        pendingContainer.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        tvPendingEmpty = new TextView(getContext());
        tvPendingEmpty.setText("No pending connection requests.");
        tvPendingEmpty.setTextSize(TypedValue.COMPLEX_UNIT_SP, PerazimTheme.TEXT_SIZE_BODY_SM);
        tvPendingEmpty.setTextColor(PerazimTheme.COLOR_TEXT_MUTED);
        tvPendingEmpty.setPadding(0, dp(6), 0, dp(6));
        pendingContainer.addView(tvPendingEmpty);

        card.addView(pendingContainer);

        root.addView(card);
        scrollView.addView(root);
        return scrollView;
    }

    private void handleSendConnectionRequest(String rawInput, EditText etCode) {
        if (TextUtils.isEmpty(rawInput)) {
            Toast.makeText(getContext(), "Please enter or paste a connection code", Toast.LENGTH_SHORT).show();
            return;
        }

        String targetPeerId;
        String targetPeerName;

        PerazimQrHelper.ConnectionPayload parsed = PerazimQrHelper.parseConnectionPayload(rawInput);
        if (parsed != null && !TextUtils.isEmpty(parsed.getUserId())) {
            targetPeerId = parsed.getUserId();
            targetPeerName = !TextUtils.isEmpty(parsed.getName()) ? parsed.getName() : "Member " + targetPeerId;
        } else {
            targetPeerId = rawInput.trim();
            targetPeerName = "Member " + targetPeerId;
        }

        if (userId.equalsIgnoreCase(targetPeerId)) {
            Toast.makeText(getContext(), "You cannot connect with yourself!", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            connectionRepository.sendConnectionRequest(userId, targetPeerId, targetPeerName);
            Toast.makeText(getContext(), "✓ Connection request sent to " + targetPeerName + "!", Toast.LENGTH_LONG).show();
            etCode.setText("");
            refreshPendingRequests();
        } catch (Exception e) {
            Toast.makeText(getContext(), "Failed to send request: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void copyPayloadToClipboard() {
        try {
            ClipboardManager clipboard = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard != null) {
                ClipData clip = ClipData.newPlainText("Perazim Connection Code", payload);
                clipboard.setPrimaryClip(clip);
                Toast.makeText(getContext(), "📋 Connection code copied to clipboard!", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(getContext(), "Failed to copy code", Toast.LENGTH_SHORT).show();
        }
    }

    private void refreshPendingRequests() {
        if (pendingContainer == null) return;

        pendingContainer.removeAllViews();

        List<Connection> requests = null;
        try {
            requests = connectionRepository.getPendingRequests(userId);
        } catch (Exception ignored) {}

        if (requests == null || requests.isEmpty()) {
            if (tvPendingEmpty == null) {
                tvPendingEmpty = new TextView(getContext());
                tvPendingEmpty.setText("No pending connection requests.");
                tvPendingEmpty.setTextSize(TypedValue.COMPLEX_UNIT_SP, PerazimTheme.TEXT_SIZE_BODY_SM);
                tvPendingEmpty.setTextColor(PerazimTheme.COLOR_TEXT_MUTED);
                tvPendingEmpty.setPadding(0, dp(6), 0, dp(6));
            }
            pendingContainer.addView(tvPendingEmpty);
            return;
        }

        for (Connection req : requests) {
            View reqRow = createPendingRequestRow(req);
            pendingContainer.addView(reqRow);
        }
    }

    private View createPendingRequestRow(Connection req) {
        LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(dp(10), dp(10), dp(10), dp(10));
        row.setBackground(createCardBackground(PerazimTheme.COLOR_BG_NEUTRAL, PerazimTheme.RADIUS_MD, PerazimTheme.COLOR_BORDER_GREY, 1));

        LinearLayout.LayoutParams lpRow = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        lpRow.setMargins(0, 0, 0, dp(8));
        row.setLayoutParams(lpRow);

        // Header inside row: Name & PENDING badge
        LinearLayout top = new LinearLayout(getContext());
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);

        String peerDisplayName = req.getPeerName() != null && !req.getPeerName().trim().isEmpty()
                ? req.getPeerName()
                : (req.getRequesterId() != null ? req.getRequesterId() : "Fellow Saint");

        TextView tvPeer = new TextView(getContext());
        tvPeer.setText("🤝 " + peerDisplayName);
        tvPeer.setTextSize(TypedValue.COMPLEX_UNIT_SP, PerazimTheme.TEXT_SIZE_BODY_SM);
        tvPeer.setTypeface(Typeface.DEFAULT_BOLD);
        tvPeer.setTextColor(PerazimTheme.COLOR_PURPLE_DEEP);
        top.addView(tvPeer);

        View sp = new View(getContext());
        top.addView(sp, new LinearLayout.LayoutParams(0, 1, 1.0f));

        TextView tvBadge = new TextView(getContext());
        tvBadge.setText("PENDING");
        tvBadge.setTextSize(TypedValue.COMPLEX_UNIT_SP, PerazimTheme.TEXT_SIZE_MICRO);
        tvBadge.setTypeface(Typeface.DEFAULT_BOLD);
        tvBadge.setTextColor(PerazimTheme.COLOR_ACCENT_ORANGE);
        tvBadge.setBackground(createPillDrawable(PerazimTheme.COLOR_ORANGE_TINT, PerazimTheme.COLOR_ORANGE_BORDER));
        tvBadge.setPadding(dp(6), dp(2), dp(6), dp(2));
        top.addView(tvBadge);

        row.addView(top);

        // Action Buttons: Accept & Decline
        LinearLayout actions = new LinearLayout(getContext());
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setPadding(0, dp(8), 0, 0);

        Button btnAccept = new Button(getContext());
        btnAccept.setText("✓ Accept");
        btnAccept.setTextSize(TypedValue.COMPLEX_UNIT_SP, PerazimTheme.TEXT_SIZE_CAPTION);
        btnAccept.setTypeface(Typeface.DEFAULT_BOLD);
        btnAccept.setTextColor(PerazimTheme.COLOR_WHITE);
        btnAccept.setBackground(create3dButtonDrawable(
                PerazimTheme.COLOR_SUCCESS,
                Color.parseColor("#065F46"),
                PerazimTheme.RADIUS_MD,
                2
        ));
        btnAccept.setPadding(dp(10), dp(4), dp(10), dp(4));
        LinearLayout.LayoutParams lpAcc = new LinearLayout.LayoutParams(0, dp(34), 1.0f);
        lpAcc.setMargins(0, 0, dp(6), 0);
        btnAccept.setLayoutParams(lpAcc);
        btnAccept.setOnClickListener(v -> {
            try {
                connectionRepository.acceptConnection(req.getId());
                Toast.makeText(getContext(), "✓ Connected with " + peerDisplayName + "!", Toast.LENGTH_SHORT).show();
                refreshPendingRequests();
            } catch (Exception e) {
                Toast.makeText(getContext(), "Error accepting connection", Toast.LENGTH_SHORT).show();
            }
        });
        actions.addView(btnAccept);

        Button btnDecline = new Button(getContext());
        btnDecline.setText("✕ Decline");
        btnDecline.setTextSize(TypedValue.COMPLEX_UNIT_SP, PerazimTheme.TEXT_SIZE_CAPTION);
        btnDecline.setTypeface(Typeface.DEFAULT_BOLD);
        btnDecline.setTextColor(PerazimTheme.COLOR_TEXT_MUTED);
        btnDecline.setBackground(createPillDrawable(PerazimTheme.COLOR_WHITE, PerazimTheme.COLOR_BORDER_GREY));
        btnDecline.setPadding(dp(10), dp(4), dp(10), dp(4));
        LinearLayout.LayoutParams lpDec = new LinearLayout.LayoutParams(0, dp(34), 1.0f);
        btnDecline.setLayoutParams(lpDec);
        btnDecline.setOnClickListener(v -> {
            try {
                connectionRepository.rejectConnection(req.getId());
                Toast.makeText(getContext(), "Connection request declined", Toast.LENGTH_SHORT).show();
                refreshPendingRequests();
            } catch (Exception e) {
                Toast.makeText(getContext(), "Error declining connection", Toast.LENGTH_SHORT).show();
            }
        });
        actions.addView(btnDecline);

        row.addView(actions);

        return row;
    }

    private View createDivider() {
        View div = new View(getContext());
        div.setBackgroundColor(PerazimTheme.COLOR_BORDER_GREY);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(1)
        );
        lp.setMargins(0, dp(10), 0, dp(10));
        div.setLayoutParams(lp);
        return div;
    }

    private int dp(int value) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                value,
                getContext().getResources().getDisplayMetrics()
        );
    }

    private Drawable createPillDrawable(int bgColor, int strokeColor) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(bgColor);
        g.setCornerRadius(dp(PerazimTheme.RADIUS_PILL));
        g.setStroke(dp(1), strokeColor);
        return g;
    }

    private Drawable createCardBackground(int bgColor, int cornerRadiusDp, int borderColor, int borderWidthDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(bgColor);
        drawable.setCornerRadius(dp(cornerRadiusDp));
        drawable.setStroke(dp(borderWidthDp), borderColor);
        return drawable;
    }

    private Drawable create3dButtonDrawable(int faceColor, int shadowColor, int radiusDp, int shadowDepthDp) {
        GradientDrawable shadow = new GradientDrawable();
        shadow.setColor(shadowColor);
        shadow.setCornerRadius(dp(radiusDp));

        GradientDrawable face = new GradientDrawable();
        face.setColor(faceColor);
        face.setCornerRadius(dp(radiusDp));

        LayerDrawable layers = new LayerDrawable(new Drawable[]{shadow, face});
        layers.setLayerInset(0, 0, dp(shadowDepthDp), 0, 0);
        layers.setLayerInset(1, 0, 0, 0, dp(shadowDepthDp));
        return layers;
    }
}
