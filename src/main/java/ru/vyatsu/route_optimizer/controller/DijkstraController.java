package ru.vyatsu.route_optimizer.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.vyatsu.route_optimizer.DijkstraAlgorithm;
import ru.vyatsu.route_optimizer.bean.graph.*;
import ru.vyatsu.route_optimizer.exception.GraphFileNotFoundException;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

import static ru.vyatsu.route_optimizer.constant.StringConstants.NON_TIME_PATTERN;
import static ru.vyatsu.route_optimizer.constant.StringConstants.TRANSFER_DELIMITER;

@RestController
public class DijkstraController {
    private Graph graph;

    @Value("${graph.file}")
    private String graphFile;

    @GetMapping("/api/findShortestPath")
    public ResponseEntity<Object> findShortestPath(@RequestParam String startVertexId,
                                                   @RequestParam String endVertexId,
                                                   @RequestParam String startTime) {

        try {
            initializeGraph();
        } catch (GraphFileNotFoundException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (IOException e) {
            return new ResponseEntity<>("An error occurred while reading the graph file.",
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }

        DijkstraAlgorithm dijkstraAlgorithm = new DijkstraAlgorithm(graph);
        dijkstraAlgorithm.execute(startVertexId, startTime);

        List<String> path = dijkstraAlgorithm.getPath(endVertexId);
        int totalTimeInMinutes = dijkstraAlgorithm.getTime(endVertexId) - dijkstraAlgorithm.getTime(startVertexId);

        List<StopInfo> detailedPath = new ArrayList<>();
        String previousRouteNumber = null;
        int currentTime = convertTimeToMinutes(startTime);

        // Обработка первой остановки
        if (!path.isEmpty()) {
            String firstStopId = path.get(0);
            Vertex firstVertex = graph.getVertex(firstStopId);

            if (firstVertex != null) {
                String routeNumber = dijkstraAlgorithm.getRouteNumber(path.get(1));
                int waitTime = calculateWaitTime(firstVertex, routeNumber, currentTime);
                String departureTime = convertMinutesToTime(currentTime + waitTime);

                String transferInfo = startTime + TRANSFER_DELIMITER + routeNumber + TRANSFER_DELIMITER + departureTime;

                int travelTime = getNextTravelTime(firstVertex, routeNumber, currentTime + waitTime);
                currentTime += waitTime + travelTime;

                detailedPath.add(new StopInfo(firstStopId, firstVertex.getName(), startTime, routeNumber, transferInfo));
                previousRouteNumber = routeNumber;
            }
        }

        // Обработка остальных остановок
        for (int i = 1; i < path.size(); i++) {
            String stopId = path.get(i);
            Vertex vertex = graph.getVertex(stopId);

            if (vertex == null) {
                continue;
            }

            String arrivalTime = convertMinutesToTime(currentTime);
            String routeNumber = (i < path.size() - 1)
                    ? dijkstraAlgorithm.getRouteNumber(path.get(i + 1))
                    : previousRouteNumber;

            String transferInfo = null;
            if (previousRouteNumber != null && !previousRouteNumber.equals(routeNumber)) {
                int waitTime = calculateWaitTime(vertex, routeNumber, currentTime);
                String nextDepartureTime = convertMinutesToTime(currentTime + waitTime);
                transferInfo = arrivalTime + TRANSFER_DELIMITER + routeNumber + TRANSFER_DELIMITER + nextDepartureTime;
                currentTime += waitTime; // добавляем время ожидания только в случае пересадки
            }

            detailedPath.add(new StopInfo(stopId, vertex.getName(), arrivalTime, routeNumber, transferInfo));

            previousRouteNumber = routeNumber;
            if (i < path.size() - 1) {
                currentTime += getNextTravelTime(vertex, routeNumber, currentTime);
            }
        }

        // Обработка последней остановки
        if (!detailedPath.isEmpty()) {
            StopInfo lastStopInfo = detailedPath.getLast();
            lastStopInfo.setTransferInfo(null);
        }

        detailedPath = optimizeTransfers(detailedPath);

        return new ResponseEntity<>(new PathResult(detailedPath, convertMinutesToTime(totalTimeInMinutes)),
                HttpStatus.OK);
    }

    private void initializeGraph() throws IOException {
        File file = new File(graphFile);
        if (!file.exists()) {
            throw new GraphFileNotFoundException("Failed to find the graph file. " +
                    "Please run graph-builder-controller to generate it.");
        }

        ObjectMapper objectMapper = new ObjectMapper();
        this.graph = objectMapper.readValue(file, Graph.class);
    }

    @ExceptionHandler(GraphFileNotFoundException.class)
    public ResponseEntity<String> handleGraphFileNotFoundException(GraphFileNotFoundException ex) {
        return new ResponseEntity<>(ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    private List<StopInfo> optimizeTransfers(List<StopInfo> detailedPath) {
        List<StopInfo> optimizedPath = new ArrayList<>();
        String currentRouteNumber;
        String previousRouteNumber = null;

        for (int i = 0; i < detailedPath.size(); i++) {
            StopInfo stopInfo = detailedPath.get(i);
            currentRouteNumber = stopInfo.getRouteNumber();

            if (previousRouteNumber != null && previousRouteNumber.equals(currentRouteNumber)) {
                stopInfo.setTransferInfo(null);
                stopInfo.setRouteNumber(previousRouteNumber);

                optimizedPath.add(stopInfo);
                previousRouteNumber = currentRouteNumber;

                continue;
            }

            if (currentRouteNumber != null && i + 1 < detailedPath.size()) {
                for (int j = i + 1; j < detailedPath.size(); j++) {
                    StopInfo nextStopInfo = detailedPath.get(j);

                    if (!currentRouteNumber.equals(nextStopInfo.getRouteNumber())) {
                        continue;
                    }

                    String arrivalTimeAtCurrentStop = stopInfo.getArrivalTime();
                    String departureTimeAtNextStop = nextStopInfo.getArrivalTime();

                    if (isTimeMatching(arrivalTimeAtCurrentStop, departureTimeAtNextStop, currentRouteNumber)) {
                        for (int k = i + 1; k < j; k++) {
                            detailedPath.get(k).setRouteNumber(currentRouteNumber);
                            detailedPath.get(k).setTransferInfo(null);
                        }
                        break;
                    }

                }
            }

            optimizedPath.add(stopInfo);
            previousRouteNumber = currentRouteNumber;
        }

        return optimizedPath;
    }

    private boolean isTimeMatching(String arrivalTime, String departureTime, String routeNumber) {
        int arrivalIndex = getTimeIndex(arrivalTime, routeNumber);
        int departureIndex = getTimeIndex(departureTime, routeNumber);
        return arrivalIndex != -1 && arrivalIndex == departureIndex;
    }

    private int getTimeIndex(String time, String routeNumber) {
        for (Vertex vertex : graph.getVertices().values()) {
            for (Schedule schedule : vertex.getSchedules()) {
                if (!schedule.getRouteNumber().equals(routeNumber)) {
                    continue;
                }

                List<String> times = schedule.getTimes();
                for (int i = 0; i < times.size(); i++) {
                    String cleanTime = times.get(i).replaceAll(NON_TIME_PATTERN, "");
                    if (cleanTime.equals(time)) {
                        return i;
                    }
                }
            }
        }
        return -1;
    }

    private int calculateWaitTime(Vertex vertex, String routeNumber, int currentTime) {
        for (Schedule schedule : vertex.getSchedules()) {
            if (!schedule.getRouteNumber().equals(routeNumber)) {
                continue;
            }

            Map<String, Integer> validTimes = getValidTimes(schedule);
            for (Map.Entry<String, Integer> entry : validTimes.entrySet()) {
                if (entry.getValue() >= currentTime) {
                    return entry.getValue() - currentTime;
                }
            }
        }
        return 0;
    }

    private int getNextTravelTime(Vertex vertex, String routeNumber, int currentTime) {
        int travelTime = Integer.MAX_VALUE;

        for (Edge edge : graph.getEdges(vertex.getId())) {
            if (!edge.getRouteNumber().equals(routeNumber)) {
                continue;
            }

            Vertex endVertex = graph.getVertex(edge.getEndVertex());
            if (endVertex != null) {
                int edgeTravelTime = calculateTravelTime(vertex, endVertex, routeNumber, currentTime);
                if (edgeTravelTime < travelTime) {
                    travelTime = edgeTravelTime;
                }
            }
        }
        return travelTime;
    }

    private int calculateTravelTime(Vertex startVertex, Vertex endVertex, String routeNumber, int currentTime) {
        Map<String, Integer> startTimes = getValidTimes(getSchedule(startVertex, routeNumber));
        Map<String, Integer> endTimes = getValidTimes(getSchedule(endVertex, routeNumber));

        if (startTimes.isEmpty() || endTimes.isEmpty()) {
            return Integer.MAX_VALUE;
        }

        int startTime = findClosestTime(startTimes, currentTime);
        int endTime = findClosestTime(endTimes, startTime);

        return endTime - startTime;
    }

    private Map<String, Integer> getValidTimes(Schedule schedule) {
        Map<String, Integer> validTimes = new LinkedHashMap<>();
        Pattern timePattern = Pattern.compile("^\\d{2}:\\d{2}$");

        Map<String, Boolean> timeWithTextMap = new HashMap<>();
        for (String time : schedule.getTimes()) {
            boolean hasText = !timePattern.matcher(time).matches();
            timeWithTextMap.put(time, timeWithTextMap.getOrDefault(time, false) || hasText);
        }

        for (String time : schedule.getTimes()) {
            if (Boolean.TRUE.equals(timeWithTextMap.get(time)) &&
                    schedule.getTimes().stream().filter(t -> t.equals(time)).count() > 1) {
                continue;
            }

            if (timePattern.matcher(time).matches()) {
                validTimes.put(time, convertTimeToMinutes(time));
            }
        }

        return validTimes;
    }

    private Schedule getSchedule(Vertex vertex, String routeNumber) {
        for (Schedule schedule : vertex.getSchedules()) {
            if (schedule.getRouteNumber().equals(routeNumber)) {
                return schedule;
            }
        }
        return null;
    }

    private int findClosestTime(Map<String, Integer> times, int currentTime) {
        int closestTime = Integer.MAX_VALUE;
        for (Map.Entry<String, Integer> entry : times.entrySet()) {
            if (entry.getValue() >= currentTime && entry.getValue() < closestTime) {
                closestTime = entry.getValue();
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
}
