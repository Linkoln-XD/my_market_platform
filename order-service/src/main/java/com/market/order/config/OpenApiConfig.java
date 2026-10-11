package com.market.order.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI marketOpenApi() {
        final String bearer = "bearerAuth";
        return new OpenAPI()
                .info(new Info()
                        .title("Market Platform API")
                        .description("Catalog and order APIs. Order lifecycle is event-driven via Kafka.")
                        .version("0.1.0"))
                .components(new Components().addSecuritySchemes(bearer, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("Demo JWT from POST /api/v1/auth/demo-token (local/test)")))
                .addSecurityItem(new SecurityRequirement().addList(bearer));
    }
}
