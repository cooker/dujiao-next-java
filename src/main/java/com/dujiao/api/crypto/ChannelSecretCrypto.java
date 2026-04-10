package com.dujiao.api.crypto;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * 与 Go {@code crypto.Encrypt/Decrypt} 对齐：AES-256-GCM，密文为 hex（nonce 前置）。
 */
public final class ChannelSecretCrypto {

    private static final int GCM_TAG_BITS = 128;
    private static final int NONCE_LEN = 12;
    private static final SecureRandom RANDOM = new SecureRandom();

    private ChannelSecretCrypto() {}

    public static byte[] deriveKey(String masterSecret) {
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            return sha.digest(masterSecret.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public static String encrypt(byte[] aesKey, String plaintext) {
        try {
            byte[] nonce = new byte[NONCE_LEN];
            RANDOM.nextBytes(nonce);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(
                    Cipher.ENCRYPT_MODE,
                    new SecretKeySpec(aesKey, "AES"),
                    new GCMParameterSpec(GCM_TAG_BITS, nonce));
            byte[] cipherBytes = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            byte[] combined = new byte[nonce.length + cipherBytes.length];
            System.arraycopy(nonce, 0, combined, 0, nonce.length);
            System.arraycopy(cipherBytes, 0, combined, nonce.length, cipherBytes.length);
            return hexEncode(combined);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public static String decrypt(byte[] aesKey, String ciphertextHex) {
        try {
            byte[] combined = hexDecode(ciphertextHex);
            if (combined.length < NONCE_LEN) {
                throw new IllegalArgumentException("ciphertext too short");
            }
            byte[] nonce = new byte[NONCE_LEN];
            System.arraycopy(combined, 0, nonce, 0, NONCE_LEN);
            byte[] cipherBytes = new byte[combined.length - NONCE_LEN];
            System.arraycopy(combined, NONCE_LEN, cipherBytes, 0, cipherBytes.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(
                    Cipher.DECRYPT_MODE,
                    new SecretKeySpec(aesKey, "AES"),
                    new GCMParameterSpec(GCM_TAG_BITS, nonce));
            byte[] plain = cipher.doFinal(cipherBytes);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalArgumentException("decrypt failed", e);
        }
    }

    private static String hexEncode(byte[] data) {
        StringBuilder sb = new StringBuilder(data.length * 2);
        for (byte b : data) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private static byte[] hexDecode(String hex) {
        int n = hex.length();
        if ((n & 1) != 0) {
            throw new IllegalArgumentException("invalid hex");
        }
        byte[] out = new byte[n / 2];
        for (int i = 0; i < n; i += 2) {
            out[i / 2] = (byte) Integer.parseInt(hex.substring(i, i + 2), 16);
        }
        return out;
    }
}
