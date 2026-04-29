package com.salvatore.gymapp.security;

import com.salvatore.gymapp.entity.gym.User;
import com.salvatore.gymapp.repository.UserRepository;
import com.salvatore.gymapp.util.EmailHashUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        String normalizedEmail = email == null ? "" : email.trim().toLowerCase();
        String emailHash = EmailHashUtils.sha256(normalizedEmail);

        User user = userRepository.findByEmailHashWithRoleAndGym(emailHash)
                .orElseThrow(() -> new UsernameNotFoundException("Utente non trovato"));

        return new CustomUserPrincipal(user);
    }
}