package org.perazimchurch.app.presentation.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Typeface;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONObject;
import org.perazimchurch.app.data.local.entity.UserEntity;
import org.perazimchurch.app.data.local.session.MembershipSnapshotCache;
import org.perazimchurch.app.data.local.session.SessionManager;
import org.perazimchurch.app.data.remote.ApiClient;
import org.perazimchurch.app.data.remote.MembershipAccessPolicy;
import org.perazimchurch.app.data.remote.MembershipRemoteSource;

import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

/** Guided membership and pilot administration forms. Privileged state exists only in this instance. */
public final class MembershipDialog {
    private static final String TAG = "MembershipDialog";
    private static final String CLAIM_REVIEW = "membership.claim.review";
    private static final String RECORD_READ = "membership.record.read";
    private static final String VERIFY = "membership.verification.confirm";
    private static final String RECORD_CORRECT = "membership.record.correct";
    private static final String VERIFY_CORRECT = "membership.verification.correct";
    private static final String REVOKE = "membership.verification.revoke";
    private static final String IMPORT = "membership.register.import";
    private static final String GRANT_READ = "admin.permissions.read";
    private static final String GRANT_ADD = "admin.permissions.grant";
    private static final String GRANT_REMOVE = "admin.permissions.revoke";
    private static final String AUDIT_READ = "admin.audit.read";

    private final Activity activity;
    private final SessionManager session;
    private final String accountId;
    private final String endpoint;
    private final String bearer;
    private final AlertDialog dialog;
    private final LinearLayout content;
    private String privilegedToken;
    private long privilegedExpiresAt;
    private boolean closed;

    private MembershipDialog(Activity activity) {
        this.activity = activity;
        this.session = SessionManager.getInstance(activity);
        this.accountId = session.getActiveUserId();
        this.endpoint = MembershipSnapshotCache.normalizeEndpoint(ApiClient.getBaseUrl(activity));
        this.bearer = session.getAuthToken();
        content = new LinearLayout(activity);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(18), dp(12), dp(18), dp(22));
        ScrollView scroll = new ScrollView(activity);
        scroll.addView(content);
        dialog = new AlertDialog.Builder(activity).setTitle("Church membership")
                .setView(scroll).setNegativeButton("Close", (d, w) -> clearPrivileged()).create();
        dialog.setOnDismissListener(d -> { closed = true; clearPrivileged(); });
    }

    public static void show(@NonNull Context context) {
        if (!(context instanceof Activity)) return;
        open((Activity) context);
    }

    static AlertDialog open(@NonNull Activity activity) {
        MembershipDialog screen = new MembershipDialog(activity);
        screen.dialog.show();
        screen.dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT,
                (int) (activity.getResources().getDisplayMetrics().heightPixels * .86));
        screen.loadStatus();
        return screen.dialog;
    }

    private void loadStatus() {
        if (!session.isLoggedIn() || bearer == null || bearer.isEmpty()) {
            renderStatus(null, false, "Sign in to check your church membership status.", 0);
            return;
        }
        MembershipRemoteSource.status(activity, bearer, new MembershipRemoteSource.Callback() {
            @Override public void onSuccess(JSONObject response) {
                if (!scopeCurrent()) return;
                String status = response.optString("status", "REGISTERED");
                long time = response.optLong("server_time", System.currentTimeMillis());
                MembershipSnapshotCache.put(accountId, endpoint, status, time);
                renderStatus(status, true, null, time);
            }
            @Override public void onError(int code, String message) {
                if (!scopeCurrent()) return;
                MembershipSnapshotCache.Snapshot cached = MembershipSnapshotCache.get(accountId, endpoint);
                renderStatus(cached == null ? null : cached.status, false, cached == null
                        ? "Status is unavailable right now. Try again when you are online."
                        : "Showing last known status from " + formatTime(cached.serverTime) + ". A fresh check is unavailable.", cached == null ? 0 : cached.serverTime);
            }
        });
    }

    private void renderStatus(String status, boolean current, String note, long serverTime) {
        content.removeAllViews();
        addText("Your membership status", 19, true);
        if (status != null) {
            addText(statusLabel(status), 16, true);
            addText(current ? "Current · checked " + formatTime(serverTime) : note, 13, false);
        } else addText(note, 14, false);
        addButton("Submit a membership request", () -> showClaimForm());
        addText("A submitted request receives the same private acknowledgement for every outcome. Candidate and register details are not shown here.", 13, false);

        // Permission snapshots are fetched live and never cached. Only exact server grants expose tools.
        refreshPermissions((permissions, error) -> {
            if (!scopeCurrent()) return;
            if (permissions != null && permissions.optBoolean("pilot") && hasAnyAdminGrant(permissions)) {
                addDivider();
                addText("Administrator tools", 17, true);
                addText("Available only while the server confirms your current permissions.", 13, false);
                addButton("Open administrator tools", () -> openAdmin());
            }
        });
    }

    private void showClaimForm() {
        if (bearer == null || bearer.isEmpty() || !scopeCurrent()) { toast("Sign in before submitting a request."); return; }
        UserEntity user = session.getCurrentUser();
        LinearLayout form = form();
        EditText name = field("Full name", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        EditText phone = field("Phone number", InputType.TYPE_CLASS_PHONE);
        if (user != null) { name.setText(user.getName()); phone.setText(user.getPhone()); }
        form.addView(name); form.addView(phone);
        new AlertDialog.Builder(activity).setTitle("Membership request").setView(form)
                .setNegativeButton("Cancel", null).setPositiveButton("Send request", (d, w) -> {
                    JSONObject body = new JSONObject();
                    try { body.put("request_id", UUID.randomUUID().toString()); body.put("name", name.getText().toString().trim()); body.put("phone", phone.getText().toString().trim()); }
                    catch (Exception ignored) { toast("Please check the name and phone number."); return; }
                    MembershipRemoteSource.submitClaim(activity, body, bearer, new MembershipRemoteSource.Callback() {
                        @Override public void onSuccess(JSONObject response) {
                            if (!scopeCurrent()) return;
                            toast("Your request has been received for review.");
                            loadStatus();
                        }
                        @Override public void onError(int code, String message) { if (scopeCurrent()) toast("The request could not be sent. Please try again."); }
                    });
                }).show();
    }

    private void openAdmin() {
        refreshPermissions((beforeStepUp, error) -> {
            if (beforeStepUp == null || !beforeStepUp.optBoolean("pilot") || !hasAnyAdminGrant(beforeStepUp)) { toast("Administrator tools are unavailable."); return; }
            requireStepUp(() -> refreshPermissions((fresh, afterError) -> {
                if (fresh != null && fresh.optBoolean("pilot") && hasAnyAdminGrant(fresh)) showAdminMenu(fresh);
                else { clearPrivileged(); loadStatus(); }
            }));
        });
    }

    private void showAdminMenu(JSONObject permissions) {
        if (!scopeCurrent() || !permissions.optBoolean("pilot") || !hasAnyAdminGrant(permissions) || !validPrivilegedSession()) { clearPrivileged(); toast("Administrator sign-in expired. Reopen the tools to continue."); return; }
        content.removeAllViews();
        addText("Administrator tools", 19, true);
        addText("Each action checks current server permission. Member details are shown only where your grant allows them.", 13, false);
        if (hasAnyGrant(permissions, CLAIM_REVIEW)) addButton("Review requests", () -> runAdmin(CLAIM_REVIEW, null, "GET", "/api/admin/membership/claims", null, this::showClaims));
        if (hasAnyGrant(permissions, RECORD_READ)) addButton("Membership register", () -> runAdmin(RECORD_READ, null, "GET", "/api/admin/membership/records", null, this::showRecords));
        if (allows(permissions, IMPORT, null)) addButton("Import synthetic test register", this::showImportForm);
        if (hasAnyGrant(permissions, RECORD_CORRECT)) addButton("Correct a register record", this::showRecordCorrection);
        if (hasAnyGrant(permissions, VERIFY_CORRECT)) addButton("Correct a verification link", this::showVerificationCorrection);
        if (hasAnyGrant(permissions, REVOKE)) addButton("Revoke a verification", this::showRevocation);
        if (allows(permissions, GRANT_READ, null)) addButton("Review permissions", this::showPermissionReview);
        if (allows(permissions, GRANT_ADD, null)) addButton("Grant a permission", this::showPermissionGrant);
        if (allows(permissions, GRANT_REMOVE, null)) addButton("Revoke a permission", this::showPermissionRevoke);
        if (allows(permissions, AUDIT_READ, null)) addButton("Audit history", () -> runAdmin(AUDIT_READ, null, "GET", "/api/admin/audit", null, this::showAudit));
        if (allows(permissions, GRANT_REMOVE, null) && allows(permissions, AUDIT_READ, null)) addButton("Revoke administrator sessions", () -> confirmSessionRevocation());
        addButton("Back to membership status", this::loadStatus);
    }

    private interface JsonAction { void run(JSONObject json); }
    private interface PermissionResult { void done(JSONObject json, String error); }

    private void refreshPermissions(PermissionResult result) {
        if (!scopeCurrent() || bearer == null || bearer.isEmpty()) { result.done(null, "Sign in required"); return; }
        MembershipRemoteSource.permissions(activity, bearer, new MembershipRemoteSource.Callback() {
            @Override public void onSuccess(JSONObject response) { if (scopeCurrent()) result.done(response, null); }
            @Override public void onError(int code, String message) { clearPrivileged(); if (scopeCurrent()) result.done(null, message); }
        });
    }

    private void requireStepUp(Runnable ready) {
        if (validPrivilegedSession()) { ready.run(); return; }
        LinearLayout form = form();
        EditText pin = field("Administrator PIN", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        EditText code = field("Authenticator code", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        form.addView(pin); form.addView(code);
        new AlertDialog.Builder(activity).setTitle("Administrator sign-in").setMessage("Confirm with your PIN and current authenticator code.")
                .setView(form).setNegativeButton("Cancel", null).setPositiveButton("Continue", (d, w) -> {
                    JSONObject body = new JSONObject();
                    try { body.put("pin", pin.getText().toString()); body.put("code", code.getText().toString()); }
                    catch (Exception ignored) { return; }
                    pin.setText(""); code.setText("");
                    MembershipRemoteSource.stepUp(activity, body, bearer, new MembershipRemoteSource.Callback() {
                        @Override public void onSuccess(JSONObject response) {
                            if (!scopeCurrent()) return;
                            privilegedToken = response.optString("token", null);
                            privilegedExpiresAt = response.optLong("expires_at", 0);
                            if (validPrivilegedSession()) ready.run(); else { clearPrivileged(); toast("Administrator sign-in could not be verified."); }
                        }
                        @Override public void onError(int status, String message) { if (scopeCurrent()) toast("Administrator sign-in failed. Check the PIN and code."); }
                    });
                }).show();
    }

    private void showClaims(JSONObject response) {
        JSONArray claims = response.optJSONArray("claims");
        content.removeAllViews(); addText("Requests for review", 19, true);
        if (claims == null || claims.length() == 0) addText("There are no requests available to you.", 14, false);
        for (int i = 0; claims != null && i < claims.length(); i++) {
            JSONObject claim = claims.optJSONObject(i); if (claim == null) continue;
            final String id = claim.optString("id");
            addButton(claim.optString("user_name", "Request") + " · " + claim.optString("status", ""), () -> openClaim(id));
        }
        addBackButton();
    }

    private void openClaim(String id) {
        runAdmin(CLAIM_REVIEW, id, "GET", "/api/admin/membership/claims/" + id, null, response -> {
            JSONObject claim = response.optJSONObject("claim"); if (claim == null) { toast("Request details are unavailable."); return; }
            content.removeAllViews(); addText("Review request", 19, true);
            addText("Name: " + claim.optString("user_name"), 15, false);
            addText("Phone: " + claim.optString("user_phone"), 15, false);
            addText("Status: " + claim.optString("status"), 15, false);
            addButton("Approve with a register record", () -> chooseRecordForClaim(id));
            addButton("Reject request", () -> decideClaim(id, "REJECT", null));
            addBackButton();
        });
    }

    private void chooseRecordForClaim(String claimId) {
        runAdmin(RECORD_READ, null, "GET", "/api/admin/membership/records", null, response -> {
            JSONArray records = response.optJSONArray("records");
            if (records == null || records.length() == 0) { toast("No register records are available to your account."); return; }
            String[] labels = new String[records.length()]; String[] ids = new String[records.length()];
            for (int i = 0; i < records.length(); i++) {
                JSONObject record = records.optJSONObject(i); if (record == null) record = new JSONObject();
                ids[i] = record.optString("id");
                labels[i] = record.optString("reference") + " · " + record.optString("full_name") + " · " + record.optString("phone");
            }
            new AlertDialog.Builder(activity).setTitle("Choose register record")
                    .setItems(labels, (d, which) -> decideClaim(claimId, "APPROVE", ids[which])).setNegativeButton("Cancel", null).show();
        });
    }

    private void decideClaim(String claimId, String decision, String recordId) {
        JSONObject body = new JSONObject();
        try { body.put("decision", decision); body.put("reason", "ADMIN_REVIEW"); body.put("request_id", UUID.randomUUID().toString()); if (recordId != null) body.put("record_id", recordId); }
        catch (Exception ignored) { return; }
        refreshPermissions((permissions, error) -> {
            if (permissions == null || !allows(permissions, CLAIM_REVIEW, claimId)
                    || "APPROVE".equals(decision) && (!allows(permissions, VERIFY, claimId) || !allows(permissions, RECORD_READ, recordId))) {
                toast("Your current permissions do not allow this decision."); return;
            }
            runAdmin(CLAIM_REVIEW, claimId, "POST", "/api/admin/membership/claims/" + claimId + "/decision", body, response -> { toast("Review decision saved."); loadStatus(); });
        });
    }

    private void showRecords(JSONObject response) {
        content.removeAllViews(); addText("Membership register", 19, true);
        JSONArray records = response.optJSONArray("records");
        if (records == null || records.length() == 0) addText("No records are available to your account.", 14, false);
        for (int i = 0; records != null && i < records.length(); i++) {
            JSONObject r = records.optJSONObject(i); if (r == null) continue;
            addText(r.optString("reference") + " · " + r.optString("full_name") + " · " + r.optString("phone") + " · " + r.optString("record_status"), 14, false);
        }
        addBackButton();
    }

    private void showRecordCorrection() {
        withRecordChoice(record -> {
            LinearLayout form = form();
            EditText name = field("Correct full name", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
            EditText phone = field("Correct phone number", InputType.TYPE_CLASS_PHONE);
            form.addView(name); form.addView(phone);
            new AlertDialog.Builder(activity).setTitle("Correct register record").setView(form).setNegativeButton("Cancel", null)
                    .setPositiveButton("Save correction", (d, w) -> {
                        JSONObject body = reasonBody("ADMIN_CORRECTION");
                        try { body.put("full_name", name.getText().toString().trim()); body.put("phone", phone.getText().toString().trim()); }
                        catch (Exception ignored) { return; }
                        runAdmin(RECORD_CORRECT, record.optString("id"), "PATCH", "/api/admin/membership/records/" + record.optString("id"), body, result -> { toast("Register correction saved."); loadStatus(); });
                    }).show();
        });
    }

    private void showVerificationCorrection() {
        LinearLayout form = form(); EditText userId = field("Account ID", InputType.TYPE_CLASS_TEXT); form.addView(userId);
        new AlertDialog.Builder(activity).setTitle("Correct verification link").setView(form).setNegativeButton("Cancel", null)
                .setPositiveButton("Choose record", (d, w) -> withRecordChoice(record -> {
                    JSONObject body = reasonBody("ADMIN_CORRECTION"); try { body.put("record_id", record.optString("id")); } catch (Exception ignored) { return; }
                    String id = userId.getText().toString().trim();
                    runAdmin(VERIFY_CORRECT, id, "POST", "/api/admin/membership/users/" + id + "/correct", body, r -> { toast("Verification correction saved."); loadStatus(); });
                })).show();
    }

    private void showRevocation() {
        LinearLayout form = form(); EditText userId = field("Account ID", InputType.TYPE_CLASS_TEXT); form.addView(userId);
        new AlertDialog.Builder(activity).setTitle("Revoke verification").setMessage("This records an audited change and cannot be queued.")
                .setView(form).setNegativeButton("Cancel", null).setPositiveButton("Revoke", (d, w) -> {
                    String id = userId.getText().toString().trim();
                    runAdmin(REVOKE, id, "POST", "/api/admin/membership/users/" + id + "/revoke", reasonBody("ADMIN_REVOCATION"), r -> { toast("Verification revoked."); loadStatus(); });
                }).show();
    }

    private void showImportForm() {
        LinearLayout form = form();
        EditText source = field("Synthetic source label", InputType.TYPE_CLASS_TEXT); source.setText("synthetic-test-import");
        EditText csv = field("Reference, full name, phone (one record per line)", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        csv.setMinLines(4);
        form.addView(source); form.addView(csv);
        new AlertDialog.Builder(activity).setTitle("Import synthetic test register").setMessage("Only use invented test records. You will review every parsed entry before sending.")
                .setView(form).setNegativeButton("Cancel", null).setPositiveButton("Preview records", (d, w) -> previewSyntheticImport(source.getText().toString().trim(), csv.getText().toString())).show();
    }

    private void previewSyntheticImport(String source, String text) {
        JSONArray rows = parseSyntheticRows(text);
        if (rows == null || rows.length() == 0) { toast("Enter valid comma-separated test rows first."); return; }
        StringBuilder preview = new StringBuilder();
        for (int i = 0; i < rows.length(); i++) { JSONObject row = rows.optJSONObject(i); preview.append(row.optString("reference")).append(" · ").append(row.optString("full_name")).append(" · ").append(row.optString("phone")).append('\n'); }
        new AlertDialog.Builder(activity).setTitle("Review " + rows.length() + " synthetic records")
                .setMessage(preview.toString()).setNegativeButton("Edit", null).setPositiveButton("Confirm import", (d, w) -> {
                    JSONObject body=reasonBody("SYNTHETIC_IMPORT");
                    try{body.put("source",source);body.put("records",rows);}catch(Exception ignored){return;}
                    runAdmin(IMPORT,null,"POST","/api/admin/membership/import",body,result->{toast("Synthetic records imported: "+result.optInt("count"));loadStatus();});
                }).show();
    }

    private void withRecordChoice(java.util.function.Consumer<JSONObject> selected) {
        runAdmin(RECORD_READ, null, "GET", "/api/admin/membership/records", null, response -> {
            JSONArray records = response.optJSONArray("records"); if (records == null || records.length() == 0) { toast("No register records are available."); return; }
            JSONObject[] values = new JSONObject[records.length()]; String[] labels = new String[records.length()];
            for (int i = 0; i < records.length(); i++) { values[i] = records.optJSONObject(i); if (values[i] == null) values[i] = new JSONObject(); labels[i] = values[i].optString("reference") + " · " + values[i].optString("full_name") + " · " + values[i].optString("phone"); }
            new AlertDialog.Builder(activity).setTitle("Choose register record").setItems(labels, (d, w) -> selected.accept(values[w])).setNegativeButton("Cancel", null).show();
        });
    }

    private void showPermissionReview() { runAdmin(GRANT_READ, null, "GET", "/api/admin/permissions/" + accountId, null, response -> {
        content.removeAllViews(); addText("Permission assignments", 19, true); JSONArray grants = response.optJSONArray("grants");
        if (grants == null || grants.length() == 0) addText("No assignments were returned.", 14, false);
        for (int i = 0; grants != null && i < grants.length(); i++) { JSONObject g = grants.optJSONObject(i); if (g != null) addText(g.optString("code") + " · " + g.optString("scope") + " · " + (g.isNull("expires_at") ? "No expiry" : "Expires " + formatTime(g.optLong("expires_at"))), 14, false); }
        addBackButton();
    }); }

    private void showPermissionGrant() {
        LinearLayout form = form(); EditText user = field("Pilot account ID", InputType.TYPE_CLASS_TEXT); user.setText(accountId);
        String[] labels={"Review requests","Confirm membership","Read register records","Import synthetic register","Correct register records","Correct verification links","Revoke verification","Read permission assignments","Grant permissions","Revoke permissions","Read audit history"};
        String[] codes={CLAIM_REVIEW,VERIFY,RECORD_READ,IMPORT,RECORD_CORRECT,VERIFY_CORRECT,REVOKE,GRANT_READ,GRANT_ADD,GRANT_REMOVE,AUDIT_READ};
        Spinner capability=new Spinner(activity);capability.setAdapter(new ArrayAdapter<>(activity,android.R.layout.simple_spinner_dropdown_item,labels));
        EditText scope = field("Scope: global or one exact resource ID", InputType.TYPE_CLASS_TEXT); scope.setText("global");
        EditText expiry = field("Expiry in days (optional)", InputType.TYPE_CLASS_NUMBER); form.addView(user); form.addView(capability); form.addView(scope); form.addView(expiry);
        new AlertDialog.Builder(activity).setTitle("Grant permission").setMessage("The server accepts administrative grants only for the isolated pilot account.")
                .setView(form).setNegativeButton("Cancel", null).setPositiveButton("Grant", (d, w) -> {
                    String target = user.getText().toString().trim(); JSONObject body = reasonBody("ADMIN_GRANT");
                    try { body.put("code", codes[capability.getSelectedItemPosition()]); body.put("scope", scope.getText().toString().trim()); String days=expiry.getText().toString().trim(); if(!days.isEmpty()) body.put("expires_at", System.currentTimeMillis()+Long.parseLong(days)*86400000L); }
                    catch (Exception ignored) { toast("Check the permission details."); return; }
                    runAdmin(GRANT_ADD, null, "POST", "/api/admin/permissions/" + target, body, r -> { toast("Permission granted."); loadStatus(); });
                }).show();
    }

    private void showPermissionRevoke() {
        LinearLayout form = form(); EditText user = field("Pilot account ID", InputType.TYPE_CLASS_TEXT); EditText grant = field("Grant ID", InputType.TYPE_CLASS_TEXT); form.addView(user); form.addView(grant);
        new AlertDialog.Builder(activity).setTitle("Revoke permission").setView(form).setNegativeButton("Cancel", null).setPositiveButton("Revoke", (d,w)->{
            String target=user.getText().toString().trim(); JSONObject body=reasonBody("ADMIN_REVOCATION");
            runAdmin(GRANT_REMOVE,null,"DELETE","/api/admin/permissions/"+target+"/"+grant.getText().toString().trim(),body,r->{toast("Permission revoked.");loadStatus();});
        }).show();
    }

    private void showAudit(JSONObject response) {
        content.removeAllViews(); addText("Administrative audit",19,true); JSONArray events=response.optJSONArray("events");
        for(int i=0;events!=null&&i<events.length();i++){JSONObject e=events.optJSONObject(i);if(e!=null)addText(formatTime(e.optLong("created_at"))+" · "+e.optString("action")+" · "+e.optString("reason"),14,false);}
        addBackButton();
    }

    private void confirmSessionRevocation() {
        new AlertDialog.Builder(activity).setTitle("Revoke administrator sessions?").setMessage("This ends privileged sessions and writes an audit event.")
                .setNegativeButton("Cancel", null).setPositiveButton("Revoke sessions", (d,w)->runAdmin("admin.permissions.revoke",null,"POST","/api/admin/security/revoke-sessions",reasonBody("ADMIN_REVOCATION"),r->{clearPrivileged();toast("Administrator sessions revoked.");loadStatus();})).show();
    }

    private void runAdmin(String capability, String scope, String method, String path, JSONObject body, JsonAction action) {
        refreshPermissions((permissions,error)->{
            boolean listRead = "/api/admin/membership/claims".equals(path) && CLAIM_REVIEW.equals(capability)
                    || "/api/admin/membership/records".equals(path) && RECORD_READ.equals(capability);
            if(permissions==null||!(listRead?hasAnyGrant(permissions,capability):allows(permissions,capability,scope))){clearPrivileged();toast("Your current server permissions do not allow this action.");return;}
            if(!validPrivilegedSession()){clearPrivileged();requireStepUp(()->runAdmin(capability,scope,method,path,body,action));return;}
            MembershipRemoteSource.admin(activity,method,path,body,bearer,privilegedToken,new MembershipRemoteSource.Callback(){
                @Override public void onSuccess(JSONObject response){if(scopeCurrent())action.run(response);}
                @Override public void onError(int code,String message){if(code==401||code==403){clearPrivileged();loadStatus();}if(scopeCurrent())toast("The action could not be completed: "+safeError(message));}
            });
        });
    }

    private boolean allows(JSONObject response, String code, String resource) {
        JSONArray grants=response.optJSONArray("grants"); java.util.List<MembershipAccessPolicy.Grant> values=new java.util.ArrayList<>();
        for(int i=0;grants!=null&&i<grants.length();i++){JSONObject g=grants.optJSONObject(i);if(g==null)continue;long expires=g.isNull("expires_at")?Long.MAX_VALUE:g.optLong("expires_at",0);values.add(new MembershipAccessPolicy.Grant(g.optString("code"),g.optString("scope"),g.optLong("effective_at",0),expires));}
        return MembershipAccessPolicy.permits(values,code,resource,response.optLong("server_time",System.currentTimeMillis()));
    }

    private boolean hasAnyAdminGrant(JSONObject response) {
        return hasAnyGrant(response,CLAIM_REVIEW)||hasAnyGrant(response,RECORD_READ)||allows(response,IMPORT,null)
                ||hasAnyGrant(response,RECORD_CORRECT)||hasAnyGrant(response,VERIFY_CORRECT)||hasAnyGrant(response,REVOKE)
                ||allows(response,GRANT_READ,null)||allows(response,GRANT_ADD,null)||allows(response,GRANT_REMOVE,null)||allows(response,AUDIT_READ,null);
    }

    private boolean hasAnyGrant(JSONObject permissions, String code) {
        JSONArray grants=permissions.optJSONArray("grants"); long now=permissions.optLong("server_time",System.currentTimeMillis());
        for(int i=0;grants!=null&&i<grants.length();i++){JSONObject g=grants.optJSONObject(i);if(g==null||!code.equals(g.optString("code")))continue;long from=g.optLong("effective_at",0);long until=g.isNull("expires_at")?Long.MAX_VALUE:g.optLong("expires_at",0);if(from<=now&&until>now)return true;}
        return false;
    }

    private boolean validPrivilegedSession() { return privilegedToken!=null&&!privilegedToken.isEmpty()&&privilegedExpiresAt>System.currentTimeMillis()&&scopeCurrent(); }
    private void clearPrivileged(){privilegedToken=null;privilegedExpiresAt=0;}
    private boolean scopeCurrent(){return !closed&&dialog.isShowing()&&!activity.isFinishing()&&accountId.equals(session.getActiveUserId())&&endpoint.equals(MembershipSnapshotCache.normalizeEndpoint(ApiClient.getBaseUrl(activity)))&&bearer!=null&&bearer.equals(session.getAuthToken());}

    private void addBackButton(){addButton("Back to administrator tools",()->refreshPermissions((p,e)->{if(p!=null&&hasAnyAdminGrant(p))showAdminMenu(p);else loadStatus();}));}
    private void addDivider(){View v=new View(activity);v.setBackgroundColor(0xffdddddd);content.addView(v,new LinearLayout.LayoutParams(-1,dp(1)));}
    private void addText(String value,int size,boolean bold){TextView t=new TextView(activity);t.setText(value);t.setTextSize(size);t.setTextColor(0xff24212b);t.setPadding(0,dp(8),0,dp(8));if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);content.addView(t);}
    private void addButton(String label,Runnable action){TextView b=new TextView(activity);b.setText(label);b.setTextSize(15);b.setTextColor(0xff52247a);b.setGravity(Gravity.CENTER_VERTICAL);b.setPadding(dp(12),dp(13),dp(12),dp(13));b.setBackgroundResource(android.R.drawable.list_selector_background);b.setOnClickListener(v->action.run());content.addView(b);}
    private LinearLayout form(){LinearLayout l=new LinearLayout(activity);l.setOrientation(LinearLayout.VERTICAL);l.setPadding(dp(18),dp(8),dp(18),dp(6));return l;}
    private EditText field(String hint,int inputType){EditText e=new EditText(activity);e.setHint(hint);e.setInputType(inputType);e.setSingleLine(inputType!= (InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_MULTI_LINE));return e;}
    private void toast(String text){if(!activity.isFinishing())Toast.makeText(activity,text,Toast.LENGTH_LONG).show();}
    private int dp(int value){return (int)(value*activity.getResources().getDisplayMetrics().density+.5f);}
    private String formatTime(long time){return DateFormat.getDateTimeInstance(DateFormat.MEDIUM,DateFormat.SHORT,Locale.getDefault()).format(new Date(time));}
    private String statusLabel(String value){switch(value){case "VERIFIED_MEMBER":return "Verified member";case "VERIFICATION_PENDING":return "Request pending";case "VERIFICATION_REJECTED":return "Request not approved";case "VERIFICATION_REVOKED":return "Verification withdrawn";default:return "Registered account · membership not yet verified";}}
    private String safeError(String message){return message==null||message.trim().isEmpty()?"please try again":message;}
    private JSONObject reasonBody(String reason){JSONObject body=new JSONObject();try{body.put("reason",reason);body.put("request_id",UUID.randomUUID().toString());}catch(Exception ignored){}return body;}
    private JSONArray parseSyntheticRows(String text){try{JSONArray rows=new JSONArray();for(String line:text.split("\\r?\\n")){String[] parts=line.split(",",3);if(parts.length!=3)return null;String ref=parts[0].trim(),name=parts[1].trim(),phone=parts[2].trim();if(ref.isEmpty()||name.isEmpty()||phone.isEmpty())return null;JSONObject row=new JSONObject();row.put("reference",ref);row.put("full_name",name);row.put("phone",phone);rows.put(row);}return rows;}catch(Exception e){return null;}}
}
