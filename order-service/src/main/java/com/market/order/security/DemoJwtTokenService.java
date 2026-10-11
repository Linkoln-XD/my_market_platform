package com.market.order.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DemoJwtTokenService {

    private final JwtEncoder jwtEncoder;
    private final MarketSecurityProperties securityProperties;

    public String issueAccessToken(String subject, List<String> roles) {
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
}
