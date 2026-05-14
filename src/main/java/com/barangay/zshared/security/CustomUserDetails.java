package com.barangay.zshared.security;

import com.barangay.user_management.user.model.User;
import com.barangay.zshared.enumerated.UserStatus;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


/**
 * Wraps the User entity so Spring Security can understand it.
 * Tells Spring who the user is, what their password is,
 * and what they are allowed to do.
 */
public record CustomUserDetails(User user) implements UserDetails {

    /**
     * Returns the list of permissions for this user.
     * Includes their role (e.g. ROLE_STAFF) and department (e.g. DEPT_BLOTTER).
     */
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

    /** Returns the user's hashed password stored in the database. */
    @Override
    public String getPassword()  { return user.getPassword(); }

    /** Returns the user's system email. Used as the identifier in the JWT token. */
    @Override
    public String getUsername()  { return user.getSystemEmail(); }

    /** Returns true if the user is not locked out of the system. */
    @Override
    public boolean isAccountNonLocked() {
        return user.getIsLocked() != null && !user.getIsLocked();
    }

    /** Returns true only if the user's status is ACTIVE. */
    @Override
    public boolean isEnabled() {
        return user.getStatus() == UserStatus.ACTIVE
                && !Boolean.TRUE.equals(user.getIsArchived());
    }

    /** Always true. Account expiry is not used in this system. */
    @Override public boolean isAccountNonExpired()   { return true; }

    /** Always true. Credential expiry is not used in this system. */
    @Override public boolean isCredentialsNonExpired() { return true; }
}