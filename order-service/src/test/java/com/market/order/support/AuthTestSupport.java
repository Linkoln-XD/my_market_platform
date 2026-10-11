package com.market.order.support;

import com.market.order.security.MarketSecurityProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

import java.time.Instant;
import java.util.List;

public final class AuthTestSupport {

    private AuthTestSupport() {
    }

    public static HttpHeaders bearerHeaders(
            JwtEncoder jwtEncoder, MarketSecurityProperties securityProperties, String subject) {
        return bearerHeaders(jwtEncoder, securityProperties, subject, List.of("USER"));
    }

    public static HttpHeaders bearerHeaders(
            JwtEncoder jwtEncoder,
            MarketSecurityProperties securityProperties,
            String subject,
            List<String> roles) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken(jwtEncoder, securityProperties, subject, roles));
        return headers;
    }

    public static String accessToken(
            JwtEncoder jwtEncoder,
            MarketSecurityProperties securityProperties,
            String subject,
            List<String> roles) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(securityProperties.demoToken().accessTokenTtl());
        List<String> normalizedRoles = roles == null || roles.isEmpty() ? List.of("USER") : roles;

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(securityProperties.jwt().issuer())
                .issuedAt(now)
                .expiresAt(expiresAt)
                .subject(subject)
                .claim("roles", normalizedRoles)
                .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    public static HttpHeaders jsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
}
