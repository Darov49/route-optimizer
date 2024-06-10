package ru.vyatsu.route_optimizer.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.vyatsu.route_optimizer.DijkstraAlgorithm;
import ru.vyatsu.route_optimizer.bean.graph.Graph;
import ru.vyatsu.route_optimizer.bean.graph.Schedule;
import ru.vyatsu.route_optimizer.bean.graph.Vertex;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@RestController
public class DijkstraController {

    private static final String GRAPH_FILE = "graph.json";
    private Graph graph;

    public DijkstraController() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        this.graph = objectMapper.readValue(new File(GRAPH_FILE), Graph.class);
    }

    @GetMapping("/api/findShortestPath")
    public PathResult findShortestPath(@RequestParam String startVertexId, @RequestParam String endVertexId, @RequestParam String startTime) {
        DijkstraAlgorithm dijkstra = new DijkstraAlgorithm(graph);
        dijkstra.execute(startVertexId, startTime);

        List<String> path = dijkstra.getPath(endVertexId);
        int startMinutes = convertTimeToMinutes(startTime);
        int endMinutes = dijkstra.getTime(endVertexId);

        List<PathResult.StopInfo> detailedPath = new ArrayList<>();
        String previousRouteNumber = null;

        for (int i = 0; i < path.size(); i++) {
            String stopId = path.get(i);
            Vertex vertex = graph.getVertex(stopId);
            if (vertex == null) continue;

            String arrivalTime = convertMinutesToTime(startMinutes);
            String routeNumber = (i < path.size() - 1) ? dijkstra.getRouteNumber(path.get(i + 1)) : null;

            String transferInfo = null;
            if (i == 0) {
                int waitTime = calculateWaitTime(vertex, routeNumber, startMinutes);
                String nextDepartureTime = convertMinutesToTime(startMinutes + waitTime);
                transferInfo = startTime + " -> " + routeNumber + " -> " + nextDepartureTime;
                startMinutes += waitTime + getNextTravelTime(vertex, startMinutes + waitTime, routeNumber);
            } else {
                if (previousRouteNumber != null && !previousRouteNumber.equals(routeNumber)) {
                    int waitTime = calculateWaitTime(vertex, routeNumber, startMinutes);
                    String nextDepartureTime = convertMinutesToTime(startMinutes + waitTime);
                    transferInfo = arrivalTime + " -> " + routeNumber + " -> " + nextDepartureTime;
                    startMinutes += waitTime;
                }
                startMinutes += getNextTravelTime(vertex, startMinutes, routeNumber);
            }

            detailedPath.add(new PathResult.StopInfo(stopId, vertex.getName(), arrivalTime, routeNumber, transferInfo));
            previousRouteNumber = routeNumber;
        }

        // Оптимизация пересадок
        detailedPath = optimizeTransfers(detailedPath);

        String totalTime = convertMinutesToTime(endMinutes - convertTimeToMinutes(startTime));

        return new PathResult(detailedPath, totalTime);
    }

    private List<PathResult.StopInfo> optimizeTransfers(List<PathResult.StopInfo> detailedPath) {
        List<PathResult.StopInfo> optimizedPath = new ArrayList<>();
        String currentRouteNumber = null;
        String previousRouteNumber = null;

        for (int i = 0; i < detailedPath.size(); i++) {
            PathResult.StopInfo stopInfo = detailedPath.get(i);
            currentRouteNumber = stopInfo.getRouteNumber();

            if (previousRouteNumber != null && previousRouteNumber.equals(currentRouteNumber)) {
                // Пропустить пересадку, если вернулись на предыдущий маршрут
                stopInfo.setTransferInfo(null);
                stopInfo.setRouteNumber(previousRouteNumber);
            } else if (currentRouteNumber != null && i + 1 < detailedPath.size()) {
                // Проверить если можем продолжить движение на текущем маршруте
                for (int j = i + 1; j < detailedPath.size(); j++) {
                    PathResult.StopInfo nextStopInfo = detailedPath.get(j);
                    if (currentRouteNumber.equals(nextStopInfo.getRouteNumber())) {
                        // Проверить, проходит ли автобус через все остановки на данном участке
                        boolean validRoute = true;
                        for (int k = i + 1; k <= j; k++) {
                            PathResult.StopInfo intermediateStop = detailedPath.get(k);
                            if (!isRouteValid(currentRouteNumber, intermediateStop.getStopId())) {
                                validRoute = false;
                                break;
                            }
                        }
                        if (validRoute) {
                            // Удалить ненужные пересадки между i и j
                            for (int k = i + 1; k < j; k++) {
                                detailedPath.get(k).setRouteNumber(currentRouteNumber);
                                detailedPath.get(k).setTransferInfo(null);
                            }
                            break;
                        }
                    }
                }
            }
            optimizedPath.add(stopInfo);
            previousRouteNumber = currentRouteNumber;
        }

        return optimizedPath;
    }

    private boolean isRouteValid(String routeNumber, String stopId) {
        // Проверить, проходит ли маршрут через данную остановку
        Vertex vertex = graph.getVertex(stopId);
        if (vertex == null) return false;
        for (Schedule schedule : vertex.getSchedules()) {
            if (schedule.getRouteNumber().equals(routeNumber)) {
                return true;
            }
        }
        return false;
    }

    private int calculateWaitTime(Vertex vertex, String routeNumber, int currentTime) {
        for (Schedule schedule : vertex.getSchedules()) {
            if (schedule.getRouteNumber().equals(routeNumber)) {
                for (String time : schedule.getTimes()) {
                    String cleanTime = time.replaceAll("[^0-9:]", "");
                    int timeMinutes = convertTimeToMinutes(cleanTime);
                    if (timeMinutes >= currentTime) {
                        return timeMinutes - currentTime;
                    }
                }
            }
        }
        return 0;
    }

    private int getNextTravelTime(Vertex vertex, int currentTime, String routeNumber) {
        // Найти следующую остановку и рассчитать время в пути по расписанию
        for (Schedule schedule : vertex.getSchedules()) {
            if (schedule.getRouteNumber().equals(routeNumber)) {
                for (String time : schedule.getTimes()) {
                    String cleanTime = time.replaceAll("[^0-9:]", "");
                    int timeMinutes = convertTimeToMinutes(cleanTime);
                    if (timeMinutes >= currentTime) {
                        return timeMinutes - currentTime;
                    }
                }
            }
        }
        return 0;
    }

    private String convertMinutesToTime(int minutes) {
        int hours = minutes / 60;
        int mins = minutes % 60;
        return String.format("%02d:%02d", hours, mins);
    }

    private int convertTimeToMinutes(String time) {
        String[] parts = time.split(":");
        return Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]);
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PathResult {
        private List<StopInfo> path;
        private String totalTime;

        @Data
        @AllArgsConstructor
        @NoArgsConstructor
        public static class StopInfo {
            private String stopId;
            private String stopName;
            private String arrivalTime;
            private String routeNumber;
            private String transferInfo;
        }
    }
}