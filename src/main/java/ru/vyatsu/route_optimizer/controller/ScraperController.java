package ru.vyatsu.route_optimizer.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.vyatsu.route_optimizer.JsonUtil;
import ru.vyatsu.route_optimizer.bean.Route;
import ru.vyatsu.route_optimizer.bean.StopSchedule;
import ru.vyatsu.route_optimizer.scraper.RouteScraper;

import java.io.IOException;
import java.util.List;

@RestController
public class ScraperController {

    private final RouteScraper routeScraper;

    private final JsonUtil jsonUtil;

    public ScraperController(RouteScraper routeScraper, JsonUtil jsonUtil) {
        this.routeScraper = routeScraper;
        this.jsonUtil = jsonUtil;
    }

    @GetMapping("/api/scrapeRoutes")
    public String scrapeRoutes() {
        String url = "https://m.cdsvyatka.com/";
        List<Route> routes = routeScraper.scrapeRoutes(url);

        for (Route route : routes) {
            List<StopSchedule> stops = routeScraper.getStopsForRoute(route.getId());

            for (StopSchedule stop : stops) {
                try {
                    String[] routeNameParts = route.getName().split(":");
                    String routeIdentifier = routeNameParts[0].split(" ")[1];
                    List<String> schedule = routeScraper.getScheduleForStop(stop.getCode(), routeIdentifier);
                    stop.setSchedule(schedule);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    routeScraper.closeDriver();
                    return "Error occurred while scraping schedule for stop " + stop.getName() + " on route " + route.getName();
                }

            }
            try {
                String[] routeNameParts = route.getName().split(":");
                String fileName = routeNameParts[0].trim().replaceAll(" ", "_");
                String filePath = "routes/" + fileName + ".json";
                jsonUtil.writeStopsToJson(stops, filePath);
            } catch (IOException e) {
                e.printStackTrace();
                routeScraper.closeDriver();
                return "Error occurred while saving data to JSON file for route " + route.getId();
            }

        }

        routeScraper.closeDriver();
        return "Scraping completed and data saved to JSON files";
    }
}

