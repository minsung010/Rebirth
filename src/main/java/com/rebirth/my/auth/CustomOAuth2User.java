package com.rebirth.my.auth;

import com.rebirth.my.domain.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import java.io.Serializable;

@Getter
public class CustomOAuth2User implements OAuth2User, Serializable {

    private static final long serialVersionUID = 1L;

    private final User user;
    private final Map<String, Object> attributes;
    private final String nameAttributeKey;

    public CustomOAuth2User(User user, Map<String, Object> attributes, String nameAttributeKey) {
        this.user = user;
        this.attributes = attributes;
        this.nameAttributeKey = nameAttributeKey;
    }

    public Long getId() {
        return user.getId();
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        String role = user.getRole();
        System.out.println("CustomOAuth2User - User Email: " + user.getEmail() + ", DB Role: " + role
                + ", Authority: ROLE_" + role);
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role));
    }

    @Override
    public String getName() {

        // Return the value of the attribute that is considered the "name" (e.g. sub,
        // id, or email)
        // Or we can return the User's name/email if we prefer.
        // Usually getName() returns the principal name.
        // For consistency with UserDetails, maybe return email?
        // But Spring Security expects this to match the nameAttributeKey if possible.
        // Let's return the user's ID or Email as the principal name.
        // Actually, let's return the attribute value to be safe with standard OAuth
        // behavior.
        Object attributeValue = attributes.get(nameAttributeKey);
        return attributeValue != null ? attributeValue.toString() : user.getEmail();
    }

    public String getRealName() {
        return user.getName();
    }
}
