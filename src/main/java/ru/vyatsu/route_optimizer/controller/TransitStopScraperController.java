package ru.vyatsu.route_optimizer.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.vyatsu.route_optimizer.JsonUtil;
import ru.vyatsu.route_optimizer.bean.Stop;
import ru.vyatsu.route_optimizer.scraper.TransitStopScraper;

import java.io.IOException;
import java.util.Set;

@RestController
public class TransitStopScraperController {
    private final TransitStopScraper scraper;
    private final JsonUtil jsonUtil;

    public TransitStopScraperController(TransitStopScraper scraper, JsonUtil jsonUtil) {
        this.scraper = scraper;
        this.jsonUtil = jsonUtil;
    }


    @GetMapping("/api/scrapeStops")
    public String scrapeStops() {
        String url = "https://m.cdsvyatka.com/";
        Set<Stop> stops = scraper.scrapeStops(url);
        try {
            jsonUtil.writeStopsToJson(stops, "transitStops.json");
            return "Scraping completed and data saved to transitStops.json";
        } catch (IOException e) {
            e.printStackTrace();
            return "Error occurred while saving data to JSON file";
        }
    }
}
