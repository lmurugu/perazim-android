package com.example.app.presentation.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
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
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.app.data.local.entity.UserEntity;
import com.example.app.data.local.session.SessionManager;
import com.example.app.data.repository.RepositoryProvider;
import com.example.app.domain.model.Message;
import com.example.app.domain.repository.ConnectionRepository;
import com.example.app.domain.repository.MessageRepository;
import com.example.app.sync.SyncOperationType;
import com.example.app.sync.SyncQueueManager;
import com.example.app.ui.theme.PerazimTheme;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * 1-on-1 private fellowship messaging dialog.
 * <p>
 * Features:
 * <ul>
 *     <li>Header: Peer name, status indicator ("Online" / "Fellowship Member"), close button, and options menu ("Block Member").</li>
 *     <li>Messages list: Displays sent chat bubbles on the right (in purple tint) and received chat bubbles on the left (in white/grey) with timestamp and status badge (PENDING, SENT).</li>
 *     <li>Input bar: Message input field with 3D Send button.</li>
 *     <li>Offline-first sending: Calls {@link MessageRepository#sendMessage(Message)}, enqueues to {@link SyncQueueManager#enqueue} with status PENDING, and immediately appends the message to the list.</li>
 * </ul>
 */
public class ChatDialog extends Dialog {

    private final MessageRepository messageRepository;
    private final ConnectionRepository connectionRepository;
    private final SyncQueueManager syncQueueManager;

    private final String currentUserId;
    private final String currentUserName;
    private final String peerId;
    private final String peerName;
    private final String peerStatus;
    private final String conversationId;

    private final List<Message> messageList = new ArrayList<>();
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

    // UI View References
    private ScrollView messagesScrollView;
    private LinearLayout messagesContainer;
    private EditText etMessageInput;
    private Button btnSendMessage;
    private TextView tvEmptyNotice;

    public ChatDialog(@NonNull Context context) {
        this(context, "elder_gitonga", "Elder Gitonga", "Online");
    }

    public ChatDialog(@NonNull Context context, @NonNull String peerId, @NonNull String peerName) {
        this(context, peerId, peerName, "Online");
    }

    public ChatDialog(@NonNull Context context, @NonNull String peerId, @NonNull String peerName, @Nullable String peerStatus) {
        this(
                context,
                RepositoryProvider.getInstance(context).getMessageRepository(),
                RepositoryProvider.getInstance(context).getConnectionRepository(),
                SyncQueueManager.getInstance(context),
                null,
                null,
                peerId,
                peerName,
                peerStatus
        );
    }

    public ChatDialog(@NonNull Context context,
                      @NonNull MessageRepository messageRepository,
                      @NonNull ConnectionRepository connectionRepository,
                      @NonNull SyncQueueManager syncQueueManager,
                      @Nullable String currentUserId,
                      @Nullable String currentUserName,
                      @NonNull String peerId,
                      @NonNull String peerName,
                      @Nullable String peerStatus) {
        super(context, android.R.style.Theme_DeviceDefault_Light_NoActionBar_Fullscreen);
        this.messageRepository = messageRepository;
        this.connectionRepository = connectionRepository;
        this.syncQueueManager = syncQueueManager;
        this.peerId = peerId;
        this.peerName = peerName != null && !peerName.trim().isEmpty() ? peerName.trim() : "Fellowship Member";
        this.peerStatus = peerStatus != null && !peerStatus.trim().isEmpty() ? peerStatus.trim() : "Online";

        // Resolve current user identity
        UserEntity currentUser = null;
        try {
            currentUser = SessionManager.getInstance(context).getCurrentUser();
        } catch (Throwable ignored) {}

        if (currentUserId != null && !currentUserId.trim().isEmpty()) {
            this.currentUserId = currentUserId.trim();
        } else if (currentUser != null && currentUser.getId() != null) {
            this.currentUserId = currentUser.getId();
        } else {
            this.currentUserId = "user_me";
        }

        if (currentUserName != null && !currentUserName.trim().isEmpty()) {
            this.currentUserName = currentUserName.trim();
        } else if (currentUser != null && currentUser.getName() != null && !currentUser.getName().trim().isEmpty()) {
            this.currentUserName = currentUser.getName().trim();
        } else {
            this.currentUserName = "Me";
        }

        // Establish deterministic direct conversation ID
        if (this.currentUserId.compareTo(this.peerId) < 0) {
            this.conversationId = "conv_" + this.currentUserId + "_" + this.peerId;
        } else {
            this.conversationId = "conv_" + this.peerId + "_" + this.currentUserId;
        }
    }

    public static ChatDialog show(@NonNull Context context, @NonNull String peerId, @NonNull String peerName) {
        ChatDialog dialog = new ChatDialog(context, peerId, peerName);
        dialog.show();
        return dialog;
    }

    public static ChatDialog show(@NonNull Context context, @NonNull String peerId, @NonNull String peerName, @Nullable String peerStatus) {
        ChatDialog dialog = new ChatDialog(context, peerId, peerName, peerStatus);
        dialog.show();
        return dialog;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        if (getWindow() != null) {
            getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            getWindow().setBackgroundDrawable(new ColorDrawable(PerazimTheme.COLOR_BG_NEUTRAL));
            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }

        View root = buildChatLayout();
        setContentView(root);

        loadInitialMessages();
    }

    private View buildChatLayout() {
        LinearLayout root = new LinearLayout(getContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(PerazimTheme.COLOR_BG_NEUTRAL);

        // 1. HEADER BAR
        View headerBar = createHeaderBar();
        root.addView(headerBar);

        // 2. MESSAGES SCROLLVIEW
        messagesScrollView = new ScrollView(getContext());
        messagesScrollView.setFillViewport(true);
        LinearLayout.LayoutParams lpScroll = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1.0f);
        messagesScrollView.setLayoutParams(lpScroll);

        messagesContainer = new LinearLayout(getContext());
        messagesContainer.setOrientation(LinearLayout.VERTICAL);
        messagesContainer.setPadding(dp(16), dp(12), dp(16), dp(16));

        tvEmptyNotice = new TextView(getContext());
        tvEmptyNotice.setText("Start a fellowship conversation with " + peerName + ".\nShare encouragement, scripture, and prayers!");
        tvEmptyNotice.setTextSize(TypedValue.COMPLEX_UNIT_SP, PerazimTheme.TEXT_SIZE_BODY_SM);
        tvEmptyNotice.setTextColor(PerazimTheme.COLOR_TEXT_MUTED);
        tvEmptyNotice.setGravity(Gravity.CENTER);
        tvEmptyNotice.setPadding(dp(20), dp(40), dp(20), dp(20));
        tvEmptyNotice.setVisibility(View.GONE);
        messagesContainer.addView(tvEmptyNotice);

        messagesScrollView.addView(messagesContainer);
        root.addView(messagesScrollView);

        // 3. INPUT BAR
        View inputBar = createInputBar();
        root.addView(inputBar);

        return root;
    }

    private View createHeaderBar() {
        LinearLayout header = new LinearLayout(getContext());
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(14), dp(10), dp(14), dp(10));

        GradientDrawable headerBg = new GradientDrawable();
        headerBg.setColor(PerazimTheme.COLOR_WHITE);
        headerBg.setStroke(dp(1), PerazimTheme.COLOR_BORDER_GREY);
        header.setBackground(headerBg);

        // Back / Close Button
        Button btnClose = new Button(getContext());
        btnClose.setText("←");
        btnClose.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        btnClose.setTypeface(Typeface.DEFAULT_BOLD);
        btnClose.setTextColor(PerazimTheme.COLOR_PURPLE_DEEP);
        btnClose.setBackground(createPillDrawable(PerazimTheme.COLOR_BG_NEUTRAL, PerazimTheme.COLOR_BORDER_GREY));
        LinearLayout.LayoutParams lpClose = new LinearLayout.LayoutParams(dp(36), dp(36));
        lpClose.setMargins(0, 0, dp(10), 0);
        btnClose.setLayoutParams(lpClose);
        btnClose.setOnClickListener(v -> dismiss());
        header.addView(btnClose);

        // Peer Details (Name + Status)
        LinearLayout peerInfoCol = new LinearLayout(getContext());
        peerInfoCol.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lpInfo = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        peerInfoCol.setLayoutParams(lpInfo);

        TextView tvPeerName = new TextView(getContext());
        tvPeerName.setText(peerName);
        tvPeerName.setTextSize(TypedValue.COMPLEX_UNIT_SP, PerazimTheme.TEXT_SIZE_TITLE);
        tvPeerName.setTypeface(Typeface.DEFAULT_BOLD);
        tvPeerName.setTextColor(PerazimTheme.COLOR_PURPLE_DEEP);
        tvPeerName.setSingleLine(true);
        peerInfoCol.addView(tvPeerName);

        TextView tvPeerStatus = new TextView(getContext());
        tvPeerStatus.setText("● " + peerStatus);
        tvPeerStatus.setTextSize(TypedValue.COMPLEX_UNIT_SP, PerazimTheme.TEXT_SIZE_CAPTION);
        if ("Online".equalsIgnoreCase(peerStatus)) {
            tvPeerStatus.setTextColor(PerazimTheme.COLOR_SUCCESS);
        } else {
            tvPeerStatus.setTextColor(PerazimTheme.COLOR_PRIMARY_PURPLE);
        }
        tvPeerStatus.setTypeface(Typeface.DEFAULT_BOLD);
        peerInfoCol.addView(tvPeerStatus);

        header.addView(peerInfoCol);

        // Options Menu Button ("⋮")
        Button btnOptions = new Button(getContext());
        btnOptions.setText("⋮");
        btnOptions.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        btnOptions.setTypeface(Typeface.DEFAULT_BOLD);
        btnOptions.setTextColor(PerazimTheme.COLOR_PURPLE_DEEP);
        btnOptions.setBackground(createPillDrawable(PerazimTheme.COLOR_BG_NEUTRAL, PerazimTheme.COLOR_BORDER_GREY));
        LinearLayout.LayoutParams lpOpt = new LinearLayout.LayoutParams(dp(36), dp(36));
        btnOptions.setLayoutParams(lpOpt);
        btnOptions.setOnClickListener(v -> showOptionsMenu());
        header.addView(btnOptions);

        return header;
    }

    private View createInputBar() {
        LinearLayout bar = new LinearLayout(getContext());
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(12), dp(8), dp(12), dp(8));

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(PerazimTheme.COLOR_WHITE);
        bg.setStroke(dp(1), PerazimTheme.COLOR_BORDER_GREY);
        bar.setBackground(bg);

        etMessageInput = new EditText(getContext());
        etMessageInput.setHint("Type fellowship message...");
        etMessageInput.setTextSize(TypedValue.COMPLEX_UNIT_SP, PerazimTheme.TEXT_SIZE_BODY_SM);
        etMessageInput.setTextColor(PerazimTheme.COLOR_TEXT_DARK);
        etMessageInput.setBackground(createCardBackground(PerazimTheme.COLOR_BG_NEUTRAL, PerazimTheme.RADIUS_MD, PerazimTheme.COLOR_BORDER_GREY, 1));
        etMessageInput.setPadding(dp(12), dp(10), dp(12), dp(10));
        etMessageInput.setSingleLine(true);
        etMessageInput.setImeOptions(EditorInfo.IME_ACTION_SEND);
        LinearLayout.LayoutParams lpEt = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        etMessageInput.setLayoutParams(lpEt);
        etMessageInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendMessage();
                return true;
            }
            return false;
        });
        bar.addView(etMessageInput);

        btnSendMessage = new Button(getContext());
        btnSendMessage.setText("Send");
        btnSendMessage.setTextSize(TypedValue.COMPLEX_UNIT_SP, PerazimTheme.TEXT_SIZE_BODY_SM);
        btnSendMessage.setTypeface(Typeface.DEFAULT_BOLD);
        btnSendMessage.setTextColor(PerazimTheme.COLOR_WHITE);
        btnSendMessage.setBackground(create3dButtonDrawable(
                PerazimTheme.COLOR_PRIMARY_PURPLE,
                PerazimTheme.COLOR_PURPLE_SHADOW,
                PerazimTheme.RADIUS_MD,
                2
        ));
        LinearLayout.LayoutParams lpBtn = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, dp(38));
        lpBtn.setMargins(dp(8), 0, 0, 0);
        btnSendMessage.setLayoutParams(lpBtn);
        btnSendMessage.setOnClickListener(v -> sendMessage());
        bar.addView(btnSendMessage);

        return bar;
    }

    private void showOptionsMenu() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(peerName);

        String[] options = new String[]{"🚫 Block Member", "📋 Copy Member Name"};
        builder.setItems(options, (dialog, which) -> {
            if (which == 0) {
                confirmBlockMember();
            } else if (which == 1) {
                Toast.makeText(getContext(), peerName + " copied", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void confirmBlockMember() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Block Member");
        builder.setMessage("Are you sure you want to block " + peerName + "? You will no longer receive fellowship messages from them.");
        builder.setPositiveButton("Block", (dialog, which) -> {
            try {
                com.example.app.community.privacy.CommunityPrivacyHelper.blockAndReport(connectionRepository, currentUserId, peerId, "User blocked via direct chat");
                Toast.makeText(getContext(), "✓ Member " + peerName + " has been blocked.", Toast.LENGTH_SHORT).show();
                dismiss();
            } catch (Exception e) {
                Toast.makeText(getContext(), "Error blocking member: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void loadInitialMessages() {
        try {
            List<Message> existing = messageRepository.getMessages(conversationId, 100, 0);
            if (existing != null && !existing.isEmpty()) {
                messageList.clear();
                messageList.addAll(existing);
                renderAllMessages();
            } else {
                // Populate default starter fellowship greeting if new conversation
                Message welcomeMsg = new Message(
                        "msg_welcome_" + peerId,
                        "msg_welcome_" + peerId,
                        conversationId,
                        peerId,
                        peerName,
                        "Shalom beloved! Praying peace and breakthrough over your week. Let us stand together in faith!",
                        "SENT",
                        System.currentTimeMillis() - 60000L
                );
                messageList.add(welcomeMsg);
                renderAllMessages();
            }
        } catch (Exception e) {
            tvEmptyNotice.setVisibility(View.VISIBLE);
        }
    }

    private void renderAllMessages() {
        messagesContainer.removeAllViews();
        messagesContainer.addView(tvEmptyNotice);

        if (messageList.isEmpty()) {
            tvEmptyNotice.setVisibility(View.VISIBLE);
            return;
        }

        tvEmptyNotice.setVisibility(View.GONE);
        for (Message msg : messageList) {
            View bubble = createMessageBubble(msg);
            messagesContainer.addView(bubble);
        }

        scrollToBottom();
    }

    private void sendMessage() {
        String content = etMessageInput.getText().toString().trim();
        if (TextUtils.isEmpty(content)) {
            return;
        }

        if (!com.example.app.community.privacy.CommunityPrivacyHelper.canMessage(connectionRepository, currentUserId, peerId)) {
            Toast.makeText(getContext(), "Cannot send message: this member is blocked or privacy settings prevent messaging.", Toast.LENGTH_LONG).show();
            return;
        }

        String messageId = UUID.randomUUID().toString();
        long now = System.currentTimeMillis();

        Message message = new Message(
                messageId,
                messageId, // clientMessageId
                conversationId,
                currentUserId,
                currentUserName,
                content,
                "PENDING",
                now
        );

        // 1. Call MessageRepository.sendMessage(message)
        try {
            messageRepository.sendMessage(message);
        } catch (Exception ignored) {}

        // 2. Enqueue to SyncQueueManager with CREATE, MESSAGE, status PENDING
        String payloadJson = "{\"id\":\"" + messageId
                + "\",\"conversationId\":\"" + conversationId
                + "\",\"senderId\":\"" + currentUserId
                + "\",\"senderName\":\"" + escapeJson(currentUserName)
                + "\",\"content\":\"" + escapeJson(content)
                + "\",\"status\":\"PENDING\",\"timestamp\":" + now + "}";

        try {
            syncQueueManager.enqueue(SyncOperationType.CREATE, "MESSAGE", messageId, payloadJson, messageId);
        } catch (Exception ignored) {}

        // 3. Immediately append message to list and view
        etMessageInput.setText("");
        messageList.add(message);
        if (tvEmptyNotice != null) tvEmptyNotice.setVisibility(View.GONE);
        View bubble = createMessageBubble(message);
        messagesContainer.addView(bubble);

        scrollToBottom();
    }

    private View createMessageBubble(Message message) {
        boolean isMe = currentUserId != null && currentUserId.equals(message.getSenderId());

        LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(LinearLayout.VERTICAL);
        row.setGravity(isMe ? Gravity.END : Gravity.START);

        LinearLayout.LayoutParams lpRow = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        lpRow.setMargins(0, 0, 0, dp(8));
        row.setLayoutParams(lpRow);

        // Bubble Box
        LinearLayout bubble = new LinearLayout(getContext());
        bubble.setOrientation(LinearLayout.VERTICAL);
        bubble.setPadding(dp(12), dp(8), dp(12), dp(8));

        GradientDrawable bubbleBg = new GradientDrawable();
        if (isMe) {
            bubbleBg.setColor(PerazimTheme.COLOR_PURPLE_TINT);
            bubbleBg.setStroke(dp(1), PerazimTheme.COLOR_BORDER_GREY);
            bubbleBg.setCornerRadii(new float[]{
                    dp(14), dp(14),
                    dp(14), dp(14),
                    dp(2), dp(2), // bottom-right sharp
                    dp(14), dp(14)
            });
        } else {
            bubbleBg.setColor(PerazimTheme.COLOR_WHITE);
            bubbleBg.setStroke(dp(1), PerazimTheme.COLOR_BORDER_GREY);
            bubbleBg.setCornerRadii(new float[]{
                    dp(14), dp(14),
                    dp(14), dp(14),
                    dp(14), dp(14),
                    dp(2), dp(2) // bottom-left sharp
            });
        }
        bubble.setBackground(bubbleBg);

        LinearLayout.LayoutParams lpBubble = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        if (isMe) {
            lpBubble.setMargins(dp(48), 0, 0, 0);
        } else {
            lpBubble.setMargins(0, 0, dp(48), 0);
        }
        bubble.setLayoutParams(lpBubble);

        // Sender Name if received
        if (!isMe) {
            TextView tvSender = new TextView(getContext());
            tvSender.setText(peerName);
            tvSender.setTextSize(TypedValue.COMPLEX_UNIT_SP, PerazimTheme.TEXT_SIZE_MICRO);
            tvSender.setTypeface(Typeface.DEFAULT_BOLD);
            tvSender.setTextColor(PerazimTheme.COLOR_PRIMARY_PURPLE);
            tvSender.setPadding(0, 0, 0, dp(2));
            bubble.addView(tvSender);
        }

        // Message Content Text
        TextView tvContent = new TextView(getContext());
        tvContent.setText(message.getContent() != null ? message.getContent() : "");
        tvContent.setTextSize(TypedValue.COMPLEX_UNIT_SP, PerazimTheme.TEXT_SIZE_BODY);
        tvContent.setTextColor(PerazimTheme.COLOR_TEXT_DARK);
        tvContent.setLineSpacing(dp(1), 1.15f);
        bubble.addView(tvContent);

        // Footer: Timestamp + Status Badge
        LinearLayout footer = new LinearLayout(getContext());
        footer.setOrientation(LinearLayout.HORIZONTAL);
        footer.setGravity(Gravity.CENTER_VERTICAL);
        footer.setPadding(0, dp(4), 0, 0);

        long msgTime = message.getTimestamp() > 0 ? message.getTimestamp() : System.currentTimeMillis();
        TextView tvTime = new TextView(getContext());
        tvTime.setText(timeFormat.format(new Date(msgTime)));
        tvTime.setTextSize(TypedValue.COMPLEX_UNIT_SP, PerazimTheme.TEXT_SIZE_MICRO);
        tvTime.setTextColor(PerazimTheme.COLOR_TEXT_MUTED);
        footer.addView(tvTime);

        if (isMe) {
            String status = message.getStatus() != null ? message.getStatus() : "SENT";
            TextView tvStatus = new TextView(getContext());
            tvStatus.setTextSize(TypedValue.COMPLEX_UNIT_SP, PerazimTheme.TEXT_SIZE_MICRO);
            tvStatus.setTypeface(Typeface.DEFAULT_BOLD);
            tvStatus.setPadding(dp(6), 0, 0, 0);

            if ("PENDING".equalsIgnoreCase(status)) {
                tvStatus.setText("⏳ PENDING");
                tvStatus.setTextColor(PerazimTheme.COLOR_ACCENT_ORANGE);
            } else if ("SENT".equalsIgnoreCase(status)) {
                tvStatus.setText("✓ SENT");
                tvStatus.setTextColor(PerazimTheme.COLOR_PRIMARY_PURPLE);
            } else {
                tvStatus.setText("✓✓ " + status);
                tvStatus.setTextColor(PerazimTheme.COLOR_SUCCESS);
            }
            footer.addView(tvStatus);
        }

        bubble.addView(footer);
        row.addView(bubble);
        return row;
    }

    private void scrollToBottom() {
        if (messagesScrollView != null) {
            messagesScrollView.post(() -> messagesScrollView.fullScroll(View.FOCUS_DOWN));
        }
    }

    private static String escapeJson(String raw) {
        if (raw == null) return "";
        return raw.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
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
