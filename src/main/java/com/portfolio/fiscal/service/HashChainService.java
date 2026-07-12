package com.portfolio.fiscal.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.InvalidKeyException;
import java.util.HexFormat;

/**
 * HMAC-SHA256 hash-chaining, reusing the same pattern SecureShield uses for
 * its audit log. Each receipt/audit entry's hash covers its own canonical
 * payload plus the previous entry's hash, so any retroactive edit breaks the
 * chain from that point forward.
 */
@Service
public class HashChainService {

    private static final String HMAC_ALGO = "HmacSHA256";
    private final SecretKeySpec keySpec;

    public HashChainService(@Value("${security.jwt.public-key-b64:}") String unusedPlaceholder,
                             @Value("${HASH_CHAIN_SECRET:dev-only-change-me-in-production}") String secret) {
        this.keySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGO);
    }

    /**
     * Computes hash(payload || previousHash). previousHash is empty string
     * for the first entry in a chain (e.g. first invoice of a fiscal day).
     */
    public String computeHash(String canonicalPayload, String previousHash) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGO);
            mac.init(keySpec);
            String input = canonicalPayload + "|" + (previousHash == null ? "" : previousHash);
            byte[] digest = mac.doFinal(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("Failed to compute hash chain entry", e);
        }
    }

    /** Short human-readable verification code derived from the full hash, for printing on receipts. */
    public String shortVerificationCode(String fullHash) {
        return fullHash.substring(0, 8).toUpperCase();
    }
}
