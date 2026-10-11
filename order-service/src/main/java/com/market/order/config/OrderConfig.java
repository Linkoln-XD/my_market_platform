package com.market.order.config;

import com.market.order.order.OrderProperties;
import com.market.order.security.MarketSecurityProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({OrderProperties.class, MarketSecurityProperties.class})
public class OrderConfig {
}
