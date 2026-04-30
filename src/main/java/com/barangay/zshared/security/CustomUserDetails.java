package com.barangay.zshared.security;

import com.barangay.user_management.user.model.User;
import com.barangay.zshared.enumerated.UserStatus;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public record CustomUserDetails(User user) implements UserDetails {

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        List<GrantedAuthority> authorities = new ArrayList<>();

        authorities.add(new SimpleGrantedAuthority(
                "ROLE_" + user.getRole().getRoleName()
        ));

        if (user.getDepartments() != null) {
            user.getDepartments().forEach(dept ->
                    authorities.add(new SimpleGrantedAuthority(
                            "DEPT_" + dept.getName().toUpperCase().replace(" ", "_")
                    ))
            );
        }

        return authorities;
    }

    @Override
    public String getPassword()  { return user.getPassword(); }

    @Override
    public String getUsername()  { return user.getSystemEmail(); }

    @Override
    public boolean isAccountNonLocked() {
        return user.getIsLocked() != null && !user.getIsLocked();
    }

    @Override
    public boolean isEnabled() {
        return user.getStatus() == UserStatus.ACTIVE;
    }

    @Override public boolean isAccountNonExpired()   { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
}