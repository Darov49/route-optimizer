package ru.vyatsu.route_optimizer.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.vyatsu.route_optimizer.JsonUtil;

@Configuration
public class AppConfig {
    @Bean
    public JsonUtil jsonUtil() {
        return new JsonUtil();
    }
}
