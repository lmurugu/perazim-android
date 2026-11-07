package org.perazimchurch.app.data.remote;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONObject;
import org.perazimchurch.app.BuildConfig;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Lightweight, zero-dependency HTTP client for Perazim Church API.
 * Uses standard HttpURLConnection, works offline-first with strict timeouts,
 * and delivers all responses to the Android main thread.
 */
public final class ApiClient {

    private static final String TAG = "ApiClient";
    private static final int CONNECT_TIMEOUT_MS = 10000;
    private static final int READ_TIMEOUT_MS = 10000;
    private static final String USER_AGENT = "Perazim-Android/2.2.0";

    private static final String PREFS_NAME = "perazim_server_prefs";
    private static final String KEY_SERVER_URL = "server_base_url";
    public static final String DEFAULT_EMULATOR_URL = "http://10.0.2.2:3000";
    public static final String DEFAULT_DEVICE_URL = "http://127.0.0.1:3000";

    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool();
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

    public interface ApiCallback {
        void onSuccess(int statusCode, @NonNull JSONObject json);
        void onError(int statusCode, @NonNull String errorMessage);
    }

    private ApiClient() {}

    @NonNull
    public static String getBaseUrl(@NonNull Context context) {
        SharedPreferences prefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String customUrl = prefs.getString(KEY_SERVER_URL, null);
        if (customUrl != null && !customUrl.trim().isEmpty()) {
            return customUrl.trim();
        }
        return DEFAULT_DEVICE_URL;
    }

    public static void setBaseUrl(@NonNull Context context, @Nullable String url) {
        SharedPreferences prefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        if (url != null && !url.trim().isEmpty()) {
            prefs.edit().putString(KEY_SERVER_URL, url.trim()).apply();
        } else {
            prefs.edit().remove(KEY_SERVER_URL).apply();
        }
    }

    public static void get(@NonNull Context context,
                           @NonNull String endpoint,
                           @Nullable String authToken,
                           @NonNull ApiCallback callback) {
        execute(context, "GET", endpoint, null, authToken, callback);
    }

    public static void post(@NonNull Context context,
                            @NonNull String endpoint,
                            @Nullable JSONObject jsonBody,
                            @Nullable String authToken,
                            @NonNull ApiCallback callback) {
        execute(context, "POST", endpoint, jsonBody != null ? jsonBody.toString() : null, authToken, callback);
    }

    public static void put(@NonNull Context context,
                           @NonNull String endpoint,
                           @Nullable JSONObject jsonBody,
                           @Nullable String authToken,
                           @NonNull ApiCallback callback) {
        execute(context, "PUT", endpoint, jsonBody != null ? jsonBody.toString() : null, authToken, callback);
    }

    public static void delete(@NonNull Context context,
                              @NonNull String endpoint,
                              @Nullable String authToken,
                              @NonNull ApiCallback callback) {
        execute(context, "DELETE", endpoint, null, authToken, callback);
    }

    /** Sends a protected operation directly; privileged requests are never queued for offline retry. */
    public static void privileged(@NonNull Context context, @NonNull String method,
                                  @NonNull String endpoint, @Nullable JSONObject body,
                                  @NonNull String authToken, @NonNull String privilegedToken,
                                  @NonNull ApiCallback callback) {
        if (!endpoint.startsWith("/api/admin/") ||
                !("GET".equals(method) || "POST".equals(method) || "PATCH".equals(method) || "DELETE".equals(method)) ||
                authToken.trim().isEmpty() || privilegedToken.trim().isEmpty()) {
            MAIN_HANDLER.post(() -> callback.onError(-1, "Administrator authentication is required."));
            return;
        }
        execute(context, method, endpoint, body == null ? null : body.toString(), authToken, privilegedToken, callback);
    }

    private static void execute(Context context,
                                String method,
                                String endpoint,
                                @Nullable String bodyString,
                                @Nullable String authToken,
                                ApiCallback callback) {
        execute(context, method, endpoint, bodyString, authToken, null, callback);
    }

    private static void execute(Context context, String method, String endpoint,
                                @Nullable String bodyString, @Nullable String authToken,
                                @Nullable String privilegedToken, ApiCallback callback) {
        final Context appContext = context.getApplicationContext();
        final String requestBaseUrl = getBaseUrl(appContext);
        final String path = endpoint.startsWith("/") ? endpoint : "/" + endpoint;
        final boolean protectedPath = path.startsWith("/api/admin/") || path.startsWith("/api/membership/") ||
                path.equals("/api/access/permissions");
        EXECUTOR.execute(() -> {
            HttpURLConnection conn = null;
            try {
                if (!requestBaseUrl.equals(getBaseUrl(appContext))) {
                    MAIN_HANDLER.post(() -> callback.onError(-1, "The server changed. Try again."));
                    return;
                }
                String fullUrl = requestBaseUrl.replaceAll("/+$", "") + path;

                URL url = new URL(fullUrl);
                if (protectedPath && !allowsProtectedTransport(url)) {
                    MAIN_HANDLER.post(() -> callback.onError(-1, "Membership and administration require a secure server connection."));
                    return;
                }
                conn = (HttpURLConnection) url.openConnection();
                conn.setInstanceFollowRedirects(false);
                conn.setRequestMethod(method);
                conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
                conn.setReadTimeout(READ_TIMEOUT_MS);
                conn.setRequestProperty("User-Agent", USER_AGENT);
                conn.setRequestProperty("Accept", "application/json");
                if (protectedPath) {
                    conn.setUseCaches(false);
                    conn.setRequestProperty("Cache-Control", "no-store");
                }

                if (authToken != null && !authToken.trim().isEmpty()) {
                    conn.setRequestProperty("Authorization", "Bearer " + authToken.trim());
                }
                if (privilegedToken != null) conn.setRequestProperty("X-Privileged-Session", privilegedToken);

                if (bodyString != null && ("POST".equals(method) || "PUT".equals(method) ||
                        "PATCH".equals(method) || "DELETE".equals(method))) {
                    conn.setDoOutput(true);
                    conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                    byte[] bytes = bodyString.getBytes(StandardCharsets.UTF_8);
                    try (OutputStream os = conn.getOutputStream()) {
                        os.write(bytes);
                        os.flush();
                    }
                }

                int statusCode = conn.getResponseCode();
                InputStream is = (statusCode >= 200 && statusCode < 400)
                        ? conn.getInputStream()
                        : conn.getErrorStream();

                String responseBody = "";
                if (is != null) {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            sb.append(line);
                        }
                        responseBody = sb.toString();
                    }
                }

                JSONObject json;
                try {
                    json = responseBody.isEmpty() ? new JSONObject() : new JSONObject(responseBody);
                } catch (Exception e) {
                    json = new JSONObject();
                    json.put("raw", responseBody);
                }

                final int finalStatus = statusCode;
                final JSONObject finalJson = json;

                MAIN_HANDLER.post(() -> {
                    if (!requestBaseUrl.equals(getBaseUrl(appContext))) {
                        callback.onError(-1, "The server changed. Try again.");
                    } else if (finalStatus >= 200 && finalStatus < 300) {
                        callback.onSuccess(finalStatus, finalJson);
                    } else {
                        callback.onError(finalStatus, finalJson.optString("error", "Request failed with HTTP " + finalStatus));
                    }
                });
            } catch (Exception e) {
                Log.w(TAG, "Network request failed [" + method + "]: " + e.getClass().getSimpleName());
                String msg = "Offline or server unreachable";
                MAIN_HANDLER.post(() -> callback.onError(-1, msg));
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
        });
    }

    static boolean allowsProtectedTransport(URL url) {
        if (url.getUserInfo() != null) return false;
        if ("https".equalsIgnoreCase(url.getProtocol())) return true;
        if (!"validation".equals(BuildConfig.BUILD_TYPE) || !"http".equalsIgnoreCase(url.getProtocol())) return false;
        String host = url.getHost();
        return "127.0.0.1".equals(host) || "[::1]".equals(host) || "localhost".equalsIgnoreCase(host);
    }
}
