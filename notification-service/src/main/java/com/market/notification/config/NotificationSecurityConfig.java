package com.market.notification.config;

import com.market.notification.security.MarketJwtProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(MarketJwtProperties.class)
public class NotificationSecurityConfig {
}
