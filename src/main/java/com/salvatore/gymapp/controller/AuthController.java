package com.salvatore.gymapp.controller;

import com.salvatore.gymapp.dto.auth.ChangePasswordRequest;
import com.salvatore.gymapp.dto.auth.LoginRequest;
import com.salvatore.gymapp.dto.auth.LoginResponse;
import com.salvatore.gymapp.entity.gym.User;
import com.salvatore.gymapp.exception.BadRequestException;
import com.salvatore.gymapp.exception.ForbiddenException;
import com.salvatore.gymapp.exception.NotFoundException;
import com.salvatore.gymapp.repository.UserRepository;
import com.salvatore.gymapp.security.CustomUserPrincipal;
import com.salvatore.gymapp.security.JwtService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/login")
    public LoginResponse login(@RequestBody @Valid LoginRequest request) {
        try {
            String normalizedEmail = request.getEmail().trim().toLowerCase();

            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            normalizedEmail,
                            request.getPassword()
                    )
            );

            CustomUserPrincipal user = (CustomUserPrincipal) authentication.getPrincipal();
            String token = jwtService.generateToken(user);

            return new LoginResponse(
                    token,
                    user.getRole(),
                    user.getId(),
                    user.isMustChangePassword()
            );
        } catch (DisabledException ex) {
            throw new ForbiddenException("Account o palestra disattivata");
        } catch (BadCredentialsException ex) {
            throw new ForbiddenException("Credenziali non valide");
        }
    }

    @GetMapping("/me")
    public LoginResponse me(Authentication authentication) {
        CustomUserPrincipal user = (CustomUserPrincipal) authentication.getPrincipal();

        return new LoginResponse(
                null,
                user.getRole(),
                user.getId(),
                user.isMustChangePassword()
        );
    }

    @PutMapping("/change-password")
    public void changePassword(@RequestBody @Valid ChangePasswordRequest request,
                               @AuthenticationPrincipal CustomUserPrincipal currentUser) {

        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new NotFoundException("Utente non trovato"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Password attuale non corretta");
        }

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("Le nuove password non coincidono");
        }

        if (request.getNewPassword().trim().length() < 8) {
            throw new BadRequestException("La nuova password deve contenere almeno 8 caratteri");
        }

        if (passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash())) {
            throw new BadRequestException("La nuova password non può essere uguale a quella attuale");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setMustChangePassword(false);
        user.setPasswordChangedAt(LocalDateTime.now());

        userRepository.save(user);
    }
}