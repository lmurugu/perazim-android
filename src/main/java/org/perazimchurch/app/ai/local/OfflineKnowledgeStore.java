package org.perazimchurch.app.ai.local;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import android.util.AtomicFile;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.perazimchurch.app.BuildConfig;
import org.perazimchurch.app.data.remote.ApiClient;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.KeyStore;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.SecretKey;

/** Verifies signed public packages before storing them atomically in private app storage. */
public final class OfflineKnowledgeStore {
    private static final String FORMAT = "perazim-knowledge-v1";
    private static final String ROLLBACK_FORMAT = "perazim-knowledge-rollback-v1";
    private static final String CONTROL_FORMAT = "perazim-knowledge-control-v1";
    private static final long MAX_BYTES = 2 * 1024 * 1024;
    private static final long MAX_SIGNED_CLOCK_SKEW_MS = 5_000L;
    private static final String PACKAGE_FILE = "ai/knowledge/current.json";
    private static final String PREVIOUS_PACKAGE_FILE = "ai/knowledge/previous.json";
    private static final String CONTROL_FILE = "ai/knowledge/control.json";
    private static final String INSTALL_STATE_PREFS = "perazim_ai_package_state_v1";
    private static final String CONTROL_STATE_PREFS = "perazim_ai_package_control_state_v1";
    private static final String INSTALL_STATE_KEY_ALIAS = "org.perazimchurch.app.ai.package-state.v1";
    private static final String UPDATE_STATUS_PREFS = "perazim_ai_package_update_status_v1";
    private static final java.util.Set<String> QUERY_STOP_WORDS = new java.util.HashSet<>(java.util.Arrays.asList(
            "about", "after", "also", "and", "are", "because", "been", "before", "being", "but",
            "can", "could", "did", "does", "doing", "down", "for", "from", "had", "has", "have",
            "bible", "chapter", "how", "into", "just", "kjv", "like", "not", "of", "off", "on", "once", "only", "or", "our",
            "out", "over", "own", "please", "read", "show", "same", "should", "so", "some", "such",
            "scripture", "passage", "mean", "means", "meaning", "explain", "interpret", "interpreting", "say",
            "tell", "than", "that", "the", "their", "them", "there", "these", "they", "this", "those",
            "through", "too", "under", "until", "very", "what", "when", "where", "which", "while",
            "verse", "verses", "who", "with", "would", "you", "your"));
    private OfflineKnowledgeStore() {}

    public static final class VerifiedPackage {
        public final String version;
        public final String digest;
        public final JSONArray documents;
        public final long publishedAt;

        private VerifiedPackage(String version, String digest, JSONArray documents, long publishedAt) {
            this.version = version;
            this.digest = digest;
            this.documents = documents;
            this.publishedAt = publishedAt;
        }
    }

    private static final class VerifiedControl {
        final long sequence;
        final long issuedAt;
        final long expiresAt;
        final String digest;
        final java.util.Set<String> withdrawnSources;

        VerifiedControl(long sequence, long issuedAt, long expiresAt, String digest,
                        java.util.Set<String> withdrawnSources) {
            this.sequence = sequence;
            this.issuedAt = issuedAt;
            this.expiresAt = expiresAt;
            this.digest = digest;
            this.withdrawnSources = withdrawnSources;
        }
    }

    private static final class ControlCheckpoint {
        final long sequence;
        final long manifestSequence;
        final String manifestDigest;
        final long manifestExpiry;
        final String lastEvent;
        final long lastEventAt;

        ControlCheckpoint(long sequence, long manifestSequence, String manifestDigest,
                          long manifestExpiry, String lastEvent, long lastEventAt) {
            this.sequence = sequence;
            this.manifestSequence = manifestSequence;
            this.manifestDigest = manifestDigest;
            this.manifestExpiry = manifestExpiry;
            this.lastEvent = lastEvent;
            this.lastEventAt = lastEventAt;
        }
    }

    /** Validation builds contain a synthetic-only test anchor; all other builds fail closed. */
    public static Map<String, String> trustedKeys(Context context) {
        String assetPath = "validation".equals(BuildConfig.BUILD_TYPE)
                ? "ai/knowledge/public-key.txt" : "ai/knowledge/trusted-keys.json";
        try (InputStream input = context.getAssets().open(assetPath);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            copyBounded(input, output, "validation".equals(BuildConfig.BUILD_TYPE) ? 256 : 16 * 1024);
            if ("validation".equals(BuildConfig.BUILD_TYPE)) {
                String key = output.toString("UTF-8").trim();
                byte[] decoded = Base64.decode(key, Base64.DEFAULT);
                if (decoded.length != 32) return Collections.emptyMap();
                Map<String, String> keys = new java.util.HashMap<>();
                keys.put("synthetic-device-key", key);
                try (InputStream controlKeyInput = context.getAssets().open("ai/knowledge/control-public-key.txt");
                     ByteArrayOutputStream controlKeyOutput = new ByteArrayOutputStream()) {
                    copyBounded(controlKeyInput, controlKeyOutput, 256);
                    String controlKey = controlKeyOutput.toString("UTF-8").trim();
                    byte[] controlBytes = Base64.decode(controlKey, Base64.DEFAULT);
                    if (controlBytes.length != 32) return Collections.emptyMap();
                    keys.put("synthetic-control-key", controlKey);
                    keys.put("synthetic-rollback-key", readValidationPublicKey(context,
                            "ai/knowledge/rollback-package-public-key.txt"));
                    keys.put("synthetic-rollback-download-key", readValidationPublicKey(context,
                            "ai/knowledge/rollback-authorization-public-key.txt"));
                    return Collections.unmodifiableMap(keys);
                }
            }
            return parseTrustedKeysJson(output.toString("UTF-8"));
        } catch (Exception ignored) {
            return Collections.emptyMap();
        }
    }

    private static String readValidationPublicKey(Context context, String assetPath) throws Exception {
        try (InputStream input = context.getAssets().open(assetPath);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            copyBounded(input, output, 256);
            String key = output.toString("UTF-8").trim();
            if (Base64.decode(key, Base64.DEFAULT).length != 32) {
                throw new SecurityException("Synthetic validation trust anchor is malformed.");
            }
            return key;
        }
    }

    /** Parses externally provisioned public trust anchors; private keys are never accepted here. */
    static Map<String, String> parseTrustedKeysJson(String json) throws Exception {
        if (json == null || json.getBytes(StandardCharsets.UTF_8).length > 16 * 1024) {
            throw new SecurityException("Trusted key set is missing or too large.");
        }
        JSONObject object = new JSONObject(json);
        if (object.length() == 0 || object.length() > 32) {
            throw new SecurityException("Trusted key set has an invalid number of keys.");
        }
        Map<String, String> keys = new java.util.HashMap<>();
        for (Iterator<String> iterator = object.keys(); iterator.hasNext();) {
            String keyId = iterator.next();
            Object value = object.get(keyId);
            if (!keyId.matches("[A-Za-z0-9._-]{1,80}") || !(value instanceof String)) {
                throw new SecurityException("Trusted key entry is malformed.");
            }
            String encodedKey = (String) value;
            if (!encodedKey.matches("[A-Za-z0-9+/]+={0,2}")) {
                throw new SecurityException("Trusted Ed25519 public key is not valid base64.");
            }
            byte[] decoded = Base64.decode(encodedKey, Base64.DEFAULT);
            String canonicalKey = Base64.encodeToString(decoded, Base64.NO_WRAP);
            if ((decoded.length != 32 && decoded.length != 44) || !canonicalKey.equals(encodedKey)
                    || keys.put(keyId, encodedKey) != null) {
                throw new SecurityException("Trusted Ed25519 public key is malformed.");
            }
        }
        return Collections.unmodifiableMap(keys);
    }

    public static VerifiedPackage installBundledValidationPackage(Context context,
            String appVersion, long nowMillis) throws Exception {
        if (!"validation".equals(BuildConfig.BUILD_TYPE)) {
            throw new SecurityException("Bundled synthetic knowledge is available only in validation builds.");
        }
        Map<String, String> trustedKeys = trustedKeys(context);
        if (trustedKeys.isEmpty()) throw new SecurityException("Validation trust anchor is unavailable.");
        try (InputStream input = context.getAssets().open("ai/knowledge/package.json");
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            copyBounded(input, output, (int) MAX_BYTES);
            VerifiedPackage installed = install(context, output.toString("UTF-8"), trustedKeys, appVersion, nowMillis);
            try (InputStream controlInput = context.getAssets().open("ai/knowledge/control.json");
                 ByteArrayOutputStream controlOutput = new ByteArrayOutputStream()) {
                copyBounded(controlInput, controlOutput, 256 * 1024);
                applyWithdrawalManifest(context, controlOutput.toString("UTF-8"), trustedKeys, nowMillis);
            }
            return installed;
        }
    }

    public static VerifiedPackage verify(String envelopeJson, Map<String, String> trustedKeys,
                                         String appVersion, long nowMillis) throws Exception {
        return verify(envelopeJson, trustedKeys, appVersion, nowMillis, false);
    }

    private static VerifiedPackage verify(String envelopeJson, Map<String, String> trustedKeys,
                                          String appVersion, long nowMillis,
                                          boolean allowExpiredForInstallComparison) throws Exception {
        if (envelopeJson == null || envelopeJson.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES) {
            throw new SecurityException("Knowledge package is missing or too large.");
        }
        JSONObject envelope = new JSONObject(envelopeJson);
        JSONObject payload = envelope.getJSONObject("payload");
        String keyId = envelope.getString("signingKeyId");
        String encodedKey = trustedKeys == null ? null : trustedKeys.get(keyId);
        if (encodedKey == null) throw new SecurityException("Package signing key is not trusted.");

        JSONObject unsigned = new JSONObject(payload.toString());
        String contentDigest = unsigned.getString("contentDigest");
        unsigned.remove("contentDigest");
        String actualDigest = sha256(canonical(unsigned).getBytes(StandardCharsets.UTF_8));
        if (!MessageDigest.isEqual(contentDigest.getBytes(StandardCharsets.US_ASCII),
                actualDigest.getBytes(StandardCharsets.US_ASCII))) {
            throw new SecurityException("Package content digest is invalid.");
        }

        byte[] publicKeyBytes = Base64.decode(encodedKey, Base64.DEFAULT);
        byte[] rawPublicKey = publicKeyBytes.length == 32 ? publicKeyBytes
                : java.util.Arrays.copyOfRange(publicKeyBytes, publicKeyBytes.length - 32, publicKeyBytes.length);
        byte[] signedBytes = canonical(payload).getBytes(StandardCharsets.UTF_8);
        if (!Ed25519Verifier.verify(rawPublicKey, signedBytes,
                Base64.decode(envelope.getString("signature"), Base64.DEFAULT))) {
            throw new SecurityException("Package signature is invalid.");
        }
        if (!FORMAT.equals(payload.optString("format")) || !"PUBLIC".equals(payload.optString("audience"))) {
            throw new SecurityException("Unsupported package format or audience.");
        }
        long publishedAt = payload.optLong("publishedAt", Long.MAX_VALUE);
        if (publishedAt > nowMillis && publishedAt - nowMillis > MAX_SIGNED_CLOCK_SKEW_MS) {
            throw new SecurityException("Package publication time is in the future.");
        }
        Object expiryValue = payload.opt("expiresAt");
        if (!(expiryValue instanceof Number)
                || ((Number) expiryValue).longValue() <= 0
                || ((Number) expiryValue).doubleValue() != (double) ((Number) expiryValue).longValue()) {
            throw new SecurityException("Package expiry is missing or invalid.");
        }
        long expiresAt = ((Number) expiryValue).longValue();
        if (!allowExpiredForInstallComparison && expiresAt <= nowMillis) {
            throw new SecurityException("Package has expired.");
        }
        if (compareVersions(appVersion, payload.optString("minimumAppVersion")) < 0) {
            throw new SecurityException("Package requires a newer app version.");
        }
        JSONArray documents = payload.getJSONArray("documents");
        String snapshotKind = payload.optString("snapshotKind");
        if ((documents.length() == 0 && !"EMPTY".equals(snapshotKind))
                || (documents.length() > 0 && "EMPTY".equals(snapshotKind))) {
            throw new SecurityException("Package snapshot kind does not match its contents.");
        }
        for (int i = 0; i < documents.length(); i++) {
            JSONObject document = documents.getJSONObject(i);
            String text = document.getString("text");
            if (!MessageDigest.isEqual(document.getString("contentHash").getBytes(StandardCharsets.US_ASCII),
                    sha256(text.getBytes(StandardCharsets.UTF_8)).getBytes(StandardCharsets.US_ASCII))) {
                throw new SecurityException("A package source failed its integrity check.");
            }
            if (document.optString("sourceId").isEmpty() || document.optString("version").isEmpty()
                    || document.optString("approvalReference").isEmpty()
                    || !citationFieldIsSafe(document.optString("sourceId"), "@]")
                    || !citationFieldIsSafe(document.optString("version"), ":]")
                    || !citationFieldIsSafe(document.optString("location"), "]")) {
                throw new SecurityException("A package source is missing provenance.");
            }
        }
        return new VerifiedPackage(payload.getString("version"), contentDigest, documents, payload.getLong("publishedAt"));
    }

    public static synchronized VerifiedPackage install(Context context, String envelope,
                                          Map<String, String> trustedKeys,
                                          String appVersion, long nowMillis) throws Exception {
        VerifiedPackage verified = verify(envelope, trustedKeys, appVersion, nowMillis);
        // An expired active snapshot must not prevent a newer, valid snapshot
        // from replacing it. Its signature and contents still establish the
        // previous publication watermark.
        String previousEnvelope = readEnvelope(context);
        VerifiedPackage current = previousEnvelope == null ? null
                : verify(previousEnvelope, trustedKeys, appVersion, nowMillis, true);
        InstallCheckpoint checkpoint = readInstallCheckpoint(context);
        rejectInstallRollback(verified, current, checkpoint);

        if (previousEnvelope != null) writeEnvelope(context, PREVIOUS_PACKAGE_FILE, previousEnvelope);
        writeEnvelope(context, PACKAGE_FILE, envelope);
        try {
            ControlCheckpoint controlCheckpoint = readControlCheckpoint(context);
            long sequence = checkpoint == null ? 0L : checkpoint.controlSequence;
            if (controlCheckpoint != null) sequence = Math.max(sequence, controlCheckpoint.sequence);
            writeInstallCheckpoint(context, new InstallCheckpoint(verified.publishedAt,
                    verified.version, verified.digest, verified.digest, sequence,
                    "PACKAGE_INSTALLED", nowMillis));
        } catch (Exception error) {
            try {
                restorePreviousPackage(new AtomicFile(new File(context.getFilesDir(), PACKAGE_FILE)), previousEnvelope);
            } catch (Exception restoreError) {
                error.addSuppressed(restoreError);
            }
            try {
                restoreInstallCheckpoint(context, checkpoint);
            } catch (Exception restoreError) {
                error.addSuppressed(restoreError);
            }
            throw error;
        }
        return verified;
    }

    /** Applies a separately signed rollback to the exact package in the private previous-package slot. */
    public static synchronized VerifiedPackage installAuthorizedRollback(Context context,
            String authorizationJson, Map<String, String> trustedKeys,
            String appVersion, long nowMillis) throws Exception {
        String currentEnvelope = readEnvelope(context);
        String targetEnvelope = readEnvelope(context, PREVIOUS_PACKAGE_FILE);
        if (currentEnvelope == null || targetEnvelope == null) {
            throw new SecurityException("No installed rollback target is available.");
        }
        VerifiedPackage current = verify(currentEnvelope, trustedKeys, appVersion, nowMillis);
        VerifiedPackage target = verify(targetEnvelope, trustedKeys, appVersion, nowMillis);
        InstallCheckpoint checkpoint = readInstallCheckpoint(context);
        ControlCheckpoint controlCheckpoint = readControlCheckpoint(context);
        if (checkpoint == null || !current.digest.equals(checkpoint.activeDigest)) {
            throw new SecurityException("Installed package does not match its authenticated checkpoint.");
        }
        JSONObject authorization = verifyRollbackAuthorization(authorizationJson, trustedKeys, nowMillis);
        JSONObject payload = authorization.getJSONObject("payload");
        if (!target.digest.equals(payload.getString("targetDigest"))
                || !targetEnvelopePackageId(targetEnvelope).equals(payload.getString("targetPackageId"))
                || payload.getLong("controlSequence") <= Math.max(checkpoint.controlSequence,
                        controlCheckpoint == null ? 0L : controlCheckpoint.sequence)) {
            throw new SecurityException("Rollback authorization does not match the exact installed target or is stale.");
        }

        writeEnvelope(context, PREVIOUS_PACKAGE_FILE, currentEnvelope);
        writeEnvelope(context, PACKAGE_FILE, targetEnvelope);
        try {
            writeInstallCheckpoint(context, new InstallCheckpoint(checkpoint.publishedAt,
                    checkpoint.version, checkpoint.digest, target.digest,
                    payload.getLong("controlSequence"), "AUTHORIZED_ROLLBACK", nowMillis));
            writeControlCheckpoint(context, new ControlCheckpoint(payload.getLong("controlSequence"),
                    controlCheckpoint == null ? 0L : controlCheckpoint.manifestSequence,
                    controlCheckpoint == null ? "" : controlCheckpoint.manifestDigest,
                    controlCheckpoint == null ? 0L : controlCheckpoint.manifestExpiry,
                    "AUTHORIZED_ROLLBACK", nowMillis));
        } catch (Exception error) {
            try {
                writeEnvelope(context, PACKAGE_FILE, currentEnvelope);
                writeEnvelope(context, PREVIOUS_PACKAGE_FILE, targetEnvelope);
                writeInstallCheckpoint(context, checkpoint);
                restoreControlCheckpoint(context, controlCheckpoint);
            } catch (Exception restoreError) {
                error.addSuppressed(restoreError);
            }
            throw error;
        }
        return target;
    }

    @Nullable
    public static synchronized VerifiedPackage read(Context context, Map<String, String> trustedKeys,
                                       String appVersion, long nowMillis) throws Exception {
        String envelope = readEnvelope(context);
        if (envelope == null) return null;
        VerifiedPackage installed = verify(envelope, trustedKeys, appVersion, nowMillis);
        InstallCheckpoint checkpoint = readInstallCheckpoint(context);
        if (checkpoint != null && !installed.digest.equals(checkpoint.activeDigest)) {
            throw new SecurityException("Installed package does not match the authenticated installation checkpoint.");
        }
        if (checkpoint == null) {
            // Recover a crash between atomic package activation and checkpoint
            // commit, or establish a checkpoint for a package from an older app.
            ControlCheckpoint controlCheckpoint = readControlCheckpoint(context);
            writeInstallCheckpoint(context, new InstallCheckpoint(installed.publishedAt,
                    installed.version, installed.digest, installed.digest,
                    controlCheckpoint == null ? 0L : controlCheckpoint.sequence,
                    "PACKAGE_INSTALLED", nowMillis));
        }
        return installed;
    }

    /** Returns a safe, non-sensitive summary suitable for the AI mode subtitle. */
    @NonNull
    public static String knowledgeStatus(Context context, Map<String, String> trustedKeys,
                                         String appVersion, long nowMillis) {
        if (trustedKeys == null || trustedKeys.isEmpty()) return "trusted knowledge key unavailable";
        try {
            VerifiedPackage installed = read(context, trustedKeys, appVersion, nowMillis);
            if (installed == null) {
                return "validation".equals(BuildConfig.BUILD_TYPE)
                        ? "synthetic validation knowledge loads on the first question"
                        : "no verified knowledge package installed";
            }
            if (installed.documents.length() == 0) {
                return "verified package has no eligible church passages";
            }
            VerifiedControl control = readWithdrawalControl(context, trustedKeys, nowMillis);
            if (control == null) return "signed withdrawal status unavailable · update required";
            int eligible = 0;
            for (int i = 0; i < installed.documents.length(); i++) {
                JSONObject document = installed.documents.getJSONObject(i);
                if (!control.withdrawnSources.contains(sourceKey(document.optString("sourceId"),
                        document.optString("version")))) eligible++;
            }
            if (eligible == 0) return "verified package has no currently eligible church passages";
            return "verified package v" + installed.version + " · "
                    + eligible + " eligible passages · withdrawal status current";
        } catch (Exception error) {
            String message = error.getMessage();
            if (message != null && (message.toLowerCase(java.util.Locale.ROOT).contains("withdrawal")
                    || message.toLowerCase(java.util.Locale.ROOT).contains("control"))) {
                return "withdrawal status unavailable or expired · update required";
            }
            if (message != null && message.toLowerCase(java.util.Locale.ROOT).contains("expired")) {
                return "knowledge package expired · update required";
            }
            return "knowledge package unavailable or unverified · update required";
        }
    }

    private static String readEnvelope(Context context) throws Exception {
        return readEnvelope(context, PACKAGE_FILE);
    }

    private static String readEnvelope(Context context, String relativePath) throws Exception {
        AtomicFile atomic = new AtomicFile(new File(context.getFilesDir(), relativePath));
        if (!atomic.getBaseFile().isFile()) return null;
        try (FileInputStream input = atomic.openRead(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int count;
            while ((count = input.read(buffer)) != -1) {
                if (output.size() + count > MAX_BYTES) throw new SecurityException("Installed package is too large.");
                output.write(buffer, 0, count);
            }
            return output.toString("UTF-8");
        }
    }

    private static void restorePreviousPackage(AtomicFile atomic, @Nullable String previousEnvelope) throws Exception {
        if (previousEnvelope == null) {
            atomic.delete();
            return;
        }
        FileOutputStream restore = null;
        try {
            restore = atomic.startWrite();
            restore.write(previousEnvelope.getBytes(StandardCharsets.UTF_8));
            atomic.finishWrite(restore);
        } catch (Exception restoreError) {
            if (restore != null) atomic.failWrite(restore);
            throw restoreError;
        }
    }

    private static void writeEnvelope(Context context, String relativePath, String envelope) throws Exception {
        File target = new File(context.getFilesDir(), relativePath);
        File parent = target.getParentFile();
        if (parent == null || (!parent.isDirectory() && !parent.mkdirs())) {
            throw new java.io.IOException("Cannot create private package storage.");
        }
        AtomicFile atomic = new AtomicFile(target);
        FileOutputStream stream = null;
        try {
            stream = atomic.startWrite();
            stream.write(envelope.getBytes(StandardCharsets.UTF_8));
            atomic.finishWrite(stream);
            stream = null;
        } catch (Exception error) {
            if (stream != null) atomic.failWrite(stream);
            throw error;
        }
    }

    private static JSONObject verifyRollbackAuthorization(String envelopeJson,
            Map<String, String> trustedKeys, long nowMillis) throws Exception {
        if (envelopeJson == null || envelopeJson.getBytes(StandardCharsets.UTF_8).length > 16 * 1024) {
            throw new SecurityException("Rollback authorization is missing or too large.");
        }
        JSONObject envelope = new JSONObject(envelopeJson);
        JSONObject payload = envelope.getJSONObject("payload");
        String keyId = envelope.getString("signingKeyId");
        String encodedKey = trustedKeys == null ? null : trustedKeys.get(keyId);
        if (encodedKey == null) throw new SecurityException("Rollback signing key is not trusted.");
        JSONObject unsigned = new JSONObject(payload.toString());
        String digest = unsigned.getString("controlDigest");
        unsigned.remove("controlDigest");
        String actualDigest = sha256(canonical(unsigned).getBytes(StandardCharsets.UTF_8));
        if (!MessageDigest.isEqual(digest.getBytes(StandardCharsets.US_ASCII),
                actualDigest.getBytes(StandardCharsets.US_ASCII))) {
            throw new SecurityException("Rollback authorization digest is invalid.");
        }
        byte[] publicKeyBytes = Base64.decode(encodedKey, Base64.DEFAULT);
        byte[] rawPublicKey = publicKeyBytes.length == 32 ? publicKeyBytes
                : java.util.Arrays.copyOfRange(publicKeyBytes, publicKeyBytes.length - 32, publicKeyBytes.length);
        if (!Ed25519Verifier.verify(rawPublicKey, canonical(payload).getBytes(StandardCharsets.UTF_8),
                Base64.decode(envelope.getString("signature"), Base64.DEFAULT))) {
            throw new SecurityException("Rollback authorization signature is invalid.");
        }
        long sequence = payload.optLong("controlSequence", -1L);
        long issuedAt = payload.optLong("issuedAt", -1L);
        long expiresAt = payload.optLong("expiresAt", -1L);
        String targetDigest = payload.optString("targetDigest", "");
        String targetPackageId = payload.optString("targetPackageId", "");
        String reasonCode = payload.optString("reasonCode", "");
        if (!ROLLBACK_FORMAT.equals(payload.optString("format")) || sequence <= 0
                || issuedAt <= 0 || expiresAt <= issuedAt || expiresAt <= nowMillis
                || (issuedAt > nowMillis && issuedAt - nowMillis > MAX_SIGNED_CLOCK_SKEW_MS)
                || !targetDigest.matches("[a-f0-9]{64}")
                || !targetPackageId.matches("[A-Za-z0-9._:-]{1,120}")
                || !java.util.Arrays.asList("PACKAGE_ROLLBACK", "SOURCE_CORRECTION", "SECURITY_REMEDIATION")
                        .contains(reasonCode)) {
            throw new SecurityException("Rollback authorization is malformed, expired, or incompatible.");
        }
        Object sequenceValue = payload.opt("controlSequence");
        Object issuedValue = payload.opt("issuedAt");
        Object expiryValue = payload.opt("expiresAt");
        if (!(sequenceValue instanceof Number) || !(issuedValue instanceof Number) || !(expiryValue instanceof Number)
                || ((Number) sequenceValue).doubleValue() != (double) sequence
                || ((Number) issuedValue).doubleValue() != (double) issuedAt
                || ((Number) expiryValue).doubleValue() != (double) expiresAt) {
            throw new SecurityException("Rollback authorization timestamps or sequence are not integers.");
        }
        return envelope;
    }

    /** Verifies, replay-protects, and atomically persists the latest signed source-withdrawal manifest. */
    public static synchronized void applyWithdrawalManifest(Context context, String envelopeJson,
            Map<String, String> trustedKeys, long nowMillis) throws Exception {
        VerifiedControl control = verifyWithdrawalManifest(envelopeJson, trustedKeys, nowMillis);
        ControlCheckpoint checkpoint = readControlCheckpoint(context);
        InstallCheckpoint installCheckpoint = readInstallCheckpoint(context);
        long highWater = checkpoint == null ? 0L : checkpoint.sequence;
        if (installCheckpoint != null) highWater = Math.max(highWater, installCheckpoint.controlSequence);
        if (control.sequence <= highWater) {
            // The current endpoint returns the same signed manifest on every
            // update check. Accept an exact, already authenticated snapshot as
            // an idempotent refresh; a different envelope at that sequence or
            // any older sequence remains a replay and is rejected.
            if (checkpoint != null && control.sequence == checkpoint.sequence
                    && control.sequence == checkpoint.manifestSequence
                    && control.digest.equals(checkpoint.manifestDigest)) return;
            throw new SecurityException("Withdrawal control is stale or has already been applied.");
        }
        String previous = readEnvelope(context, CONTROL_FILE);
        writeEnvelope(context, CONTROL_FILE, envelopeJson);
        try {
            writeControlCheckpoint(context, new ControlCheckpoint(control.sequence,
                    control.sequence, control.digest, control.expiresAt,
                    "WITHDRAWAL_CONTROL_APPLIED", nowMillis));
        } catch (Exception error) {
            try {
                restorePreviousPackage(new AtomicFile(new File(context.getFilesDir(), CONTROL_FILE)), previous);
            } catch (Exception restoreError) {
                error.addSuppressed(restoreError);
            }
            try {
                restoreControlCheckpoint(context, checkpoint);
            } catch (Exception restoreError) {
                error.addSuppressed(restoreError);
            }
            throw error;
        }
    }

    private static VerifiedControl verifyWithdrawalManifest(String envelopeJson,
            Map<String, String> trustedKeys, long nowMillis) throws Exception {
        if (envelopeJson == null || envelopeJson.getBytes(StandardCharsets.UTF_8).length > 256 * 1024) {
            throw new SecurityException("Withdrawal control is missing or too large.");
        }
        JSONObject envelope = new JSONObject(envelopeJson);
        JSONObject payload = envelope.getJSONObject("payload");
        String keyId = envelope.getString("signingKeyId");
        String encodedKey = trustedKeys == null ? null : trustedKeys.get(keyId);
        if (encodedKey == null) throw new SecurityException("Withdrawal control signing key is not trusted.");
        JSONObject unsigned = new JSONObject(payload.toString());
        String digest = unsigned.getString("controlDigest");
        unsigned.remove("controlDigest");
        String actualDigest = sha256(canonical(unsigned).getBytes(StandardCharsets.UTF_8));
        if (!MessageDigest.isEqual(digest.getBytes(StandardCharsets.US_ASCII),
                actualDigest.getBytes(StandardCharsets.US_ASCII))) {
            throw new SecurityException("Withdrawal control digest is invalid.");
        }
        byte[] publicKeyBytes = Base64.decode(encodedKey, Base64.DEFAULT);
        byte[] rawPublicKey = publicKeyBytes.length == 32 ? publicKeyBytes
                : java.util.Arrays.copyOfRange(publicKeyBytes, publicKeyBytes.length - 32, publicKeyBytes.length);
        String signature = envelope.getString("signature");
        byte[] signatureBytes = Base64.decode(signature, Base64.DEFAULT);
        if (signatureBytes.length != 64 || !Base64.encodeToString(signatureBytes, Base64.NO_WRAP).equals(signature)
                || !Ed25519Verifier.verify(rawPublicKey,
                canonical(payload).getBytes(StandardCharsets.UTF_8), signatureBytes)) {
            throw new SecurityException("Withdrawal control signature is invalid.");
        }
        long sequence = payload.optLong("controlSequence", -1L);
        long issuedAt = payload.optLong("issuedAt", -1L);
        long expiresAt = payload.optLong("expiresAt", -1L);
        JSONArray withdrawn = payload.getJSONArray("withdrawnSources");
        Object sequenceValue = payload.opt("controlSequence");
        Object issuedValue = payload.opt("issuedAt");
        Object expiryValue = payload.opt("expiresAt");
        if (!CONTROL_FORMAT.equals(payload.optString("format")) || sequence <= 0 ||
                issuedAt <= 0 || expiresAt <= nowMillis || expiresAt <= issuedAt ||
                (issuedAt > nowMillis && issuedAt - nowMillis > MAX_SIGNED_CLOCK_SKEW_MS) ||
                !"Perazim AI knowledge service".equals(payload.optString("issuer")) ||
                withdrawn.length() > 10000 || !(sequenceValue instanceof Number) ||
                !(issuedValue instanceof Number) || !(expiryValue instanceof Number) ||
                ((Number) sequenceValue).doubleValue() != (double) sequence ||
                ((Number) issuedValue).doubleValue() != (double) issuedAt ||
                ((Number) expiryValue).doubleValue() != (double) expiresAt) {
            throw new SecurityException("Withdrawal control is malformed, expired, or incompatible.");
        }
        java.util.Set<String> revoked = new java.util.HashSet<>();
        java.util.Set<String> validReasons = new java.util.HashSet<>(java.util.Arrays.asList(
                "SOURCE_ERROR", "AUTHORITY_WITHDRAWAL", "LICENSING_CHANGE", "REPLACED", "SECURITY_REVIEW"));
        for (int i = 0; i < withdrawn.length(); i++) {
            JSONObject source = withdrawn.getJSONObject(i);
            String sourceId = source.optString("sourceId", "");
            String version = source.optString("version", "");
            String reason = source.optString("reasonCode", "");
            if (!sourceId.matches("[A-Za-z0-9._:-]{1,200}") ||
                    !version.matches("[A-Za-z0-9._:-]{1,100}") || !validReasons.contains(reason) ||
                    !revoked.add(sourceId + "\u0000" + version)) {
                throw new SecurityException("Withdrawal control contains an invalid or duplicate source version.");
            }
        }
        return new VerifiedControl(sequence, issuedAt, expiresAt, digest, revoked);
    }

    @Nullable
    private static VerifiedControl readWithdrawalControl(Context context,
            Map<String, String> trustedKeys, long nowMillis) throws Exception {
        String envelope = readEnvelope(context, CONTROL_FILE);
        if (envelope == null) return null;
        VerifiedControl control = verifyWithdrawalManifest(envelope, trustedKeys, nowMillis);
        ControlCheckpoint checkpoint = readControlCheckpoint(context);
        if (checkpoint == null || checkpoint.manifestSequence != control.sequence ||
                !control.digest.equals(checkpoint.manifestDigest) || control.expiresAt != checkpoint.manifestExpiry) {
            throw new SecurityException("Withdrawal control does not match its authenticated checkpoint.");
        }
        return control;
    }

    private static ControlCheckpoint readControlCheckpoint(Context context) throws Exception {
        SharedPreferences preferences = context.getSharedPreferences(CONTROL_STATE_PREFS, Context.MODE_PRIVATE);
        if (!preferences.contains("sequence") && !preferences.contains("manifest_sequence") &&
                !preferences.contains("manifest_digest") && !preferences.contains("manifest_expiry") &&
                !preferences.contains("mac")) return null;
        long sequence = preferences.getLong("sequence", -1L);
        long manifestSequence = preferences.getLong("manifest_sequence", -1L);
        String digest = preferences.getString("manifest_digest", null);
        long expiry = preferences.getLong("manifest_expiry", -1L);
        String event = preferences.getString("last_event", null);
        long eventAt = preferences.getLong("last_event_at", -1L);
        String storedMac = preferences.getString("mac", null);
        if (sequence < 0 || manifestSequence < 0 || digest == null ||
                (!digest.isEmpty() && !digest.matches("[a-f0-9]{64}")) || expiry < 0 ||
                event == null || event.length() > 40 || eventAt < 0 || storedMac == null) {
            throw new SecurityException("Withdrawal-control checkpoint is incomplete.");
        }
        byte[] expected = controlCheckpointMac(context, sequence, manifestSequence, digest, expiry, event, eventAt);
        byte[] actual;
        try { actual = Base64.decode(storedMac, Base64.DEFAULT); }
        catch (IllegalArgumentException error) { throw new SecurityException("Withdrawal-control checkpoint is invalid."); }
        if (!MessageDigest.isEqual(expected, actual)) {
            throw new SecurityException("Withdrawal-control checkpoint authentication failed.");
        }
        return new ControlCheckpoint(sequence, manifestSequence, digest, expiry, event, eventAt);
    }

    private static void writeControlCheckpoint(Context context, ControlCheckpoint checkpoint) throws Exception {
        String mac = Base64.encodeToString(controlCheckpointMac(context, checkpoint.sequence,
                checkpoint.manifestSequence, checkpoint.manifestDigest, checkpoint.manifestExpiry,
                checkpoint.lastEvent, checkpoint.lastEventAt), Base64.NO_WRAP);
        boolean saved = context.getSharedPreferences(CONTROL_STATE_PREFS, Context.MODE_PRIVATE).edit()
                .putLong("sequence", checkpoint.sequence)
                .putLong("manifest_sequence", checkpoint.manifestSequence)
                .putString("manifest_digest", checkpoint.manifestDigest)
                .putLong("manifest_expiry", checkpoint.manifestExpiry)
                .putString("last_event", checkpoint.lastEvent)
                .putLong("last_event_at", checkpoint.lastEventAt)
                .putString("mac", mac).commit();
        if (!saved) throw new java.io.IOException("Cannot persist withdrawal-control checkpoint.");
    }

    private static void restoreControlCheckpoint(Context context, @Nullable ControlCheckpoint checkpoint)
            throws Exception {
        if (checkpoint != null) {
            writeControlCheckpoint(context, checkpoint);
            return;
        }
        boolean cleared = context.getSharedPreferences(CONTROL_STATE_PREFS, Context.MODE_PRIVATE)
                .edit().clear().commit();
        if (!cleared) throw new java.io.IOException("Cannot restore withdrawal-control checkpoint.");
    }

    private static byte[] controlCheckpointMac(Context context, long sequence, long manifestSequence,
            String digest, long expiry, String event, long eventAt) throws Exception {
        return checkpointMacValue(context, "control\n" + sequence + "\n" + manifestSequence + "\n"
                + digest + "\n" + expiry + "\n" + event + "\n" + eventAt);
    }

    private static String targetEnvelopePackageId(String envelopeJson) throws JSONException {
        return new JSONObject(envelopeJson).getJSONObject("payload").getString("packageId");
    }

    private static final class InstallCheckpoint {
        final long publishedAt;
        final String version;
        final String digest;
        final String activeDigest;
        final long controlSequence;
        final String lastEvent;
        final long lastEventAt;

        InstallCheckpoint(long publishedAt, String version, String digest, String activeDigest,
                          long controlSequence, String lastEvent, long lastEventAt) {
            this.publishedAt = publishedAt;
            this.version = version;
            this.digest = digest;
            this.activeDigest = activeDigest;
            this.controlSequence = controlSequence;
            this.lastEvent = lastEvent;
            this.lastEventAt = lastEventAt;
        }
    }

    private static void rejectInstallRollback(VerifiedPackage candidate,
                                              @Nullable VerifiedPackage current,
                                              @Nullable InstallCheckpoint checkpoint) {
        if (current != null) {
            if (candidate.digest.equals(current.digest)
                    || isNotNewer(candidate, current.publishedAt, current.version, current.digest)) {
                throw new SecurityException("Package update is not newer than the installed package.");
            }
        }
        if (checkpoint != null && isNotNewer(candidate, checkpoint.publishedAt,
                checkpoint.version, checkpoint.digest)) {
            throw new SecurityException("Package update is older than the persisted installation checkpoint.");
        }
        if (checkpoint != null && candidate.digest.equals(checkpoint.digest)) {
            throw new SecurityException("Reinstalling the package high-water mark requires an authorized rollback control.");
        }
    }

    private static boolean isNotNewer(VerifiedPackage candidate, long publishedAt,
                                     String version, String digest) {
        if (candidate.digest.equals(digest)) return false;
        int publicationOrder = Long.compare(candidate.publishedAt, publishedAt);
        int versionOrder = compareVersions(candidate.version, version);
        return publicationOrder < 0 || (publicationOrder == 0 && versionOrder <= 0);
    }

    private static InstallCheckpoint readInstallCheckpoint(Context context) throws Exception {
        SharedPreferences preferences = context.getSharedPreferences(INSTALL_STATE_PREFS, Context.MODE_PRIVATE);
        if (!preferences.contains("published_at") && !preferences.contains("version")
                && !preferences.contains("digest") && !preferences.contains("mac")) return null;
        long publishedAt = preferences.getLong("published_at", -1L);
        String version = preferences.getString("version", null);
        String digest = preferences.getString("digest", null);
        String storedMac = preferences.getString("mac", null);
        if (publishedAt < 0 || version == null || digest == null || storedMac == null
                || !digest.matches("[a-f0-9]{64}")) {
            throw new SecurityException("Package installation checkpoint is incomplete.");
        }
        boolean legacy = !preferences.contains("active_digest")
                && !preferences.contains("control_sequence")
                && !preferences.contains("last_event")
                && !preferences.contains("last_event_at");
        String activeDigest = preferences.getString("active_digest", digest);
        long controlSequence = preferences.getLong("control_sequence", 0L);
        String lastEvent = preferences.getString("last_event", "PACKAGE_INSTALLED");
        long lastEventAt = preferences.getLong("last_event_at", 0L);
        if (activeDigest == null || !activeDigest.matches("[a-f0-9]{64}") || controlSequence < 0
                || lastEvent == null || lastEvent.length() > 40 || lastEventAt < 0) {
            throw new SecurityException("Package installation checkpoint is invalid.");
        }
        byte[] expected = legacy ? checkpointMacLegacy(context, publishedAt, version, digest)
                : checkpointMac(context, publishedAt, version, digest, activeDigest, controlSequence,
                lastEvent, lastEventAt);
        byte[] actual;
        try { actual = Base64.decode(storedMac, Base64.DEFAULT); }
        catch (IllegalArgumentException error) { throw new SecurityException("Package installation checkpoint is invalid."); }
        if (!MessageDigest.isEqual(expected, actual)) {
            throw new SecurityException("Package installation checkpoint authentication failed.");
        }
        return new InstallCheckpoint(publishedAt, version, digest, activeDigest, controlSequence,
                lastEvent, lastEventAt);
    }

    private static void writeInstallCheckpoint(Context context, InstallCheckpoint checkpoint) throws Exception {
        String encodedMac = Base64.encodeToString(checkpointMac(context, checkpoint.publishedAt,
                checkpoint.version, checkpoint.digest, checkpoint.activeDigest, checkpoint.controlSequence,
                checkpoint.lastEvent, checkpoint.lastEventAt), Base64.NO_WRAP);
        boolean saved = context.getSharedPreferences(INSTALL_STATE_PREFS, Context.MODE_PRIVATE).edit()
                .putLong("published_at", checkpoint.publishedAt)
                .putString("version", checkpoint.version)
                .putString("digest", checkpoint.digest)
                .putString("active_digest", checkpoint.activeDigest)
                .putLong("control_sequence", checkpoint.controlSequence)
                .putString("last_event", checkpoint.lastEvent)
                .putLong("last_event_at", checkpoint.lastEventAt)
                .putString("mac", encodedMac)
                .commit();
        if (!saved) throw new java.io.IOException("Cannot persist package installation checkpoint.");
    }

    private static void restoreInstallCheckpoint(Context context, @Nullable InstallCheckpoint checkpoint)
            throws Exception {
        if (checkpoint != null) {
            writeInstallCheckpoint(context, checkpoint);
            return;
        }
        boolean cleared = context.getSharedPreferences(INSTALL_STATE_PREFS, Context.MODE_PRIVATE)
                .edit().clear().commit();
        if (!cleared) throw new java.io.IOException("Cannot restore package installation checkpoint.");
    }

    private static byte[] checkpointMac(Context context, long publishedAt,
            String version, String digest, String activeDigest, long controlSequence,
            String lastEvent, long lastEventAt) throws Exception {
        return checkpointMacValue(context, publishedAt + "\n" + version + "\n" + digest + "\n"
                + activeDigest + "\n" + controlSequence + "\n" + lastEvent + "\n" + lastEventAt);
    }

    private static byte[] checkpointMacLegacy(Context context, long publishedAt,
                                               String version, String digest) throws Exception {
        return checkpointMacValue(context, publishedAt + "\n" + version + "\n" + digest);
    }

    private static byte[] checkpointMacValue(Context context, String value) throws Exception {
        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null);
        java.security.Key stored = keyStore.getKey(INSTALL_STATE_KEY_ALIAS, null);
        SecretKey key;
        if (stored instanceof SecretKey) {
            key = (SecretKey) stored;
        } else {
            KeyGenerator generator = KeyGenerator.getInstance("HmacSHA256", "AndroidKeyStore");
            generator.init(new KeyGenParameterSpec.Builder(INSTALL_STATE_KEY_ALIAS,
                    KeyProperties.PURPOSE_SIGN | KeyProperties.PURPOSE_VERIFY)
                    .setDigests(KeyProperties.DIGEST_SHA256)
                    .build());
            key = generator.generateKey();
        }
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(key);
        return mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
    }

    public interface UpdateCallback {
        void onInstalled(VerifiedPackage installed);
        void onError(String message);
    }

    /** Local, non-sensitive record of the most recent package update attempt. */
    public static final class UpdateStatus {
        public final String state;
        public final long lastAttemptAt;
        public final long lastSuccessAt;
        public final String lastSuccessVersion;
        public final String lastSuccessDigest;
        public final String failureCode;

        private UpdateStatus(String state, long lastAttemptAt, long lastSuccessAt,
                             String lastSuccessVersion, String lastSuccessDigest, String failureCode) {
            this.state = state;
            this.lastAttemptAt = lastAttemptAt;
            this.lastSuccessAt = lastSuccessAt;
            this.lastSuccessVersion = lastSuccessVersion;
            this.lastSuccessDigest = lastSuccessDigest;
            this.failureCode = failureCode;
        }
    }

    @NonNull
    public static UpdateStatus lastUpdateStatus(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(UPDATE_STATUS_PREFS, Context.MODE_PRIVATE);
        return new UpdateStatus(preferences.getString("state", "NEVER"),
                preferences.getLong("last_attempt_at", 0L), preferences.getLong("last_success_at", 0L),
                preferences.getString("last_success_version", ""),
                preferences.getString("last_success_digest", ""),
                preferences.getString("failure_code", ""));
    }

    private static void recordUpdateAttempt(Context context, String state, String failureCode,
                                            @Nullable VerifiedPackage success) {
        SharedPreferences.Editor editor = context.getSharedPreferences(UPDATE_STATUS_PREFS, Context.MODE_PRIVATE)
                .edit().putString("state", state)
                .putLong("last_attempt_at", System.currentTimeMillis())
                .putString("failure_code", failureCode == null ? "" : failureCode);
        if (success != null) {
            editor.putLong("last_success_at", System.currentTimeMillis())
                    .putString("last_success_version", success.version)
                    .putString("last_success_digest", success.digest);
        }
        editor.commit();
    }

    public static void downloadAndInstall(Context context, String sessionToken,
                                          Map<String, String> trustedKeys,
                                          UpdateCallback callback) {
        if (sessionToken == null || sessionToken.trim().isEmpty()) {
            recordUpdateAttempt(context, "AUTH_REQUIRED", "SIGN_IN_REQUIRED", null);
            callback.onError("Sign in to download approved offline knowledge.");
            return;
        }
        if (trustedKeys == null || trustedKeys.isEmpty()) {
            recordUpdateAttempt(context, "REJECTED", "NO_TRUSTED_SIGNING_KEY", null);
            callback.onError("This build has no trusted knowledge signing key.");
            return;
        }
        recordUpdateAttempt(context, "UPDATING", "", null);
        // Apply withdrawal controls first so a source is blocked before any new
        // package state becomes available to retrieval.
        ApiClient.get(context, "/api/ai/offline-package/control/current", sessionToken,
                new ApiClient.ApiCallback() {
            @Override public void onSuccess(int statusCode, @NonNull JSONObject controlJson) {
                try {
                    applyWithdrawalManifest(context, controlJson.toString(), trustedKeys,
                            System.currentTimeMillis());
                } catch (Exception error) {
                    recordUpdateAttempt(context, "REJECTED", "CONTROL_VERIFICATION_OR_INSTALL_FAILED", null);
                    callback.onError("Offline knowledge status could not be verified.");
                    return;
                }
                ApiClient.get(context, "/api/ai/offline-package/current", sessionToken,
                        new ApiClient.ApiCallback() {
                    @Override public void onSuccess(int packageStatus, @NonNull JSONObject packageJson) {
                        try {
                            VerifiedPackage installed = install(context, packageJson.toString(), trustedKeys,
                                    BuildConfig.VERSION_NAME, System.currentTimeMillis());
                            recordUpdateAttempt(context, "INSTALLED", "", installed);
                            callback.onInstalled(installed);
                        } catch (Exception error) {
                            recordUpdateAttempt(context, "REJECTED", "PACKAGE_VERIFICATION_OR_INSTALL_FAILED", null);
                            callback.onError("The offline knowledge package could not be verified or installed.");
                        }
                    }

                    @Override public void onError(int packageStatus, @NonNull String packageError) {
                        recordUpdateAttempt(context, "UNAVAILABLE", "PACKAGE_DOWNLOAD_FAILED", null);
                        callback.onError("Offline knowledge update is unavailable.");
                    }
                });
            }

            @Override public void onError(int statusCode, @NonNull String errorMessage) {
                recordUpdateAttempt(context, "UNAVAILABLE", "CONTROL_DOWNLOAD_FAILED", null);
                callback.onError("Offline knowledge status is unavailable.");
            }
        });
    }

    /** Retrieves a server-authorized exact-target rollback for the private previous package slot. */
    public static void downloadAndInstallAuthorizedRollback(Context context, String sessionToken,
            Map<String, String> trustedKeys, UpdateCallback callback) {
        if (sessionToken == null || sessionToken.trim().isEmpty()) {
            callback.onError("Sign in to request an authorized offline knowledge rollback.");
            return;
        }
        if (trustedKeys == null || trustedKeys.isEmpty()) {
            callback.onError("This build has no trusted knowledge signing key.");
            return;
        }
        android.os.AsyncTask.THREAD_POOL_EXECUTOR.execute(() -> {
            final String targetPackageId;
            final String targetDigest;
            try {
                String targetEnvelope = readEnvelope(context, PREVIOUS_PACKAGE_FILE);
                if (targetEnvelope == null) throw new SecurityException("No previous package is available.");
                VerifiedPackage target = verify(targetEnvelope, trustedKeys, BuildConfig.VERSION_NAME,
                        System.currentTimeMillis());
                targetPackageId = targetEnvelopePackageId(targetEnvelope);
                targetDigest = target.digest;
            } catch (Exception error) {
                new android.os.Handler(android.os.Looper.getMainLooper()).post(() ->
                        callback.onError("No eligible verified previous package is available for rollback."));
                return;
            }
            String path = "/api/ai/offline-package/rollback/current?targetPackageId="
                    + android.net.Uri.encode(targetPackageId) + "&targetDigest=" + android.net.Uri.encode(targetDigest);
            ApiClient.get(context, path, sessionToken, new ApiClient.ApiCallback() {
            @Override public void onSuccess(int statusCode, @NonNull JSONObject authorizationJson) {
                try {
                    VerifiedPackage restored = installAuthorizedRollback(context, authorizationJson.toString(),
                            trustedKeys, BuildConfig.VERSION_NAME, System.currentTimeMillis());
                    recordUpdateAttempt(context, "ROLLED_BACK", "", restored);
                    callback.onInstalled(restored);
                } catch (Exception error) {
                    recordUpdateAttempt(context, "REJECTED", "ROLLBACK_VERIFICATION_OR_INSTALL_FAILED", null);
                    callback.onError("The authorized offline knowledge rollback could not be verified or installed.");
                }
            }

            @Override public void onError(int statusCode, @NonNull String errorMessage) {
                recordUpdateAttempt(context, "UNAVAILABLE", "ROLLBACK_AUTHORIZATION_UNAVAILABLE", null);
                callback.onError("No current signed rollback authorization is available.");
            }
            });
        });
    }

    /** Fast app-private presence check for whether the rollback option should be shown. */
    public static boolean hasRollbackCandidate(Context context) {
        return new File(context.getFilesDir(), PACKAGE_FILE).isFile()
                && new File(context.getFilesDir(), PREVIOUS_PACKAGE_FILE).isFile();
    }

    @NonNull
    public static JSONArray retrieve(Context context, Map<String, String> trustedKeys,
                                     String appVersion, long nowMillis, String question) throws Exception {
        VerifiedPackage installed = read(context, trustedKeys, appVersion, nowMillis);
        if (question == null || question.trim().isEmpty()) return new JSONArray();
        if (isUnderspecifiedReferenceQuestion(question)) return new JSONArray();
        java.util.List<JSONObject> candidates = new java.util.ArrayList<>();
        VerifiedControl control = null;
        if (installed != null && installed.documents.length() > 0) {
            try { control = readWithdrawalControl(context, trustedKeys, nowMillis); }
            catch (Exception ignored) { control = null; }
        }
        if (installed != null && (installed.documents.length() == 0 || control != null)) {
            String[] terms = question.toLowerCase(java.util.Locale.ROOT).split("[^\\p{L}\\p{N}]+");
            for (int i = 0; i < installed.documents.length(); i++) {
                JSONObject document = installed.documents.getJSONObject(i);
                if (control != null && control.withdrawnSources.contains(sourceKey(
                        document.optString("sourceId"), document.optString("version")))) continue;
                String text = document.getString("text");
                String normalized = text.toLowerCase(java.util.Locale.ROOT);
                int score = 0;
                for (String term : terms) {
                    if (term.length() >= 3 && !QUERY_STOP_WORDS.contains(term) && normalized.contains(term)) score++;
                }
                if (score > 0) candidates.add(document);
            }
        }
        candidates.sort((left, right) -> {
            try {
                return Integer.compare(overlap(right.getString("text"), question), overlap(left.getString("text"), question));
            } catch (JSONException error) { return 0; }
        });
        JSONArray selected = new JSONArray();
        int chars = 0;
        boolean scriptureQuestion = LocalScriptureRetriever.isScriptureRelated(question);
        int documentLimit = scriptureQuestion ? 2 : 3;
        for (JSONObject document : candidates) {
            if (selected.length() >= documentLimit || chars >= 3600) break;
            selected.put(document);
            chars += Math.min(1200, document.optString("text").length());
        }
        if (scriptureQuestion && selected.length() < 3) {
            JSONArray scripture = LocalScriptureRetriever.retrieve(context, question);
            for (int i = 0; i < scripture.length() && selected.length() < 3; i++) {
                JSONObject passage = scripture.getJSONObject(i);
                selected.put(passage);
                chars += passage.optString("text").length();
                if (chars >= 3600) break;
            }
        }
        return selected;
    }

    private static int overlap(String text, String question) {
        int score = 0;
        String normalized = text.toLowerCase(java.util.Locale.ROOT);
        for (String term : question.toLowerCase(java.util.Locale.ROOT).split("[^\\p{L}\\p{N}]+")) {
            if (term.length() >= 3 && !QUERY_STOP_WORDS.contains(term) && normalized.contains(term)) score++;
        }
        return score;
    }

    static boolean isUnderspecifiedReferenceQuestion(String question) {
        if (question == null || question.trim().isEmpty()) return false;
        boolean refersToUnspecifiedText = false;
        for (String term : question.toLowerCase(java.util.Locale.ROOT).split("[^\\p{L}\\p{N}]+")) {
            if ("passage".equals(term) || "verse".equals(term) || "verses".equals(term)
                    || "scripture".equals(term) || "text".equals(term)) {
                refersToUnspecifiedText = true;
                continue;
            }
            if (term.length() >= 3 && !QUERY_STOP_WORDS.contains(term)) return false;
        }
        return refersToUnspecifiedText;
    }

    private static String sourceKey(String sourceId, String version) {
        return sourceId + "\u0000" + version;
    }

    private static String canonical(Object value) throws JSONException {
        if (value == JSONObject.NULL || value == null) return "null";
        if (value instanceof JSONObject) {
            JSONObject object = (JSONObject) value;
            TreeMap<String, Object> sorted = new TreeMap<>();
            for (Iterator<String> keys = object.keys(); keys.hasNext();) {
                String key = keys.next();
                sorted.put(key, object.get(key));
            }
            StringBuilder result = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<String, Object> entry : sorted.entrySet()) {
                if (!first) result.append(',');
                first = false;
                result.append(JSONObject.quote(entry.getKey())).append(':').append(canonical(entry.getValue()));
            }
            return result.append('}').toString();
        }
        if (value instanceof JSONArray) {
            JSONArray array = (JSONArray) value;
            StringBuilder result = new StringBuilder("[");
            for (int i = 0; i < array.length(); i++) {
                if (i > 0) result.append(',');
                result.append(canonical(array.get(i)));
            }
            return result.append(']').toString();
        }
        if (value instanceof String) return JSONObject.quote((String) value);
        if (value instanceof Number || value instanceof Boolean) return value.toString();
        throw new JSONException("Unsupported package JSON value.");
    }

    private static String sha256(byte[] value) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(value);
        StringBuilder hex = new StringBuilder();
        for (byte item : digest) hex.append(String.format(java.util.Locale.ROOT, "%02x", item & 0xff));
        return hex.toString();
    }

    private static void copyBounded(InputStream input, ByteArrayOutputStream output, int maximum) throws Exception {
        byte[] buffer = new byte[4096];
        int count;
        while ((count = input.read(buffer)) != -1) {
            if (output.size() + count > maximum) throw new SecurityException("Knowledge asset is too large.");
            output.write(buffer, 0, count);
        }
    }

    private static boolean citationFieldIsSafe(String value, String forbidden) {
        if (value == null || value.trim().isEmpty()) return false;
        for (int i = 0; i < value.length(); i++) {
            char current = value.charAt(i);
            if (Character.isISOControl(current) || forbidden.indexOf(current) >= 0) return false;
        }
        return true;
    }

    private static int compareVersions(String left, String right) {
        String[] leftParts = left.split("-", 2), rightParts = right.split("-", 2);
        String[] a = leftParts[0].split("\\."), b = rightParts[0].split("\\.");
        for (int i = 0; i < Math.max(a.length, b.length); i++) {
            long x = i < a.length ? parseVersionPart(a[i]) : 0;
            long y = i < b.length ? parseVersionPart(b[i]) : 0;
            if (x != y) return Long.compare(x, y);
        }
        if (leftParts.length == 1 && rightParts.length == 2) return 1;
        if (leftParts.length == 2 && rightParts.length == 1) return -1;
        if (leftParts.length == 2) return leftParts[1].compareTo(rightParts[1]);
        return 0;
    }

    private static long parseVersionPart(String value) {
        if (!value.matches("\\d{1,18}")) throw new IllegalArgumentException("Invalid application version.");
        return Long.parseLong(value);
    }
}
