package com.salvatore.gymapp.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class EmailHashUtils {

    private EmailHashUtils() {
    }

    public static String normalize(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    public static String sha256(String email) {
        try {
            String normalized = normalize(email);
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(normalized.getBytes(StandardCharsets.UTF_8));

            StringBuilder hex = new StringBuilder();
            for (byte b : digest) {
                hex.append(String.format("%02x", b));
            }

            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 non disponibile", e);
        }
    }
}