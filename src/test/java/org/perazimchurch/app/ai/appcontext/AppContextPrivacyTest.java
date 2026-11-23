package org.perazimchurch.app.ai.appcontext;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Guards the privacy rules of the app-context reader at the source level: no personal message, conversation or
 * connection store, and no network code. Reads the package sources, so it fails if a later change adds either.
 */
public class AppContextPrivacyTest {

    private static final String[] FORBIDDEN_IN_READER = {
            "MessageDao", "ConversationDao", "ConnectionDao", "MessageEntity", "ConversationEntity",
            "ConnectionEntity", "messageDao", "conversationDao", "connectionDao", "MessageRepository",
            "ConversationHistory", "HttpURLConnection", "java.net", "OkHttp", "ApiClient", "AiRelayClient"
    };

    @Test
    public void readerNeverTouchesMessagesConversationsOrNetwork() throws IOException {
        String source = read("src/main/java/org/perazimchurch/app/ai/appcontext/AppContextReader.java");
        for (String forbidden : FORBIDDEN_IN_READER) {
            assertFalse("the app-context reader must not use " + forbidden, source.contains(forbidden));
        }
    }

    @Test
    public void wholePackageStaysOfflineAndPure() throws IOException {
        for (String name : new String[] {"AppContextRouter.java", "AppContextComposer.java", "AppSnapshot.java"}) {
            String source = read("src/main/java/org/perazimchurch/app/ai/appcontext/" + name);
            assertFalse(name + " must not use the network", source.contains("java.net"));
            assertFalse(name + " must not write to storage", source.contains("putString") || source.contains(".edit()"));
        }
    }

    @Test
    public void readerWritesNothingToGameProgress() throws IOException {
        String source = read("src/main/java/org/perazimchurch/app/ai/appcontext/AppContextReader.java");
        assertTrue("the game store adapter must stay read-only", source.contains("Read-only: the guide never changes game progress."));
    }

    private static String read(String path) throws IOException {
        File file = new File(path);
        assertTrue("expected source at " + file.getAbsolutePath(), file.isFile());
        return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
    }
}
