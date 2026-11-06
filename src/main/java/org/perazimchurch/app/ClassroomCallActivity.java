package org.perazimchurch.app;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import org.json.JSONObject;
import org.perazimchurch.app.data.local.session.SessionManager;
import org.perazimchurch.app.data.remote.ApiClient;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;

/** Local, trusted WebRTC page; only native authenticated signaling reaches the PC. */
public final class ClassroomCallActivity extends Activity {
    private static final String ORIGIN = "https://appassets.androidplatform.net";
    private static final String PAGE = ORIGIN + "/classroom.html";
    private WebView web;
    private String classId, token, sessionId;
    private boolean closing;

    public static void open(Activity owner, String id, String title) {
        Intent intent = new Intent(owner, ClassroomCallActivity.class);
        intent.putExtra("classId", id); intent.putExtra("title", title);
        owner.startActivity(intent);
    }
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        classId = getIntent().getStringExtra("classId");
        token = SessionManager.getInstance(this).getAuthToken();
        if (classId == null || !classId.matches("[a-zA-Z0-9_-]{1,64}") || token == null) { finish(); return; }
        getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO}, 52);
        } else startPage();
    }
    @Override public void onRequestPermissionsResult(int request, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(request, permissions, results);
        if (request != 52) return;
        if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
                checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) startPage();
        else { Toast.makeText(this, "Allow camera and microphone to join a video class.", Toast.LENGTH_LONG).show(); finish(); }
    }
    private void startPage() {
        if (closing || web != null) return;
        web = new WebView(this); setContentView(web);
        WebSettings settings = web.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setAllowFileAccess(false); settings.setAllowContentAccess(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setSupportMultipleWindows(false);
        web.addJavascriptInterface(new Bridge(), "ClassroomNative");
        web.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) { return true; }
            @Override public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                try {
                    if (PAGE.equals(request.getUrl().toString())) return new WebResourceResponse("text/html", "UTF-8", getAssets().open("classroom.html"));
                } catch (Exception ignored) { }
                return new WebResourceResponse("text/plain", "UTF-8", new ByteArrayInputStream(new byte[0]));
            }
            @Override public void onPageFinished(WebView view, String url) {
                if (PAGE.equals(url) && !closing) {
                    String title = getIntent().getStringExtra("title");
                    web.evaluateJavascript("window.setTitle(" + JSONObject.quote(title == null ? "Video class" : title) + ")", null);
                }
            }
        });
        web.setWebChromeClient(new WebChromeClient() {
            @Override public void onPermissionRequest(PermissionRequest request) {
                runOnUiThread(() -> {
                    if (closing || !ORIGIN.equals(request.getOrigin().getScheme() + "://" + request.getOrigin().getAuthority()) ||
                            checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                            checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) { request.deny(); return; }
                    ArrayList<String> resources = new ArrayList<>();
                    for (String resource : request.getResources()) {
                        if (PermissionRequest.RESOURCE_AUDIO_CAPTURE.equals(resource) || PermissionRequest.RESOURCE_VIDEO_CAPTURE.equals(resource)) resources.add(resource);
                    }
                    if (resources.isEmpty()) request.deny(); else request.grant(resources.toArray(new String[0]));
                });
            }
        });
        web.loadUrl(PAGE);
    }
    private final class Bridge {
        @JavascriptInterface public void close() { runOnUiThread(() -> finish()); }
        @JavascriptInterface public void request(String id, String action, String encoded) {
            if (id == null || !id.matches("[0-9]{1,12}") || encoded == null || encoded.length() > 80000) return;
            runOnUiThread(() -> {
                if (closing || web == null) return;
                try {
                    JSONObject body = new JSONObject(encoded);
                    String base = "/api/classroom-calls/" + classId;
                    ApiClient.ApiCallback callback = new ApiClient.ApiCallback() {
                        @Override public void onSuccess(int status, JSONObject json) {
                            if ("join".equals(action)) {
                                String joined = json.optString("sessionId");
                                if (joined.matches("[a-f0-9]{36}")) {
                                    if (closing) leaveSession(joined); else sessionId = joined;
                                }
                            }
                            respond(id, true, json);
                        }
                        @Override public void onError(int status, String message) {
                            JSONObject error = new JSONObject();
                            try { error.put("error", message); error.put("status", status); } catch (Exception ignored) { }
                            respond(id, false, error);
                        }
                    };
                    if ("join".equals(action) && sessionId == null) ApiClient.post(ClassroomCallActivity.this, base + "/join", body, token, callback);
                    else if (sessionId != null && "poll".equals(action)) {
                        long cursor = body.optLong("after", -1);
                        if (cursor < 0) throw new IllegalArgumentException();
                        ApiClient.get(ClassroomCallActivity.this, base + "/" + sessionId + "/events?after=" + cursor, token, callback);
                    } else if (sessionId != null && "signal".equals(action)) ApiClient.post(ClassroomCallActivity.this, base + "/" + sessionId + "/signal", body, token, callback);
                    else throw new IllegalArgumentException();
                } catch (Exception ignored) {
                    respond(id, false, new JSONObject());
                }
            });
        }
    }
    private void respond(String id, boolean success, JSONObject json) {
        if (!closing && web != null) web.evaluateJavascript("window.nativeResponse(" + JSONObject.quote(id) + "," + success + "," + json + ")", null);
    }
    private void leaveSession(String sid) {
        ApiClient.delete(this, "/api/classroom-calls/" + classId + "/" + sid, token, new ApiClient.ApiCallback() {
            @Override public void onSuccess(int status, JSONObject json) { }
            @Override public void onError(int status, String message) { }
        });
    }
    @Override protected void onStop() { super.onStop(); if (web != null) { release(); finish(); } }
    @Override protected void onDestroy() { release(); super.onDestroy(); }
    private void release() {
        if (closing) return;
        closing = true;
        if (sessionId != null) { leaveSession(sessionId); sessionId = null; }
        if (web != null) {
            web.evaluateJavascript("window.leaveCall && window.leaveCall()", null);
            web.removeJavascriptInterface("ClassroomNative");
            web.loadUrl("about:blank"); web.destroy(); web = null;
        }
        getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }
}
