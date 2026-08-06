package vn.hoidanit.resumeservice.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

public class SignatureUtil {

    private static final String HMAC_SHA256 = "HmacSHA256";

    private SignatureUtil() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static String createSignatureData(String userId, String userEmail, String userRoles, long timestamp) {
        return String.format("%s:%s:%s:%d",
            userId != null ? userId : "",
            userEmail != null ? userEmail : "",
            userRoles != null ? userRoles : "",
            timestamp);
    }

    public static String generateSignature(String data, String secret) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
            mac.init(secretKeySpec);
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Error generating signature", e);
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
}

