package dev.nefor.activator.plugin.util;

import java.security.SecureRandom;
import java.util.Base64;

public final class NonceGenerator {
    private static final SecureRandom RANDOM = new SecureRandom();

    private NonceGenerator() {
    }

    public static String randomNonce(int bytes) {
        byte[] data = new byte[bytes];
        RANDOM.nextBytes(data);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(data);
    }
}
