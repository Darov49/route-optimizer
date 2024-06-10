package ru.vyatsu.route_optimizer.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.vyatsu.route_optimizer.JsonUtil;
import ru.vyatsu.route_optimizer.scraper.TransitStopScraper;

@Configuration
public class AppConfig {
    @Bean
    public TransitStopScraper scraper() {
        return new TransitStopScraper();
    }

    @Bean
    public JsonUtil jsonUtil() {
        return new JsonUtil();
    }
}
