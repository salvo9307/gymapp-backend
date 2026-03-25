package com.salvatore.gymapp.controller;

import com.salvatore.gymapp.dto.auth.LoginRequest;
import com.salvatore.gymapp.dto.auth.LoginResponse;
import com.salvatore.gymapp.exception.ForbiddenException;
import com.salvatore.gymapp.security.CustomUserPrincipal;
import com.salvatore.gymapp.security.JwtService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    @PostMapping("/login")
    public LoginResponse login(@RequestBody @Valid LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );

            CustomUserPrincipal user = (CustomUserPrincipal) authentication.getPrincipal();
            String token = jwtService.generateToken(user);

            return new LoginResponse(
                    token,
                    user.getRole(),
                    user.getId()
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
                user.getId()
        );
    }
}