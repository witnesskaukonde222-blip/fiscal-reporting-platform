package com.portfolio.fiscal.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * Dual-strategy RSA public key loader: Base64 env var first, classpath PEM
 * fallback. Same pattern used in SecureShield's own RsaKeyConfig — this
 * service verifies tokens *issued* by SecureShield, so it only ever needs
 * SecureShield's public key, never a private key.
 */
@Configuration
public class RsaKeyConfig {

    private final ResourceLoader resourceLoader;

    public RsaKeyConfig(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @Bean
    public PublicKey shieldPublicKey(
            @Value("${security.jwt.public-key-b64:}") String publicKeyB64,
            @Value("${security.jwt.public-key-path:classpath:keys/shield-public-key.pem}") String publicKeyPath
    ) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {

        String pem;
        if (publicKeyB64 != null && !publicKeyB64.isBlank()) {
            pem = new String(Base64.getDecoder().decode(publicKeyB64), StandardCharsets.UTF_8);
        } else {
            Resource resource = resourceLoader.getResource(publicKeyPath);
            try (InputStream is = resource.getInputStream()) {
                pem = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }
        }

        String cleaned = pem
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");

        byte[] decoded = Base64.getDecoder().decode(cleaned);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(decoded);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePublic(spec);
    }
}
