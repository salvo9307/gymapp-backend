package com.salvatore.gymapp.security;

import com.salvatore.gymapp.entity.gym.User;
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
    private final boolean mustChangePassword;

    public CustomUserPrincipal(User user) {
        this.id = user.getId();
        this.email = user.getEmailBackup();
        this.password = user.getPasswordHash();
        this.role = user.getRole().getName();
        this.active = user.isActive();
        this.gymId = user.getGym() != null ? user.getGym().getId() : null;
        this.mustChangePassword = user.isMustChangePassword();
        this.gymActive = user.getGym() == null || user.getGym().isActive();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role));
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public String getPassword() {
        return password;
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
        return active && gymActive;
    }
}