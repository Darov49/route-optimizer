package ru.vyatsu.route_optimizer.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.vyatsu.route_optimizer.JsonUtil;
import ru.vyatsu.route_optimizer.bean.Route;
import ru.vyatsu.route_optimizer.bean.StopSchedule;
import ru.vyatsu.route_optimizer.scraper.RouteScraper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static ru.vyatsu.route_optimizer.constant.StringConstants.JSON_FILE_EXTENSION;

@RestController
public class ScraperController {

    private final RouteScraper routeScraper;

    @Value("${routes.directory}")
    private String routesDirectory;

    public ScraperController(RouteScraper routeScraper) {
        this.routeScraper = routeScraper;
    }

    @GetMapping("/api/scrapeRoutes")
    public String scrapeRoutes() {
        List<Route> routes = routeScraper.scrapeRoutes();

        // Создаем директорию, если она не существует
        File directory = new File(routesDirectory);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        // Обработка всех маршрутов
        for (Route route : routes) {
            List<StopSchedule> stops = routeScraper.getStopsForRoute(route.getId());

            // Обработка всех остановок для маршрута
            for (StopSchedule stop : stops) {
                try {
                    String[] routeNameParts = route.getName().split(":");
                    String routeIdentifier = routeNameParts[0].split(" ")[1];

                    // Обработка расписания для каждой остановки
                    List<String> schedule = routeScraper.getScheduleForStop(stop.getCode(), routeIdentifier);
                    stop.setSchedule(schedule);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    routeScraper.closeDriver();
                    return "Error occurred while scraping schedule for stop " + stop.getName()
                            + " on route " + route.getName();
                }

            }
            try {
                String[] routeNameParts = route.getName().split(":");
                String fileName = routeNameParts[0].trim().replace(" ", "_");
                Path filePath = Paths.get(routesDirectory, fileName + JSON_FILE_EXTENSION);
                JsonUtil.writeStopsToJson(stops, filePath.toString());
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

