package org.perazimchurch.app.data.remote;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.net.URL;
import org.junit.Test;

public final class ApiClientTransportTest {
    @Test
    public void everyApiRouteRequiresProtectedTransport() {
        for (String path : new String[] {
                "/api/auth/login", "/api/auth/register", "/api/auth/logout", "/api/auth/me",
                "/api/admin/audit", "/api/membership/claims", "/api/access/permissions"
        }) {
            assertTrue(path, SecureTransportPolicy.requiresSecureTransport(path, false));
        }
        assertTrue(SecureTransportPolicy.requiresSecureTransport("/api/health", false));
        assertTrue(SecureTransportPolicy.requiresSecureTransport("/api/campuses", false));
        assertTrue(SecureTransportPolicy.requiresSecureTransport("/api/classroom-calls/sessions", true));
        assertTrue(SecureTransportPolicy.requiresSecureTransport("/api/community/posts", true));
        assertTrue(SecureTransportPolicy.requiresSecureTransport("/api/community/posts", false));
    }

    @Test
    public void httpsIsAllowedAndCleartextLoopbackIsValidationOnly() throws Exception {
        assertTrue(SecureTransportPolicy.allowsProtectedTransport(new URL("https://api.example.test/api/auth/login"), "debug"));
        assertTrue(SecureTransportPolicy.allowsProtectedTransport(new URL("https://api.example.test/api/auth/login"), "release"));
        assertTrue(SecureTransportPolicy.allowsProtectedTransport(new URL("http://127.0.0.1:3000/api/auth/login"), "validation"));
        assertTrue(SecureTransportPolicy.allowsProtectedTransport(new URL("http://[::1]:3000/api/auth/login"), "validation"));
        assertTrue(SecureTransportPolicy.allowsProtectedTransport(new URL("http://localhost:3000/api/auth/login"), "validation"));

        assertFalse(SecureTransportPolicy.allowsProtectedTransport(new URL("http://127.0.0.1:3000/api/auth/login"), "debug"));
        assertFalse(SecureTransportPolicy.allowsProtectedTransport(new URL("http://api.example.test/api/auth/login"), "validation"));
        assertFalse(SecureTransportPolicy.allowsProtectedTransport(new URL("https://user:secret@api.example.test/api/auth/login"), "release"));
    }
}
