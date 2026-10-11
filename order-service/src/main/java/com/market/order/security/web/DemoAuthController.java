package com.market.order.security.web;

import com.market.order.security.DemoJwtTokenService;
import com.market.order.security.DemoTokenDisabledException;
import com.market.order.security.MarketSecurityProperties;
import com.market.order.security.dto.DemoTokenRequest;
import com.market.order.security.dto.DemoTokenResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Auth (demo)", description = "Local/test HS256 JWT issuer — not for production")
public class DemoAuthController {

    private final DemoJwtTokenService demoJwtTokenService;
    private final MarketSecurityProperties securityProperties;

    @PostMapping("/demo-token")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Issue a demo JWT (local/test only)")
    public DemoTokenResponse issueDemoToken(@Valid @RequestBody DemoTokenRequest request) {
        if (!securityProperties.demoToken().enabled()) {
            throw new DemoTokenDisabledException();
        }
        Instant expiresAt = Instant.now().plus(securityProperties.demoToken().accessTokenTtl());
        String token = demoJwtTokenService.issueAccessToken(request.subject(), request.roles());
        return new DemoTokenResponse(token, "Bearer", expiresAt);
    }
}
