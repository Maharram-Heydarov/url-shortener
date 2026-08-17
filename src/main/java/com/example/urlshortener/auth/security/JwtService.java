package com.example.urlshortener.auth.security;

import com.example.urlshortener.auth.entity.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class JwtService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final TypeReference<Map<String, Object>> CLAIMS_TYPE =
            new TypeReference<>() {
            };

    private final ObjectMapper objectMapper;
    private final SecretKeySpec signingKey;
    private final Duration expiration;

    public JwtService(
            ObjectMapper objectMapper,
            @Value("${app.security.jwt.secret}") String secret,
            @Value("${app.security.jwt.expiration}") Duration expiration
    ) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalArgumentException("JWT secret must be configured");
        }

        this.objectMapper = objectMapper;
        this.signingKey = new SecretKeySpec(
                secret.getBytes(StandardCharsets.UTF_8),
                HMAC_ALGORITHM
        );
        this.expiration = expiration;
    }

    public String generateToken(User user) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(expiration);

        Map<String, Object> header = new LinkedHashMap<>();
        header.put("alg", "HS256");
        header.put("typ", "JWT");

        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("sub", user.getEmail());
        claims.put("uid", user.getId());
        claims.put("iat", issuedAt.getEpochSecond());
        claims.put("exp", expiresAt.getEpochSecond());

        String encodedHeader = encodeJson(header);
        String encodedClaims = encodeJson(claims);
        String signingInput = encodedHeader + "." + encodedClaims;
        String signature = base64Url(hmac(signingInput));

        return signingInput + "." + signature;
    }

    public Optional<JwtClaims> parseAndValidate(String token) {
        String[] parts = token.split("\\.");

        if (parts.length != 3) {
            return Optional.empty();
        }

        String signingInput = parts[0] + "." + parts[1];
        String expectedSignature = base64Url(hmac(signingInput));

        if (!constantTimeEquals(expectedSignature, parts[2])) {
            return Optional.empty();
        }

        try {
            Map<String, Object> claims = objectMapper.readValue(
                    Base64.getUrlDecoder().decode(parts[1]),
                    CLAIMS_TYPE
            );

            Long userId = numberClaim(claims, "uid")
                    .map(Number::longValue)
                    .orElse(null);
            String email = stringClaim(claims, "sub").orElse(null);
            Instant expiresAt = numberClaim(claims, "exp")
                    .map(Number::longValue)
                    .map(Instant::ofEpochSecond)
                    .orElse(null);

            if (userId == null
                    || email == null
                    || expiresAt == null
                    || !expiresAt.isAfter(Instant.now())) {

                return Optional.empty();
            }

            return Optional.of(new JwtClaims(userId, email, expiresAt));

        } catch (IllegalArgumentException | JacksonException exception) {
            return Optional.empty();
        }
    }

    private String encodeJson(Map<String, Object> value) {
        try {
            return base64Url(objectMapper.writeValueAsBytes(value));
        } catch (JacksonException exception) {
            throw new IllegalStateException("Unable to encode JWT", exception);
        }
    }

    private byte[] hmac(String value) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(signingKey);
            return mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException | InvalidKeyException exception) {
            throw new IllegalStateException("Unable to sign JWT", exception);
        }
    }

    private String base64Url(byte[] value) {
        return Base64
                .getUrlEncoder()
                .withoutPadding()
                .encodeToString(value);
    }

    private boolean constantTimeEquals(String expected, String actual) {
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.US_ASCII),
                actual.getBytes(StandardCharsets.US_ASCII)
        );
    }

    private Optional<Number> numberClaim(
            Map<String, Object> claims,
            String name
    ) {
        Object value = claims.get(name);

        if (value instanceof Number number) {
            return Optional.of(number);
        }

        return Optional.empty();
    }

    private Optional<String> stringClaim(
            Map<String, Object> claims,
            String name
    ) {
        Object value = claims.get(name);

        if (value instanceof String string && !string.isBlank()) {
            return Optional.of(string);
        }

        return Optional.empty();
    }
}
