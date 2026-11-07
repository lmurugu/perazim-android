package org.perazimchurch.app.data.remote;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

/** Actual device HTTP acceptance, using the separately started synthetic backend only. */
@RunWith(AndroidJUnit4.class)
public class PhaseBBackendDeviceTest {
    @Test public void syntheticClaimIsReviewedAndRevokedThroughRealBackend() throws Exception {
        Bundle args = InstrumentationRegistry.getArguments();
        assumeTrue("Requires explicit synthetic fixture and isolated reverse port", "true".equals(args.getString("phasebFixture")));
        String pin = args.getString("phasebPin");
        String otp = args.getString("phasebOtp");
        assertNotNull(pin);
        assertNotNull(otp);
        Context target = InstrumentationRegistry.getInstrumentation().getTargetContext();
        String preference = "phaseb_device_transport_" + UUID.randomUUID();
        Context transport = new ContextWrapper(target) {
            @Override public Context getApplicationContext() { return this; }
            @Override public SharedPreferences getSharedPreferences(String name, int mode) {
                return target.getSharedPreferences(preference, mode);
            }
        };
        try {
            ApiClient.setBaseUrl(transport, "http://127.0.0.1:55476");
            String pilotToken = login(transport, "+254700008861", pin);
            String memberToken = login(transport, "+254700008862", pin);
            Result step = call(cb -> ApiClient.post(transport, "/api/admin/security/step-up",
                    json("pin", pin, "code", otp), pilotToken, cb), 200);
            String privilegedToken = step.data.getString("token");
            assertEquals("REGISTERED", status(transport, memberToken));
            call(cb -> ApiClient.get(transport, "/api/membership/member-resource", memberToken, cb), 403);

            JSONArray records = new JSONArray().put(json("reference", "SYNTHETIC_PHONE_FIXTURE",
                    "full_name", "Synthetic Device Claim", "phone", "+254700008862"));
            admin(transport, "POST", "/membership/import", json("source", "SYNTHETIC_DEVICE", "records", records,
                    "request_id", UUID.randomUUID().toString(), "reason", "SYNTHETIC_TEST"), pilotToken, privilegedToken, 201);
            Result receipt = call(cb -> ApiClient.post(transport, "/api/membership/claims", json(
                    "name", "Synthetic Device Claim", "phone", "+254700008862", "request_id", UUID.randomUUID().toString()), memberToken, cb), 202);
            assertEquals(3, receipt.data.length());
            assertTrue(receipt.data.getBoolean("received"));
            assertEquals("VERIFICATION_PENDING", status(transport, memberToken));
            admin(transport, "GET", "/membership/records", null, memberToken, privilegedToken, 403);

            JSONObject record = admin(transport, "GET", "/membership/records", null, pilotToken, privilegedToken, 200)
                    .data.getJSONArray("records").getJSONObject(0);
            JSONObject claim = admin(transport, "GET", "/membership/claims", null, pilotToken, privilegedToken, 200)
                    .data.getJSONArray("claims").getJSONObject(0);
            admin(transport, "PATCH", "/membership/records/" + record.getString("id"), json(
                    "full_name", "Synthetic Device Claim", "phone", "+254700008862",
                    "request_id", UUID.randomUUID().toString(), "reason", "SYNTHETIC_TEST"), pilotToken, privilegedToken, 200);
            admin(transport, "POST", "/membership/claims/" + claim.getString("id") + "/decision", json(
                    "decision", "APPROVE", "record_id", record.getString("id"),
                    "request_id", UUID.randomUUID().toString(), "reason", "IDENTITY_CONFIRMED"), pilotToken, privilegedToken, 200);
            assertEquals("VERIFIED_MEMBER", status(transport, memberToken));
            call(cb -> ApiClient.get(transport, "/api/membership/member-resource", memberToken, cb), 200);
            admin(transport, "POST", "/membership/users/" + claim.getString("user_id") + "/revoke", json(
                    "request_id", UUID.randomUUID().toString(), "reason", "VERIFICATION_ERROR"), pilotToken, privilegedToken, 200);
            assertEquals("VERIFICATION_REVOKED", status(transport, memberToken));
            call(cb -> ApiClient.get(transport, "/api/membership/member-resource", memberToken, cb), 403);
        } finally {
            target.deleteSharedPreferences(preference);
        }
    }

    private static String login(Context context, String phone, String pin) throws Exception {
        return call(cb -> ApiClient.post(context, "/api/auth/login", json("phone", phone, "pin", pin), null, cb), 200)
                .data.getString("token");
    }

    private static String status(Context context, String token) throws Exception {
        return call(cb -> ApiClient.get(context, "/api/membership/status", token, cb), 200).data.getString("status");
    }

    private static Result admin(Context context, String method, String path, JSONObject body,
                                String bearer, String privileged, int status) throws Exception {
        return call(cb -> ApiClient.privileged(context, method, "/api/admin" + path, body, bearer, privileged, cb), status);
    }

    private static JSONObject json(Object... pairs) throws Exception {
        JSONObject value = new JSONObject();
        for (int i = 0; i < pairs.length; i += 2) value.put((String) pairs[i], pairs[i + 1]);
        return value;
    }

    private interface Request { void send(ApiClient.ApiCallback callback) throws Exception; }
    private static final class Result {
        int status;
        JSONObject data;
    }

    private static Result call(Request request, int expected) throws Exception {
        CountDownLatch complete = new CountDownLatch(1);
        Result result = new Result();
        request.send(new ApiClient.ApiCallback() {
            @Override public void onSuccess(int status, @NonNull JSONObject data) {
                result.status = status;
                result.data = data;
                complete.countDown();
            }
            @Override public void onError(int status, @NonNull String message) {
                result.status = status;
                complete.countDown();
            }
        });
        assertTrue("Timed out waiting for device HTTP request", complete.await(15, TimeUnit.SECONDS));
        assertEquals(expected, result.status);
        return result;
    }
}
