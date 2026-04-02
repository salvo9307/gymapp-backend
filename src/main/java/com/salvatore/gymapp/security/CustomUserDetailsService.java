package com.salvatore.gymapp.security;

import com.salvatore.gymapp.entity.User;
import com.salvatore.gymapp.repository.UserRepository;
import com.salvatore.gymapp.util.EmailHashUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        String emailHash = EmailHashUtils.sha256(email);

        User user = userRepository.findByEmailHashWithRoleAndGym(emailHash)
                .orElseThrow(() -> new UsernameNotFoundException("Utente non trovato"));

        return new CustomUserPrincipal(user);
    }

    private String sha256(String value) {
        try {
            String normalized = value == null ? "" : value.trim().toLowerCase();

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