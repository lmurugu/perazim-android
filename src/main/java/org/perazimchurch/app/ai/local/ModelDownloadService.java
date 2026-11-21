package org.perazimchurch.app.ai.local;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

import androidx.core.app.NotificationCompat;

/**
 * Runs the on-phone model download as a foreground service, so battery savers do not stop it when the screen
 * is off or the app is in the background. Resumes from the .part file (HTTP Range) and reports progress.
 */
public final class ModelDownloadService extends Service {

    public static final String ACTION_START = "org.perazimchurch.app.action.MODEL_DOWNLOAD_START";
    public static final String ACTION_CANCEL = "org.perazimchurch.app.action.MODEL_DOWNLOAD_CANCEL";
    private static final String CHANNEL_ID = "model_download";
    private static final int NOTIFICATION_ID = 4107;

    private static volatile boolean running;
    private static volatile int percent;
    private static volatile Listener listener;

    /** Listener for the guide dialog; receives progress while the service is running. */
    public interface Listener {
        void onProgress(int percent);
        void onFinished(String error);
    }

    public static boolean isRunning() { return running; }
    public static int percent() { return percent; }
    public static void setListener(Listener l) { listener = l; }

    public static void start(Context context) {
        Intent intent = new Intent(context, ModelDownloadService.class).setAction(ACTION_START);
        if (Build.VERSION.SDK_INT >= 26) context.startForegroundService(intent);
        else context.startService(intent);
    }

    public static void cancel(Context context) {
        context.startService(new Intent(context, ModelDownloadService.class).setAction(ACTION_CANCEL));
    }

    @Override public IBinder onBind(Intent intent) { return null; }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent == null ? ACTION_START : intent.getAction();
        if (ACTION_CANCEL.equals(action)) {
            OnDeviceAi.cancel();
            return START_NOT_STICKY;
        }
        ensureChannel();
        startForeground(NOTIFICATION_ID, notification("Downloading the on-phone AI model…", 0));
        if (running) return START_NOT_STICKY;
        running = true;
        percent = 0;
        OnDeviceAi.download(this, new OnDeviceAi.DownloadCallback() {
            @Override public void onProgress(int p) {
                percent = p;
                update(p);
                Listener l = listener;
                if (l != null) l.onProgress(p);
            }
            @Override public void onComplete() { finish(null); }
            @Override public void onError(String error) { finish(error == null ? "Download failed." : error); }
        });
        return START_NOT_STICKY;
    }

    private void update(int p) {
        NotificationManager nm = getSystemService(NotificationManager.class);
        if (nm != null) nm.notify(NOTIFICATION_ID, notification("Downloading the on-phone AI model… " + p + "%", p));
    }

    private void finish(String error) {
        running = false;
        percent = error == null ? 100 : percent;
        NotificationManager nm = getSystemService(NotificationManager.class);
        String text = error == null ? "On-phone AI model is ready." : error;
        if (nm != null) nm.notify(NOTIFICATION_ID, notification(text, -1));
        Listener l = listener;
        if (l != null) l.onFinished(error);
        stopForeground(false);
        stopSelf();
    }

    private Notification notification(String text, int progress) {
        Intent cancel = new Intent(this, ModelDownloadService.class).setAction(ACTION_CANCEL);
        PendingIntent cancelPi = PendingIntent.getService(this, 1, cancel,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        NotificationCompat.Builder b = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setContentTitle("Perazim AI model")
                .setContentText(text)
                .setOnlyAlertOnce(true)
                .setOngoing(progress >= 0 && progress < 100);
        if (progress >= 0 && progress < 100) {
            b.setProgress(100, progress, false).addAction(android.R.drawable.ic_menu_close_clear_cancel, "Cancel", cancelPi);
        }
        return b.build();
    }

    private void ensureChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null && nm.getNotificationChannel(CHANNEL_ID) == null) {
                nm.createNotificationChannel(new NotificationChannel(CHANNEL_ID, "On-phone AI model",
                        NotificationManager.IMPORTANCE_LOW));
            }
        }
    }
}
