package com.example.demo.config.security.oauth2;

import com.example.demo.domain.entity.User;
import com.example.demo.domain.enums.UserRole;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

@Getter
public class UserPrincipal implements UserDetails, OAuth2User {
    private final UUID id;
    private final User user;
    private final String nameAttributeKey;                // OAuth
    private final Map<String, Object> attributes;         // OAuth
    private final Collection<? extends GrantedAuthority> authorities;

    public UserPrincipal(User user) {
        this.id = user.getId();
        this.user = user;
        this.nameAttributeKey = null;
        this.attributes = null;
        this.authorities = Stream.of(user.getRole())
            .map(UserRole::name)
            .map(SimpleGrantedAuthority::new)
            .toList();
    }

    public UserPrincipal(User user, Map<String, Object> attributes, String nameAttributeKey) {
        this.id = user.getId();
        this.user = user;
        this.nameAttributeKey = nameAttributeKey;
        this.attributes = attributes;
        this.authorities = Stream.of(user.getRole())
            .map(UserRole::name)
            .map(SimpleGrantedAuthority::new)
            .toList();
    }

    @Override
    public String getName() {
        return user.getDisplayName();
    }

    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public String getUsername() {
        return user.getEmail();
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
        return true;
    }
}
