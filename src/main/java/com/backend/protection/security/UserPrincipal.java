package com.backend.protection.security;

import com.backend.protection.entity.Role;
import com.backend.protection.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class UserPrincipal implements UserDetails {

    private final String id;
    private final String email;
    private final String username;
    private final String passwordHash;
    private final Role role;
    private final boolean isActive;
    private final String fullName;

    public UserPrincipal(User user) {
        this.id = user.getId();
        this.email = user.getEmail();
        this.username = user.getUsername();
        this.passwordHash = user.getPasswordHash();
        this.role = user.getRole();
        this.isActive = user.isActive();
        this.fullName = user.getFullName();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() { return passwordHash; }

    @Override
    public String getUsername() { return email; }

    @Override
    public boolean isAccountNonLocked() { return isActive; }

    @Override
    public boolean isEnabled() { return isActive; }

    public String getId() { return id; }
    public String getEmail() { return email; }
    public Role getRole() { return role; }
    public String getFullName() { return fullName; }
}
