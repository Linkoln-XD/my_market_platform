package com.market.order.security.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record DemoTokenRequest(
        @NotBlank
        @Size(min = 1, max = 128)
        String subject,
        List<@NotBlank @Size(max = 32) String> roles
) {
}
