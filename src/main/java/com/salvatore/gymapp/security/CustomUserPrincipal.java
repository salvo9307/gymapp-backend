package com.salvatore.gymapp.security;

import com.salvatore.gymapp.entity.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Getter
public class CustomUserPrincipal implements UserDetails {

    private final Long id;
    private final String email;
    private final String password;
    private final String role;
    private final boolean active;
    private final boolean gymActive;
    private final Long gymId;

    public CustomUserPrincipal(User user) {
        this.id = user.getId();
        this.email = user.getEmail();
        this.password = user.getPasswordHash();
        this.role = user.getRole().getName();
        this.active = user.isActive();
        this.gymId = user.getGym() != null ? user.getGym().getId() : null;

        if (user.getGym() == null) {
            this.gymActive = true;
        } else {
            this.gymActive = user.getGym().isActive();
        }
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role));
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        if ("ADMIN".equals(role)) {
            return active;
        }

        if ("MANAGER".equals(role)) {
            return active && gymActive;
        }

        if ("USER".equals(role)) {
            return gymActive;
        }

        return false;
    }
}