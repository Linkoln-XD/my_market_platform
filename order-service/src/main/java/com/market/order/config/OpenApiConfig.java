package com.market.order.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI marketOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Market Platform API")
                        .description("Catalog and order APIs. Order lifecycle is event-driven via Kafka.")
                        .version("0.1.0"));
    }
}
