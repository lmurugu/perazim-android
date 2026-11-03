package org.perazimchurch.app.community.qr;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Self-contained QR token generator & parser for Perazim Fellowship Connections.
 * Generates and parses connection payloads and produces standards-compliant ISO/IEC 18004
 * 2D QR Code bitmaps with 4-module quiet zones readable by any camera scanner.
 */
public final class PerazimQrHelper {

    private static final String SCHEME = "perazim";
    private static final String HOST = "connect";
    private static final int DEFAULT_QR_SIZE = 512;

    public static final int COLOR_QR_DARK = Color.parseColor("#1A1225");
    public static final int COLOR_QR_LIGHT = Color.parseColor("#FFFFFF");
    public static final int STANDARD_QUIET_ZONE_MODULES = 4;

    private PerazimQrHelper() {
        // Utility class
    }

    /**
     * Generates a connection payload URI for a member.
     * Format: perazim://connect?uid=<userId>&name=<encodedName>&campus=<encodedCampus>&ts=<timestamp>
     *
     * @param userId Unique user ID of the member.
     * @param name   Display name of the member.
     * @param campus Fellowship campus name or ID.
     * @return Formatted connection URI string.
     */
    @NonNull
    public static String generateConnectionPayload(@Nullable String userId, @Nullable String name, @Nullable String campus) {
        long timestamp = System.currentTimeMillis();
        try {
            String encodedUid = URLEncoder.encode(userId != null ? userId : "", StandardCharsets.UTF_8.name());
            String encodedName = URLEncoder.encode(name != null ? name : "", StandardCharsets.UTF_8.name());
            String encodedCampus = URLEncoder.encode(campus != null ? campus : "", StandardCharsets.UTF_8.name());
            return SCHEME + "://" + HOST + "?uid=" + encodedUid + "&name=" + encodedName + "&campus=" + encodedCampus + "&ts=" + timestamp;
        } catch (Exception e) {
            String safeUid = userId != null ? userId : "";
            String safeName = name != null ? name : "";
            String safeCampus = campus != null ? campus : "";
            return SCHEME + "://" + HOST + "?uid=" + safeUid + "&name=" + safeName + "&campus=" + safeCampus + "&ts=" + timestamp;
        }
    }

    /**
     * Parses a connection payload QR string or URI into a {@link ConnectionPayload}.
     *
     * @param qrString Scanned or pasted connection string.
     * @return Parsed ConnectionPayload or null if invalid.
     */
    @Nullable
    public static ConnectionPayload parseConnectionPayload(@Nullable String qrString) {
        if (qrString == null || qrString.trim().isEmpty()) {
            return null;
        }
        String trimmed = qrString.trim();
        try {
            Uri uri = Uri.parse(trimmed);
            String userId = uri.getQueryParameter("uid");
            String name = uri.getQueryParameter("name");
            String campus = uri.getQueryParameter("campus");
            String tsParam = uri.getQueryParameter("ts");
            long timestamp = 0L;
            if (tsParam != null && !tsParam.isEmpty()) {
                try {
                    timestamp = Long.parseLong(tsParam);
                } catch (NumberFormatException ignored) {
                }
            }

            // Fallback for manual parameters if query params couldn't be parsed directly
            if (userId == null && trimmed.contains("uid=")) {
                userId = extractParam(trimmed, "uid");
                name = extractParam(trimmed, "name");
                campus = extractParam(trimmed, "campus");
                String rawTs = extractParam(trimmed, "ts");
                if (rawTs != null) {
                    try {
                        timestamp = Long.parseLong(rawTs);
                    } catch (NumberFormatException ignored) {}
                }
            }

            if (userId != null && !userId.isEmpty()) {
                return new ConnectionPayload(userId, name != null ? name : "", campus != null ? campus : "", timestamp);
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private static String extractParam(String query, String key) {
        try {
            int start = query.indexOf(key + "=");
            if (start == -1) return null;
            start += key.length() + 1;
            int end = query.indexOf("&", start);
            if (end == -1) end = query.length();
            return URLDecoder.decode(query.substring(start, end), StandardCharsets.UTF_8.name());
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Generates a standards-compliant ISO/IEC 18004 QR Code bitmap.
     *
     * @param context Application/activity context (optional, unused).
     * @param payload Payload string to encode.
     * @param width   Target bitmap width in pixels.
     * @param height  Target bitmap height in pixels.
     * @return Generated standards-compliant high-contrast Bitmap.
     */
    @NonNull
    public static Bitmap generateQrBitmap(@Nullable Context context, @Nullable String payload, int width, int height) {
        return generateQrBitmap(payload, width, height, COLOR_QR_DARK, COLOR_QR_LIGHT);
    }

    /**
     * Generates a square standards-compliant ISO/IEC 18004 QR Code bitmap.
     *
     * @param payload Payload string to encode.
     * @param size    Target bitmap width and height in pixels.
     * @return Generated standards-compliant high-contrast Bitmap.
     */
    @NonNull
    public static Bitmap generateQrBitmap(@Nullable String payload, int size) {
        return generateQrBitmap(payload, size, size, COLOR_QR_DARK, COLOR_QR_LIGHT);
    }

    /**
     * Generates a square standards-compliant ISO/IEC 18004 QR Code bitmap with custom colors.
     *
     * @param content         Payload string to encode.
     * @param size            Target bitmap width and height in pixels.
     * @param foregroundColor ARGB color for dark modules.
     * @param backgroundColor ARGB color for light modules and quiet zone.
     * @return Generated standards-compliant high-contrast Bitmap.
     */
    @NonNull
    public static Bitmap generateQrBitmap(@Nullable String content, int size, int foregroundColor, int backgroundColor) {
        return generateQrBitmap(content, size, size, foregroundColor, backgroundColor);
    }

    /**
     * Full implementation of standards-compliant ISO/IEC 18004 QR Code bitmap generation.
     * Uses {@link QrMatrixEncoder} and adds a 4-module quiet zone around the active matrix.
     *
     * @param content         The payload string to encode.
     * @param width           Bitmap width in pixels.
     * @param height          Bitmap height in pixels.
     * @param foregroundColor ARGB color for dark modules.
     * @param backgroundColor ARGB color for light modules and quiet zone.
     * @return Standards-compliant high-contrast Bitmap.
     */
    @NonNull
    public static Bitmap generateQrBitmap(@Nullable String content, int width, int height, int foregroundColor, int backgroundColor) {
        String safeContent = (content != null && !content.trim().isEmpty()) ? content.trim() : "perazim://connect";
        int bitmapWidth = width > 0 ? width : DEFAULT_QR_SIZE;
        int bitmapHeight = height > 0 ? height : DEFAULT_QR_SIZE;

        // 1. Generate ISO/IEC 18004 standard QR boolean matrix
        boolean[][] rawMatrix = QrMatrixEncoder.encode(safeContent);
        int matrixDim = rawMatrix.length;

        // 2. Add standard 4-module quiet zone border
        int quietZone = STANDARD_QUIET_ZONE_MODULES;
        int totalModules = matrixDim + (quietZone * 2);

        // 3. Render into high-contrast Android Bitmap
        Bitmap bitmap = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888);
        int[] pixels = new int[bitmapWidth * bitmapHeight];

        for (int py = 0; py < bitmapHeight; py++) {
            int moduleY = (py * totalModules) / bitmapHeight;
            int rowOffset = py * bitmapWidth;
            for (int px = 0; px < bitmapWidth; px++) {
                int moduleX = (px * totalModules) / bitmapWidth;
                int mx = moduleX - quietZone;
                int my = moduleY - quietZone;
                boolean isDark = (mx >= 0 && mx < matrixDim && my >= 0 && my < matrixDim) && rawMatrix[my][mx];
                pixels[rowOffset + px] = isDark ? foregroundColor : backgroundColor;
            }
        }

        bitmap.setPixels(pixels, 0, bitmapWidth, 0, 0, bitmapWidth, bitmapHeight);
        return bitmap;
    }

    /**
     * Immutable data class representing the parsed connection payload.
     */
    public static class ConnectionPayload {
        private final String userId;
        private final String name;
        private final String campus;
        private final long timestamp;

        public ConnectionPayload(String userId, String name, String campus, long timestamp) {
            this.userId = userId;
            this.name = name;
            this.campus = campus;
            this.timestamp = timestamp;
        }

        public String getUserId() { return userId; }
        public String getName() { return name; }
        public String getCampus() { return campus; }
        public long getTimestamp() { return timestamp; }

        @Override
        public String toString() {
            return "ConnectionPayload{" +
                    "userId='" + userId + '\'' +
                    ", name='" + name + '\'' +
                    ", campus='" + campus + '\'' +
                    ", timestamp=" + timestamp +
                    '}';
        }
    }
}
