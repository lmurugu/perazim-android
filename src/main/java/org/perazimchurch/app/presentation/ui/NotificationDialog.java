package org.perazimchurch.app.presentation.ui;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.perazimchurch.app.data.local.entity.UserEntity;
import org.perazimchurch.app.data.local.session.SessionManager;
import org.perazimchurch.app.data.repository.RepositoryProvider;
import org.perazimchurch.app.domain.model.Notification;
import org.perazimchurch.app.domain.repository.NotificationRepository;
import org.perazimchurch.app.presentation.viewmodel.FellowshipViewModel;
import org.perazimchurch.app.ui.theme.PerazimTheme;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Modal dialog listing notifications from {@link NotificationRepository#getNotifications(String)}.
 * <p>
 * Displays a list of in-app notifications with unread dots, titles, bodies, and timestamps.
 * Provides a "Mark All as Read" action and marks individual notifications as read on tap.
 */
public class NotificationDialog extends Dialog {

    private final NotificationRepository notificationRepo;
    private final String userId;
    private final Runnable onDismissCallback;
    private final Executor executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d, h:mm a", Locale.getDefault());

    private final List<Notification> notificationList = new ArrayList<>();
    private LinearLayout listContainer;
    private ProgressBar loadingSpinner;
    private TextView tvEmptyState;
    private TextView tvUnreadHeaderCount;

    public NotificationDialog(@NonNull Context context) {
        this(
                context,
                RepositoryProvider.getInstance(context).getNotificationRepository(),
                resolveCurrentUserId(context),
                null
        );
    }

    public NotificationDialog(@NonNull Context context,
                              @NonNull NotificationRepository notificationRepo,
                              @NonNull String userId) {
        this(context, notificationRepo, userId, null);
    }

    public NotificationDialog(@NonNull Context context,
                              @NonNull FellowshipViewModel viewModel,
                              @NonNull String userId) {
        this(context, viewModel.getNotificationRepo() != null
                        ? viewModel.getNotificationRepo()
                        : RepositoryProvider.getInstance(context).getNotificationRepository(),
                userId,
                null);
    }

    public NotificationDialog(@NonNull Context context,
                              @NonNull NotificationRepository notificationRepo,
                              @NonNull String userId,
                              @Nullable Runnable onDismissCallback) {
        super(context);
        this.notificationRepo = notificationRepo;
        this.userId = userId != null && !userId.trim().isEmpty() ? userId.trim() : "user_active";
        this.onDismissCallback = onDismissCallback;
    }

    private static String resolveCurrentUserId(Context context) {
        try {
            UserEntity user = SessionManager.getInstance(context).getCurrentUser();
            if (user != null && user.getId() != null) {
                return user.getId();
            }
        } catch (Throwable ignored) {
        }
        return "user_active";
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        if (getWindow() != null) {
            getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        View root = buildContentView();
        setContentView(root);

        setOnDismissListener(dialog -> {
            if (onDismissCallback != null) {
                onDismissCallback.run();
            }
        });

        loadNotifications();
    }

    @NonNull
    private View buildContentView() {
        // Outer wrapper with margin & card shape
        LinearLayout card = new LinearLayout(getContext());
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(createCardBackground(PerazimTheme.COLOR_BG_NEUTRAL, PerazimTheme.RADIUS_LG, PerazimTheme.COLOR_BORDER_GREY, 1));
        card.setPadding(dp(16), dp(16), dp(16), dp(16));

        // 1. Header Bar: Title, Dismiss "✕"
        LinearLayout headerBar = new LinearLayout(getContext());
        headerBar.setOrientation(LinearLayout.HORIZONTAL);
        headerBar.setGravity(Gravity.CENTER_VERTICAL);
        headerBar.setPadding(0, 0, 0, dp(10));

        TextView tvTitle = new TextView(getContext());
        tvTitle.setText("🔔 Notifications");
        tvTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, PerazimTheme.TEXT_SIZE_TITLE);
        tvTitle.setTypeface(Typeface.DEFAULT_BOLD);
        tvTitle.setTextColor(PerazimTheme.COLOR_PURPLE_DEEP);
        LinearLayout.LayoutParams lpTitle = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        tvTitle.setLayoutParams(lpTitle);
        headerBar.addView(tvTitle);

        Button btnClose = new Button(getContext());
        btnClose.setText("✕");
        btnClose.setTextSize(TypedValue.COMPLEX_UNIT_SP, PerazimTheme.TEXT_SIZE_BODY);
        btnClose.setTextColor(PerazimTheme.COLOR_TEXT_MUTED);
        btnClose.setBackground(null);
        btnClose.setPadding(dp(8), dp(4), dp(8), dp(4));
        btnClose.setOnClickListener(v -> dismiss());
        headerBar.addView(btnClose);

        card.addView(headerBar);

        // 2. Action bar with unread summary & "Mark All as Read" button
        LinearLayout subHeaderBar = new LinearLayout(getContext());
        subHeaderBar.setOrientation(LinearLayout.HORIZONTAL);
        subHeaderBar.setGravity(Gravity.CENTER_VERTICAL);
        subHeaderBar.setPadding(0, 0, 0, dp(12));

        tvUnreadHeaderCount = new TextView(getContext());
        tvUnreadHeaderCount.setText("Fellowship updates & announcements");
        tvUnreadHeaderCount.setTextSize(TypedValue.COMPLEX_UNIT_SP, PerazimTheme.TEXT_SIZE_CAPTION);
        tvUnreadHeaderCount.setTextColor(PerazimTheme.COLOR_TEXT_MUTED);
        LinearLayout.LayoutParams lpUnread = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        tvUnreadHeaderCount.setLayoutParams(lpUnread);
        subHeaderBar.addView(tvUnreadHeaderCount);

        Button btnMarkAllRead = new Button(getContext());
        btnMarkAllRead.setText("✓ Mark All Read");
        btnMarkAllRead.setTextSize(TypedValue.COMPLEX_UNIT_SP, PerazimTheme.TEXT_SIZE_MICRO);
        btnMarkAllRead.setTypeface(Typeface.DEFAULT_BOLD);
        btnMarkAllRead.setTextColor(PerazimTheme.COLOR_PRIMARY_PURPLE);
        btnMarkAllRead.setBackground(createPillDrawable(PerazimTheme.COLOR_PURPLE_TINT, PerazimTheme.COLOR_BORDER_GREY));
        btnMarkAllRead.setPadding(dp(10), dp(4), dp(10), dp(4));
        btnMarkAllRead.setOnClickListener(v -> markAllAsRead());
        subHeaderBar.addView(btnMarkAllRead);

        card.addView(subHeaderBar);

        // Divider
        View divider = new View(getContext());
        divider.setBackgroundColor(PerazimTheme.COLOR_BORDER_GREY);
        divider.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1)));
        card.addView(divider);

        // 3. Scrollable Notifications Container
        ScrollView scrollView = new ScrollView(getContext());
        scrollView.setFillViewport(true);
        LinearLayout.LayoutParams lpScroll = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(380)
        );
        lpScroll.setMargins(0, dp(8), 0, 0);
        scrollView.setLayoutParams(lpScroll);

        listContainer = new LinearLayout(getContext());
        listContainer.setOrientation(LinearLayout.VERTICAL);
        listContainer.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        // Loading spinner
        loadingSpinner = new ProgressBar(getContext());
        LinearLayout.LayoutParams lpSpinner = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        lpSpinner.gravity = Gravity.CENTER;
        lpSpinner.setMargins(0, dp(32), 0, dp(32));
        loadingSpinner.setLayoutParams(lpSpinner);
        listContainer.addView(loadingSpinner);

        // Empty state view: "No new notifications. You're all caught up!"
        tvEmptyState = new TextView(getContext());
        tvEmptyState.setText("🕊️ No new notifications. You're all caught up!");
        tvEmptyState.setTextSize(TypedValue.COMPLEX_UNIT_SP, PerazimTheme.TEXT_SIZE_BODY_SM);
        tvEmptyState.setTextColor(PerazimTheme.COLOR_TEXT_MUTED);
        tvEmptyState.setGravity(Gravity.CENTER);
        tvEmptyState.setPadding(dp(16), dp(48), dp(16), dp(48));
        tvEmptyState.setVisibility(View.GONE);
        listContainer.addView(tvEmptyState);

        scrollView.addView(listContainer);
        card.addView(scrollView);

        return card;
    }

    private void loadNotifications() {
        if (loadingSpinner != null) {
            loadingSpinner.setVisibility(View.VISIBLE);
        }
        if (tvEmptyState != null) {
            tvEmptyState.setVisibility(View.GONE);
        }

        executor.execute(() -> {
            try {
                List<Notification> items = notificationRepo != null
                        ? notificationRepo.getNotifications(userId)
                        : null;
                mainHandler.post(() -> {
                    if (loadingSpinner != null) {
                        loadingSpinner.setVisibility(View.GONE);
                    }
                    notificationList.clear();
                    if (items != null) {
                        notificationList.addAll(items);
                    }
                    renderNotifications();
                });
            } catch (Exception e) {
                mainHandler.post(() -> {
                    if (loadingSpinner != null) {
                        loadingSpinner.setVisibility(View.GONE);
                    }
                    renderNotifications();
                });
            }
        });
    }

    private void renderNotifications() {
        if (listContainer == null) return;

        // Clear existing card views, keeping loading spinner & empty state view
        for (int i = listContainer.getChildCount() - 1; i >= 0; i--) {
            View child = listContainer.getChildAt(i);
            if (child != loadingSpinner && child != tvEmptyState) {
                listContainer.removeViewAt(i);
            }
        }

        if (notificationList.isEmpty()) {
            if (tvEmptyState != null) {
                tvEmptyState.setVisibility(View.VISIBLE);
            }
            if (tvUnreadHeaderCount != null) {
                tvUnreadHeaderCount.setText("All caught up");
            }
            return;
        }

        if (tvEmptyState != null) {
            tvEmptyState.setVisibility(View.GONE);
        }

        int unreadCount = 0;
        for (Notification notif : notificationList) {
            if (!notif.isRead()) {
                unreadCount++;
            }
            View card = createNotificationCard(notif);
            listContainer.addView(card);
        }

        if (tvUnreadHeaderCount != null) {
            if (unreadCount > 0) {
                tvUnreadHeaderCount.setText(unreadCount + " unread update" + (unreadCount > 1 ? "s" : ""));
            } else {
                tvUnreadHeaderCount.setText("All updates read");
            }
        }
    }

    @NonNull
    private View createNotificationCard(@NonNull Notification notification) {
        final boolean isUnread = !notification.isRead();

        LinearLayout card = new LinearLayout(getContext());
        card.setOrientation(LinearLayout.VERTICAL);
        card.setClickable(true);
        card.setFocusable(true);

        int bgColor = isUnread ? PerazimTheme.COLOR_PURPLE_TINT : PerazimTheme.COLOR_WHITE;
        int borderColor = isUnread ? PerazimTheme.COLOR_PRIMARY_PURPLE : PerazimTheme.COLOR_BORDER_GREY;
        card.setBackground(createCardBackground(bgColor, PerazimTheme.RADIUS_MD, borderColor, isUnread ? 2 : 1));
        card.setPadding(dp(12), dp(10), dp(12), dp(10));

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        lp.setMargins(0, dp(6), 0, dp(6));
        card.setLayoutParams(lp);

        // Header Row inside Card: [Unread Dot] [Type Badge] [Timestamp]
        LinearLayout metaRow = new LinearLayout(getContext());
        metaRow.setOrientation(LinearLayout.HORIZONTAL);
        metaRow.setGravity(Gravity.CENTER_VERTICAL);
        metaRow.setPadding(0, 0, 0, dp(4));

        // Unread indicator dot
        final View dot = new View(getContext());
        int dotSize = dp(8);
        LinearLayout.LayoutParams lpDot = new LinearLayout.LayoutParams(dotSize, dotSize);
        lpDot.setMargins(0, 0, dp(6), 0);
        dot.setLayoutParams(lpDot);
        dot.setBackground(createCircleDrawable(PerazimTheme.COLOR_ACCENT_ORANGE));
        dot.setVisibility(isUnread ? View.VISIBLE : View.GONE);
        metaRow.addView(dot);

        // Type badge
        TextView tvType = new TextView(getContext());
        String typeLabel = notification.getType() != null ? notification.getType().toUpperCase(Locale.getDefault()) : "UPDATE";
        tvType.setText(typeLabel);
        tvType.setTextSize(TypedValue.COMPLEX_UNIT_SP, PerazimTheme.TEXT_SIZE_MICRO);
        tvType.setTypeface(Typeface.DEFAULT_BOLD);
        tvType.setTextColor(PerazimTheme.COLOR_PRIMARY_PURPLE);
        metaRow.addView(tvType);

        // Spacer
        View spacer = new View(getContext());
        LinearLayout.LayoutParams lpSpacer = new LinearLayout.LayoutParams(0, 1, 1.0f);
        spacer.setLayoutParams(lpSpacer);
        metaRow.addView(spacer);

        // Timestamp
        TextView tvTime = new TextView(getContext());
        long ts = notification.getTimestamp() > 0 ? notification.getTimestamp() : System.currentTimeMillis();
        tvTime.setText(dateFormat.format(new Date(ts)));
        tvTime.setTextSize(TypedValue.COMPLEX_UNIT_SP, PerazimTheme.TEXT_SIZE_MICRO);
        tvTime.setTextColor(PerazimTheme.COLOR_TEXT_MUTED);
        metaRow.addView(tvTime);

        card.addView(metaRow);

        // Notification Title
        TextView tvCardTitle = new TextView(getContext());
        tvCardTitle.setText(notification.getTitle() != null ? notification.getTitle() : "Fellowship Notification");
        tvCardTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, PerazimTheme.TEXT_SIZE_BODY_SM);
        tvCardTitle.setTypeface(Typeface.DEFAULT_BOLD);
        tvCardTitle.setTextColor(PerazimTheme.COLOR_TEXT_DARK);
        card.addView(tvCardTitle);

        // Notification Body
        String bodyText = notification.getBody() != null ? notification.getBody() : notification.getMessage();
        if (bodyText != null && !bodyText.isEmpty()) {
            TextView tvBody = new TextView(getContext());
            tvBody.setText(bodyText);
            tvBody.setTextSize(TypedValue.COMPLEX_UNIT_SP, PerazimTheme.TEXT_SIZE_CAPTION);
            tvBody.setTextColor(PerazimTheme.COLOR_TEXT_MUTED);
            tvBody.setPadding(0, dp(2), 0, 0);
            tvBody.setLineSpacing(dp(1), 1.15f);
            card.addView(tvBody);
        }

        // Tapping a notification calls notificationRepo.markAsRead(...)
        card.setOnClickListener(v -> {
            if (!notification.isRead()) {
                notification.setRead(true);
                dot.setVisibility(View.GONE);
                card.setBackground(createCardBackground(PerazimTheme.COLOR_WHITE, PerazimTheme.RADIUS_MD, PerazimTheme.COLOR_BORDER_GREY, 1));

                executor.execute(() -> {
                    try {
                        if (notificationRepo != null) {
                            notificationRepo.markAsRead(notification.getId());
                        }
                    } catch (Exception ignored) {
                    }
                });

                Toast.makeText(getContext(), "Marked as read", Toast.LENGTH_SHORT).show();
                updateUnreadSummary();
            }
        });

        return card;
    }

    private void markAllAsRead() {
        if (notificationList.isEmpty()) return;

        executor.execute(() -> {
            try {
                if (notificationRepo != null) {
                    notificationRepo.markAllAsRead(userId);
                }
                mainHandler.post(() -> {
                    for (Notification n : notificationList) {
                        n.setRead(true);
                    }
                    renderNotifications();
                    Toast.makeText(getContext(), "✓ All notifications marked as read.", Toast.LENGTH_SHORT).show();
                });
            } catch (Exception e) {
                mainHandler.post(() -> Toast.makeText(getContext(), "Failed to mark all as read", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void updateUnreadSummary() {
        int count = 0;
        for (Notification n : notificationList) {
            if (!n.isRead()) count++;
        }
        if (tvUnreadHeaderCount != null) {
            if (count > 0) {
                tvUnreadHeaderCount.setText(count + " unread update" + (count > 1 ? "s" : ""));
            } else {
                tvUnreadHeaderCount.setText("All updates read");
            }
        }
    }

    private int dp(int value) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                value,
                getContext().getResources().getDisplayMetrics()
        );
    }

    private Drawable createCardBackground(int bgColor, int cornerRadiusDp, int borderColor, int borderWidthDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(bgColor);
        drawable.setCornerRadius(dp(cornerRadiusDp));
        drawable.setStroke(dp(borderWidthDp), borderColor);
        return drawable;
    }

    private Drawable createPillDrawable(int bgColor, int strokeColor) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(bgColor);
        g.setCornerRadius(dp(PerazimTheme.RADIUS_PILL));
        g.setStroke(dp(1), strokeColor);
        return g;
    }

    private Drawable createCircleDrawable(int color) {
        GradientDrawable g = new GradientDrawable();
        g.setShape(GradientDrawable.OVAL);
        g.setColor(color);
        return g;
    }
}
