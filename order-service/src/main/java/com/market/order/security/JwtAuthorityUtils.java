package com.market.order.security;

import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;

public final class JwtAuthorityUtils {

    private JwtAuthorityUtils() {
    }

    public static boolean hasAdminRole(Jwt jwt) {
        if (jwt == null) {
            return false;
        }
        Object roles = jwt.getClaim("roles");
        if (roles instanceof List<?> list) {
            return list.stream()
                    .map(Object::toString)
                    .anyMatch(role -> "ADMIN".equals(role) || "ROLE_ADMIN".equals(role));
        }
        return false;
    }
}
