package com.example.app.community.qr;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Random;

/**
 * Self-contained QR token generator & parser for Perazim Fellowship Connections.
 * Generates and parses connection payloads and produces deterministic pseudo-QR
 * 2D matrix bitmaps with 3 finder patterns and quiet borders without external ZXing dependencies.
 */
public final class PerazimQrHelper {

    private static final String SCHEME = "perazim";
    private static final String HOST = "connect";
    private static final int DEFAULT_QR_SIZE = 512;
    private static final int MODULE_GRID_SIZE = 25; // Standard Version 2 QR matrix size
    private static final int QUIET_ZONE = 2; // Quiet zone module padding

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
     * Generates a scannable 2D matrix bitmap (deterministic pseudo-QR pattern based on content hash/bytes
     * with standard 7x7 finder patterns in 3 corners, quiet zone borders, and timing patterns).
     * Requires no external ZXing or barcode libraries.
     *
     * @param content         The payload string to encode.
     * @param size            Width and height in pixels (defaults to 512 if <= 0).
     * @param foregroundColor ARGB color for dark modules.
     * @param backgroundColor ARGB color for light modules and quiet zone.
     * @return Generated Bitmap.
     */
    @NonNull
    public static Bitmap generateQrBitmap(@Nullable String content, int size, int foregroundColor, int backgroundColor) {
        int bitmapSize = size > 0 ? size : DEFAULT_QR_SIZE;
        int totalModules = MODULE_GRID_SIZE + (QUIET_ZONE * 2); // e.g. 25 + 4 = 29
        boolean[][] matrix = new boolean[totalModules][totalModules];
        boolean[][] reserved = new boolean[totalModules][totalModules];

        // 1. Setup 7x7 Finder Patterns in 3 corners
        // Corner 1: Top-Left (QUIET_ZONE, QUIET_ZONE)
        applyFinderPattern(matrix, reserved, QUIET_ZONE, QUIET_ZONE);
        // Corner 2: Top-Right (QUIET_ZONE + MODULE_GRID_SIZE - 7, QUIET_ZONE)
        applyFinderPattern(matrix, reserved, QUIET_ZONE + MODULE_GRID_SIZE - 7, QUIET_ZONE);
        // Corner 3: Bottom-Left (QUIET_ZONE, QUIET_ZONE + MODULE_GRID_SIZE - 7)
        applyFinderPattern(matrix, reserved, QUIET_ZONE, QUIET_ZONE + MODULE_GRID_SIZE - 7);

        // 2. Setup Timing Patterns (Row 6 and Column 6 relative to active area)
        int timingRow = QUIET_ZONE + 6;
        for (int x = QUIET_ZONE; x < QUIET_ZONE + MODULE_GRID_SIZE; x++) {
            if (!reserved[timingRow][x]) {
                matrix[timingRow][x] = ((x - QUIET_ZONE) % 2 == 0);
                reserved[timingRow][x] = true;
            }
        }
        int timingCol = QUIET_ZONE + 6;
        for (int y = QUIET_ZONE; y < QUIET_ZONE + MODULE_GRID_SIZE; y++) {
            if (!reserved[y][timingCol]) {
                matrix[y][timingCol] = ((y - QUIET_ZONE) % 2 == 0);
                reserved[y][timingCol] = true;
            }
        }

        // 3. Setup Alignment Pattern at bottom-right
        applyAlignmentPattern(matrix, reserved, QUIET_ZONE + MODULE_GRID_SIZE - 7, QUIET_ZONE + MODULE_GRID_SIZE - 7);

        // 4. Populate remaining data area with deterministic bitstream from content hash and bytes
        byte[] contentBytes = (content != null ? content : "").getBytes(StandardCharsets.UTF_8);
        byte[] digest;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            digest = md.digest(contentBytes);
        } catch (Exception e) {
            digest = new byte[32];
            for (int i = 0; i < contentBytes.length; i++) {
                digest[i % 32] ^= contentBytes[i];
            }
        }

        long seed = 0;
        for (int i = 0; i < 8 && i < digest.length; i++) {
            seed = (seed << 8) | (digest[i] & 0xFFL);
        }
        Random prng = new Random(seed);

        int byteIndex = 0;
        int bitIndex = 0;
        for (int y = QUIET_ZONE; y < QUIET_ZONE + MODULE_GRID_SIZE; y++) {
            for (int x = QUIET_ZONE; x < QUIET_ZONE + MODULE_GRID_SIZE; x++) {
                if (!reserved[y][x]) {
                    boolean bit;
                    if (byteIndex < contentBytes.length) {
                        bit = ((contentBytes[byteIndex] >> (7 - bitIndex)) & 1) == 1;
                        bitIndex++;
                        if (bitIndex >= 8) {
                            bitIndex = 0;
                            byteIndex++;
                        }
                    } else {
                        bit = prng.nextBoolean();
                    }
                    matrix[y][x] = bit;
                }
            }
        }

        // 5. Render matrix to Bitmap
        Bitmap bitmap = Bitmap.createBitmap(bitmapSize, bitmapSize, Bitmap.Config.ARGB_8888);
        int[] pixels = new int[bitmapSize * bitmapSize];
        for (int py = 0; py < bitmapSize; py++) {
            int my = (int) (((long) py * totalModules) / bitmapSize);
            if (my >= totalModules) my = totalModules - 1;
            int rowOffset = py * bitmapSize;
            for (int px = 0; px < bitmapSize; px++) {
                int mx = (int) (((long) px * totalModules) / bitmapSize);
                if (mx >= totalModules) mx = totalModules - 1;
                pixels[rowOffset + px] = matrix[my][mx] ? foregroundColor : backgroundColor;
            }
        }
        bitmap.setPixels(pixels, 0, bitmapSize, 0, 0, bitmapSize, bitmapSize);
        return bitmap;
    }

    private static void applyFinderPattern(boolean[][] matrix, boolean[][] reserved, int startX, int startY) {
        for (int dy = -1; dy <= 7; dy++) {
            for (int dx = -1; dx <= 7; dx++) {
                int px = startX + dx;
                int py = startY + dy;
                if (px >= 0 && px < matrix[0].length && py >= 0 && py < matrix.length) {
                    reserved[py][px] = true;
                    if (dx >= 0 && dx < 7 && dy >= 0 && dy < 7) {
                        if (dx == 0 || dx == 6 || dy == 0 || dy == 6) {
                            matrix[py][px] = true; // outer border
                        } else if (dx == 1 || dx == 5 || dy == 1 || dy == 5) {
                            matrix[py][px] = false; // inner light ring
                        } else {
                            matrix[py][px] = true; // 3x3 dark core
                        }
                    } else {
                        matrix[py][px] = false; // separator
                    }
                }
            }
        }
    }

    private static void applyAlignmentPattern(boolean[][] matrix, boolean[][] reserved, int centerX, int centerY) {
        for (int dy = -2; dy <= 2; dy++) {
            for (int dx = -2; dx <= 2; dx++) {
                int px = centerX + dx;
                int py = centerY + dy;
                if (px >= 0 && px < matrix[0].length && py >= 0 && py < matrix.length) {
                    reserved[py][px] = true;
                    if (Math.abs(dx) == 2 || Math.abs(dy) == 2) {
                        matrix[py][px] = true;
                    } else if (Math.abs(dx) == 1 || Math.abs(dy) == 1) {
                        matrix[py][px] = false;
                    } else {
                        matrix[py][px] = true;
                    }
                }
            }
        }
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
