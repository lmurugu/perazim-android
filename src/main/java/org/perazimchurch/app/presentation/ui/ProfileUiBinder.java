package org.perazimchurch.app.presentation.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;

import org.json.JSONObject;
import org.perazimchurch.app.data.local.PerazimDatabase;
import org.perazimchurch.app.data.local.entity.UserEntity;
import org.perazimchurch.app.data.local.entity.SyncQueueEntity;
import org.perazimchurch.app.data.remote.ApiClient;
import org.perazimchurch.app.data.remote.CommunityRemoteSource;
import org.perazimchurch.app.data.local.preference.GamificationStore;
import org.perazimchurch.app.data.local.session.SessionManager;
import org.perazimchurch.app.domain.model.BibleVerse;
import org.perazimchurch.app.domain.model.DownloadedContent;
import org.perazimchurch.app.domain.model.Hymn;
import org.perazimchurch.app.domain.model.User;
import org.perazimchurch.app.navigation.ChurchIntentManager;
import org.perazimchurch.app.presentation.viewmodel.ProfileViewModel;
import org.perazimchurch.app.presentation.viewmodel.SavedContentData;
import org.perazimchurch.app.sync.SheetSyncManager;
import org.perazimchurch.app.sync.SyncDispatcher;
import org.perazimchurch.app.sync.SyncQueueManager;
import org.perazimchurch.app.ui.theme.PerazimTheme;

import java.io.InputStream;
import java.io.File;
import java.io.ByteArrayOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * UI Binder responsible for orchestrating the Profile tab.
 * <p>
 * Binds directly to {@link ProfileViewModel} and manages:
 * <ul>
 *     <li>Member Profile Header (active user name, email, role, and avatar).</li>
 *     <li>Spiritual Journey Card (streak counter, streak freeze toggle, spiritual XP progress bar, Grace Points).</li>
 *     <li>Saved Content Manager with tabbed/expandable view for Downloaded Sermons, Favorite Hymns, and Bookmarked Scripture Verses.</li>
 * </ul>
 */
public class ProfileUiBinder {

    private static final int REQUEST_AVATAR_GALLERY = 7411;
    private static final int REQUEST_AVATAR_CAMERA = 7412;

    private final Context context;
    private final Activity activity;
    private final ProfileViewModel viewModel;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    // Active User UI references
    private TextView tvMemberName;
    private TextView tvMemberEmail;
    private TextView tvCovenantBadge;
    private ImageView ivAvatar;
    private Uri cameraPhotoUri;
    private File cameraPhotoFile;
    private String displayedAvatarRef = "";

    // Spiritual Journey UI references
    private TextView tvStreakValue;
    private TextView tvStreakStatus;
    private Button btnStreakFreezeToggle;
    private TextView tvXpValue;
    private TextView tvGraceValue;
    private ProgressBar xpProgressBar;
    private TextView tvXpLevelLabel;

    // Stats & Badges UI references
    private TextView tvLifetimeDaysValue;
    private TextView tvBadgesCountValue;
    private TextView tvBadgeFirstRoutine;
    private TextView tvBadgeStreak7;

    // Current State Cache
    private User currentUser;
    private SavedContentData currentSavedContent;
    private boolean isStreakFrozen = false;

    // Saved Content Manager UI references
    private LinearLayout savedContentTabRow;
    private LinearLayout savedContentListContainer;
    private Button btnTabSermons;
    private Button btnTabHymns;
    private Button btnTabVerses;
    private int selectedSavedContentTab = 0; // 0 = Sermons, 1 = Hymns, 2 = Verses

    // Content Synchronization UI reference
    private TextView tvSyncStatusVal;
    private Button btnAuthAction;

    public ProfileUiBinder(@NonNull Activity activity, @NonNull ProfileViewModel viewModel) {
        this.activity = activity;
        this.context = activity;
        this.viewModel = viewModel;
    }

    public ProfileUiBinder(@NonNull Context context, @NonNull ProfileViewModel viewModel) {
        this.context = context;
        this.activity = context instanceof Activity ? (Activity) context : null;
        this.viewModel = viewModel;
    }

    /**
     * Builds and returns the complete ScrollView representing the Profile Screen.
     */
    @NonNull
    public ScrollView buildView() {
        ScrollView scrollView = new ScrollView(context);
        scrollView.setFillViewport(true);
        scrollView.setBackground(PerazimTheme.createBackgroundGradient());

        LinearLayout content = new LinearLayout(context);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(16), dp(16), dp(32));

        bind(content);

        scrollView.addView(content);
        return scrollView;
    }

    /**
     * Builds and binds the complete Profile Tab view.
     */
    @NonNull
    public ScrollView bind() {
        return buildView();
    }

    /**
     * Binds the Profile UI components into the provided root container.
     */
    public void bind(@NonNull ViewGroup rootContainer) {
        // 1. Member Profile Header Card
        View headerCard = createProfileHeaderCard();
        rootContainer.addView(headerCard);

        // 2. Stats Row Card (Quiet Numbers)
        View statsCard = createStatsRowCard();
        rootContainer.addView(statsCard);

        // 3. Single Primary Action: Give with M-Pesa (Solid Orange)
        View giveCard = createGiveButtonCard();
        rootContainer.addView(giveCard);

        // 4. Settings / Deeper Options List (Card with Chevrons)
        View optionsCard = createOptionsListCard();
        rootContainer.addView(optionsCard);

        // Initial Data Load
        refreshProfile();
        refreshSavedContent();
    }

    // =========================================================================
    // 1. MEMBER PROFILE HEADER
    // =========================================================================

    private View createProfileHeaderCard() {
        FrameLayout rootFrame = new FrameLayout(context);
        LinearLayout.LayoutParams lpRoot = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(140));
        lpRoot.setMargins(0, 0, 0, dp(12));
        rootFrame.setLayoutParams(lpRoot);
        rootFrame.setBackground(PerazimTheme.createHeroGradient(dp(PerazimTheme.RADIUS_LG), dp(1)));
        rootFrame.setClipToOutline(true);

        Bitmap coverBmp = loadAssetBitmap("backgrounds/bg_me_path.png", 720);
        if (coverBmp != null) {
            ImageView ivCover = new ImageView(context);
            ivCover.setImageBitmap(coverBmp);
            ivCover.setScaleType(ImageView.ScaleType.CENTER_CROP);
            rootFrame.addView(ivCover, new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

            View bottomScrim = new View(context);
            GradientDrawable scrimGrad = new GradientDrawable(
                    GradientDrawable.Orientation.TOP_BOTTOM,
                    new int[]{Color.TRANSPARENT, Color.parseColor("#2A0F33")});
            bottomScrim.setBackground(scrimGrad);
            rootFrame.addView(bottomScrim, new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        }

        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT));
        card.setPadding(dp(14), dp(14), dp(14), dp(14));

        // Member Avatar with Church Logo
        ivAvatar = new ImageView(context);
        ivAvatar.setScaleType(ImageView.ScaleType.CENTER_CROP);
        GradientDrawable avatarBackground = new GradientDrawable();
        avatarBackground.setShape(GradientDrawable.OVAL);
        avatarBackground.setColor(Color.WHITE);
        ivAvatar.setBackground(avatarBackground);
        ivAvatar.setClipToOutline(true);
        ivAvatar.setOnClickListener(v -> showAvatarPicker());
        ivAvatar.setContentDescription("Profile picture. Tap to change.");
        LinearLayout.LayoutParams lpAv = new LinearLayout.LayoutParams(dp(54), dp(54));
        lpAv.setMargins(0, 0, dp(14), 0);
        ivAvatar.setLayoutParams(lpAv);
        card.addView(ivAvatar);

        // Member Text Info
        LinearLayout memberInfo = new LinearLayout(context);
        memberInfo.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lpInfo = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        memberInfo.setLayoutParams(lpInfo);

        tvMemberName = new TextView(context);
        tvMemberName.setText("Perazim Covenant Member");
        tvMemberName.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        tvMemberName.setTypeface(Typeface.DEFAULT_BOLD);
        tvMemberName.setTextColor(PerazimTheme.COLOR_TEXT_PRIMARY);
        memberInfo.addView(tvMemberName);

        tvMemberEmail = new TextView(context);
        tvMemberEmail.setText("member@perazimchurch.org");
        tvMemberEmail.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        tvMemberEmail.setTextColor(PerazimTheme.COLOR_TEXT_SECONDARY);
        tvMemberEmail.setPadding(0, dp(2), 0, dp(4));
        memberInfo.addView(tvMemberEmail);


        // Auth Action Button (Sign In / Register or Log Out)
        btnAuthAction = new Button(context);
        btnAuthAction.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        btnAuthAction.setTypeface(Typeface.DEFAULT_BOLD);
        btnAuthAction.setPadding(dp(10), dp(2), dp(10), dp(2));
        LinearLayout.LayoutParams lpAuth = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, dp(32));
        lpAuth.topMargin = dp(6);
        btnAuthAction.setLayoutParams(lpAuth);
        updateAuthButtonState();
        memberInfo.addView(btnAuthAction);

        card.addView(memberInfo);
        rootFrame.addView(card);
        return rootFrame;
    }

    private void updateAuthButtonState() {
        if (btnAuthAction == null) return;
        boolean isGuest = SessionManager.getInstance(context).isGuest();
        if (isGuest) {
            btnAuthAction.setText("🔑 Sign In / Register");
            btnAuthAction.setTextColor(PerazimTheme.COLOR_ORANGE_SOFT);
            btnAuthAction.setBackground(PerazimTheme.createCardGradient(dp(8), dp(1)));
            btnAuthAction.setOnClickListener(v -> AuthDialog.show(context, user -> refreshProfile()));
        } else {
            btnAuthAction.setText("Log Out");
            btnAuthAction.setTextColor(PerazimTheme.COLOR_TEXT_SECONDARY);
            btnAuthAction.setBackgroundColor(Color.TRANSPARENT);
            btnAuthAction.setOnClickListener(v -> showLogoutConfirmationDialog());
        }
    }

    private void showLogoutConfirmationDialog() {
        new AlertDialog.Builder(context)
                .setTitle("Log Out")
                .setMessage("Are you sure you want to sign out of your Perazim account? Your offline prayers and readings will remain saved on this device in Guest Mode.")
                .setPositiveButton("Log Out", (dialog, which) -> {
                    SessionManager.getInstance(context).logout();
                    Toast.makeText(context, "Logged out. Switched to Guest Mode.", Toast.LENGTH_SHORT).show();
                    refreshProfile();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // =========================================================================
    // 2. STATS ROW (Quiet numbers, no neon pills)
    // =========================================================================

    private View createStatsRowCard() {
        LinearLayout card = createCard(PerazimTheme.COLOR_SURFACE, PerazimTheme.RADIUS_LG, PerazimTheme.COLOR_STROKE, 1);
        card.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lpCard = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lpCard.setMargins(0, 0, 0, dp(12));
        card.setLayoutParams(lpCard);
        card.setPadding(dp(12), dp(12), dp(12), dp(12));

        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        String uid = resolveGamificationUserId();
        int currentStreak = GamificationStore.getStreak(context, uid, 0);
        int currentXp = GamificationStore.getSpiritualXp(context, uid, 0);
        int currentGrace = GamificationStore.getGracePoints(context, uid, 0);
        int lifetimeDays = GamificationStore.getLifetimeRoutineDays(context, uid, 0);

        LinearLayout c1 = createStatColumn(currentStreak + "d", "Streak");
        tvStreakValue = (TextView) c1.getChildAt(0);
        row.addView(c1);

        row.addView(createVerticalDivider());

        LinearLayout c2 = createStatColumn(String.valueOf(currentXp), "Spiritual XP");
        tvXpValue = (TextView) c2.getChildAt(0);
        row.addView(c2);

        row.addView(createVerticalDivider());

        LinearLayout c3 = createStatColumn(String.valueOf(currentGrace), "Grace");
        tvGraceValue = (TextView) c3.getChildAt(0);
        row.addView(c3);

        row.addView(createVerticalDivider());

        LinearLayout c4 = createStatColumn(lifetimeDays + "d", "Lifetime");
        tvLifetimeDaysValue = (TextView) c4.getChildAt(0);
        row.addView(c4);

        card.addView(row);
        return card;
    }

    private View createVerticalDivider() {
        View v = new View(context);
        v.setBackgroundColor(PerazimTheme.COLOR_STROKE);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(1), dp(24));
        v.setLayoutParams(lp);
        return v;
    }

    private LinearLayout createStatColumn(String value, String label) {
        LinearLayout col = new LinearLayout(context);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        col.setLayoutParams(lp);

        TextView tvVal = new TextView(context);
        tvVal.setText(value);
        tvVal.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
        tvVal.setTypeface(Typeface.DEFAULT_BOLD);
        tvVal.setTextColor(PerazimTheme.COLOR_TEXT_PRIMARY);
        col.addView(tvVal);

        TextView tvLbl = new TextView(context);
        tvLbl.setText(label);
        tvLbl.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        tvLbl.setTextColor(PerazimTheme.COLOR_PURPLE_SOFT);
        tvLbl.setPadding(0, dp(2), 0, 0);
        col.addView(tvLbl);

        return col;
    }

    // =========================================================================
    // 3. SINGLE PRIMARY ACTION: GIVE WITH M-PESA
    // =========================================================================

    private View createGiveButtonCard() {
        Button btnGive = new Button(context);
        btnGive.setText("Give with M-Pesa");
        btnGive.setTextColor(PerazimTheme.COLOR_TEXT_PRIMARY);
        btnGive.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        btnGive.setTypeface(Typeface.DEFAULT_BOLD);
        btnGive.setBackground(PerazimTheme.createPrimaryOrangeGradient(dp(12)));
        btnGive.setPadding(dp(14), dp(12), dp(14), dp(12));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(14));
        btnGive.setLayoutParams(lp);
        btnGive.setOnClickListener(v -> GivingDialog.show(context));
        return btnGive;
    }

    // =========================================================================
    // 4. SETTINGS & DEEPER LIST (Quiet card with chevrons)
    // =========================================================================

    private View createOptionsListCard() {
        LinearLayout card = createCard(PerazimTheme.COLOR_SURFACE, PerazimTheme.RADIUS_LG, PerazimTheme.COLOR_STROKE, 1);
        card.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(14));
        card.setLayoutParams(lp);
        card.setPadding(dp(4), dp(8), dp(4), dp(8));

        TextView tvHeader = new TextView(context);
        tvHeader.setText("CHURCH & RESOURCES");
        tvHeader.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        tvHeader.setTypeface(Typeface.DEFAULT_BOLD);
        tvHeader.setTextColor(PerazimTheme.COLOR_PURPLE_SOFT);
        tvHeader.setLetterSpacing(0.04f);
        tvHeader.setPadding(dp(12), dp(4), 0, dp(8));
        // Row 0: Member Account Authentication
        String authSummary = SessionManager.getInstance(context).isGuest()
                ? "Offline Guest Mode · Tap to Sign In or Register"
                : "Signed in · Tap to manage account";
        card.addView(createOptionRow(
                org.perazimchurch.app.R.drawable.ic_christian_church,
                "Member Account",
                authSummary,
                v -> AuthDialog.show(context, user -> refreshProfile())
        ));
        card.addView(createHorizontalDivider());
        card.addView(createOptionRow(
                org.perazimchurch.app.R.drawable.ic_phone,
                "Change account phone number",
                "Unavailable · your current number and membership link stay unchanged",
                v -> new AlertDialog.Builder(context)
                        .setTitle("Phone number change unavailable")
                        .setMessage("Phone number changes are currently unavailable. Your current account number and any membership link are unchanged.")
                        .setPositiveButton("Close", null)
                        .show()
        ));
        card.addView(createHorizontalDivider());

        card.addView(createOptionRow(
                org.perazimchurch.app.R.drawable.ic_christian_church,
                "Church Membership",
                "Check your status or submit a private request",
                v -> MembershipDialog.show(context)
        ));
        card.addView(createHorizontalDivider());

        // Row 1: Refresh Content
        card.addView(createOptionRow(
                org.perazimchurch.app.R.drawable.ic_refresh,
                "Refresh Content",
                SheetSyncManager.getLastSyncStatusText(context),
                v -> triggerContentSync()
        ));
        card.addView(createHorizontalDivider());

        card.addView(createOptionRow(
                org.perazimchurch.app.R.drawable.ic_refresh,
                "Failed offline updates",
                "Review saved updates and tap one to retry",
                v -> showFailedSyncItems()
        ));
        card.addView(createHorizontalDivider());

        // Row 2: Saved / Offline Content
        card.addView(createOptionRow(
                org.perazimchurch.app.R.drawable.ic_christian_bible,
                "Saved & Offline Content",
                "Downloaded sermons, favorite hymns, bookmarked verses",
                v -> showSavedContentDialog()
        ));
        card.addView(createHorizontalDivider());

        // Row 3: Tithes & Stewardship
        card.addView(createOptionRow(
                org.perazimchurch.app.R.drawable.ic_christian_wheat,
                "Tithe & Stewardship",
                "Support ministry, worship, and missions via M-Pesa",
                v -> GivingDialog.show(context)
        ));
        card.addView(createHorizontalDivider());

        // Row 4: Breakthrough Guide
        card.addView(createOptionRow(
                org.perazimchurch.app.R.drawable.ic_christian_candle,
                "Breakthrough Guide",
                "Contemplative spiritual companion & scriptures",
                v -> showBreakthroughGuideDialog()
        ));
        card.addView(createHorizontalDivider());

        // Row 5: Fellowship Extras (Riddles & Jokes)
        card.addView(createOptionRow(
                org.perazimchurch.app.R.drawable.ic_christian_praying_hands,
                "Fellowship Extras",
                "Devotional riddles and Christian joy & humor",
                v -> showFellowshipExtrasDialog()
        ));
        card.addView(createHorizontalDivider());

        // Row 6: Pastoral Care
        card.addView(createOptionRow(
                org.perazimchurch.app.R.drawable.ic_phone,
                "Pastoral Care & Office",
                "Call, WhatsApp, SMS, or directions to the church",
                v -> showPastoralCareDialog()
        ));
        card.addView(createHorizontalDivider());

        // Row 7: About Perazim
        card.addView(createOptionRow(
                org.perazimchurch.app.R.drawable.ic_christian_church,
                "About Perazim Mission Church",
                "Vision, leadership & version 2.2.0 (Build 4)",
                v -> showAboutDialog()
        ));

        return card;
    }

    private void showFailedSyncItems() {
        new Thread(() -> {
            List<SyncQueueEntity> failed = PerazimDatabase.getInstance(context).syncQueueDao().getFailedItems();
            mainHandler.post(() -> {
                if (failed == null || failed.isEmpty()) {
                    new AlertDialog.Builder(context).setTitle("Failed offline updates")
                            .setMessage("There are no failed offline updates.")
                            .setPositiveButton("Close", null).show();
                    return;
                }
                String[] labels = new String[failed.size()];
                for (int i = 0; i < failed.size(); i++) {
                    SyncQueueEntity item = failed.get(i);
                    String detail = item.getLastError() == null ? "Needs attention" : item.getLastError();
                    labels[i] = item.getEntityType() + " · " + item.getEntityId() + "\n" + detail + "\nTap to retry";
                }
                new AlertDialog.Builder(context).setTitle("Failed offline updates")
                        .setItems(labels, (dialog, which) -> {
                            SyncQueueEntity item = failed.get(which);
                            new Thread(() -> {
                                SyncQueueManager.getInstance(context).retry(item.getId());
                                mainHandler.post(() -> {
                                    SyncDispatcher.dispatchSync(context, null);
                                    Toast.makeText(context, "Retry queued", Toast.LENGTH_SHORT).show();
                                });
                            }, "retry-sync-item").start();
                        }).setNegativeButton("Close", null).show();
            });
        }, "failed-sync-list").start();
    }

    private View createHorizontalDivider() {
        View v = new View(context);
        v.setBackgroundColor(PerazimTheme.COLOR_STROKE);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(1));
        lp.setMargins(dp(12), 0, dp(12), 0);
        v.setLayoutParams(lp);
        return v;
    }

    private View createOptionRow(int iconRes, String title, String subtitle, View.OnClickListener onClickListener) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(12), dp(10), dp(12), dp(10));

        ImageView icon = new ImageView(context);
        icon.setImageResource(iconRes);
        icon.setColorFilter(PerazimTheme.COLOR_PURPLE_SOFT, PorterDuff.Mode.SRC_IN);
        LinearLayout.LayoutParams lpIcon = new LinearLayout.LayoutParams(dp(20), dp(20));
        lpIcon.setMargins(0, 0, dp(12), 0);
        icon.setLayoutParams(lpIcon);
        row.addView(icon);

        LinearLayout textCol = new LinearLayout(context);
        textCol.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lpText = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        textCol.setLayoutParams(lpText);

        TextView tvTitle = new TextView(context);
        tvTitle.setText(title);
        tvTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        tvTitle.setTypeface(Typeface.DEFAULT_BOLD);
        tvTitle.setTextColor(PerazimTheme.COLOR_TEXT_PRIMARY);
        textCol.addView(tvTitle);

        TextView tvSub = new TextView(context);
        tvSub.setText(subtitle);
        tvSub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        tvSub.setTextColor(PerazimTheme.COLOR_TEXT_SECONDARY);
        tvSub.setMaxLines(1);
        textCol.addView(tvSub);
        row.addView(textCol);

        ImageView chevron = new ImageView(context);
        chevron.setImageResource(org.perazimchurch.app.R.drawable.ic_chevron_right);
        chevron.setColorFilter(PerazimTheme.COLOR_PURPLE_MUTED, PorterDuff.Mode.SRC_IN);
        LinearLayout.LayoutParams lpChev = new LinearLayout.LayoutParams(dp(14), dp(14));
        chevron.setLayoutParams(lpChev);
        row.addView(chevron);

        row.setOnClickListener(onClickListener);
        return row;
    }

    public void showSavedContentDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        ScrollView sv = new ScrollView(context);
        sv.setFillViewport(true);

        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);

        Bitmap arkBmp = loadAssetBitmap("backgrounds/bg_offline_ark.png", 720);
        if (arkBmp != null) {
            FrameLayout headerStrip = new FrameLayout(context);
            headerStrip.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dp(120)));
            headerStrip.setBackground(PerazimTheme.createCardGradient(dp(12), dp(1)));
            headerStrip.setClipToOutline(true);

            ImageView ivArk = new ImageView(context);
            ivArk.setImageBitmap(arkBmp);
            ivArk.setScaleType(ImageView.ScaleType.CENTER_CROP);
            headerStrip.addView(ivArk, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

            View bottomScrim = new View(context);
            GradientDrawable scrimGrad = new GradientDrawable(
                    GradientDrawable.Orientation.TOP_BOTTOM,
                    new int[]{Color.TRANSPARENT, Color.parseColor("#E62A0F33")});
            bottomScrim.setBackground(scrimGrad);
            headerStrip.addView(bottomScrim, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

            TextView tvTag = new TextView(context);
            tvTag.setText("SAVED & OFFLINE CONTENT");
            tvTag.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            tvTag.setTypeface(Typeface.DEFAULT_BOLD);
            tvTag.setTextColor(PerazimTheme.COLOR_TEXT_PRIMARY);
            FrameLayout.LayoutParams lpTag = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lpTag.gravity = Gravity.BOTTOM | Gravity.START;
            lpTag.setMargins(dp(16), 0, 0, dp(12));
            tvTag.setLayoutParams(lpTag);
            headerStrip.addView(tvTag);

            container.addView(headerStrip);
        }

        View view = createSavedContentManagerCard();
        container.addView(view);
        sv.addView(container);
        builder.setView(sv);
        builder.setPositiveButton("Close", null);
        AlertDialog dialog = builder.create();
        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(PerazimTheme.createDialogGradient(dp(18), dp(1)));
        }
    }

    public void showBreakthroughGuideDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        ScrollView sv = new ScrollView(context);
        sv.setFillViewport(true);

        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);

        Bitmap flameBmp = loadAssetBitmap("backgrounds/bg_ai_guide_flame.png", 720);
        if (flameBmp != null) {
            FrameLayout headerStrip = new FrameLayout(context);
            headerStrip.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dp(120)));
            headerStrip.setBackground(PerazimTheme.createCardGradient(dp(12), dp(1)));
            headerStrip.setClipToOutline(true);

            ImageView ivFlame = new ImageView(context);
            ivFlame.setImageBitmap(flameBmp);
            ivFlame.setScaleType(ImageView.ScaleType.CENTER_CROP);
            headerStrip.addView(ivFlame, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

            View bottomScrim = new View(context);
            GradientDrawable scrimGrad = new GradientDrawable(
                    GradientDrawable.Orientation.TOP_BOTTOM,
                    new int[]{Color.TRANSPARENT, Color.parseColor("#E62A0F33")});
            bottomScrim.setBackground(scrimGrad);
            headerStrip.addView(bottomScrim, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

            TextView tvTag = new TextView(context);
            tvTag.setText("BREAKTHROUGH GUIDE");
            tvTag.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            tvTag.setTypeface(Typeface.DEFAULT_BOLD);
            tvTag.setTextColor(PerazimTheme.COLOR_TEXT_PRIMARY);
            FrameLayout.LayoutParams lpTag = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lpTag.gravity = Gravity.BOTTOM | Gravity.START;
            lpTag.setMargins(dp(16), 0, 0, dp(12));
            tvTag.setLayoutParams(lpTag);
            headerStrip.addView(tvTag);

            container.addView(headerStrip);
        }

        LinearLayout body = new LinearLayout(context);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(20), dp(16), dp(20), dp(20));

        TextView tvSubtitle = new TextView(context);
        tvSubtitle.setText("Contemplative Spiritual Companion");
        tvSubtitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        tvSubtitle.setTypeface(Typeface.DEFAULT_BOLD);
        tvSubtitle.setTextColor(PerazimTheme.COLOR_TEXT_PRIMARY);
        body.addView(tvSubtitle);

        TextView tvDesc = new TextView(context);
        tvDesc.setText("The Breakthrough Guide is an offline-first spiritual companion for scripture study, sermon reflections, and guided prayer devotionals tailored for Perazim Mission Church members.\n\n“The Lord has broken through my enemies before me, like a breakthrough of water.”\n— 2 Samuel 5:20");
        tvDesc.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        tvDesc.setTextColor(PerazimTheme.COLOR_PURPLE_SOFT);
        tvDesc.setLineSpacing(dp(3), 1.2f);
        tvDesc.setPadding(0, dp(10), 0, 0);
        body.addView(tvDesc);

        container.addView(body);
        sv.addView(container);
        builder.setView(sv);
        builder.setPositiveButton("Amen", null);
        AlertDialog dialog = builder.create();
        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(PerazimTheme.createDialogGradient(dp(18), dp(1)));
        }
    }

    private void showPastoralCareDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Pastoral Care & Headquarters");
        builder.setMessage("Bishop Dr. David Mutweri & Pastoral Team\n"
                + "Perazim Mission Church · Embu Headquarters\n\n"
                + "Office Line: (+254) 0710 772 227\n"
                + "Email: info@perazimchurch.org");

        builder.setPositiveButton("WhatsApp", (d, w) -> {
            ChurchIntentManager.openWhatsAppChat(context, "+254710772227", "Peace be with you Bishop Dr. David Mutweri, I am reaching out for pastoral care / prayer support.");
        });
        builder.setNegativeButton("Call", (d, w) -> {
            ChurchIntentManager.dialPhone(context, "+254710772227");
        });
        builder.setNeutralButton("Directions", (d, w) -> {
            ChurchIntentManager.openChurchMaps(context);
        });
        builder.show();
    }

    public void showAboutDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        ScrollView sv = new ScrollView(context);
        sv.setFillViewport(true);

        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);

        Bitmap sanctuaryBmp = loadAssetBitmap("backgrounds/bg_about_sanctuary.png", 720);
        if (sanctuaryBmp != null) {
            FrameLayout headerStrip = new FrameLayout(context);
            headerStrip.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dp(140)));
            headerStrip.setBackground(PerazimTheme.createCardGradient(dp(12), dp(1)));
            headerStrip.setClipToOutline(true);

            ImageView ivSanctuary = new ImageView(context);
            ivSanctuary.setImageBitmap(sanctuaryBmp);
            ivSanctuary.setScaleType(ImageView.ScaleType.CENTER_CROP);
            headerStrip.addView(ivSanctuary, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

            View bottomScrim = new View(context);
            GradientDrawable scrimGrad = new GradientDrawable(
                    GradientDrawable.Orientation.TOP_BOTTOM,
                    new int[]{Color.TRANSPARENT, Color.parseColor("#E62A0F33")});
            bottomScrim.setBackground(scrimGrad);
            headerStrip.addView(bottomScrim, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

            TextView tvTag = new TextView(context);
            tvTag.setText("ABOUT PERAZIM MISSION CHURCH");
            tvTag.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            tvTag.setTypeface(Typeface.DEFAULT_BOLD);
            tvTag.setTextColor(PerazimTheme.COLOR_TEXT_PRIMARY);
            FrameLayout.LayoutParams lpTag = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lpTag.gravity = Gravity.BOTTOM | Gravity.START;
            lpTag.setMargins(dp(16), 0, 0, dp(12));
            tvTag.setLayoutParams(lpTag);
            headerStrip.addView(tvTag);

            container.addView(headerStrip);
        }

        LinearLayout body = new LinearLayout(context);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(20), dp(16), dp(20), dp(20));

        TextView tvMotto = new TextView(context);
        tvMotto.setText("“A place where everybody is somebody, and no body is a nobody.”");
        tvMotto.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        tvMotto.setTypeface(Typeface.SERIF, Typeface.ITALIC);
        tvMotto.setTextColor(PerazimTheme.COLOR_ORANGE_SOFT);
        body.addView(tvMotto);

        TextView tvContent = new TextView(context);
        tvContent.setText("\nLeadership: Bishop Dr. David Mutweri\n"
                + "Embu Headquarters, Kenya\n"
                + "Mission: Spreading the Gospel of Christ across Kenya & Beyond\n\n"
                + "App Version: 2.2.0 (Build 4)\n"
                + "Faithful offline-first faith companion.");
        tvContent.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        tvContent.setTextColor(PerazimTheme.COLOR_PURPLE_SOFT);
        tvContent.setLineSpacing(dp(3), 1.2f);
        body.addView(tvContent);

        container.addView(body);
        sv.addView(container);
        builder.setView(sv);
        builder.setPositiveButton("Close", null);
        AlertDialog dialog = builder.create();
        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(PerazimTheme.createDialogGradient(dp(18), dp(1)));
        }
    }

    public void showFellowshipExtrasDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Fellowship Extras");

        ScrollView sv = new ScrollView(context);
        sv.setFillViewport(true);
        LinearLayout content = new LinearLayout(context);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(16), dp(16), dp(16));
        content.setBackgroundColor(PerazimTheme.COLOR_SURFACE);

        // Tab Row: Riddles | Jokes
        LinearLayout tabRow = new LinearLayout(context);
        tabRow.setOrientation(LinearLayout.HORIZONTAL);
        tabRow.setPadding(0, 0, 0, dp(12));

        Button btnTabRiddles = new Button(context);
        btnTabRiddles.setText("Devotional Riddles");
        btnTabRiddles.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        btnTabRiddles.setTextColor(PerazimTheme.COLOR_TEXT_PRIMARY);
        GradientDrawable rBg = new GradientDrawable();
        rBg.setColor(PerazimTheme.COLOR_PURPLE_BRAND);
        rBg.setCornerRadius(dp(8));
        btnTabRiddles.setBackground(rBg);
        LinearLayout.LayoutParams lpTr = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        lpTr.setMargins(0, 0, dp(4), 0);
        btnTabRiddles.setLayoutParams(lpTr);
        tabRow.addView(btnTabRiddles);

        Button btnTabJokes = new Button(context);
        btnTabJokes.setText("Christian Joy & Humor");
        btnTabJokes.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        btnTabJokes.setTextColor(PerazimTheme.COLOR_PURPLE_MUTED);
        GradientDrawable jBg = new GradientDrawable();
        jBg.setColor(PerazimTheme.COLOR_SURFACE_2);
        jBg.setCornerRadius(dp(8));
        btnTabJokes.setBackground(jBg);
        LinearLayout.LayoutParams lpTj = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        lpTj.setMargins(dp(4), 0, 0, 0);
        btnTabJokes.setLayoutParams(lpTj);
        tabRow.addView(btnTabJokes);

        content.addView(tabRow);

        // Container for active tab items
        LinearLayout listContainer = new LinearLayout(context);
        listContainer.setOrientation(LinearLayout.VERTICAL);
        content.addView(listContainer);

        new Thread(() -> {
            List<org.perazimchurch.app.data.local.entity.RiddleEntity> riddles = null;
            List<org.perazimchurch.app.data.local.entity.JokeEntity> jokes = null;
            try {
                org.perazimchurch.app.data.local.PerazimDatabase db = org.perazimchurch.app.data.local.PerazimDatabase.getInstance(context);
                riddles = db.riddleDao().getAllRiddles();
                jokes = db.jokeDao().getAllJokes();
            } catch (Exception ignored) {}

            final List<org.perazimchurch.app.data.local.entity.RiddleEntity> fRiddles = riddles;
            final List<org.perazimchurch.app.data.local.entity.JokeEntity> fJokes = jokes;

            postToMain(() -> {
                Runnable renderRiddles = () -> {
                    listContainer.removeAllViews();
                    btnTabRiddles.setTextColor(PerazimTheme.COLOR_TEXT_PRIMARY);
                    ((GradientDrawable) btnTabRiddles.getBackground()).setColor(PerazimTheme.COLOR_PURPLE_BRAND);
                    btnTabJokes.setTextColor(PerazimTheme.COLOR_PURPLE_MUTED);
                    ((GradientDrawable) btnTabJokes.getBackground()).setColor(PerazimTheme.COLOR_SURFACE_2);

                    if (fRiddles == null || fRiddles.isEmpty()) {
                        TextView empty = new TextView(context);
                        empty.setText("No riddles loaded. Sync sheets to load.");
                        empty.setTextColor(PerazimTheme.COLOR_TEXT_SECONDARY);
                        listContainer.addView(empty);
                        return;
                    }

                    for (org.perazimchurch.app.data.local.entity.RiddleEntity r : fRiddles) {
                        LinearLayout item = new LinearLayout(context);
                        item.setOrientation(LinearLayout.VERTICAL);
                        item.setPadding(dp(12), dp(10), dp(12), dp(10));
                        GradientDrawable ibg = new GradientDrawable();
                        ibg.setColor(PerazimTheme.COLOR_SURFACE_2);
                        ibg.setCornerRadius(dp(8));
                        item.setBackground(ibg);
                        LinearLayout.LayoutParams lpItem = new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                        lpItem.setMargins(0, 0, 0, dp(8));
                        item.setLayoutParams(lpItem);

                        TextView tvQ = new TextView(context);
                        tvQ.setText(r.getQuestion());
                        tvQ.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
                        tvQ.setTypeface(Typeface.DEFAULT_BOLD);
                        tvQ.setTextColor(PerazimTheme.COLOR_TEXT_PRIMARY);
                        item.addView(tvQ);

                        TextView tvAns = new TextView(context);
                        tvAns.setText("Answer: " + r.getAnswer() + (r.getScriptureRef() != null ? " (" + r.getScriptureRef() + ")" : ""));
                        tvAns.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
                        tvAns.setTextColor(PerazimTheme.COLOR_PURPLE_SOFT);
                        tvAns.setVisibility(View.GONE);
                        tvAns.setPadding(0, dp(4), 0, dp(4));

                        Button btnRev = new Button(context);
                        btnRev.setText("Reveal Answer");
                        btnRev.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
                        btnRev.setTextColor(PerazimTheme.COLOR_TEXT_PRIMARY);
                        GradientDrawable rbg2 = new GradientDrawable();
                        rbg2.setColor(PerazimTheme.COLOR_PURPLE_GHOST);
                        rbg2.setCornerRadius(dp(6));
                        btnRev.setBackground(rbg2);
                        btnRev.setPadding(dp(8), dp(4), dp(8), dp(4));
                        btnRev.setOnClickListener(v -> {
                            boolean isVis = (tvAns.getVisibility() == View.VISIBLE);
                            tvAns.setVisibility(isVis ? View.GONE : View.VISIBLE);
                            btnRev.setText(isVis ? "Reveal Answer" : "Hide Answer");
                        });
                        item.addView(tvAns);
                        item.addView(btnRev);
                        listContainer.addView(item);
                    }
                };

                Runnable renderJokes = () -> {
                    listContainer.removeAllViews();
                    btnTabJokes.setTextColor(PerazimTheme.COLOR_TEXT_PRIMARY);
                    ((GradientDrawable) btnTabJokes.getBackground()).setColor(PerazimTheme.COLOR_PURPLE_BRAND);
                    btnTabRiddles.setTextColor(PerazimTheme.COLOR_PURPLE_MUTED);
                    ((GradientDrawable) btnTabRiddles.getBackground()).setColor(PerazimTheme.COLOR_SURFACE_2);

                    if (fJokes == null || fJokes.isEmpty()) {
                        TextView empty = new TextView(context);
                        empty.setText("No jokes loaded. Sync sheets to load.");
                        empty.setTextColor(PerazimTheme.COLOR_TEXT_SECONDARY);
                        listContainer.addView(empty);
                        return;
                    }

                    for (org.perazimchurch.app.data.local.entity.JokeEntity j : fJokes) {
                        LinearLayout item = new LinearLayout(context);
                        item.setOrientation(LinearLayout.VERTICAL);
                        item.setPadding(dp(12), dp(10), dp(12), dp(10));
                        GradientDrawable ibg = new GradientDrawable();
                        ibg.setColor(PerazimTheme.COLOR_SURFACE_2);
                        ibg.setCornerRadius(dp(8));
                        item.setBackground(ibg);
                        LinearLayout.LayoutParams lpItem = new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                        lpItem.setMargins(0, 0, 0, dp(8));
                        item.setLayoutParams(lpItem);

                        TextView tvJoke = new TextView(context);
                        tvJoke.setText(j.getText());
                        tvJoke.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
                        tvJoke.setTextColor(PerazimTheme.COLOR_TEXT_PRIMARY);
                        item.addView(tvJoke);
                        listContainer.addView(item);
                    }
                };

                btnTabRiddles.setOnClickListener(v -> renderRiddles.run());
                btnTabJokes.setOnClickListener(v -> renderJokes.run());
                renderRiddles.run();
            });
        }).start();

        sv.addView(content);
        builder.setView(sv);
        builder.setPositiveButton("Close", null);
        builder.show();
    }

    private String resolveGamificationUserId() {
        if (context == null) return "guest";
        try {
            SessionManager sm = SessionManager.getInstance(context);
            if (sm != null && sm.getActiveUserId() != null && !sm.getActiveUserId().trim().isEmpty()) {
                return sm.getActiveUserId();
            }
        } catch (Exception ignored) {}
        return "guest";
    }

    // =========================================================================
    // 3. SAVED CONTENT MANAGER (Tabbed / Expandable)
    // =========================================================================

    private View createSavedContentManagerCard() {
        LinearLayout card = createCard(PerazimTheme.COLOR_WHITE, PerazimTheme.RADIUS_LG, PerazimTheme.COLOR_BORDER_GREY, 1);
        card.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lpCard = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lpCard.setMargins(0, 0, 0, dp(12));
        card.setLayoutParams(lpCard);

        TextView tvTag = new TextView(context);
        tvTag.setText("📦 SAVED & OFFLINE CONTENT MANAGER");
        tvTag.setTextSize(TypedValue.COMPLEX_UNIT_SP, PerazimTheme.TEXT_SIZE_CAPTION);
        tvTag.setTypeface(Typeface.DEFAULT_BOLD);
        tvTag.setTextColor(PerazimTheme.COLOR_PRIMARY_PURPLE);
        tvTag.setPadding(0, 0, 0, dp(8));
        card.addView(tvTag);

        // Tab Selector Row
        savedContentTabRow = new LinearLayout(context);
        savedContentTabRow.setOrientation(LinearLayout.HORIZONTAL);
        savedContentTabRow.setPadding(0, 0, 0, dp(10));

        btnTabSermons = createTabButton("🎙️ Sermons", 0);
        btnTabHymns = createTabButton("🎵 Hymns", 1);
        btnTabVerses = createTabButton("📖 Verses", 2);

        savedContentTabRow.addView(btnTabSermons);
        savedContentTabRow.addView(btnTabHymns);
        savedContentTabRow.addView(btnTabVerses);
        card.addView(savedContentTabRow);

        // List Container for active tab
        savedContentListContainer = new LinearLayout(context);
        savedContentListContainer.setOrientation(LinearLayout.VERTICAL);
        card.addView(savedContentListContainer);

        // Update tab styles
        updateTabButtonStyles();

        return card;
    }

    private Button createTabButton(String label, int tabIndex) {
        Button btn = new Button(context);
        btn.setText(label);
        btn.setTextSize(TypedValue.COMPLEX_UNIT_SP, PerazimTheme.TEXT_SIZE_CAPTION);
        btn.setTypeface(Typeface.DEFAULT_BOLD);
        btn.setPadding(dp(8), dp(6), dp(8), dp(6));

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        lp.setMargins(dp(2), 0, dp(2), 0);
        btn.setLayoutParams(lp);

        btn.setOnClickListener(v -> {
            selectedSavedContentTab = tabIndex;
            updateTabButtonStyles();
            renderActiveSavedContentTab();
        });
        return btn;
    }

    private void updateTabButtonStyles() {
        applyTabStyle(btnTabSermons, selectedSavedContentTab == 0);
        applyTabStyle(btnTabHymns, selectedSavedContentTab == 1);
        applyTabStyle(btnTabVerses, selectedSavedContentTab == 2);
    }

    private void applyTabStyle(Button btn, boolean isSelected) {
        if (btn == null) return;
        if (isSelected) {
            btn.setTextColor(PerazimTheme.COLOR_WHITE);
            GradientDrawable g = new GradientDrawable();
            g.setColor(PerazimTheme.COLOR_PRIMARY_PURPLE);
            g.setCornerRadius(dp(PerazimTheme.RADIUS_MD));
            btn.setBackground(g);
        } else {
            btn.setTextColor(PerazimTheme.COLOR_TEXT_SECONDARY);
            GradientDrawable g = new GradientDrawable();
            g.setColor(PerazimTheme.COLOR_SURFACE_2);
            g.setCornerRadius(dp(PerazimTheme.RADIUS_MD));
            g.setStroke(dp(1), PerazimTheme.COLOR_BORDER_GREY);
            btn.setBackground(g);
        }
    }

    public void refreshSavedContent() {
        viewModel.loadSavedContent(data -> {
            postToMain(() -> {
                currentSavedContent = data;
                updateTabBadgeCounts(data);
                renderActiveSavedContentTab();
            });
        });
    }

    private void updateTabBadgeCounts(@Nullable SavedContentData data) {
        int sermonsCount = (data != null && data.getDownloadedSermons() != null) ? data.getDownloadedSermons().size() : 0;
        int hymnsCount = (data != null && data.getFavoriteHymns() != null) ? data.getFavoriteHymns().size() : 0;
        int versesCount = (data != null && data.getBookmarkedVerses() != null) ? data.getBookmarkedVerses().size() : 0;

        if (btnTabSermons != null) btnTabSermons.setText("🎙️ Sermons (" + sermonsCount + ")");
        if (btnTabHymns != null) btnTabHymns.setText("🎵 Hymns (" + hymnsCount + ")");
        if (btnTabVerses != null) btnTabVerses.setText("📖 Verses (" + versesCount + ")");
    }

    private void renderActiveSavedContentTab() {
        if (savedContentListContainer == null) return;
        savedContentListContainer.removeAllViews();

        if (currentSavedContent == null) {
            renderEmptySavedContent("Loading offline content...");
            return;
        }

        switch (selectedSavedContentTab) {
            case 0: // Downloaded Sermons
                renderDownloadedSermons(currentSavedContent.getDownloadedSermons());
                break;
            case 1: // Favorite Hymns
                renderFavoriteHymns(currentSavedContent.getFavoriteHymns());
                break;
            case 2: // Bookmarked Scripture Verses
                renderBookmarkedVerses(currentSavedContent.getBookmarkedVerses());
                break;
        }
    }

    private void renderDownloadedSermons(@Nullable List<DownloadedContent> downloads) {
        if (downloads == null || downloads.isEmpty()) {
            renderEmptySavedContent("No audio sermons downloaded yet. Tap download on any sermon to listen offline with zero data.");
            return;
        }

        for (DownloadedContent item : downloads) {
            LinearLayout row = createSavedItemRow();

            TextView tvIcon = new TextView(context);
            tvIcon.setText("🎙️");
            tvIcon.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f);
            tvIcon.setPadding(0, 0, dp(8), 0);
            row.addView(tvIcon);

            LinearLayout textCol = new LinearLayout(context);
            textCol.setOrientation(LinearLayout.VERTICAL);
            textCol.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));

            TextView tvTitle = new TextView(context);
            tvTitle.setText(item.getTitle() != null ? item.getTitle() : "Sermon Audio");
            tvTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, PerazimTheme.TEXT_SIZE_BODY_SM);
            tvTitle.setTypeface(Typeface.DEFAULT_BOLD);
            tvTitle.setTextColor(PerazimTheme.COLOR_PURPLE_DEEP);
            textCol.addView(tvTitle);

            long mb = Math.max(1, item.getFileSizeBytes() / (1024 * 1024));
            TextView tvSub = new TextView(context);
            tvSub.setText(mb + " MB · 100% Offline Ready (32kbps Data Saver)");
            tvSub.setTextSize(TypedValue.COMPLEX_UNIT_SP, PerazimTheme.TEXT_SIZE_MICRO);
            tvSub.setTextColor(PerazimTheme.COLOR_SUCCESS);
            textCol.addView(tvSub);

            row.addView(textCol);

            Button btnPlay = new Button(context);
            btnPlay.setText("▶ Play");
            btnPlay.setTextSize(TypedValue.COMPLEX_UNIT_SP, PerazimTheme.TEXT_SIZE_MICRO);
            btnPlay.setTextColor(PerazimTheme.COLOR_WHITE);
            btnPlay.setBackground(create3dButtonDrawable(
                    PerazimTheme.COLOR_PRIMARY_PURPLE,
                    PerazimTheme.COLOR_PURPLE_SHADOW,
                    PerazimTheme.RADIUS_SM,
                    2
            ));
            btnPlay.setPadding(dp(8), dp(4), dp(8), dp(4));
            btnPlay.setOnClickListener(v -> Toast.makeText(context, "Playing offline sermon: " + item.getTitle(), Toast.LENGTH_SHORT).show());
            row.addView(btnPlay);

            savedContentListContainer.addView(row);
        }
    }

    private void renderFavoriteHymns(@Nullable List<Hymn> hymns) {
        if (hymns == null || hymns.isEmpty()) {
            renderEmptySavedContent("No favorite hymns saved yet. Heart any hymn in the Worship tab to keep it ready for devotion.");
            return;
        }

        for (Hymn hymn : hymns) {
            LinearLayout row = createSavedItemRow();

            TextView tvIcon = new TextView(context);
            tvIcon.setText("🎵");
            tvIcon.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f);
            tvIcon.setPadding(0, 0, dp(8), 0);
            row.addView(tvIcon);

            LinearLayout textCol = new LinearLayout(context);
            textCol.setOrientation(LinearLayout.VERTICAL);
            textCol.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));

            TextView tvTitle = new TextView(context);
            tvTitle.setText("#" + hymn.getNumber() + " " + hymn.getTitle());
            tvTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, PerazimTheme.TEXT_SIZE_BODY_SM);
            tvTitle.setTypeface(Typeface.DEFAULT_BOLD);
            tvTitle.setTextColor(PerazimTheme.COLOR_PURPLE_DEEP);
            textCol.addView(tvTitle);

            TextView tvSub = new TextView(context);
            String keyBpm = (hymn.getKey() != null ? "Key: " + hymn.getKey() : "") +
                    (hymn.getTempoBpm() > 0 ? " · " + hymn.getTempoBpm() + " BPM" : "");
            tvSub.setText(keyBpm.isEmpty() ? "Offline Chord Chart Available" : keyBpm);
            tvSub.setTextSize(TypedValue.COMPLEX_UNIT_SP, PerazimTheme.TEXT_SIZE_MICRO);
            tvSub.setTextColor(PerazimTheme.COLOR_TEXT_MUTED);
            textCol.addView(tvSub);

            row.addView(textCol);

            Button btnSing = new Button(context);
            btnSing.setText("View");
            btnSing.setTextSize(TypedValue.COMPLEX_UNIT_SP, PerazimTheme.TEXT_SIZE_MICRO);
            btnSing.setTextColor(PerazimTheme.COLOR_WHITE);
            btnSing.setBackground(create3dButtonDrawable(
                    PerazimTheme.COLOR_ACCENT_ORANGE,
                    PerazimTheme.COLOR_ORANGE_SHADOW,
                    PerazimTheme.RADIUS_SM,
                    2
            ));
            btnSing.setPadding(dp(8), dp(4), dp(8), dp(4));
            btnSing.setOnClickListener(v -> Toast.makeText(context, "Hymn #" + hymn.getNumber() + ": " + hymn.getTitle(), Toast.LENGTH_SHORT).show());
            row.addView(btnSing);

            savedContentListContainer.addView(row);
        }
    }

    private void renderBookmarkedVerses(@Nullable List<BibleVerse> verses) {
        if (verses == null || verses.isEmpty()) {
            renderEmptySavedContent("No scripture verses bookmarked yet. Tap the bookmark icon while reading scripture to save verses here.");
            return;
        }

        for (BibleVerse verse : verses) {
            LinearLayout row = createSavedItemRow();

            TextView tvIcon = new TextView(context);
            tvIcon.setText("📖");
            tvIcon.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f);
            tvIcon.setPadding(0, 0, dp(8), 0);
            row.addView(tvIcon);

            LinearLayout textCol = new LinearLayout(context);
            textCol.setOrientation(LinearLayout.VERTICAL);
            textCol.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));

            TextView tvRef = new TextView(context);
            tvRef.setText(verse.getBookId() + " " + verse.getChapterNumber() + ":" + verse.getVerseNumber() + " (" + verse.getTranslationId() + ")");
            tvRef.setTextSize(TypedValue.COMPLEX_UNIT_SP, PerazimTheme.TEXT_SIZE_BODY_SM);
            tvRef.setTypeface(Typeface.DEFAULT_BOLD);
            tvRef.setTextColor(PerazimTheme.COLOR_PRIMARY_PURPLE);
            textCol.addView(tvRef);

            TextView tvText = new TextView(context);
            tvText.setText(verse.getText());
            tvText.setTextSize(TypedValue.COMPLEX_UNIT_SP, PerazimTheme.TEXT_SIZE_CAPTION);
            tvText.setTextColor(PerazimTheme.COLOR_TEXT_PRIMARY);
            tvText.setMaxLines(2);
            textCol.addView(tvText);

            row.addView(textCol);
            savedContentListContainer.addView(row);
        }
    }

    private LinearLayout createSavedItemRow() {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(10), dp(8), dp(10), dp(8));

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(PerazimTheme.COLOR_SURFACE_2);
        bg.setCornerRadius(dp(PerazimTheme.RADIUS_SM));
        bg.setStroke(dp(1), PerazimTheme.COLOR_BORDER_GREY);
        row.setBackground(bg);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(6));
        row.setLayoutParams(lp);
        return row;
    }

    private void renderEmptySavedContent(String message) {
        TextView tv = new TextView(context);
        tv.setText(message);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, PerazimTheme.TEXT_SIZE_BODY_SM);
        tv.setTextColor(PerazimTheme.COLOR_TEXT_MUTED);
        tv.setPadding(dp(12), dp(16), dp(12), dp(16));
        tv.setGravity(Gravity.CENTER);
        savedContentListContainer.addView(tv);
    }

    // =========================================================================
    // 4. CAMPUS SWITCHER & CHURCH IDENTITY
    // =========================================================================

    private View createChurchIdentityCard() {
        LinearLayout card = createCard(PerazimTheme.COLOR_WHITE, PerazimTheme.RADIUS_LG, PerazimTheme.COLOR_BORDER_GREY, 1);
        card.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(12));
        card.setLayoutParams(lp);

        TextView tvTag = new TextView(context);
        tvTag.setText("🏛️ CHURCH IDENTITY & AFFILIATION");
        tvTag.setTextSize(TypedValue.COMPLEX_UNIT_SP, PerazimTheme.TEXT_SIZE_CAPTION);
        tvTag.setTypeface(Typeface.DEFAULT_BOLD);
        tvTag.setTextColor(PerazimTheme.COLOR_PRIMARY_PURPLE);
        tvTag.setPadding(0, 0, 0, dp(6));
        card.addView(tvTag);

        TextView tvName = new TextView(context);
        tvName.setText("PERAZIM MISSION CHURCH");
        tvName.setTextSize(TypedValue.COMPLEX_UNIT_SP, PerazimTheme.TEXT_SIZE_SUBTITLE);
        tvName.setTypeface(Typeface.DEFAULT_BOLD);
        tvName.setTextColor(PerazimTheme.COLOR_PURPLE_DEEP);
        card.addView(tvName);

        TextView tvMotto = new TextView(context);
        tvMotto.setText("“The Place of Great Breakthrough” (2 Samuel 5:20)");
        tvMotto.setTextSize(TypedValue.COMPLEX_UNIT_SP, PerazimTheme.TEXT_SIZE_BODY_SM);
        tvMotto.setTypeface(Typeface.DEFAULT_BOLD);
        tvMotto.setTextColor(PerazimTheme.COLOR_ACCENT_ORANGE);
        tvMotto.setPadding(0, dp(2), 0, dp(4));
        card.addView(tvMotto);

        // Bishop Contact Row
        LinearLayout rowBishop = createInteractiveRow(
                "👤",
                "Presiding Bishop",
                "Bishop Dr. David Mutweri (+254 710 772 227)",
                v -> ChurchIntentManager.openWhatsAppChat(context, "+254710772227", "Peace be with you Bishop Dr. David Mutweri.")
        );
        card.addView(rowBishop);

        // Content Synchronization Row (Google Sheets)
        LinearLayout rowSync = createContentSyncRow();
        card.addView(rowSync);

        return card;
    }

    private LinearLayout createContentSyncRow() {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(8), dp(8), dp(8), dp(8));

        GradientDrawable rowBg = new GradientDrawable();
        rowBg.setColor(PerazimTheme.COLOR_BG_NEUTRAL);
        rowBg.setCornerRadius(dp(PerazimTheme.RADIUS_SM));
        rowBg.setStroke(dp(1), PerazimTheme.COLOR_BORDER_GREY);
        row.setBackground(rowBg);

        LinearLayout.LayoutParams lpRow = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lpRow.setMargins(0, 0, 0, dp(6));
        row.setLayoutParams(lpRow);

        TextView tvIcon = new TextView(context);
        tvIcon.setText("🔄");
        tvIcon.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f);
        tvIcon.setPadding(0, 0, dp(8), 0);
        row.addView(tvIcon);

        LinearLayout textCol = new LinearLayout(context);
        textCol.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lpText = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        textCol.setLayoutParams(lpText);

        TextView tvLbl = new TextView(context);
        tvLbl.setText("Refresh Content");
        tvLbl.setTextSize(TypedValue.COMPLEX_UNIT_SP, PerazimTheme.TEXT_SIZE_MICRO);
        tvLbl.setTextColor(PerazimTheme.COLOR_TEXT_MUTED);
        textCol.addView(tvLbl);

        tvSyncStatusVal = new TextView(context);
        tvSyncStatusVal.setText(SheetSyncManager.getLastSyncStatusText(context));
        tvSyncStatusVal.setTextSize(TypedValue.COMPLEX_UNIT_SP, PerazimTheme.TEXT_SIZE_BODY_SM);
        tvSyncStatusVal.setTypeface(Typeface.DEFAULT_BOLD);
        tvSyncStatusVal.setTextColor(PerazimTheme.COLOR_PURPLE_DEEP);
        textCol.addView(tvSyncStatusVal);

        row.addView(textCol);

        TextView tvHint = new TextView(context);
        tvHint.setText("Tap to Sync ➔");
        tvHint.setTextSize(TypedValue.COMPLEX_UNIT_SP, PerazimTheme.TEXT_SIZE_MICRO);
        tvHint.setTypeface(Typeface.DEFAULT_BOLD);
        tvHint.setTextColor(PerazimTheme.COLOR_ACCENT_ORANGE);
        row.addView(tvHint);

        row.setOnClickListener(v -> triggerContentSync());
        return row;
    }

    private void triggerContentSync() {
        if (tvSyncStatusVal != null) {
            tvSyncStatusVal.setText("Syncing church content...");
        }
        Toast.makeText(context, "Refreshing local and church content...", Toast.LENGTH_SHORT).show();
        org.perazimchurch.app.sync.SyncDispatcher.dispatchSync(context, null);
        SheetSyncManager.syncNow(context, (success, synced, skipped, message) -> {
            postToMain(() -> {
                if (tvSyncStatusVal != null) {
                    tvSyncStatusVal.setText(SheetSyncManager.getLastSyncStatusText(context));
                }
                if (success) {
                    Toast.makeText(context, "✅ Church content updated (" + synced + " items synced)", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    // =========================================================================
    // 5. PASTORAL CARE & SUPPORT
    // =========================================================================

    private View createPastoralSupportCard() {
        LinearLayout card = createCard(PerazimTheme.COLOR_WHITE, PerazimTheme.RADIUS_LG, PerazimTheme.COLOR_BORDER_GREY, 1);
        card.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(10));
        card.setLayoutParams(lp);

        TextView tvTag = new TextView(context);
        tvTag.setText("🙏 PASTORAL CARE & DIRECT SUPPORT");
        tvTag.setTextSize(TypedValue.COMPLEX_UNIT_SP, PerazimTheme.TEXT_SIZE_CAPTION);
        tvTag.setTypeface(Typeface.DEFAULT_BOLD);
        tvTag.setTextColor(PerazimTheme.COLOR_PRIMARY_PURPLE);
        tvTag.setPadding(0, 0, 0, dp(6));
        card.addView(tvTag);

        card.addView(createInteractiveRow("💬", "WhatsApp Bishop Directly", "(+254) 710 772 227 (Tap to Chat)", v -> ChurchIntentManager.openWhatsAppChat(context, "+254710772227", "Peace be with you Bishop Dr. David Mutweri. I am reaching out from the Perazim App for pastoral support.")));
        card.addView(createInteractiveRow("📞", "Call Bishop Directly", "(+254) 710 772 227 (Native Phone Dialer)", v -> ChurchIntentManager.dialPhone(context, "+254710772227")));
        card.addView(createInteractiveRow("✉️", "SMS Pastoral Line", "(+254) 710 772 227 (Offline SMS)", v -> ChurchIntentManager.sendSms(context, "+254710772227", "Praise God Bishop, I am requesting prayer through the Perazim App.")));
        card.addView(createInteractiveRow("📍", "Directions to Embu HQ", "Perazim Mission Church, Embu", v -> ChurchIntentManager.openChurchMaps(context)));
        card.addView(createInteractiveRow("💚", "Safaricom Giving Paybill", "4069983 (Lipa na M-Pesa)", v -> ChurchIntentManager.launchMpesaGiving(context, "4069983", "OFFERING", "")));
        card.addView(createInteractiveRow("📧", "Secretariat Email", "info@perazimchurch.org", v -> ChurchIntentManager.sendEmail(context, "info@perazimchurch.org", "Perazim Church Inquiry", "")));
        card.addView(createInteractiveRow("🛠️", "Feedback & Report Issue", "WhatsApp App Admin (Bug reports & suggestions)", v -> ChurchIntentManager.openWhatsAppChat(context, "+254710772227", "Perazim App Pilot Feedback:\n\nDevice: " + android.os.Build.MANUFACTURER + " " + android.os.Build.MODEL + " (Android " + android.os.Build.VERSION.RELEASE + ")\nVersion: 2.2.0\nFeedback / Bug: ")));

        return card;
    }

    // =========================================================================
    // PROFILE DATA LOADING & UI SYNC
    // =========================================================================

    public void refreshProfile() {
        viewModel.loadProfile(user -> {
            postToMain(() -> {
                currentUser = user;
                renderAvatarFromCurrentUser();
                updateAuthButtonState();
                if (SessionManager.getInstance(context).isGuest()) {
                    if (tvMemberName != null) tvMemberName.setText("Guest Pilgrim (Offline)");
                    if (tvMemberEmail != null) tvMemberEmail.setText("Tap 'Sign In / Register' to connect with church");
                } else if (user != null) {
                    if (tvMemberName != null && user.getFullName() != null) {
                        tvMemberName.setText(user.getFullName());
                    }
                    if (tvMemberEmail != null && user.getEmail() != null) {
                        tvMemberEmail.setText(user.getEmail());
                    }
                }
                if (user != null && user.getRole() != null && tvCovenantBadge != null) {
                    tvCovenantBadge.setText("🕊️ " + user.getRole());
                }
                String uid = (user != null && user.getId() != null) ? user.getId() : SessionManager.getInstance(context).getActiveUserId();
                if (uid == null || uid.trim().isEmpty() || "guest_user".equals(uid)) {
                    uid = "guest";
                }
                int liveStreak = GamificationStore.getStreak(context, uid, user != null ? user.getStreakCount() : 0);
                int liveXp = GamificationStore.getSpiritualXp(context, uid, user != null ? user.getSpiritualXp() : 0);
                int liveGrace = GamificationStore.getGracePoints(context, uid, user != null ? user.getGracePoints() : 0);

                if (tvStreakValue != null) {
                    tvStreakValue.setText("🔥 " + liveStreak + " Days");
                }
                isStreakFrozen = user != null && user.isStreakFrozen();

                    if (tvXpValue != null) {
                        tvXpValue.setText("⭐ " + liveXp + " XP");
                    }
                    if (tvGraceValue != null) {
                        tvGraceValue.setText("💜 " + liveGrace + " Pts");
                    }

                    int liveLifetime = GamificationStore.getLifetimeRoutineDays(context, uid, 0);
                    boolean hasFirst = GamificationStore.hasBadgeFirstRoutine(context, uid);
                    boolean hasStreak7 = GamificationStore.hasBadgeStreak7(context, uid);
                    int badgeCount = (hasFirst ? 1 : 0) + (hasStreak7 ? 1 : 0);

                    if (tvLifetimeDaysValue != null) {
                        tvLifetimeDaysValue.setText("📅 " + liveLifetime + " Days");
                    }
                    if (tvBadgesCountValue != null) {
                        tvBadgesCountValue.setText("🏆 " + badgeCount + " / 2");
                    }
                    if (tvBadgeFirstRoutine != null) {
                        tvBadgeFirstRoutine.setText(hasFirst ? "🌟 First Routine ✓" : "🔒 First Routine");
                        tvBadgeFirstRoutine.setTextColor(hasFirst ? PerazimTheme.COLOR_PRIMARY_PURPLE : PerazimTheme.COLOR_TEXT_MUTED);
                        tvBadgeFirstRoutine.setBackground(createPillDrawable(
                                hasFirst ? PerazimTheme.COLOR_PURPLE_TINT : PerazimTheme.COLOR_BG_NEUTRAL,
                                hasFirst ? PerazimTheme.COLOR_PRIMARY_PURPLE : PerazimTheme.COLOR_BORDER_GREY));
                    }
                    if (tvBadgeStreak7 != null) {
                        tvBadgeStreak7.setText(hasStreak7 ? "🔥 7-Day Flame ✓" : "🔒 7-Day Flame");
                        tvBadgeStreak7.setTextColor(hasStreak7 ? PerazimTheme.COLOR_ACCENT_ORANGE : PerazimTheme.COLOR_TEXT_MUTED);
                        tvBadgeStreak7.setBackground(createPillDrawable(
                                hasStreak7 ? PerazimTheme.COLOR_ORANGE_TINT : PerazimTheme.COLOR_BG_NEUTRAL,
                                hasStreak7 ? PerazimTheme.COLOR_ACCENT_ORANGE : PerazimTheme.COLOR_BORDER_GREY));
                    }

                    int level = (liveXp / 500) + 1;
                    int progress = liveXp % 500;
                    if (xpProgressBar != null) {
                        xpProgressBar.setProgress(progress);
                    }
                    if (tvXpLevelLabel != null) {
                        tvXpLevelLabel.setText("Level " + level + " Disciple — " + progress + " / 500 XP to Level " + (level + 1));
                    }
                    if (tvSyncStatusVal != null) {
                        tvSyncStatusVal.setText(SheetSyncManager.getLastSyncStatusText(context));
                    }
                });
            });
        }

    private void renderAvatarFromCurrentUser() {
        if (ivAvatar == null) return;
        SessionManager session = SessionManager.getInstance(context);
        UserEntity entity = session.getCurrentUser();
        String userId = entity != null ? entity.getId()
                : (currentUser != null ? currentUser.getId() : session.getActiveUserId());
        String cachedRef = context.getSharedPreferences("profile_avatar_cache", Context.MODE_PRIVATE)
                .getString("avatar:" + userId, "");
        String avatarRef = entity != null && entity.getAvatarUrl() != null && !entity.getAvatarUrl().isEmpty()
                ? entity.getAvatarUrl() : cachedRef;
        displayedAvatarRef = avatarRef;
        renderAvatar(avatarRef, userId);
    }

    private void renderAvatar(@Nullable String avatarRef, @Nullable String userId) {
        if (ivAvatar == null) return;
        if (avatarRef == null) avatarRef = "";
        int drawable = ProfileAvatarHelper.drawableId(context,
                ProfileAvatarHelper.numberFor(avatarRef, userId));
        ivAvatar.setImageResource(drawable != 0 ? drawable : org.perazimchurch.app.R.drawable.ic_perazim_logo);
        ivAvatar.setTag(userId + "|" + avatarRef);
        if (avatarRef.startsWith("/uploads/")) loadUploadedAvatar(avatarRef, userId);
    }

    private void loadUploadedAvatar(@NonNull String path, @Nullable String userId) {
        String token = SessionManager.getInstance(context).getAuthToken();
        if (token == null || token.trim().isEmpty()) return;
        Object expectedTag = ivAvatar.getTag();
        String base = ApiClient.getBaseUrl(context).replaceAll("/+$", "");
        new Thread(() -> {
            Bitmap bitmap = null;
            HttpURLConnection connection = null;
            try {
                URL url = new URL(base + path);
                if (!ApiClient.allowsProtectedTransport(url)) return;
                connection = (HttpURLConnection) url.openConnection();
                connection.setInstanceFollowRedirects(false);
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(15000);
                connection.setRequestProperty("Authorization", "Bearer " + token.trim());
                connection.setRequestProperty("Accept", "image/*");
                if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    try (InputStream input = connection.getInputStream(); ByteArrayOutputStream bytes = new ByteArrayOutputStream()) {
                        byte[] chunk = new byte[8192];
                        int count;
                        int total = 0;
                        while ((count = input.read(chunk)) != -1 && total <= 2 * 1024 * 1024) {
                            bytes.write(chunk, 0, count);
                            total += count;
                        }
                        if (total <= 2 * 1024 * 1024) bitmap = BitmapFactory.decodeByteArray(bytes.toByteArray(), 0, bytes.size());
                    }
                }
            } catch (Exception ignored) {
                // The deterministic default remains visible for missing/unavailable photos.
            } finally {
                if (connection != null) connection.disconnect();
            }
            Bitmap loaded = bitmap;
            postToMain(() -> {
                if (loaded != null && ivAvatar != null && expectedTag.equals(ivAvatar.getTag())) {
                    ivAvatar.setImageBitmap(loaded);
                }
            });
        }, "profile-avatar-load").start();
    }

    private void showAvatarPicker() {
        LinearLayout panel = new LinearLayout(context);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(12), dp(8), dp(12), dp(4));

        TextView heading = new TextView(context);
        heading.setText("Choose a profile picture");
        heading.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        heading.setTypeface(Typeface.DEFAULT_BOLD);
        heading.setPadding(0, 0, 0, dp(10));
        panel.addView(heading);

        LinearLayout grid = new LinearLayout(context);
        grid.setOrientation(LinearLayout.VERTICAL);
        String activeId = SessionManager.getInstance(context).getActiveUserId();
        int selectedNumber = displayedAvatarRef.startsWith("default:")
                ? ProfileAvatarHelper.numberFor(displayedAvatarRef, activeId)
                : (displayedAvatarRef.isEmpty() ? ProfileAvatarHelper.defaultNumber(activeId) : -1);
        for (int row = 0; row < 5; row++) {
            LinearLayout rowView = new LinearLayout(context);
            rowView.setGravity(Gravity.CENTER);
            for (int col = 0; col < 4; col++) {
                int number = row * 4 + col + 1;
                ImageView choice = new ImageView(context);
                choice.setScaleType(ImageView.ScaleType.CENTER_CROP);
                choice.setImageResource(ProfileAvatarHelper.drawableId(context, number));
                GradientDrawable outline = new GradientDrawable();
                outline.setShape(GradientDrawable.OVAL);
                outline.setColor(Color.TRANSPARENT);
                outline.setStroke(dp(number == selectedNumber ? 3 : 1),
                        number == selectedNumber ? PerazimTheme.COLOR_ORANGE_SOFT : PerazimTheme.COLOR_STROKE);
                choice.setBackground(outline);
                choice.setClipToOutline(true);
                choice.setContentDescription("Default avatar " + number);
                LinearLayout.LayoutParams choiceParams = new LinearLayout.LayoutParams(dp(50), dp(50));
                choiceParams.setMargins(dp(3), dp(3), dp(3), dp(3));
                rowView.addView(choice, choiceParams);
                choice.setOnClickListener(v -> {
                    AlertDialog dialog = (AlertDialog) panel.getTag();
                    if (dialog != null) dialog.dismiss();
                    persistAvatarChoice("default:" + number);
                });
            }
            grid.addView(rowView);
        }
        panel.addView(grid);

        Button gallery = new Button(context);
        gallery.setText("Choose photo from gallery");
        gallery.setOnClickListener(v -> { dismissAvatarPicker(panel); choosePhotoFromGallery(); });
        panel.addView(gallery);
        Button camera = new Button(context);
        camera.setText("Take a photo");
        camera.setOnClickListener(v -> { dismissAvatarPicker(panel); takePhoto(); });
        panel.addView(camera);
        Button reset = new Button(context);
        reset.setText("Use my automatic default / remove photo");
        reset.setOnClickListener(v -> {
            dismissAvatarPicker(panel);
            persistAvatarChoice("");
        });
        panel.addView(reset);

        ScrollView pickerScroll = new ScrollView(context);
        pickerScroll.setFillViewport(true);
        pickerScroll.addView(panel);
        AlertDialog dialog = new AlertDialog.Builder(context).setView(pickerScroll).setNegativeButton("Close", null).create();
        panel.setTag(dialog);
        dialog.show();
    }

    private void dismissAvatarPicker(@NonNull View panel) {
        Object dialog = panel.getTag();
        if (dialog instanceof AlertDialog) ((AlertDialog) dialog).dismiss();
    }

    private void choosePhotoFromGallery() {
        if (activity == null) return;
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        activity.startActivityForResult(Intent.createChooser(intent, "Choose profile photo"), REQUEST_AVATAR_GALLERY);
    }

    private void takePhoto() {
        if (activity == null) return;
        if (context.checkSelfPermission(android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            activity.requestPermissions(new String[]{android.Manifest.permission.CAMERA}, REQUEST_AVATAR_CAMERA);
            Toast.makeText(context, "Allow camera access, then tap your avatar again.", Toast.LENGTH_LONG).show();
            return;
        }
        try {
            cameraPhotoFile = File.createTempFile("profile_avatar_", ".jpg", context.getCacheDir());
            cameraPhotoUri = FileProvider.getUriForFile(context,
                    context.getPackageName() + ".fileprovider", cameraPhotoFile);
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, cameraPhotoUri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            activity.startActivityForResult(intent, REQUEST_AVATAR_CAMERA);
        } catch (Exception e) {
            if (cameraPhotoFile != null) cameraPhotoFile.delete();
            cameraPhotoFile = null;
            cameraPhotoUri = null;
            Toast.makeText(context, "Could not open the camera.", Toast.LENGTH_SHORT).show();
        }
    }

    public boolean onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (resultCode != Activity.RESULT_OK) {
            if (requestCode == REQUEST_AVATAR_CAMERA && cameraPhotoUri != null) {
                if (cameraPhotoFile != null) cameraPhotoFile.delete();
                cameraPhotoFile = null;
                cameraPhotoUri = null;
            }
            return requestCode == REQUEST_AVATAR_CAMERA || requestCode == REQUEST_AVATAR_GALLERY;
        }
        Uri imageUri = requestCode == REQUEST_AVATAR_CAMERA ? cameraPhotoUri
                : (data == null ? null : data.getData());
        cameraPhotoUri = null;
        File capturedFile = requestCode == REQUEST_AVATAR_CAMERA ? cameraPhotoFile : null;
        cameraPhotoFile = null;
        if (requestCode != REQUEST_AVATAR_CAMERA && requestCode != REQUEST_AVATAR_GALLERY) return false;
        if (imageUri == null) {
            if (capturedFile != null) capturedFile.delete();
            Toast.makeText(context, "The selected photo could not be opened.", Toast.LENGTH_SHORT).show();
            return true;
        }
        Toast.makeText(context, "Preparing photo…", Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            byte[] png = ProfileAvatarHelper.makeCircularPng(context, imageUri);
            if (capturedFile != null) capturedFile.delete();
            if (png == null) {
                postToMain(() -> Toast.makeText(context, "Could not prepare this photo. Try another image.", Toast.LENGTH_LONG).show());
                return;
            }
            String encoded = Base64.encodeToString(png, Base64.NO_WRAP);
            new CommunityRemoteSource(context).uploadPhoto(encoded, "profile.png", new CommunityRemoteSource.CommunityCallback<String>() {
                @Override public void onSuccess(String mediaUrl) {
                    if (mediaUrl == null || mediaUrl.isEmpty()) {
                        postToMain(() -> Toast.makeText(context, "Photo upload returned no reference.", Toast.LENGTH_LONG).show());
                    } else {
                        persistAvatarChoice(mediaUrl);
                    }
                }
                @Override public void onError(int statusCode, String message) {
                    postToMain(() -> Toast.makeText(context, "Photo upload failed: " + message, Toast.LENGTH_LONG).show());
                }
            });
        }, "profile-avatar-upload").start();
        return true;
    }

    private void persistAvatarChoice(@NonNull String avatarRef) {
        SessionManager session = SessionManager.getInstance(context);
        UserEntity active = session.getCurrentUser();
        String userId = active != null ? active.getId() : session.getActiveUserId();
        if (session.isGuest()) {
            saveAvatarLocally(userId, avatarRef);
            displayedAvatarRef = avatarRef;
            renderAvatar(avatarRef, userId);
            return;
        }
        new CommunityRemoteSource(context).updateAvatar(avatarRef,
                new CommunityRemoteSource.CommunityCallback<JSONObject>() {
                    @Override public void onSuccess(JSONObject response) {
                        saveAvatarLocally(userId, avatarRef);
                        postToMain(() -> {
                            displayedAvatarRef = avatarRef;
                            renderAvatar(avatarRef, userId);
                            Toast.makeText(context, "Profile picture updated.", Toast.LENGTH_SHORT).show();
                        });
                    }
                    @Override public void onError(int statusCode, String message) {
                        postToMain(() -> Toast.makeText(context, "Could not update profile picture: " + message,
                                Toast.LENGTH_LONG).show());
                    }
                });
    }

    private void saveAvatarLocally(@NonNull String userId, @NonNull String avatarRef) {
        context.getSharedPreferences("profile_avatar_cache", Context.MODE_PRIVATE)
                .edit().putString("avatar:" + userId, avatarRef).apply();
        new Thread(() -> {
            UserEntity entity = PerazimDatabase.getInstance(context).userDao().getUserById(userId);
            if (entity != null) {
                entity.setAvatarUrl(avatarRef);
                entity.setUpdatedAt(System.currentTimeMillis());
                PerazimDatabase.getInstance(context).userDao().update(entity);
                SessionManager.getInstance(context).refreshCurrentUser();
            }
        }, "profile-avatar-save").start();
    }

    // =========================================================================
    // GRAPHICS & UTILITIES
    // =========================================================================

    private int dp(int value) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                value,
                context.getResources().getDisplayMetrics()
        );
    }

    private LinearLayout createCard(int bgColor, int cornerRadiusDp, int borderColor, int borderWidthDp) {
        LinearLayout card = new LinearLayout(context);
        card.setPadding(dp(16), dp(16), dp(16), dp(16));
        if (bgColor == PerazimTheme.COLOR_SURFACE) {
            card.setBackground(PerazimTheme.createCardGradient(dp(cornerRadiusDp), dp(borderWidthDp)));
        } else if (bgColor == PerazimTheme.COLOR_SURFACE_2) {
            card.setBackground(PerazimTheme.createSurface2Gradient(dp(cornerRadiusDp), dp(borderWidthDp)));
        } else {
            GradientDrawable drawable = new GradientDrawable();
            drawable.setColor(bgColor);
            drawable.setCornerRadius(dp(cornerRadiusDp));
            drawable.setStroke(dp(borderWidthDp), borderColor);
            card.setBackground(drawable);
        }
        return card;
    }

    private Drawable createPillDrawable(int bgColor, int strokeColor) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(bgColor);
        g.setCornerRadius(dp(PerazimTheme.RADIUS_PILL));
        g.setStroke(dp(1), strokeColor);
        return g;
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

    private LinearLayout createInteractiveRow(String icon, String label, String value, View.OnClickListener listener) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(8), dp(8), dp(8), dp(8));

        GradientDrawable rowBg = new GradientDrawable();
        rowBg.setColor(PerazimTheme.COLOR_BG_NEUTRAL);
        rowBg.setCornerRadius(dp(PerazimTheme.RADIUS_SM));
        rowBg.setStroke(dp(1), PerazimTheme.COLOR_BORDER_GREY);
        row.setBackground(rowBg);

        LinearLayout.LayoutParams lpRow = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lpRow.setMargins(0, 0, 0, dp(6));
        row.setLayoutParams(lpRow);

        TextView tvIcon = new TextView(context);
        tvIcon.setText(icon);
        tvIcon.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f);
        tvIcon.setPadding(0, 0, dp(8), 0);
        row.addView(tvIcon);

        LinearLayout textCol = new LinearLayout(context);
        textCol.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lpText = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        textCol.setLayoutParams(lpText);

        TextView tvLbl = new TextView(context);
        tvLbl.setText(label);
        tvLbl.setTextSize(TypedValue.COMPLEX_UNIT_SP, PerazimTheme.TEXT_SIZE_MICRO);
        tvLbl.setTextColor(PerazimTheme.COLOR_TEXT_MUTED);
        textCol.addView(tvLbl);

        TextView tvVal = new TextView(context);
        tvVal.setText(value);
        tvVal.setTextSize(TypedValue.COMPLEX_UNIT_SP, PerazimTheme.TEXT_SIZE_BODY_SM);
        tvVal.setTypeface(Typeface.DEFAULT_BOLD);
        tvVal.setTextColor(PerazimTheme.COLOR_PURPLE_DEEP);
        textCol.addView(tvVal);

        row.addView(textCol);

        if (listener != null) {
            TextView tvHint = new TextView(context);
            tvHint.setText("Tap ➔");
            tvHint.setTextSize(TypedValue.COMPLEX_UNIT_SP, PerazimTheme.TEXT_SIZE_MICRO);
            tvHint.setTypeface(Typeface.DEFAULT_BOLD);
            tvHint.setTextColor(PerazimTheme.COLOR_ACCENT_ORANGE);
            row.addView(tvHint);
            row.setOnClickListener(listener);
        }
        return row;
    }

    private void dialPhone(String number) {
        try {
            Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + number));
            context.startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(context, "Phone: " + number, Toast.LENGTH_SHORT).show();
        }
    }

    private void sendEmail(String address, String subject) {
        try {
            Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + address));
            intent.putExtra(Intent.EXTRA_SUBJECT, subject);
            context.startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(context, "Email: " + address, Toast.LENGTH_SHORT).show();
        }
    }

    @Nullable
    private Bitmap loadAssetBitmap(String name, int maxDim) {
        try {
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inJustDecodeBounds = true;
            InputStream is = context.getAssets().open(name);
            BitmapFactory.decodeStream(is, null, opts);
            is.close();

            int sampleSize = 1;
            while ((opts.outWidth / sampleSize) > maxDim || (opts.outHeight / sampleSize) > maxDim) {
                sampleSize *= 2;
            }

            opts.inJustDecodeBounds = false;
            opts.inSampleSize = sampleSize;
            is = context.getAssets().open(name);
            Bitmap bmp = BitmapFactory.decodeStream(is, null, opts);
            is.close();
            return bmp;
        } catch (Exception ignored) {
            return null;
        }
    }

    private Bitmap getRoundedCornerBitmap(Bitmap bitmap, int pixels) {
        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);
        Paint paint = new Paint();
        Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        RectF rectF = new RectF(rect);
        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(Color.BLACK);
        canvas.drawRoundRect(rectF, (float) pixels, (float) pixels, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);
        return output;
    }

    private Bitmap createMonogramBitmap(String text, int sizePx) {
        Bitmap bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(PerazimTheme.COLOR_PRIMARY_PURPLE);
        canvas.drawCircle(sizePx / 2f, sizePx / 2f, sizePx / 2f, paint);

        paint.setColor(PerazimTheme.COLOR_WHITE);
        paint.setTextSize(sizePx * 0.4f);
        paint.setTypeface(Typeface.DEFAULT_BOLD);
        paint.setTextAlign(Paint.Align.CENTER);
        Rect bounds = new Rect();
        paint.getTextBounds(text, 0, text.length(), bounds);
        float y = (sizePx / 2f) - bounds.exactCenterY();
        canvas.drawText(text, sizePx / 2f, y, paint);
        return bmp;
    }

    private void postToMain(Runnable runnable) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            runnable.run();
        } else {
            mainHandler.post(runnable);
        }
    }

    @Nullable
    public User getCurrentUser() {
        return currentUser;
    }

    @Nullable
    public SavedContentData getCurrentSavedContent() {
        return currentSavedContent;
    }
}
