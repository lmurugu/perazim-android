package org.perazimchurch.app.data.repository;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.Xml;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Consumer;

import org.perazimchurch.app.domain.model.Sermon;
import org.xmlpull.v1.XmlPullParser;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Repository for official Perazim Church Embu YouTube broadcast integration.
 * Fetches and parses the live Atom feed from @perazimchurchembu (Channel ID: UC2Yv5gJArnHSuXApBupkv-A)
 * and provides intent launchers for live video streaming and WhatsApp community sharing.
 */
public class YouTubeFeedRepository {

    private static final String TAG = "YouTubeFeedRepo";
    public static final String PERAZIM_CHANNEL_ID = "UC2Yv5gJArnHSuXApBupkv-A";
    public static final String PERAZIM_CHANNEL_HANDLE = "@perazimchurchembu";
    public static final String YOUTUBE_FEED_URL = "https://www.youtube.com/feeds/videos.xml?channel_id=" + PERAZIM_CHANNEL_ID;
    public static final String YOUTUBE_LIVE_URL = "https://www.youtube.com/@perazimchurchembu/live";

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

    private static final List<Sermon> CACHED_VIDEOS = new ArrayList<>();

    /**
     * Asynchronously fetches recent sermon broadcasts from the official Perazim YouTube channel feed.
     */
    public static void fetchRecentVideos(@NonNull Consumer<List<Sermon>> callback) {
        EXECUTOR.execute(() -> {
            List<Sermon> result = fetchVideosFromFeedInternal();
            if (result != null && !result.isEmpty()) {
                synchronized (CACHED_VIDEOS) {
                    CACHED_VIDEOS.clear();
                    CACHED_VIDEOS.addAll(result);
                }
            } else {
                synchronized (CACHED_VIDEOS) {
                    if (!CACHED_VIDEOS.isEmpty()) {
                        result = new ArrayList<>(CACHED_VIDEOS);
                    }
                }
            }
            final List<Sermon> finalResult = result != null ? result : Collections.emptyList();
            MAIN_HANDLER.post(() -> callback.accept(finalResult));
        });
    }

    @Nullable
    private static List<Sermon> fetchVideosFromFeedInternal() {
        HttpURLConnection conn = null;
        InputStream in = null;
        try {
            URL url = new URL(YOUTUBE_FEED_URL);
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(8000);
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "PerazimChurchAndroidApp/2.1");

            int responseCode = conn.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                Log.w(TAG, "YouTube feed returned HTTP: " + responseCode);
                return null;
            }

            in = conn.getInputStream();
            return parseAtomFeed(in);
        } catch (Exception e) {
            Log.e(TAG, "Error fetching YouTube feed");
            return null;
        } finally {
            if (in != null) {
                try { in.close(); } catch (Exception ignored) {}
            }
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    @NonNull
    public static List<Sermon> parseAtomFeed(InputStream in) throws Exception {
        List<Sermon> list = new ArrayList<>();
        XmlPullParser parser = Xml.newPullParser();
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true);
        parser.setInput(in, "UTF-8");

        int eventType = parser.getEventType();
        boolean insideEntry = false;

        String currentVideoId = null;
        String currentTitle = null;
        String currentPublished = null;
        String currentThumbnail = null;
        String currentDescription = null;

        while (eventType != XmlPullParser.END_DOCUMENT) {
            String tag = parser.getName();
            if (eventType == XmlPullParser.START_TAG) {
                if ("entry".equals(tag)) {
                    insideEntry = true;
                    currentVideoId = null;
                    currentTitle = null;
                    currentPublished = null;
                    currentThumbnail = null;
                    currentDescription = null;
                } else if (insideEntry) {
                    if ("videoId".equals(tag)) {
                        currentVideoId = parser.nextText();
                    } else if ("title".equals(tag)) {
                        currentTitle = parser.nextText();
                    } else if ("published".equals(tag)) {
                        currentPublished = parser.nextText();
                    } else if ("thumbnail".equals(tag)) {
                        currentThumbnail = parser.getAttributeValue(null, "url");
                    } else if ("description".equals(tag)) {
                        currentDescription = parser.nextText();
                    }
                }
            } else if (eventType == XmlPullParser.END_TAG) {
                if ("entry".equals(tag)) {
                    insideEntry = false;
                    if (currentVideoId != null && !currentVideoId.trim().isEmpty()) {
                        long dateMillis = System.currentTimeMillis();
                        if (currentPublished != null) {
                            try {
                                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                                Date d = sdf.parse(currentPublished.substring(0, 10));
                                if (d != null) {
                                    dateMillis = d.getTime();
                                }
                            } catch (Exception ignored) {}
                        }

                        String title = currentTitle != null ? currentTitle.trim() : "Live Broadcast";
                        String videoUrl = "https://www.youtube.com/watch?v=" + currentVideoId;
                        String thumb = currentThumbnail != null ? currentThumbnail : "https://i.ytimg.com/vi/" + currentVideoId + "/hqdefault.jpg";

                        // Infer preacher from title if specified
                        String preacher = "Bishop Dr. David Mutweri";
                        if (title.contains("By:") || title.contains("by:")) {
                            int idx = title.toLowerCase(Locale.US).indexOf("by:");
                            String extracted = title.substring(idx + 3).trim();
                            if (!extracted.isEmpty()) preacher = extracted;
                        }

                        // Infer scripture or series context
                        String scripture = "Breakthrough Word";
                        if (title.contains("(") && title.contains(")")) {
                            int start = title.lastIndexOf('(');
                            int end = title.lastIndexOf(')');
                            if (start < end && end - start > 2) {
                                scripture = title.substring(start + 1, end).trim();
                            }
                        }

                        Sermon sermon = new Sermon(
                                "yt_" + currentVideoId,
                                title,
                                preacher,
                                scripture,
                                "Perazim Live Broadcast",
                                "",
                                videoUrl,
                                thumb,
                                3600L,
                                String.valueOf(dateMillis),
                                currentDescription != null ? currentDescription.trim() : title,
                                false,
                                0L
                        );
                        list.add(sermon);
                    }
                }
            }
            eventType = parser.next();
        }
        return list;
    }

    /**
     * Opens the live broadcast in the YouTube app if installed, falling back to the web browser.
     */
    public static void openLiveBroadcast(@NonNull Context context) {
        openVideo(context, YOUTUBE_LIVE_URL);
    }

    /**
     * Opens a specific YouTube video via intent with native YouTube app priority and browser fallback.
     */
    public static void openVideo(@NonNull Context context, @NonNull String videoUrl) {
        if (videoUrl == null || videoUrl.trim().isEmpty()) {
            videoUrl = YOUTUBE_LIVE_URL;
        }

        try {
            Uri uri = Uri.parse(videoUrl);
            Intent appIntent = new Intent(Intent.ACTION_VIEW, uri);
            appIntent.setPackage("com.google.android.youtube");
            appIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(appIntent);
        } catch (Exception e) {
            // Fallback to browser
            try {
                Intent webIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(videoUrl));
                webIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(webIntent);
            } catch (Exception ex) {
                Toast.makeText(context, "Unable to open video: " + ex.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Shares a sermon with passage and video link to WhatsApp or other social apps.
     */
    public static void shareSermonToWhatsApp(@NonNull Context context, @NonNull Sermon sermon) {
        String title = sermon.getTitle() != null ? sermon.getTitle() : "Perazim Sermon";
        String preacher = sermon.getPreacher() != null ? sermon.getPreacher() : "Bishop Dr. David Mutweri";
        String passage = sermon.getPassage() != null ? sermon.getPassage() : "2 Samuel 5:20";
        String link = sermon.getVideoUrl() != null && !sermon.getVideoUrl().isEmpty()
                ? sermon.getVideoUrl()
                : (sermon.getAudioUrl() != null && !sermon.getAudioUrl().isEmpty() ? sermon.getAudioUrl() : "https://www.youtube.com/@perazimchurchembu");

        String shareText = "🔥 *Perazim Mission Church - Breakthrough Word*\n\n"
                + "📖 *Title:* " + title + "\n"
                + "🎙 *Preacher:* " + preacher + "\n"
                + "📜 *Scripture:* " + passage + "\n\n"
                + "Watch & Listen here:\n" + link + "\n\n"
                + "_\"The LORD hath broken forth upon mine enemies before me!\" — 2 Samuel 5:20_";

        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, shareText);
        sendIntent.setType("text/plain");

        Intent shareIntent = Intent.createChooser(sendIntent, "Share Sermon to WhatsApp & Fellowship");
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(shareIntent);
    }
}
