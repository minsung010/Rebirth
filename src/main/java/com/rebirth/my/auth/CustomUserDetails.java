package com.rebirth.my.auth;

import com.rebirth.my.domain.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

@Getter
public class CustomUserDetails implements UserDetails {

    private final User user;

    public CustomUserDetails(User user) {
        this.user = user;
    }

    public Long getId() {
        return user.getId();
    }
    
    public String getName() {
        return user.getName();
    }
    

    public String getMemImg() {
        return user.getMemImg();
    }


    public User getUser() {
        return this.user;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Assuming role is stored as "USER" or "ADMIN"
        String role = user.getRole();
        System.out.println("CustomUserDetails - User Email: " + user.getEmail() + ", DB Role: " + role + ", Authority: ROLE_" + role);
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role));
    }
    

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getEmail(); // Using email as username
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
        return "ACTIVE".equals(user.getStatus());
    }
}
