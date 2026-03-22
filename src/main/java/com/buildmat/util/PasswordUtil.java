package com.buildmat.util;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

public class PasswordUtil {

    public static String hash(String password, String salt) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(Base64.getDecoder().decode(salt));
            byte[] hashed = md.digest(password.getBytes("UTF-8"));
            return Base64.getEncoder().encodeToString(hashed);
        } catch (Exception e) {
            throw new RuntimeException("Hashing failed", e);
        }
    }

    public static String generateSalt() {
        SecureRandom sr = new SecureRandom();
        byte[] salt = new byte[16];
        sr.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    // Store as "hash:salt"
    public static String createStoredPassword(String plainPassword) {
        String salt = generateSalt();
        String hash = hash(plainPassword, salt);
        return hash + ":" + salt;
    }

    public static boolean verify(String plainPassword, String storedPassword) {
        try {
            String[] parts = storedPassword.split(":");
            if (parts.length != 2) return false;
            String hash = parts[0];
            String salt = parts[1];
            String computed = hash(plainPassword, salt);
            return computed.equals(hash);
        } catch (Exception e) {
            return false;
        }
    }
}
