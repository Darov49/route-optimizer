package ru.vyatsu.route_optimizer.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.vyatsu.route_optimizer.DijkstraAlgorithm;
import ru.vyatsu.route_optimizer.bean.graph.Edge;
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
        int totalTimeInMinutes = dijkstra.getTime(endVertexId) - dijkstra.getTime(startVertexId); // Разница между конечным и начальным временем

        List<PathResult.StopInfo> detailedPath = new ArrayList<>();
        String previousRouteNumber = null;
        int currentTime = convertTimeToMinutes(startTime);

        // Обработка первой остановки
        if (!path.isEmpty()) {
            String firstStopId = path.get(0);
            Vertex firstVertex = graph.getVertex(firstStopId);
            if (firstVertex != null) {
                // Получаем данные для первой остановки
                String routeNumber = dijkstra.getRouteNumber(path.get(1));
                int waitTime = calculateWaitTime(firstVertex, routeNumber, currentTime);
                String departureTime = convertMinutesToTime(currentTime + waitTime);

                String transferInfo = startTime + " -> " + routeNumber + " -> " + departureTime;

                // Обновляем текущее время прибытия на следующую остановку
                int travelTime = getNextTravelTime(firstVertex, routeNumber, currentTime + waitTime);
                currentTime += waitTime + travelTime;

                detailedPath.add(new PathResult.StopInfo(firstStopId, firstVertex.getName(), departureTime, routeNumber, transferInfo));
                previousRouteNumber = routeNumber;
            }
        }

        // Обработка остальных остановок
        for (int i = 1; i < path.size(); i++) {
            String stopId = path.get(i);
            Vertex vertex = graph.getVertex(stopId);
            if (vertex == null) continue;

            String arrivalTime = convertMinutesToTime(currentTime);
            String routeNumber = (i < path.size() - 1) ? dijkstra.getRouteNumber(path.get(i + 1)) : null; // Номер маршрута, на котором уедем с данной остановки

            String transferInfo = null;
            if (previousRouteNumber != null && !previousRouteNumber.equals(routeNumber)) {
                int waitTime = calculateWaitTime(vertex, routeNumber, currentTime);
                String nextDepartureTime = convertMinutesToTime(currentTime + waitTime);
                transferInfo = arrivalTime + " -> " + routeNumber + " -> " + nextDepartureTime;
                currentTime += waitTime; // добавляем время ожидания только если была пересадка
            }

            detailedPath.add(new PathResult.StopInfo(stopId, vertex.getName(), arrivalTime, routeNumber, transferInfo));

            previousRouteNumber = routeNumber;
            if (i < path.size() - 1) {
                currentTime += getNextTravelTime(vertex, routeNumber, currentTime);
            }
        }

        // Оптимизация пересадок
        detailedPath = optimizeTransfers(detailedPath);

        String totalTime = convertMinutesToTime(totalTimeInMinutes);

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
                    if (time.matches("\\d{2}:\\d{2}")) { // проверяем, что время в правильном формате
                        int timeMinutes = convertTimeToMinutes(time);
                        if (timeMinutes >= currentTime) {
                            return timeMinutes - currentTime;
                        }
                    }
                }
            }
        }
        return 0;
    }

    private int getNextTravelTime(Vertex vertex, String routeNumber, int currentTime) {
        int travelTime = Integer.MAX_VALUE;
        for (Edge edge : graph.getEdges(vertex.getId())) {
            if (edge.getRouteNumber().equals(routeNumber)) {
                Vertex endVertex = graph.getVertex(edge.getEndVertex());
                if (endVertex != null) {
                    int edgeTravelTime = calculateTravelTime(vertex, endVertex, routeNumber, currentTime);
                    if (edgeTravelTime < travelTime) {
                        travelTime = edgeTravelTime;
                    }
                }
            }
        }
        return travelTime;
    }

    private int calculateTravelTime(Vertex startVertex, Vertex endVertex, String routeNumber, int currentTime) {
        List<String> startTimes = getScheduleTimes(startVertex, routeNumber);
        List<String> endTimes = getScheduleTimes(endVertex, routeNumber);

        if (startTimes.isEmpty() || endTimes.isEmpty()) {
            return Integer.MAX_VALUE;
        }

        int startTime = findClosestTime(startTimes, currentTime);
        int endTime = findClosestTime(endTimes, startTime);

        return endTime - startTime;
    }

    private List<String> getScheduleTimes(Vertex vertex, String routeNumber) {
        for (Schedule schedule : vertex.getSchedules()) {
            if (schedule.getRouteNumber().equals(routeNumber)) {
                return schedule.getTimes();
            }
        }
        return new ArrayList<>();
    }

    private int findClosestTime(List<String> times, int currentTime) {
        int closestTime = Integer.MAX_VALUE;
        for (String time : times) {
            if (time.matches("\\d{2}:\\d{2}")) { // проверяем, что время в правильном формате
                int timeMinutes = convertTimeToMinutes(time);
                if (timeMinutes >= currentTime && timeMinutes < closestTime) {
                    closestTime = timeMinutes;
                }
            }
        }
        return closestTime;
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