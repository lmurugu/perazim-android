package org.perazimchurch.app.ai.local;

import java.nio.file.Files;
import java.nio.file.Paths;

/** Decodes Node-produced base64 lines with StrictBase64 and prints hex, for comparison. */
public final class Base64Harness {
    public static void main(String[] args) throws Exception {
        for (String line : new String(Files.readAllBytes(Paths.get(args[0])), "UTF-8").split("\n")) {
            if (line.isEmpty()) continue;
            StringBuilder hex = new StringBuilder();
            for (byte b : StrictBase64.decode(line)) hex.append(String.format("%02x", b & 0xff));
            System.out.println(hex);
        }
    }
}
