package com.dujiao.api.crypto;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * 与 {@code internal/upstream/signer.go} 一致：HMAC-SHA256(hex)，签名字符串
 * {@code method + "\n" + path + "\n" + timestamp + "\n" + md5Hex(body)}。
 */
public final class ChannelHmac {

    private ChannelHmac() {}

    public static String sign(String secret, String method, String path, long timestamp, byte[] body) {
        try {
            String bodyMd5 = md5Hex(body == null ? new byte[0] : body);
            String signString =
                    method
                            + "\n"
                            + path
                            + "\n"
                            + timestamp
                            + "\n"
                            + bodyMd5;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] sum = mac.doFinal(signString.getBytes(StandardCharsets.UTF_8));
            return hexEncode(sum);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public static boolean verify(
            String secret,
            String method,
            String path,
            String signatureHex,
            long timestamp,
            byte[] body) {
        String expected = sign(secret, method, path, timestamp, body);
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                signatureHex.getBytes(StandardCharsets.UTF_8));
    }

    private static String md5Hex(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            return hexEncode(md.digest(data));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static String hexEncode(byte[] data) {
        StringBuilder sb = new StringBuilder(data.length * 2);
        for (byte b : data) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
