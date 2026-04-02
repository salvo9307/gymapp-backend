package com.salvatore.gymapp.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

@Service
public class CryptoService {

    private final SecretKeySpec secretKeySpec;

    public CryptoService(@Value("${app.crypto.key}") String key) {
        this.secretKeySpec = buildKey(key);
    }

    public String encrypt(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);
            byte[] encrypted = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new IllegalStateException("Errore durante la cifratura", e);
        }
    }

    public String decrypt(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec);
            byte[] decoded = Base64.getDecoder().decode(value);
            byte[] decrypted = cipher.doFinal(decoded);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Errore durante la decifratura", e);
        }
    }

    private SecretKeySpec buildKey(String myKey) {
        try {
            byte[] key = myKey.getBytes(StandardCharsets.UTF_8);
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            key = sha.digest(key);
            byte[] key16 = new byte[16];
            System.arraycopy(key, 0, key16, 0, 16);
            return new SecretKeySpec(key16, "AES");
        } catch (Exception e) {
            throw new IllegalStateException("Errore durante la creazione della chiave AES", e);
        }
    }
}