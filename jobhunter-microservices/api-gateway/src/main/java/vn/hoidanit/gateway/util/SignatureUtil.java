package vn.hoidanit.gateway.util;

import lombok.extern.slf4j.Slf4j;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

@Slf4j
public class SignatureUtil {

    private static final String HMAC_SHA256 = "HmacSHA256";

    private SignatureUtil() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static String generateSignature(String data, String secret) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                    secret.getBytes(StandardCharsets.UTF_8),
                    HMAC_SHA256
            );
            mac.init(secretKeySpec);
            byte[] signatureBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(signatureBytes);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("Error generating signature", e);
            throw new RuntimeException("Failed to generate signature", e);
        }
    }

    public static boolean verifySignature(String data, String signature, String secret) {
        if (signature == null || data == null || secret == null) {
            return false;
        }

        String expectedSignature = generateSignature(data, secret);

        // Use timing-safe comparison to prevent timing attacks
        return MessageDigest.isEqual(
            signature.getBytes(StandardCharsets.UTF_8),
            expectedSignature.getBytes(StandardCharsets.UTF_8)
        );
    }

    public static String createSignatureData(String userId, String email, String roles, long timestamp) {
        return String.format("%s:%s:%s:%d",
            userId != null ? userId : "",
            email != null ? email : "",
            roles != null ? roles : "",
            timestamp);
    }
}

