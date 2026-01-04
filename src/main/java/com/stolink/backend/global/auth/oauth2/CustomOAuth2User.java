package com.stolink.backend.global.auth.oauth2;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;

public class CustomOAuth2User extends DefaultOAuth2User {

    private final UUID userId;

    public CustomOAuth2User(Collection<? extends GrantedAuthority> authorities, Map<String, Object> attributes,
            String nameAttributeKey, UUID userId) {
        super(authorities, attributes, nameAttributeKey);
        this.userId = userId;
    }

    public UUID getUserId() {
        return userId;
    }
}
